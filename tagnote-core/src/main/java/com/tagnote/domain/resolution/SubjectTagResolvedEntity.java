package com.tagnote.domain.resolution;

import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(
        name = "subject_tag_resolved",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_subject_tag_resolved_identity",
                columnNames = {"subject_type", "subject_id", "tag_id"}
        ),
        indexes = @Index(
                name = "idx_subject_tag_resolved_subject_score",
                columnList = "subject_type, subject_id, score"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubjectTagResolvedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resolvedId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false)
    private SubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private TagEntity tag;

    @Column(nullable = false)
    private double score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResolvedStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_reason", nullable = false)
    private ResolutionReason resolutionReason;

    @Column(name = "last_resolved_at", nullable = false)
    private LocalDateTime lastResolvedAt;

    private SubjectTagResolvedEntity(
            SubjectRef subject,
            TagEntity tag,
            double score,
            ResolvedStatus status,
            ResolutionReason resolutionReason,
            LocalDateTime resolvedAt
    ) {
        SubjectRef requiredSubject = Objects.requireNonNull(subject, "Resolved subject must not be null");
        this.subjectType = requiredSubject.type();
        this.subjectId = requiredSubject.subjectId();
        this.tag = Objects.requireNonNull(tag, "Resolved tag must not be null");
        this.score = requireScore(score);
        this.status = Objects.requireNonNull(status, "Resolved status must not be null");
        this.resolutionReason = Objects.requireNonNull(resolutionReason, "Resolution reason must not be null");
        this.lastResolvedAt = Objects.requireNonNull(resolvedAt, "Resolved time must not be null");
    }

    public static SubjectTagResolvedEntity automatic(
            SubjectRef subject,
            TagEntity tag,
            double score,
            LocalDateTime resolvedAt
    ) {
        return new SubjectTagResolvedEntity(
                subject, tag, score, ResolvedStatus.ACTIVE, ResolutionReason.AUTO, resolvedAt
        );
    }

    public static SubjectTagResolvedEntity autoManaged(
            SubjectRef subject,
            TagEntity tag,
            double score,
            ResolutionReason reason,
            LocalDateTime resolvedAt
    ) {
        if (reason != ResolutionReason.AUTO && reason != ResolutionReason.INHERITED_FROM_ALBUM) {
            throw new IllegalArgumentException("Auto-managed reason must be AUTO or INHERITED_FROM_ALBUM");
        }
        return new SubjectTagResolvedEntity(subject, tag, score, ResolvedStatus.ACTIVE, reason, resolvedAt);
    }

    public static SubjectTagResolvedEntity manual(
            SubjectRef subject,
            TagEntity tag,
            double score,
            ResolvedStatus status,
            ResolutionReason reason,
            LocalDateTime resolvedAt
    ) {
        if (isAutoManaged(status, reason)) {
            throw new IllegalArgumentException("Use auto-managed factory for automatic resolved tags");
        }
        return new SubjectTagResolvedEntity(subject, tag, score, status, reason, resolvedAt);
    }

    public void updateAutomatic(double score, ResolutionReason reason, LocalDateTime resolvedAt) {
        if (!isAutomatic()) {
            throw new IllegalStateException("Manual resolved state cannot be changed automatically");
        }
        if (reason != ResolutionReason.AUTO && reason != ResolutionReason.INHERITED_FROM_ALBUM) {
            throw new IllegalArgumentException("Auto-managed reason must be AUTO or INHERITED_FROM_ALBUM");
        }
        this.score = requireScore(score);
        this.resolutionReason = reason;
        this.lastResolvedAt = Objects.requireNonNull(resolvedAt, "Resolved time must not be null");
    }

    public boolean isAutomatic() {
        return isAutoManaged(status, resolutionReason);
    }

    public SubjectRef subjectRef() {
        return new SubjectRef(subjectType, subjectId);
    }

    private static double requireScore(double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("Resolved score must be between 0.0 and 1.0");
        }
        return value;
    }

    private static boolean isAutoManaged(ResolvedStatus status, ResolutionReason reason) {
        return status == ResolvedStatus.ACTIVE
                && (reason == ResolutionReason.AUTO || reason == ResolutionReason.INHERITED_FROM_ALBUM);
    }
}
