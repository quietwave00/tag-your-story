package com.tagnote.api.domain.board;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.api.auth.PrincipalDetails;
import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.application.board.BoardCommandService;
import com.tagnote.application.board.BoardQueryService;
import com.tagnote.application.board.model.BoardPage;
import com.tagnote.application.board.model.BoardSubject;
import com.tagnote.application.board.model.BoardSystemTag;
import com.tagnote.application.board.model.BoardView;
import com.tagnote.core.domain.board.BoardOrderType;
import com.tagnote.core.domain.user.Role;
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
    private BoardCommandService commandService;

    @MockBean
    private BoardQueryService queryService;

    @Test
    void Track_Board_작성은_기존_trackId와_response_wrapper를_유지한다() throws Exception {
        when(commandService.create(any())).thenReturn(trackBoard("board-1"));

        mockMvc.perform(post("/api/boards")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBoardPayload(
                                "content", "spotify-track", null, List.of("tag-1")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.boardId").value("board-1"))
                .andExpect(jsonPath("$.response.subjectType").value("TRACK"))
                .andExpect(jsonPath("$.response.trackId").value("spotify-track"))
                .andExpect(jsonPath("$.response.albumId").isEmpty())
                .andExpect(jsonPath("$.response.systemTags[0].name").value("Ambient"));
    }

    @Test
    void Album_Board를_albumId로_작성한다() throws Exception {
        when(commandService.create(any())).thenReturn(albumBoard("board-2"));

        mockMvc.perform(post("/api/boards")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBoardPayload(
                                "content", null, "spotify-album", List.of("정주행")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.subjectType").value("ALBUM"))
                .andExpect(jsonPath("$.response.albumId").value("spotify-album"))
                .andExpect(jsonPath("$.response.trackId").isEmpty());
    }

    @Test
    void trackId와_albumId가_둘다_있으면_400이다() throws Exception {
        mockMvc.perform(post("/api/boards")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBoardPayload(
                                "content", "track", "album", List.of("tag")
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void trackId와_albumId가_둘다_없으면_400이다() throws Exception {
        mockMvc.perform(post("/api/boards")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBoardPayload(
                                "content", null, null, List.of("tag")
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void Board_작성은_인증이_없으면_차단된다() throws Exception {
        mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBoardPayload(
                                "content", "track", null, List.of("tag")
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void Track_Board_목록은_익명_조회할수있다() throws Exception {
        when(queryService.getTrackBoards("spotify-track", BoardOrderType.CREATED_AT, 0))
                .thenReturn(new BoardPage(List.of(trackBoard("board-1")), 1));

        mockMvc.perform(get("/api/boards/{trackId}", "spotify-track")
                        .param("order-type", "CREATED_AT")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.boardResponseList[0].subjectType").value("TRACK"))
                .andExpect(jsonPath("$.response.totalCount").value(1));
    }

    @Test
    void Album_Board_목록과_count는_익명_조회할수있다() throws Exception {
        when(queryService.getAlbumBoards("spotify-album", BoardOrderType.LIKE, 0))
                .thenReturn(new BoardPage(List.of(albumBoard("board-2")), 1));
        when(queryService.countAlbumBoards("spotify-album")).thenReturn(1);

        mockMvc.perform(get("/api/boards/albums/{albumId}", "spotify-album")
                        .param("order-type", "LIKE")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.boardResponseList[0].subjectType").value("ALBUM"));

        mockMvc.perform(get("/api/boards/count/albums/{albumId}", "spotify-album"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.count").value(1));
    }

    @Test
    void Track_Board_count는_기존_route로_익명_조회한다() throws Exception {
        when(queryService.countTrackBoards("spotify-track")).thenReturn(7);

        mockMvc.perform(get("/api/boards/count/{trackId}", "spotify-track"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.count").value(7));
    }

    @Test
    void 상세과_UserTag_조회는_익명_접근을_유지한다() throws Exception {
        when(queryService.getDetail("board-1")).thenReturn(trackBoard("board-1"));
        when(queryService.getBoardsByUserTagName("tag-1")).thenReturn(List.of(trackBoard("board-1")));

        mockMvc.perform(get("/api/boards").param("boardId", "board-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.systemTags[0].tagId").value(1));

        mockMvc.perform(get("/api/boards/user-tags").param("userTagName", "tag-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response[0].userTagNameList.nameList[0]").value("tag-1"));
    }

    @Test
    void 수정과_삭제는_현재_userId를_전달한다() throws Exception {
        when(commandService.update(any(), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(trackBoard("board-1"));

        mockMvc.perform(patch("/api/boards")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateBoardPayload(
                                "board-1", "updated", List.of("tag-1")
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/boards/{boardId}", "board-1")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER))))
                .andExpect(status().isOk());

        verify(commandService).delete("board-1", 1L);
    }

    @Test
    void 작성자_확인은_인증_userId를_전달한다() throws Exception {
        when(queryService.isWriter("board-1", 1L)).thenReturn(true);

        mockMvc.perform(get("/api/boards/auth/{boardId}", "board-1")
                        .with(user(new PrincipalDetails(1L, Role.ROLE_USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value(true));
    }

    @Test
    void 익명_수정과_삭제는_차단된다() throws Exception {
        mockMvc.perform(patch("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateBoardPayload(
                                "board-1", "updated", List.of("tag-1")
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/boards/{boardId}", "board-1"))
                .andExpect(status().isForbidden());
    }

    private BoardView trackBoard(String boardId) {
        return board(boardId, BoardSubject.track(10L, "spotify-track"));
    }

    private BoardView albumBoard(String boardId) {
        return board(boardId, BoardSubject.album(20L, "spotify-album"));
    }

    private BoardView board(String boardId, BoardSubject subject) {
        return new BoardView(
                boardId,
                "content",
                LocalDateTime.of(2026, 10, 14, 0, 0),
                "nickname",
                3,
                subject,
                List.of("tag-1"),
                List.of(new BoardSystemTag(1L, "Ambient", 0.9))
        );
    }

    private record CreateBoardPayload(
            String content, String trackId, String albumId, List<String> userTagList
    ) {
    }

    private record UpdateBoardPayload(String boardId, String content, List<String> userTagList) {
    }
}
