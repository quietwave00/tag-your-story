package com.tagstory.user.api.dto.request;

import lombok.Getter;

@Getter
public class ReissueJwtRequest {
    private String refreshToken;
}
