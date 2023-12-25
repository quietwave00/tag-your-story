package com.tagstory.api.domain.user.dto.request;

import com.tagstory.core.domain.user.service.dto.command.ReissueAccessTokenCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReissueAccessTokenRequest {

    @NotBlank(message = "refreshToken은 비어 있을 수 없습니다.")
    private String refreshToken;

    public ReissueAccessTokenCommand toCommand() {
        return ReissueAccessTokenCommand.builder()
                .refreshToken(refreshToken)
                .build();
    }
}


