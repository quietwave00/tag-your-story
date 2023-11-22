package com.tagstory.api.domain.user.dto.request;

import com.tagstory.core.domain.user.service.dto.command.RegisterCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "nickname은 비어 있을 수 없습니다.")
    @Size(min = 1, max = 7, message = "nickname은 1자에서 7자 사이어야 합니다.")
    private String nickname;

    public RegisterCommand toCommand(String pendingUserId) {
        return RegisterCommand.builder()
                .pendingUserId(pendingUserId)
                .nickname(nickname)
                .build();
    }
}
