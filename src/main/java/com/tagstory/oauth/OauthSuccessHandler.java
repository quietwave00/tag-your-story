package com.tagstory.oauth;

import com.tagstory.auth.PrincipalDetails;
import com.tagstory.entity.User;
import com.tagstory.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OauthSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser();
        String jwt = jwtProvider.generateToken(user);
        getRedirectStrategy().sendRedirect(request, response, getRedirectUrl(jwt));
    }
    private String getRedirectUrl(String token) {
        return UriComponentsBuilder.fromUriString("http://localhost:5500/html/index.html")
                .queryParam("token", token)
                .build().toUriString();
    }
}
