package com.tagstory.api.domain.user.dto.request;

import lombok.Getter;

@Getter
public class ReissueAccessTokenRequest {
    private String refreshToken;
}


