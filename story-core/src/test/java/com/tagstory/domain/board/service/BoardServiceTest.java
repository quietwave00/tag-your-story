package com.tagstory.domain.board.service;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.board.repository.BoardRepository;
import com.tagstory.core.domain.board.service.BoardService;
import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import com.tagstory.core.domain.boardhashtag.repository.BoardHashtagRepository;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNameList;
import com.tagstory.core.domain.user.service.dto.response.User;
import com.tagstory.domain.board.fixture.BoardFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardHashtagRepository boardHashtagRepository;

    @InjectMocks
    private BoardService boardService;


    @Test
    void 게시글을_작성한다() {
        // given
        User user = User.builder()
                .userId(1L)
                .build();

        List<BoardHashtagEntity> boardHashtagEntityList = List.of(
                BoardHashtagEntity.builder()
                        .boardHashtagId(1L)
                        .build(),
                BoardHashtagEntity.builder()
                        .boardHashtagId(2L)
                        .build()
        );

        BoardEntity mockSavedBoard = BoardEntity.builder()
                .boardId("1")
                .userEntity(user.toEntity())
                .boardHashtagEntityList(boardHashtagEntityList)
                .build();

        // when
        when(boardRepository.save(any())).thenReturn(mockSavedBoard);
        when(boardHashtagRepository.findHashtagNameByBoardId(any()))
                .thenReturn(List.of("hashtag1", "hashtag2"));

        Board board = boardService.create(BoardFixture.createBoardEntityWithUserEntity(), user, boardHashtagEntityList);

        // then
        assertThat(board.getBoardId()).isEqualTo("1");
        assertThat(board.getUser().getUserId()).isEqualTo(1L);
        assertThat(board.getHashtagNameList()).isNotNull();
    }

    @Test
    void 글쓴이가_맞으면_true를_반환한다() {
        // given
        String boardId = "1";
        Long userId = 1L;

        // when
        when(boardRepository.findByBoardIdAndUserEntity_UserId(boardId, userId))
                .thenReturn(Optional.of(BoardFixture.createBoardEntityWithUserEntity()));

        // then
        Boolean result = boardService.isWriter(boardId, userId);
        assertThat(result).isTrue();
    }

    @Test
    void 글쓴이가_아니면_false를_반환한다() {
        // given
        String boardId = "1";
        Long userId = 1L;

        // when
        when(boardRepository.findByBoardIdAndUserEntity_UserId(boardId, userId))
                .thenReturn(Optional.empty());

        // then
        Boolean result = boardService.isWriter(boardId, userId);
        assertThat(result).isFalse();
    }

    @Test
    void 트랙아이디에_해당하는_게시글_리스트를_불러온다() {
        //given
        List<Board> boardList = List.of(
                BoardFixture.createBoard("1"),
                BoardFixture.createBoard("2")
                );

        List<HashtagNameList> hashtagNameList = List.of(
                HashtagNameList.onComplete(List.of("hashtag1-1", "hashtag1-2")),
                HashtagNameList.onComplete(List.of("hashtag2-1", "hashtag2-2"))
                );

        // when
        List<Board> resultList = boardService.getBoardListByTrackId(boardList, hashtagNameList);

        // then
        assertThat(boardList.get(0).getHashtagNameList().getNameList().get(0)).isEqualTo("hashtag1-1");
        assertThat(boardList.get(0).getHashtagNameList().getNameList().size()).isEqualTo(2);
    }
}
