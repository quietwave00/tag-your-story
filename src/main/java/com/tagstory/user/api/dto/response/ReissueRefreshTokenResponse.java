package com.tagstory.user.api.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReissueRefreshTokenResponse {
    private String newRefreshToken;
}