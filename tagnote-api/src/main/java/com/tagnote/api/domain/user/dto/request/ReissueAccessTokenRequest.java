package com.tagnote.api.domain.user.dto.request;

import com.tagnote.core.domain.user.service.dto.command.ReissueAccessTokenCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReissueAccessTokenRequest {

    @NotBlank(message = "refreshToken은 비어 있을 수 없습니다.")
    @Schema(description = "현재 Redis에 저장된 RefreshToken JWT 원문. 필수, 공백 불가", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;

    public ReissueAccessTokenCommand toCommand() {
        return ReissueAccessTokenCommand.builder()
                .refreshToken(refreshToken)
                .build();
    }
}


