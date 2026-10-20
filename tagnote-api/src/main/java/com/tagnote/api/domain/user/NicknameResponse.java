package com.tagnote.api.domain.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "확정된 닉네임")
public record NicknameResponse(
        @Schema(description = "DB에 저장된 현재 닉네임", example = "음악고양이")
        String nickname) { }
