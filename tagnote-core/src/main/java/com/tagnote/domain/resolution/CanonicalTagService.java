package com.tagnote.domain.resolution;

import com.tagnote.domain.taxonomy.tag.TagStatus;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;

@Component
public class CanonicalTagService {

    public OptionalLong findActiveCanonicalTag(long tagId, Map<Long, CanonicalTagNode> taxonomy) {
        Objects.requireNonNull(taxonomy, "Taxonomy must not be null");
        Set<Long> visited = new HashSet<>();
        long currentId = tagId;

        while (true) {
            if (!visited.add(currentId)) {
                throw new IllegalStateException("Canonical tag cycle detected at tag: " + currentId);
            }
            CanonicalTagNode current = taxonomy.get(currentId);
            if (current == null) {
                return OptionalLong.empty();
            }
            if (current.status() == TagStatus.ACTIVE) {
                return OptionalLong.of(current.tagId());
            }
            if (current.status() != TagStatus.MERGED || current.mergedIntoTagId() == null) {
                return OptionalLong.empty();
            }
            currentId = current.mergedIntoTagId();
        }
    }
}
