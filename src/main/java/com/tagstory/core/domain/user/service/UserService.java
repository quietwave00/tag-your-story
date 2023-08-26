package com.tagstory.core.domain.user.service;

import com.mysql.cj.util.StringUtils;
import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionCode;
import com.tagstory.api.jwt.JwtUtil;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.User;
import com.tagstory.core.domain.user.redis.TagStoryRedisTemplate;
import com.tagstory.core.domain.user.repository.UserRepository;
import com.tagstory.core.domain.user.service.dto.receive.ReceiveReissueAccessToken;
import com.tagstory.core.domain.user.service.dto.receive.ReceiveUpdateNickname;
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

    public ReissueAccessTokenResponse reissueAccessToken(ReceiveReissueAccessToken receiveReissueAccessToken) {
        Long userId = jwtUtil.getUserIdFromRefreshToken(receiveReissueAccessToken.getRefreshToken());
        User findUser = findByUserId(userId);
        String newAccessToken = jwtUtil.generateAccessToken(findUser.getUserId());
        return ReissueAccessTokenResponse.onComplete(newAccessToken);
    }

    public ReissueRefreshTokenResponse reissueRefreshToken(final Long userId) {
        User user = findByUserId(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUserId());
        redisTemplate.set(user.getUserId(), newRefreshToken, CacheSpec.REFRESH_TOKEN);
        return ReissueRefreshTokenResponse.onComplete(newRefreshToken);
    }

    public LogoutResponse logout(Long userId) {
        SecurityContextHolder.clearContext();
        return LogoutResponse.onComplete(userId);
    }

    @Transactional
    public UpdateNicknameResponse updateNickname(ReceiveUpdateNickname receiveUpdateNickname, Long userId) {
        User findUser = userRepository.findByUserId(userId).orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        findUser.updateNickname(receiveUpdateNickname.getNickname());
        userRepository.saveCache(findUser, CacheSpec.USER);
        return UpdateNicknameResponse.onComplete(findUser.getNickname());
    }

    public CheckRegisterUserResponse checkRegisterUser(Long userId) {
        User findUser = findByUserId(userId);
        boolean status = StringUtils.isNullOrEmpty(findUser.getNickname());
        return CheckRegisterUserResponse.onComplete(status);
    }

    @Cacheable(value = "user", key = "#userId")
    public User findByUserId(Long userId) {
        return userRepository.findCacheByUserId(userId, CacheSpec.USER);
    }
}
