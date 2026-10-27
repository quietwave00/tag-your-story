package com.tagnote.infrastructure.notification.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.application.notification.NotificationDelivery;
import com.tagnote.core.domain.notification.NotificationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class SseNotificationTransportTest {
    private final SseConnectionRegistry registry = new SseConnectionRegistry();
    private final SseNotificationTransport transport = new SseNotificationTransport(registry, new ObjectMapper(),
            60000, 60000, 2);

    @AfterEach void shutdown() { transport.shutdown(); }

    @Test
    void 탭마다_별도_연결을_만들고_상한을_넘으면_가장_오래된_연결을_종료한다() {
        SseEmitter first = transport.open(1L);
        SseEmitter second = transport.open(1L);
        assertThat(first).isNotSameAs(second);
        assertThat(registry.connections(1L)).hasSize(2);
        transport.open(1L);
        assertThat(registry.connections(1L)).hasSize(2);
        assertThat(registry.connections(1L)).extracting(SseConnectionRegistry.Connection::emitter)
                .doesNotContain(first).contains(second);
    }

    @Test
    void 오래된_연결의_종료는_다른_연결을_삭제하지_않는다() {
        SseEmitter first = transport.open(1L);
        SseEmitter second = transport.open(1L);
        SseConnectionRegistry.Connection old = new SseConnectionRegistry.Connection(UUID.randomUUID(), first);
        registry.remove(1L, old);
        assertThat(registry.connections(1L)).hasSize(2);
        first.complete();
        assertThat(registry.connections(1L)).extracting(SseConnectionRegistry.Connection::emitter).contains(second);
    }

    @Test
    void 한_연결의_전송_실패가_다른_연결의_전송을_막지_않는다() throws IOException {
        SseEmitter failed = mock(SseEmitter.class);
        SseEmitter healthy = mock(SseEmitter.class);
        SseEmitter unrelated = mock(SseEmitter.class);
        doThrow(new IOException("closed")).when(failed).send(any(SseEmitter.SseEventBuilder.class));
        registry.add(1L, new SseConnectionRegistry.Connection(UUID.randomUUID(), failed), 2);
        registry.add(1L, new SseConnectionRegistry.Connection(UUID.randomUUID(), healthy), 2);
        registry.add(2L, new SseConnectionRegistry.Connection(UUID.randomUUID(), unrelated), 2);

        transport.deliver(new NotificationDelivery(9L, 1L, "board", "name", NotificationType.LIKE, false));

        verify(healthy).send(any(SseEmitter.SseEventBuilder.class));
        verifyNoInteractions(unrelated);
        assertThat(registry.connections(1L)).hasSize(1);
    }

    @Test
    void 하트비트는_연결_수를_변경하지_않는다() {
        transport.open(1L);
        transport.heartbeat();
        assertThat(registry.connections(1L)).hasSize(1);
    }

    @Test
    void 전송_데이터에는_공개_알림_필드만_포함한다() throws Exception {
        var json = new ObjectMapper().readTree(transport.payload(
                new NotificationDelivery(9L, 1L, "board", "name", NotificationType.COMMENT, false)));
        assertThat(json.size()).isEqualTo(5);
        assertThat(json.get("notificationId").asLong()).isEqualTo(9L);
        assertThat(json.get("contentId").asText()).isEqualTo("board");
        assertThat(json.get("pubNickname").asText()).isEqualTo("name");
        assertThat(json.get("type").asText()).isEqualTo("COMMENT");
        assertThat(json.get("isRead").asBoolean()).isFalse();
    }

    @Test
    void 초기_전송에_실패한_연결은_제거한다() throws IOException {
        SseEmitter broken = mock(SseEmitter.class);
        doThrow(new IOException("closed")).when(broken).send(any(SseEmitter.SseEventBuilder.class));
        SseNotificationTransport failing = new SseNotificationTransport(registry, new ObjectMapper(),
                60000, 60000, 2) {
            @Override SseEmitter createEmitter() { return broken; }
        };
        try {
            failing.open(3L);
            assertThat(registry.connections(3L)).isEmpty();
        } finally {
            failing.shutdown();
        }
    }

    @Test
    void 시간_초과와_오류는_해당_연결만_제거한다() {
        SseEmitter timedOut = mock(SseEmitter.class);
        SseEmitter errored = mock(SseEmitter.class);
        SseNotificationTransport firstTransport = new SseNotificationTransport(registry, new ObjectMapper(),
                60000, 60000, 3) {
            @Override SseEmitter createEmitter() { return timedOut; }
        };
        SseNotificationTransport secondTransport = new SseNotificationTransport(registry, new ObjectMapper(),
                60000, 60000, 3) {
            @Override SseEmitter createEmitter() { return errored; }
        };
        try {
            firstTransport.open(1L);
            secondTransport.open(1L);
            ArgumentCaptor<Runnable> timeout = ArgumentCaptor.forClass(Runnable.class);
            verify(timedOut).onTimeout(timeout.capture());
            timeout.getValue().run();
            assertThat(registry.connections(1L)).extracting(SseConnectionRegistry.Connection::emitter)
                    .containsExactly(errored);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Consumer<Throwable>> error = ArgumentCaptor.forClass(Consumer.class);
            verify(errored).onError(error.capture());
            error.getValue().accept(new IOException("closed"));
            assertThat(registry.connections(1L)).isEmpty();
        } finally {
            firstTransport.shutdown();
            secondTransport.shutdown();
        }
    }
}
