package com.tagstory.api.domain.comment.dto.response;

import com.tagstory.core.domain.comment.service.Comment;
import com.tagstory.core.domain.comment.service.dto.response.CommentWithReplies;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CommentWithRepliesResponse {
    private Comment comment;
    private List<Comment> children;

    public static CommentWithRepliesResponse from(CommentWithReplies commentWithReplies) {
        return CommentWithRepliesResponse.builder()
                .comment(commentWithReplies.getComment())
                .children(commentWithReplies.getChildren())
                .build();
    }
}
