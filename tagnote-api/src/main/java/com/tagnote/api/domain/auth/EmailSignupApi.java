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

@Tag(name = "Email signup", description = "인증 전 이메일 가입. 모든 endpoint는 로그인 인증이 필요 없다. "
        + "오류는 기존 ApiResult.success=false, response.exceptionCode/message/status 형태다. "
        + "exceptionCode는 서버의 custom_error_code에 해당하며 Google OAuth redirect의 error_code와 구분한다.")
@SecurityRequirements
public interface EmailSignupApi {
    @Operation(summary = "이메일 가입 시작", description = "POST /api/auth/signup/email/start. email은 trim 후 소문자화한다. "
            + "password는 8자 이상·UTF-8 기준 72바이트 이하, 영문자와 숫자를 각각 하나 이상 포함해야 하며 passwordConfirm과 같아야 한다. "
            + "성공 시 10분 유효한 6자리 코드를 발송하고 challengeId를 반환한다. DB 계정은 생성하지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ApiResult.response.challengeId. 오류는 SIGNUP_INPUT_INVALID, GOOGLE_ACCOUNT_EXISTS, LOCAL_ACCOUNT_EXISTS, SIGNUP_RATE_LIMITED, SIGNUP_MAIL_UNAVAILABLE, SIGNUP_CHALLENGE_UNAVAILABLE. 기존 CustomExceptionManager 정책상 HTTP 200이며 success=false, response.exceptionCode/message/status를 확인한다.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "필수 body 필드 누락: validation 오류", content = @Content)
    })
    ApiResult<EmailSignupController.StartResponse> start(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "email, password, passwordConfirm 필수", required = true)
            EmailSignupController.StartRequest body, @Parameter(hidden = true) HttpServletRequest request);

    @Operation(summary = "인증 코드 재발송", description = "POST /api/auth/signup/email/resend. challengeId 필수. "
            + "60초 cooldown과 최대 5회 발송 제한. 새 코드가 활성화되면 이전 코드는 무효다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ApiResult.success=true. 오류: SIGNUP_CHALLENGE_INVALID, SIGNUP_RATE_LIMITED, SIGNUP_MAIL_UNAVAILABLE, SIGNUP_CHALLENGE_UNAVAILABLE", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "필수 body 필드 누락", content = @Content)
    })
    ApiResult<Void> resend(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "challengeId 필수", required = true)
            EmailSignupController.ResendRequest body, @Parameter(hidden = true) HttpServletRequest request);

    @Operation(summary = "6자리 코드 확인과 가입", description = "POST /api/auth/signup/email/confirm. challengeId와 숫자 6자리 code 필수. "
            + "성공 시 ACTIVE LOCAL USER를 저장한 뒤 accessToken/refreshToken cookie를 발급한다. "
            + "ApiResult.response는 userId, nickname이다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공 또는 기존 오류 envelope. 오류 exceptionCode: SIGNUP_CODE_INVALID, SIGNUP_CHALLENGE_INVALID, SIGNUP_CONFIRM_BUSY, GOOGLE_ACCOUNT_EXISTS, LOCAL_ACCOUNT_EXISTS, SIGNUP_CONFLICT, SIGNUP_CHALLENGE_UNAVAILABLE. response.status는 각각 논리 HTTP status다.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "필수 body 필드 누락", content = @Content)
    })
    ApiResult<EmailSignupController.ConfirmResponse> confirm(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "challengeId, code 필수", required = true)
            EmailSignupController.ConfirmRequest body, @Parameter(hidden = true) HttpServletResponse response);

    @Operation(summary = "이메일 가입 가능 여부", description = "GET /api/auth/email-availability?email=. "
            + "email은 trim 후 소문자화한다. 사용 가능하면 response.available=true. 이메일/IP별 조회 제한이 있다. "
            + "Google 계정이면 GOOGLE_ACCOUNT_EXISTS로 가입 수단을 알린다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공 또는 기존 오류 envelope. 오류 exceptionCode: SIGNUP_INPUT_INVALID, GOOGLE_ACCOUNT_EXISTS, LOCAL_ACCOUNT_EXISTS, SIGNUP_RATE_LIMITED, SIGNUP_CHALLENGE_UNAVAILABLE. response.status는 논리 HTTP status다.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "email query parameter 누락", content = @Content)
    })
    ApiResult<EmailSignupController.AvailabilityResponse> availability(
            @Parameter(description = "확인할 이메일", required = true) String email,
            @Parameter(hidden = true) HttpServletRequest request);
}
