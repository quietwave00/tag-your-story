package com.tagnote.api.domain.board;

import com.tagnote.api.domain.board.dto.request.CreateBoardRequest;
import com.tagnote.api.domain.board.dto.request.UpdateBoardRequest;
import com.tagnote.api.domain.board.dto.response.BoardCountResponse;
import com.tagnote.api.domain.board.dto.response.BoardListResponse;
import com.tagnote.api.domain.board.dto.response.BoardResponse;
import com.tagnote.api.domain.board.dto.response.CreateBoardResponse;
import com.tagnote.api.domain.board.dto.response.DetailBoardResponse;
import com.tagnote.core.domain.board.BoardOrderType;
import com.tagnote.core.utils.api.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Board", description = "Track 또는 Album 커뮤니티 Board API")
public interface BoardApi {

    @Operation(summary = "Board 작성", description = "인증 필요. Spotify trackId 또는 albumId 중 하나만 전달한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "작성 성공. Catalog selection 실패나 USER_NOT_FOUND, "
                    + "TRACK_NOT_FOUND, ALBUM_NOT_FOUND는 기존 정책상 success=false와 response.status로 표현",
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "대상 ID가 없거나 둘 다 존재, 또는 요청 validation 실패"),
            @ApiResponse(responseCode = "403", description = "미인증 사용자")
    })
    ApiResult<CreateBoardResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "작성 내용, UserTag와 정확히 하나의 Spotify 대상 ID",
                    required = true
            )
            CreateBoardRequest request,
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "Track Board 목록", description = "인증 불필요. trackId는 Spotify Track ID다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "필수 parameter 누락 또는 타입 오류", content = @Content)
    })
    ApiResult<BoardListResponse> getBoardListByTrackId(
            @Parameter(name = "trackId", in = ParameterIn.PATH, required = true,
                    description = "Spotify Track ID") String trackId,
            @Parameter(name = "order-type", in = ParameterIn.QUERY, required = true,
                    description = "CREATED_AT 또는 LIKE") BoardOrderType orderType,
            @Parameter(name = "page", in = ParameterIn.QUERY, required = true,
                    description = "0-based page, size 8") int page
    );

    @Operation(summary = "Album Board 목록", description = "인증 불필요. albumId는 Spotify Album ID다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "필수 parameter 누락 또는 타입 오류", content = @Content)
    })
    ApiResult<BoardListResponse> getBoardListByAlbumId(
            @Parameter(name = "albumId", in = ParameterIn.PATH, required = true,
                    description = "Spotify Album ID") String albumId,
            @Parameter(name = "order-type", in = ParameterIn.QUERY, required = true,
                    description = "CREATED_AT 또는 LIKE") BoardOrderType orderType,
            @Parameter(name = "page", in = ParameterIn.QUERY, required = true,
                    description = "0-based page, size 8") int page
    );

    @Operation(summary = "Board 상세", description = "인증 불필요. 대상의 visible resolved System Tag를 포함한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. BOARD_NOT_FOUND는 기존 정책상 "
                    + "success=false와 response.status=404로 표현", useReturnTypeSchema = true)
    })
    ApiResult<DetailBoardResponse> getDetailBoard(
            @Parameter(name = "boardId", in = ParameterIn.QUERY, required = true) String boardId
    );

    @Operation(summary = "Track Board 개수", description = "인증 불필요")
    ApiResult<BoardCountResponse> getBoardCountByTrackId(
            @Parameter(name = "trackId", in = ParameterIn.PATH, required = true,
                    description = "Spotify Track ID") String trackId
    );

    @Operation(summary = "Album Board 개수", description = "인증 불필요")
    ApiResult<BoardCountResponse> getBoardCountByAlbumId(
            @Parameter(name = "albumId", in = ParameterIn.PATH, required = true,
                    description = "Spotify Album ID") String albumId
    );

    @Operation(summary = "UserTag 이름 기준 Board 조회", description = "인증 불필요. 이름을 변형하지 않고 exact match한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. USER_TAG_NOT_FOUND는 기존 정책상 "
                    + "success=false와 response.status=404로 표현", useReturnTypeSchema = true)
    })
    ApiResult<List<BoardResponse>> getBoardListByUserTagName(
            @Parameter(name = "userTagName", in = ParameterIn.QUERY, required = true) String userTagName
    );

    @Operation(summary = "Board 작성자 여부", description = "인증 필요")
    ApiResult<Boolean> isWriter(
            @Parameter(name = "boardId", in = ParameterIn.PATH, required = true) String boardId,
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "Board 수정", description = "인증 및 작성자 권한 필요. Board 대상은 변경할 수 없다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공. BOARD_ACCESS_DENIED 또는 BOARD_NOT_FOUND는 "
                    + "기존 정책상 success=false와 response.status로 표현", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "미인증 사용자", content = @Content)
    })
    ApiResult<BoardResponse> updateBoardAndUserTag(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true) UpdateBoardRequest request,
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "Board 삭제", description = "인증 및 작성자 권한 필요. REMOVAL 상태로 soft delete한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공. BOARD_ACCESS_DENIED 또는 BOARD_NOT_FOUND는 "
                    + "기존 정책상 success=false와 response.status로 표현"),
            @ApiResponse(responseCode = "403", description = "미인증 사용자", content = @Content)
    })
    ApiResult<Void> delete(
            @Parameter(name = "boardId", in = ParameterIn.PATH, required = true) String boardId,
            @Parameter(hidden = true) Long userId
    );
}
