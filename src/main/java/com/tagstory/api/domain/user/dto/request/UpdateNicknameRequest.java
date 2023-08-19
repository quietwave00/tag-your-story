package com.tagstory.api.domain.user.dto.request;

import com.tagstory.core.domain.user.service.dto.receive.ReceiveUpdateNickname;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateNicknameRequest {
    private String nickname;

    public ReceiveUpdateNickname toCommand() {
        return ReceiveUpdateNickname.builder()
                .nickname(nickname)
                .build();
    }
}
