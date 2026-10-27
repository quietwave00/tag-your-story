# CATALOG-SEARCH-002 — Spotify 타입별 Catalog 검색

> 상태: 2026-10-27 사용자 확인으로 완료 처리. 검증 명령별 출력은 미전달.

## Goal

프론트엔드가 Track, Album, Artist 탭에서 선택한 타입만 Spotify에 검색하고 해당 타입의 결과 수와 페이지를 독립적으로 관리할 수 있도록 한다. 기존 타입 미지정 통합 검색과 `GET /api/tracks` 계약은 하위 호환을 위해 유지한다.

## 근거 문서와 ADR

- `agents/server/server_spec.md` §13.1, `agents/server/current_state.md`, `agents/server/conventions.md`, `agents/server/progress.md`를 확인했다.
- ADR-005는 `GET /api/catalog/search?keyword=&page=`가 Spotify의 단일 Search 요청으로 Track, Album, Artist를 함께 조회하도록 결정한다.
- 이번 마일스톤은 ADR-005를 폐기하지 않고 선택적 타입 검색으로 확장한다. 세 타입 통합 검색을 타입별 검색으로 임의 교체하지 않는다.
- 확장 결정은 `ADR-017-type-selective-catalog-search.md`에 기록한다.

## AS-IS

1. 공개 API는 `GET /api/catalog/search?keyword={keyword}&page={page}`이며 `type` query parameter를 받지 않는다.
2. `SpotifyWebClient`는 항상 Spotify `type=track,album,artist`, 타입별 `limit=10`, `offset=page*10`으로 한 번 요청한다.
3. Adapter는 세 결과를 공통 `CatalogSearchItem` 목록으로 합치고 세 total을 합산한다.
4. Application은 최대 30개인 현재 page 후보를 타입 간 함께 정렬한다.
5. React의 기존 Track 화면은 별도 호환 API인 `GET /api/tracks`를 사용한다.

## API Contract

기존 엔드포인트에 선택적 `type`을 추가한다.

```http
GET /api/catalog/search?keyword=Queen&page=0
GET /api/catalog/search?keyword=Queen&type=TRACK&page=0
GET /api/catalog/search?keyword=Queen&type=ALBUM&page=0
GET /api/catalog/search?keyword=Queen&type=ARTIST&page=0
```

- `keyword`: 필수
- `page`: 필수, 0-based
- `type`: 선택, `TRACK | ALBUM | ARTIST`
- `type` 생략: 기존 통합 검색과 응답을 그대로 유지한다.
- `type` 지정: `items`에는 지정 타입만 포함하고 `totalCount`는 해당 타입의 Spotify total만 반환한다.
- 타입별 page size는 10이며 Spotify offset은 `page * 10`이다.
- 허용되지 않은 `type`은 HTTP 400이다.
- 성공 response DTO와 item 필드 `subjectType`, `spotifyId`, `title`, `artistName`, `albumName`, `imageUrl`은 변경하지 않는다.

## Changes

1. 기존 타입 미지정 통합 검색을 특성화 테스트로 보호한다.
2. Presentation의 선택적 `type`을 Application 내부의 명시적인 검색 범위 `ALL | TRACK | ALBUM | ARTIST`로 변환한다. Application/Infrastructure에서는 `null`로 검색 의도를 표현하지 않는다.
3. `CatalogSearchController`와 분리된 `CatalogSearchApi` Swagger에 선택적 `type`, 허용 값, 타입별 total/page 의미와 잘못된 타입의 400 응답을 문서화한다.
4. `CatalogSearchService`와 `CatalogSearchProvider`가 검색 범위를 전달하도록 변경한다. 검색어는 요청당 한 번만 기록한다.
5. `SpotifyWebClient`는 `ALL`이면 `track,album,artist`, 타입 지정이면 해당 Spotify type 하나만 전달한다. 서버 요청 한 번당 Spotify Search HTTP 요청도 한 번만 수행한다.
6. `SpotifyCatalogSearchAdapter`는 요청 범위에 해당하는 결과만 공통 모델로 변환한다. 타입 지정 total은 다른 타입과 합산하지 않으며, 요청하지 않은 Spotify 결과 group의 null 여부에 의존하지 않는다.
7. 타입별 검색은 해당 타입 후보 안에서만 기존 exact, prefix, 이름 유사도, artist 유사도, Spotify 원본 순서 기반 정렬을 적용한다. `ALL`은 기존 타입 간 결정적 정렬까지 유지한다.
8. Server Spec과 Swagger를 구현에 맞게 갱신한다. 완료 시 `progress.md`를 갱신하고 Plan을 `plans/completed/`로 이동한다.

