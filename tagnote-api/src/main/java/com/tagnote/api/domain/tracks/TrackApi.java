package com.tagnote.api.domain.tracks;

import com.tagnote.api.domain.tracks.dto.response.RankingListResponse;
import com.tagnote.api.domain.tracks.dto.response.SearchTracksResponse;
import com.tagnote.api.domain.tracks.dto.request.ImportTrackRequest;
import com.tagnote.api.domain.tracks.dto.response.CatalogTrackResponse;
import com.tagnote.core.domain.tracks.service.dto.TrackData;
import com.tagnote.core.utils.api.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Track", description = "Spotify 기반 트랙 검색, 상세 조회 및 검색어 랭킹 API")
public interface TrackApi {

    @Operation(
            summary = "트랙 검색",
            description = "Spotify 트랙 검색"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "검색 성공. Spotify 장애 시에도 현재 API 정책에 따라 HTTP 200과 success=false, "
                            + "response.exceptionCode=SPOTIFY_EXCEPTION 형태로 반환될 수 있음",
                    useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(
                            name = "spotifyError",
                            summary = "Spotify 검색 실패",
                            value = "{\"success\":false,\"response\":{\"exceptionCode\":\"SPOTIFY_EXCEPTION\","
                                    + "\"message\":\"스포티파이 라이브러리 사용 중 예외가 발생했습니다.\",\"status\":503}}"
                    ))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "필수 query parameter 누락 또는 page 타입 오류",
                    content = @Content
            )
    })
    ApiResult<SearchTracksResponse> search(
            @Parameter(
                    name = "keyword",
                    description = "검색할 트랙, 아티스트 또는 앨범 키워드",
                    required = true,
                    in = ParameterIn.QUERY,
                    example = "Radiohead"
            )
            String keyword,
            @Parameter(
                    name = "page",
                    description = "페이지 번호 0-based. 페이지 크기는 10",
                    required = true,
                    in = ParameterIn.QUERY,
                    example = "0"
            )
            int page
    );

    @Operation(
            summary = "Spotify Track Catalog Import",
            description = "Spotify track id를 기준으로 Artist, Album, Track과 전체 Artist credit을 내부 Catalog에 "
                    + "저장하거나 기존 데이터를 재사용하고, 계산된 System Tag를 함께 조회"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Import 또는 기존 Catalog 조회 성공. systemTags는 HIDDEN을 제외한 resolved System Tag이며 "
                            + "score 내림차순, 동일 score에서는 tagId 오름차순. Spotify 장애 시 기존 오류 정책에 따라 "
                            + "HTTP 200과 success=false로 반환될 수 있음",
                    useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(
                            name = "spotifyError",
                            value = "{\"success\":false,\"response\":{\"exceptionCode\":\"SPOTIFY_EXCEPTION\","
                                    + "\"message\":\"스포티파이 라이브러리 사용 중 예외가 발생했습니다.\",\"status\":503}}"
                    ))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 body 누락 또는 spotifyTrackId blank",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"success\":false,\"response\":{\"exceptionCode\":null,"
                                    + "\"message\":\"spotifyTrackId는 비어 있을 수 없습니다.\",\"status\":400}}"
                    ))
            )
    })
    ApiResult<CatalogTrackResponse> importTrack(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "선택한 Spotify Track",
                    required = true
            )
            ImportTrackRequest request
    );

    @Operation(
            summary = "트랙 상세 조회",
            description = "Spotify track id로 트랙 상세 정보를 조회"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공. Spotify 장애 시에도 현재 API 정책에 따라 HTTP 200과 success=false, "
                            + "response.exceptionCode=SPOTIFY_EXCEPTION 형태로 반환될 수 있음",
                    useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(
                            name = "spotifyError",
                            summary = "Spotify 상세 조회 실패",
                            value = "{\"success\":false,\"response\":{\"exceptionCode\":\"SPOTIFY_EXCEPTION\","
                                    + "\"message\":\"스포티파이 라이브러리 사용 중 예외가 발생했습니다.\",\"status\":503}}"
                    ))
            )
    })
    ApiResult<TrackData> getDetail(
            @Parameter(
                    name = "trackId",
                    description = "Spotify track id",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "4u7EnebtmKWzUH433cf5Qv"
            )
            String trackId
    );

    @Operation(
            summary = "검색어 랭킹 조회",
            description = "검색 횟수 기준 상위 5개 키워드를 조회"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색어 랭킹 조회 성공", useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "500",
                    description = "Redis 조회 실패 등 처리되지 않은 서버 오류",
                    content = @Content
            )
    })
    ApiResult<RankingListResponse> getKeywordRanking();
}
