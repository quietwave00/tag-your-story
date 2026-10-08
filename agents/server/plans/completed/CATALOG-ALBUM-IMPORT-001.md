# CATALOG-ALBUM-IMPORT-001 — Subject Type 기반 Catalog Import

## Status

- State: Closed at user request on 2026-09-28; latest user verification not recorded

## Scope

- `POST /api/catalog/import`는 통합 검색의 `subjectType`/`spotifyId`를 입력받는다.
- `TRACK`은 기존 Track 선택 흐름을 재사용하고, `ALBUM`은 Spotify Album 상세 조회로 Album/Artist Catalog를 생성하거나 재사용한다.
- Album 선택은 Album에 속한 외부 근거만 수집하고 ALBUM subject의 resolved tag와 preview를 반환한다.
- `ARTIST`는 import 대상에서 제외한다. 기존 `/api/tracks/import` 동작은 유지하고 Swagger에서 deprecated로 표시한다.

## Acceptance Criteria

- Album ID가 Spotify Track 상세 API로 전달되지 않는다.
- 기존 Album의 resolved projection이 있으면 외부 enrichment를 재실행하지 않는다.
- Album tag 응답에는 TRACK subject의 결과가 섞이지 않는다.
- Album의 빈 처리 결과도 완료 marker로 구분하여 반복 선택 때 외부 요청을 재실행하지 않는다.
- Album/Artist unique 충돌은 기존 Catalog conflict 정책 범위에서 복구한다.
- MusicBrainz Album 검색은 canonical 제목을 첫 요청에 사용한다. edition qualifier가 있고 일치 후보가 없을 때만 원문 제목으로 한 번 재검색한다. Artist, Release Group 연도, 유일성 조건은 유지한다.
- Swagger, Controller, Application 및 테스트를 함께 갱신한다.

## Verification

- Codex: diff 및 정적 검토만 수행한다.
- 사용자: `./gradlew :tagnote-core:test --tests "*AlbumSelectionServiceTest"` 및 `./gradlew :tagnote-api:test --tests "*CatalogImportControllerTest"`, 전체 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh` 실행.
- 사용자 결과와 diff 리뷰 확인 전까지 active 상태로 유지한다.

## 2026-09-28 보완

`Nevermind (Remastered)` Album import의 MusicBrainz branch가 Release Group `Nevermind`를
찾지 못했다. 기존 edition title normalizer는 Discogs/Last.fm 경로에서 사용되지만
MusicBrainz Album 검색에는 연결되지 않아, 동일한 정규화를 첫 검색에 적용했다.
원문 Release Group 자체가 별도로 존재하는 경우에만 원문 제목으로 bounded fallback한다.
Release Group 최초 발매연도와 Spotify edition 발매연도가 다른 문제는 별도 Release
검증 모델이 필요하므로 이번 보완에서 연도 조건을 제거하지 않는다.

## Closure note

2026-09-28 사용자 요청에 따라 completed로 이동했다. Album import와 MusicBrainz 정규화 변경을 포함한 대상 테스트 및 전체 검증 결과는 전달받지 못했으므로, 수용 기준 통과를 검증 완료로 표시하지 않는다.
