# CATALOG-DETAIL-001 — Spotify Album·Artist 상세 조회

> 상태: 2026-10-27 사용자 Human Review 승인, 구현 및 정적 검토 완료. 사용자 실행 검증 결과 대기. 팔로워 수는 사용자 요청으로 제외한다.

## Goal

Catalog 검색 결과의 Spotify Album·Artist ID로 공개 상세 메타데이터를 조회한다. 기존 Track 상세 API와 Catalog import 계약은 유지한다.

## Decision and Scope

- 공개 API는 Catalog 경계의 `GET /api/catalog/albums/{spotifyAlbumId}`와 `GET /api/catalog/artists/{spotifyArtistId}`에 둔다. Track 전용 호환 Controller에 새 subject를 추가하지 않는다.
- Album 응답: `spotifyId`, `title`, `imageUrl`, `releaseDate`, `totalTracks`, `artists[{spotifyId,name}]`.
- Artist 응답: `spotifyId`, `name`, `imageUrl`, `genres`. 팔로워 수는 포함하지 않는다.
- 인증 불필요. Spotify 메타데이터 읽기만 수행하며 Catalog 저장, DB 조회, enrichment 및 DB transaction을 추가하지 않는다.
- 이미지가 없으면 기존 `NO_IMAGE` 규칙을 사용한다. Spotify 장애는 기존 `SPOTIFY_EXCEPTION` 응답 체계를 유지한다.
- Album track 목록과 Artist album 목록은 별도 pagination 계약이 필요하므로 이번 상세 메타데이터 범위에 포함하지 않는다.
- 클라이언트 소스는 수정하지 않는다. 공개 API 문서인 `tag-note-client/agents/client/api-contract.md`만 서버 컨벤션에 따라 갱신한다.

## Implementation

1. `CatalogDetailApi` Swagger interface와 `CatalogDetailController`를 추가한다.
2. Application detail model/port/service와 Spotify adapter를 통해 SDK 객체를 공개 DTO와 분리한다.
3. 기존 Album 상세 Spotify HTTP 호출을 재사용하고 Artist 상세 호출을 추가한다.
4. Server Spec과 클라이언트 API 계약 문서를 구현과 일치시킨다.
5. Controller 및 Adapter 단위 테스트로 두 경로, 응답 필드, 이미지 부재, Spotify 예외 경계를 확인한다.

## Acceptance Criteria

- Album·Artist 검색 결과의 `spotifyId`로 각각 공개 상세 API를 호출할 수 있다.
- 응답은 명시한 필드만 포함하며 팔로워 수가 없다.
- Album Artist credit 순서를 보존하고 이미지 부재 시 `NO_IMAGE`를 반환한다.
- 두 GET 경로는 저장·enrichment·트랜잭션을 수행하지 않는다.
- 기존 `/api/tracks/{trackId}` 및 Catalog 검색/import 계약은 변경하지 않는다.
- Swagger, Server Spec, 클라이언트 API 계약 문서와 실제 구현이 일치한다.
- 사용자가 검증 명령 실행 결과를 전달하고 관련 범위 diff review를 마친 뒤 completed로 이동한다.

## Verification

Codex는 저장소 규칙에 따라 Gradle 및 `verify.sh`를 직접 실행하지 않는다. 사용자 실행 명령: `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`.

## Implementation Record

- Catalog 경계에 Album·Artist 공개 상세 API, Spotify adapter 및 응답 DTO를 추가했다.
- 프로젝트의 Spotify Java SDK 8.0.0에는 `Album.getTotalTracks()`가 없어 Album `tracks.total`을 사용한다.
- Controller·adapter 테스트, Swagger, Server Spec 및 클라이언트 API 계약 문서를 갱신했다. 클라이언트 소스는 변경하지 않았다.
- Gradle·verify.sh는 실행하지 않았다. 사용자 실행 결과를 확인한 뒤 Plan 완료 여부를 갱신한다.
