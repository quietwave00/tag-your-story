package com.tagstory.core.domain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagstory.core.domain.notification.service.dto.command.NotificationCommand;
import com.tagstory.core.domain.notification.service.dto.response.NotificationMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Getter
@Slf4j
public class NotificationManager implements MessageListener {

    private final RedisTemplate<String, String> notificationTemplate;
    private final ObjectMapper objectMapper;

    public void sendMessage(NotificationCommand command) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(command);
            notificationTemplate.convertAndSend("Notification", jsonMessage);
        } catch(JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            NotificationMessage response = objectMapper.readValue(message.getBody(), NotificationMessage.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getNotificationData(Notification notification) {
        try {
            return objectMapper.writeValueAsString(notification);
        } catch(JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
