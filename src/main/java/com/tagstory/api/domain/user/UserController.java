package com.tagstory.api.domain.user;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.user.dto.request.ReissueAccessTokenRequest;
import com.tagstory.api.domain.user.dto.request.UpdateNicknameRequest;
import com.tagstory.api.domain.user.dto.response.*;
import com.tagstory.api.utils.ApiUtils;
import com.tagstory.api.utils.dto.ApiResult;
import com.tagstory.core.domain.user.service.UserService;
import com.tagstory.core.domain.user.service.dto.response.*;
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
    public ApiResult<ReissueAccessToken> reissueAccessToken(@RequestBody ReissueAccessTokenRequest reissueAccessTokenRequest) {
        ReissueAccessTokenResponse reissueAccessTokenResponse = userService.reissueAccessToken(reissueAccessTokenRequest.toCommand());
        return ApiUtils.success(ReissueAccessToken.create(reissueAccessTokenResponse));
    }

    /*
     * RefreshToken을 재발급한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/user/reissue/refreshToken")
    public ApiResult<ReissueRefreshToken> reissueRefreshToken(@CurrentUserId Long userId) {
        ReissueRefreshTokenResponse reissueRefreshTokenResponse = userService.reissueRefreshToken(userId);
        return ApiUtils.success(ReissueRefreshToken.create(reissueRefreshTokenResponse));
    }

    /*
     * 로그아웃을 수행한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/logout")
    public ApiResult<Logout> logout(@CurrentUserId Long userId) {
        LogoutResponse logoutResponse = userService.logout(userId);
        return ApiUtils.success(Logout.create(logoutResponse));
    }

    /*
     * 닉네임을 설정해준다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PatchMapping("/nicknames")
    public ApiResult<UpdateNickname> updateNickname(@CurrentUserId Long userId, @RequestBody UpdateNicknameRequest updateNicknameRequest) {
        UpdateNicknameResponse updateNicknameResponse = userService.updateNickname(updateNicknameRequest.toCommand(), userId);
        return ApiUtils.success(UpdateNickname.create(updateNicknameResponse));
    }

    /*
     * 회원가입한 회원인지 로그인한 회원인지 상태를 체크한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/check-registration")
    public ApiResult<CheckRegisterUser> checkRegisterUser(@CurrentUserId Long userId) {
        CheckRegisterUserResponse checkRegisterUserResponse = userService.checkRegisterUser(userId);
        return ApiUtils.success(CheckRegisterUser.create(checkRegisterUserResponse));
    }
}
