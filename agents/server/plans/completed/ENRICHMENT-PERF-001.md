# ENRICHMENT-PERF-001 — First-load Persistence Round-trip Reduction

## Status

- State: Implementation complete / user verification pending
- Approved: 2026-09-15
- Scope: External enrichment JDBC insert batching and safe Catalog creation reread removal
- Related decision: `ADR-008-enrichment-sequence-jdbc-batching.md`

## Goal

신규 Track 최초 enrichment의 데이터 정합성, 동시성, 멱등성과 API contract를 유지하면서 DB network round-trip을 줄인다.

관측된 baseline은 단일 Swagger Track import/execute 요청에 SQL 약 67개, ExternalTagObservation 개별 INSERT 31개이다. 현재 `ObservationWriteService` 는 `saveAll()`을 사용하지만 Entity PK가 `IDENTITY`이므로 Hibernate JDBC insert batching이 비활성화된다.

## Source of Truth

- `agents/server/server_spec.md`
- `agents/server/server_tag_feature_architecture.md`
- `agents/server/current_state.md`
- `agents/server/conventions.md`
- `agents/server/progress.md`
- `agents/server/plans/active/ENRICHMENT-001.md`
- `agents/server/decisions/ADR-008-enrichment-sequence-jdbc-batching.md`
- Hibernate ORM 6 batching/identifier generation official documentation

## Scope

### 1. Sequence-based identifiers

다음 Entity의 PK 생성을 `GenerationType.IDENTITY`에서 독립 database sequence로 변경한다.

| Entity | PK | Sequence |
|---|---|---|
| `ExternalTagObservationEntity` | `observation_id` | `external_tag_observation_seq` |
| `TagAssertionEntity` | `assertion_id` | `tag_assertion_seq` |
| `SubjectTagResolvedEntity` | `resolved_id` | `subject_tag_resolved_seq` |

공통 정책:

- `@SequenceGenerator(allocationSize = 50)`
- DB `CREATE SEQUENCE ... INCREMENT BY 50`
- Entity generator name과 physical sequence name을 명시적으로 분리
- 세 Entity 이외의 Catalog/Board/User 등 `IDENTITY` 전략은 변경하지 않음

### 2. Oracle/H2 schema and migration

운영 Oracle은 `ddl-auto=validate`이므로 배포 전 migration을 별도로 수행한다.

Preflight:

- 세 테이블의 `MAX(id)` 조회
- `USER_TAB_IDENTITY_COLS`로 identity `GENERATION_TYPE` 확인
- 기존 column이 explicit ID INSERT를 허용하는지 확인
- 운영 Oracle version에서 identity 속성 변경 DDL 검증

Migration:

- 필요 시 identity 속성을 제거하고 기존 PK/FK/unique/index는 보존
- 기존 `MAX(id)`를 넘는 안전한 첫 ID block 산정
- 세 sequence에 `INCREMENT BY 50`, 적절한 `START WITH`, `CACHE 50`, `NOCYCLE` 명시
- `USER_SEQUENCES.INCREMENT_BY = 50` 검증
- migration 후 첫 생성 ID가 기존 `MAX(id)`보다 큰지 검증
- application rollback이 sequence migration과 어떻게 조합되는지 runbook에 명시

Local H2의 `db/init_schema.sql`도 동일 sequence 이름과 increment를 사용하고 auto-increment 의존을 제거한다.

### 3. Hibernate JDBC batching

`application-jpa.yml`에 다음을 추가한다.

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
```

- `hibernate.order_inserts`/`hibernate.order_updates`는 기본 범위에서 제외한다.
- test instrumentation으로 batch 분할이 실제 발생하고 정렬 이득이 확인될 때만 Plan을 갱신한 뒤 적용한다.
- 대상 Entity 세 개에 `@Version`이 없으므로 `hibernate.jdbc.batch_versioned_data`를 명시적으로 추가하지 않는다.
- 향후 `@Version` 추가 시 Hibernate 기본값과 Oracle JDBC batch row count 정확성을 재검증한다.

### 4. Existing save grouping preservation

provider 결과는 기존과 같이 `CollectedExternalTags`에서 병합하고 Album/Track subject별로 `saveAll()`한다.

```text
MusicBrainz + Discogs + Last.fm
              ↓
      CollectedExternalTags
       ├─ Album inputs batch
       └─ Track inputs batch
