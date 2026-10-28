# CATALOG-DETAIL-001 — Spotify 상세·하위 목록 및 화면 검색 조회

> 상태: 2026-10-28 사용자 확인으로 완료 처리. 팔로워 수는 사용자 요청으로 제외했다. Codex는 Gradle·verify.sh를 실행하지 않았으며 명령별 실행 출력은 전달받지 않아 개별 통과 여부는 미확인이다.

## Goal

Catalog 검색 결과의 Spotify Album·Artist ID로 공개 상세 메타데이터를 조회한다. 기존 Track 상세 API와 Catalog import 계약은 유지한다.

## Decision and Scope

- 공개 API는 Catalog 경계의 `GET /api/catalog/albums/{spotifyAlbumId}`와 `GET /api/catalog/artists/{spotifyArtistId}`에 둔다. Track 전용 호환 Controller에 새 subject를 추가하지 않는다.
- Album 응답: `spotifyId`, `title`, `imageUrl`, `releaseDate`, `totalTracks`, `artists[{spotifyId,name}]`.
- Artist 응답: `spotifyId`, `name`, `imageUrl`, `genres`. 팔로워 수는 포함하지 않는다.
- 인증 불필요. Spotify 메타데이터 읽기만 수행하며 Catalog 저장, DB 조회, enrichment 및 DB transaction을 추가하지 않는다.
- 이미지가 없으면 기존 `NO_IMAGE` 규칙을 사용한다. Spotify 장애는 기존 `SPOTIFY_EXCEPTION` 응답 체계를 유지한다.
- 2026-10-27 사용자 추가 요청에 따라 `GET /api/catalog/artists/{spotifyArtistId}/albums?page=0`과 `GET /api/catalog/albums/{spotifyAlbumId}/tracks?page=0`을 같은 Catalog 읽기 마일스톤에 포함한다. 기존 단건 응답 구조는 바꾸지 않는다.
- 두 목록의 `page`는 선택적 0-based이며 기본값 0, page size 10, Spotify offset `page * 10`이다. 응답은 `items`와 해당 Spotify `totalCount`를 반환한다.
- Artist Album 목록은 Spotify 기본 범위(Album, Single, Compilation, Appears On)를 그대로 반환한다. 로컬 필터링이나 중복 제거로 Spotify total과 페이지 경계를 왜곡하지 않는다. 각 항목은 `spotifyId`, `title`, `imageUrl`, `releaseDate`다.
- Album Track 목록은 Spotify 디스크/트랙 순서를 유지한다. 각 항목은 `spotifyId`, `title`, nullable `artistName`, `discNumber`, `trackNumber`, `durationMs`다.
- 2026-10-28 사용자 요청으로 화면용 `GET /api/catalog/search/screen`을 추가한다. `keyword`와 선택적 0-based `trackPage`, `albumPage`, `artistPage`를 받고, 각 타입의 `items`, `totalCount`, `hasNext`를 반환한다. 각 타입 page size는 10이다.
- 같은 키워드의 Spotify 통합 검색 `type=track,album,artist`를 3개 연속 offset에 호출해 각 타입 최대 30개 후보를 만든다. 타입별로 기존 `CatalogSearchRanker`의 규칙을 전체 30개에 적용하고 요청 page에 해당하는 10개만 반환한다. 3-page window는 짧은 TTL로 공유해 같은 window의 화면 페이지가 같은 순서를 사용한다.
- Spotify 검색 offset 최대 1000에 맞춰 화면 page는 0~100만 허용한다. `hasNext`는 `totalCount` 변동에 의존하지 않고 window에 실제로 확보한 후보와 마지막 window 경계를 기준으로 계산한다. window 끝이 정확히 30개면 다음 window가 있을 수 있으므로 한 번 더 조회할 수 있고, 빈 결과에서 탐색을 멈춘다.
- 기존 `/api/catalog/search` 통합·타입별 계약과 `/api/tracks` 호환 API는 유지한다. 새 화면 계약의 검색 정책 변경은 ADR-018에 기록한다.
- 2026-10-28 화면 재현 결과, 현재 클라이언트가 새 endpoint 대신 기존 타입별 endpoint를 호출하므로 기존 타입별 검색도 동일한 3-page window를 사용하도록 범위를 보정한다. URL·응답 필드는 유지하되 Spotify 조회·정렬 정책 변경은 ADR-019에 기록한다. 타입 생략 검색과 `/api/tracks`는 유지한다.
- 클라이언트 소스는 수정하지 않는다. 공개 API 문서인 `tag-note-client/agents/client/api-contract.md`만 서버 컨벤션에 따라 갱신한다.

## Implementation

1. `CatalogDetailApi` Swagger interface와 `CatalogDetailController`를 추가한다.
2. Application detail model/port/service와 Spotify adapter를 통해 SDK 객체를 공개 DTO와 분리한다.
3. 기존 Album 상세 Spotify HTTP 호출을 재사용하고 Artist 상세 호출을 추가한다.
4. Server Spec과 클라이언트 API 계약 문서를 구현과 일치시킨다.
5. Controller 및 Adapter 단위 테스트로 두 경로, 응답 필드, 이미지 부재, Spotify 예외 경계를 확인한다.
6. 같은 detail port/service/adapter에 Artist Album 및 Album Track 페이지 조회를 추가하고 API/Swagger/문서/테스트를 확장한다.
7. 화면용 검색 응답/Swagger, 3-page Spotify window provider, 타입별 순위·slice Application Service와 짧은 Redis window cache를 추가한다. 기존 ranker의 비교 정책은 재사용한다.
8. 화면 검색의 3회 Spotify 호출, 페이지 간 일관성, 타입별 분리, 최대 offset 및 기존 검색 회귀 테스트를 작성한다.

