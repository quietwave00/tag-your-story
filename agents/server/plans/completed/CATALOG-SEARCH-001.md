# CATALOG-SEARCH-001 — Spotify 통합 Catalog 검색

- Status: Active
- Scope: Spotify Track/Album/Artist 통합 검색 API와 로컬 유사도 정렬
- Non-goal: Track/Album 선택·Import 변경, Artist 상세/저장, enrichment 변경, Spotify 태그 evidence 추가

## Goal

기존 Track 검색 API를 유지하면서, 사용자가 하나의 keyword로 Spotify Track, Album, Artist 후보를 함께 조회하고 후보의 종류를 명확히 식별할 수 있는 읽기 전용 Catalog 검색 API를 추가한다.

## Changes

1. `CatalogSearchProvider` port와 provider-neutral 검색 모델을 추가한다.
2. `SpotifyWebClient`에 Track/Album/Artist를 한 요청으로 조회하는 Search Item 호출을 추가한다.
3. Spotify adapter에서 세 결과 그룹을 공통 모델로 변환한다.
4. Application service에서 검색어를 한 번 기록하고 현재 page 후보를 합쳐 결정적으로 정렬한다.
5. `GET /api/catalog/search?keyword={keyword}&page={page}`와 분리된 Swagger interface/response DTO를 추가한다.
6. 기존 `GET /api/tracks` 계약과 구현은 유지한다.
7. Server Spec과 System Tag 아키텍처의 검색 흐름을 새 결정에 맞춘다.

## Ranking Policy

정규화된 keyword와 후보 표시 이름을 비교해 다음 우선순위로 정렬한다.

1. 이름 exact match
2. 이름 prefix match
3. 이름 문자열 유사도
4. artist 이름 문자열 유사도
5. 동일 subject type 안의 Spotify 원본 순서
6. `TRACK`, `ALBUM`, `ARTIST` 순서
7. Spotify ID 사전순

문자열 정규화는 Unicode NFKC, 소문자화, trim, 연속 공백 축약을 사용한다. 유사도는 정규화 문자열의 Levenshtein distance를 길이로 보정한 `0..1` 값이다.

## Acceptance Criteria

- 새 API가 TRACK, ALBUM, ARTIST 후보를 하나의 `items` 목록으로 반환한다.
- 각 item은 `subjectType`, `spotifyId`, `title`, `artistName`, `albumName`, `imageUrl`을 제공한다.
- Spotify Search Item 요청은 `track,album,artist`, type별 limit 10, `offset=page*10`을 사용한다.
- 최대 30개인 현재 page 후보에 승인된 ranking policy가 결정적으로 적용된다.
- `totalCount`는 세 type total의 합이다.
- 검색어는 요청당 한 번만 기록된다.
- 검색 과정에서 JPA repository 접근, Catalog 저장, enrichment가 발생하지 않는다.
- 기존 `GET /api/tracks`의 endpoint와 response contract가 바뀌지 않는다.
- Swagger가 새 endpoint, query parameter, response와 Spotify 실패 응답을 문서화한다.
- 관련 단위/API 테스트가 추가된다.
- 사용자가 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh` 성공 결과를 제공한다.

## Verification

Codex는 저장소 정책에 따라 Gradle과 `verify.sh`를 실행하지 않는다.

사용자 실행 명령:

```bash
./gradlew test
./gradlew check
./scripts/verify.sh
```

## Completion Record — 2026-09-18

- Spotify Track/Album/Artist 통합 검색 API, provider-neutral port/모델, Spotify adapter,
  결정적 로컬 ranking, 분리된 Swagger interface 및 Controller/API 테스트를 구현했다.
- 기존 `GET /api/tracks` 계약은 유지했고, Catalog search 경로가 Catalog 저장이나
  enrichment를 수행하지 않도록 분리했다.
- 사용자가 대상 테스트와 전체 `test`, `check`, `scripts/verify.sh` 통과를 확인했다.
- Acceptance Criteria 및 범위 diff를 정적 검토했으며, 관련 Plan을 completed로 이동한다.
