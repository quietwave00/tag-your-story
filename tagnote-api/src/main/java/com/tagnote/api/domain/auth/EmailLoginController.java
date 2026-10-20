package com.tagnote.api.domain.auth;

import com.tagnote.api.jwt.JwtCookieProvider;
import com.tagnote.application.auth.EmailLoginService;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.utils.api.ApiResult;
import com.tagnote.core.utils.api.ApiUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class EmailLoginController implements EmailLoginApi {
    private final EmailLoginService login;
    private final SignupTokenService tokens;
    private final JwtCookieProvider cookies;

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest body,
                                          HttpServletRequest request, HttpServletResponse response) {
        User user = login.login(body.email(), body.password(), request.getRemoteAddr());
        SignupTokenService.Tokens issued = tokens.issue(user);
        response.addCookie(cookies.generateAccessTokenCookie(issued.access()));
        response.addCookie(cookies.generateRefreshTokenCookie(issued.refresh()));
        return ApiUtils.success(new LoginResponse(user.getUserId(), user.getNickname()));
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) { }
    public record LoginResponse(Long userId, String nickname) { }
}
