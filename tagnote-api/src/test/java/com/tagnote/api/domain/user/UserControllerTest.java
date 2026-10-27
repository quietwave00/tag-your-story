package com.tagnote.api.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.api.auth.PrincipalDetails;
import com.tagnote.application.user.NicknameChangeService;
import com.tagnote.application.user.AccountWithdrawalService;
import com.tagnote.api.jwt.JwtCookieProvider;
import jakarta.servlet.http.Cookie;
import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.service.UserService;
import com.tagnote.core.domain.user.service.dto.response.Token;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(WebMvcMethodSecurityTestConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private NicknameChangeService nicknameChangeService;

    @MockBean
    private AccountWithdrawalService accountWithdrawalService;

    @MockBean
    private JwtCookieProvider jwtCookieProvider;

    @Test
    void 내_프로필_조회는_인증된_사용자의_닉네임만_반환한다() throws Exception {
        mockMvc.perform(get("/api/user/me?userId=2")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER, "GoogleListener"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.nickname").value("GoogleListener"))
                .andExpect(jsonPath("$.response.userId").doesNotExist())
                .andExpect(jsonPath("$.response.email").doesNotExist());
        org.mockito.Mockito.verifyNoInteractions(userService);
    }

    @Test
    void 내_프로필_조회는_인증이_필요하다() throws Exception {
        mockMvc.perform(get("/api/user/me")).andExpect(status().isForbidden());
    }

    @Test
    void withdrawal_uses_only_principal_id_and_expires_both_cookies() throws Exception {
        Cookie access = new Cookie("Authorization", "");
        access.setMaxAge(0);
        Cookie refresh = new Cookie("Refresh", "");
        refresh.setMaxAge(0);
        when(jwtCookieProvider.expireAccessTokenCookie()).thenReturn(access);
        when(jwtCookieProvider.expireRefreshTokenCookie()).thenReturn(refresh);

        mockMvc.perform(delete("/api/user/me?userId=2")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                        .maxAge("Authorization", 0))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                        .maxAge("Refresh", 0));
        verify(accountWithdrawalService).withdraw(1L);
    }

    @Test
    void withdrawal_requires_authentication() throws Exception {
        mockMvc.perform(delete("/api/user/me")).andExpect(status().isForbidden());
        org.mockito.Mockito.verifyNoInteractions(accountWithdrawalService);
    }

    @Test
    void nickname_change_uses_authenticated_user_id_and_returns_confirmed_value() throws Exception {
        when(nicknameChangeService.change(1L, "새닉네임")).thenReturn("새닉네임");

        mockMvc.perform(patch("/api/user/me/nickname")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새닉네임\",\"userId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.nickname").value("새닉네임"));
        verify(nicknameChangeService).change(1L, "새닉네임");
    }

    @Test
    void nickname_change_requires_authentication() throws Exception {
        mockMvc.perform(patch("/api/user/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새닉네임\"}"))
                .andExpect(status().isForbidden());
        org.mockito.Mockito.verifyNoInteractions(nicknameChangeService);
    }

    @Test
    void nickname_change_rejects_missing_body() throws Exception {
        mockMvc.perform(patch("/api/user/me/nickname")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_api_user_reissue_accessToken은_newAccessToken_field를_유지한다() throws Exception {
        when(userService.reissueAccessToken(any())).thenReturn(Token.onComplete("new-access-token"));

        mockMvc.perform(post("/api/user/reissue/accessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenPayload("refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.newAccessToken").value("new-access-token"));
    }

    @Test
    void POST_api_user_reissue_refreshToken은_CurrentUserId를_사용한다() throws Exception {
        when(userService.reissueRefreshToken(1L)).thenReturn(Token.onComplete("new-refresh-token"));

        mockMvc.perform(post("/api/user/reissue/refreshToken")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.newRefreshToken").value("new-refresh-token"));
    }

    @Test
    void POST_api_user_logout_route를_유지한다() throws Exception {
        mockMvc.perform(post("/api/user/logout")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).logout(1L);
    }

    @Test
    void removed_register_and_guest_test_routes_are_404() throws Exception {
        mockMvc.perform(post("/api/user/register")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/user/test")).andExpect(status().isNotFound());
    }

    @Test
    void POST_api_user_reissue_accessToken은_body가_없으면_거부된다() throws Exception {
        mockMvc.perform(post("/api/user/reissue/accessToken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_api_user_reissue_accessToken은_유효하지_않은_refresh를_401로_응답한다() throws Exception {
        when(userService.reissueAccessToken(any()))
                .thenThrow(new CustomException(ExceptionCode.INVALID_REFRESH_TOKEN));

        mockMvc.perform(post("/api/user/reissue/accessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenPayload("old-refresh-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.response.exceptionCode").value("INVALID_REFRESH_TOKEN"));
    }

    private record RefreshTokenPayload(String refreshToken) {
    }

}
