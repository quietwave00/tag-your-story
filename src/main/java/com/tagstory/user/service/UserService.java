package com.tagstory.user.service;

import com.tagstory.entity.User;
import com.tagstory.exception.CustomException;
import com.tagstory.exception.ExceptionCode;
import com.tagstory.jwt.JwtUtil;
import com.tagstory.user.api.dto.request.ReissueJwtRequest;
import com.tagstory.user.api.dto.response.ReissueJwtResponse;
import com.tagstory.user.api.dto.response.ReissueRefreshTokenResponse;
import com.tagstory.user.repository.UserRepository;
import com.tagstory.user.service.mapper.UserMapper;
import com.tagstory.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        Long userId = UserContextHolder.getUserId();
        if(!isValidRefreshToken(reissueJwtRequest.getRefreshToken(), userId)) {
            throw new CustomException(ExceptionCode.INVALID_REFRESH_TOKEN);
        }
        String newJwt = jwtUtil.generateAccessToken(userId);
        return userMapper.toReissueJwtResponse(newJwt);
    }

    @Transactional
    public ReissueRefreshTokenResponse reissueRefreshToken() {
        User user = findByUserId(UserContextHolder.getUserId());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUserKey());
        user.reissueRefreshToken(newRefreshToken);
        return userMapper.toReissueRefreshTokenResponse(newRefreshToken);
    }

    private boolean isValidRefreshToken(String refreshToken, Long userId) {
        User user = findByUserId(userId);
        return refreshToken.equals(user.getRefreshToken());
    }

    private User findByUserId(Long userId) {
        return userRepository.findByUserId(userId).orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
    }


}
