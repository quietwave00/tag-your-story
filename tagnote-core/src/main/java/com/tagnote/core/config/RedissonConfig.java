package com.tagnote.core.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.URI;

@Configuration
public class RedissonConfig {
    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    private static final String REDISSON_HOST_PREFIX = "redis://";

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer();
        if (StringUtils.hasText(redisUrl)) {
            configureUpstash(singleServerConfig);
        } else {
            singleServerConfig.setAddress(REDISSON_HOST_PREFIX + redisHost + ":" + redisPort);
        }
        return Redisson.create(config);
    }

    private void configureUpstash(SingleServerConfig singleServerConfig) {
        URI uri = URI.create(redisUrl);
        String scheme = "rediss".equals(uri.getScheme()) ? "rediss://" : REDISSON_HOST_PREFIX;
        singleServerConfig.setAddress(scheme + uri.getHost() + ":" + uri.getPort());

        String userInfo = uri.getUserInfo();
        if (!StringUtils.hasText(userInfo)) {
            return;
        }

        String[] credentials = userInfo.split(":", 2);
        if (credentials.length == 2) {
            singleServerConfig.setUsername(credentials[0]);
            singleServerConfig.setPassword(credentials[1]);
            return;
        }

        singleServerConfig.setPassword(credentials[0]);
    }
}
