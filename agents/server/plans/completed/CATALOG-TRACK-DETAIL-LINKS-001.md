# CATALOG-TRACK-DETAIL-LINKS-001 — Track 상세의 Artist·Album 이동 ID

> 상태: 2026-10-30 사용자 테스트 확인 및 완료 요청으로 completed.

## Goal

Track 상세 화면의 Artist·Album 이름에서 각 상세 화면으로 이동한다.

## Decision and Scope

- `GET /api/tracks/{trackId}`의 기존 `TrackData`에 `spotifyArtistId`, `spotifyAlbumId`를 추가한다. 이름·이미지·오류 계약은 유지한다.
- Artist ID는 현재 이름으로 표시하는 Spotify Track의 첫 번째 Artist ID다. Album ID는 같은 Track의 Album ID다. 외부 호출이나 DB SELECT를 추가하지 않는다.
- 클라이언트는 ID가 있는 이름만 기존 Artist·Album 상세 경로로 연결한다. ID가 없는 응답에서는 이름을 그대로 보여준다. 기존 상세→상세 뒤로가기 상태를 이어간다.
- Catalog 검색, import, 영속성 및 Tag 정책은 변경하지 않는다.

## Implementation

1. TrackService의 기존 Spotify Track 응답에서 두 ID를 TrackData에 매핑한다.
2. 기존 API 모듈의 `DetailTrackResponse`를 실제 상세 응답에 연결하고 Swagger interface·DTO Schema, Server Spec, 클라이언트 API 계약을 실제 JSON에 맞춘다.
3. Track 상세 화면에서 ID 기반 상세 링크를 만들고 기존 내비게이션 상태를 전달한다.
4. 서비스·Controller·클라이언트 테스트에서 ID 매핑, 기존 필드, 링크·뒤로가기, ID 부재 fallback을 검증한다.
5. 정적 diff 검토 후 사용자 실행 검증 결과를 기다린다.

## Acceptance Criteria

- Track 상세 API의 기존 응답 필드는 유지되고 두 Spotify ID가 추가된다.
- 현재 표시되는 첫 Artist 이름과 `spotifyArtistId`가 같은 credit에서 나온다.
- Track 상세 조회에 Spotify 추가 호출 또는 DB SELECT가 없다.
- 클라이언트에서 두 이름이 각각 올바른 상세 경로로 이동하고 뒤로가기로 Track에 복귀한다.
- 서버와 클라이언트 테스트·검증 결과 및 리뷰 확인 후 completed로 이동한다.

## Verification

Codex는 저장소 규칙에 따라 Gradle 및 `verify.sh`를 실행하지 않는다. 사용자 실행: `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`. 클라이언트 실행: `cd tag-note-client/tag-note-react && npm run test && npm run lint && npm run build`.

## Implementation Record

- 2026-10-29: 기존 Spotify Track 단건 응답의 첫 Artist ID와 Album ID를 `TrackData`에 담았다. 새로운 Repository 조회·Spotify 호출은 없다.
- API 모듈의 기존 `DetailTrackResponse`를 상세 Controller에 연결하고 Swagger Schema·Server Spec·클라이언트 계약을 갱신했다. 기존 JSON 필드를 유지한다.
- Track 화면의 이름을 ID가 있을 때만 상세 링크로 바꾸고 기존 뒤로가기 상태를 전달한다. 서비스·Controller·클라이언트 테스트를 갱신했다.
- 변경 파일 `git diff --check`를 통과했다. Gradle·verify.sh와 클라이언트 테스트·lint·build는 실행하지 않았으며 사용자 실행 검증 결과 대기 중이다.
- 2026-10-30: 사용자가 이 Plan의 테스트 확인과 completed 이동을 요청했다. Track ID 매핑·응답 DTO·클라이언트 링크를 정적으로 재검토했다. Codex는 Gradle·verify.sh 및 클라이언트 명령을 실행하지 않았고 명령별 출력은 전달받지 않아 `check`·`verify.sh`·클라이언트 lint/build의 개별 통과 여부는 미확인으로 남긴다.
