package com.tagstory.api.domain.user.dto.response;

import com.tagstory.core.domain.user.service.dto.response.ReissueAccessToken;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReissueAccessTokenResponse {
    private String newJwt;

    public static ReissueAccessTokenResponse from(ReissueAccessToken reissueAccessTokenResponse) {
        return ReissueAccessTokenResponse.builder()
                .newJwt(reissueAccessTokenResponse.getNewJwt())
                .build();
    }
}
