package com.tagnote.infrastructure.external.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisSearchKeywordTrackerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private RedisSearchKeywordTracker searchKeywordTracker;

    @Test
    void record는_search_keyword_prefix로_incrementScore를_호출한다() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        searchKeywordTracker.record("rock");

        verify(zSetOperations).incrementScore("search_keyword:", "rock", 1);
    }

    @Test
    void getTopSearchKeywordList는_역순_0부터_4까지_조회하고_keyword만_반환한다() {
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(new DefaultTypedTuple<>("rock", 10.0));
        tuples.add(new DefaultTypedTuple<>("pop", 9.0));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores("search_keyword:", 0, 4)).thenReturn(tuples);

        List<String> result = searchKeywordTracker.getTopSearchKeywordList();

        assertThat(result).containsExactly("rock", "pop");
        verify(zSetOperations).reverseRangeWithScores("search_keyword:", 0, 4);
    }
}
