package com.tagstory.core.domain.notification.sse;

import com.tagstory.core.domain.notification.sse.object.SseKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;

import static com.tagstory.core.domain.notification.properties.NotificationProperties.INIT_NAME;
import static com.tagstory.core.domain.notification.properties.NotificationProperties.INIT_DATA;

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

        SseEmitter sseEmitter = new SseEmitter();
        setUp(sseEmitter, key);
        init(sseEmitter);

        sseStorage.save(key, sseEmitter);

//        log.info("현재 Sse Map 크기: {}", sseStorage.getSseEmitterMap().size());
//        log.info("현재 Sse Map key값: {}", sseStorage.getSseEmitterMap().keySet().iterator().next());
        return sseEmitter;
    }

    /*
     * SseEmitter를 가져온다.
     */
    public SseEmitter get(Long userId) {
        return sseStorage.get(userId);
    }

    /*
     * SseEmitter의 상태를 정의한다.
     */
    public void setUp(SseEmitter sseEmitter, String key) {
        /* SSE 연결이 종료됐을 때 */
        sseEmitter.onCompletion(() -> {
            sseStorage.delete(key);
        });

        /* SSE 지연 시간 도달 */
        sseEmitter.onTimeout(() -> {
            sseEmitter.complete();
            sseStorage.delete(key);
        });
    }

    /*
     * 초기 데이터를 보낸다.
     */
    public void init(SseEmitter sseEmitter) {
        try {
            sseEmitter.send(SseEmitter.event()
                    .name(INIT_NAME)
                    .data(INIT_DATA));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
