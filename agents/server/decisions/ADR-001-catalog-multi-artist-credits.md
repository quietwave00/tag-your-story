# ADR-001 — Catalog Multi-Artist Credits

## Decision

Album과 Track의 Artist 관계는 단일 `artist_id` 컬럼 대신 `album_artist`, `track_artist` 연결 테이블을 유일한 source of truth로 사용한다.

- 각 연결 row는 Spotify Artist 배열의 0-based `position`을 보존한다.
- `position=0`을 표시상 대표 Artist로 해석한다.
- Album, Track 또는 Artist에는 중복된 Artist 관계 컬렉션을 두지 않는다.
- 연결 Entity는 parent와 Artist를 각각 `ManyToOne(LAZY)`로 참조한다.
- `(parent_id, artist_id)`와 `(parent_id, position)`에 각각 unique 제약을 둔다.

## Reason

Spotify Track과 Album은 복수 Artist credit을 반환한다. 단일 `artist_id`는 featuring, collaboration, compilation의 전체 credit과 표시 순서를 보존할 수 없다.

JPA `ManyToMany` 대신 명시적 연결 Entity를 사용하면 관계 자체의 속성인 `position`을 모델링할 수 있고, FK와 unique 제약으로 중복 Artist 및 순서 충돌을 DB에서 최종 방어할 수 있다. 단방향 LAZY 관계와 명시적 query를 사용하면 불필요한 Aggregate 로딩 및 양방향 관계의 복잡성도 피할 수 있다.

## Consequence

- `album.artist_id`와 `track.artist_id`는 사용하지 않는다.
- 대표 Artist는 별도 FK나 boolean으로 중복 저장하지 않고 `position=0`으로 계산한다.
- Artist credit 조회에는 연결 테이블과 Artist를 함께 적재하는 명시적 query가 필요하다.
- Catalog import는 Artist, Album, Track 및 연결 row를 같은 짧은 transaction에서 저장한다.
- 기존 단일 Artist 관계를 전제로 한 Server Spec과 System Tag Catalog ERD를 연결 테이블 구조로 변경한다.
- 향후 Artist 역할이 필요하면 연결 Entity에 role을 추가할 수 있으나 Spotify가 제공하지 않는 역할은 추측하지 않는다.