```

source별 `saveAll()` 분할은 동일 SQL batch를 더 잘게 나누므로 금지한다.

### 5. Catalog creation snapshot

`CatalogWriteService.upsert()`는 새 Track을 생성한 정상 경로에서 저장된 Entity ID와 Catalog metadata로 만든 결과를 반환한다.

예상 모델:

```java
public record CatalogUpsertResult(
        boolean created,
        ImportedTrack importedTrack
) {
}
```

동작:

```text
신규 Track 생성
→ created=true + ImportedTrack snapshot
→ TrackImportService의 생성 직후 getBySpotifyId() 생략

기존 Track 또는 동시 생성 경합
→ created=false
→ CatalogTrackReadService로 DB canonical state 조회
```

기존 Album을 재사용하는 신규 Track의 경우 Spotify 응답만으로 Album artist credit을 재구성해 기존 DB와 다른 응답을 만들지 않는다. 기존 DB credit 조회가 필요하면 해당 부분만 최소 조회한다.

Production 직접 호출부는 `TrackImportService` 두 곳뿐이다. 변경 전 다음 호출부/테스트를 함께 갱신한다.

- `TrackImportService`
- `TrackImportServiceTest`
- `CatalogJpaRepositoryTest`
- `CatalogImportConcurrencyTest`
- `FirstVerticalSliceIntegrationTest` bean/test wiring

## Out of Scope

- Album/Track Observation을 하나의 transaction으로 통합
- provider source별 persistence transaction 분할
- native Oracle `INSERT ALL` 또는 JPA를 우회하는 raw JDBC domain write
- Catalog 이외 Entity의 PK 생성 전략 변경
- Resolver 수식, confidence, inheritance, Observation/Assertion identity 변경
- API/Swagger request/response contract 변경
- 전체 Catalog/Resolver query architecture 재설계

Album/Track transaction 통합은 Observation/alias 조회와 flush를 추가로 줄일 수 있지만 unique 충돌 재시도, partial failure, rollback 범위를 넓힌다. 먼저 이 Plan의 batch 결과를 측정하고 실제 병목으로 남을 때만 별도 마일스톤으로 검토한다.

## Transaction and Concurrency

- 외부 HTTP 호출 중 DB transaction을 열지 않는 기존 경계를 유지한다.
- Observation은 Album/Track별 기존 transaction을 유지한다.
- DB composite unique를 최종 방어선으로 유지한다.
- `ObservationProcessingService`와 `TagResolutionService`의 제한적 1회 재시도를 유지한다.
- Sequence allocation으로 ID gap이 발생해도 멱등성/순서 판단에 ID 연속성을 사용하지 않는다.
- Catalog 생성 결과를 반환하더라도 unique conflict 후에는 경쟁 transaction이 commit한 DB row를 재조회한다.

## Implementation Steps

1. Batch/round-trip 특성화 테스트를 추가해 baseline을 고정한다.
2. 세 Entity에 sequence generator를 적용한다.
3. H2 schema와 Oracle migration/runbook을 갱신한다.
4. `hibernate.jdbc.batch_size=50`을 적용한다.
5. JDBC proxy/spy 또는 동등한 테스트 instrumentation으로 `addBatch`/`executeBatch`를 검증한다.
6. 51건 이상 insert, existing ID, FK/unique, rollback/retry, concurrency를 검증한다.
7. Catalog 생성 반환 모델과 특성화 테스트를 추가한다.
8. 신규 Track 생성에서 in-memory snapshot을 반환하고 정상 경로의 3개 재조회를 제거한다.
9. 기존 Track/Album 및 동시 충돌 경로의 DB canonical read를 유지한다.
10. 대상 테스트, 전체 검증, diff review 결과를 사용자로부터 확인한다.

## Expected Files

Production:

- `domain/enrichment/observation/ExternalTagObservationEntity.java`
- `domain/enrichment/assertion/TagAssertionEntity.java`
- `domain/resolution/SubjectTagResolvedEntity.java`
- `application/catalog/importer/CatalogWriteService.java`
- `application/catalog/importer/TrackImportService.java`
- `application/catalog/importer/model/CatalogUpsertResult.java` (name finalized during implementation)
- `resources/application-jpa.yml`
- `resources/db/init_schema.sql`

Tests:

- `infrastructure/persistence/enrichment/EnrichmentJpaRepositoryTest.java`
- `infrastructure/persistence/enrichment/ObservationProcessingConcurrencyTest.java`
- `infrastructure/persistence/resolution/SubjectTagResolvedJpaRepositoryTest.java`
- batch observation test configuration/instrumentation
- `infrastructure/persistence/catalog/CatalogJpaRepositoryTest.java`
- `infrastructure/persistence/catalog/CatalogImportConcurrencyTest.java`
- `application/catalog/importer/TrackImportServiceTest.java`
- `application/catalog/selection/FirstVerticalSliceIntegrationTest.java`

Documents:

- `agents/server/decisions/ADR-008-enrichment-sequence-jdbc-batching.md`
- `agents/server/migrations/ENRICHMENT-PERF-001.md`
- `agents/server/plans/active/ENRICHMENT-PERF-001.md`
- implementation 시 `server_spec.md`, `server_tag_feature_architecture.md`
- 완료 시 `progress.md`

## Test Plan

Sequence/schema:

- generator/physical sequence 이름과 increment 50 일치
- 세 Entity별 51건 이상 저장 후 ID unique
- 기존 row `MAX(id)`보다 큰 첫 ID 생성
- FK/unique/index 제약 보존
- Oracle preflight/migration/post-check DDL review

Batching:

- `ExternalTagObservationEntity` 다건 저장이 개별 `executeUpdate()`가 아니라 JDBC `addBatch()`/`executeBatch()`를 사용
- subject당 50건 이하 Observation이 하나의 batch execution으로 수렴
- 50건 초과 시 설정 크기로 batch 분할
- Assertion/Resolved insert에도 동일 원칙 적용
- SQL 로그 줄 수가 아닌 JDBC batch execution 횟수와 elapsed time을 판정 기준으로 사용

Catalog:

- 신규 Artist/Album/Track 생성 결과가 재조회 없이 정확한 `ImportedTrack`을 반환
- 기존 Album을 사용하는 신규 Track의 Album artist credit이 DB canonical state와 일치
- 기존 Track fast path 유지
- 첫 unique conflict 후 경쟁 요청 결과 재사용
- conflict 후 DB row가 없으면 한 번만 재시도
- 반복/동시 import에서 Catalog row 수 수렴
- 기존 response 필드/정렬 동일

Regression:

- Observation/Assertion/Resolution/Inheritance integration tests
- provider partial-success tests
- Track selection vertical slice
- Catalog JPA/concurrency tests
- Swagger/API contract tests

## Acceptance Criteria

- [ ] `allocationSize=50`과 Oracle/H2 `INCREMENT BY 50`이 세 sequence에서 일치한다.
- [ ] 기존 운영 ID보다 큰 안전한 sequence block에서 생성을 시작한다.
- [ ] 운영 identity column의 explicit ID INSERT 가능 여부를 migration 전에 확인한다.
- [ ] Observation/Assertion/Resolved 다건 INSERT가 JDBC batch로 실행된다.
- [ ] 31건 Observation이 유실 없이 저장되고 subject/source/externalRef lineage가 유지된다.
- [ ] unique/FK/멱등성/동시성/제한적 재시도가 유지된다.
- [ ] 현재 `@Version`이 없는 Entity에 불필요한 `batch_versioned_data`를 추가하지 않는다.
- [ ] `order_inserts`/`order_updates`를 측정 근거 없이 추가하지 않는다.
- [ ] 신규 Catalog 정상 생성 경로의 Track/TrackArtist/AlbumArtist 직후 재조회 3개가 제거된다.
- [ ] 기존 Track/Album 및 동시 충돌 경로는 DB canonical state를 반환한다.
- [ ] Album/Track Observation transaction 통합은 구현하지 않는다.
- [ ] API/Swagger contract가 변경되지 않는다.
- [ ] 사용자가 대상 테스트, `test`, `check`, `scripts/verify.sh`를 실행하고 결과를 전달한다.
- [ ] diff review에서 명세 퇴행, N+1, transaction, schema, 멱등성, 동시성 문제가 없다.

## User Verification Commands

PowerShell target tests와 전체 검증 명령은 구현 중 확정한 test class 기준으로 안내한다. 기본 전체 검증은 다음과 같다.

```powershell
.\gradlew.bat :tagnote-core:test --tests '*EnrichmentJdbcBatchTest' --tests '*EnrichmentJpaRepositoryTest' --tests '*ObservationProcessingConcurrencyTest' --tests '*SubjectTagResolvedJpaRepositoryTest' --tests '*TagInheritanceConcurrencyTest' --tests '*TagResolutionConcurrencyTest' --tests '*TrackImportServiceTest' --tests '*CatalogJpaRepositoryTest' --tests '*CatalogImportConcurrencyTest' --tests '*FirstVerticalSliceIntegrationTest'
.\gradlew.bat test
.\gradlew.bat check
```

`scripts/verify.sh`는 WSL/Git Bash에서 사용자가 실행한다.

## Completion

구현, Acceptance Criteria, 사용자 테스 결과, migration review, diff review가 모두 통과한 뒤에만:

1. `progress.md`에 완료 내용과 검증 결과를 기록한다.
2. 이 Plan을 `plans/completed/ENRICHMENT-PERF-001.md`로 이동한다.

## Implementation Record — 2026-09-15

- 세 대상 Entity를 독립 sequence와 `allocationSize=50`으로 전환했다.
- H2 init schema에 동일 이름/증분의 sequence를 추가하고 대상 PK의 auto increment를 제거했다.
- Hibernate JDBC batch size 50을 적용했다. `order_inserts`, `order_updates`,
  `batch_versioned_data`는 추가하지 않았다.
- JDBC DataSource probe 기반으로 51건 INSERT가 50/1 batch로 분할되는 테스트와 기존
  MAX ID 이후 안전한 첫 block, sequence increment, 31건 Observation lineage 테스트를 추가했다.
- 신규 Catalog 생성은 `CatalogUpsertResult`의 in-memory `ImportedTrack` snapshot을
  반환하고 `TrackImportService`가 정상 생성 직후 canonical reread를 생략하도록 변경했다.
- 기존 Track, write 중 기존 row 발견, unique conflict 경로는 DB canonical read를 유지했다.
- 기존 Album 재사용 시 Album artist credit을 fetch join query로 읽고, Spotify 응답의
  다른 credit으로 기존 DB 응답이나 불필요한 Artist row를 만들지 않도록 했다.
- Oracle preflight/migration/post-check/application rollback 절차를
  `agents/server/migrations/ENRICHMENT-PERF-001.md`에 기록했다.
- Gradle test/check 및 `scripts/verify.sh`는 프로젝트 지침에 따라 Codex가 실행하지 않았다.
  사용자 실행 결과와 migration review가 확인될 때까지 Plan은 active에 유지하고
  `progress.md` 완료 기록도 추가하지 않는다.

## Completion Record — 2026-09-18

- 사용자 실행 결과로 대상 테스트와 전체 `test`, `check`, `scripts/verify.sh` 통과를 확인했다.
- Oracle migration runbook의 preflight, migration, post-check 및 rollback 절차를 검토했다.
- Acceptance Criteria와 범위 diff를 정적 검토했으며, CATALOG-IMPORT-002가 후속으로
  Catalog optimistic-create semantics를 대체하는 범위는 별도 active Plan으로 유지한다.
