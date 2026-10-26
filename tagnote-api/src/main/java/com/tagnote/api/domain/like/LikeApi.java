package com.tagnote.api.domain.like;

import com.tagnote.api.domain.like.dto.request.LikeBoardRequest;
import com.tagnote.api.domain.like.dto.request.UnLikeRequest;
import com.tagnote.api.domain.like.dto.response.LikeStatusResponse;
import com.tagnote.api.domain.like.dto.response.LikedBoardResponse;
import com.tagnote.core.utils.api.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;

@Tag(name = "Like", description = "게시글 좋아요와 마이페이지 좋아요 목록")
public interface LikeApi {
    @Operation(summary = "좋아요 등록 (기존 경로)", description = "인증 필요. JSON body의 boardId는 필수다. 성공 시 ApiResult.response=true. 본인 글도 가능하며 알림은 생략한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공. BOARD_NOT_FOUND, USER_NOT_FOUND, USER_INACTIVE, ACCOUNT_WITHDRAWN은 기존 오류 처리에 따라 success=false와 response.status로 표현", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "boardId 누락 또는 공백", content = @Content),
            @ApiResponse(responseCode = "403", description = "미인증", content = @Content),
            @ApiResponse(responseCode = "409", description = "LIKE_ALREADY_EXISTS. custom_error_code/exceptionCode; 자동 재시도하지 않는다", content = @Content)
    })
    ApiResult<Boolean> like(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            description = "boardId: 필수, 공백 불가") LikeBoardRequest request,
            @Parameter(hidden = true) Long userId);

    @Operation(summary = "좋아요 취소 (기존 경로)", description = "인증 필요. JSON body의 boardId는 필수다. 관계가 없어도 성공한다. REMOVAL 게시글의 기존 Like도 취소할 수 있다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "빈 ApiResult 성공. USER_NOT_FOUND, USER_INACTIVE, ACCOUNT_WITHDRAWN은 success=false와 response.status로 표현", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "boardId 누락 또는 공백", content = @Content),
            @ApiResponse(responseCode = "403", description = "미인증", content = @Content)
    })
    ApiResult<Void> unLike(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            description = "boardId: 필수, 공백 불가") UnLikeRequest request,
            @Parameter(hidden = true) Long userId);

    @Operation(summary = "좋아요 상태 (기존 경로)", description = "인증 필요. 없는 게시글은 false로 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ApiResult.response.liked", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "인증 실패", content = @Content)
    })
    ApiResult<LikeStatusResponse> isLiked(
            @Parameter(name = "boardId", in = ParameterIn.PATH, required = true) String boardId,
            @Parameter(hidden = true) Long userId);

    @Operation(summary = "게시글 좋아요 등록", description = "인증 필요. Path boardId의 POST 게시글에 좋아요한다. 성공 시 ApiResult.response=true.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공. BOARD_NOT_FOUND, USER_NOT_FOUND, USER_INACTIVE, ACCOUNT_WITHDRAWN은 success=false와 response.status로 표현", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "미인증", content = @Content),
            @ApiResponse(responseCode = "409", description = "LIKE_ALREADY_EXISTS (custom_error_code/exceptionCode)", content = @Content)
    })
    ApiResult<Boolean> likeBoard(@Parameter(name = "boardId", in = ParameterIn.PATH, required = true) String boardId,
                                 @Parameter(hidden = true) Long userId);

    @Operation(summary = "게시글 좋아요 취소", description = "인증 필요. 관계가 없어도 빈 ApiResult로 성공한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공. USER_NOT_FOUND, USER_INACTIVE, ACCOUNT_WITHDRAWN은 success=false와 response.status로 표현", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "미인증", content = @Content)
    })
    ApiResult<Void> unLikeBoard(@Parameter(name = "boardId", in = ParameterIn.PATH, required = true) String boardId,
                                @Parameter(hidden = true) Long userId);

    @Operation(summary = "내가 좋아요한 게시글", description = "인증 필요. 현재 사용자의 POST 게시글만 createdAt DESC, likeId DESC로 조회한다. response.items 항목은 boardId, author(탈퇴 시 '탈퇴한 사용자'), content만 포함한다. totalCount는 전체 건수다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ApiResult.response.items/totalCount", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "page가 음수거나 정수가 아님", content = @Content),
            @ApiResponse(responseCode = "403", description = "인증 실패", content = @Content)
    })
    ApiResult<LikedBoardResponse> likedBoards(
            @Parameter(name = "page", in = ParameterIn.QUERY, description = "0-based, 기본값 0, 크기 8", schema = @Schema(defaultValue = "0", minimum = "0")) int page,
            @Parameter(hidden = true) Long userId);
}