## Acceptance Criteria

- Album·Artist 검색 결과의 `spotifyId`로 각각 공개 상세 API를 호출할 수 있다.
- 응답은 명시한 필드만 포함하며 팔로워 수가 없다.
- Artist Album·Album Track 목록은 각각 선택한 Spotify ID와 page만 조회하고 결과 순서·total을 보존한다. 한 서버 요청당 해당 Spotify 목록 HTTP 요청은 한 번이다.
- 음수 또는 offset 계산 범위를 벗어난 page는 HTTP 400이며, 빈 목록은 `items=[]`와 Spotify total로 반환한다.
- Album Artist credit 순서를 보존하고 이미지 부재 시 `NO_IMAGE`를 반환한다.
- 네 GET 경로는 저장·enrichment·트랜잭션을 수행하지 않는다.
- 기존 `/api/tracks/{trackId}` 및 Catalog 검색/import 계약은 변경하지 않는다. 새 화면 검색 API를 추가한다.
- 새 화면 API의 첫 페이지 요청은 Spotify 통합 검색을 최대 3회 호출해 Track·Album·Artist 각각 최대 30개를 후보로 정렬하고, 타입별 현재 page 최대 10개만 반환한다.
- 기존 타입별 검색도 현재 화면에서 최대 30개 window 정렬을 사용해 Spotify 3페이지의 정확 일치를 해당 window 첫 page로 올린다. 동일 Spotify ID는 window 안에서 한 번만 반환한다.
- 같은 keyword/window의 후속 page는 저장된 순서를 재사용하며 타입별 페이지 이동은 독립적이다. `totalCount`는 참고용 Spotify 원본 값이며 다음 페이지 탐색은 `hasNext`를 사용한다.
- Swagger, Server Spec, 클라이언트 API 계약 문서와 실제 구현이 일치한다.
- 구현·정적 diff review 후 2026-10-28 사용자 확인을 받아 completed로 이동했다. 검증 명령별 출력은 제공되지 않아 개별 통과 여부는 미확인으로 기록한다.

## Verification

Codex는 저장소 규칙에 따라 Gradle 및 `verify.sh`를 직접 실행하지 않는다. 사용자 실행 명령: `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`.

## Implementation Record

- Catalog 경계에 Album·Artist 공개 상세 API, Spotify adapter 및 응답 DTO를 추가했다.
- 프로젝트의 Spotify Java SDK 8.0.0에는 `Album.getTotalTracks()`가 없어 Album `tracks.total`을 사용한다.
- Controller·adapter 테스트, Swagger, Server Spec 및 클라이언트 API 계약 문서를 갱신했다. 클라이언트 소스는 변경하지 않았다.
- Gradle·verify.sh는 실행하지 않았다. 사용자 실행 결과를 확인한 뒤 Plan 완료 여부를 갱신한다.
- 2026-10-27 사용자가 Artist Album 목록과 Album Track 목록의 개발을 추가 요청했다. 기존 Plan의 목록 제외 결정을 이 사용자 승인으로 변경했으며 관련 읽기 경계만 확장한다.
- 두 목록에 page/total 모델, Spotify 단일 호출, provider-neutral 변환, Controller·adapter 테스트와 Swagger·문서 계약을 추가했다. 목록 page 입력은 Presentation에서 검증하고 `page * 10`으로 Spotify offset을 계산한다.
- 2026-10-28 사용자가 검색 정확도 개선을 위해 3페이지 후보 정렬과 화면 전용 통합 응답을 추가 승인했다. 이 요청은 기존 Plan의 Catalog 검색 제외 범위와 ADR-005/017의 현재 페이지·단일 요청 정책을 바꾸므로 ADR-018로 화면 전용 예외를 명시한다.
- 화면 전용 `GET /api/catalog/search/screen`을 추가했다. Application Service가 타입별 독립 page를 조율하며 Spotify adapter는 최대 3개 연속 통합 검색을 provider-neutral window로 변환한다. 기존 ranker의 비교 규칙은 변경하지 않고 window 전체에 적용한다.
- Redis 5분 cache에 정렬된 window를 보관하고 JSON 직렬화 테스트를 추가했다. 타입별 page/offset 경계, window 재사용, 정확 일치 승격, 기존 Spotify 오류 응답의 Controller·Service·adapter 테스트를 작성했다. Swagger, Server Spec, 클라이언트 API 계약을 갱신했으며 클라이언트 소스는 변경하지 않았다.
- Gradle·verify.sh는 실행하지 않았다. `git diff --check`와 코드·문서 정적 검토 결과를 확인한 뒤 사용자 실행 결과를 기다린다.
- 사용자가 `in love` 검색의 11번째 결과가 정확 일치한다고 보고했다. 실제 React 호출은 기존 타입별 `/api/catalog/search`여서 새 화면 endpoint만 구현한 것은 화면에 적용되지 않았다. ADR-019에 따라 기존 타입별 경로를 화면 window 검색에 연결하고 같은 프로세스의 동시 cache miss를 합친다. 기존 API의 URL·응답 필드는 보존한다.
- 2026-10-28 사용자가 구현을 확인하고 완료 처리를 요청했다. 이 확인을 Human Review로 기록한다. `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`의 명령별 결과는 전달받지 못해 통과 여부를 주장하지 않는다. slug 작업 요청은 사용자 지시에 따라 일시 중지하고 이 Plan에는 포함하지 않는다.
