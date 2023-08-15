package com.tagstory.user.api;

import com.tagstory.annotations.CurrentUserId;
import com.tagstory.user.api.dto.request.ReissueAccessTokenRequest;
import com.tagstory.user.api.dto.request.UpdateNicknameRequest;
import com.tagstory.user.api.dto.response.*;
import com.tagstory.user.mapper.UserControllerMapper;
import com.tagstory.user.service.UserService;
import com.tagstory.user.service.dto.ReissueAccessTokenCommand;
import com.tagstory.utils.ApiUtils;
import com.tagstory.utils.dto.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final UserControllerMapper userControllerMapper;

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/user/test")
    public ApiResult<String> test() {
        return ApiUtils.success("success");
    }

    /*
     * AccessToken을 재발급한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/user/reissue/jwt")
    public ApiResult<ReissueJwtResponse> reissueJwt(@RequestBody ReissueAccessTokenRequest reissueAccessTokenRequest) {
        ReissueJwtResponse reissueJwtResponse = userService.reissueJwt(userControllerMapper.toCommand(reissueAccessTokenRequest, ReissueAccessTokenCommand.class));
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

    /*
     * 닉네임을 설정해준다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PatchMapping("/nicknames")
    public ApiResult<UpdateNicknameResponse> updateNickname(@CurrentUserId Long userId, @RequestBody UpdateNicknameRequest updateNicknameRequest) {
        UpdateNicknameResponse updateNicknameResponse = userService.updateNickname(updateNicknameRequest, userId);
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
