package com.tagstory.core.domain.notification.sse;

import com.tagstory.core.domain.notification.sse.object.CustomSseEmitter;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SseStorage {

    public static Map<String, CustomSseEmitter> sseEmitterMap = new ConcurrentHashMap<>();

    /*
     * SseEmitter를 저장한다.
     */
    public void save(String key, CustomSseEmitter sseEmitter) {
        sseEmitterMap.put(key, sseEmitter);
    }

    /*
     * SseEmitter를 삭제한다.
     */
    public void delete(String key) {
        sseEmitterMap.remove(key);
    }

    /*
     * SseEmitter를 반환한다.
     */
    public CustomSseEmitter get(String key) {
        return sseEmitterMap.get(key);
    }

}
