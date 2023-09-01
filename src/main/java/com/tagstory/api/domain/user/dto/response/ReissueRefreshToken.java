package com.tagstory.api.domain.user.dto.response;

import com.tagstory.core.domain.user.service.dto.response.ReissueRefreshTokenResponse;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReissueRefreshToken {
    private String newRefreshToken;

    public static ReissueRefreshToken create(ReissueRefreshTokenResponse reissueRefreshTokenResponse) {
        return ReissueRefreshToken.builder()
                .newRefreshToken(reissueRefreshTokenResponse.getNewRefreshToken())
                .build();
    }
}
