package com.tagstory.core.domain.comment.service.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.comment.CommentEntity;
import com.tagstory.core.domain.comment.CommentStatus;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    private Long commentId;

    private User user;

    private String content;

    private Comment parent;

    private List<Comment> children = new ArrayList<>();

    private CommentStatus status;

    @JsonIgnore
    private Board board;

    private LocalDateTime createdAt;

    /*
     * 형변환
     */
    public CommentEntity toEntity() {
        CommentEntity.CommentEntityBuilder commentEntityBuilder = CommentEntity.builder()
                .commentId(this.getCommentId())
                .userEntity(this.getUser().toEntity())
                .content(this.getContent())
                .status(this.getStatus())
                .boardEntity(this.getBoard().toEntity());

        if (Objects.nonNull(this.getParent())) {
            commentEntityBuilder.parent(this.getParent().toEntity());
        }

        if (Objects.nonNull(this.getChildren())) {
            commentEntityBuilder.children(this.getChildren().stream().map(Comment::toEntity).collect(Collectors.toList()));
        }
        return commentEntityBuilder.build();
    }

    /*
     * 객체 생성 메소드
     */
    public static Comment createReply(CommentEntity entity) {
        return builder()
                .commentId(entity.getCommentId())
                .user(entity.getUserEntity().toUser())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
