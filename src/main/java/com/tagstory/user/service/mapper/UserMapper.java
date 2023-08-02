package com.tagstory.user.service.mapper;

import com.tagstory.user.api.dto.response.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserMapper {
    public ReissueJwtResponse toReissueJwtResponse(String newJwt) {
        return ReissueJwtResponse.builder()
                .newJwt(newJwt)
                .build();
    }

    public ReissueRefreshTokenResponse toReissueRefreshTokenResponse(String newRefreshToken) {
        return ReissueRefreshTokenResponse.builder()
                .newRefreshToken(newRefreshToken)
                .build();
    }

    public LogoutResponse toLogoutResponse(Long userId) {
        return LogoutResponse.builder()
                .logoutAt(LocalDateTime.now())
                .userId(userId)
                .build();
    }

    public UpdateNicknameResponse toUpdateNicknameResponse(String nickname) {
        return UpdateNicknameResponse.builder()
                .updateAt(LocalDateTime.now())
                .nickname(nickname)
                .build();
    }

    public CheckRegisterUserResponse toCheckRegisterUserResponse(boolean status) {
        return CheckRegisterUserResponse.builder()
                .isRegisterUser(status)
                .build();
    }
}
