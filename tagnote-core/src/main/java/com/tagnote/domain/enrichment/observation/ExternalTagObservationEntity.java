package com.tagnote.domain.enrichment.observation;

import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.taxonomy.matching.NormalizedTagName;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
        name = "external_tag_observation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_external_tag_observation_identity",
                columnNames = {"subject_type", "subject_id", "source", "normalized_name", "external_ref"}
        ),
        indexes = {
                @Index(name = "idx_external_tag_observation_subject", columnList = "subject_type, subject_id"),
                @Index(name = "idx_external_tag_observation_status_name", columnList = "status, normalized_name")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalTagObservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long observationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false)
    private SubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExternalTagSource source;

    @Column(name = "raw_name", nullable = false)
    private String rawName;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @Column(name = "external_ref", nullable = false)
    private String externalRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ObservationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_tag_id")
    private TagEntity matchedTag;

    @Column(name = "observed_at", nullable = false, updatable = false)
    private LocalDateTime observedAt;

    private ExternalTagObservationEntity(
            SubjectRef subject,
            ExternalTagSource source,
            String rawName,
            NormalizedTagName normalizedName,
            String externalRef
    ) {
        SubjectRef requiredSubject = Objects.requireNonNull(subject, "Observation subject must not be null");
        this.subjectType = requiredSubject.type();
        this.subjectId = requiredSubject.subjectId();
        this.source = Objects.requireNonNull(source, "Observation source must not be null");
        this.rawName = requireText(rawName, "Raw tag name");
        this.normalizedName = Objects.requireNonNull(
                normalizedName,
                "Normalized tag name must not be null"
        ).value();
        this.externalRef = requireText(externalRef, "External reference");
        this.status = ObservationStatus.NEW;
        this.observedAt = LocalDateTime.now();
    }

    public static ExternalTagObservationEntity createNew(
            SubjectRef subject,
            ExternalTagSource source,
            String rawName,
            NormalizedTagName normalizedName,
            String externalRef
    ) {
        return new ExternalTagObservationEntity(subject, source, rawName, normalizedName, externalRef);
    }

    public static ExternalTagObservationEntity create(
            SubjectRef subject,
            ExternalTagSource source,
            String rawName,
            NormalizedTagName normalizedName,
            String externalRef
    ) {
        return createNew(subject, source, rawName, normalizedName, externalRef);
    }

    public void match(TagEntity tag) {
        requireNew("match");
        this.matchedTag = Objects.requireNonNull(tag, "Matched tag must not be null");
        this.status = ObservationStatus.MATCHED;
    }

    public void ignore() {
        requireNew("ignore");
        this.matchedTag = null;
        this.status = ObservationStatus.IGNORED;
    }

    public void promote(TagEntity tag) {
        requireNew("promote");
        this.matchedTag = Objects.requireNonNull(tag, "Promoted tag must not be null");
        this.status = ObservationStatus.PROMOTED;
    }

    public SubjectRef subjectRef() {
        return new SubjectRef(subjectType, subjectId);
    }

    private void requireNew(String action) {
        if (status != ObservationStatus.NEW) {
            throw new IllegalStateException("Only new observations can " + action);
        }
    }

    @PrePersist
    @PreUpdate
    private void validateInvariant() {
        if ((status == ObservationStatus.MATCHED || status == ObservationStatus.PROMOTED) && matchedTag == null) {
            throw new IllegalStateException(status + " observation requires a matched tag");
        }
        if ((status == ObservationStatus.NEW || status == ObservationStatus.IGNORED) && matchedTag != null) {
            throw new IllegalStateException(status + " observation must not have a matched tag");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
