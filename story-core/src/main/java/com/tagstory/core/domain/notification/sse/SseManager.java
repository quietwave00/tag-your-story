package com.tagstory.core.domain.notification.sse;

import com.tagstory.core.domain.notification.sse.object.SseKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseManager {

    private final SseStorage sseStorage;

    /*
     * SseEmitter를 생성한다.
     */
    public SseEmitter create(Long userId, LocalDateTime createdAt) {
        String key = SseKey.generate(userId, createdAt);

        CustomSseEmitter sseEmitter = CustomSseEmitter.of(key, new SseEmitter());
        sseEmitter.setUp();
        sseEmitter.init();

        sseStorage.save(key, sseEmitter);
        return sseEmitter;
    }

    /*
     * SseEmitter를 가져온다.
     */
    public SseEmitter get(String key) {
        return sseStorage.get(key);
    }
}
