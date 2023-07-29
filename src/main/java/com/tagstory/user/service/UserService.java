package com.tagstory.user.service;

import com.tagstory.entity.User;
import com.tagstory.exception.CustomException;
import com.tagstory.exception.ExceptionCode;
import com.tagstory.jwt.JwtUtil;
import com.tagstory.user.api.dto.request.ReissueJwtRequest;
import com.tagstory.user.api.dto.response.LogoutResponse;
import com.tagstory.user.api.dto.response.ReissueJwtResponse;
import com.tagstory.user.api.dto.response.ReissueRefreshTokenResponse;
import com.tagstory.user.repository.UserRepository;
import com.tagstory.user.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserService  {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public ReissueJwtResponse reissueJwt(ReissueJwtRequest reissueJwtRequest) {
        String userKey = getUserKeyFromRefreshToken(reissueJwtRequest.getRefreshToken());
        User user = findByUserKey(userKey);
        String newJwt = jwtUtil.generateAccessToken(user.getUserId());
        return userMapper.toReissueJwtResponse(newJwt);
    }

    @Transactional
    public ReissueRefreshTokenResponse reissueRefreshToken(final Long userId) {
        User user = findByUserId(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUserKey());
        user.reissueRefreshToken(newRefreshToken);
        return userMapper.toReissueRefreshTokenResponse(newRefreshToken);
    }

    public LogoutResponse logout(Long userId) {
        SecurityContextHolder.clearContext();
        return userMapper.toLogoutResponse(userId);
    }


    private String getUserKeyFromRefreshToken(String refreshToken) {
        return jwtUtil.validateToken(refreshToken).getClaim("userKey").asString();
    }

    private User findByUserId(Long userId) {
        return userRepository.findByUserId(userId).orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
    }

    private User findByUserKey(String userKey) {
        return userRepository.findByUserKey(userKey).orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
    }
}
