# CATALOG-ARTIST-ALBUM-RELEASE-TYPE-001 — Artist Album 목록 발매 유형

> 상태: 2026-10-30 사용자 요청과 재사용·효율 기준 검토 후 구현 중. 사용자 실행 검증 대기.

## Goal

Artist Album 목록의 각 항목에 Album 상세와 동일한 `releaseType`을 제공한다.

## Decision and Scope

- `GET /api/catalog/artists/{spotifyArtistId}/albums`의 기존 item 필드를 유지하고 nullable `releaseType`을 추가한다.
- ADR-021의 `CatalogReleaseType.classify`를 재사용한다. `single`과 `compilation`은 목록의 유형만으로 분류한다.
- SDK 8.0.0의 `AlbumSimplified`에는 총 곡 수 접근자가 없어 `album` 항목만 Album Track 목록을 조회한다. 총 곡 수가 6 이하이면 후속 조회 없이 `SINGLE_EP`다. 7곡 이상은 30분 경계까지 필요한 페이지를 조회한다.
- 개별 Album Track 조회 오류나 불완전한 데이터는 그 항목의 `releaseType=null`로 처리한다. 원래 Artist Album 목록 조회 오류는 기존 계약을 따른다.
- DB 조회·저장·transaction은 추가하지 않는다. 목록 순서와 `totalCount`는 그대로 유지한다.

## Implementation and Acceptance Criteria

1. Spotify adapter에서 필요한 항목에만 Track 조회를 수행하고 기존 분류 함수를 호출한다.
2. Provider model, API DTO·Swagger interface, Server Spec, 클라이언트 API 계약을 갱신한다.
3. 직접 분류·곡 수 경계·30분 경계·추가 페이지·개별 실패·목록 순서 및 기존 응답을 테스트한다.
4. 정적 diff를 검토한다. 사용자 실행 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh` 결과와 리뷰 후 completed로 이동한다.

## Implementation Record

- 2026-10-30: 기존 `CatalogReleaseType.classify`와 `observedDurationMs`를 목록에서도 사용했다. `single`·`compilation`에는 추가 Spotify 호출이 없고, `album`은 첫 Track page에서 곡 수를 확인한 뒤 필요한 경우만 후속 page를 읽는다.
- Artist Album 목록 DTO·Swagger·Server Spec·클라이언트 계약·테스트를 갱신했다. 개별 Album Track 조회 실패는 해당 항목의 null 유형으로 처리한다.
- 변경 파일의 `git diff --check`는 통과했다. Gradle과 verify.sh는 실행하지 않았으며 사용자 실행 검증을 기다린다.
- 2026-11-02: 구현 재점검에서 목록의 `total_tracks`와 Track 응답 건수 불일치, 불완전한 첫 Track 페이지를 null 분류로 처리하도록 보강했다. compilation·원본 순서/건수·정확히 30분·후속 페이지·불완전한 metadata·원본 목록 오류 전파 테스트를 추가했다. 변경 코드 파일 대상 `git diff --check`는 통과했다. 사용자 실행 검증은 아직 받지 않았다.
