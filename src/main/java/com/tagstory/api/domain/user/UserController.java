package com.tagstory.api.domain.user;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.user.dto.request.ReissueAccessTokenRequest;
import com.tagstory.api.domain.user.dto.request.UpdateNicknameRequest;
import com.tagstory.api.utils.ApiUtils;
import com.tagstory.api.utils.dto.ApiResult;
import com.tagstory.core.domain.user.service.UserService;
import com.tagstory.core.domain.user.service.dto.response.*;
import com.tagstory.core.domain.user.service.dto.response.ReissueAccessTokenResponse;
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
     * AccessToken을 재발급한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/user/reissue/accessToken")
    public ApiResult<ReissueAccessTokenResponse> reissueAccessToken(@RequestBody ReissueAccessTokenRequest reissueAccessTokenRequest) {
        ReissueAccessTokenResponse reissueAccessTokenResponse = userService.reissueAccessToken(reissueAccessTokenRequest.toCommand());
        return ApiUtils.success(reissueAccessTokenResponse);
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

    /*
     * 닉네임을 설정해준다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PatchMapping("/nicknames")
    public ApiResult<UpdateNicknameResponse> updateNickname(@CurrentUserId Long userId, @RequestBody UpdateNicknameRequest updateNicknameRequest) {
        UpdateNicknameResponse updateNicknameResponse = userService.updateNickname(updateNicknameRequest.toCommand(), userId);
        return ApiUtils.success(updateNicknameResponse);
    }

    /*
     * 회원가입한 회원인지 로그인한 회원인지 상태를 체크한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/check-registration")
    public ApiResult<CheckRegisterUserResponse> checkRegisterUser(@CurrentUserId Long userId) {
        CheckRegisterUserResponse checkRegisterUserResponse = userService.checkRegisterUser(userId);
        return ApiUtils.success(checkRegisterUserResponse);
    }
}
