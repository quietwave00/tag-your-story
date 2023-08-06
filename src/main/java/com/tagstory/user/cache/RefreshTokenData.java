package com.tagstory.user.cache;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.Optional;

@Getter
@Builder
public class RefreshTokenData implements Serializable {
    private Long userId;
    private String refreshToken;
}
