# CATALOG-RELEASE-TYPE-001 — Album 상세 발매 유형

> 상태: 2026-10-30 사용자 테스트 확인 및 완료 요청으로 completed.

## Goal

`GET /api/catalog/albums/{spotifyAlbumId}`에서 Spotify 발매 유형과 사용자 지정 곡 수·재생 시간 기준을 조합한 발매 분류를 제공한다.

## Decision and Scope

- 기존 Album 상세 응답에 `releaseType: ALBUM | COMPILATION | SINGLE_EP`를 추가한다. JSON 값에는 `/`를 쓰지 않는다.
- Spotify `album_type=compilation`은 `COMPILATION`, `single`은 `SINGLE_EP`다.
- `album_type=album`은 6곡 이하 또는 총 재생 시간 30분 미만이면 `SINGLE_EP`, 그 외 `ALBUM`이다. 정확히 30분은 미만이 아니다.
- 총 재생 시간이 필요할 때는 앨범 상세에 포함된 Track 페이지를 우선 사용한다. 부족하면 후속 페이지를 조회하고 30분 이상이 확인되면 중단한다.
- 불완전한 Spotify metadata 또는 추가 Track 페이지 조회 실패로 분류할 수 없으면 추측하지 않고 nullable로 반환한다. 원래 응답 필드는 유지한다.
- Artist Album 목록, Catalog 검색·import·저장·태그·DB는 범위 밖이다. 기존 Spotify 조회 실패 계약을 유지한다.

## Implementation

1. Spotify 상세 adapter에서 `album_type`, `total_tracks` 및 필요 시 Track 총 재생 시간을 provider-neutral model에 담는다.
2. Application model의 단일 분류 규칙으로 `releaseType`을 계산하고 API DTO에 노출한다.
3. Swagger interface, Server Spec, 클라이언트 API 계약 및 ADR을 갱신한다.
4. 컴필레이션 우선순위, Spotify 싱글, 6/7곡·30분 경계, 페이지가 나뉜 Track 및 기존 응답 회귀 테스트를 작성한다.
5. 정적 diff를 검토하고 사용자 실행 검증 결과를 기다린다.

## Acceptance Criteria

- 세 분류 값과 6곡 이하·30분 미만 규칙이 일치한다.
- 상세 조회는 Catalog 저장·enrichment·DB transaction을 실행하지 않는다.
- 기존 필드·오류 응답·별도 목록 API 계약을 유지한다.
- Swagger·Server Spec·클라이언트 계약이 실제 응답과 일치한다.
- 사용자 실행 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh` 결과 확인과 리뷰 후 completed로 이동한다.

## Implementation Record

- 2026-10-29: Album 상세 provider model에 Spotify `album_type`과 재생 시간 관측값을 추가하고 단일 Application 분류 규칙으로 `releaseType`을 계산했다.
- 상세의 Track 페이지로 30분 이상이 확인되면 추가 조회를 멈춘다. 필요한 페이지에서 Spotify 오류가 나면 상세 정보는 유지하고 `releaseType`을 null로 둔다.
- Swagger·Server Spec·클라이언트 API 계약, 경계·추가 페이지·오류 회귀 테스트를 갱신했다.
- Spotify Java SDK 8.0.0 JAR의 `Album.getAlbumType(): AlbumType`, `TrackSimplified.getDurationMs(): Integer`를 정적 확인했다. Gradle·verify.sh는 실행하지 않았으며 사용자 검증 결과 대기 중이다.
- 2026-10-30: 사용자가 이 Plan의 테스트 확인과 completed 이동을 요청했다. 응답 DTO·분류 규칙·계약을 정적으로 재검토했다. Codex는 Gradle·verify.sh를 실행하지 않았고 명령별 출력은 전달받지 않아 `check`·`verify.sh`의 개별 통과 여부는 미확인으로 남긴다.
