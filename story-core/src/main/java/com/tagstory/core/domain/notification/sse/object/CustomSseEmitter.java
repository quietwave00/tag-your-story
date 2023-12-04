package com.tagstory.core.domain.notification.sse.object;

import com.tagstory.core.domain.notification.sse.SseStorage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class CustomSseEmitter  {

    private final SseStorage sseStorage;

    private String key;
    private SseEmitter sseEmitter;


    public SseEmitter getSseEmitter() {
        return sseEmitter;
    }

    public static CustomSseEmitter of(String key, SseEmitter sseEmitter) {
        return CustomSseEmitter.builder()
                .key(key)
                .sseEmitter(sseEmitter)
                .build();
    }

    /*
     * SseEmitter의 상태를 정의한다.
     */
    public void setUp() {
        /* SSE 연결이 종료됐을 때 */
        sseEmitter.onCompletion(() -> {
            log.info("SSE on Complete");
            sseStorage.delete(key);
        });

        /* SSE 지연 시간 도달 */
        sseEmitter.onTimeout(() -> {
            log.info("SSE on Time Out");
            sseEmitter.complete();
            sseStorage.delete(key);
        });
    }

    /*
     * 초기 데이터를 보낸다.
     */
    public void init() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                sseEmitter.send(SseEmitter.event()
                        .name("init")
                        .data("Subscribe"));
                System.out.println("다보냈다");
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }
}
