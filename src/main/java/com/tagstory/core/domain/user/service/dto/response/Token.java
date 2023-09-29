package com.tagstory.core.domain.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Token {
    private String token;

    public static Token onComplete(String token) {
        return Token.builder()
                .token(token)
                .build();
    }
}
