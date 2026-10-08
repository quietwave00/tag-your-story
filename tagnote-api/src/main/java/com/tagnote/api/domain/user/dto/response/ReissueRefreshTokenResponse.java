package com.tagnote.api.domain.user.dto.response;

import com.tagnote.core.domain.user.service.dto.response.Token;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReissueRefreshTokenResponse {
    private String newRefreshToken;

    public static ReissueRefreshTokenResponse from(Token token) {
        return builder()
                .newRefreshToken(token.getToken())
                .build();
    }
}
