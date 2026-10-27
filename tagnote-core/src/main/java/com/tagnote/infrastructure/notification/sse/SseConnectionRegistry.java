package com.tagnote.infrastructure.notification.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseConnectionRegistry {
    private final Map<Long, LinkedHashMap<UUID, Connection>> users = new ConcurrentHashMap<>();

    public static final class Connection {
        private final UUID id;
        private final SseEmitter emitter;
        private volatile boolean active = true;

        public Connection(UUID id, SseEmitter emitter) {
            this.id = id;
            this.emitter = emitter;
        }

        public UUID id() { return id; }
        public SseEmitter emitter() { return emitter; }

        public void send(SseEmitter.SseEventBuilder event) throws java.io.IOException {
            synchronized (this) {
                if (!active) return;
                emitter.send(event);
            }
        }

        public void deactivate() { active = false; }
    }

    public synchronized List<Connection> add(Long userId, Connection connection, int limit) {
        LinkedHashMap<UUID, Connection> connections = users.computeIfAbsent(userId, ignored -> new LinkedHashMap<>());
        connections.put(connection.id(), connection);
        List<Connection> evicted = new ArrayList<>();
        while (connections.size() > limit) {
            UUID oldestId = connections.keySet().iterator().next();
            Connection old = connections.remove(oldestId);
            old.deactivate();
            evicted.add(old);
        }
        return evicted;
    }

    public synchronized void remove(Long userId, Connection connection) {
        Map<UUID, Connection> connections = users.get(userId);
        if (connections != null && connections.remove(connection.id(), connection)) {
            connection.deactivate();
            if (connections.isEmpty()) users.remove(userId, connections);
        }
    }

    public synchronized List<Connection> connections(Long userId) {
        Map<UUID, Connection> connections = users.get(userId);
        return connections == null ? List.of() : List.copyOf(connections.values());
    }

    public synchronized Map<Long, List<Connection>> snapshot() {
        Map<Long, List<Connection>> snapshot = new LinkedHashMap<>();
        users.forEach((userId, connections) -> snapshot.put(userId, List.copyOf(connections.values())));
        return snapshot;
    }
}
