package com.tagstory.api.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReissueRefreshTokenResponse {
    private String newRefreshToken;
}
