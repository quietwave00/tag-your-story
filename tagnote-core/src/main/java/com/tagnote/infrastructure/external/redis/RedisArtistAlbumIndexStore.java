package com.tagnote.infrastructure.external.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.application.catalog.detail.model.ArtistAlbumIndex;
import com.tagnote.application.catalog.detail.port.ArtistAlbumIndexStore;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisArtistAlbumIndexStore implements ArtistAlbumIndexStore {
    private static final String KEY_PREFIX = "artistAlbumIndex:classificationV1:formatV1:";
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    @Override
    public ArtistAlbumIndex find(String artistId) {
        String json = redis.opsForValue().get(KEY_PREFIX + artistId);
        if (json == null) return null;
        try {
            ArtistAlbumIndex index = mapper.readValue(json, ArtistAlbumIndex.class);
            return index.formatVersion() == 1 ? index : null;
        } catch (JsonProcessingException e) {
            throw new CustomException(ExceptionCode.CONVERSION_EXCEPTION);
        }
    }

    @Override
    public void save(String artistId, ArtistAlbumIndex index) {
        try {
            String json = mapper.writeValueAsString(index);
            redis.opsForValue().set(KEY_PREFIX + artistId, json,
                    index.classificationComplete() ? Duration.ofMinutes(15) : Duration.ofMinutes(1));
        } catch (JsonProcessingException e) {
            throw new CustomException(ExceptionCode.CONVERSION_EXCEPTION);
        }
    }
}
