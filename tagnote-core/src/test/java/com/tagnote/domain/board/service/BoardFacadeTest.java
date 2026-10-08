package com.tagnote.domain.board.service;

import com.tagnote.core.domain.board.BoardOrderType;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.board.dto.command.CreateBoardCommand;
import com.tagnote.core.domain.board.dto.command.UpdateBoardCommand;
import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.board.service.BoardFacade;
import com.tagnote.core.domain.board.service.BoardService;
import com.tagnote.core.domain.board.service.BoardWriteService;
import com.tagnote.core.domain.board.service.dto.BoardList;
import com.tagnote.core.domain.boardusertag.service.BoardUserTag;
import com.tagnote.core.domain.boardusertag.service.BoardUserTagService;
import com.tagnote.core.domain.boardusertag.service.dto.UserTagNames;
import com.tagnote.core.domain.usertag.service.UserTag;
import com.tagnote.core.domain.user.service.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardFacadeTest {

    @Mock
    private BoardService boardService;

    @Mock
    private BoardUserTagService boardUserTagService;

    @Mock
    private BoardWriteService boardWriteService;

    @InjectMocks
    private BoardFacade boardFacade;

    @Test
    void create는_write_transaction에_위임한다() {
        CreateBoardCommand command = CreateBoardCommand.builder()
                .content("content")
                .trackId("track-1")
                .userTagList(List.of("tag-1"))
                .userId(1L)
                .build();
        Board savedBoard = board("board-1");

        when(boardWriteService.create(command)).thenReturn(savedBoard);

        Board result = boardFacade.create(command);

        assertThat(result).isSameAs(savedBoard);
        verify(boardWriteService).create(command);
    }

    @Test
    void getBoardListByTrackId는_CREATED_AT일때_createdAt_path를_탄다() {
        Board board = boardWithTags("board-1", List.of("tag-1", "tag-2"));
        BoardList source = BoardList.of(List.of(board), 1L);
        BoardList merged = BoardList.of(List.of(board), 1L);

        when(boardService.getBoardListByTrackIdSortedCreatedAt(BoardStatus.POST, "track-1", 0)).thenReturn(source);
        when(boardService.getBoardListByTrackId(same(source), argThat(userTagNamesList ->
                userTagNamesList.size() == 1
                        && userTagNamesList.get(0).getNameList().equals(List.of("tag-1", "tag-2"))
        ))).thenReturn(merged);

        BoardList result = boardFacade.getBoardListByTrackId("track-1", BoardOrderType.CREATED_AT, 0);

        assertThat(result).isSameAs(merged);
        verify(boardService).getBoardListByTrackIdSortedCreatedAt(BoardStatus.POST, "track-1", 0);
        verify(boardService, never()).getBoardListByTrackIdSortedLike(any(), any(), anyInt());
    }

    @Test
    void getBoardListByTrackId는_LIKE일때_like_path를_탄다() {
        Board board = boardWithTags("board-1", List.of("tag-1"));
        BoardList source = BoardList.of(List.of(board), 1L);
        BoardList merged = BoardList.of(List.of(board), 1L);

        when(boardService.getBoardListByTrackIdSortedLike(BoardStatus.POST, "track-1", 1)).thenReturn(source);
        when(boardService.getBoardListByTrackId(same(source), argThat(userTagNamesList ->
                userTagNamesList.size() == 1
                        && userTagNamesList.get(0).getNameList().equals(List.of("tag-1"))
        ))).thenReturn(merged);

        BoardList result = boardFacade.getBoardListByTrackId("track-1", BoardOrderType.LIKE, 1);

        assertThat(result).isSameAs(merged);
        verify(boardService).getBoardListByTrackIdSortedLike(BoardStatus.POST, "track-1", 1);
        verify(boardService, never()).getBoardListByTrackIdSortedCreatedAt(any(), any(), anyInt());
    }

    @Test
    void getDetailBoard는_태그명_조회와_게시글_조회를_조합한다() {
        UserTagNames userTagNames = UserTagNames.ofNameList(List.of("tag-1"));
        Board board = board("board-1");
        when(boardUserTagService.getUserTagNameByBoardId("board-1")).thenReturn(userTagNames);
        when(boardService.getDetailBoard("board-1", userTagNames)).thenReturn(board);

        Board result = boardFacade.getDetailBoard("board-1");

        assertThat(result).isSameAs(board);
        verify(boardUserTagService).getUserTagNameByBoardId("board-1");
        verify(boardService).getDetailBoard("board-1", userTagNames);
    }

    @Test
    void updateBoardAndUserTag는_태그가_있으면_기존태그를_삭제하고_재구성한다() {
        UpdateBoardCommand command = UpdateBoardCommand.builder()
                .boardId("board-1")
                .content("updated")
                .userTagList(List.of("tag-1"))
                .build();
        Board updated = board("board-1");

        when(boardWriteService.updateBoardAndUserTag(command)).thenReturn(updated);

        Board result = boardFacade.updateBoardAndUserTag(command);

        assertThat(result).isSameAs(updated);
        verify(boardWriteService).updateBoardAndUserTag(command);
    }

    @Test
    void updateBoardAndUserTag는_태그가_비어있으면_내용만_수정한다() {
        UpdateBoardCommand command = UpdateBoardCommand.builder()
                .boardId("board-1")
                .content("updated")
                .userTagList(List.of())
                .build();
        Board updated = board("board-1");

        when(boardWriteService.updateBoardAndUserTag(command)).thenReturn(updated);

        Board result = boardFacade.updateBoardAndUserTag(command);

        assertThat(result).isSameAs(updated);
        verify(boardWriteService).updateBoardAndUserTag(command);
    }

    @Test
    void getBoardListByUserTagName는_각_board마다_태그명을_추가한다() {
        Board first = board("board-1");
        Board second = board("board-2");
        UserTagNames firstTags = UserTagNames.ofNameList(List.of("tag-1"));
        UserTagNames secondTags = UserTagNames.ofNameList(List.of("tag-2"));

        first.addUserTagList(firstTags);
        second.addUserTagList(secondTags);
        when(boardService.getBoardListByUserTagName("genre")).thenReturn(List.of(first, second));

        List<Board> result = boardFacade.getBoardListByUserTagName("genre");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserTagNameList().getNameList()).containsExactly("tag-1");
        assertThat(result.get(1).getUserTagNameList().getNameList()).containsExactly("tag-2");
        verify(boardService).getBoardListByUserTagName("genre");
    }

    private Board board(String boardId) {
        return Board.builder()
                .boardId(boardId)
                .user(User.builder().userId(1L).nickname("nickname").build())
                .build();
    }

    private Board boardWithTags(String boardId, List<String> tagNames) {
        return Board.builder()
                .boardId(boardId)
                .user(User.builder().userId(1L).nickname("nickname").build())
                .boardUserTagList(tagNames.stream()
                        .map(tagName -> BoardUserTag.builder()
                                .userTag(UserTag.builder().name(tagName).build())
                                .build())
                        .toList())
                .build();
    }
}
