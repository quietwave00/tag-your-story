package com.tagstory.core.domain.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReissueRefreshTokenResponse {
    private String newRefreshToken;

    public static ReissueRefreshTokenResponse onComplete(String newRefreshToken) {
        return ReissueRefreshTokenResponse.builder()
                .newRefreshToken(newRefreshToken)
                .build();
    }
}
