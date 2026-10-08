# ADR-005 — Spotify Multi-Subject Catalog Search

## Decision

사용자 음악 검색의 새 진입점은 Spotify의 단일 Search 요청으로 Track, Album, Artist 후보를 함께 조회한다.

- 새 공개 API는 `GET /api/catalog/search?keyword={keyword}&page={page}`다.
- 기존 `GET /api/tracks`는 하위 호환을 위해 유지한다.
- Spotify 응답은 provider-neutral `CatalogSearchItem`으로 변환하며 각 후보에 `TRACK`, `ALBUM`, `ARTIST` subject type과 Spotify ID를 명시한다.
- 각 subject type에서 최대 10개씩 받은 현재 page의 후보를 서버에서 합치고, 이름 exact match, prefix match, 이름 유사도, artist 유사도, Spotify 원본 순서 순으로 정렬한다.
- 완전히 같은 순위는 `TRACK`, `ALBUM`, `ARTIST` 순으로 안정적으로 정렬한다.
- 검색은 후보만 반환하며 Catalog 저장이나 외부 enrichment를 실행하지 않는다.
- Spotify는 Discovery / Identity Seed로만 사용하고 System Tag evidence source에는 추가하지 않는다.
- Album의 태그를 Track에 상속하는 기존 정책은 유지한다. 컴필레이션을 포함한 Album type별 제한은 두지 않는다.

## Reason

사용자의 검색 의도는 Track에 한정되지 않으므로 Track 전용 검색 결과만으로는 Album과 Artist 선택을 정확하게 표현할 수 없다. 검색 결과에 subject type과 외부 식별자를 함께 노출하면 후속 선택 유스케이스가 Track과 Album을 구분해 올바른 태그 계산 경로를 실행할 수 있다.

Spotify 원본 점수는 서로 다른 subject type 사이에서 직접 비교할 수 없으므로, 동일한 keyword에 대해 설명 가능하고 결정적인 로컬 정렬 규칙이 필요하다. 동시에 기존 Track 검색 API를 유지해 현재 client의 회귀를 방지한다.

## Consequence

- 하나의 Spotify HTTP 요청이 세 subject type의 결과를 반환한다.
- `limit=10`, `offset=page*10`은 subject type별로 적용되므로 한 page는 최대 30개 후보를 반환한다.
- 서버 정렬은 현재 page에 포함된 후보 안에서만 수행한다. Spotify 전체 결과 집합을 대상으로 한 전역 순위는 아니다.
- `totalCount`는 Track, Album, Artist의 Spotify total 합계다.
- 후속 Track/Album 선택 API는 검색 결과의 subject type과 Spotify ID를 command 입력으로 사용해야 한다.
- Artist 선택은 태그 계산을 실행하지 않는 정책으로 후속 마일스톤에서 구현한다.

