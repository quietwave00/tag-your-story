package com.tagnote.api.domain.user;

import com.tagnote.api.annotations.CurrentUserId;
import com.tagnote.api.domain.user.dto.request.ReissueAccessTokenRequest;
import com.tagnote.api.domain.user.dto.response.ReissueAccessTokenResponse;
import com.tagnote.api.domain.user.dto.response.ReissueRefreshTokenResponse;
import com.tagnote.application.user.NicknameChangeService;
import com.tagnote.application.user.AccountWithdrawalService;
import com.tagnote.api.jwt.JwtCookieProvider;
import com.tagnote.core.domain.user.service.UserService;
import com.tagnote.core.domain.user.service.dto.response.Token;
import com.tagnote.core.utils.api.ApiUtils;
import com.tagnote.core.utils.api.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController implements UserApi {

    private final UserService userService;
    private final NicknameChangeService nicknameChangeService;
    private final AccountWithdrawalService accountWithdrawalService;
    private final JwtCookieProvider jwtCookieProvider;

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/me")
    public ApiResult<Void> withdraw(@CurrentUserId Long userId, HttpServletResponse response) {
        accountWithdrawalService.withdraw(userId);
        response.addCookie(jwtCookieProvider.expireAccessTokenCookie());
        response.addCookie(jwtCookieProvider.expireRefreshTokenCookie());
        SecurityContextHolder.clearContext();
        return ApiUtils.success();
    }

    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/me/nickname")
    public ApiResult<NicknameResponse> changeNickname(@CurrentUserId Long userId,
                                                       @RequestBody NicknameRequest request) {
        return ApiUtils.success(new NicknameResponse(nicknameChangeService.change(userId, request.nickname())));
    }

    /*
     * AccessToken을 재발급한다.
     */
    @PostMapping("/reissue/accessToken")
    public ApiResult<ReissueAccessTokenResponse> reissueAccessToken(@RequestBody @Valid ReissueAccessTokenRequest request) {
        Token response = userService.reissueAccessToken(request.toCommand());
        return ApiUtils.success(ReissueAccessTokenResponse.from(response));
    }

    /*
     * RefreshToken을 재발급한다.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/reissue/refreshToken")
    public ApiResult<ReissueRefreshTokenResponse> reissueRefreshToken(@CurrentUserId Long userId) {
        Token response = userService.reissueRefreshToken(userId);
        return ApiUtils.success(ReissueRefreshTokenResponse.from(response));
    }

    /*
     * 로그아웃을 수행한다.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/logout")
    public ApiResult<Void> logout(@CurrentUserId Long userId) {
        userService.logout(userId);
        return ApiUtils.success();
    }

}
