# ADR-008 — Enrichment Sequence JDBC Batching

## Decision

External enrichment의 다건 쓰기 테이블에 JDBC batching을 적용할 수 있도록 PK 생성 전략을 `IDENTITY`에서 database sequence로 변경한다.

- 대상은 `external_tag_observation`, `tag_assertion`, `subject_tag_resolved`로 한정한다.
- 테이블별 독립 sequence와 `allocationSize=50`을 사용한다.
- JPA `allocationSize=50`과 Oracle/H2 DDL sequence `INCREMENT BY 50`을 반드시 일치시킨다.
- 기존 운영 row가 있는 테이블은 identity 속성과 `MAX(id)`를 사전 조사하고, 첫 Hibernate 생성 ID가 기존 최대 ID보다 큰 안전한 sequence block에서 시작하도록 migration한다.
- `hibernate.jdbc.batch_size=50`을 활성화한다.
- 현재 flush 경계에서는 entity type별 쓰기가 이미 분리되므로 `hibernate.order_inserts`/`hibernate.order_updates`는 측정 근거 없이 활성화하지 않는다.
- 세 대상 Entity에 `@Version`이 없고 Hibernate 5.0 이후 `hibernate.jdbc.batch_versioned_data`의 기본값이 `true`이므로 이 설정을 명시적으로 추가하지 않는다.
- provider source별로 쓰기를 나누지 않고 기존 Album/Track subject별 입력 병합을 유지한다.
- Album/Track Observation을 하나의 transaction으로 통합하는 변경은 unique 충돌 재시도와 rollback 범위에 비해 현재 규모의 추가 이득이 작으므로 보류한다.
- 정상적인 신규 Catalog 생성 경로에서 `CatalogWriteService.upsert()`가 생성 snapshot을 반환하여 직후 Track/TrackArtist/AlbumArtist 재조회를 제거한다. 기존 row 및 동시 충돌 경로는 기존 read service를 유지한다.

## Reason

현재 `saveAll()`은 입력을 배열로 전달하지만 `GenerationType.IDENTITY`가 INSERT 직후의 PK 획득을 강제하여 Hibernate JDBC insert batching을 사용할 수 없다. 실제 신규 Track enrichment에서 31건의 Observation이 31개의 개별 INSERT로 실행되었다.

Sequence로 INSERT 전에 ID block을 확보하면 기존 JPA Entity lifecycle, unique/FK, Observation/Assertion 규칙을 유지하면서 동일 SQL을 JDBC batch로 전달할 수 있다. source별 분할은 동일 SQL batch를 더 잘게 나누므로 적용하지 않는다.

Album/Track multi-subject transaction은 추가 SELECT/flush를 줄일 수 있지만 한 subject의 unique 충돌이 다른 subject를 rollback시키는 새로운 운영 의미를 만든다. 먼저 batching의 실측 효과를 확인하고 필요할 때만 별도 결정한다.

## Consequence

- 운영 Oracle은 `ddl-auto=validate`이므로 애플리케이션 배포 전에 identity/sequence migration이 필수다.
- Sequence `INCREMENT BY`와 JPA `allocationSize`가 다르거나 sequence 시작 block이 기존 ID와 겹치면 배포를 중단한다.
- ID는 연속적이지 않을 수 있으며, ID gap을 비즈니스 의미로 해석하지 않는다.
- Hibernate SQL 로그 줄 수는 batch 성공을 대표하지 않으므로 JDBC `addBatch`/`executeBatch` 관측과 요청 시간으로 검증한다.
- Catalog 신규 생성은 저장된 ID와 metadata로 반환 snapshot을 만들어 재조회를 피하지만, 기존 Album credit을 재사용하는 경우 DB 기준 응답을 유지해야 한다.
- API/Swagger contract, provider 수집 정책, Observation/Assertion/Resolver 의미는 변경하지 않는다.
