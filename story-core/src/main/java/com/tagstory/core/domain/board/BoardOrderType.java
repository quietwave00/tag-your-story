package com.tagstory.core.domain.board;

public enum BoardOrderType {
    CREATED_AT, LIKE;

    public boolean isCreatedAt() {
        return this == CREATED_AT;
    }
}
