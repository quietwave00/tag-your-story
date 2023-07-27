package com.tagstory.user.service.mapper;

import com.tagstory.user.api.dto.response.ReissueJwtResponse;
import com.tagstory.user.api.dto.response.ReissueRefreshTokenResponse;
import org.springframework.stereotype.Component;

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
}
