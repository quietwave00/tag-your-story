package com.tagstory.core.board.service;

import com.tagstory.core.domain.board.Board;
import com.tagstory.core.domain.board.dto.receive.ReceiveCreateBoard;
import com.tagstory.core.domain.board.dto.response.CreateBoardResponse;
import com.tagstory.core.domain.board.repository.BoardRepository;
import com.tagstory.core.domain.board.service.BoardService;
import com.tagstory.core.domain.hashtag.Hashtag;
import com.tagstory.core.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private BoardService boardService;

    @Test
    @DisplayName("글 작성")
    void create() {
        //given
        User user = User.builder()
                .userId(1L)
                .nickname("test")
                .build();

        Hashtag hashtag = Hashtag.builder()
                .hashtagId(1L)
                .name("test")
                .build();
        List<Hashtag> hashtagList = List.of(hashtag);

        ReceiveCreateBoard receiveCreateBoard = ReceiveCreateBoard.builder()
                .hashtagList(List.of("test"))
                .content("test")
                .build();

        Board board = Board.create(receiveCreateBoard);
        board.addUser(user);
        board.addHashtag(hashtagList);

        //when
        when(boardRepository.save(any())).thenReturn(board);
        CreateBoardResponse createBoardResponse = boardService.create(receiveCreateBoard, user, hashtagList);

        //then
        assertThat(createBoardResponse.getNickname()).isEqualTo(user.getNickname());
        assertThat(createBoardResponse.getContent()).isEqualTo(board.getContent());
        assertThat(createBoardResponse.getHashtagList().get(0)).isEqualTo(hashtag.getName());
    }
}
