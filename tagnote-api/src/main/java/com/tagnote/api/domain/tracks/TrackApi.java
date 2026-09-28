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
            deprecated = true,
            description = "Spotify track id를 기준으로 Artist, Album, Track과 전체 Artist credit을 내부 Catalog에 "
                    + "저장하거나 기존 데이터를 재사용하고, MusicBrainz/Discogs/Last.fm enrichment에서 성공한 "
                    + "부분 결과로 계산된 System Tag를 함께 조회. 확정 태그가 없으면 검증 전 raw preview를 "
                    + "출처와 함께 최대 5개 제공. 일부 또는 전체 enrichment provider 실패는 "
                    + "Catalog import 성공을 실패로 변경하지 않음. 인증 불필요. "
                    + "새 클라이언트는 POST /api/catalog/import에 subjectType=TRACK과 spotifyId를 전달"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Import 또는 기존 Catalog 조회 성공. systemTags는 HIDDEN을 제외한 resolved System Tag이며 "
                            + "previewTags는 확정 결과가 없을 때의 검증 전 raw 이름과 출처. "
                            + "tagDisplayStatus는 CONFIRMED, PREVIEW, EMPTY 중 하나. "
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
    @Deprecated(since = "2026-09", forRemoval = false)
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
