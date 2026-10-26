package com.tagnote.api.domain.comment;

import com.tagnote.api.domain.comment.dto.request.CreateCommentRequest;
import com.tagnote.api.domain.comment.dto.request.CreateReplyRequest;
import com.tagnote.api.domain.comment.dto.request.UpdateCommentRequest;
import com.tagnote.api.domain.comment.dto.response.CommentCountResponse;
import com.tagnote.api.domain.comment.dto.response.CommentResponse;
import com.tagnote.api.domain.comment.dto.response.CommentWithRepliesResponse;
import com.tagnote.core.utils.api.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Tag(name = "Comment", description = "게시글 댓글과 답글 API. 성공 응답은 ApiResult.response에 담긴다. CustomException 응답의 response.exceptionCode는 custom_error_code에 해당한다. OAuth redirect의 error_code와는 다르다.")
public interface CommentApi {
    @Operation(summary = "댓글 작성", description = "POST /api/comments. ROLE_USER 필요. boardId는 POST 상태 Board의 ID이며 content는 공백 불가. 작성자가 게시글 작성자와 다를 때 COMMENT 알림을 발행한다. response는 commentId, parentId(null), nickname, content, createdDate를 포함한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "작성 성공. BOARD_NOT_FOUND, USER_NOT_FOUND, USER_INACTIVE, ACCOUNT_WITHDRAWN은 기존 오류 응답의 response.status로 표현", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "boardId 또는 content 누락·공백", content = @Content),
            @ApiResponse(responseCode = "403", description = "미인증", content = @Content)
    })
    ApiResult<CommentResponse> create(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            description = "boardId: 필수·공백 불가, content: 필수·공백 불가") CreateCommentRequest request,
            @Parameter(hidden = true) Long userId) throws ExecutionException, InterruptedException;

    @Operation(summary = "댓글 수정", description = "PATCH /api/comments. ROLE_USER 필요. commentId와 공백이 아닌 content가 필수다. 현재 구현은 작성자 소유권을 검사하지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공. COMMENT_NOT_FOUND는 기존 오류 응답의 response.status=404로 표현", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "commentId 또는 content validation 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "미인증", content = @Content)
    })
    ApiResult<CommentResponse> update(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            description = "commentId: 필수, content: 필수·공백 불가") UpdateCommentRequest request);

    @Operation(summary = "댓글 삭제", description = "PATCH /api/comments/status/{commentId}. ROLE_USER 필요. REMOVAL로 soft delete한다. 현재 구현은 작성자 소유권을 검사하지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공, response는 null. COMMENT_NOT_FOUND는 response.status=404로 표현", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "commentId 타입 오류", content = @Content),
            @ApiResponse(responseCode = "403", description = "미인증", content = @Content)
    })
    ApiResult<Void> delete(@Parameter(name = "commentId", in = ParameterIn.PATH, required = true) Long commentId);

    @Operation(summary = "최상위 댓글 목록", description = "GET /api/comments/{boardId}/{page}. 인증 불필요. POST 최상위 댓글을 commentId 내림차순, 0-based page당 20개 조회하고 POST 자식을 함께 반환한다. 없는 Board는 빈 목록이다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "comment와 children 배열의 목록", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "page 타입 오류", content = @Content),
            @ApiResponse(responseCode = "500", description = "음수 page는 현재 별도 검증 없이 PageRequest에서 실패", content = @Content)
    })
    ApiResult<List<CommentWithRepliesResponse>> getCommentList(
            @Parameter(name = "boardId", in = ParameterIn.PATH, required = true) String boardId,
            @Parameter(name = "page", in = ParameterIn.PATH, required = true, description = "0-based, 크기 20") int page);

    @Operation(summary = "내 댓글 ID 목록", description = "GET /api/comments/auth/{boardId}. ROLE_USER 필요. 현재 사용자가 작성한 댓글 ID 목록이며 상태 필터는 없다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 ID 배열", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "미인증", content = @Content)
    })
    ApiResult<List<Long>> getUserCommentId(
            @Parameter(name = "boardId", in = ParameterIn.PATH, required = true) String boardId,
            @Parameter(hidden = true) Long userId);

    @Operation(summary = "게시글 댓글 수", description = "GET /api/comments/count/{boardId}. 인증 불필요. POST 상태 최상위 댓글과 답글의 합계다. 없는 Board는 0이다.")
    @ApiResponse(responseCode = "200", description = "response.count", useReturnTypeSchema = true)
    ApiResult<CommentCountResponse> getCommentCountByBoardId(
            @Parameter(name = "boardId", in = ParameterIn.PATH, required = true) String boardId);

    @Operation(summary = "답글 작성", description = "POST /api/comments/replies. ROLE_USER 필요. boardId, parentId, content가 필수다. 현재 구현은 부모 깊이와 Board 일치를 검사하지 않으며 알림을 발행하지 않는다. response는 CommentResponse다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "작성 성공. BOARD_NOT_FOUND, COMMENT_NOT_FOUND, USER_NOT_FOUND, USER_INACTIVE, ACCOUNT_WITHDRAWN은 기존 오류 응답의 response.status로 표현", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "boardId, parentId 또는 content validation 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "미인증", content = @Content)
    })
    ApiResult<CommentResponse> createReply(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            description = "boardId: 필수·공백 불가, parentId: 필수, content: 필수·공백 불가") CreateReplyRequest request,
            @Parameter(hidden = true) Long userId);

    @Operation(summary = "답글 목록", description = "GET /api/comments/replies/{parentId}/{page}. 인증 불필요. POST 상태 답글을 0-based page당 5개 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "답글 배열. COMMENT_NOT_FOUND는 기존 오류 응답의 response.status=404로 표현", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "parentId 또는 page 타입 오류", content = @Content),
            @ApiResponse(responseCode = "500", description = "음수 page는 현재 별도 검증 없이 PageRequest에서 실패", content = @Content)
    })
    ApiResult<List<CommentResponse>> getReplyList(
            @Parameter(name = "parentId", in = ParameterIn.PATH, required = true) Long parentId,
            @Parameter(name = "page", in = ParameterIn.PATH, required = true, description = "0-based, 크기 5") int page);
}
