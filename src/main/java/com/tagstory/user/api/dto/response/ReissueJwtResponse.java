package com.tagstory.user.api.dto.response;

import lombok.Builder;

@Builder
public class ReissueJwtResponse {
    private String newJwt;
}
