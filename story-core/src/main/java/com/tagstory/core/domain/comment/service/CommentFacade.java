package com.tagstory.core.domain.comment.service;

import com.tagstory.core.domain.board.service.Board;
import com.tagstory.core.domain.board.service.BoardService;
import com.tagstory.core.domain.comment.service.dto.command.CreateCommentCommand;
import com.tagstory.core.domain.comment.service.dto.command.CreateReplyCommand;
import com.tagstory.core.domain.comment.service.dto.command.UpdateCommentCommand;
import com.tagstory.core.domain.comment.service.dto.response.CommentWithReplies;
import com.tagstory.core.domain.user.service.User;
import com.tagstory.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CommentFacade {
    private final BoardService boardService;
    private final UserService userService;
    private final CommentService commentService;

    public Comment create(CreateCommentCommand command) {
        Board board = boardService.getBoardByBoardId(command.getBoardId());
        User user = userService.getCacheByUserId(command.getUserId());
        return commentService.create(board, user, command);
    }

    public Comment update(UpdateCommentCommand command) {
        return commentService.update(command);
    }

    public void delete(Long commentId) {
        commentService.delete(commentId);
    }

    public List<CommentWithReplies> getCommentList(String boardId, int page) {
        return commentService.getCommentList(boardId, page);
    }

    public List<Long> getUserCommentId(String boardId, Long userId) {
        return commentService.getUserCommentId(boardId, userId);
    }

    public Comment createReply(CreateReplyCommand command) {
        Board board = boardService.getBoardByBoardId(command.getBoardId());
        User user = userService.getCacheByUserId(command.getUserId());
        return commentService.createReply(board, user, command);
    }

    public int getCommentCountByBoardId(String boardId) {
        return commentService.getCommentCountByBoardId(boardId);
    }

    public List<Comment> getReplyList(Long parentId, int page) {
        return commentService.getReplyList(parentId, page);
    }
}
