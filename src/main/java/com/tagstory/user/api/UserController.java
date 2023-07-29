package com.tagstory.user.api;

import com.tagstory.user.api.dto.request.ReissueJwtRequest;
import com.tagstory.user.api.dto.response.LogoutResponse;
import com.tagstory.user.api.dto.response.ReissueJwtResponse;
import com.tagstory.user.api.dto.response.ReissueRefreshTokenResponse;
import com.tagstory.user.service.UserService;
import com.tagstory.utils.ApiUtils;
import com.tagstory.annotations.CurrentUserId;
import com.tagstory.utils.dto.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/user/test")
    public ApiResult<String> test() {
        return ApiUtils.success("success");
    }

    /*
     * 토큰을 재발급한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/user/reissue/jwt")
    public ApiResult<ReissueJwtResponse> reissueJwt(@RequestBody ReissueJwtRequest reissueJwtRequest) {
        ReissueJwtResponse reissueJwtResponse = userService.reissueJwt(reissueJwtRequest);
        return ApiUtils.success(reissueJwtResponse);
    }

    /*
     * RefreshToken을 재발급한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/user/reissue/refreshToken")
    public ApiResult<ReissueRefreshTokenResponse> reissueRefreshToken(@CurrentUserId Long userId) {
        ReissueRefreshTokenResponse reissueRefreshTokenResponse = userService.reissueRefreshToken(userId);
        return ApiUtils.success(reissueRefreshTokenResponse);
    }

    /*
     * 로그아웃을 수행한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/logout")
    public ApiResult<LogoutResponse> logout(@CurrentUserId Long userId) {
        LogoutResponse logoutResponse = userService.logout(userId);
        return ApiUtils.success(logoutResponse);
    }


}
