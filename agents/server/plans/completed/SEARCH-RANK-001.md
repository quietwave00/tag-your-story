# SEARCH-RANK-001 — Search Keyword Port Boundary

- Status: Completed (2026-08-20)
- Scope: 검색어 기록/랭킹 조회 포트 분리와 Redis 구현체의 Infrastructure 이동
- Non-goal: Track 상세조회 리팩토링, API contract 변경, Redis 자료구조 변경

## Goal

기존 검색어 기록과 상위 5개 랭킹 동작을 유지하면서 Application이 역할별 포트에 의존하고 Redis 구현 세부사항은 Infrastructure에만 위치하도록 한다.

## Changes

1. Application에 `SearchKeywordRankingReader` output port를 추가한다.
2. 기존 `SearchKeywordTracker`를 `infrastructure.external.redis.RedisSearchKeywordTracker`로 이동하고 기록/조회 포트를 모두 구현한다.
3. `TrackSearchService`는 기존 `SearchKeywordRecorder` 의존성을 유지한다.
4. legacy `TrackService`는 concrete Redis 구현체 대신 `SearchKeywordRankingReader`에 의존한다.
5. 기존 Redis key, 증가 score, 조회 범위, 결과 순서와 공개 API contract를 유지하는 테스트를 갱신한다.

## Acceptance Criteria

- Application/legacy service가 Redis 구현체를 직접 참조하지 않는다.
- Redis 구현체가 Domain 패키지에 남지 않는다.
- 검색 기록은 `search_keyword:` ZSET score를 1 증가시킨다.
- 랭킹은 기존과 동일하게 score 역순 상위 5개 keyword만 반환한다.
- Track 랭킹 API의 endpoint와 response shape는 바뀌지 않는다.
- `./gradlew test`, `./gradlew check`가 통과한다.

## Files

- Add: `tagnote-core/src/main/java/com/tagnote/application/catalog/search/port/SearchKeywordRankingReader.java`
- Move/rename: `core/domain/tracks/util/SearchKeywordTracker.java` -> `infrastructure/external/redis/RedisSearchKeywordTracker.java`
- Update: `tagnote-core/src/main/java/com/tagnote/core/domain/tracks/service/TrackService.java`
- Update: 관련 TrackService/Redis adapter 테스트
- Complete: `agents/server/progress.md`, Plan 이동

## Verification

- PASS: Search Application, TrackService, Redis adapter 대상 테스트
- PASS: Track ranking Controller 회귀 테스트
- PASS: `./gradlew test`
- PASS: `./gradlew check`
- PASS: `./scripts/verify.sh`
- 실행 환경: Windows JDK 17, Gradle wrapper
