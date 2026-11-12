package com.tagnote.api.domain.catalog;

import com.tagnote.api.domain.catalog.dto.response.CatalogSubjectTagsResponse;
import com.tagnote.core.utils.api.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Catalog Subject Tags", description = "Track·Album 상세 화면의 독립 태그 조회")
public interface CatalogSubjectTagsApi {
    @Operation(summary = "Track 태그 조회",
            description = "인증 불필요. 먼저 POST /api/catalog/import에 subjectType=TRACK과 spotifyId를 보내 태그 수집을 시작한다. 이 GET은 저장된 TRACK subject의 태그만 조회하며 Catalog 저장, 외부 HTTP, Resolver 계산을 하지 않는다. Album에서 상속된 태그는 기존 Resolver 결과를 따른다. 확정 태그가 없으면 저장된 검증 전 preview를 최대 5개 반환한다. 상세 메타데이터는 GET /api/tracks/{trackId}에서 별도 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "태그 조회 성공. 빈 결과는 systemTags와 previewTags가 모두 빈 배열이고 tagDisplayStatus=EMPTY. Catalog에 Track이 없으면 기존 오류 정책상 HTTP 200, success=false, response.exceptionCode=TRACK_NOT_FOUND, response.status=404",
                    useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "trackNotFound",
                            value = "{\"success\":false,\"response\":{\"exceptionCode\":\"TRACK_NOT_FOUND\",\"message\":\"존재하지 않는 트랙입니다.\",\"status\":404}}"))),
            @ApiResponse(responseCode = "500", description = "처리되지 않은 서버 오류. 기존 error_code/custom_error_code 정책 적용", content = @Content)
    })
    ApiResult<CatalogSubjectTagsResponse> getTrackTags(
            @Parameter(name = "spotifyTrackId", description = "Spotify Track ID", required = true,
                    in = ParameterIn.PATH, example = "4u7EnebtmKWzUH433cf5Qv") String spotifyTrackId);

    @Operation(summary = "Album 태그 조회",
            description = "인증 불필요. 먼저 POST /api/catalog/import에 subjectType=ALBUM과 spotifyId를 보내 태그 수집을 시작한다. 이 GET은 저장된 ALBUM subject의 태그만 조회하며 Catalog 저장, 외부 HTTP, Resolver 계산을 하지 않는다. 확정 태그가 없으면 저장된 검증 전 preview를 최대 5개 반환한다. 상세 메타데이터는 GET /api/catalog/albums/{spotifyAlbumId}에서 별도 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "태그 조회 성공. 빈 결과는 systemTags와 previewTags가 모두 빈 배열이고 tagDisplayStatus=EMPTY. Catalog에 Album이 없으면 기존 오류 정책상 HTTP 200, success=false, response.exceptionCode=ALBUM_NOT_FOUND, response.status=404",
                    useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "albumNotFound",
                            value = "{\"success\":false,\"response\":{\"exceptionCode\":\"ALBUM_NOT_FOUND\",\"message\":\"존재하지 않는 앨범입니다.\",\"status\":404}}"))),
            @ApiResponse(responseCode = "500", description = "처리되지 않은 서버 오류. 기존 error_code/custom_error_code 정책 적용", content = @Content)
    })
    ApiResult<CatalogSubjectTagsResponse> getAlbumTags(
            @Parameter(name = "spotifyAlbumId", description = "Spotify Album ID", required = true,
                    in = ParameterIn.PATH, example = "6t8D2f9s6yYw2GzXJx3aM2") String spotifyAlbumId);
}
