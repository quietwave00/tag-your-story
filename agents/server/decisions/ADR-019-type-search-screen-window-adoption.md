# ADR-019 — Type Search Uses Screen Window

## Decision

- 기존 `GET /api/catalog/search?keyword=&page=&type=TRACK|ALBUM|ARTIST`의 URL과 응답 필드는 유지하고, 내부 검색은 ADR-018의 3-page window를 사용한다.
- 타입별 결과는 첫 Spotify 통합 검색 window의 3개 page에서 해당 타입 후보를 모아 동일 Spotify ID 중복을 제거하고, 전체 window에 기존 `CatalogSearchRanker`를 적용한 뒤 요청 page의 최대 10개만 반환한다. page는 0~100의 0-based다.
- `totalCount`는 같은 keyword의 첫 window에서 얻은 타입별 Spotify total을 짧은 cache 수명 동안 고정한 참고 값이다. 기존 클라이언트가 이를 페이지 버튼 계산에 사용하므로 값은 window 간 재조회할 때 흔들릴 수 있다.
- 세 타입의 동시 HTTP 요청이 같은 서버 프로세스에 도착하면 진행 중인 window fetch를 합쳐 Spotify 통합 검색 최대 3회로 처리한다. Redis 5분 cache는 후속 page에 같은 정렬 순서를 제공한다.
- `type`을 생략한 통합 검색과 `GET /api/tracks`는 변경하지 않는다. 화면 전용 `/api/catalog/search/screen`도 계속 제공한다.

## Reason

현재 클라이언트는 화면 전용 API가 아니라 기존 타입별 endpoint를 세 번 호출한다. 따라서 ADR-018의 새 API만으로는 실제 화면의 정확 일치 결과가 다른 페이지에서 첫 페이지로 이동하지 않는다. 사용자에게서 `in love` 검색의 11번째 결과가 정확 일치한다는 재현 사례를 받았으며, 클라이언트 소스는 수정하지 말라는 명시적 지시가 있다.

## Consequence

- ADR-017의 타입별 Spotify 단일 타입·단일 HTTP 요청 정책은 이 타입별 endpoint에서 종료된다. 요청·응답 필드는 유지하지만 정렬 범위와 Spotify 조회 방식이 바뀐다.
- 첫 페이지의 cache miss는 Spotify 검색 최대 3회이고 같은 서버 프로세스의 동시 요청은 하나의 작업을 공유한다. 이미 뒤쪽 window의 page로 바로 접근하면 기준 total용 첫 window와 해당 window를 함께 조회할 수 있다. 서로 다른 서버 인스턴스가 동시에 cache miss를 만나면 중복 조회가 가능하다.
- 기존 클라이언트는 새 정확도 정렬을 즉시 사용할 수 있으나, `totalCount` 기반 페이지 번호 UI의 신뢰성 문제는 완전히 해결되지 않는다. 별도 클라이언트 작업에서 화면 전용 API의 `hasNext`를 사용해야 한다.
- Spotify 전체 결과의 전역 순위를 보장하지 않으며, 최대 30개 후보 window를 넘어선 정확 일치는 해당 window의 첫 page로만 이동할 수 있다.
