package com.tagnote.core.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.StringUtils;

import java.net.URI;


@Configuration
@EnableCaching
@RequiredArgsConstructor
public class RedisConfig {
    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        if (StringUtils.hasText(redisUrl)) {
            URI uri = URI.create(redisUrl);
            RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration();
            redisConfiguration.setHostName(uri.getHost());
            redisConfiguration.setPort(uri.getPort());
            setCredentials(redisConfiguration, uri);

            LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                    .useSsl()
                    .build();
            return new LettuceConnectionFactory(redisConfiguration, clientConfiguration);
        }

        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration();
        redisConfiguration.setHostName(host);
        redisConfiguration.setPort(port);
        return new LettuceConnectionFactory(redisConfiguration);
    }

    private void setCredentials(RedisStandaloneConfiguration redisConfiguration, URI uri) {
        String userInfo = uri.getUserInfo();
        if (!StringUtils.hasText(userInfo)) {
            return;
        }

        String[] credentials = userInfo.split(":", 2);
        if (credentials.length == 2) {
            redisConfiguration.setUsername(credentials[0]);
            redisConfiguration.setPassword(RedisPassword.of(credentials[1]));
            return;
        }

        redisConfiguration.setPassword(RedisPassword.of(credentials[0]));
    }
}
