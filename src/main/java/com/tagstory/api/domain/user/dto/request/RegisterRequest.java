package com.tagstory.api.domain.user.dto.request;

import com.tagstory.core.domain.user.service.dto.command.RegisterCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String nickname;

    public RegisterCommand toCommand(String pendingUserId) {
        return RegisterCommand.builder()
                .pendingUserId(pendingUserId)
                .nickname(nickname)
                .build();
    }
}
