package com.tagstory.user.api;

import com.tagstory.user.api.dto.request.ReissueJwtRequest;
import com.tagstory.user.api.dto.response.ReissueJwtResponse;
import com.tagstory.user.api.dto.response.ReissueRefreshTokenResponse;
import com.tagstory.user.service.UserService;
import com.tagstory.utils.ApiUtils;
import com.tagstory.utils.dto.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;


    @GetMapping("/user/test")
    public ApiResult<String> test() {
        return ApiUtils.success("success");
    }

    /*
     * 토큰을 재발급한다.
     */
    @PostMapping("/user/reissue/jwt")
    public ApiResult<ReissueJwtResponse> reissueJwt(@RequestBody ReissueJwtRequest reissueJwtRequest) {
        ReissueJwtResponse reissueJwtResponse = userService.reissueJwt(reissueJwtRequest);
        return ApiUtils.success(reissueJwtResponse);
    }

    /*
     * RefreshToken을 재발급한다.
     */
    @PostMapping("/user/reissue/refreshToken")
    public ApiResult<ReissueRefreshTokenResponse> reissueRefreshToken() {
        ReissueRefreshTokenResponse reissueRefreshTokenResponse = userService.reissueRefreshToken();
        return ApiUtils.success(reissueRefreshTokenResponse);
    }

}
