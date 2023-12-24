package com.tagstory.core.domain.notification.sse;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SseStorage {

    public static Map<Long, SseEmitter> sseEmitterMap = new ConcurrentHashMap<>();

    /*
     * SseEmitter를 저장한다.
     */
    public void save(Long key, SseEmitter sseEmitter) {
        sseEmitterMap.put(key, sseEmitter);
    }

    /*
     * SseEmitter를 삭제한다.
     */
    public void delete(Long key) {
        sseEmitterMap.remove(key);
    }

    /*
     * SseEmitter를 반환한다.
     */
    @Nullable
    public SseEmitter get(Long key) {
        return sseEmitterMap.get(key);
    }

    /* log용 임시 */
    public Map<Long, SseEmitter> getSseEmitterMap() {
        return sseEmitterMap;
    }

}
