package com.tagstory.api.domain.user.dto.response;

import com.tagstory.core.domain.user.service.dto.response.ReissueRefreshToken;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReissueRefreshTokenResponse {
    private String newRefreshToken;

    public static ReissueRefreshTokenResponse from(ReissueRefreshToken reissueRefreshTokenResponse) {
        return ReissueRefreshTokenResponse.builder()
                .newRefreshToken(reissueRefreshTokenResponse.getNewRefreshToken())
                .build();
    }
}
