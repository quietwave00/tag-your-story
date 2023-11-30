package com.tagstory.core.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static List<String> notificationList = new ArrayList<>();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            Notification notification = objectMapper.readValue(message.getBody(), Notification.class);
            notificationList.add(notification.toString());

            log.info("received notification: " + notification.getNotificationId());
            log.info("sender: " + notification.getPublisher().getUserId());
            log.info("receiver: " + notification.getSubscriber().getUserId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
