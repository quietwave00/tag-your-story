# SEARCH-RANK-LIMIT-001 — 검색어 랭킹 상위 10개

> 상태: 2026-10-30 사용자 테스트 확인 및 완료 요청으로 completed.

## Goal

`GET /api/tracks/ranking`이 Redis ZSET 점수 역순으로 검색어 최대 10개를 반환하도록 계약과 검증을 정렬한다.

## Decision and Scope

- 사용자가 이미 변경한 `RedisSearchKeywordTracker.reverseRangeWithScores("search_keyword:", 0, 9)`를 유지한다.
- 응답 envelope과 `keywordList` 형태, Redis key·score 증가 방식·정렬 순서는 유지한다.
- 랭킹 API 테스트, Swagger, Server Spec, 클라이언트 API 계약을 최대 10개에 맞춘다.
- Catalog 검색 결과 페이지 크기와 Last.fm top tag 상위 5개 정책은 범위 밖이다.

## Implementation

1. Redis tracker 테스트를 `0..9` 조회와 10개 순서·값 검증으로 갱신한다.
2. Track 랭킹 Swagger 및 서버·클라이언트 계약 문서를 갱신한다.
3. 정적 diff를 검토하고 사용자 실행 검증 결과를 기다린다.

## Acceptance Criteria

- Redis 조회 범위가 0-based `0..9`이고 최대 10개 keyword만 반환한다.
- 공개 응답 구조와 검색어 기록 동작은 유지한다.
- Swagger와 문서가 실제 구현·테스트와 일치한다.
- 사용자 실행 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh` 결과와 리뷰 확인 후 completed로 이동한다.

## Implementation Record

- 2026-10-29: 사용자의 기존 Redis 조회 `0..9` 변경을 유지하고 Mockito 테스트를 같은 범위 및 10개 순서 검증으로 갱신했다.
- Swagger·Server Spec·클라이언트 API 계약을 최대 10개로 정렬했다. 클라이언트 홈은 응답 목록 전체를 표시하므로 화면 코드 변경은 필요하지 않다.
- Gradle·verify.sh는 실행하지 않았으며 사용자 실행 검증 결과를 기다린다.
- 2026-10-30: 사용자가 이 Plan의 테스트 확인과 completed 이동을 요청했다. Redis `0..9` 조회·Mockito stub·응답 계약을 정적으로 재검토했다. Codex는 Gradle·verify.sh를 실행하지 않았고 명령별 출력은 전달받지 않아 `check`·`verify.sh`의 개별 통과 여부는 미확인으로 남긴다.
