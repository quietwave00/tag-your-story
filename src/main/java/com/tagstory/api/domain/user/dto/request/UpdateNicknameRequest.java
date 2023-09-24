package com.tagstory.api.domain.user.dto.request;

import com.tagstory.core.domain.user.service.dto.command.UpdateNicknameCommand;
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

    public UpdateNicknameCommand toCommand(String tempId) {
        return UpdateNicknameCommand.builder()
                .tempId(tempId)
                .nickname(nickname)
                .build();
    }
}
