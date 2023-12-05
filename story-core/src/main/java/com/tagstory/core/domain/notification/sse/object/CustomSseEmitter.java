package com.tagstory.core.domain.notification.sse.object;

import com.tagstory.core.domain.notification.sse.SseStorage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class CustomSseEmitter extends SseEmitter {

    private final SseStorage sseStorage;
    private String key;

    /*
     * SseEmitter의 상태를 정의한다.
     */
    public void setUp() {
        /* SSE 연결이 종료됐을 때 */
        this.onCompletion(() -> {
            log.info("SSE on Complete");
            sseStorage.delete(key);
        });

        /* SSE 지연 시간 도달 */
        this.onTimeout(() -> {
            log.info("SSE on Time Out");
            this.complete();
            sseStorage.delete(key);
        });
    }

    /*
     * 초기 데이터를 보낸다.
     */
    public void init() {
        try {
            this.send(SseEmitter.event()
                    .name("init")
                    .data("Subscribe"));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
