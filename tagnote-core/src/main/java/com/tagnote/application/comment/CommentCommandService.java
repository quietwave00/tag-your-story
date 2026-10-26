package com.tagnote.application.comment;

import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.board.service.BoardService;
import com.tagnote.core.domain.comment.service.Comment;
import com.tagnote.core.domain.comment.service.CommentService;
import com.tagnote.core.domain.comment.service.dto.command.CreateCommentCommand;
import com.tagnote.core.domain.comment.service.dto.command.CreateReplyCommand;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentCommandService {
    private final BoardService boardService;
    private final UserService userService;
    private final CommentService commentService;

    public Comment create(CreateCommentCommand command) {
        Board board = boardService.getBoardByBoardId(command.getBoardId());
        User user = userService.getCacheByUserId(command.getUserId());
        return commentService.create(board, user, command);
    }

    public Comment createReply(CreateReplyCommand command) {
        Board board = boardService.getBoardByBoardId(command.getBoardId());
        User user = userService.getCacheByUserId(command.getUserId());
        return commentService.createReply(board, user, command);
    }
}
