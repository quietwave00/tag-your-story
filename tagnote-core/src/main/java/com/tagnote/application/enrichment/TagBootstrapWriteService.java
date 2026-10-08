package com.tagnote.application.enrichment;

import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.domain.taxonomy.tag.TagType;
import com.tagnote.infrastructure.persistence.taxonomy.TagJpaRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagBootstrapWriteService {
    private final TagJpaRepository tags;

    @Transactional
    public List<TagEntity> create(List<TagBootstrapService.NameCandidate> candidates) {
        List<TagEntity> created = new ArrayList<>();
        for (TagBootstrapService.NameCandidate candidate : candidates) {
            String slug = "auto-" + UUID.nameUUIDFromBytes(
                    candidate.normalizedName().getBytes(StandardCharsets.UTF_8));
            try {
                created.add(tags.saveAndFlush(TagEntity.create(
                        candidate.displayName(), slug, TagType.UNCLASSIFIED, TagStatus.ACTIVE, null)));
            } catch (DataIntegrityViolationException failure) {
                if (isKnownDuplicate(failure)) {
                    throw new TagDuplicateException(failure);
                }
                throw failure;
            }
        }
        return List.copyOf(created);
    }

    private boolean isKnownDuplicate(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String name = violation.getConstraintName();
                return name != null && (name.toLowerCase(Locale.ROOT).contains("uk_tag_normalized_name")
                        || name.toLowerCase(Locale.ROOT).contains("uk_tag_slug"));
            }
        }
        return false;
    }

    public static final class TagDuplicateException extends RuntimeException {
        public TagDuplicateException(Throwable cause) { super(cause); }
    }
}
