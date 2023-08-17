package com.tagstory.core.domain.user.service.dto;

import lombok.Data;

@Data
public class ReissueAccessTokenCommand {
    private String refreshToken;
}
