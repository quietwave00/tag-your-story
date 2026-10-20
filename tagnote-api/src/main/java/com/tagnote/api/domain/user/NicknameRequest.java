package com.tagnote.api.domain.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "본인 닉네임 변경 요청")
public record NicknameRequest(
        @Schema(description = "저장할 닉네임. 필수, 1~20자, 공백 불가", example = "음악고양이")
        String nickname) { }
