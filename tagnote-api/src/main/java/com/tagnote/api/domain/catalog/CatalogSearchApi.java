package com.tagnote.api.domain.catalog;

import com.tagnote.api.domain.catalog.dto.response.CatalogSearchResponse;
import com.tagnote.core.utils.api.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Catalog Search", description = "Spotify 기반 Track, Album, Artist 통합 검색 API")
public interface CatalogSearchApi {

    @Operation(
            summary = "Catalog 통합 검색",
            description = "인증 없이 Spotify Track, Album, Artist를 함께 검색한다. 각 type에서 최대 10개를 조회한 뒤 "
                    + "현재 page의 후보를 이름 유사도 기준으로 통합 정렬한다. 검색만 수행하며 Catalog 저장이나 태그 계산은 하지 않는다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "검색 성공. totalCount는 Track, Album, Artist total의 합",
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
    ApiResult<CatalogSearchResponse> search(
            @Parameter(
                    name = "keyword",
                    description = "검색할 Track, Album 또는 Artist 키워드",
                    required = true,
                    in = ParameterIn.QUERY,
                    example = "Queen"
            )
            String keyword,
            @Parameter(
                    name = "page",
                    description = "0-based page. 각 subject type의 페이지 크기는 10이며 응답 items는 최대 30개",
                    required = true,
                    in = ParameterIn.QUERY,
                    example = "0"
            )
            int page
    );
}
