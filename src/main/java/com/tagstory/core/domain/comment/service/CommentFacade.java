package com.tagstory.core.domain.comment.service;

import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.board.service.BoardService;
import com.tagstory.core.domain.comment.service.dto.Comment;
import com.tagstory.core.domain.comment.service.dto.command.CreateCommentCommand;
import com.tagstory.core.domain.comment.service.dto.command.CreateReplyCommand;
import com.tagstory.core.domain.comment.service.dto.command.UpdateCommentCommand;
import com.tagstory.core.domain.user.service.UserService;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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

    public Comment createReply(CreateReplyCommand command) {
        Board board = boardService.getBoardByBoardId(command.getBoardId());
        User user = userService.getCacheByUserId(command.getUserId());
        return commentService.createReply(board, user, command);
    }
}
