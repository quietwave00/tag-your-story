package com.tagstory.core.domain.comment.service.dto.response;

import com.tagstory.core.domain.comment.CommentStatus;
import com.tagstory.core.domain.comment.service.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CommentWithReplies {
    private Comment comment;
    private List<Comment> children;

    public static CommentWithReplies of(Comment comment, List<Comment> children) {
        return CommentWithReplies.builder()
                .comment(comment)
                .children(children)
                .build();
    }
}
