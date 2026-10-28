# ADR-018 — Screen Catalog Search Window

## Decision

- 기존 `GET /api/catalog/search` 통합·타입별 검색과 `GET /api/tracks` 계약은 유지한다.
- 검색 결과 화면 전용 `GET /api/catalog/search/screen`은 하나의 응답에 Track, Album, Artist의 독립 페이지를 담는다.
- 같은 keyword에 대해 Spotify Search `type=track,album,artist`, `limit=10`으로 연속 3개 offset을 조회해 각 타입 최대 30개 후보를 확보한다. 기존 `CatalogSearchRanker`를 타입별 30개 window 전체에 적용한 후 타입별 요청 page를 10개씩 잘라 반환한다.
- 서버는 keyword와 3-page window 번호로 짧게 캐시해 같은 window의 화면 1~3페이지가 동일한 정렬 결과를 사용한다. 클라이언트에는 타입별 최대 10개만 보낸다.
- `totalCount`는 Spotify가 제공한 참고 값으로 유지한다. 화면의 다음 페이지 판단에는 `totalCount`를 사용하지 않고 window 내 실제 후보 수와 허용 offset 범위를 사용한다. 정확히 30개가 찬 window 끝에서는 다음 window 조회가 한 번 필요할 수 있다.
- 화면 page는 타입별로 독립적이고 0-based이며 0~100으로 제한한다. 마지막 window에서 Spotify offset 1000을 넘는 호출은 하지 않는다.

## Reason

현재 검색은 현재 Spotify page의 최대 10개만 정렬하므로 정확히 일치하는 결과가 Spotify 3페이지에 있으면 화면 첫 페이지로 이동시킬 수 없다. Spotify Search는 하나의 요청에서 세 타입 각각 최대 10개를 반환하므로 3개의 통합 요청으로 각 타입 30개를 얻을 수 있다. 이는 현재 화면의 타입별 3개 Spotify 요청과 같은 호출 수다.

## Consequence

- ADR-005의 현재 페이지 후보 정렬과 ADR-017의 요청당 단일 Spotify 호출은 새 화면 전용 endpoint에만 적용하지 않는다. 기존 endpoint의 동작은 그대로 유지한다.
- 화면 첫 요청은 최대 3개 Spotify HTTP 요청을 수행한다. 같은 window의 다음 페이지는 짧은 캐시 수명 안에서 재사용한다. Redis cache가 필요하지만 DB transaction과 Catalog 저장은 없다.
- 최대 30개 후보 밖의 정확 일치 결과를 첫 window로 올릴 수 없으며, Spotify 검색 결과가 cache 만료 후 달라지면 재조회한 window의 순서도 달라질 수 있다.
- 클라이언트는 별도 작업에서 화면 전용 API와 타입별 `hasNext`를 사용해야 한다. 이전 3개 타입별 요청은 새 endpoint가 실제로 연결되기 전까지 유지된다.
