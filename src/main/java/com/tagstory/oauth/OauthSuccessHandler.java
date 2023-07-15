package com.tagstory.oauth;

import com.tagstory.auth.PrincipalDetails;
import com.tagstory.entity.User;
import com.tagstory.jwt.JwtCookieProvider;
import com.tagstory.jwt.JwtUtil;
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

    private final JwtUtil jwtUtil;
    private final JwtCookieProvider jwtCookieProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser();
        String jwt = jwtUtil.generateToken(user);

        response.addCookie(jwtCookieProvider.generateCookie(jwt));
    }
}
