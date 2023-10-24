package com.tagstory.api.domain.user.dto.request;

import com.tagstory.core.domain.user.service.dto.command.ReissueAccessTokenCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReissueAccessTokenRequest {
    private String refreshToken;

    public ReissueAccessTokenCommand toCommand() {
        return ReissueAccessTokenCommand.builder()
                .refreshToken(refreshToken)
                .build();
    }
}


