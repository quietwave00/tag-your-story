package com.tagnote.domain.taxonomy.tag;

import com.tagnote.core.domain.BaseTime;
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
        name = "tag",
        uniqueConstraints = @UniqueConstraint(name = "uk_tag_slug", columnNames = "slug"),
        indexes = {
                @Index(name = "idx_tag_type_status", columnList = "type, status"),
                @Index(name = "idx_tag_name", columnList = "name")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tagId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_into_tag_id")
    private TagEntity mergedIntoTag;

    private String description;

    private TagEntity(String name, String slug, TagType type, TagStatus status, String description) {
        if (status == TagStatus.MERGED) {
            throw new IllegalArgumentException("Merged tag requires a merge target");
        }
        this.name = requireText(name, "Tag name");
        this.slug = requireText(slug, "Tag slug");
        this.type = Objects.requireNonNull(type, "Tag type must not be null");
        this.status = Objects.requireNonNull(status, "Tag status must not be null");
        this.description = description;
    }

    public static TagEntity create(String name, String slug, TagType type, TagStatus status, String description) {
        return new TagEntity(name, slug, type, status, description);
    }

    public void changeStatus(TagStatus status) {
        Objects.requireNonNull(status, "Tag status must not be null");
        if (status == TagStatus.MERGED) {
            throw new IllegalArgumentException("Use mergeInto to merge a tag");
        }
        this.status = status;
        this.mergedIntoTag = null;
    }

    public void mergeInto(TagEntity target) {
        Objects.requireNonNull(target, "Merge target must not be null");
        if (this == target || (tagId != null && tagId.equals(target.tagId))) {
            throw new IllegalArgumentException("Tag cannot be merged into itself");
        }
        this.status = TagStatus.MERGED;
        this.mergedIntoTag = target;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
