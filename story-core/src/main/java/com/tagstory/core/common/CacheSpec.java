package com.tagstory.core.common;

import com.tagstory.core.domain.file.FileEntity;
import com.tagstory.core.domain.user.service.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Getter
@AllArgsConstructor
@Slf4j
@SuppressWarnings({"unchecked"})
public enum CacheSpec {
    REFRESH_TOKEN("refreshToken", Duration.ofDays(30L), String.class),
    USER("user", Duration.ofDays(365L), User.class),
    PENDING_USER("pending_user", Duration.ofDays(30L), User.class),
    SPOTIFY_ACCESS_TOKEN("spotifyAccessToken", Duration.ofMinutes(30), String.class),
    FILE("file", Duration.ofDays(365L), FileEntity.class)
    ;

    private static final String SEPARATOR = ":";
    private final String prefix;
    private final Duration ttl;
    private final Class<?> clazz;

    /*
     * id 값으로 key 값을 생성한다.
     */
    public String generateKey(Object id) {
        return this.prefix + SEPARATOR + id.toString();
    }

    /*
     * Class 타입을 반환한다.
     */
    public <T> Class<T> getClazz() {
        return (Class<T>) this.clazz;
    }
}
