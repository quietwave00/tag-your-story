package com.tagstory.core.domain.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReissueRefreshToken {
    private String newRefreshToken;

    public static ReissueRefreshToken onComplete(String newRefreshToken) {
        return ReissueRefreshToken.builder()
                .newRefreshToken(newRefreshToken)
                .build();
    }
}
