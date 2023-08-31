package com.tagstory.core.domain.user.service.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateNicknameCommand {
    private String nickname;
}
