package com.tagnote.domain.board.service;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.board.repository.BoardRepository;
import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.board.service.BoardService;
import com.tagnote.core.domain.board.service.dto.BoardList;
import com.tagnote.core.domain.boardusertag.repository.BoardUserTagRepository;
import com.tagnote.core.domain.boardusertag.service.dto.UserTagNames;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.domain.board.fixture.BoardFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceCharacterizationTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardUserTagRepository boardUserTagRepository;

    @InjectMocks
    private BoardService boardService;

    @Test
    void getBoardCountByTrackId는_POST상태만_count한다() {
        when(boardRepository.countByTrackIdAndStatus("track-1", BoardStatus.POST)).thenReturn(5);

        int result = boardService.getBoardCountByTrackId("track-1");

        assertThat(result).isEqualTo(5);
        verify(boardRepository).countByTrackIdAndStatus("track-1", BoardStatus.POST);
    }

    @Test
    void getBoardEntityByBoardId와_getBoardByBoardId는_POST상태만_조회한다() {
        BoardEntity boardEntity = boardEntity("board-1");
        when(boardRepository.findByBoardIdAndStatus("board-1", BoardStatus.POST)).thenReturn(Optional.of(boardEntity));

        BoardEntity entityResult = boardService.getBoardEntityByBoardId("board-1");
        Board boardResult = boardService.getBoardByBoardId("board-1");

        assertThat(entityResult).isSameAs(boardEntity);
        assertThat(boardResult.getBoardId()).isEqualTo("board-1");
        verify(boardRepository, times(2)).findByBoardIdAndStatus("board-1", BoardStatus.POST);
    }

    @Test
    void getBoardListByTrackIdSortedCreatedAt는_pageSize8을_사용한다() {
        when(boardRepository.findByStatusAndTrackIdOrderByCreatedAtDesc(eq(BoardStatus.POST), eq("track-1"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(boardEntity("board-1"))));

        BoardList result = boardService.getBoardListByTrackIdSortedCreatedAt(BoardStatus.POST, "track-1", 2);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(boardRepository).findByStatusAndTrackIdOrderByCreatedAtDesc(eq(BoardStatus.POST), eq("track-1"), captor.capture());
        assertThat(captor.getValue()).isEqualTo(PageRequest.of(2, 8));
        assertThat(result.getTotalCount()).isEqualTo(1);
    }

    @Test
    void getBoardListByTrackIdSortedLike는_pageSize8을_사용한다() {
        when(boardRepository.findByStatusAndTrackIdOrderByLikeCountDesc(eq(BoardStatus.POST), eq("track-1"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(boardEntity("board-1"))));

        BoardList result = boardService.getBoardListByTrackIdSortedLike(BoardStatus.POST, "track-1", 3);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(boardRepository).findByStatusAndTrackIdOrderByLikeCountDesc(eq(BoardStatus.POST), eq("track-1"), captor.capture());
        assertThat(captor.getValue()).isEqualTo(PageRequest.of(3, 8));
        assertThat(result.getTotalCount()).isEqualTo(1);
    }

    @Test
    void delete는_entity를_삭제하지_않고_REMOVAL로_바꾼다() {
        BoardEntity boardEntity = boardEntity("board-1");
        when(boardRepository.findByBoardIdAndStatus("board-1", BoardStatus.POST)).thenReturn(Optional.of(boardEntity));

        boardService.delete("board-1");

        assertThat(boardEntity.getStatus()).isEqualTo(BoardStatus.REMOVAL);
    }

    @Test
    void increaseLikeCount와_decreaseLikeCount는_atomic_update를_위임한다() {
        boardService.increaseLikeCount("board-1");
        boardService.decreaseLikeCount("board-1");

        verify(boardRepository).updateLikeCount("board-1", 1);
        verify(boardRepository).updateLikeCount("board-1", -1);
    }

    @Test
    void getUserTagNameListByBoardId는_repository결과를_그대로_래핑한다() {
        when(boardUserTagRepository.findUserTagNameByBoardId("board-1")).thenReturn(List.of("tag-1", "tag-2"));

        UserTagNames result = boardService.getUserTagNameListByBoardId("board-1");

        assertThat(result.getNameList()).containsExactly("tag-1", "tag-2");
    }

    private BoardEntity boardEntity(String boardId) {
        BoardEntity boardEntity = BoardFixture.createBoardEntityWithUserEntity();
        UserEntity userEntity = UserEntity.builder()
                .userId(1L)
                .userKey("user-key")
                .email("user@test.com")
                .nickname("nickname")
                .role(Role.ROLE_USER)
                .build();

        return BoardEntity.builder()
                .boardId(boardId)
                .content("content")
                .status(BoardStatus.POST)
                .count(0)
                .trackId("track-1")
                .likeCount(0)
                .userEntity(userEntity)
                .boardUserTagEntityList(boardEntity.getBoardUserTagEntityList())
                .build();
    }
}
