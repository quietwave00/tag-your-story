package com.tagnote.application.resolution;

import com.tagnote.application.resolution.config.TagResolutionProperties;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.domain.enrichment.assertion.AssertionSource;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.assertion.TagAssertionEntity;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.infrastructure.persistence.enrichment.TagAssertionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagInheritanceService {

    private final TagAssertionJpaRepository assertionRepository;
    private final TagResolutionProperties properties;

    public void synchronizeAlbumInheritance(
            TrackEntity track,
            List<TagAssertionEntity> trackDirectAssertions
    ) {
        Objects.requireNonNull(track, "Track must not be null");
        Objects.requireNonNull(trackDirectAssertions, "Track direct assertions must not be null");

        SubjectRef trackSubject = SubjectRef.track(track);
        long albumId = track.getAlbum().getAlbumId();
        List<TagAssertionEntity> albumAssertions = assertionRepository.findApprovedDirectBySubject(
                SubjectType.ALBUM, albumId
        );
        List<TagAssertionEntity> existingInherited = assertionRepository.findInheritedBySubject(
                SubjectType.TRACK, track.getTrackId()
        );

        Set<InheritedAssertionKey> directKeys = trackDirectAssertions.stream()
                .map(InheritedAssertionKey::from)
                .collect(Collectors.toCollection(HashSet::new));
        Map<InheritedAssertionKey, TagAssertionEntity> expectedByKey = albumAssertions.stream()
                .filter(assertion -> !directKeys.contains(InheritedAssertionKey.from(assertion)))
                .collect(Collectors.toMap(
                        InheritedAssertionKey::from,
                        Function.identity(),
                        this::higherConfidence,
                        LinkedHashMap::new
                ));
        Map<InheritedAssertionKey, TagAssertionEntity> existingByKey = existingInherited.stream()
                .collect(Collectors.toMap(
                        InheritedAssertionKey::from,
                        Function.identity(),
                        this::higherConfidence,
                        LinkedHashMap::new
                ));

        List<TagAssertionEntity> stale = existingInherited.stream()
                .filter(assertion -> !expectedByKey.containsKey(InheritedAssertionKey.from(assertion)))
                .toList();
        if (!stale.isEmpty()) {
            assertionRepository.deleteAllInBatch(stale);
        }

        double weight = properties.getAlbumToTrackInheritanceWeight();
        expectedByKey.forEach((key, parentAssertion) -> {
            double inheritedConfidence = clamp(parentAssertion.getConfidence() * weight);
            TagAssertionEntity existing = existingByKey.get(key);
            if (existing == null) {
                assertionRepository.save(TagAssertionEntity.createInheritedApproved(
                        trackSubject, parentAssertion, inheritedConfidence
                ));
                return;
            }
            if (needsUpdate(existing, parentAssertion, inheritedConfidence)) {
                existing.updateInherited(parentAssertion, inheritedConfidence);
            }
        });
    }

    private boolean needsUpdate(
            TagAssertionEntity existing,
            TagAssertionEntity parentAssertion,
            double inheritedConfidence
    ) {
        Long existingParentId = existing.getInheritedFromAssertion().getAssertionId();
        return !Objects.equals(existingParentId, parentAssertion.getAssertionId())
                || Double.compare(existing.getConfidence(), inheritedConfidence) != 0;
    }

    private TagAssertionEntity higherConfidence(TagAssertionEntity first, TagAssertionEntity second) {
        if (first.getConfidence() >= second.getConfidence()) {
            return first;
        }
        return second;
    }

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    private record InheritedAssertionKey(
            long tagId,
            AssertionSource source,
            EvidenceType evidenceType
    ) {

        private static InheritedAssertionKey from(TagAssertionEntity assertion) {
            return new InheritedAssertionKey(
                    assertion.getTag().getTagId(),
                    assertion.getSource(),
                    assertion.getEvidenceType()
            );
        }
    }
}
