package com.tagnote.application.enrichment;

import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.ObservationProcessingResult;
import com.tagnote.application.enrichment.port.EnrichmentConflictTranslator;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.domain.enrichment.assertion.AssertionSource;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.assertion.TagAssertionEntity;
import com.tagnote.domain.enrichment.observation.ExternalTagObservationEntity;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.domain.enrichment.observation.ObservationStatus;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.taxonomy.alias.TagAliasEntity;
import com.tagnote.domain.taxonomy.matching.NormalizedTagName;
import com.tagnote.domain.taxonomy.matching.TagMatchResult;
import com.tagnote.domain.taxonomy.matching.TagMatchingService;
import com.tagnote.domain.taxonomy.matching.TagNameNormalizer;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import com.tagnote.infrastructure.persistence.enrichment.ExternalTagObservationJpaRepository;
import com.tagnote.infrastructure.persistence.enrichment.TagAssertionJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagAliasJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ObservationWriteService {

    private final TrackJpaRepository trackRepository;
    private final AlbumJpaRepository albumRepository;
    private final ExternalTagObservationJpaRepository observationRepository;
    private final TagAssertionJpaRepository assertionRepository;
    private final TagAliasJpaRepository aliasRepository;
    private final TagNameNormalizer normalizer;
    private final TagMatchingService matchingService;
    private final EnrichmentConflictTranslator conflictTranslator;

    @Transactional
    public ObservationProcessingResult process(
            SubjectType subjectType,
            long subjectId,
            List<ExternalTagInput> inputs
    ) {
        try {
            return processWithinTransaction(subjectType, subjectId, inputs);
        } catch (DataIntegrityViolationException failure) {
            throw conflictTranslator.translate(failure);
        }
    }

    private ObservationProcessingResult processWithinTransaction(
            SubjectType subjectType,
            long subjectId,
            List<ExternalTagInput> inputs
    ) {
        SubjectRef subject = requireSubject(subjectType, subjectId);
        if (inputs.isEmpty()) {
            return ObservationProcessingResult.empty();
        }

        List<PreparedInput> preparedInputs = inputs.stream()
                .map(input -> new PreparedInput(input, normalizer.normalize(input.rawName())))
                .toList();
        Map<ObservationKey, PreparedInput> uniqueObservationInputs = preparedInputs.stream()
                .collect(Collectors.toMap(
                        input -> input.observationKey(subject),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        Map<ObservationKey, ExternalTagObservationEntity> observations = findExistingObservations(
                subject,
                uniqueObservationInputs.values()
        );
        int reusedObservationCount = observations.size();
        List<ExternalTagObservationEntity> createdObservations = new ArrayList<>();
        uniqueObservationInputs.forEach((key, input) -> {
            if (!observations.containsKey(key)) {
                ExternalTagObservationEntity created = ExternalTagObservationEntity.createNew(
                        subject,
                        input.input().source(),
                        input.input().rawName(),
                        input.normalizedName(),
                        input.input().externalRef()
                );
                observations.put(key, created);
                createdObservations.add(created);
            }
        });
        if (!createdObservations.isEmpty()) {
            observationRepository.saveAll(createdObservations);
        }

        matchNewObservations(observations.values());
        observationRepository.flush();

        Map<AssertionKey, AssertionCandidate> assertionCandidates = buildAssertionCandidates(
                subject,
                preparedInputs,
                observations
        );
        Map<AssertionKey, TagAssertionEntity> existingAssertions = findExistingAssertions(
                subject,
                assertionCandidates.values()
        );
        List<TagAssertionEntity> createdAssertions = assertionCandidates.entrySet().stream()
                .filter(entry -> !existingAssertions.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .map(candidate -> TagAssertionEntity.createApproved(
                        subject,
                        candidate.tag(),
                        candidate.source(),
                        candidate.evidenceType(),
                        candidate.confidence()
                ))
                .toList();
        if (!createdAssertions.isEmpty()) {
            assertionRepository.saveAll(createdAssertions);
        }
        assertionRepository.flush();

        int matchedCount = (int) observations.values().stream()
                .filter(observation -> observation.getStatus() == ObservationStatus.MATCHED)
                .count();
        int newCount = (int) observations.values().stream()
                .filter(observation -> observation.getStatus() == ObservationStatus.NEW)
                .count();
        return new ObservationProcessingResult(
                createdObservations.size(),
                reusedObservationCount,
                matchedCount,
                newCount,
                createdAssertions.size(),
                existingAssertions.size()
        );
    }

    private SubjectRef requireSubject(SubjectType subjectType, long subjectId) {
        return switch (subjectType) {
            case TRACK -> {
                TrackEntity track = trackRepository.findById(subjectId)
                        .orElseThrow(() -> new IllegalArgumentException("Track subject does not exist: " + subjectId));
                yield SubjectRef.track(track);
            }
            case ALBUM -> {
                AlbumEntity album = albumRepository.findById(subjectId)
                        .orElseThrow(() -> new IllegalArgumentException("Album subject does not exist: " + subjectId));
                yield SubjectRef.album(album);
            }
        };
    }

    private Map<ObservationKey, ExternalTagObservationEntity> findExistingObservations(
            SubjectRef subject,
            Collection<PreparedInput> inputs
    ) {
        Set<ObservationKey> inputKeys = inputs.stream()
                .map(input -> input.observationKey(subject))
                .collect(Collectors.toSet());
        Set<ExternalTagSource> sources = inputs.stream()
                .map(input -> input.input().source())
                .collect(Collectors.toSet());
        Set<String> names = inputs.stream()
                .map(input -> input.normalizedName().value())
                .collect(Collectors.toSet());
        Set<String> refs = inputs.stream()
                .map(input -> input.input().externalRef())
                .collect(Collectors.toSet());
        return observationRepository.findExistingForInputs(
                        subject.type(), subject.subjectId(), sources, names, refs
                ).stream()
                .filter(observation -> inputKeys.contains(ObservationKey.from(observation)))
                .collect(Collectors.toMap(
                        observation -> ObservationKey.from(observation),
                        Function.identity()
                ));
    }

    private void matchNewObservations(Collection<ExternalTagObservationEntity> observations) {
        Set<String> normalizedNames = observations.stream()
                .filter(observation -> observation.getStatus() == ObservationStatus.NEW)
                .map(ExternalTagObservationEntity::getNormalizedName)
                .collect(Collectors.toSet());
        if (normalizedNames.isEmpty()) {
            return;
        }

        Map<String, List<TagAliasEntity>> aliasesByName = aliasRepository
                .findApprovedByNormalizedAliases(normalizedNames)
                .stream()
                .collect(Collectors.groupingBy(TagAliasEntity::getNormalizedAlias));
        observations.stream()
                .filter(observation -> observation.getStatus() == ObservationStatus.NEW)
                .forEach(observation -> {
                    NormalizedTagName name = new NormalizedTagName(observation.getNormalizedName());
                    TagMatchResult match = matchingService.match(
                            name,
                            aliasesByName.getOrDefault(name.value(), List.of())
                    );
                    match.getMatchedTag().ifPresent(observation::match);
                });
    }

    private Map<AssertionKey, AssertionCandidate> buildAssertionCandidates(
            SubjectRef subject,
            List<PreparedInput> inputs,
            Map<ObservationKey, ExternalTagObservationEntity> observations
    ) {
        Map<AssertionKey, AssertionCandidate> candidates = new LinkedHashMap<>();
        for (PreparedInput input : inputs) {
            ExternalTagObservationEntity observation = observations.get(input.observationKey(subject));
            if (observation.getStatus() != ObservationStatus.MATCHED) {
                continue;
            }
            TagEntity tag = observation.getMatchedTag();
            AssertionSource source = AssertionSource.valueOf(input.input().source().name());
            AssertionKey key = new AssertionKey(tag.getTagId(), source, input.input().evidenceType());
            candidates.putIfAbsent(key, new AssertionCandidate(
                    tag,
                    source,
                    input.input().evidenceType(),
                    input.input().confidence()
            ));
        }
        return candidates;
    }

    private Map<AssertionKey, TagAssertionEntity> findExistingAssertions(
            SubjectRef subject,
            Collection<AssertionCandidate> candidates
    ) {
        if (candidates.isEmpty()) {
            return Map.of();
        }
        Set<Long> tagIds = candidates.stream().map(candidate -> candidate.tag().getTagId()).collect(Collectors.toSet());
        Set<AssertionSource> sources = candidates.stream().map(AssertionCandidate::source).collect(Collectors.toSet());
        Set<EvidenceType> evidenceTypes = candidates.stream()
                .map(AssertionCandidate::evidenceType)
                .collect(Collectors.toSet());
        Set<AssertionKey> candidateKeys = candidates.stream()
                .map(candidate -> new AssertionKey(
                        candidate.tag().getTagId(), candidate.source(), candidate.evidenceType()
                ))
                .collect(Collectors.toSet());
        return assertionRepository.findExistingForInputs(
                        subject.type(), subject.subjectId(), tagIds, sources, evidenceTypes
                ).stream()
                .filter(assertion -> candidateKeys.contains(new AssertionKey(
                        assertion.getTag().getTagId(), assertion.getSource(), assertion.getEvidenceType()
                )))
                .collect(Collectors.toMap(
                        assertion -> new AssertionKey(
                                assertion.getTag().getTagId(),
                                assertion.getSource(),
                                assertion.getEvidenceType()
                        ),
                        Function.identity()
                ));
    }

    private record PreparedInput(ExternalTagInput input, NormalizedTagName normalizedName) {
        private ObservationKey observationKey(SubjectRef subject) {
            return new ObservationKey(
                    subject.type(),
                    subject.subjectId(),
                    input.source(),
                    normalizedName.value(),
                    input.externalRef()
            );
        }
    }

    private record ObservationKey(
            SubjectType subjectType,
            long subjectId,
            ExternalTagSource source,
            String normalizedName,
            String externalRef
    ) {
        private static ObservationKey from(ExternalTagObservationEntity observation) {
            return new ObservationKey(
                    observation.getSubjectType(),
                    observation.getSubjectId(),
                    observation.getSource(),
                    observation.getNormalizedName(),
                    observation.getExternalRef()
            );
        }
    }

    private record AssertionKey(Long tagId, AssertionSource source, EvidenceType evidenceType) {
    }

    private record AssertionCandidate(
            TagEntity tag,
            AssertionSource source,
            EvidenceType evidenceType,
            double confidence
    ) {
    }
}
