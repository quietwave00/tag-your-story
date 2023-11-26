package com.tagstory.domain.comment.fixture;

import com.tagstory.core.domain.comment.CommentEntity;

public class CommentFixture {
    public static CommentEntity createCommentEntity(Long commentId, String content) {
        return CommentEntity.builder()
                .commentId(commentId)
                .content(content)
                .build();
    }
}
