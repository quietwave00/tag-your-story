package com.tagstory.domain.board;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import com.tagstory.core.domain.user.UserEntity;
import com.tagstory.domain.board.fixture.BoardFixture;
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
    void 해시태그_리스트를_추가한다() {
        // given
        BoardHashtagEntity hashtagEntity1 =   BoardHashtagEntity.builder()
                .boardHashtagId(1L)
                .build();
        BoardHashtagEntity hashtagEntity2 =  BoardHashtagEntity.builder()
                .boardHashtagId(2L)
                .build();
        List<BoardHashtagEntity> boardHashtagEntityList = List.of(
                hashtagEntity1, hashtagEntity2
        );

        BoardEntity boardEntity = BoardFixture.createBoardEntityWithUserEntity();
        boardEntity.addBoardHashTagList(boardHashtagEntityList);

        //then
        assertThat(boardEntity.getBoardHashtagEntityList()).isNotNull();
        assertThat(boardEntity.getBoardHashtagEntityList().get(0).getBoardHashtagId()).isEqualTo(1L);
        assertThat(boardEntity.getBoardHashtagEntityList().size()).isEqualTo(2);

      //  assertThat(boardEntity.getBoardHashtagEntityList()).containsExactly(hashtagEntity1, hashtagEntity2);
    }
}
