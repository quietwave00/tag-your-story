package com.tagnote.domain.resolution;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TagResolver {

    private final CanonicalTagService canonicalTagService;

    public List<ResolvedTagCandidate> resolve(
            Collection<DirectTagEvidence> evidence,
            Map<Long, CanonicalTagNode> taxonomy,
            double minimumScore
    ) {
        Objects.requireNonNull(evidence, "Direct evidence must not be null");
        Objects.requireNonNull(taxonomy, "Taxonomy must not be null");
        requireMinimumScore(minimumScore);

        Map<Long, EvidenceAccumulator> evidenceByCanonicalTag = new LinkedHashMap<>();
        for (DirectTagEvidence item : evidence) {
            Objects.requireNonNull(item, "Direct evidence must not contain null");
            canonicalTagService.findActiveCanonicalTag(item.tagId(), taxonomy)
                    .ifPresent(canonicalTagId -> evidenceByCanonicalTag
                            .computeIfAbsent(canonicalTagId, ignored -> new EvidenceAccumulator())
                            .add(item));
        }

        return evidenceByCanonicalTag.entrySet().stream()
                .map(entry -> entry.getValue().toCandidate(entry.getKey()))
                .filter(candidate -> candidate.score() >= minimumScore)
                .sorted(Comparator.comparingLong(ResolvedTagCandidate::tagId))
                .toList();
    }

    private void requireMinimumScore(double minimumScore) {
        if (!Double.isFinite(minimumScore) || minimumScore < 0.0 || minimumScore > 1.0) {
            throw new IllegalArgumentException("Minimum score must be between 0.0 and 1.0");
        }
    }

    private static final class EvidenceAccumulator {

        private Double directScore;
        private Double inheritedScore;

        private void add(DirectTagEvidence evidence) {
            if (evidence.isDirect()) {
                directScore = max(directScore, evidence.confidence());
                return;
            }
            inheritedScore = max(inheritedScore, evidence.confidence());
        }

        private ResolvedTagCandidate toCandidate(long tagId) {
            if (directScore != null) {
                return ResolvedTagCandidate.automatic(tagId, directScore);
            }
            return ResolvedTagCandidate.inheritedFromAlbum(tagId, inheritedScore);
        }

        private Double max(Double current, double candidate) {
            if (current == null) {
                return candidate;
            }
            return Math.max(current, candidate);
        }
    }
}
