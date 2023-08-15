package com.tagstory.user.service.dto;

import lombok.Data;

@Data
public class ReissueAccessTokenCommand {
    private String refreshToken;
}
