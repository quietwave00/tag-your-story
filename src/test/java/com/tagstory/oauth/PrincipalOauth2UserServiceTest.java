package com.tagstory.oauth;


import com.tagstory.entity.User;
import com.tagstory.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class PrincipalOauth2UserServiceTest {


    @InjectMocks
    PrincipalOauth2UserService principalOauth2UserService;

    @Mock
    UserRepository userRepository;

    Map<String, Object> attributes;
    @BeforeEach
    void init() {
        attributes = new HashMap<>();
        attributes.put("email", "test@email.com");
        attributes.put("sub", "userKey");
    }

    @Test
    void 처음_로그인_유저_register() {

        //given
        Optional<User> userOptional = Optional.empty();
        User user = User.builder().userKey("userKey").email("test@email.com").build();

        //when
        when(userRepository.findByUserKey(any())).thenReturn(userOptional);
        when(userRepository.save(any())).thenReturn(user);
        User result = principalOauth2UserService.register(attributes);

        //then
        assertThat(result.getUserKey()).isEqualTo("userKey");
        assertThat(result.getEmail()).isEqualTo("test@email.com");
    }

    @Test
    void 이미_가입된_유저_register() {
        //given
        User user = User.builder().userKey("userKey").email("test@email.com").build();
        Optional<User> userOptional = Optional.ofNullable(user);

        //when
        when(userRepository.findByUserKey(any())).thenReturn(userOptional);
        User result = principalOauth2UserService.register(attributes);

        //then
        assertThat(result.getUserKey()).isEqualTo("userKey");
    }

}
