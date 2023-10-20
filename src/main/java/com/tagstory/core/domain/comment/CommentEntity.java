package com.tagstory.core.domain.comment;

import com.tagstory.core.domain.BaseTime;
import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.comment.service.dto.Comment;
import com.tagstory.core.domain.user.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "comments")
public class CommentEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CommentEntity parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST)
    private List<CommentEntity> children = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private CommentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;

    @ManyToOne
    @JoinColumn(name = "board_id")
    private BoardEntity boardEntity;

    /*
     * 연관관계 메소드
     */
    public void addBoard(BoardEntity boardEntity) {
        this.boardEntity = boardEntity;
    }

    public void addUser(UserEntity userEntity) {
        this.userEntity = userEntity;
    }

    /*
     * 비즈니스 로직
     */
    public static CommentEntity create(String content) {
        return CommentEntity.builder()
                .content(content)
                .status(CommentStatus.POST)
                .build();
    }

    public CommentEntity update(String content) {
        this.content = content;
        return this;
    }

    public CommentEntity delete() {
        this.status = CommentStatus.REMOVAL;
        return this;
    }

    public void addParent(CommentEntity parentEntity) {
        this.parent = parentEntity;
        parentEntity.getChildren().add(this);
    }

    /*
     * 형변환
     */
    public Comment toComment() {
        Comment.CommentBuilder commentBuilder = Comment.builder()
                .commentId(this.getCommentId())
                .user(this.getUserEntity().toUser())
                .content(this.getContent())
                .status(this.getStatus())
                .board(this.getBoardEntity().toBoard());

        if (Objects.nonNull(this.getParent())) {
            Comment comment = Comment.builder()
                    .commentId(this.getParent().getCommentId())
                    .build();
            commentBuilder.parent(comment);
        }

        if (Objects.nonNull(this.getChildren())) {
            List<Comment> children = this.getChildren().stream()
                    .map(child -> Comment.builder()
                            .commentId(child.getCommentId())
                            .build())
                    .collect(Collectors.toList());
            commentBuilder.children(children);
        }
        return commentBuilder.build();
    }
}
