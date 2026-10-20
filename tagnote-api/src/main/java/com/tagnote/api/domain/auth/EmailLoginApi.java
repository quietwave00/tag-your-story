package com.tagnote.api.domain.auth;

import com.tagnote.core.utils.api.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "Email login", description = "LOCAL 계정 로그인. 인증 없음. 오류는 ApiResult.success=false와 "
        + "response.exceptionCode/message/status(custom_error_code)로 전달된다.")
@SecurityRequirements
public interface EmailLoginApi {
    @Operation(summary = "이메일·비밀번호 로그인", description = "POST /api/auth/login. 필수 JSON body: email, password. "
            + "email은 trim 후 Locale.ROOT 소문자화한다. ACTIVE LOCAL USER만 로그인할 수 있다. "
            + "성공 시 response.userId/nickname과 기존 Authorization/Refresh token cookie를 발급한다. "
            + "이후 access 재발급·refresh token 갱신·로그아웃은 기존 /api/user endpoint를 사용한다. "
            + "이메일과 IP별 15분 요청 제한, 이메일별 5회 비밀번호 실패 제한을 적용한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공 또는 기존 오류 envelope. 주요 오류 exceptionCode: LOGIN_INVALID_CREDENTIALS(LOCAL 이메일 없음·비밀번호 오류), GOOGLE_ACCOUNT_EXISTS, USER_INACTIVE, LOGIN_RATE_LIMITED, LOGIN_UNAVAILABLE. response.status는 논리 HTTP status다.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "email/password 필수 필드 누락", content = @Content)
    })
    ApiResult<EmailLoginController.LoginResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "email, password 필수", required = true)
            EmailLoginController.LoginRequest body,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response);
}
