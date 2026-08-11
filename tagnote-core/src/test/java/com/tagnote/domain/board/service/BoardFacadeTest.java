package com.tagnote.domain.board.service;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardOrderType;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.board.dto.command.CreateBoardCommand;
import com.tagnote.core.domain.board.dto.command.UpdateBoardCommand;
import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.board.service.BoardFacade;
import com.tagnote.core.domain.board.service.BoardService;
import com.tagnote.core.domain.board.service.dto.BoardList;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.service.BoardUserTag;
import com.tagnote.core.domain.boardusertag.service.BoardUserTagService;
import com.tagnote.core.domain.boardusertag.service.dto.UserTagNames;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.usertag.service.UserTag;
import com.tagnote.core.domain.usertag.service.UserTagService;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.domain.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardFacadeTest {

    @Mock
    private BoardService boardService;

    @Mock
    private UserService userService;

    @Mock
    private UserTagService userTagService;

    @Mock
    private BoardUserTagService boardUserTagService;

    @InjectMocks
    private BoardFacade boardFacade;

    @Test
    void create는_user_userTag_joinEntity를_조합한_뒤_boardService에_위임한다() {
        CreateBoardCommand command = CreateBoardCommand.builder()
                .content("content")
                .trackId("track-1")
                .userTagList(List.of("tag-1"))
                .userId(1L)
                .build();
        User user = User.builder().userId(1L).nickname("nickname").build();
        List<UserTagEntity> userTagEntityList = List.of(UserTagEntity.builder().userTagId(1L).name("tag-1").build());
        List<BoardUserTagEntity> boardUserTagEntityList = List.of(BoardUserTagEntity.builder().boardUserTagId(1L).build());
        Board savedBoard = board("board-1");

        when(userService.getCacheByUserId(1L)).thenReturn(user);
        when(userTagService.makeUserTagList(command.getUserTagList())).thenReturn(userTagEntityList);
        when(boardUserTagService.makeBoardUserTagList(any(BoardEntity.class), same(userTagEntityList))).thenReturn(boardUserTagEntityList);
        when(boardService.create(any(BoardEntity.class), same(user), same(boardUserTagEntityList), same(command))).thenReturn(savedBoard);

        Board result = boardFacade.create(command);

        assertThat(result).isSameAs(savedBoard);
        InOrder inOrder = inOrder(userService, userTagService, boardUserTagService, boardService);
        inOrder.verify(userService).getCacheByUserId(1L);
        inOrder.verify(userTagService).makeUserTagList(command.getUserTagList());
        inOrder.verify(boardUserTagService).makeBoardUserTagList(any(BoardEntity.class), same(userTagEntityList));
        inOrder.verify(boardService).create(any(BoardEntity.class), same(user), same(boardUserTagEntityList), same(command));
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
        BoardEntity boardEntity = BoardEntity.create("content", "track-1");
        List<UserTagEntity> userTagEntityList = List.of(UserTagEntity.builder().userTagId(1L).name("tag-1").build());
        List<BoardUserTagEntity> boardUserTagEntityList = List.of(BoardUserTagEntity.builder().boardUserTagId(1L).build());
        Board updated = board("board-1");

        when(boardService.getBoardEntityByBoardId("board-1")).thenReturn(boardEntity);
        when(userTagService.makeUserTagList(command.getUserTagList())).thenReturn(userTagEntityList);
        when(boardUserTagService.makeBoardUserTagList(boardEntity, userTagEntityList)).thenReturn(boardUserTagEntityList);
        when(boardService.updateBoardWithUserTag(command, boardEntity, boardUserTagEntityList)).thenReturn(updated);

        Board result = boardFacade.updateBoardAndUserTag(command);

        assertThat(result).isSameAs(updated);
        InOrder inOrder = inOrder(boardService, boardUserTagService, userTagService);
        inOrder.verify(boardService).getBoardEntityByBoardId("board-1");
        inOrder.verify(boardUserTagService).deleteUserTag("board-1");
        inOrder.verify(userTagService).makeUserTagList(command.getUserTagList());
        inOrder.verify(boardUserTagService).makeBoardUserTagList(boardEntity, userTagEntityList);
        verify(boardService).updateBoardWithUserTag(command, boardEntity, boardUserTagEntityList);
    }

    @Test
    void updateBoardAndUserTag는_태그가_비어있으면_내용만_수정한다() {
        UpdateBoardCommand command = UpdateBoardCommand.builder()
                .boardId("board-1")
                .content("updated")
                .userTagList(List.of())
                .build();
        BoardEntity boardEntity = BoardEntity.create("content", "track-1");
        Board updated = board("board-1");

        when(boardService.getBoardEntityByBoardId("board-1")).thenReturn(boardEntity);
        when(boardService.updateBoard(command, boardEntity)).thenReturn(updated);

        Board result = boardFacade.updateBoardAndUserTag(command);

        assertThat(result).isSameAs(updated);
        verify(boardUserTagService, never()).deleteUserTag(any());
        verify(userTagService, never()).makeUserTagList(any());
        verify(boardService).updateBoard(command, boardEntity);
    }

    @Test
    void getBoardListByUserTagName는_각_board마다_태그명을_추가한다() {
        Board first = board("board-1");
        Board second = board("board-2");
        UserTagNames firstTags = UserTagNames.ofNameList(List.of("tag-1"));
        UserTagNames secondTags = UserTagNames.ofNameList(List.of("tag-2"));

        when(userTagService.getUserTagIdByUserTagName("genre")).thenReturn(3L);
        when(boardService.getBoardListByUserTagId(3L)).thenReturn(List.of(first, second));
        when(boardUserTagService.getUserTagNameByBoardId("board-1")).thenReturn(firstTags);
        when(boardUserTagService.getUserTagNameByBoardId("board-2")).thenReturn(secondTags);

        List<Board> result = boardFacade.getBoardListByUserTagName("genre");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserTagNameList().getNameList()).containsExactly("tag-1");
        assertThat(result.get(1).getUserTagNameList().getNameList()).containsExactly("tag-2");
        verify(boardUserTagService, times(2)).getUserTagNameByBoardId(any());
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
