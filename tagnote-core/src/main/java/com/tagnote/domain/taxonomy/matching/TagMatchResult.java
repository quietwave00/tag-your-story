package com.tagnote.domain.taxonomy.matching;

import com.tagnote.domain.taxonomy.tag.TagEntity;

import java.util.Objects;
import java.util.Optional;

public final class TagMatchResult {

    public enum Status {
        MATCHED,
        UNMATCHED,
        AMBIGUOUS
    }

    private final Status status;
    private final TagEntity matchedTag;

    private TagMatchResult(Status status, TagEntity matchedTag) {
        this.status = status;
        this.matchedTag = matchedTag;
    }

    public static TagMatchResult matched(TagEntity tag) {
        return new TagMatchResult(Status.MATCHED, Objects.requireNonNull(tag, "Matched tag must not be null"));
    }

    public static TagMatchResult unmatched() {
        return new TagMatchResult(Status.UNMATCHED, null);
    }

    public static TagMatchResult ambiguous() {
        return new TagMatchResult(Status.AMBIGUOUS, null);
    }

    public Status getStatus() {
        return status;
    }

    public Optional<TagEntity> getMatchedTag() {
        return Optional.ofNullable(matchedTag);
    }

    public boolean isMatched() {
        return status == Status.MATCHED;
    }
}
