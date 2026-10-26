package com.tagnote.application.comment;

import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.board.service.BoardService;
import com.tagnote.core.domain.comment.service.Comment;
import com.tagnote.core.domain.comment.service.CommentService;
import com.tagnote.core.domain.comment.service.dto.command.CreateCommentCommand;
import com.tagnote.core.domain.comment.service.dto.command.CreateReplyCommand;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.domain.user.service.UserService;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentCommandServiceTest {
    @Mock private BoardService boardService;
    @Mock private UserService userService;
    @Mock private CommentService commentService;
    @InjectMocks private CommentCommandService commands;

    @Test
    void 최상위_작성은_Board_User_저장_순서로_조율한다() {
        CreateCommentCommand command = CreateCommentCommand.builder().boardId("board-1").userId(7L).content("내용").build();
        Board board = Board.builder().boardId("board-1").build();
        User user = User.builder().userId(7L).build();
        Comment saved = Comment.builder().commentId(11L).build();
        when(boardService.getBoardByBoardId("board-1")).thenReturn(board);
        when(userService.getCacheByUserId(7L)).thenReturn(user);
        when(commentService.create(board, user, command)).thenReturn(saved);

        assertThat(commands.create(command)).isSameAs(saved);
        InOrder order = inOrder(boardService, userService, commentService);
        order.verify(boardService).getBoardByBoardId("board-1");
        order.verify(userService).getCacheByUserId(7L);
        order.verify(commentService).create(board, user, command);
    }

    @Test
    void 답글_작성도_Board_User_저장_순서로_조율한다() {
        CreateReplyCommand command = CreateReplyCommand.builder().boardId("board-1").userId(7L).parentId(4L).content("답글").build();
        Board board = Board.builder().boardId("board-1").build();
        User user = User.builder().userId(7L).build();
        Comment saved = Comment.builder().commentId(12L).build();
        when(boardService.getBoardByBoardId("board-1")).thenReturn(board);
        when(userService.getCacheByUserId(7L)).thenReturn(user);
        when(commentService.createReply(board, user, command)).thenReturn(saved);

        assertThat(commands.createReply(command)).isSameAs(saved);
        InOrder order = inOrder(boardService, userService, commentService);
        order.verify(boardService).getBoardByBoardId("board-1");
        order.verify(userService).getCacheByUserId(7L);
        order.verify(commentService).createReply(board, user, command);
    }

    @Test
    void Board_조회_실패시_사용자와_저장소를_호출하지_않는다() {
        CreateCommentCommand command = CreateCommentCommand.builder().boardId("missing").userId(7L).build();
        CustomException failure = new CustomException(ExceptionCode.BOARD_NOT_FOUND);
        when(boardService.getBoardByBoardId("missing")).thenThrow(failure);

        assertThatThrownBy(() -> commands.create(command)).isSameAs(failure);
        verify(userService, never()).getCacheByUserId(7L);
        verify(commentService, never()).create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void User_조회_실패시_답글을_저장하지_않는다() {
        CreateReplyCommand command = CreateReplyCommand.builder().boardId("board-1").userId(7L).parentId(4L).build();
        Board board = Board.builder().boardId("board-1").build();
        CustomException failure = new CustomException(ExceptionCode.USER_NOT_FOUND);
        when(boardService.getBoardByBoardId("board-1")).thenReturn(board);
        when(userService.getCacheByUserId(7L)).thenThrow(failure);

        assertThatThrownBy(() -> commands.createReply(command)).isSameAs(failure);
        verify(commentService, never()).createReply(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
