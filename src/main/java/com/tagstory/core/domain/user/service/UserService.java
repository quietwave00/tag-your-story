package com.tagstory.core.domain.user.service;

import com.mysql.cj.util.StringUtils;
import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionCode;
import com.tagstory.api.jwt.JwtUtil;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.UserEntity;
import com.tagstory.core.domain.user.redis.TagStoryRedisTemplate;
import com.tagstory.core.domain.user.repository.UserRepository;
import com.tagstory.core.domain.user.repository.dto.CacheUser;
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

    public ReissueAccessToken reissueAccessToken(ReissueAccessTokenCommand reissueAccessTokenCommand) {
        Long userId = jwtUtil.getUserIdFromToken(reissueAccessTokenCommand.getRefreshToken());
        CacheUser cacheUser = findCacheByUserId(userId);
        String newAccessToken = jwtUtil.generateAccessToken(cacheUser.getUserId());
        return ReissueAccessToken.onComplete(newAccessToken);
    }

    public ReissueRefreshToken reissueRefreshToken(final Long userId) {
        CacheUser cacheUser = findCacheByUserId(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(cacheUser.getUserId());
        redisTemplate.set(cacheUser.getUserId(), newRefreshToken, CacheSpec.REFRESH_TOKEN);
        return ReissueRefreshToken.onComplete(newRefreshToken);
    }

    public Logout logout(Long userId) {
        SecurityContextHolder.clearContext();
        return Logout.onComplete(userId);
    }

    @Transactional
    public UpdateNickname updateNickname(UpdateNicknameCommand updateNicknameCommand) {
        CacheUser cacheUser = userRepository.findPendingUserByTempId(updateNicknameCommand.getTempId(), CacheSpec.PENDING_USER);
        UserEntity user = userRepository.save(CacheUser.toEntity(cacheUser));
        user.updateNickname(updateNicknameCommand.getNickname());
        userRepository.saveCache(CacheUser.toCacheUser(user), CacheSpec.USER);
        return UpdateNickname.onComplete(user.getNickname());
    }

    public CheckRegisterUser checkRegisterUser(Long userId) {
        CacheUser cacheUser = findCacheByUserId(userId);
        boolean status = StringUtils.isNullOrEmpty(cacheUser.getNickname());
        return CheckRegisterUser.onComplete(status);
    }

    @Cacheable(value = "user", key = "#userId")
    public CacheUser findCacheByUserId(Long userId) {
        return UserEntity.toCacheUser(userRepository.findByUserId(userId).orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND)));
    }

    public UserEntity findByUserId(Long userId) {
        return userRepository.getReferenceById(userId);
    }
}
