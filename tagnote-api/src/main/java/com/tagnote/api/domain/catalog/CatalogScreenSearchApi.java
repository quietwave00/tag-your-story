package com.tagnote.api.domain.catalog;

import com.tagnote.api.domain.catalog.dto.response.CatalogScreenSearchResponse;
import com.tagnote.core.utils.api.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Catalog Search", description = "Spotify 기반 Catalog 검색")
public interface CatalogScreenSearchApi {
    @Operation(summary = "검색 화면 통합 조회", description = "인증 불필요. Track·Artist는 일반 keyword, Album은 Spotify album: 제목 필터로 각각 연속 3페이지를 조회하고 타입별 최대 30개 후보를 정렬한다. cache miss의 Spotify 검색은 최대 6회다. 각 타입의 선택 page에서 최대 10개만 반환한다. 동일 3-page window는 5분간 재사용한다. 저장·태그 계산은 하지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공. Spotify 장애는 기존 정책상 HTTP 200의 success=false, response.exceptionCode=SPOTIFY_EXCEPTION, response.status=503으로 반환될 수 있음",
                    useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "spotifyError",
                            value = "{\"success\":false,\"response\":{\"exceptionCode\":\"SPOTIFY_EXCEPTION\",\"message\":\"스포티파이 라이브러리 사용 중 예외가 발생했습니다.\",\"status\":503}}"))),
            @ApiResponse(responseCode = "400", description = "필수 keyword 누락 또는 page가 0~100 정수 범위를 벗어남", content = @Content)
    })
    ApiResult<CatalogScreenSearchResponse> searchScreen(
            @Parameter(name = "keyword", description = "Spotify 검색어", required = true, in = ParameterIn.QUERY) String keyword,
            @Parameter(name = "trackPage", description = "0-based Track page, 기본값 0, 크기 10", in = ParameterIn.QUERY,
                    schema = @Schema(defaultValue = "0", minimum = "0", maximum = "100")) int trackPage,
            @Parameter(name = "albumPage", description = "0-based Album page, 기본값 0, 크기 10", in = ParameterIn.QUERY,
                    schema = @Schema(defaultValue = "0", minimum = "0", maximum = "100")) int albumPage,
            @Parameter(name = "artistPage", description = "0-based Artist page, 기본값 0, 크기 10", in = ParameterIn.QUERY,
                    schema = @Schema(defaultValue = "0", minimum = "0", maximum = "100")) int artistPage);
}
