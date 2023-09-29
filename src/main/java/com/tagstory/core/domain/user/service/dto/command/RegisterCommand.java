package com.tagstory.core.domain.user.service.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterCommand {
    private String pendingUserId;
    private String nickname;
}
