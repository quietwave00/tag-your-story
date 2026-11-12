# ADR-026 — MusicBrainz ISRC 미발견 시 metadata 검색

## Decision

Spotify Track의 MusicBrainz Recording 식별은 발매 유형과 관계없이 ISRC 조회를 우선한다. ISRC가 없거나, ISRC 조회가 HTTP 404(`NOT_FOUND`) 또는 빈 후보를 반환하거나, 반환된 후보 중 기존 검증을 통과한 Recording이 없으면 Track 제목과 Artist 이름으로 metadata 검색을 수행한다. metadata 후보는 기존 제목·Artist·재생 시간 검증을 거친다.

HTTP 404는 해당 ISRC가 MusicBrainz에 등록되지 않았다는 조회 결과로 취급한다. 인증 오류, rate limit, 서버 오류, timeout 등 다른 HTTP 실패는 이 fallback의 조건이 아니다. 싱글/EP에만 별도 검색 순서를 적용하지 않는다.

후보 선택 규칙은 이 결정에서 변경하지 않는다. ISRC 복수 후보의 선택은 ADR-012를 따른다. metadata 후보는 기존 유일성 규칙을 따르되, ISRC HTTP 404 뒤 metadata fallback의 복수 유효 후보 선택에는 후속 ADR-028을 적용한다.

## Reason

사용자 진단 표본 `Billboard Style`의 ISRC `ZZA0Z2601215` 조회는 HTTP 404였지만, 독립적인 제목·Artist 검색은 Recording 후보 2개를 반환했다. 현재 구현은 404를 예외로 종료해 이 후보를 검색하지 않는다. ISRC를 먼저 사용하면 같은 제목의 다른 녹음을 구별할 기회를 유지하면서, ISRC 등록이 없는 Track의 metadata 검색 경로도 확보할 수 있다.

Spotify의 `single` 발매에서 태그가 비는 현상만으로 싱글/EP 전체의 ISRC 우선순위를 바꿀 근거는 없다. 이 fallback은 태그의 존재를 보장하지 않으며 후보 식별 실패를 줄이는 조치다.

## Consequence

- ISRC 404 또는 후보 미일치 Track에는 MusicBrainz metadata 검색 요청이 추가된다. ISRC 조회가 유효한 Recording을 확정하면 추가 요청은 없다.
- metadata 검색에서도 후보 검증을 통과하지 못하면 기존처럼 `NOT_FOUND`다. ISRC HTTP 404 뒤 복수 유효 후보는 ADR-028의 첫 유효 후보 정책으로 선택한다.
- MusicBrainz의 다른 HTTP 실패와 다른 제공자의 부분 성공 정책은 유지한다.
- 2026-11-06 사용자 요청으로 active `ENRICHMENT-DIAGNOSTICS-001`의 고도화 범위에 포함됐다. 구현과 사용자 실행 검증은 아직 남아 있다.
