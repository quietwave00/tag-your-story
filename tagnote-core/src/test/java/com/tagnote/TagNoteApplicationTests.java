package com.tagnote;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ContextConfiguration(classes = TestConfiguration.class)
@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "JWT_SECRET=test-jwt-secret",
        "GOOGLE_CLIENT_ID=test-google-client-id",
        "GOOGLE_CLIENT_SECRET=test-google-client-secret",
        "SPOTIFY_CLIENT_ID=test-spotify-client-id",
        "SPOTIFY_CLIENT_SECRET=test-spotify-client-secret"
})
public class TagNoteApplicationTests {

    @MockBean
    private RedissonClient redissonClient;

    @Test
    void contextLoads() {
    }
}
