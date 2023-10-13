package com.tagstory.core.domain.comment;

import com.tagstory.core.domain.BaseTime;
import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.comment.service.dto.Comment;
import com.tagstory.core.domain.comment.service.dto.command.CreateCommentCommand;
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

    @OneToMany(mappedBy = "parent")
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
    public static CommentEntity create(CreateCommentCommand command) {
        return CommentEntity.builder()
                .content(command.getContent())
                .status(CommentStatus.POST)
                .build();
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
            commentBuilder.parent(this.getParent().toComment());
        }

        if (Objects.nonNull(this.getParent())) {
            commentBuilder.children(this.getChildren().stream().map(CommentEntity::toComment).collect(Collectors.toList()));
        }
        return commentBuilder.build();
    }
}
