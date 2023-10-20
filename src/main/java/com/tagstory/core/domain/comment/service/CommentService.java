package com.tagstory.core.domain.comment.service;

import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionCode;
import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.comment.CommentEntity;
import com.tagstory.core.domain.comment.CommentStatus;
import com.tagstory.core.domain.comment.repository.CommentRepository;
import com.tagstory.core.domain.comment.service.dto.Comment;
import com.tagstory.core.domain.comment.service.dto.command.CreateCommentCommand;
import com.tagstory.core.domain.comment.service.dto.command.CreateReplyCommand;
import com.tagstory.core.domain.comment.service.dto.command.UpdateCommentCommand;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;

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
            throw new RuntimeException("An exception occurred While deleting the comment.");
        }
    }

    public List<Comment> getCommentList(String boardId) {
        return getCommentListByBoardId(boardId);
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

    private List<Comment> getCommentListByBoardId(String boardId) {
        return commentRepository.findByStatusAndBoardEntity_BoardIdOrderByCommentIdDesc(CommentStatus.POST, boardId)
                .stream().map(CommentEntity::toComment).collect(Collectors.toList());
    }

    private List<Comment> getCommentListByBoardIdAndUserId(String boardId, Long userId) {
        return commentRepository.findByBoardEntity_BoardIdAndUserEntity_UserId(boardId, userId)
                .stream().map(CommentEntity::toComment).collect(Collectors.toList());
    }


}
