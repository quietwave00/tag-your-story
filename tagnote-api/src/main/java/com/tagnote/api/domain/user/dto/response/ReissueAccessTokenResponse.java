package com.tagnote.api.domain.user.dto.response;

import com.tagnote.core.domain.user.service.dto.response.Token;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReissueAccessTokenResponse {
    private String newAccessToken;

    public static ReissueAccessTokenResponse from(Token token) {
        return builder()
                .newAccessToken(token.getToken())
                .build();
    }
}
