package com.tagstory.core.domain.tracks.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SearchKeywordTracker {
    private final StringRedisTemplate redisTemplate;
    private static final String SEARCH_KEYWORD = "search_keyword:";

    /*
     * 키워드를 zset 구조로 저장한다.
     */
    public void save(String keyword) {
        redisTemplate.opsForZSet().incrementScore(SEARCH_KEYWORD, keyword, 1);
    }

    /*
     * 1~5위까지의 키워드 순위를 반환한다.
     */
    public List<String> getTopSearchKeywordList() {
        Set<ZSetOperations.TypedTuple<String>> typedTupleSet =  redisTemplate
                .opsForZSet()
                .reverseRangeWithScores(SEARCH_KEYWORD, 0, 4);

        return typedTupleSet.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .toList();
    }
}
