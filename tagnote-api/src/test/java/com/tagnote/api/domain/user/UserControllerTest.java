package com.tagnote.api.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.api.auth.PrincipalDetails;
import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.domain.user.service.UserService;
import com.tagnote.core.domain.user.service.dto.response.Token;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void POST_api_user_reissue_accessToken은_newAccessToken_field를_유지한다() throws Exception {
        when(userService.reissueAccessToken(any())).thenReturn(Token.onComplete("new-access-token"));

        mockMvc.perform(post("/api/user/reissue/accessToken")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER)))
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

        verify(userService).logout();
    }

    @Test
    void POST_api_user_register는_CurrentPendingUserId를_사용한다() throws Exception {
        when(userService.register(any()))
                .thenReturn(User.builder().pendingUserId("pending-1").nickname("tagger").role(Role.ROLE_USER).build());

        mockMvc.perform(post("/api/user/register")
                        .with(user(new PrincipalDetails("pending-1", Role.ROLE_PENDING_USER, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterPayload("tagger"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.nickname").value("tagger"));
    }

    @Test
    void GET_api_user_test는_guest_role_requirements를_유지한다() throws Exception {
        mockMvc.perform(get("/api/user/test")
                        .with(user(new PrincipalDetails(null, Role.ROLE_GUEST))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").value("success"));
    }

    @Test
    void POST_api_user_reissue_accessToken은_인증없이는_차단된다() throws Exception {
        mockMvc.perform(post("/api/user/reissue/accessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenPayload("refresh-token"))))
                .andExpect(status().isForbidden());
    }

    private record RefreshTokenPayload(String refreshToken) {
    }

    private record RegisterPayload(String nickname) {
    }
}
