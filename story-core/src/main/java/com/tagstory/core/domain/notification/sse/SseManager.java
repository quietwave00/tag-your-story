package com.tagstory.core.domain.notification.sse;

import com.tagstory.core.domain.notification.sse.object.CustomSseEmitter;
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
    public CustomSseEmitter create(Long userId, LocalDateTime createdAt) {
        String key = SseKey.generate(userId, createdAt);

        CustomSseEmitter sseEmitter = new CustomSseEmitter(sseStorage, key);
        sseEmitter.setUp();
        sseEmitter.init();

        sseStorage.save(key, sseEmitter);

        log.info("현재 Sse Map 크기: {}", sseStorage.getSseEmitterMap().size());
        log.info("현재 Sse Map key값: {}", sseStorage.getSseEmitterMap().keySet().iterator().next());
        return sseEmitter;
    }

    /*
     * SseEmitter를 가져온다.
     */
    public SseEmitter get(Long userId) {
        return sseStorage.get(userId);
    }
}
