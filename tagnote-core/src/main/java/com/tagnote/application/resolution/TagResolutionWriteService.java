package com.tagnote.application.resolution;

import com.tagnote.application.resolution.config.TagResolutionProperties;
import com.tagnote.application.resolution.model.ResolvedTagResult;
import com.tagnote.application.resolution.port.ResolutionConflictTranslator;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.domain.enrichment.assertion.TagAssertionEntity;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.resolution.CanonicalTagNode;
import com.tagnote.domain.resolution.DirectTagEvidence;
import com.tagnote.domain.resolution.ResolvedTagCandidate;
import com.tagnote.domain.resolution.SubjectTagResolvedEntity;
import com.tagnote.domain.resolution.TagResolver;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import com.tagnote.infrastructure.persistence.enrichment.TagAssertionJpaRepository;
import com.tagnote.infrastructure.persistence.resolution.SubjectTagResolvedJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagResolutionWriteService {

    private final TrackJpaRepository trackRepository;
    private final AlbumJpaRepository albumRepository;
    private final TagAssertionJpaRepository assertionRepository;
    private final TagJpaRepository tagRepository;
    private final SubjectTagResolvedJpaRepository resolvedRepository;
    private final TagResolver tagResolver;
    private final TagInheritanceService inheritanceService;
    private final TagResolutionProperties properties;
    private final ResolutionConflictTranslator conflictTranslator;

    @Transactional
    public List<ResolvedTagResult> resolve(SubjectRef subject) {
        try {
            return resolveWithinTransaction(subject);
        } catch (DataIntegrityViolationException failure) {
            throw conflictTranslator.translate(failure);
        }
    }

    private List<ResolvedTagResult> resolveWithinTransaction(SubjectRef subject) {
        List<TagAssertionEntity> assertions = loadAssertionsForResolution(subject);
        List<TagEntity> taxonomyEntities = tagRepository.findAllWithMergeTarget();
        Map<Long, TagEntity> tagsById = taxonomyEntities.stream()
                .collect(Collectors.toMap(TagEntity::getTagId, Function.identity()));
        Map<Long, CanonicalTagNode> taxonomy = taxonomyEntities.stream()
                .map(this::toCanonicalNode)
                .collect(Collectors.toMap(CanonicalTagNode::tagId, Function.identity()));
        List<DirectTagEvidence> evidence = assertions.stream()
                .map(this::toEvidence)
                .toList();
        List<ResolvedTagCandidate> candidates = tagResolver.resolve(
                evidence, taxonomy, properties.getMinimumScore()
        );

        List<SubjectTagResolvedEntity> existing = resolvedRepository.findAllBySubjectWithTag(
                subject.type(), subject.subjectId()
        );
        LocalDateTime resolvedAt = LocalDateTime.now();
        List<SubjectTagResolvedEntity> current = synchronize(
                subject, candidates, existing, tagsById, resolvedAt
        );
        resolvedRepository.flush();

        return current.stream()
                .map(this::toResult)
                .sorted(Comparator.comparingDouble(ResolvedTagResult::score).reversed()
                        .thenComparingLong(ResolvedTagResult::tagId))
                .toList();
    }

    private List<SubjectTagResolvedEntity> synchronize(
            SubjectRef subject,
            List<ResolvedTagCandidate> candidates,
            List<SubjectTagResolvedEntity> existing,
            Map<Long, TagEntity> tagsById,
            LocalDateTime resolvedAt
    ) {
        Map<Long, SubjectTagResolvedEntity> manualByTagId = existing.stream()
                .filter(row -> !row.isAutomatic())
                .collect(Collectors.toMap(row -> row.getTag().getTagId(), Function.identity()));
        Map<Long, SubjectTagResolvedEntity> automaticByTagId = existing.stream()
                .filter(SubjectTagResolvedEntity::isAutomatic)
                .collect(Collectors.toMap(row -> row.getTag().getTagId(), Function.identity()));
        Map<Long, ResolvedTagCandidate> applicableCandidates = candidates.stream()
                .filter(candidate -> !manualByTagId.containsKey(candidate.tagId()))
                .collect(Collectors.toMap(
                        ResolvedTagCandidate::tagId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        Set<Long> applicableTagIds = new HashSet<>(applicableCandidates.keySet());
        List<SubjectTagResolvedEntity> obsolete = automaticByTagId.entrySet().stream()
                .filter(entry -> !applicableTagIds.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        if (!obsolete.isEmpty()) {
            resolvedRepository.deleteAllInBatch(obsolete);
        }

        List<SubjectTagResolvedEntity> current = new ArrayList<>(manualByTagId.values());
        List<SubjectTagResolvedEntity> created = new ArrayList<>();
        applicableCandidates.forEach((tagId, candidate) -> {
            SubjectTagResolvedEntity row = automaticByTagId.get(tagId);
            if (row == null) {
                TagEntity tag = tagsById.get(tagId);
                if (tag == null) {
                    throw new IllegalStateException("Canonical tag was not loaded: " + tagId);
                }
                row = SubjectTagResolvedEntity.autoManaged(
                        subject, tag, candidate.score(), candidate.reason(), resolvedAt
                );
                created.add(row);
            } else if (Double.compare(row.getScore(), candidate.score()) != 0
                    || row.getResolutionReason() != candidate.reason()) {
                row.updateAutomatic(candidate.score(), candidate.reason(), resolvedAt);
            }
            current.add(row);
        });
        if (!created.isEmpty()) {
            resolvedRepository.saveAll(created);
        }
        return current;
    }

    private List<TagAssertionEntity> loadAssertionsForResolution(SubjectRef subject) {
        if (subject.type() == SubjectType.TRACK) {
            TrackEntity track = trackRepository.findByIdWithAlbum(subject.subjectId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "TRACK subject does not exist: " + subject.subjectId()
                    ));
            List<TagAssertionEntity> directAssertions = assertionRepository.findApprovedDirectBySubject(
                    SubjectType.TRACK, subject.subjectId()
            );
            inheritanceService.synchronizeAlbumInheritance(track, directAssertions);
            return assertionRepository.findApprovedBySubject(SubjectType.TRACK, subject.subjectId());
        }

        if (!albumRepository.existsById(subject.subjectId())) {
            throw new IllegalArgumentException("ALBUM subject does not exist: " + subject.subjectId());
        }
        return assertionRepository.findApprovedDirectBySubject(SubjectType.ALBUM, subject.subjectId());
    }

    private DirectTagEvidence toEvidence(TagAssertionEntity assertion) {
        if (assertion.getInheritedFromAssertion() == null) {
            return new DirectTagEvidence(assertion.getTag().getTagId(), assertion.getConfidence());
        }
        return DirectTagEvidence.inheritedFromAlbum(
                assertion.getTag().getTagId(), assertion.getConfidence()
        );
    }

    private CanonicalTagNode toCanonicalNode(TagEntity tag) {
        Long targetId = tag.getMergedIntoTag() == null ? null : tag.getMergedIntoTag().getTagId();
        return new CanonicalTagNode(tag.getTagId(), tag.getStatus(), targetId);
    }

    private ResolvedTagResult toResult(SubjectTagResolvedEntity row) {
        return new ResolvedTagResult(
                row.getTag().getTagId(),
                row.getTag().getName(),
                row.getScore(),
                row.getStatus(),
                row.getResolutionReason(),
                row.getLastResolvedAt()
        );
    }
}
