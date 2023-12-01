package com.tagstory.core.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagstory.core.domain.notification.service.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSubService implements MessageListener {

    private final ObjectMapper objectMapper;
//    private static List<String> notificationList = new ArrayList<>();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            NotificationResponse response = objectMapper.readValue(message.getBody(), NotificationResponse.class);

            log.info("received notification: " + response.getType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
