package com.tagstory.api.domain.user.dto.response;

import com.tagstory.core.domain.user.service.dto.response.UpdateNickname;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class UpdateNicknameResponse {
    private String nickname;

    public static UpdateNicknameResponse from(UpdateNickname updateNicknameResponse) {
        return UpdateNicknameResponse.builder()
                .nickname(updateNicknameResponse.getNickname())
                .build();
    }
}
