package com.tagstory.domain.boardhashtag.service;

import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import com.tagstory.core.domain.boardhashtag.repository.BoardHashtagRepository;
import com.tagstory.core.domain.boardhashtag.service.BoardHashtagService;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.domain.board.fixture.BoardFixture;
import com.tagstory.domain.boardhashtag.fixture.HashtagFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@ExtendWith(MockitoExtension.class)
public class BoardHashtagServiceTest {

    @Mock
    private BoardHashtagRepository boardHashtagRepository;

    @InjectMocks
    private BoardHashtagService boardHashtagService;

    @Test
    void BoardHashtag의_리스트를_만들어준다() {
        // given
        List<HashtagEntity> hashtagEntityList = List.of(
                HashtagFixture.createHashtagEntity(1L, "hashtag1"),
                HashtagFixture.createHashtagEntity(2L, "hashtag2")
                );

        // when
        List<BoardHashtagEntity> resultList = boardHashtagService.makeBoardHashtagList(BoardFixture.createBoardEntityWithUserEntity(),
                                                hashtagEntityList);

        // then
        assertSoftly(softly -> {
            assertThat(resultList.get(0).getHashtag().getHashtagId()).isEqualTo(1L);
            assertThat(resultList.get(0).getHashtag().getName()).isEqualTo("hashtag1");
            assertThat(resultList.size()).isEqualTo(2);
        });
    }
}
