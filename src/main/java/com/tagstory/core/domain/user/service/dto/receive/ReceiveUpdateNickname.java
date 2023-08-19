package com.tagstory.core.domain.user.service.dto.receive;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReceiveUpdateNickname {
    private String nickname;
}
