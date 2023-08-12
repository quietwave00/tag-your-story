package com.tagstory.user.service;

import com.mysql.cj.util.StringUtils;
import com.tagstory.entity.User;
import com.tagstory.exception.CustomException;
import com.tagstory.exception.ExceptionCode;
import com.tagstory.jwt.JwtUtil;
import com.tagstory.user.api.dto.request.ReissueJwtRequest;
import com.tagstory.user.api.dto.request.UpdateNicknameRequest;
import com.tagstory.user.api.dto.response.*;
import com.tagstory.user.cache.CacheSpec;
import com.tagstory.user.cache.CacheUserRepository;
import com.tagstory.user.cache.TagStoryRedisTemplate;
import com.tagstory.user.repository.UserRepository;
import com.tagstory.user.service.mapper.UserMapper;
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
    private final UserMapper userMapper;

    public ReissueJwtResponse reissueJwt(ReissueJwtRequest reissueJwtRequest) {
        Long userId = jwtUtil.getUserIdFromRefreshToken(reissueJwtRequest.getRefreshToken());
        User findUser = findByUserId(userId);
        String newAccessToken = jwtUtil.generateAccessToken(findUser.getUserId());
        return userMapper.toReissueJwtResponse(newAccessToken);
    }

    public ReissueRefreshTokenResponse reissueRefreshToken(final Long userId) {
        User user = findByUserId(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUserId());
        redisTemplate.set(user.getUserId(), newRefreshToken, CacheSpec.REFRESH_TOKEN);
        return userMapper.toReissueRefreshTokenResponse(newRefreshToken);
    }

    public LogoutResponse logout(Long userId) {
        SecurityContextHolder.clearContext();
        return userMapper.toLogoutResponse(userId);
    }

    @Transactional
    public UpdateNicknameResponse updateNickname(UpdateNicknameRequest updateNicknameRequest, Long userId) {
        User findUser = userRepository.findByUserId(userId).orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        findUser.updateNickname(updateNicknameRequest.getNickname());
        cacheUserRepository.save(findUser, CacheSpec.USER);
        return userMapper.toUpdateNicknameResponse(findUser.getNickname());
    }

    public CheckRegisterUserResponse checkRegisterUser(Long userId) {
        User findUser = findByUserId(userId);
        boolean status = StringUtils.isNullOrEmpty(findUser.getNickname());
        return userMapper.toCheckRegisterUserResponse(status);
    }

    @Cacheable(value = "user", key = "#userId")
    public User findByUserId(Long userId) {
        return cacheUserRepository.findByUserId(userId, CacheSpec.USER);
    }
}
