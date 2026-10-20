package com.tagnote.api.domain.auth;

import com.tagnote.api.jwt.JwtCookieProvider;
import com.tagnote.application.auth.EmailSignupService;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.utils.api.ApiResult;
import com.tagnote.core.utils.api.ApiUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class EmailSignupController implements EmailSignupApi {
    private final EmailSignupService signup;
    private final SignupTokenService tokens;
    private final JwtCookieProvider cookies;

    @PostMapping("/signup/email/start")
    public ApiResult<StartResponse> start(@Valid @RequestBody StartRequest body, HttpServletRequest request) {
        return ApiUtils.success(new StartResponse(signup.start(body.email(), body.password(),
                body.passwordConfirm(), request.getRemoteAddr())));
    }

    @PostMapping("/signup/email/resend")
    public ApiResult<Void> resend(@Valid @RequestBody ResendRequest body, HttpServletRequest request) {
        signup.resend(body.challengeId(), request.getRemoteAddr());
        return ApiUtils.success();
    }

    @PostMapping("/signup/email/confirm")
    public ApiResult<ConfirmResponse> confirm(@Valid @RequestBody ConfirmRequest body, HttpServletResponse response) {
        User user = signup.confirm(body.challengeId(), body.code());
        SignupTokenService.Tokens issued = tokens.issue(user);
        response.addCookie(cookies.generateAccessTokenCookie(issued.access()));
        response.addCookie(cookies.generateRefreshTokenCookie(issued.refresh()));
        return ApiUtils.success(new ConfirmResponse(user.getUserId(), user.getNickname()));
    }

    @GetMapping("/email-availability")
    public ApiResult<AvailabilityResponse> availability(@RequestParam String email, HttpServletRequest request) {
        return ApiUtils.success(new AvailabilityResponse(signup.available(email, request.getRemoteAddr())));
    }

    public record StartRequest(@NotBlank String email, @NotBlank String password, @NotBlank String passwordConfirm) { }
    public record ResendRequest(@NotBlank String challengeId) { }
    public record ConfirmRequest(@NotBlank String challengeId, @NotBlank String code) { }
    public record StartResponse(String challengeId) { }
    public record ConfirmResponse(Long userId, String nickname) { }
    public record AvailabilityResponse(boolean available) { }
}
