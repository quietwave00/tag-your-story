package com.tagnote.domain.board.service;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.dto.command.CreateBoardCommand;
import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.board.repository.BoardRepository;
import com.tagnote.core.domain.board.service.BoardService;
import com.tagnote.core.domain.board.service.dto.BoardList;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.repository.BoardUserTagRepository;
import com.tagnote.core.domain.boardusertag.service.dto.UserTagNames;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.domain.board.fixture.BoardFixture;
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
    private BoardUserTagRepository boardUserTagRepository;

    @InjectMocks
    private BoardService boardService;


    @Test
    void 게시글을_작성한다() {
        // given
        User user = User.builder()
                .userId(1L)
                .build();

        CreateBoardCommand command = CreateBoardCommand.builder()
                .content("content")
                .trackId("trackId")
                .userTagList(List.of("userTag1", "userTag2"))
                .userId(user.getUserId())
                .build();

        UserEntity savedUserEntity = UserEntity.builder()
                .userId(1L)
                .userKey("user-key")
                .email("user@test.com")
                .nickname("nickname")
                .role(Role.ROLE_USER)
                .userStatus(UserStatus.ACTIVE)
                .build();

        BoardEntity mockSavedBoard = BoardEntity.builder()
                .boardId("1")
                .content("content")
                .trackId("trackId")
                .userEntity(savedUserEntity)
                .build();
        List<BoardUserTagEntity> boardUserTagEntityList = List.of(
                BoardUserTagEntity.of(mockSavedBoard, UserTagEntity.create("userTag1")),
                BoardUserTagEntity.of(mockSavedBoard, UserTagEntity.create("userTag2"))
        );
        mockSavedBoard.addBoardUserTagList(boardUserTagEntityList);

        // when
        when(boardRepository.save(any())).thenReturn(mockSavedBoard);

        Board board = boardService.create(BoardFixture.createBoardEntityWithUserEntity(),
                user, boardUserTagEntityList, command
                );

        // then
        assertThat(board.getBoardId()).isEqualTo("1");
        assertThat(board.getUser().getUserId()).isEqualTo(1L);
        assertThat(board.getUserTagNameList()).isNotNull();
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

        List<UserTagNames> userTagNameList = List.of(
                UserTagNames.ofNameList(List.of("userTag1-1", "userTag1-2")),
                UserTagNames.ofNameList(List.of("userTag2-1", "userTag2-2"))
                );

        BoardList pagedBoardList = BoardList.builder()
                .boardList(boardList)
                .totalCount(boardList.size())
                .build();

        // when
        BoardList resultList = boardService.getBoardListByTrackId(pagedBoardList, userTagNameList);

        // then
        List<Board> resultBoardList = resultList.getBoardList();
        assertThat(resultBoardList.get(0).getUserTagNameList().getNameList().get(0)).isEqualTo("userTag1-1");
        assertThat(resultBoardList.get(0).getUserTagNameList().getNameList().size()).isEqualTo(2);
    }
}
