package com.tagnote.api.domain.board;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.api.auth.PrincipalDetails;
import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.core.domain.board.BoardOrderType;
import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.board.service.BoardFacade;
import com.tagnote.core.domain.board.service.dto.BoardList;
import com.tagnote.core.domain.boardusertag.service.dto.UserTagNames;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.service.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BoardController.class)
@Import(WebMvcMethodSecurityTestConfig.class)
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BoardFacade boardFacade;

    @Test
    void POST_api_boards는_success_response_wrapper를_유지한다() throws Exception {
        Board board = board(
                "board-1",
                "board content",
                "nickname",
                UserTagNames.ofNameList(List.of("tag-1", "tag-2"))
        );
        when(boardFacade.create(any())).thenReturn(board);

        mockMvc.perform(post("/api/boards")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBoardPayload("board content", "track-1", List.of("tag-1", "tag-2")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.boardId").value("board-1"))
                .andExpect(jsonPath("$.response.nickname").value("nickname"))
                .andExpect(jsonPath("$.response.content").value("board content"))
                .andExpect(jsonPath("$.response.userTagList.nameList[0]").value("tag-1"));
    }

    @Test
    void POST_api_boards는_인증이_없으면_차단된다() throws Exception {
        mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBoardPayload("board content", "track-1", List.of("tag-1")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void GET_api_boards_trackId는_order_type과_page_query를_유지한다() throws Exception {
        BoardList boardList = BoardList.of(List.of(board("board-1", "content", "nickname", UserTagNames.ofNameList(List.of("tag-1")))), 1L);
        when(boardFacade.getBoardListByTrackId("track-1", BoardOrderType.CREATED_AT, 0)).thenReturn(boardList);

        mockMvc.perform(get("/api/boards/{trackId}", "track-1")
                        .param("order-type", "CREATED_AT")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.boardResponseList[0].boardId").value("board-1"))
                .andExpect(jsonPath("$.response.boardResponseList[0].nickname").value("nickname"))
                .andExpect(jsonPath("$.response.totalCount").value(1));
    }

    @Test
    void GET_api_boards는_boardId_query로_상세를_반환한다() throws Exception {
        when(boardFacade.getDetailBoard("board-1"))
                .thenReturn(board("board-1", "detail content", "nickname", UserTagNames.ofNameList(List.of("tag-1"))));

        mockMvc.perform(get("/api/boards").param("boardId", "board-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content").value("detail content"))
                .andExpect(jsonPath("$.response.nickname").value("nickname"))
                .andExpect(jsonPath("$.response.userTagNameList.nameList[0]").value("tag-1"));
    }

    @Test
    void GET_api_boards_count_trackId_route를_유지한다() throws Exception {
        when(boardFacade.getBoardCountByTrackId("track-1")).thenReturn(7);

        mockMvc.perform(get("/api/boards/count/{trackId}", "track-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.count").value(7));
    }

    @Test
    void GET_api_boards_user_tags는_userTagName_query를_유지한다() throws Exception {
        when(boardFacade.getBoardListByUserTagName("tag-1"))
                .thenReturn(List.of(board("board-1", "content", "nickname", UserTagNames.ofNameList(List.of("tag-1")))));

        mockMvc.perform(get("/api/boards/user-tags").param("userTagName", "tag-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response[0].boardId").value("board-1"))
                .andExpect(jsonPath("$.response[0].userTagNameList.nameList[0]").value("tag-1"));
    }

    @Test
    void GET_api_boards_auth_boardId는_boolean_response를_유지한다() throws Exception {
        when(boardFacade.isWriter("board-1", 1L)).thenReturn(true);

        mockMvc.perform(get("/api/boards/auth/{boardId}", "board-1")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").value(true));
    }

    @Test
    void PATCH_api_boards_route를_유지한다() throws Exception {
        when(boardFacade.updateBoardAndUserTag(any()))
                .thenReturn(board("board-1", "updated content", "nickname", UserTagNames.ofNameList(List.of("tag-1"))));

        mockMvc.perform(patch("/api/boards")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateBoardPayload("board-1", "updated content", List.of("tag-1")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.boardId").value("board-1"))
                .andExpect(jsonPath("$.response.content").value("updated content"));
    }

    @Test
    void DELETE_api_boards_boardId_route를_유지한다() throws Exception {
        mockMvc.perform(delete("/api/boards/{boardId}", "board-1")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(boardFacade).delete(eq("board-1"));
    }

    private Board board(String boardId, String content, String nickname, UserTagNames userTagNames) {
        return Board.builder()
                .boardId(boardId)
                .content(content)
                .createdAt(LocalDateTime.of(2026, 8, 11, 0, 0))
                .likeCount(3)
                .user(User.builder().userId(1L).nickname(nickname).build())
                .userTagNameList(userTagNames)
                .build();
    }

    private record CreateBoardPayload(String content, String trackId, List<String> userTagList) {
    }

    private record UpdateBoardPayload(String boardId, String content, List<String> userTagList) {
    }
}
