package com.tagnote.api.domain.like;

import com.tagnote.api.auth.PrincipalDetails;
import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.application.like.LikeCommandService;
import com.tagnote.application.like.LikeQueryService;
import com.tagnote.application.like.LikedBoardItem;
import com.tagnote.application.like.LikedBoardPage;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LikeController.class)
@Import(WebMvcMethodSecurityTestConfig.class)
class LikeControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private LikeCommandService commands;
    @MockBean private LikeQueryService queries;

    private final PrincipalDetails principal = new PrincipalDetails(7L, Role.ROLE_USER);

    @Test
    void 기존_등록_취소_상태의_요청과_응답을_유지한다() throws Exception {
        when(commands.like("board-1", 7L)).thenReturn(true);
        when(queries.isLiked("board-1", 7L)).thenReturn(true);

        mvc.perform(post("/api/likes").with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"boardId\":\"board-1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").value(true));
        mvc.perform(get("/api/likes/status/board-1").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response.liked").value(true));
        mvc.perform(delete("/api/likes").with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"boardId\":\"board-1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(commands).unLike("board-1", 7L);
    }

    @Test
    void 새_경로도_같은_유스케이스를_사용한다() throws Exception {
        when(commands.like("board-1", 7L)).thenReturn(true);
        mvc.perform(post("/api/boards/board-1/likes").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response").value(true));
        mvc.perform(delete("/api/boards/board-1/likes").with(user(principal)))
                .andExpect(status().isOk());
        verify(commands).like("board-1", 7L);
        verify(commands).unLike("board-1", 7L);
    }

    @Test
    void 중복_등록은_409와_기존_custom_error_code_형식이다() throws Exception {
        when(commands.like("board-1", 7L)).thenThrow(new CustomException(ExceptionCode.LIKE_ALREADY_EXISTS));

        mvc.perform(post("/api/likes").with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"boardId\":\"board-1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.response.exceptionCode").value("LIKE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.response.status").value(409));
    }

    @Test
    void 상태와_목록은_인증_사용자만_조회한다() throws Exception {
        when(queries.isLiked("missing", 7L)).thenReturn(false);
        when(queries.likedBoards(7L, 0)).thenReturn(new LikedBoardPage(
                List.of(new LikedBoardItem("board-1", "탈퇴한 사용자", "본문")), 1));

        mvc.perform(get("/api/likes/status/missing").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response.liked").value(false));
        mvc.perform(get("/api/users/me/liked-boards").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response.items[0].boardId").value("board-1"))
                .andExpect(jsonPath("$.response.items[0].author").value("탈퇴한 사용자"))
                .andExpect(jsonPath("$.response.items[0].content").value("본문"))
                .andExpect(jsonPath("$.response.items[0].likeCount").doesNotExist())
                .andExpect(jsonPath("$.response.totalCount").value(1));
        verify(queries).likedBoards(7L, 0);
        mvc.perform(get("/api/users/me/liked-boards")).andExpect(status().isForbidden());
        mvc.perform(post("/api/likes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"boardId\":\"board-1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 빈_목록과_페이지_입력을_처리한다() throws Exception {
        when(queries.likedBoards(7L, 2)).thenReturn(new LikedBoardPage(List.of(), 0));
        mvc.perform(get("/api/users/me/liked-boards").with(user(principal)).param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.items").isEmpty())
                .andExpect(jsonPath("$.response.totalCount").value(0));
        mvc.perform(get("/api/users/me/liked-boards").with(user(principal)).param("page", "-1"))
                .andExpect(status().isBadRequest());
    }
}
