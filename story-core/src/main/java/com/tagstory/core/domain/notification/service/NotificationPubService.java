package com.tagstory.core.domain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagstory.core.domain.notification.service.dto.command.NotificationCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPubService {

    private final RedisTemplate<String, String> stringValueRedisTemplate;
    private final ObjectMapper objectMapper;

    public void sendMessage(NotificationCommand command) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(command);
            stringValueRedisTemplate.convertAndSend("topic1", jsonMessage);
        } catch(JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }
}
