package com.tagnote.api.domain.catalog;

import com.tagnote.api.domain.catalog.dto.response.CatalogAlbumDetailResponse;
import com.tagnote.api.domain.catalog.dto.response.CatalogAlbumTracksResponse;
import com.tagnote.api.domain.catalog.dto.response.CatalogArtistAlbumsResponse;
import com.tagnote.api.domain.catalog.dto.response.CatalogArtistDetailResponse;
import com.tagnote.core.utils.api.ApiResult;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Catalog Detail", description = "Spotify Album·Artist 상세 메타데이터 조회")
public interface CatalogDetailApi {
    @Operation(summary = "Album 상세 조회", description = "인증 불필요. Spotify Album ID로 제목, 이미지, 발매일, Track 수, 발매 유형(ALBUM/COMPILATION/SINGLE_EP)과 참여 Artist를 조회한다. Catalog 저장 및 태그 계산은 하지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. Spotify 조회 실패 시에도 기존 정책에 따라 HTTP 200, success=false, response.exceptionCode=SPOTIFY_EXCEPTION, response.status=503 가능",
                    useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "spotifyError", value = "{\"success\":false,\"response\":{\"exceptionCode\":\"SPOTIFY_EXCEPTION\",\"message\":\"스포티파이 라이브러리 사용 중 예외가 발생했습니다.\",\"status\":503}}")))
    })
    ApiResult<CatalogAlbumDetailResponse> getAlbum(
            @Parameter(name = "spotifyAlbumId", description = "Spotify Album ID", required = true, in = ParameterIn.PATH) String spotifyAlbumId);

    @Operation(summary = "Artist 상세 조회", description = "인증 불필요. Spotify Artist ID로 이름, 이미지와 장르를 조회한다. 팔로워 수, Catalog 저장 및 태그 계산은 포함하지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. Spotify 조회 실패 시에도 기존 정책에 따라 HTTP 200, success=false, response.exceptionCode=SPOTIFY_EXCEPTION, response.status=503 가능",
                    useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "spotifyError", value = "{\"success\":false,\"response\":{\"exceptionCode\":\"SPOTIFY_EXCEPTION\",\"message\":\"스포티파이 라이브러리 사용 중 예외가 발생했습니다.\",\"status\":503}}")))
    })
    ApiResult<CatalogArtistDetailResponse> getArtist(
            @Parameter(name = "spotifyArtistId", description = "Spotify Artist ID", required = true, in = ParameterIn.PATH) String spotifyArtistId);

    @Operation(summary = "Artist의 Album 목록 조회", description = "인증 불필요. releaseType 생략 시 Spotify 원본 page와 totalCount를 유지한다. ALL/ALBUM/SINGLE_EP/COMPILATION 지정 시 Spotify의 전체 offset 창을 수집한 뒤 분류 후 0-based page(크기 10)와 실제 수집된 항목의 유형별 건수, classificationComplete, unclassifiedCount를 반환한다. 짧은 원본 창이 있으면 ALL 건수는 Spotify 원본 total보다 작을 수 있다. 분류 불가 항목은 ALL에만 포함된다. 구축 중 HTTP 202와 Retry-After: 1, response.status=BUILDING이다. DB 조회·저장 없음.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. Spotify 장애는 기존 정책상 HTTP 200, success=false, response.exceptionCode=SPOTIFY_EXCEPTION, response.status=503으로 반환될 수 있음", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "202", description = "인덱스 구축 중. Retry-After: 1; response.status=BUILDING", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "page가 음수·정수가 아니거나 offset 계산 범위를 벗어남, 또는 releaseType이 빈 값·허용 범위 밖", content = @Content)
    })
    ResponseEntity<ApiResult<CatalogArtistAlbumsResponse>> getArtistAlbums(
            @Parameter(name = "spotifyArtistId", description = "Spotify Artist ID", required = true, in = ParameterIn.PATH) String spotifyArtistId,
            @Parameter(name = "page", description = "선택적 0-based page. 기본값 0, 크기 10", in = ParameterIn.QUERY,
                    schema = @Schema(defaultValue = "0", minimum = "0")) int page,
            @Parameter(name = "releaseType", description = "선택적 ALL/ALBUM/SINGLE_EP/COMPILATION. 생략 시 원본 Spotify page", in = ParameterIn.QUERY,
                    schema = @Schema(allowableValues = {"ALL", "ALBUM", "SINGLE_EP", "COMPILATION"})) String releaseType);

    @Operation(summary = "Album의 Track 목록 조회", description = "인증 불필요. Spotify Album ID로 Track을 Spotify 디스크·트랙 순서대로 조회한다. page는 0-based, 크기는 10이다. Catalog 저장 및 태그 계산은 하지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. Spotify 장애는 기존 정책상 HTTP 200, success=false, response.exceptionCode=SPOTIFY_EXCEPTION, response.status=503으로 반환될 수 있음", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "page가 음수, 정수가 아니거나 offset 계산 범위를 벗어남", content = @Content)
    })
    ApiResult<CatalogAlbumTracksResponse> getAlbumTracks(
            @Parameter(name = "spotifyAlbumId", description = "Spotify Album ID", required = true, in = ParameterIn.PATH) String spotifyAlbumId,
            @Parameter(name = "page", description = "선택적 0-based page. 기본값 0, 크기 10", in = ParameterIn.QUERY,
                    schema = @Schema(defaultValue = "0", minimum = "0")) int page);
}
