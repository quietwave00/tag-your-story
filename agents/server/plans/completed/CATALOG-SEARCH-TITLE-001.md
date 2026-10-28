# CATALOG-SEARCH-TITLE-001 — Album 제목 필터 검색

> 상태: 2026-10-28 사용자 결과 검증 및 완료 요청에 따라 완료 처리. Codex는 Gradle·verify.sh를 실행하지 않았으며 명령별 출력은 전달받지 않아 개별 통과 여부는 미확인이다.

## Goal

Album 검색에서 정확한 제목 후보가 일반 키워드 검색의 뒤쪽 window에 밀리는 빈도를 줄인다.

## Decision and Scope

- 기존 화면의 `GET /api/catalog/search?type=ALBUM`과 화면 전용 `GET /api/catalog/search/screen`은 같은 Album 제목 필터 후보를 사용한다.
- Album은 Spotify `q=album:{keyword}`, `type=album`으로 조회하고, Track·Artist는 일반 keyword와 `type=track,artist`로 조회한다. 3-page window의 offset 0,10,20 정책을 유지한다.
- 각 window의 Spotify 검색은 최대 6번이고 Redis cache key를 버전 변경해 기존 Album 후보를 섞지 않는다.
- 타입 생략 통합 검색, `/api/tracks`, 응답 DTO, 클라이언트 소스 및 상세·import API는 변경하지 않는다.
- `album:` 필터는 정확 제목의 전역 1페이지 배치를 보장하지 않는다. Album 이름 외 필드로 검색하던 결과는 줄어들 수 있다. 변경 결정은 ADR-020에 기록한다.

## Implementation

1. Spotify adapter의 화면 window 후보 조회를 일반 Track·Artist 질의와 제목 필터 Album 질의로 분리한다.
2. cache key를 버전 변경하고 Swagger, Server Spec, 클라이언트 API 계약을 갱신한다.
3. 기존 통합 검색 회귀, Album 질의·total·순서, 타입별/화면 API 동일 후보 테스트를 갱신한다.

## Acceptance Criteria

- `type=ALBUM`과 화면 API Album 영역 모두 Spotify `album:` 제목 필터의 결과를 사용한다.
- Track·Artist는 일반 검색을 유지하고 타입 생략 기존 통합 검색은 유지한다.
- 첫 window는 offset 0,10,20 각각에서 일반·Album 질의를 한 번씩 수행하며 총 최대 6번이다.
- 필터 Album total을 반환하고 기존 page/hasNext/중복 제거 정책을 유지한다.
- 이전 Redis window key의 데이터는 새 정책에 사용되지 않는다.
- Swagger, Server Spec, 클라이언트 API 계약과 실제 구현이 일치한다.
- 사용자 실행 검증 결과와 diff review 후 completed로 이동한다.

## Verification

Codex는 Gradle 및 `verify.sh`를 직접 실행하지 않는다. 사용자 실행 명령은 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`다.

## Implementation Record

- Spotify 화면 window 조회를 일반 Track·Artist 검색과 `album:` 제목 필터 Album 검색으로 분리했다. 기존 타입 생략 통합 검색은 그대로 둔다.
- Album total은 필터 검색의 Spotify total로 전환했고 응답 DTO·페이지 정책·정렬 규칙은 유지했다.
- Redis window key를 `albumTitleV1`로 버전 변경해 이전 일반 Album 후보를 재사용하지 않는다.
- Adapter 요청·total 회귀 테스트와 cache key 테스트를 갱신했다. Swagger, Server Spec, 클라이언트 API 계약을 실제 호출과 맞췄으며 클라이언트 소스는 수정하지 않았다.
- Gradle·verify.sh는 실행하지 않았다. 정적 diff review를 마쳤다.
- 2026-10-28 사용자가 결과 검증을 확인하고 server의 active Plan 전체 완료 처리를 요청했다. 이를 이 Plan의 사용자 검증 및 Human Review 근거로 기록한다. 명령별 실행 출력은 전달받지 않아 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`의 개별 통과 여부는 미확인이다.
