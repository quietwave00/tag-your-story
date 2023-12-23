package com.tagstory.core.domain.comment.service;

import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.comment.CommentEntity;
import com.tagstory.core.domain.comment.CommentStatus;
import com.tagstory.core.domain.comment.repository.CommentRepository;
import com.tagstory.core.domain.comment.service.dto.Comment;
import com.tagstory.core.domain.comment.service.dto.command.CreateCommentCommand;
import com.tagstory.core.domain.comment.service.dto.command.CreateReplyCommand;
import com.tagstory.core.domain.comment.service.dto.command.UpdateCommentCommand;
import com.tagstory.core.domain.comment.service.dto.response.CommentWithReplies;
import com.tagstory.core.domain.notification.NotificationType;
import com.tagstory.core.domain.notification.service.Notification;
import com.tagstory.core.domain.user.service.dto.response.User;
import com.tagstory.core.exception.CustomException;
import com.tagstory.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CompletableFuture<Comment> createWithNotification(Board board, User user, CreateCommentCommand command) {
        return CompletableFuture.supplyAsync(() -> create(board, user, command))
                .thenApplyAsync(comment -> {
                    onEvent(user, board);
                    return comment;
                });
    }

    @Transactional
    public Comment create(Board board, User user, CreateCommentCommand command) {
        CommentEntity commentEntity = CommentEntity.create(command.getContent());
        commentEntity.addUser(user.toEntity());
        commentEntity.addBoard(board.toEntity());

        return commentRepository.save(commentEntity).toComment();
    }

    @Transactional
    public Comment update(UpdateCommentCommand command) {
        CommentEntity commentEntity = getCommentEntityByCommentId(command.getCommentId());
        return commentEntity.update(command.getContent()).toComment();
    }

    @Transactional
    public void delete(Long commentId) {
        CommentEntity commentEntity = getCommentEntityByCommentId(commentId);
        try {
            commentEntity.delete();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException();
        }
    }

    public List<CommentWithReplies> getCommentList(String boardId, int page) {
        List<Comment> commentList = findCommentListByBoardId(boardId, page);
        return commentList.stream()
                .map(comment -> {
                    return CommentWithReplies.of(comment, comment.getChildren());
                })
                .collect(Collectors.toList());
    }

    public List<Long> getUserCommentId(String boardId, Long userId) {
        return getCommentListByBoardIdAndUserId(boardId, userId)
                .stream().map(Comment::getCommentId).collect(Collectors.toList());
    }


    @Transactional
    public Comment createReply(Board board, User user, CreateReplyCommand command) {
        CommentEntity parentComment = getCommentEntityByCommentId(command.getParentId());
        CommentEntity replyEntity = CommentEntity.create(command.getContent());
        replyEntity.addParent(parentComment);
        replyEntity.addUser(user.toEntity());
        replyEntity.addBoard(board.toEntity());

        return commentRepository.save(replyEntity).toComment();
    }

    public List<Comment> getReplyList(Long parentId, int page) {
        Comment parentComment = getCommentByCommentId(parentId);
        return getReplyListByParentComment(parentComment, page);
    }

    public int getCommentCountByBoardId(String boardId) {
        return commentRepository.countByBoardEntity_BoardIdAndStatus(boardId, CommentStatus.POST);
    }



    /*
     * 단일 메소드
     */
    private Comment getCommentByCommentId(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(
                () -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND)).toComment();
    }

    private CommentEntity getCommentEntityByCommentId(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(
                () -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND));
    }

    private List<Comment> findCommentListByBoardId(String boardId, int page) {
         Page<CommentEntity> commentEntityList = commentRepository
                 .findByStatusAndParentIsNullAndBoardEntity_BoardIdOrderByCommentIdDesc(CommentStatus.POST, boardId, PageRequest.of(page, 20))
                 .orElse(Page.empty());

         return commentEntityList.stream()
                 .map(CommentEntity::toComment)
                 .collect(Collectors.toList());
    }

    private List<Comment> getCommentListByBoardIdAndUserId(String boardId, Long userId) {
        return commentRepository.findByBoardEntity_BoardIdAndUserEntity_UserId(boardId, userId)
                .stream().map(CommentEntity::toComment).collect(Collectors.toList());
    }

    private List<Comment> getReplyListByParentComment(Comment parentComment, int page) {
        Page<CommentEntity> replyList = commentRepository.
                findByParentAndStatus(parentComment.toEntity(), CommentStatus.POST, PageRequest.of(page, 5));

        return replyList.stream()
                .map(CommentEntity::toComment)
                .collect(Collectors.toList());
    }

    private void onEvent(User user, Board board) {
        eventPublisher.publishEvent(Notification.onEvent(user,
                board.getUser(),
                NotificationType.COMMENT,
                board.getBoardId()
        ));
    }
}
