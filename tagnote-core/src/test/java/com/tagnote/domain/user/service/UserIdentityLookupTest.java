package com.tagnote.domain.user.service;

import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.domain.user.AccountProvider;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.domain.user.service.UserService;
import com.tagnote.core.utils.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserIdentityLookupTest {
    @Mock UserRepository users;
    @Mock CommonRedisTemplate redis;
    @Mock JwtUtil jwt;
    @InjectMocks UserService service;

    @Test
    void email_조회는_저장과_동일하게_정규화한다() {
        UserEntity saved = UserEntity.builder().userId(42L).userKey("google-sub")
                .email("user@example.com").provider(AccountProvider.GOOGLE).subject("google-sub").build();
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(saved));

        assertThat(service.findByEmail(" User@Example.com ").getUserId()).isEqualTo(42L);
        verify(users).findByEmail("user@example.com");
    }

    @Test
    void Google_조회는_email이_아닌_provider_subject를_사용한다() {
        when(users.findByProviderAndSubject(AccountProvider.GOOGLE, "google-sub"))
                .thenReturn(Optional.of(UserEntity.builder().userId(42L).userKey("google-sub")
                        .email("user@example.com").provider(AccountProvider.GOOGLE)
                        .subject("google-sub").build()));

        assertThat(service.findGoogleBySubject("google-sub").getUserId()).isEqualTo(42L);
        verify(users).findByProviderAndSubject(AccountProvider.GOOGLE, "google-sub");
    }
}
