package com.tagnote.api.domain.user;

import com.tagnote.api.auth.PrincipalDetails;
import com.tagnote.api.domain.user.dto.request.ReissueAccessTokenRequest;
import com.tagnote.api.domain.user.dto.response.ReissueAccessTokenResponse;
import com.tagnote.api.domain.user.dto.response.ReissueRefreshTokenResponse;
import com.tagnote.core.utils.api.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User", description = "JWT 재발급, 로그아웃, 프로필 변경")
public interface UserApi {

    @Operation(summary = "내 닉네임 조회", description = "GET /api/user/me. ROLE_USER AccessToken 인증 필수. "
            + "인증 필터가 현재 DB의 ACTIVE USER 상태를 확인한 뒤 저장된 닉네임을 principal에 담는다. "
            + "응답은 ApiResult.response.nickname이며 이메일·내부 식별자 등은 반환하지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "현재 인증된 사용자의 nickname", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "만료·위변조 token 또는 ACCOUNT_WITHDRAWN", content = @Content),
            @ApiResponse(responseCode = "403", description = "인증 누락 NO_PERMISSION 또는 USER_INACTIVE", content = @Content),
            @ApiResponse(responseCode = "404", description = "계정 없음 USER_NOT_FOUND", content = @Content)
    })
    ApiResult<NicknameResponse> me(@Parameter(hidden = true) PrincipalDetails principal);

    @Operation(summary = "회원 탈퇴", description = "DELETE /api/user/me. ROLE_USER AccessToken 인증 필수. 요청 본문과 userId parameter가 없다. "
            + "성공 시 빈 ApiResult와 만료된 access/refresh 쿠키를 반환한다. 재요청은 기존 token의 계정 상태 검사에서 ACCOUNT_WITHDRAWN으로 거부된다. "
            + "오류 JSON의 exceptionCode는 custom_error_code이고 OAuth redirect의 error_code와 다르다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "탈퇴 성공; ApiResult.success=true, response=null", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "탈퇴 계정 ACCOUNT_WITHDRAWN, 만료·위변조 token: TOKEN_HAS_EXPIRED, TOKEN_HAS_TEMPERED. 필터 ExceptionResponse.exceptionCode/message/status", content = @Content),
            @ApiResponse(responseCode = "403", description = "인증 누락 NO_PERMISSION 또는 탈퇴 외 비활성 계정 USER_INACTIVE", content = @Content),
            @ApiResponse(responseCode = "404", description = "계정 없음 USER_NOT_FOUND", content = @Content)
    })
    ApiResult<Void> withdraw(@Parameter(hidden = true) Long userId,
            @Parameter(hidden = true) jakarta.servlet.http.HttpServletResponse response);

    @Operation(summary = "내 닉네임 변경", description = "PATCH /api/user/me/nickname. ROLE_USER AccessToken 인증 필수. "
            + "인증 principal의 userId만 변경한다. 닉네임은 1~20자이고 공백을 포함할 수 없다. "
            + "수동 닉네임은 입력 그대로 저장한다. 현재 닉네임 재요청도 성공한다. "
            + "성공 응답은 ApiResult.response.nickname이다. 오류의 exceptionCode는 custom_error_code이며 OAuth redirect의 error_code와 구분한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공, 또는 NICKNAME_INVALID(논리 status 400), NICKNAME_ALREADY_EXISTS(논리 status 409), USER_INACTIVE(논리 status 403), USER_NOT_FOUND(논리 status 404). 오류는 ApiResult.success=false, response.exceptionCode/message/status 형태다.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "JSON body 누락 또는 파싱 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "만료·위변조 AccessToken: TOKEN_HAS_EXPIRED, TOKEN_HAS_TEMPERED. 필터 ExceptionResponse.exceptionCode/message/status", content = @Content),
            @ApiResponse(responseCode = "403", description = "인증 정보가 없으면 NO_PERMISSION", content = @Content)
    })
    ApiResult<NicknameResponse> changeNickname(@Parameter(hidden = true) Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "필수 nickname 문자열. 1~20자, 공백 불가", required = true)
            NicknameRequest request);

    @Operation(summary = "Access token 재발급", description = "POST /api/user/reissue/accessToken. 별도 Authorization 헤더 없이 body의 현재 RefreshToken으로 요청한다. "
            + "refreshToken은 필수이며 Redis에 저장된 현재 토큰과 같아야 한다. 성공 응답은 ApiResult.response.newAccessToken이다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "body 누락 또는 refreshToken 공백", content = @Content),
            @ApiResponse(responseCode = "401", description = "TOKEN_HAS_EXPIRED, TOKEN_HAS_TEMPERED, INVALID_REFRESH_TOKEN, GOOGLE_RELOGIN_REQUIRED. "
                    + "ApiResult.success=false, response.exceptionCode/message/status=401", content = @Content)
    })
    ApiResult<ReissueAccessTokenResponse> reissueAccessToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "필수 refreshToken 문자열", required = true)
            ReissueAccessTokenRequest request);

    @Operation(summary = "Refresh token 갱신", description = "POST /api/user/reissue/refreshToken. 유효한 AccessToken 인증이 필요하다. "
            + "이전 refresh token은 더 이상 access 재발급에 사용할 수 없다. 성공 응답은 ApiResult.response.newRefreshToken이다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "갱신 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "TOKEN_HAS_EXPIRED, TOKEN_HAS_TEMPERED, INVALID_REFRESH_TOKEN, GOOGLE_RELOGIN_REQUIRED. "
                    + "필터의 ExceptionResponse.exceptionCode/message/status=401", content = @Content),
            @ApiResponse(responseCode = "403", description = "인증 정보가 없으면 NO_PERMISSION, 비활성 계정은 USER_INACTIVE", content = @Content),
            @ApiResponse(responseCode = "404", description = "계정이 없으면 USER_NOT_FOUND", content = @Content)
    })
    ApiResult<ReissueRefreshTokenResponse> reissueRefreshToken(@Parameter(hidden = true) Long userId);

    @Operation(summary = "로그아웃", description = "POST /api/user/logout. 유효한 AccessToken 인증이 필요하다. "
            + "서버의 refresh token을 삭제한다. 기존 access token은 만료까지 유효하다. 성공 응답은 ApiResult.success=true다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "TOKEN_HAS_EXPIRED, TOKEN_HAS_TEMPERED, INVALID_REFRESH_TOKEN, GOOGLE_RELOGIN_REQUIRED. "
                    + "필터의 ExceptionResponse.exceptionCode/message/status=401", content = @Content),
            @ApiResponse(responseCode = "403", description = "인증 정보가 없으면 NO_PERMISSION, 비활성 계정은 USER_INACTIVE", content = @Content),
            @ApiResponse(responseCode = "404", description = "계정이 없으면 USER_NOT_FOUND", content = @Content)
    })
    ApiResult<Void> logout(@Parameter(hidden = true) Long userId);
}
