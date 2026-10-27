package com.tagnote.api.domain.notification;

import com.tagnote.api.auth.PrincipalDetails;
import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.core.domain.notification.service.NotificationFacade;
import com.tagnote.core.domain.user.Role;
import com.tagnote.infrastructure.notification.sse.SseNotificationTransport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(WebMvcMethodSecurityTestConfig.class)
class NotificationSseControllerTest {
    @Autowired MockMvc mvc;
    @MockBean NotificationFacade facade;
    @MockBean SseNotificationTransport transport;
    private final PrincipalDetails principal = new PrincipalDetails(7L, Role.ROLE_USER);

    @Test
    void 알림_구독은_인증과_이벤트_형식과_초기_이벤트를_유지한다() throws Exception {
        SseEmitter emitter = new SseEmitter();
        emitter.send(SseEmitter.event().name("init").data("subscribe"));
        when(transport.open(7L)).thenReturn(emitter);

        mvc.perform(get("/api/notification/subscription").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:init")));
        verify(transport).open(7L);
        mvc.perform(get("/api/notification/subscription")).andExpect(status().isForbidden());
    }

    @Test
    void 알림_목록과_건수와_모두_읽음_경로를_유지한다() throws Exception {
        when(facade.getNotificationList(7L, 0)).thenReturn(List.of());
        when(facade.getNotificationCount(7L)).thenReturn(3);
        mvc.perform(get("/api/notification").with(user(principal)).param("page", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response").isArray());
        mvc.perform(get("/api/notification/count").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response.count").value(3));
        mvc.perform(patch("/api/notification/all").with(user(principal)))
                .andExpect(status().isOk());
        verify(facade).setAllAsRead(7L);
    }
}
