package com.tagnote.infrastructure.external.redis;

import com.tagnote.application.catalog.search.port.SearchKeywordRankingReader;
import com.tagnote.application.catalog.search.port.SearchKeywordRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisSearchKeywordTracker implements SearchKeywordRecorder, SearchKeywordRankingReader {
    private static final String SEARCH_KEYWORD = "search_keyword:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void record(String keyword) {
        redisTemplate.opsForZSet().incrementScore(SEARCH_KEYWORD, keyword, 1);
    }

    @Override
    public List<String> getTopSearchKeywordList() {
        Set<ZSetOperations.TypedTuple<String>> typedTupleSet = redisTemplate
                .opsForZSet()
                .reverseRangeWithScores(SEARCH_KEYWORD, 0, 4);

        return typedTupleSet.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .toList();
    }
}
