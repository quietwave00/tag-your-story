package com.tagstory.core.domain.user.service;

import com.mysql.cj.util.StringUtils;
import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionCode;
import com.tagstory.api.jwt.JwtUtil;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.UserEntity;
import com.tagstory.core.domain.user.redis.TagStoryRedisTemplate;
import com.tagstory.core.domain.user.repository.UserRepository;
import com.tagstory.core.domain.user.service.dto.command.ReissueAccessTokenCommand;
import com.tagstory.core.domain.user.service.dto.command.UpdateNicknameCommand;
import com.tagstory.core.domain.user.service.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserService  {

    private final UserRepository userRepository;
    private final TagStoryRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    public ReissueAccessTokenResponse reissueAccessToken(ReissueAccessTokenCommand reissueAccessTokenCommand) {
        Long userId = jwtUtil.getUserIdFromRefreshToken(reissueAccessTokenCommand.getRefreshToken());
        UserEntity findUserEntity = findCacheByUserId(userId);
        String newAccessToken = jwtUtil.generateAccessToken(findUserEntity.getUserId());
        return ReissueAccessTokenResponse.onComplete(newAccessToken);
    }

    public ReissueRefreshTokenResponse reissueRefreshToken(final Long userId) {
        UserEntity userEntity = findCacheByUserId(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(userEntity.getUserId());
        redisTemplate.set(userEntity.getUserId(), newRefreshToken, CacheSpec.REFRESH_TOKEN);
        return ReissueRefreshTokenResponse.onComplete(newRefreshToken);
    }

    public LogoutResponse logout(Long userId) {
        SecurityContextHolder.clearContext();
        return LogoutResponse.onComplete(userId);
    }

    @Transactional
    public UpdateNicknameResponse updateNickname(UpdateNicknameCommand updateNicknameCommand, Long userId) {
        UserEntity findUserEntity = userRepository.findByUserId(userId).orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        findUserEntity.updateNickname(updateNicknameCommand.getNickname());
        userRepository.saveCache(findUserEntity, CacheSpec.USER);
        return UpdateNicknameResponse.onComplete(findUserEntity.getNickname());
    }

    public CheckRegisterUserResponse checkRegisterUser(Long userId) {
        UserEntity findUserEntity = findCacheByUserId(userId);
        boolean status = StringUtils.isNullOrEmpty(findUserEntity.getNickname());
        return CheckRegisterUserResponse.onComplete(status);
    }

    @Cacheable(value = "user", key = "#userId")
    public UserEntity findCacheByUserId(Long userId) {
        return userRepository.findCacheByUserId(userId, CacheSpec.USER);
    }

    public UserEntity findByUserId(Long userId) {
        return userRepository.getReferenceById(userId);
    }
}
