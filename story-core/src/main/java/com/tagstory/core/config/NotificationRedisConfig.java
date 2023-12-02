package com.tagstory.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
public class NotificationRedisConfig {

    private final LettuceConnectionFactory redisConnectionFactory;
    private final ObjectMapper objectMapper;

//    @Bean
//    MessageListenerAdapter messageListenerAdapter() {
//        return new MessageListenerAdapter(new NotificationSubService(objectMapper));
//    }
//
//    @Bean
//    RedisMessageListenerContainer redisContainer() {
//        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
//        container.setConnectionFactory(redisConnectionFactory);
//        container.addMessageListener(messageListenerAdapter(), topic());
//        return container;
//    }

    @Bean
    ChannelTopic topic() {
        return new ChannelTopic("Notification");
    }

    @Bean
    public RedisTemplate<String, String> notificationTemplate() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        return redisTemplate;
    }
}
