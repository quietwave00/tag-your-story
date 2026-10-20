package com.tagnote.api.oauth;

import com.tagnote.api.auth.PrincipalDetails;
import com.tagnote.core.domain.user.service.GoogleAccountService;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    private final GoogleAccountService accounts;

    /*
     * 사용자 정보를 가져오고, 정보를 토대로 회원가입 여부를 체크한다.
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        // Google HTTP userinfo completes before the short account write transaction.
        return login(super.loadUser(request).getAttributes());
    }

    PrincipalDetails login(Map<String, Object> attributes) {
        if (!Boolean.TRUE.equals(attributes.get("email_verified"))) {
            throw oauthError("GOOGLE_EMAIL_UNVERIFIED");
        }
        if (!(attributes.get("sub") instanceof String subject) || subject.isBlank()) {
            throw oauthError("GOOGLE_IDENTITY_INVALID");
        }
        if (!(attributes.get("email") instanceof String email) || email.isBlank()) {
            throw oauthError("GOOGLE_EMAIL_UNVERIFIED");
        }
        try {
            User user = accounts.loginOrCreate(subject, email);
            return new PrincipalDetails(user.getUserId(), user.getRole(), attributes);
        } catch (CustomException e) {
            throw oauthError(e.getExceptionCode().name());
        }
    }

    private OAuth2AuthenticationException oauthError(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), code);
    }
}
