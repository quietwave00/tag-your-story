package com.tagnote.domain.boardusertag.service;

import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.repository.BoardUserTagRepository;
import com.tagnote.core.domain.boardusertag.service.BoardUserTagService;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.domain.board.fixture.BoardFixture;
import com.tagnote.domain.boardusertag.fixture.UserTagFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@ExtendWith(MockitoExtension.class)
public class BoardUserTagServiceTest {

    @Mock
    private BoardUserTagRepository boardUserTagRepository;

    @InjectMocks
    private BoardUserTagService boardUserTagService;

    @Test
    void BoardUserTag의_리스트를_만들어준다() {
        // given
        List<UserTagEntity> userTagEntityList = List.of(
                UserTagFixture.createUserTagEntity(1L, "userTag1"),
                UserTagFixture.createUserTagEntity(2L, "userTag2")
                );

        // when
        List<BoardUserTagEntity> resultList = boardUserTagService.makeBoardUserTagList(BoardFixture.createBoardEntityWithUserEntity(),
                                                userTagEntityList);

        // then
        assertSoftly(softly -> {
            assertThat(resultList.get(0).getUserTag().getUserTagId()).isEqualTo(1L);
            assertThat(resultList.get(0).getUserTag().getName()).isEqualTo("userTag1");
            assertThat(resultList.size()).isEqualTo(2);
        });
    }

    @Test
    void Board_writer와_UserTag_owner가_다르면_거부한다() {
        UserTagEntity otherUsersTag = UserTagEntity.builder()
                .owner(com.tagnote.core.domain.user.UserEntity.builder().userId(2L).build())
                .name("tag")
                .normalizedName("tag")
                .build();

        assertThatThrownBy(() -> boardUserTagService.makeBoardUserTagList(
                BoardFixture.createBoardEntityWithUserEntity(), List.of(otherUsersTag)
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
