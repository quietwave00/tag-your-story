package com.tagstory.api.domain.user.dto.response;

import com.tagstory.core.domain.user.service.dto.response.Token;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReissueAccessTokenResponse {
    private String newAccessToken;

    public static ReissueAccessTokenResponse from(Token token) {
        return ReissueAccessTokenResponse.builder()
                .newAccessToken(token.getToken())
                .build();
    }
}
