package com.tagstory.core.board.service;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import com.tagstory.core.domain.board.dto.response.BoardByTrack;
import com.tagstory.core.domain.board.dto.response.CreateBoard;
import com.tagstory.core.domain.board.repository.BoardRepository;
import com.tagstory.core.domain.board.service.BoardService;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.core.domain.user.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BoardEntityServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private BoardService boardService;

    private UserEntity userEntity;

    @BeforeEach
    void init() {
        userEntity = UserEntity.builder()
                .userId(1L)
                .nickname("test")
                .build();

    }

    @Test
    @DisplayName("글 작성")
    void create() {
        //given

        HashtagEntity hashtagEntity = HashtagEntity.builder()
                .hashtagId(1L)
                .name("test")
                .build();
        List<HashtagEntity> hashtagEntityList = List.of(hashtagEntity);

        CreateBoardCommand createBoardCommand = CreateBoardCommand.builder()
                .hashtagList(List.of("test"))
                .content("test")
                .build();

        BoardEntity boardEntity = BoardEntity.create(createBoardCommand);
        boardEntity.addUser(userEntity);
//        boardEntity.addHashtag(hashtagEntityList);

        //when
        when(boardRepository.save(any())).thenReturn(boardEntity);
        CreateBoard createBoard = boardService.create(createBoardCommand, userEntity, hashtagEntityList);

        //then
        assertThat(createBoard.getNickname()).isEqualTo(userEntity.getNickname());
        assertThat(createBoard.getContent()).isEqualTo(boardEntity.getContent());
//        assertThat(createBoard.getHashtagList().get(0)).isEqualTo(hashtagEntity.getName());
    }

//    @Test
//    @DisplayName("트랙에 따른 게시글 리스트 조회")
//    void getBoardListByTrackId() {
//        //given
//        String trackId = "test";
//        BoardEntity boardEntity1 = new BoardEntity();
//        boardEntity1.addUser(userEntity);
//        boardEntity1.generateTestBoard("test1", "content1", "nickname1");
//        BoardEntity boardEntity2 = new BoardEntity();
//        boardEntity2.addUser(userEntity);
//        boardEntity2.generateTestBoard("test2", "content2", "nickname2");
//        List<BoardEntity> boardEntityList = List.of(boardEntity1, boardEntity2);
//
//        //when
//        when(boardRepository.findByStatusAndTrackIdOrderByBoardIdDesc(any(), anyString(), any(Pageable.class)))
//                .thenReturn(new PageImpl<>(boardEntityList));
//        List<BoardByTrack> result = boardService.getBoardListByTrackId(trackId, 0);
//
//        //then
//        assertThat(result.size()).isEqualTo(2);
//        assertThat(result.get(0).getContent()).isEqualTo("content1");
//        assertThat(result.get(1).getContent()).isEqualTo("content2");
//    }
}
