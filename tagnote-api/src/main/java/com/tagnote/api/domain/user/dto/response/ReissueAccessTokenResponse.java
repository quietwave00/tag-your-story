package com.tagnote.api.domain.user.dto.response;

import com.tagnote.core.domain.user.service.dto.response.Token;
import lombok.Builder;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Builder
public class ReissueAccessTokenResponse {
    @Schema(description = "새 AccessToken JWT 원문", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newAccessToken;

    public static ReissueAccessTokenResponse from(Token token) {
        return builder()
                .newAccessToken(token.getToken())
                .build();
    }
}
