package com.tagstory.core.domain.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReissueAccessToken {
    private String newJwt;

    public static ReissueAccessToken onComplete(String newJwt) {
        return ReissueAccessToken.builder()
                .newJwt(newJwt)
                .build();
    }
}
