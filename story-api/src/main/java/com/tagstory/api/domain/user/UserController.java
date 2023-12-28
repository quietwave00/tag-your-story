package com.tagstory.api.domain.user;

import com.tagstory.api.annotations.CurrentPendingUserId;
import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.user.dto.request.RegisterRequest;
import com.tagstory.api.domain.user.dto.request.ReissueAccessTokenRequest;
import com.tagstory.api.domain.user.dto.response.RegisterResponse;
import com.tagstory.api.domain.user.dto.response.ReissueAccessTokenResponse;
import com.tagstory.api.domain.user.dto.response.ReissueRefreshTokenResponse;
import com.tagstory.core.domain.user.service.UserService;
import com.tagstory.core.domain.user.service.dto.response.Token;
import com.tagstory.core.domain.user.service.dto.response.User;
import com.tagstory.core.utils.api.ApiUtils;
import com.tagstory.core.utils.api.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/test")
    public ApiResult<String> test() {
        return ApiUtils.success("success");
    }

    /*
     * AccessToken을 재발급한다.
     */
    @PreAuthorize("hasRole('USER')")
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
    public ApiResult<Void> logout() {
        userService.logout();
        return ApiUtils.success();
    }

    /*
     * 회원가입을 완료한다.
     */
    @PreAuthorize("hasRole('PENDING_USER')")
    @PostMapping("/register")
    public ApiResult<RegisterResponse> register(@RequestBody @Valid RegisterRequest request,
                                                @CurrentPendingUserId String pendingUserId) {
        User response = userService.register(request.toCommand(pendingUserId));
        return ApiUtils.success(RegisterResponse.from(response));
    }
}
