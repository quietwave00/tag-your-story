package com.tagstory.core.board.service;

import com.tagstory.core.domain.board.Board;
import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import com.tagstory.core.domain.board.dto.response.BoardByTrackResponse;
import com.tagstory.core.domain.board.dto.response.CreateBoardResponse;
import com.tagstory.core.domain.board.repository.BoardRepository;
import com.tagstory.core.domain.board.service.BoardService;
import com.tagstory.core.domain.hashtag.Hashtag;
import com.tagstory.core.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private BoardService boardService;

    private User user;

    @BeforeEach
    void init() {
        user = User.builder()
                .userId(1L)
                .nickname("test")
                .build();

    }

    @Test
    @DisplayName("글 작성")
    void create() {
        //given

        Hashtag hashtag = Hashtag.builder()
                .hashtagId(1L)
                .name("test")
                .build();
        List<Hashtag> hashtagList = List.of(hashtag);

        CreateBoardCommand createBoardCommand = CreateBoardCommand.builder()
                .hashtagList(List.of("test"))
                .content("test")
                .build();

        Board board = Board.create(createBoardCommand);
        board.addUser(user);
        board.addHashtag(hashtagList);

        //when
        when(boardRepository.save(any())).thenReturn(board);
        CreateBoardResponse createBoardResponse = boardService.create(createBoardCommand, user, hashtagList);

        //then
        assertThat(createBoardResponse.getNickname()).isEqualTo(user.getNickname());
        assertThat(createBoardResponse.getContent()).isEqualTo(board.getContent());
        assertThat(createBoardResponse.getHashtagList().get(0)).isEqualTo(hashtag.getName());
    }

    @Test
    @DisplayName("트랙에 따른 게시글 리스트 조회")
    void getBoardListByTrackId() {
        //given
        String trackId = "test";
        Board board1 = new Board();
        board1.addUser(user);
        board1.generateTestBoard(1L, "content1", "nickname1");
        Board board2 = new Board();
        board2.addUser(user);
        board2.generateTestBoard(2L, "content2", "nickname2");
        List<Board> boardList = List.of(board1, board2);

        //when
        when(boardRepository.findByStatusAndTrackIdOrderByBoardIdDesc(any(), anyString()))
                .thenReturn(boardList);
        List<BoardByTrackResponse> result = boardService.getBoardListByTrackId(trackId);

        //then
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).getContent()).isEqualTo("content1");
        assertThat(result.get(1).getContent()).isEqualTo("content2");
    }
}
