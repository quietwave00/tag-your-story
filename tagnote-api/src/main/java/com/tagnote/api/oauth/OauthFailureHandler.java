package com.tagnote.api.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OauthFailureHandler implements AuthenticationFailureHandler {
    @Value("${api.redirect-url}")
    private String redirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String code = exception instanceof OAuth2AuthenticationException oauth
                ? oauth.getError().getErrorCode() : "GOOGLE_SIGNUP_CONFLICT";
        if (!code.equals("GOOGLE_EMAIL_UNVERIFIED") && !code.equals("GOOGLE_IDENTITY_INVALID")
                && !code.equals("GOOGLE_EMAIL_CONFLICT_LOCAL")
                && !code.equals("GOOGLE_EMAIL_CONFLICT_GOOGLE") && !code.equals("USER_INACTIVE")
                && !code.equals("GOOGLE_SIGNUP_CONFLICT")) {
            code = "GOOGLE_SIGNUP_CONFLICT";
        }
        String separator = redirectUrl.contains("?") ? "&" : "?";
        response.sendRedirect(redirectUrl + separator + "error_code=" + code);
    }
}
