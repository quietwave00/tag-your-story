package com.tagstory.api.domain.user.dto.response;

import com.tagstory.core.domain.user.service.dto.response.ReissueAccessTokenResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReissueAccessToken {
    private String newJwt;

    public static ReissueAccessToken create(ReissueAccessTokenResponse reissueAccessTokenResponse) {
        return ReissueAccessToken.builder()
                .newJwt(reissueAccessTokenResponse.getNewJwt())
                .build();
    }
}
