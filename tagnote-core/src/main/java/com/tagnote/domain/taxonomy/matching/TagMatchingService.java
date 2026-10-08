package com.tagnote.domain.taxonomy.matching;

import com.tagnote.domain.taxonomy.alias.AliasStatus;
import com.tagnote.domain.taxonomy.alias.TagAliasEntity;
import com.tagnote.domain.taxonomy.tag.TagEntity;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class TagMatchingService {

    public TagMatchResult match(NormalizedTagName normalizedName, Collection<TagAliasEntity> candidates) {
        Objects.requireNonNull(normalizedName, "Normalized tag name must not be null");
        Objects.requireNonNull(candidates, "Alias candidates must not be null");

        List<TagEntity> matchedTags = new ArrayList<>();
        for (TagAliasEntity candidate : candidates) {
            if (candidate != null
                    && candidate.getStatus() == AliasStatus.APPROVED
                    && candidate.getNormalizedAlias().equals(normalizedName.value())) {
                TagEntity candidateTag = candidate.getTag();
                boolean alreadyMatched = matchedTags.stream()
                        .anyMatch(tag -> representsSameTag(tag, candidateTag));
                if (!alreadyMatched) {
                    matchedTags.add(candidateTag);
                }
            }
        }

        if (matchedTags.isEmpty()) {
            return TagMatchResult.unmatched();
        }
        if (matchedTags.size() > 1) {
            return TagMatchResult.ambiguous();
        }
        return TagMatchResult.matched(matchedTags.iterator().next());
    }

    private boolean representsSameTag(TagEntity left, TagEntity right) {
        return left == right
                || (left.getTagId() != null && left.getTagId().equals(right.getTagId()));
    }
}
