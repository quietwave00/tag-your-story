# ADR-017 — Type-selective Spotify Catalog Search

## Decision

- 기존 `GET /api/catalog/search?keyword={keyword}&page={page}`의 타입 미지정 통합 검색은 ADR-005에 따라 유지한다.
- 같은 endpoint에 선택적 `type=TRACK|ALBUM|ARTIST` query parameter를 추가한다.
- 타입이 지정되면 Spotify Search API에도 해당 type 하나만 전달하고, 응답 `items`와 `totalCount`는 해당 타입만 나타낸다.
- 타입이 생략되면 Spotify `track,album,artist`를 한 요청으로 조회하고 세 타입 후보 통합, total 합산 및 기존 결정적 ranking을 유지한다.
- 타입 지정과 미지정 모두 서버 요청 한 번당 Spotify Search HTTP 요청을 한 번만 수행한다.
- 성공 response DTO는 유지하며, 허용되지 않은 type은 HTTP 400으로 처리한다.
- 기존 `GET /api/tracks` Track 전용 호환 API는 유지한다.

## Reason

통합 검색은 세 종류의 후보를 한 화면에 제시하는 데 유용하지만, 타입별 탭에서는 다른 타입의 후보와 순위가 섞이고 하나의 page가 세 타입에 동시에 적용된다. 클라이언트가 Artist, Album, Track 결과와 total 및 page를 독립적으로 조회할 수 있어야 각 탭의 검색 의도를 정확히 표현할 수 있다. 기존 공개 계약을 제거하거나 새 endpoint 세 개를 중복 구현하지 않고 선택적 query parameter로 확장하면 하위 호환과 단일 검색 경계를 함께 유지할 수 있다.

## Consequence

- 클라이언트는 필요한 탭만 지연 호출하거나 세 타입을 병렬로 독립 호출할 수 있다.
- 타입 지정 시 page size는 10이고 `totalCount`는 선택 타입 total이다. 타입 미지정 시 기존 최대 30개와 세 total의 합을 유지한다.
- Application과 provider port는 통합/타입별 검색 범위를 명시적으로 전달해야 한다.
- Spotify 미요청 결과 group이 null일 수 있으므로 adapter는 해당 group에 의존하지 않아야 한다.
- 검색어 기록, 오류 변환, Catalog 비저장, enrichment 미실행 정책은 변하지 않는다.
- Artist 상세, Artist 저장 및 타입별 프론트 화면은 별도 마일스톤이다.
