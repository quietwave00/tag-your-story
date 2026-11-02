# ADR-025 — Artist Album 원본 페이지의 짧은 응답 처리

## Decision

- Artist Album 인덱스는 Spotify `total`을 offset 탐색의 상한으로 사용한다. 각 요청의 offset은 `page * 10`이며, 한 응답에 10건보다 적은 항목이 있어도 다음 offset을 계속 조회한다.
- 인덱스의 `ALL` 건수는 실제로 받은 항목 수다. 유형별 건수도 실제로 받은 항목 중 분류가 확정된 수다. 원본 페이지 API의 Spotify `totalCount` 계약은 유지한다.
- 원본 항목의 순서와 중복을 보존한다. 인접한 두 페이지의 항목이 같다는 이유만으로 실패 처리하지 않는다. 전체 offset 범위는 유한하게 순회한다.
- `total` 변경, 응답 누락, 요청 창의 최대 크기 초과, 양수 `total`인데 전체 순회 결과가 비어 있는 경우에는 불완전한 스냅샷을 게시하지 않는다.

## Reason

2026-11-02 실사용 Artist 목록에서 Spotify `total=60`, `offset=10`, `limit=10` 요청이 5건을 반환했다. 기존 인덱스는 모든 비마지막 페이지가 10건이라고 가정해 `SPOTIFY_EXCEPTION`으로 실패했다. 수집 건수로 다음 offset을 계산하면 짧은 페이지 뒤의 원본 위치를 건너뛰거나 중복 조회할 수 있으므로 요청 offset을 기준으로 탐색한다.

## Consequence

- 첫 구축은 `ceil(Spotify total / 10)`회의 Artist Album 목록 요청을 수행한다. 짧거나 빈 창이 있어도 뒤의 offset을 조회한다.
- 인덱스의 `ALL.totalCount`는 Spotify 원본 `total`보다 작을 수 있다. 분류 상태 필드는 분류 가능 여부를 나타내며, 원본 창에서 반환되지 않은 항목까지 포함하지 않는다.
- ADR-024의 단일 Redis 스냅샷, TTL, 동시 빌드 조율 및 분류 정책은 유지한다.
