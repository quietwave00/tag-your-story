package com.tagnote.api.domain.catalog;

import com.tagnote.api.domain.catalog.dto.request.CatalogImportRequest;
import com.tagnote.api.domain.catalog.dto.response.CatalogImportResponse;
import com.tagnote.core.utils.api.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Catalog Import", description = "Spotify Track 또는 Album 선택 및 System Tag 계산")
public interface CatalogImportApi {
    @Operation(summary = "Track 또는 Album import",
            description = "인증 불필요. 검색 결과의 subjectType과 spotifyId를 전달한다. TRACK은 Track과 Album을, "
                    + "ALBUM은 Album만 저장하고 해당 subject의 태그를 계산한다. 기존 resolved 결과가 있으면 재사용한다. "
                    + "systemTags는 확정 결과, previewTags는 확정 결과가 없을 때만 표시하는 검증 전 결과다. "
                    + "일부 외부 제공자 실패는 Catalog import 성공을 취소하지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import 성공. Spotify 장애 시 기존 정책에 따라 HTTP 200과 success=false, response.exceptionCode=SPOTIFY_EXCEPTION 가능",
                    useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "spotifyError",
                            value = "{\"success\":false,\"response\":{\"exceptionCode\":\"SPOTIFY_EXCEPTION\","
                                    + "\"message\":\"스포티파이 라이브러리 사용 중 예외가 발생했습니다.\",\"status\":503}}"))),
            @ApiResponse(responseCode = "400", description = "subjectType/spotifyId 누락, blank 또는 지원하지 않는 subjectType. validation 오류는 success=false와 response.status=400을 반환",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "처리되지 않은 서버 오류. 기존 error_code/custom_error_code 정책 적용",
                    content = @Content)
    })
    ApiResult<CatalogImportResponse> importCatalog(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "선택한 Spotify Track 또는 Album", required = true)
            CatalogImportRequest request);
}
