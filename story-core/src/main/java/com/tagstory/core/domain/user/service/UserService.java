package com.tagstory.core.domain.user.service;

import com.tagstory.core.exception.CustomException;
import com.tagstory.core.exception.ExceptionCode;
import com.tagstory.core.utils.jwt.JwtUtil;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.Role;
import com.tagstory.core.domain.user.UserEntity;
import com.tagstory.core.common.CommonRedisTemplate;
import com.tagstory.core.domain.user.repository.UserRepository;
import com.tagstory.core.domain.user.service.dto.command.RegisterCommand;
import com.tagstory.core.domain.user.service.dto.command.ReissueAccessTokenCommand;
import com.tagstory.core.domain.user.service.dto.response.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService  {

    private final UserRepository userRepository;
    private final CommonRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    public Token reissueAccessToken(ReissueAccessTokenCommand reissueAccessTokenCommand) {
        Long userId = jwtUtil.getUserIdFromToken(reissueAccessTokenCommand.getRefreshToken());
        User user = getCacheByUserId(userId);
        String newAccessToken = jwtUtil.generateAccessToken(user.getUserId());
        return Token.onComplete(newAccessToken);
    }

    public Token reissueRefreshToken(final Long userId) {
        User user = getCacheByUserId(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUserId());
        redisTemplate.set(user.getUserId(), newRefreshToken, CacheSpec.REFRESH_TOKEN);
        return Token.onComplete(newRefreshToken);
    }

    public void logout() {
        SecurityContextHolder.clearContext();
    }

    @Transactional
    public User register(RegisterCommand command) {
        User user = getCachedPendingUserById(command.getPendingUserId());
        user.addNickname(command.getNickname());
        user.addRole(Role.ROLE_USER);

        UserEntity userEntity = userRepository.save(user.toEntity());
        userRepository.saveCache(userEntity.toUser(), CacheSpec.USER);

        userRepository.deletePendingUser(user, CacheSpec.PENDING_USER);
        return user;
    }


    /*
     * 단일 메소드
     */
    public User getCacheByUserId(Long userId) {
        return Optional.ofNullable(userRepository.findCachedUserByUserId(userId, CacheSpec.USER))
                .orElseGet(() -> userRepository.findByUserId(userId)
                        .map(UserEntity::toUser)
                        .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND)));
    }

    public User getCacheByUserKey(String userKey) {
        return Optional.ofNullable(userRepository.findCachedUserByUserKey(userKey, CacheSpec.USER))
                .orElseGet(() -> userRepository.findByUserKey(userKey)
                        .map(UserEntity::toUser)
                        .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND)));
    }

    public User getCachedPendingUserById(String pendingUserId) {
        return Optional.ofNullable(userRepository.findCachedUserByPendingUserId(pendingUserId, CacheSpec.PENDING_USER))
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
    }

    @Nullable
    public User findByUserId(Long userId) {
        return userRepository.findByUserId(userId)
                .map(UserEntity::toUser)
                .orElse(null);
    }

    @Nullable
    public User findByUserKey(String userKey) {
        return userRepository.findByUserKey(userKey)
                .map(UserEntity::toUser)
                .orElse(null);
    }

    public User saveCachedPendingUser(User user) {
        return userRepository.saveCache(user, CacheSpec.PENDING_USER);
    }
}