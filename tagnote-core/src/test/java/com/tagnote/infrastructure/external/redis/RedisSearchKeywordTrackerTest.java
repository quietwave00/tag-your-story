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
import java.util.stream.IntStream;

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
    void 검색어_랭킹은_역순_상위_열_개_keyword를_반환한다() {
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        IntStream.rangeClosed(1, 10)
                .forEach(rank -> tuples.add(new DefaultTypedTuple<>("keyword-" + rank, (double) (11 - rank))));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores("search_keyword:", 0, 9)).thenReturn(tuples);

        List<String> result = searchKeywordTracker.getTopSearchKeywordList();

        assertThat(result).containsExactlyElementsOf(IntStream.rangeClosed(1, 10)
                .mapToObj(rank -> "keyword-" + rank).toList());
        verify(zSetOperations).reverseRangeWithScores("search_keyword:", 0, 9);
    }
}
