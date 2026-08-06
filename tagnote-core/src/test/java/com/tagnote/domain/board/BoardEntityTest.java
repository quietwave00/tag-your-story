package com.tagnote.domain.board;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.domain.board.fixture.BoardFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;


public class BoardEntityTest {

    private BoardEntity boardEntity;

    @BeforeEach
    void setUp() {
        boardEntity = BoardEntity.builder()
                .boardId("1")
                .build();
    }

    @Test
    void 유저를_추가한다() {
        // given
        UserEntity userEntity = UserEntity.builder()
                .userId(1L)
                .build();

        // when
        /* 의존하는 객체가 없으므로 검증 할 필요가 없다.*/

        boardEntity.addUser(userEntity);

        // then
        assertSoftly(softly -> {
            softly.assertThat(boardEntity.getUserEntity()).isNotNull();
            softly.assertThat(boardEntity.getUserEntity()).isEqualTo(userEntity);
        });
    }

    @Test
    void 유저 태그_리스트를_추가한다() {
        // given
        BoardUserTagEntity userTagEntity1 =   BoardUserTagEntity.builder()
                .boardUserTagId(1L)
                .build();
        BoardUserTagEntity userTagEntity2 =  BoardUserTagEntity.builder()
                .boardUserTagId(2L)
                .build();
        List<BoardUserTagEntity> boardUserTagEntityList = List.of(
                userTagEntity1, userTagEntity2
        );

        BoardEntity boardEntity = BoardFixture.createBoardEntityWithUserEntity();
        boardEntity.addBoardUserTagList(boardUserTagEntityList);

        //then
        assertThat(boardEntity.getBoardUserTagEntityList()).isNotNull();
        assertThat(boardEntity.getBoardUserTagEntityList().get(0).getBoardUserTagId()).isEqualTo(1L);
        assertThat(boardEntity.getBoardUserTagEntityList().size()).isEqualTo(2);

      //  assertThat(boardEntity.getBoardUserTagEntityList()).containsExactly(userTagEntity1, userTagEntity2);
    }
}
