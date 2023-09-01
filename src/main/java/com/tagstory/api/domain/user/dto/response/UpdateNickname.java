package com.tagstory.api.domain.user.dto.response;

import com.tagstory.core.domain.user.service.dto.response.UpdateNicknameResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class UpdateNickname {
    private LocalDateTime updateAt;
    private String nickname;

    public static UpdateNickname create(UpdateNicknameResponse updateNicknameResponse) {
        return UpdateNickname.builder()
                .updateAt(updateNicknameResponse.getUpdateAt())
                .nickname(updateNicknameResponse.getNickname())
                .build();
    }
}