## Ranking Policy

기존 Unicode NFKC, 소문자화, trim, 연속 공백 축약과 Levenshtein 기반 정렬을 유지한다.

1. 표시 이름 exact match
2. 표시 이름 prefix match
3. 표시 이름 문자열 유사도
4. 대표 Artist 이름 문자열 유사도
5. Spotify 원본 순서
6. 통합 검색에서만 `TRACK`, `ALBUM`, `ARTIST` 결정적 순서
7. Spotify ID 사전순

`ARTIST`는 Artist 이름, `ALBUM`은 Album 제목과 대표 Artist 이름, `TRACK`은 Track 제목과 대표 Artist 이름을 사용한다. 이번 마일스톤은 검색어에 Spotify `artist:`, `album:`, `track:` field filter를 자동 삽입하지 않는다.

## Tests

### Controller / Swagger

- `TRACK`, `ALBUM`, `ARTIST`가 Application service에 정확히 전달된다.
- `type` 생략 시 기존 통합 검색이 유지된다.
- 허용되지 않은 `type`과 필수 query 누락은 400이다.
- 기존 성공 response와 `SPOTIFY_EXCEPTION` 오류 contract를 유지한다.

### Application

- 검색어가 요청당 한 번만 기록된다.
- 검색 범위가 Provider에 정확히 전달된다.
- 타입별 `totalCount`를 유지하고 같은 타입 안에서 결정적으로 정렬한다.
- 타입 미지정 통합 ranking 회귀가 없다.

### Spotify Adapter / Client 경계

- `TRACK`, `ALBUM`, `ARTIST` 요청은 지정 타입만 매핑하고 해당 total만 반환한다.
- `ALL`은 기존 세 타입 매핑과 total 합산을 유지한다.
- 타입 지정 요청에서 미요청 결과 group 때문에 NPE가 발생하지 않는다.
- 이미지나 Artist credit이 없을 때 기존 `NO_IMAGE`와 nullable field 정책을 유지한다.
- 한 서버 요청이 Spotify Search 요청 하나로 귀결된다.

### Regression

- 기존 `GET /api/tracks` endpoint와 response가 바뀌지 않는다.
- 검색 중 JPA Repository 접근, Catalog 저장, import 또는 enrichment가 발생하지 않는다.
- 검색어 기록 횟수와 Spotify 예외 변환을 유지한다.

## Acceptance Criteria

- 프론트엔드가 `type=TRACK|ALBUM|ARTIST`로 타입별 검색을 독립 호출할 수 있다.
- 타입 지정 응답에는 지정 `subjectType`만 존재하고 `totalCount`는 해당 타입 total이다.
- 타입별 page 이동은 다른 타입의 page나 total에 영향을 주지 않는다.
- 타입 생략 요청은 기존 통합 검색 contract, 최대 30개 후보, total 합산과 ranking을 유지한다.
- 서버 요청 한 번당 Spotify Search HTTP 요청은 한 번이다.
- 기존 `GET /api/tracks`는 변경되지 않는다.
- 검색 경로에서 DB transaction, Catalog 저장 및 enrichment를 추가하지 않는다.
- Swagger와 실제 Controller/DTO가 일치한다.
- 관련 단위/API 테스트가 추가되고 사용자가 검증 명령의 성공 결과를 제공한다.
- 관련 범위 diff 검토에서 명세 불일치, 기존 동작 퇴보 및 계층 위반이 없다.

## Non-goals

- 프론트엔드 탭 구현
- Artist 상세 API, 저장 또는 enrichment
- Track/Album import 및 Board 흐름 변경
- 타입별 별도 endpoint 3개 추가
- response를 타입별 group 구조로 변경
- Spotify field filter 자동 생성이나 검색 알고리즘 전면 교체
- 기존 `GET /api/tracks` 제거

## Verification

Codex는 저장소 정책에 따라 Gradle과 `verify.sh`를 실행하지 않는다.

사용자 실행 명령:

```bash
./gradlew test
./gradlew check
./scripts/verify.sh
```

## Completion Record (2026-10-27)

- 선택적 타입 검색, 기존 통합 검색 호환, Swagger·Server Spec·클라이언트 계약 및 관련 테스트 코드를 정적 검토했다.
- 관련 범위의 `git diff --check`를 통과했다. Codex는 Gradle 및 `verify.sh`를 실행하지 않았다.
- 사용자가 검증 확인 후 완료 처리를 요청했다. 명령별 출력은 전달받지 않아 `test`, `check`, `verify.sh` 각각의 통과 여부는 별도로 확인하지 못했다.
