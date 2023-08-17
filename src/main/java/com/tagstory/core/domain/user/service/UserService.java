package com.tagstory.core.domain.user.service;

import com.mysql.cj.util.StringUtils;
import com.tagstory.api.domain.user.dto.response.*;
import com.tagstory.core.domain.user.User;
import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionCode;
import com.tagstory.api.jwt.JwtUtil;
import com.tagstory.api.domain.user.dto.request.UpdateNicknameRequest;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.repository.CacheUserRepository;
import com.tagstory.core.domain.user.redis.TagStoryRedisTemplate;
import com.tagstory.core.domain.user.repository.UserRepository;
import com.tagstory.core.domain.user.service.dto.ReissueAccessTokenCommand;
import com.tagstory.core.domain.user.service.mapper.UserServiceMapper;
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
    private final CacheUserRepository cacheUserRepository;
    private final TagStoryRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    private final UserServiceMapper userServiceMapper;

    public ReissueJwtResponse reissueJwt(ReissueAccessTokenCommand refreshTokenCommend) {
        Long userId = jwtUtil.getUserIdFromRefreshToken(refreshTokenCommend.getRefreshToken());
        User findUser = findByUserId(userId);
        String newAccessToken = jwtUtil.generateAccessToken(findUser.getUserId());
        return userServiceMapper.toReissueJwtResponse(newAccessToken);
    }

    public ReissueRefreshTokenResponse reissueRefreshToken(final Long userId) {
        User user = findByUserId(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUserId());
        redisTemplate.set(user.getUserId(), newRefreshToken, CacheSpec.REFRESH_TOKEN);
        return userServiceMapper.toReissueRefreshTokenResponse(newRefreshToken);
    }

    public LogoutResponse logout(Long userId) {
        SecurityContextHolder.clearContext();
        return userServiceMapper.toLogoutResponse(userId);
    }

    @Transactional
    public UpdateNicknameResponse updateNickname(UpdateNicknameRequest updateNicknameRequest, Long userId) {
        User findUser = userRepository.findByUserId(userId).orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        findUser.updateNickname(updateNicknameRequest.getNickname());
        cacheUserRepository.save(findUser, CacheSpec.USER);
        return userServiceMapper.toUpdateNicknameResponse(findUser.getNickname());
    }

    public CheckRegisterUserResponse checkRegisterUser(Long userId) {
        User findUser = findByUserId(userId);
        boolean status = StringUtils.isNullOrEmpty(findUser.getNickname());
        return userServiceMapper.toCheckRegisterUserResponse(status);
    }

    @Cacheable(value = "user", key = "#userId")
    public User findByUserId(Long userId) {
        return cacheUserRepository.findByUserId(userId, CacheSpec.USER);
    }
}
