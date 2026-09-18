# ADR-009 — Catalog Optimistic Create Conflict Recovery

## Decision

Catalog Track import는 최초 Catalog read fast path 뒤에 Track 존재 여부를 다시 조회하지 않고, optimistic create를 시도한다.

- `CatalogWriteService.upsert()`는 `create()`로 변경하고, 성공한 신규 Catalog의 `ImportedTrack` snapshot을 직접 반환한다.
- `CatalogUpsertResult`의 `created/existing` marker는 제거한다.
- `TrackImportService`는 최초 read에서 existing Track을 반환해 Spotify HTTP 호출을 생략한다.
- 최초 read 이후 write 중 발생한 Catalog Spotify ID unique 충돌은 database constraint를 최종 방어선으로 사용한다.
- `uk_track_spotify_id` 충돌 뒤에는 canonical Track을 DB에서 읽어 반환한다.
- `uk_artist_spotify_id` 또는 `uk_album_spotify_id` 충돌 뒤에 Track이 아직 없으면 create를 한 번만 재시도한다.
- 알려진 세 Catalog Spotify ID unique constraint 이외의 DB 무결성 오류는 conflict recovery나 재시도 없이 전파한다.
- 두 번째 create도 retryable Catalog duplicate로 실패하면 예외를 전파한다. 임의의 다회 retry, lock, reservation은 도입하지 않는다.

## Reason

현재 write service의 Track existence check는 최초 read와 Spotify metadata HTTP 사이에 다른 요청이 생성한 Track을 발견할 수 있으나, 발견한 Entity를 반환하지 않는다. 호출자는 빈 `existing` marker를 받은 뒤 canonical response를 만들기 위해 같은 Track과 credit을 다시 읽는다.

동시 최초 import의 정합성은 pre-write read가 아니라 Artist, Album, Track의 Spotify ID unique constraints가 보장한다. System Tag architecture의 Concurrent First Import 정책도 duplicate key 뒤 canonical row 재조회를 충분한 해법으로 정한다.

정상 최초 import에서 indexed Track read 한 번을 제거하고 write service의 실제 동작을 create라는 이름으로 드러낸다. 단, 모든 `DataIntegrityViolationException`을 경쟁으로 취급하면 FK나 NOT NULL 등 programmer/data error를 재시도할 수 있으므로 constraint 이름이 알려진 Catalog duplicate만 복구한다.

## Consequence

- 정상 최초 import는 Catalog fast-path read, Spotify HTTP, Artist/Album 조회 및 create write transaction으로 진행하며 write service 내부 Track existence SELECT가 없다.
- 기존 Track은 최초 fast path에서만 read하며 Spotify 호출과 Catalog write가 없다.
- 같은 Track의 동시 import 및 서로 다른 Track의 shared Artist/Album 최초 import는 unique conflict를 통해 수렴한다.
- 충돌 요청은 rollback과 canonical read 또는 최대 한 번의 create 재시도를 겪을 수 있다.
- public API/Swagger contract, Catalog schema constraints, external HTTP transaction boundary, Artist credit 및 Album credit canonical response 정책은 변경하지 않는다.
- ADR-008의 신규 Catalog snapshot 반환 결정은 유지한다. 구현 메서드명과 result wrapper만 이 ADR의 create semantics로 대체한다.
