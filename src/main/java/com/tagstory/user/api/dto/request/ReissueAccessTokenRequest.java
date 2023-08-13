package com.tagstory.user.api.dto.request;

import lombok.Getter;

@Getter
public class ReissueAccessTokenRequest {
    private String refreshToken;
}


