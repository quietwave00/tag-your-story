package com.tagnote.domain.taxonomy.alias;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@Table(
        name = "tag_alias",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tag_alias_tag_normalized",
                columnNames = {"tag_id", "normalized_alias"}
        ),
        indexes = @Index(name = "idx_tag_alias_normalized_status", columnList = "normalized_alias, status")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagAliasEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long aliasId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private TagEntity tag;

    @Column(nullable = false)
    private String alias;

    @Column(name = "normalized_alias", nullable = false)
    private String normalizedAlias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AliasSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AliasStatus status;

    private TagAliasEntity(
            TagEntity tag,
            String alias,
            NormalizedTagName normalizedAlias,
            AliasSource source,
            AliasStatus status
    ) {
        this.tag = Objects.requireNonNull(tag, "Alias tag must not be null");
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("Alias must not be blank");
        }
        this.alias = alias;
        this.normalizedAlias = Objects.requireNonNull(
                normalizedAlias,
                "Normalized alias must not be null"
        ).value();
        this.source = Objects.requireNonNull(source, "Alias source must not be null");
        this.status = Objects.requireNonNull(status, "Alias status must not be null");
    }

    public static TagAliasEntity create(
            TagEntity tag,
            String alias,
            NormalizedTagName normalizedAlias,
            AliasSource source,
            AliasStatus status
    ) {
        return new TagAliasEntity(tag, alias, normalizedAlias, source, status);
    }

    public void approve() {
        transitionFromPendingTo(AliasStatus.APPROVED);
    }

    public void reject() {
        transitionFromPendingTo(AliasStatus.REJECTED);
    }

    private void transitionFromPendingTo(AliasStatus target) {
        if (status != AliasStatus.PENDING) {
            throw new IllegalStateException("Only pending aliases can change review status");
        }
        status = target;
    }
}
