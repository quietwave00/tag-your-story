package com.tagnote.api.domain.user.dto.response;

import com.tagnote.core.domain.user.service.dto.response.Token;
import lombok.Builder;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

@Builder
@Getter
public class ReissueRefreshTokenResponse {
    @Schema(description = "새 RefreshToken JWT 원문. 이전 토큰은 즉시 재발급에 사용할 수 없음", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newRefreshToken;

    public static ReissueRefreshTokenResponse from(Token token) {
        return builder()
                .newRefreshToken(token.getToken())
                .build();
    }
}
