package com.tagnote.domain.comment.fixture;

import com.tagnote.core.domain.comment.CommentEntity;

public class CommentFixture {
    public static CommentEntity createCommentEntity(Long commentId, String content) {
        return CommentEntity.builder()
                .commentId(commentId)
                .content(content)
                .build();
    }
}
