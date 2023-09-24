package com.tagstory.api.domain.user;

import com.tagstory.api.annotations.CurrentTempId;
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

    @PreAuthorize("hasRole('ROLE_GUEST')")
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
        ReissueAccessToken reissueAccessTokenResponse = userService.reissueAccessToken(reissueAccessTokenRequest.toCommand());
        return ApiUtils.success(ReissueAccessTokenResponse.from(reissueAccessTokenResponse));
    }

    /*
     * RefreshToken을 재발급한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/user/reissue/refreshToken")
    public ApiResult<ReissueRefreshTokenResponse> reissueRefreshToken(@CurrentUserId Long userId) {
        ReissueRefreshToken reissueRefreshTokenResponse = userService.reissueRefreshToken(userId);
        return ApiUtils.success(ReissueRefreshTokenResponse.from(reissueRefreshTokenResponse));
    }

    /*
     * 로그아웃을 수행한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/logout")
    public ApiResult<LogoutResponse> logout(@CurrentUserId Long userId) {
        Logout logoutResponse = userService.logout(userId);
        return ApiUtils.success(LogoutResponse.from(logoutResponse));
    }

    /*
     * 닉네임을 설정해준다.
     */
    @PreAuthorize("hasRole('ROLE_PENDING_USER')")
    @PatchMapping("/nicknames")
    public ApiResult<UpdateNicknameResponse> updateNickname(@RequestBody UpdateNicknameRequest updateNicknameRequest, @CurrentTempId String tempId) {
        UpdateNickname updateNicknameResponse = userService.updateNickname(updateNicknameRequest.toCommand(tempId));
        return ApiUtils.success(UpdateNicknameResponse.from(updateNicknameResponse));
    }
}
