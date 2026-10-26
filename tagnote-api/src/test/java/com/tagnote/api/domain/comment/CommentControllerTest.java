package com.tagnote.api.domain.comment;

import com.tagnote.api.auth.PrincipalDetails;
import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.application.comment.CommentCommandService;
import com.tagnote.core.domain.comment.service.Comment;
import com.tagnote.core.domain.comment.service.CommentFacade;
import com.tagnote.core.domain.comment.service.dto.command.CreateCommentCommand;
import com.tagnote.core.domain.comment.service.dto.command.CreateReplyCommand;
import com.tagnote.core.domain.comment.service.dto.command.UpdateCommentCommand;
import com.tagnote.core.domain.comment.service.dto.response.CommentWithReplies;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.service.User;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@Import(WebMvcMethodSecurityTestConfig.class)
class CommentControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private CommentFacade facade;
    @MockBean private CommentCommandService commands;

    private final PrincipalDetails principal = new PrincipalDetails(7L, Role.ROLE_USER);

    private Comment comment(long id, Comment parent) {
        return Comment.builder().commentId(id).parent(parent).content("내용")
                .user(User.builder().userId(7L).nickname("작성자").userStatus(UserStatus.ACTIVE).build())
                .build();
    }

    @Test
    void 작성_경로와_ApiResult_필드를_유지한다() throws Exception {
        when(commands.create(any(CreateCommentCommand.class))).thenReturn(comment(11L, null));

        mvc.perform(post("/api/comments").with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"boardId\":\"board-1\",\"content\":\"내용\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.commentId").value(11))
                .andExpect(jsonPath("$.response.parentId").isEmpty())
                .andExpect(jsonPath("$.response.nickname").value("작성자"))
                .andExpect(jsonPath("$.response.content").value("내용"))
                .andExpect(jsonPath("$.response.createdDate").isEmpty());
        verify(commands).create(org.mockito.ArgumentMatchers.argThat(command ->
                command.getUserId().equals(7L) && command.getBoardId().equals("board-1")));
    }

    @Test
    void 답글_작성은_parentId와_동일한_응답_DTO를_반환한다() throws Exception {
        when(commands.createReply(any(CreateReplyCommand.class))).thenReturn(comment(12L, comment(11L, null)));

        mvc.perform(post("/api/comments/replies").with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"boardId\":\"board-1\",\"parentId\":11,\"content\":\"답글\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response.commentId").value(12))
                .andExpect(jsonPath("$.response.parentId").value(11));
        verify(commands).createReply(org.mockito.ArgumentMatchers.argThat(command ->
                command.getUserId().equals(7L) && command.getParentId().equals(11L)));
    }

    @Test
    void 수정과_soft_delete_경로를_유지한다() throws Exception {
        when(facade.update(any(UpdateCommentCommand.class))).thenReturn(comment(11L, null));
        mvc.perform(patch("/api/comments").with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commentId\":11,\"content\":\"내용\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response.commentId").value(11));
        mvc.perform(patch("/api/comments/status/11").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").doesNotExist());
        verify(facade).delete(11L);
    }

    @Test
    void 공개_목록_카운트_답글_목록과_인증_작성자_ID_경로를_유지한다() throws Exception {
        when(facade.getCommentList("board-1", 2))
                .thenReturn(List.of(CommentWithReplies.of(comment(11L, null), List.of(comment(12L, comment(11L, null))))));
        when(facade.getCommentCountByBoardId("board-1")).thenReturn(2);
        when(facade.getReplyList(11L, 1)).thenReturn(List.of(comment(12L, comment(11L, null))));
        when(facade.getUserCommentId("board-1", 7L)).thenReturn(List.of(11L, 12L));

        mvc.perform(get("/api/comments/board-1/2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response[0].comment.commentId").value(11))
                .andExpect(jsonPath("$.response[0].children[0].commentId").value(12));
        mvc.perform(get("/api/comments/count/board-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response.count").value(2));
        mvc.perform(get("/api/comments/replies/11/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response[0].parentId").value(11));
        mvc.perform(get("/api/comments/auth/board-1").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response[0]").value(11));
    }

    @Test
    void 미인증_쓰기와_작성자_ID_조회는_거부한다() throws Exception {
        mvc.perform(post("/api/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"boardId\":\"board-1\",\"content\":\"내용\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/comments/replies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"boardId\":\"board-1\",\"parentId\":1,\"content\":\"답글\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commentId\":1,\"content\":\"수정\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/comments/status/1")).andExpect(status().isForbidden());
        mvc.perform(get("/api/comments/auth/board-1")).andExpect(status().isForbidden());
    }

    @Test
    void validation_오류는_기존_ApiResult_형식을_유지한다() throws Exception {
        mvc.perform(post("/api/comments").with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"boardId\":\" \",\"content\":\"내용\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.response.status").value(400));
        mvc.perform(post("/api/comments/replies").with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"boardId\":\"board-1\",\"content\":\"답글\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.response.status").value(400));
        mvc.perform(patch("/api/comments").with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commentId\":11,\"content\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 기존_CustomException은_HTTP_200과_오류_본문으로_반환한다() throws Exception {
        when(commands.create(any(CreateCommentCommand.class)))
                .thenThrow(new CustomException(ExceptionCode.BOARD_NOT_FOUND));
        when(facade.getReplyList(99L, 0)).thenThrow(new CustomException(ExceptionCode.COMMENT_NOT_FOUND));
        when(facade.update(any(UpdateCommentCommand.class)))
                .thenThrow(new CustomException(ExceptionCode.COMMENT_NOT_FOUND));

        mvc.perform(post("/api/comments").with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"boardId\":\"missing\",\"content\":\"내용\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.response.exceptionCode").value("BOARD_NOT_FOUND"))
                .andExpect(jsonPath("$.response.status").value(404));
        mvc.perform(get("/api/comments/replies/99/0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response.exceptionCode").value("COMMENT_NOT_FOUND"));
        mvc.perform(patch("/api/comments").with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commentId\":99,\"content\":\"내용\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response.exceptionCode").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void 없는_게시글의_공개_조회는_빈_목록과_0을_반환한다() throws Exception {
        mvc.perform(get("/api/comments/missing/0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response").isEmpty());
        mvc.perform(get("/api/comments/count/missing"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response.count").value(0));
    }
}
