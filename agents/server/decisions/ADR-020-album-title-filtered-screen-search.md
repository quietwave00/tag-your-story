# ADR-020 — Album Title Filter in Screen Search

## Decision

- 기존 타입별 Catalog 검색과 화면용 통합 검색이 공유하는 3-page window에서 Album 후보는 Spotify Search `q=album:{keyword}`, `type=album`으로 조회한다.
- Track·Artist 후보는 같은 keyword로 `type=track,artist`를 조회한다. 타입 생략 `GET /api/catalog/search`의 레거시 통합 동작과 `/api/tracks`는 변경하지 않는다.
- 각 page offset에 두 Spotify 요청을 수행하므로 3-page window cache miss는 최대 6번의 Spotify 검색 요청을 한다. 기존 Redis window cache key의 버전을 올려 이전 일반 검색 Album 결과를 재사용하지 않는다.
- Album 후보 정렬, ID 중복 제거, page size 10, totalCount 참고 값 및 hasNext 계산은 기존 window 정책을 유지한다. Album total은 제목 필터 검색의 Spotify total이다.

## Reason

사용자 확인에서 `dreampop` Album 정확 제목 후보가 다음 3-page window에 남아 있었다. 30개 window 내부 정렬만으로는 window 경계를 넘어 후보를 이동시킬 수 없다. Spotify의 `album` 검색 필터로 Album 후보를 제목 중심으로 좁히면 일반 키워드 검색보다 제목 일치 결과를 앞쪽 window에서 찾을 가능성을 높인다.

## Consequence

- ADR-018/019의 Spotify 통합 3회 조회 정책은 이 화면 검색 window에서 2개 질의의 3-page 조회로 변경된다. 클라이언트 API의 URL 및 응답 필드는 유지한다.
- Album 영역에서는 아티스트 이름 등 Album 제목 밖의 정보만으로 찾던 결과가 줄어들 수 있다. 필터는 Spotify의 검색 조건이며 모든 정확 제목 결과가 첫 page에 온다는 보장은 없다.
- Spotify 호출 수와 첫 cache miss 지연 시간이 증가한다. 동일 keyword/window의 후속 요청은 짧은 Redis cache와 같은 프로세스의 진행 중 요청 공유를 유지한다.
