package com.tagnote.domain.board.service;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.dto.command.CreateBoardCommand;
import com.tagnote.core.domain.board.dto.command.UpdateBoardCommand;
import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.board.service.BoardService;
import com.tagnote.core.domain.board.service.BoardWriteService;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.service.BoardUserTagService;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.domain.user.service.UserService;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.usertag.service.UserTagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardWriteServiceTest {

    @Mock
    private BoardService boardService;

    @Mock
    private UserService userService;

    @Mock
    private UserTagService userTagService;

    @Mock
    private BoardUserTagService boardUserTagService;

    @InjectMocks
    private BoardWriteService boardWriteService;

    @Test
    void create는_인증_user를_owner와_writer로_사용한다() {
        CreateBoardCommand command = CreateBoardCommand.builder()
                .content("content")
                .trackId("track-1")
                .userTagList(List.of("Jazz"))
                .userId(1L)
                .build();
        User user = User.builder().userId(1L).build();
        Board expected = Board.builder().boardId("board-1").build();
        when(userService.getCacheByUserId(1L)).thenReturn(user);
        when(userTagService.makeUserTagList(any(UserEntity.class), same(command.getUserTagList())))
                .thenAnswer(invocation -> {
                    UserEntity owner = invocation.getArgument(0);
                    return List.of(UserTagEntity.create(owner, "Jazz", "jazz"));
                });
        when(boardUserTagService.makeBoardUserTagList(any(BoardEntity.class), any()))
                .thenAnswer(invocation -> {
                    BoardEntity board = invocation.getArgument(0);
                    List<UserTagEntity> tags = invocation.getArgument(1);
                    return List.of(BoardUserTagEntity.of(board, tags.get(0)));
                });
        when(boardService.create(any(BoardEntity.class), any())).thenReturn(expected);

        assertThat(boardWriteService.create(command)).isSameAs(expected);
        verify(userTagService).makeUserTagList(
                org.mockito.ArgumentMatchers.argThat(owner -> owner.getUserId().equals(1L)),
                same(command.getUserTagList())
        );
    }

    @Test
    void update는_Board의_원작성자를_UserTag_owner로_사용한다() {
        UpdateBoardCommand command = UpdateBoardCommand.builder()
                .boardId("board-1")
                .content("updated")
                .userTagList(List.of("Jazz"))
                .build();
        UserEntity writer = UserEntity.builder().userId(7L).build();
        BoardEntity board = BoardEntity.create("content", "track-1");
        board.addUser(writer);
        List<UserTagEntity> tags = List.of(UserTagEntity.create(writer, "Jazz", "jazz"));
        List<BoardUserTagEntity> joins = List.of(BoardUserTagEntity.of(board, tags.get(0)));
        Board expected = Board.builder().boardId("board-1").build();
        when(boardService.getBoardEntityByBoardId("board-1")).thenReturn(board);
        when(userTagService.makeUserTagList(writer, command.getUserTagList())).thenReturn(tags);
        when(boardUserTagService.makeBoardUserTagList(board, tags)).thenReturn(joins);
        when(boardService.updateBoardWithUserTag(command, board, joins)).thenReturn(expected);

        assertThat(boardWriteService.updateBoardAndUserTag(command)).isSameAs(expected);
        verify(userTagService).makeUserTagList(writer, command.getUserTagList());
        verify(boardUserTagService).deleteUserTag("board-1");
    }

    @Test
    void update의_태그목록이_비어있으면_기존태그를_유지한다() {
        UpdateBoardCommand command = UpdateBoardCommand.builder()
                .boardId("board-1")
                .content("updated")
                .userTagList(List.of())
                .build();
        BoardEntity board = BoardEntity.create("content", "track-1");
        Board expected = Board.builder().boardId("board-1").build();
        when(boardService.getBoardEntityByBoardId("board-1")).thenReturn(board);
        when(boardService.updateBoard(command, board)).thenReturn(expected);

        assertThat(boardWriteService.updateBoardAndUserTag(command)).isSameAs(expected);
        verify(userTagService, never()).makeUserTagList(any(), any());
        verify(boardUserTagService, never()).deleteUserTag(any());
    }
}
