package com.tagnote.domain.user.service;

import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.domain.user.service.UserService;
import com.tagnote.core.domain.user.service.dto.command.RegisterCommand;
import com.tagnote.core.domain.user.service.dto.command.ReissueAccessTokenCommand;
import com.tagnote.core.domain.user.service.dto.response.Token;
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
    void reissueAccessToken은_refreshToken에서_userId를_꺼내_cache우선으로_조회한다() {
        User cachedUser = user(1L, "cached", Role.ROLE_USER);
        when(jwtUtil.getUserIdFromToken("refresh-token")).thenReturn(1L);
        when(userRepository.findCachedUserByUserId(1L, CacheSpec.USER)).thenReturn(cachedUser);
        when(jwtUtil.generateAccessToken(1L)).thenReturn("new-access-token");

        Token result = userService.reissueAccessToken(ReissueAccessTokenCommand.builder().refreshToken("refresh-token").build());

        assertThat(result.getToken()).isEqualTo("new-access-token");
        verify(userRepository, never()).findByUserId(any());
    }

    @Test
    void reissueRefreshToken은_userId기준_새토큰을_발급하고_redis에_저장한다() {
        User cachedUser = user(1L, "cached", Role.ROLE_USER);
        when(userRepository.findCachedUserByUserId(1L, CacheSpec.USER)).thenReturn(cachedUser);
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("new-refresh-token");

        Token result = userService.reissueRefreshToken(1L);

        assertThat(result.getToken()).isEqualTo("new-refresh-token");
        verify(redisTemplate).set(1L, "new-refresh-token", CacheSpec.REFRESH_TOKEN);
    }

    @Test
    void logout은_SecurityContext를_비운다() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("principal", "credentials"));

        userService.logout();

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void register는_pending_cache를_읽어_role과_nickname을_변경하고_pending_cache를_삭제한다() {
        User pendingUser = User.builder()
                .pendingUserId("pending-1")
                .userId(1L)
                .userKey("user-key")
                .email("user@test.com")
                .role(Role.ROLE_PENDING_USER)
                .userStatus(UserStatus.ACTIVE)
                .build();
        UserEntity savedEntity = UserEntity.builder()
                .userId(1L)
                .userKey("user-key")
                .email("user@test.com")
                .nickname("newbie")
                .role(Role.ROLE_USER)
                .userStatus(UserStatus.ACTIVE)
                .build();
        RegisterCommand command = RegisterCommand.builder()
                .pendingUserId("pending-1")
                .nickname("newbie")
                .build();

        when(userRepository.findCachedUserByPendingUserId("pending-1", CacheSpec.PENDING_USER)).thenReturn(pendingUser);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);

        User result = userService.register(command);

        assertThat(result.getNickname()).isEqualTo("newbie");
        assertThat(result.getRole()).isEqualTo(Role.ROLE_USER);
        verify(userRepository).saveCache(argThat(savedUser ->
                savedUser.getUserId().equals(1L)
                        && "user-key".equals(savedUser.getUserKey())
                        && "user@test.com".equals(savedUser.getEmail())
                        && "newbie".equals(savedUser.getNickname())
                        && savedUser.getRole() == Role.ROLE_USER
        ), any());
        verify(userRepository).deletePendingUser(pendingUser, CacheSpec.PENDING_USER);
    }

    @Test
    void getCacheByUserId는_cache_miss면_DB로_fallback한다() {
        UserEntity userEntity = userEntity(1L, "user-key", "user@test.com", "nickname", Role.ROLE_USER);
        when(userRepository.findCachedUserByUserId(1L, CacheSpec.USER)).thenReturn(null);
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(userEntity));

        User result = userService.getCacheByUserId(1L);

        assertThat(result.getUserId()).isEqualTo(1L);
        verify(userRepository).findByUserId(1L);
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

    private User user(Long userId, String nickname, Role role) {
        return User.builder()
                .userId(userId)
                .nickname(nickname)
                .role(role)
                .userKey("user-key")
                .email("user@test.com")
                .userStatus(UserStatus.ACTIVE)
                .build();
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
