package com.tagstory.core.domain.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReissueAccessTokenResponse {
    private String newJwt;

    public static ReissueAccessTokenResponse onComplete(String newJwt) {
        return ReissueAccessTokenResponse.builder()
                .newJwt(newJwt)
                .build();
    }
}
