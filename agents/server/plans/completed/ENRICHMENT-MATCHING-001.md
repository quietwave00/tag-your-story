# ENRICHMENT-MATCHING-001 — External Catalog Matching Fallbacks

## Status

- State: Closed at user request on 2026-09-28; latest user verification not recorded
- Approved: 2026-09-28
- Related decision: `ADR-012-external-catalog-candidate-disambiguation.md`

## Goal

같은 ISRC에 연결된 MusicBrainz Recording 복수 후보를 안전하게 좁히고, Spotify와 Discogs의 box set title 표현 차이 때문에 존재하는 Album을 놓치는 문제를 해결한다.

## Scope

### In Scope

- MusicBrainz ISRC 복수 후보에서 title, Artist, duration tolerance를 통과한 후보의 유일한 최소 duration 차이 선택
- 최소 duration 차이 동률 및 duration 누락 시 기존 ambiguity 유지
- Discogs search field와 검색값, 검증용 Album title을 담는 search attempt 모델 추가
- Discogs 검색 순서를 만드는 search plan 분리
- canonical Album title, 제한된 box set base title, Track title + Track Artist 순서의 fallback
- `release_title`과 `track` query 지원
- 각 attempt의 MASTER → RELEASE 순서와 복수 후보 거부 유지
- matching/provider/client 단위 테스트 갱신

### Out of Scope

- Discogs ISRC 검색
- 임의 fuzzy score 또는 첫 번째 후보 선택
- 일반적인 모든 괄호 suffix 제거
- timeout/retry/backoff 정책 변경
- confidence, Observation, Assertion, Resolver 정책 변경
- API/Swagger 또는 DB schema 변경

## Acceptance Criteria

- `GBAYE9200070` 형태처럼 복수 ISRC 후보가 모두 tolerance를 통과해도 유일하게 duration이 가까운 후보를 선택한다.
- 최소 duration 차이가 같은 후보는 선택하지 않는다.
- `5 Album Set (A/B/C)`는 원문 검색 실패 후 `5 Album Set` 검색으로 유일한 Discogs Album을 확정할 수 있다.
- 첫 attempt 성공 시 추가 Discogs 검색을 실행하지 않는다.
- 한 attempt에서 복수 후보가 남으면 다음 fallback을 실행하지 않는다.
- Track title fallback은 Album identity 검증을 통과한 유일 후보만 채택한다.

## Verification

사용자 실행:

```powershell
.\gradlew.bat :tagnote-core:test --tests "*MusicBrainzEntityMatchingServiceTest" --tests "*MusicBrainzExternalTagProviderTest" --tests "*DiscogsAlbumSearchPlanTest" --tests "*DiscogsAlbumMatchingServiceTest" --tests "*DiscogsExternalTagProviderTest" --tests "*DiscogsClientTest"
```

전체 검증:

```powershell
.\gradlew.bat test
.\gradlew.bat check
```

## Closure note

2026-09-28 사용자 요청에 따라 completed로 이동했다. 이 변경 이후 대상 테스트와 전체 검증의 실행 결과는 전달받지 못했으므로, 수용 기준 통과를 검증 완료로 표시하지 않는다.
