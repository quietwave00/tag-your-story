package com.tagstory.core.domain.notification.service;

import com.tagstory.core.common.CommonRedisTemplate;
import com.tagstory.core.domain.notification.service.dto.command.NotificationCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationPubService {
    private final CommonRedisTemplate redisTemplate;

    public void sendMessage(NotificationCommand command) {
        redisTemplate.convertAndSend("topic1", command);
    }
}
