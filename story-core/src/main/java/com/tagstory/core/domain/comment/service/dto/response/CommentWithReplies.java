package com.tagstory.core.domain.comment.service.dto.response;

import com.tagstory.core.domain.comment.CommentStatus;
import com.tagstory.core.domain.comment.service.dto.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class CommentWithReplies {
    private Comment comment;
    private List<Comment> children;

    public static CommentWithReplies of(Comment comment, List<Comment> children) {
        children.forEach(child -> {
            System.out.println("///" + child.getStatus() + "/////" + " " + child.getContent());
        });

        children = children.stream()
                .filter(child -> CommentStatus.POST == child.getStatus())
                .collect(Collectors.toList());

        return CommentWithReplies.builder()
                .comment(comment)
                .children(children)
                .build();
    }
}
