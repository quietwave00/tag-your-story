package com.tagnote.infrastructure.notification.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.application.notification.NotificationDelivery;
import com.tagnote.application.notification.NotificationDeliveryPort;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.tagnote.core.domain.notification.properties.NotificationProperties.*;

@Component
@Slf4j
public class SseNotificationTransport implements NotificationDeliveryPort {
    private final SseConnectionRegistry registry;
    private final ObjectMapper objectMapper;
    private final long timeoutMs;
    private final long heartbeatMs;
    private final int perUserLimit;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "notification-sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public SseNotificationTransport(SseConnectionRegistry registry, ObjectMapper objectMapper,
                                    @Value("${notification.sse.timeout-ms:1800000}") long timeoutMs,
                                    @Value("${notification.sse.heartbeat-ms:25000}") long heartbeatMs,
                                    @Value("${notification.sse.per-user-limit:5}") int perUserLimit) {
        if (timeoutMs <= 0 || heartbeatMs <= 0 || perUserLimit <= 0) {
            throw new IllegalArgumentException("SSE settings must be positive");
        }
        this.registry = registry;
        this.objectMapper = objectMapper;
        this.timeoutMs = timeoutMs;
        this.heartbeatMs = heartbeatMs;
        this.perUserLimit = perUserLimit;
        heartbeatExecutor.scheduleWithFixedDelay(this::heartbeat, heartbeatMs, heartbeatMs, TimeUnit.MILLISECONDS);
    }

    public SseEmitter open(Long userId) {
        SseEmitter emitter = createEmitter();
        SseConnectionRegistry.Connection connection = new SseConnectionRegistry.Connection(UUID.randomUUID(), emitter);
        emitter.onCompletion(() -> registry.remove(userId, connection));
        emitter.onTimeout(() -> close(userId, connection));
        emitter.onError(error -> close(userId, connection));
        List<SseConnectionRegistry.Connection> evicted;
        synchronized (connection) {
            evicted = registry.add(userId, connection, perUserLimit);
            if (!send(userId, connection, SseEmitter.event().name(INIT_NAME).data(INIT_DATA))) {
                // A failed initial write cannot leave a registered emitter behind.
                close(userId, connection);
            }
        }
        for (SseConnectionRegistry.Connection old : evicted) {
            close(userId, old);
        }
        return emitter;
    }

    @Override
    public void deliver(NotificationDelivery delivery) {
        String payload;
        try {
            payload = payload(delivery);
        } catch (JsonProcessingException e) {
            log.error("Cannot serialize notification {}", delivery.notificationId(), e);
            return;
        }
        for (SseConnectionRegistry.Connection connection : registry.connections(delivery.subscriberId())) {
            send(delivery.subscriberId(), connection, SseEmitter.event().name(NOTIFICATION_NAME).data(payload));
        }
    }

    private record EventPayload(Long notificationId, String contentId, String pubNickname,
                                com.tagnote.core.domain.notification.NotificationType type, boolean isRead) {}

    SseEmitter createEmitter() {
        return new SseEmitter(timeoutMs);
    }

    String payload(NotificationDelivery delivery) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new EventPayload(delivery.notificationId(),
                delivery.contentId(), delivery.pubNickname(), delivery.type(), delivery.isRead()));
    }

    void heartbeat() {
        registry.snapshot().forEach((userId, connections) -> connections.forEach(connection ->
                send(userId, connection, SseEmitter.event().comment("heartbeat"))));
    }

    private boolean send(Long userId, SseConnectionRegistry.Connection connection, SseEmitter.SseEventBuilder event) {
        if (!registry.connections(userId).contains(connection)) {
            return false;
        }
        try {
            connection.send(event);
            return true;
        } catch (IOException | RuntimeException e) {
            log.warn("SSE connection {} failed for user {}", connection.id(), userId, e);
            close(userId, connection);
            return false;
        }
    }

    private void close(Long userId, SseConnectionRegistry.Connection connection) {
        registry.remove(userId, connection);
        connection.emitter().complete();
    }

    @PreDestroy
    public void shutdown() {
        heartbeatExecutor.shutdownNow();
        registry.snapshot().forEach((userId, connections) -> connections.forEach(connection -> close(userId, connection)));
    }
}
