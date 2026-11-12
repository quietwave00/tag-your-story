# ADR-028 — MusicBrainz ISRC 404 후 첫 유효 metadata Recording 선택

## Decision

ADR-026에 따라 MusicBrainz ISRC 조회가 HTTP 404(`NOT_FOUND`)여서 Track 제목·Artist metadata 검색으로 넘어간 경우, 기존 제목·Artist·재생 시간 검증을 통과한 Recording 후보가 두 개 이상이면 MusicBrainz 검색 응답 순서에서 **첫 번째 유효 후보**를 선택한다. 후보가 한 개이면 그 후보를 선택하고, 유효 후보가 없으면 기존처럼 `NOT_FOUND`다. 원본 응답의 첫 번째 후보가 검증에서 탈락했다면 다음 유효 후보를 선택한다.

이 선택 규칙은 **ISRC HTTP 404 이후의 metadata fallback**에만 적용한다. ISRC 조회에서 복수 후보가 반환된 경우의 선택은 ADR-012를 유지한다. ISRC 부재 또는 404가 아닌 후보 미일치로 시작한 metadata 검색의 유일 후보 규칙도 유지한다. 제목·Artist·재생 시간 검증 기준과 Release Group 연결 정책은 변경하지 않는다.

## Reason

사용자 진단 표본 `Billboard Style`의 ISRC `ZZA0Z2601215` 조회는 HTTP 404였고, 독립 metadata 검색에서 제목·Artist·재생 시간이 모두 일치하는 후보 두 개를 반환했다. 기존 유일 후보 규칙을 그대로 적용하면 ADR-026의 fallback을 추가해도 이 Track의 Recording을 선택하지 못한다. 사용자는 이 경우 첫 번째 후보를 사용해 태그 손실을 줄이기로 결정했다.

검색 응답 순서는 외부 제공자가 정한다. 이 정책은 후보가 동일한 녹음임을 증명하지 않으며, 검증을 통과한 후보 가운데 첫 번째를 택하는 명시적인 선택 기준이다.

## Consequence

- ISRC 404 뒤 유효 metadata 후보가 여러 개인 Track도 첫 유효 후보의 Recording ID와 genre를 사용한다. 선택 후보 ID, 유효 후보 수, `FIRST_VALID_AFTER_ISRC_404` 사유를 진단에 기록한다.
- 같은 입력에 대해 검색 응답 순서가 달라지면 선택 Recording이 달라질 수 있다. 구현 테스트에는 후보 순서와 검증 탈락 후보가 앞에 있는 경우를 포함한다.
- 기존 `EMPTY_COMPLETED` subject의 일괄 재처리는 이번 변경 범위에 포함하지 않는다. 해당 subject는 기존 완료 표식에 따라 조회가 생략된다.
- 2026-11-06 사용자 요청으로 active `ENRICHMENT-DIAGNOSTICS-001`의 고도화 범위에 포함됐다. 구현과 사용자 실행 검증은 아직 남아 있다.
