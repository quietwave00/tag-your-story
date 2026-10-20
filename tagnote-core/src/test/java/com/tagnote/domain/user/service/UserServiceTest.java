package com.tagnote.domain.user.service;

import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.domain.user.service.UserService;
import com.tagnote.core.domain.user.service.dto.command.ReissueAccessTokenCommand;
import com.tagnote.core.domain.user.service.dto.response.Token;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.core.utils.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.tagnote.core.utils.jwt.JwtProperties.TOKEN_TYPE_REFRESH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommonRedisTemplate redisTemplate;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    @Test
    void reissueAccessToken은_refreshToken에서_userId를_꺼내_DB의_현재_상태를_확인한다() {
        UserEntity activeUser = userEntity(1L, "user-key", "user@test.com", "cached", Role.ROLE_USER);
        when(jwtUtil.getUserIdFromToken("refresh-token", TOKEN_TYPE_REFRESH)).thenReturn(1L);
        when(redisTemplate.get(1L, CacheSpec.REFRESH_TOKEN)).thenReturn("refresh-token");
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(activeUser));
        when(jwtUtil.generateAccessToken(1L)).thenReturn("new-access-token");

        Token result = userService.reissueAccessToken(ReissueAccessTokenCommand.builder().refreshToken("refresh-token").build());

        assertThat(result.getToken()).isEqualTo("new-access-token");
        verify(userRepository, never()).findCachedUserByUserId(any(), any());
    }

    @Test
    void reissueAccessToken은_현재_redis_refresh_token만_허용한다() {
        when(jwtUtil.getUserIdFromToken("old-refresh-token", TOKEN_TYPE_REFRESH)).thenReturn(1L);
        when(redisTemplate.get(1L, CacheSpec.REFRESH_TOKEN)).thenReturn("new-refresh-token");

        assertThatThrownBy(() -> userService.reissueAccessToken(
                ReissueAccessTokenCommand.builder().refreshToken("old-refresh-token").build()))
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getExceptionCode())
                        .isEqualTo(ExceptionCode.INVALID_REFRESH_TOKEN));
        verify(userRepository, never()).findByUserId(any());
    }

    @Test
    void reissueAccessToken은_redis_token이_삭제되면_거부한다() {
        when(jwtUtil.getUserIdFromToken("refresh-token", TOKEN_TYPE_REFRESH)).thenReturn(1L);

        assertThatThrownBy(() -> userService.reissueAccessToken(
                ReissueAccessTokenCommand.builder().refreshToken("refresh-token").build()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void reissueAccessToken은_비활성_계정을_거부한다() {
        when(jwtUtil.getUserIdFromToken("refresh-token", TOKEN_TYPE_REFRESH)).thenReturn(1L);
        when(redisTemplate.get(1L, CacheSpec.REFRESH_TOKEN)).thenReturn("refresh-token");
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(UserEntity.builder()
                .userId(1L).role(Role.ROLE_USER).userStatus(UserStatus.DORMANT).build()));

        assertThatThrownBy(() -> userService.reissueAccessToken(
                ReissueAccessTokenCommand.builder().refreshToken("refresh-token").build()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void withdrawn_account_cannot_reissue_even_when_redis_token_remains() {
        when(jwtUtil.getUserIdFromToken("refresh-token", TOKEN_TYPE_REFRESH)).thenReturn(1L);
        when(redisTemplate.get(1L, CacheSpec.REFRESH_TOKEN)).thenReturn("refresh-token");
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(UserEntity.builder()
                .userId(1L).role(Role.ROLE_USER).userStatus(UserStatus.WITHDRAWN).build()));

        assertThatThrownBy(() -> userService.reissueAccessToken(
                ReissueAccessTokenCommand.builder().refreshToken("refresh-token").build()))
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getExceptionCode())
                        .isEqualTo(ExceptionCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void reissueRefreshToken은_userId기준_새토큰을_발급하고_redis에_저장한다() {
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(userEntity(1L, "user-key", "user@test.com", "cached", Role.ROLE_USER)));
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("new-refresh-token");

        Token result = userService.reissueRefreshToken(1L);

        assertThat(result.getToken()).isEqualTo("new-refresh-token");
        verify(redisTemplate).set(1L, "new-refresh-token", CacheSpec.REFRESH_TOKEN);
    }

    @Test
    void logout은_refresh_token을_삭제하고_SecurityContext를_비운다() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("principal", "credentials"));

        userService.logout(1L);

        verify(redisTemplate).delete(1L, CacheSpec.REFRESH_TOKEN);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void getCacheByUserId는_cache_miss면_DB로_fallback한다() {
        UserEntity userEntity = userEntity(1L, "user-key", "user@test.com", "nickname", Role.ROLE_USER);
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(userEntity));

        User result = userService.getCacheByUserId(1L);

        assertThat(result.getUserId()).isEqualTo(1L);
        verify(userRepository).findByUserId(1L);
    }

    @Test
    void cached_active_user_does_not_override_inactive_database_status() {
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(UserEntity.builder()
                .userId(1L).role(Role.ROLE_USER).userStatus(UserStatus.DORMANT).build()));

        assertThatThrownBy(() -> userService.getCacheByUserId(1L))
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getExceptionCode())
                        .isEqualTo(ExceptionCode.USER_INACTIVE));
        verify(userRepository, never()).findCachedUserByUserId(any(), any());
    }

    @Test
    void withdrawn_account_is_rejected_before_cached_identity_is_read() {
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(UserEntity.builder()
                .userId(1L).role(Role.ROLE_USER).userStatus(UserStatus.WITHDRAWN).build()));

        assertThatThrownBy(() -> userService.requireActiveUser(1L))
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getExceptionCode())
                        .isEqualTo(ExceptionCode.ACCOUNT_WITHDRAWN));
        verify(userRepository, never()).findCachedUserByUserId(any(), any());
    }

    @Test
    void getCacheByUserKey는_cache_miss면_DB로_fallback한다() {
        UserEntity userEntity = userEntity(1L, "user-key", "user@test.com", "nickname", Role.ROLE_USER);
        when(userRepository.findCachedUserByUserKey("user-key", CacheSpec.USER)).thenReturn(null);
        when(userRepository.findByUserKey("user-key")).thenReturn(Optional.of(userEntity));

        User result = userService.getCacheByUserKey("user-key");

        assertThat(result.getUserKey()).isEqualTo("user-key");
        verify(userRepository).findByUserKey("user-key");
    }

    private UserEntity userEntity(Long userId, String userKey, String email, String nickname, Role role) {
        return UserEntity.builder()
                .userId(userId)
                .userKey(userKey)
                .email(email)
                .nickname(nickname)
                .role(role)
                .userStatus(UserStatus.ACTIVE)
                .build();
    }
}
