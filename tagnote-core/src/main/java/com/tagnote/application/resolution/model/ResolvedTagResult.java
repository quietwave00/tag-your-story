package com.tagnote.application.resolution.model;

import com.tagnote.domain.resolution.ResolutionReason;
import com.tagnote.domain.resolution.ResolvedStatus;

import java.time.LocalDateTime;

public record ResolvedTagResult(
        long tagId,
        String tagName,
        double score,
        ResolvedStatus status,
        ResolutionReason reason,
        LocalDateTime lastResolvedAt
) {
}
