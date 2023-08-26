package com.tagstory.core.hashtag;

import com.tagstory.core.domain.hashtag.Hashtag;
import com.tagstory.core.domain.hashtag.repository.HashtagRepository;
import com.tagstory.core.domain.hashtag.service.HashtagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class HashtagServiceTest {

    private HashtagService hashtagService;

    @Mock
    private HashtagRepository hashtagRepository;

    @BeforeEach
    void init() {
        hashtagService = new HashtagService(hashtagRepository);
    }

    @Test
    @DisplayName("해시태그 리스트 반환")
    void getHashtagList() {
        //given
        List<String> hashtagStrList = List.of("value1", "value2", "value3");

        //when
        List<Hashtag> hashtagList = hashtagService.getHashtagList(hashtagStrList);

        //then
        assertThat(hashtagList.get(0).getName()).isEqualTo(hashtagStrList.get(0));
        assertThat(hashtagList.get(1).getName()).isEqualTo(hashtagStrList.get(1));
        assertThat(hashtagList.get(2).getName()).isEqualTo(hashtagStrList.get(2));
    }
}
