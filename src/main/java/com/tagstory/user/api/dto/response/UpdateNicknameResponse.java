package com.tagstory.user.api.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class UpdateNicknameResponse {
    private LocalDateTime updateAt;
    private String nickname;
}
