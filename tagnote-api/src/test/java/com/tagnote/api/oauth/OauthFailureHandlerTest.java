package com.tagnote.api.oauth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OauthFailureHandlerTest {
    @Test
    void provider_conflict_redirects_with_stable_error_code() throws Exception {
        OauthFailureHandler handler = new OauthFailureHandler();
        ReflectionTestUtils.setField(handler, "redirectUrl", "http://localhost/token.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new OAuth2AuthenticationException(new OAuth2Error("GOOGLE_EMAIL_CONFLICT_LOCAL")));

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost/token.html?error_code=GOOGLE_EMAIL_CONFLICT_LOCAL");
    }
}
