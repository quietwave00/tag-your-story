package com.tagnote.domain.enrichment.assertion;

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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(
        name = "tag_assertion",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tag_assertion_identity",
                columnNames = {"subject_type", "subject_id", "tag_id", "source", "evidence_type"}
        ),
        indexes = {
                @Index(name = "idx_tag_assertion_subject_status", columnList = "subject_type, subject_id, status"),
                @Index(name = "idx_tag_assertion_tag", columnList = "tag_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagAssertionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assertionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false)
    private SubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private TagEntity tag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssertionSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false)
    private EvidenceType evidenceType;

    @Column(nullable = false)
    private double confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssertionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inherited_from_assertion_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TagAssertionEntity inheritedFromAssertion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private TagAssertionEntity(
            SubjectRef subject,
            TagEntity tag,
            AssertionSource source,
            EvidenceType evidenceType,
            double confidence,
            AssertionStatus status
    ) {
        SubjectRef requiredSubject = Objects.requireNonNull(subject, "Assertion subject must not be null");
        this.subjectType = requiredSubject.type();
        this.subjectId = requiredSubject.subjectId();
        this.tag = Objects.requireNonNull(tag, "Assertion tag must not be null");
        this.source = Objects.requireNonNull(source, "Assertion source must not be null");
        this.evidenceType = Objects.requireNonNull(evidenceType, "Evidence type must not be null");
        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
        this.confidence = confidence;
        this.status = Objects.requireNonNull(status, "Assertion status must not be null");
        this.createdAt = LocalDateTime.now();
    }

    public static TagAssertionEntity create(
            SubjectRef subject,
            TagEntity tag,
            AssertionSource source,
            EvidenceType evidenceType,
            double confidence,
            AssertionStatus status
    ) {
        return new TagAssertionEntity(subject, tag, source, evidenceType, confidence, status);
    }

    public static TagAssertionEntity createApproved(
            SubjectRef subject,
            TagEntity tag,
            AssertionSource source,
            EvidenceType evidenceType,
            double confidence
    ) {
        return create(subject, tag, source, evidenceType, confidence, AssertionStatus.APPROVED);
    }

    public static TagAssertionEntity createInheritedApproved(
            SubjectRef subject,
            TagAssertionEntity parentAssertion,
            double confidence
    ) {
        TagAssertionEntity requiredParent = Objects.requireNonNull(
                parentAssertion, "Parent assertion must not be null"
        );
        if (requiredParent.getStatus() != AssertionStatus.APPROVED
                || requiredParent.getInheritedFromAssertion() != null) {
            throw new IllegalArgumentException("Only approved direct assertions can be inherited");
        }
        TagAssertionEntity inherited = createApproved(
                subject,
                requiredParent.getTag(),
                requiredParent.getSource(),
                requiredParent.getEvidenceType(),
                confidence
        );
        inherited.inheritedFromAssertion = requiredParent;
        return inherited;
    }

    public void updateInherited(TagAssertionEntity parentAssertion, double confidence) {
        TagAssertionEntity requiredParent = Objects.requireNonNull(
                parentAssertion, "Parent assertion must not be null"
        );
        if (inheritedFromAssertion == null) {
            throw new IllegalStateException("Only inherited assertions can be updated as inherited");
        }
        if (requiredParent.getStatus() != AssertionStatus.APPROVED
                || requiredParent.getInheritedFromAssertion() != null) {
            throw new IllegalArgumentException("Only approved direct assertions can be inherited");
        }
        if (!Objects.equals(tag.getTagId(), requiredParent.getTag().getTagId())
                || source != requiredParent.getSource()
                || evidenceType != requiredParent.getEvidenceType()) {
            throw new IllegalArgumentException("Inherited assertion identity must match parent assertion");
        }
        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
        this.confidence = confidence;
        this.status = AssertionStatus.APPROVED;
        this.inheritedFromAssertion = requiredParent;
    }

    public void approve() {
        transitionFromPendingTo(AssertionStatus.APPROVED);
    }

    public void reject() {
        transitionFromPendingTo(AssertionStatus.REJECTED);
    }

    public SubjectRef subjectRef() {
        return new SubjectRef(subjectType, subjectId);
    }

    private void transitionFromPendingTo(AssertionStatus target) {
        if (status != AssertionStatus.PENDING) {
            throw new IllegalStateException("Only pending assertions can change review status");
        }
        status = target;
    }
}
