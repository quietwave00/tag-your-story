# CATALOG-SUBJECT-TAGS-001 — Track·Album 상세 화면용 태그 조회

> 상태: 2026-11-09 사용자 요청으로 계획 종료 및 completed 보관. 사용자 실행 검증·제공자 설정 확인이 남아 있어 수용 기준 통과로 표시하지 않는다.

## Goal

Track·Album 상세 메타데이터 조회와 독립적으로, 각 Spotify ID의 System Tag를 화면에서 요청할 수 있게 한다.

## Decision and Scope

- 공개 `GET /api/catalog/tracks/{spotifyTrackId}/tags`와 `GET /api/catalog/albums/{spotifyAlbumId}/tags`를 추가한다. 두 응답은 기존 `ApiResult` 안에 `systemTags`, `previewTags`, `tagDisplayStatus`만 담는다.
- 상세 화면은 먼저 기존 `POST /api/catalog/import`에 `subjectType=TRACK|ALBUM`과 `spotifyId`를 보내 Catalog 생성·외부 근거 수집·Resolver 계산을 트리거한다. 완료 후 태그 GET을 호출한다. GET은 저장된 결과만 조회하며 외부 HTTP, DB write, Resolver 계산을 수행하지 않는다.
- GET의 Spotify ID에 해당하는 Catalog Track/Album이 아직 없으면 기존 `TRACK_NOT_FOUND`/`ALBUM_NOT_FOUND` 오류 envelope(`response.status=404`, 현재 HTTP 200)를 반환한다. Catalog 대상은 있지만 visible resolved와 preview가 없으면 빈 배열과 `EMPTY`를 반환한다.
- Track 응답은 TRACK subject의 최종 resolved 결과를, Album 응답은 ALBUM subject의 최종 resolved 결과를 사용한다. Track의 Album 상속 결과는 기존 Resolver 정책을 따른다. HIDDEN은 확정 태그에 표시하지 않고, 미리보기는 확정 태그가 없을 때만 제공한다.
- `GET /api/tracks/{trackId}`와 `GET /api/catalog/albums/{spotifyAlbumId}`의 메타데이터 계약은 유지한다. 기존 `POST /api/catalog/import`와 deprecated `POST /api/tracks/import`도 유지한다.
- Artist 태그, 새 Resolver 정책, 데이터베이스 스키마 변경, React 소스 변경은 범위 밖이다. `tag-note-client/agents/client/api-contract.md`는 갱신한다.

## API Contract

| Endpoint | Request | Response | Authentication |
| --- | --- | --- | --- |
| `GET /api/catalog/tracks/{spotifyTrackId}/tags` | Spotify Track ID path | `ApiResult<{systemTags,previewTags,tagDisplayStatus}>` | 없음 |
| `GET /api/catalog/albums/{spotifyAlbumId}/tags` | Spotify Album ID path | 동일 | 없음 |

`systemTags`는 `tagId`, `name`, `score`를 가지며 visible resolved 결과다. `previewTags`는 검증 전 `name`, `source` 최대 5개다. 상태는 `CONFIRMED`, `PREVIEW`, `EMPTY`다. POST import에서 Spotify 실패는 기존 `SPOTIFY_EXCEPTION` envelope를 따른다. 부분 provider 실패는 기존 선택 서비스의 성공 결과를 유지한다.

GET에는 Spotify 요청이 없으므로 `SPOTIFY_EXCEPTION`은 POST import에만 적용한다. GET의 주요 실패는 위 Catalog 대상 부재 오류다.

## Implementation and Acceptance Criteria

1. 두 endpoint, 공통 태그 전용 응답 DTO와 Swagger API interface를 추가한다. Controller는 read-only Application Service에 위임하고 응답만 변환한다.
2. Server Spec과 클라이언트 API 계약 문서의 endpoint, 응답, 오류, 최초 계산 의미를 갱신한다.
3. MVC와 Application 테스트에서 각 Spotify ID 전달, Track/Album subject 분리, 확정·미리보기·빈 상태, 메타데이터 미포함, import 전 대상 부재 오류, GET의 외부 호출·write 부재를 검증한다. 기존 선택 서비스 테스트로 POST import의 최초 계산·재사용 정책을 유지한다.
4. diff와 계층·트랜잭션·쿼리 영향·계약을 정적으로 리뷰한다. Codex는 Gradle/verify.sh를 실행하지 않는다. 사용자 실행 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh` 결과를 확인한 후 completed로 이동하고 progress를 완료 상태로 갱신한다.

## Implementation Record

- 2026-11-02: 사용자가 제안한 두 태그 GET endpoint와 기존 상세 GET 분리 구조를 승인했다.
- 2026-11-02: Controller/API interface/태그 전용 DTO, Server Spec·클라이언트 계약 및 MVC 회귀 테스트를 작성했다. Gradle·verify.sh 실행 결과는 대기 중이다.
- 2026-11-02: 변경된 tracked 문서의 `git diff --check`와 신규 파일의 trailing whitespace 정적 검사를 통과했다. 전체 작업 트리에는 관련 없는 선행 변경이 있어 해당 파일만 검토했다. 트랜잭션·Resolver·DB 스키마·기존 상세 API는 수정하지 않았다.
- 2026-11-02: 사용자가 화면에서 POST import를 명시적으로 트리거하고 GET은 조회만 해야 한다고 정정했다. 최초 GET이 import하는 설계는 이 요청과 달라졌으므로 GET을 저장 결과 전용으로 변경한다. 이 변경은 승인된 작업의 API 동작 수정이며, 사용자 정정을 Human Review 근거로 기록한다.
- 2026-11-02: GET Controller를 read-only Application Service에 연결하고 기존 resolved/preview reader를 재사용하도록 수정했다. MVC 및 Application 테스트를 변경하고 Server Spec·Swagger·클라이언트 계약을 POST→GET 호출 순서로 정렬했다. 사용자 실행 검증은 대기 중이다.
- 2026-11-02: 사용자가 태그·Observation 테이블에 row가 없음을 보고했다. 기존 Track/Album 선택 서비스는 활성 외부 제공자가 0개여도 빈 `providerResults`의 `allMatch`를 참으로 평가해 완료 marker를 기록한다. 이러면 제공자를 나중에 켜도 재수집이 건너뛰어질 수 있으므로, 제공자 결과가 1개 이상일 때만 빈 결과 완료 marker를 남기도록 범위를 조정한다. 기존 marker 일괄 삭제나 제공자 설정 변경은 이 코드 수정에 포함하지 않는다.
- 2026-11-02: Track/Album 선택 서비스의 완료 조건을 보강하고, 제공자 0개는 완료하지 않으며 제공자의 정상 EMPTY 결과는 완료하는 회귀 테스트를 추가했다. 관련 파일의 `git diff --check`를 통과했다. 사용자 실행 테스트와 현재 DB의 기존 완료 marker 확인을 기다린다.

## Closure Record (2026-11-09)

사용자 요청에 따라 active 목록에서 completed로 보관한다. `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`의 사용자 실행 결과, 실제 제공자 설정과 기존 완료 marker의 검토는 미확인이다. 태그 조회 계약은 후속 재설계의 현행 기준선이며, 이 이동을 검증 통과로 해석하지 않는다.
