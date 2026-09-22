# CATALOG-IMPORT-002 — End-to-End Track Import Query Reduction

## Status

- State: Implementation complete — user verification pending
- Original scope approved: 2026-09-18
- End-to-end replan requested: 2026-09-18
- End-to-end replan approved: 2026-09-18
- Scope: Track 선택부터 Catalog, enrichment persistence, resolution, inheritance, detail 응답까지 전체 import query reduction과 constraint-specific duplicate recovery
- Related decisions: `ADR-001-catalog-multi-artist-credits.md`, `ADR-008-enrichment-sequence-jdbc-batching.md`, `ADR-009-catalog-optimistic-create-conflict-recovery.md`

## Goal

`TrackSelectionService.select()`의 전체 import 흐름에서 중복 read와 이미 확보한 in-memory 결과의 재조회를 제거한다. 범위는 Catalog optimistic create뿐 아니라 external identity attach, Observation/Assertion 저장, Album/Track resolution, inheritance 및 최종 detail 조립까지 포함한다.

DB unique constraint 기반 멱등성, provider partial success, Album/Track별 짧은 write transaction, shared Artist/Album concurrency, canonical response와 공개 API contract는 유지한다. SQL 로그 줄 수와 JDBC batch 실행 횟수를 구분해 실제 DB round-trip을 기준으로 개선 전후를 검증한다.

## Source of Truth

- `agents/server/server_spec.md`
- `agents/server/server_tag_feature_architecture.md`
- `agents/server/current_state.md`
- `agents/server/conventions.md`
- `agents/server/progress.md`
- `agents/server/decisions/ADR-001-catalog-multi-artist-credits.md`
- `agents/server/decisions/ADR-008-enrichment-sequence-jdbc-batching.md`
- `agents/server/decisions/ADR-009-catalog-optimistic-create-conflict-recovery.md`
- `agents/server/plans/completed/CATALOG-001.md`
- `agents/server/plans/completed/ENRICHMENT-PERF-001.md`

## Current State

구현 전 `TrackImportService`는 최초 `CatalogTrackReadService.findBySpotifyId()`에서 existing Track을 빠르게 반환했다. 최초 import인 경우 Spotify metadata HTTP 뒤 `CatalogWriteService.upsert()`가 다시 `trackRepository.findBySpotifyId()`를 호출했다.

구현 전 write service가 existing Track을 발견하면 `CatalogUpsertResult.existing()`만 반환했다. `TrackImportService`는 이 marker를 받은 뒤 `CatalogTrackReadService.getBySpotifyId()`로 canonical `ImportedTrack`을 다시 만들었다. 따라서 write service의 Track select 결과는 response에 재사용되지 않았다.

구현 전 `TrackImportService`는 모든 `DataIntegrityViolationException`을 catch해 Track 존재 여부를 재조회하고, 없으면 write를 한 번 재시도했다. 이 범용 catch는 expected Catalog duplicate뿐 아니라 FK/NOT NULL 같은 non-duplicate integrity failure도 retryable로 취급할 수 있었다.

### Query boundary analysis (2026-09-18)

사용자 제공 first-load SQL trace에는 Hibernate SQL 로그 기준 약 65개 statement가 있다. `external_tag_observation` INSERT 31개는 Album/Track subject별 JDBC batch로 실행되므로 로그 줄 수와 DB `executeBatch()` 횟수는 같지 않다.

| Phase | 주요 SQL | CATALOG-IMPORT-002 범위 |
|---|---|---|
| Catalog import | 최초 Track fast-path read, Album read, Artist bulk read, Catalog inserts | 포함 |
| Catalog selection / identity attach | resolved projection gate, Track/Album `FOR UPDATE`, identity update | 포함 |
| Catalog detail | Track+Album, TrackArtist, AlbumArtist, resolved projection read | 포함 |
| Enrichment persistence | subject validation, Observation existing/alias/assertion reads 및 batched INSERT | 포함 |
| Resolution | Album/Track assertion·taxonomy·resolved reads, inheritance | 포함 |

정상 신규 Catalog 생성에서 최초 Track fast-path read는 기존 Track의 Spotify HTTP와 write를 생략하기 위해 유지한다. 이번 변경의 제거 대상은 그 뒤 `CatalogWriteService` 내부에서 하던 두 번째 Track Spotify ID existence read와 existing marker에 따른 canonical reread다.

Catalog Entity의 PK는 현재 `IDENTITY`이므로 Artist/Album/Track 및 credit INSERT를 sequence 기반 JDBC batch로 일괄 전환하는 것은 이 Plan의 범위를 벗어난 schema/migration 결정이다. ADR-008도 Catalog Entity를 batching 대상에서 제외한다.

현재 trace 기준 first-load의 약 65개 Hibernate SQL statement는 JDBC batching을 고려하면 약 36회 DB execution으로 추정된다. 이 Plan의 1차 목표는 API contract와 transaction 의미를 유지하면서 first-load를 약 27회(계측 오차/입력 차이 ±1회) DB execution으로 줄이는 것이다. 정확한 수치는 datasource-proxy 계측 결과로 확정한다.

예상 절감 근거:

| 제거 대상 | first-load 예상 절감 |
|---|---:|
| Observation Album/Track subject existence read | 2 |
| Resolution Album existence + Track/Album context read | 2 |
| Track final approved assertion reread | 1 |
| 최종 resolved + Catalog/credit canonical reread | 4 |
| identity attach의 별도 Album lock/read | 조건부 1 |

사용자 제공 trace와 같은 identity 입력에서는 우선 9회 절감을 목표로 한다. 별도 Album lock/read가 실제 발생하는 입력은 lock semantics 검증까지 통과하면 추가 1회를 줄인다.

## Target Flow

```text
TrackImportService.importTrack(spotifyTrackId)
  → CatalogTrackReadService.findBySpotifyId()
    ├─ existing: canonical ImportedTrack 반환; Spotify/write 호출 없음
    └─ absent: Spotify metadata HTTP (transaction 없음)
              → CatalogWriteService.create() (짧은 transaction)
                ├─ success: in-memory ImportedTrack snapshot 반환
                └─ retryable Catalog Spotify-ID duplicate
                    ├─ canonical Track 존재: canonical ImportedTrack 반환
                    └─ Track 없음 (Artist/Album parent conflict): create 1회 재시도
```

동시에 Track 존재 여부를 확인한 여러 요청이 모두 create를 시작할 수 있다. 정합성은 application read가 아니라 DB `UNIQUE(spotify_id)`가 최종 보장한다.

Catalog import 이후 target flow:

```text
ImportedTrack snapshot
  → resolved projection 1회 조회
    ├─ existing: 조회한 resolved rows + canonical Catalog read로 detail 반환
    └─ absent: provider 병렬 수집 (transaction 없음)
              → external identity attach
              → persisted Catalog subject임이 보장된 Album/Track observation 처리
              → Album resolve
              → Track direct assertion + Album inheritance 동기화
              → Track resolve 결과 반환
              → 기존 ImportedTrack + Track resolve 결과로 detail 조립
                 (최종 Catalog/resolved canonical reread 없음)
```

Album/Track observation 및 resolution transaction은 현재처럼 분리한다. 한 subject의 conflict가 다른 subject의 성공 결과를 rollback시키는 의미 변경은 하지 않는다.

## Scope

### In Scope

- `CatalogWriteService.upsert()`를 `create()`로 변경
- write service 최상단의 `trackRepository.findBySpotifyId()` 제거
- `CatalogUpsertResult` 제거 및 successful create의 `ImportedTrack` 직접 반환
- Catalog Spotify ID unique constraint를 식별하는 infrastructure conflict translator와 application exception/port 추가
  - `uk_track_spotify_id`
  - `uk_album_spotify_id`
  - `uk_artist_spotify_id`
- `TrackImportService`의 recovery를 constraint-specific Catalog duplicate로 제한
  - Track duplicate 후 canonical read return
- Artist/Album duplicate 후 Track absent일 때만 create 1회 재시도
- Catalog unit/JPA/concurrency/selection integration 테스트 갱신
- resolved Track selection의 projection existence check와 visible projection read를 1회 query로 통합
- Catalog external identity attach에서 Track+Album lock query와 별도 Album lock/read가 중복인지 H2/Oracle lock semantics 및 concurrency test로 검증하고, 두 DB에서 동일하게 보장될 때만 1회 query로 축소
- `TrackSelectionService`가 전달하는 persisted `ImportedTrack` context를 사용해 Observation Album/Track subject existence query를 생략하되 generic processing entry point의 validation은 유지
- Track resolution에 이미 알고 있는 Track/Album ID context를 전달해 subject Track/Album 재조회 제거
- `TagInheritanceService`가 동기화된 inherited assertion을 반환하고 direct assertion과 합쳐 final resolution에 사용해 Track approved assertion 재조회 제거
- 최종 Track resolve 결과와 기존 `ImportedTrack` snapshot으로 `TrackDetail`을 직접 조립해 resolved/Track/TrackArtist/AlbumArtist 최종 4-query reread 제거
- Catalog, enrichment persistence, resolution, detail의 SQL/JDBC execution을 phase별로 계측하는 테스트 추가
- ADR-008 및 active enrichment plans의 구현 메서드명 참조를 ADR-009와 일치하도록 최소 수정

### Out of Scope

- Spotify HTTP adapter/search/metadata mapping 변경
- Catalog schema, unique/FK/index, Entity 관계 또는 credit ordering 변경
- native DB upsert/MERGE, 새로운 pessimistic lock 전략, Redis/distributed lock, reservation row
- arbitrary multi-attempt retry/backoff/scheduler
- Album/Track enrichment transaction 통합
- Album/Track Observation 또는 resolution을 하나의 transaction으로 통합
- taxonomy/alias global cache 또는 cross-request cache 도입
- Catalog `IDENTITY` PK를 sequence로 변경하거나 Catalog INSERT JDBC batching 도입
- Resolver score/confidence/inheritance 정책 및 provider 수집·matching 정책 변경
- public endpoint, Swagger request/response/error contract 변경
- ENRICHMENT-PERF-001의 sequence/batching/migration 범위 재변경
- unrelated Catalog/Search/UserTag/Board refactoring

## Design Details

### Write service

`CatalogWriteService.create(SpotifyTrackMetadata)`는 `@Transactional`을 유지한다. Artist bulk lookup/create, existing Album reuse, new Album credit create, Track create, Track credit create 및 flush 순서는 현재와 동일하다.

새 Track은 in-memory `ImportedTrack` snapshot을 반환한다. 기존 Album을 재사용할 때 Album artist credit은 Spotify metadata로 재구성하지 않고 현재와 같이 DB canonical credit을 조회한다.

write service는 failed transaction 안에서 existing Track을 읽거나 재시도하지 않는다. flush에서 발생한 failure는 transaction rollback 뒤 caller가 처리한다.

### Measurement policy

SQL logger의 statement 줄 수는 JDBC batch의 실제 DB 왕복 횟수와 같지 않다. 검증은 Catalog import, Catalog selection/identity attach/detail, enrichment persistence, resolution을 phase별로 나누고, Catalog write 내부의 repository interaction과 JDBC `addBatch`/`executeBatch` 계측을 함께 사용한다.

### Catalog query-reduction design

1. **Resolved selection gate 통합**
   - 현재 resolved Track은 `hasResolvedProjection()`의 exists query 뒤 `getByCatalogTrackId()`에서 visible resolved tag query를 다시 실행한다.
   - `TrackDetailReadService`는 resolved rows를 한 번 읽고, row가 있으면 HIDDEN row를 제외한 SystemTagDetail과 Catalog detail을 조립하는 `findIfResolved(...)` 경로를 제공한다.
   - row가 없으면 `Optional.empty()`를 반환해 현재처럼 enrichment로 진행한다. HIDDEN-only projection도 “이미 resolve됨”으로 취급해야 하므로 visible row가 아닌 전체 resolved row의 존재로 판단한다.
   - resolved Track selection은 기존 두 resolved query를 한 번으로 줄이고, first-load Track은 기존 projection check 한 번을 유지한다.

2. **External identity attach lock/read 검증**
   - 현재 `findByIdWithAlbumForUpdate()`는 Album을 join fetch한 뒤, release group ID가 있으면 `findByIdForUpdate()`로 Album을 다시 읽고 lock한다.
   - H2와 운영 Oracle에서 첫 `FOR UPDATE`가 Track과 join된 Album 모두를 lock하는지 SQL/동시성 test와 Oracle review로 확인한다.
   - 두 DB에서 Album row lock과 lost-update 방지가 보장될 때에만 두 번째 Album query를 제거하고 join-fetched managed Album을 갱신한다. 보장되지 않으면 이 query는 유지하며 성능 개선으로 주장하지 않는다.

3. **Catalog import와 detail read의 유지 근거**
   - 정상 신규 import의 최초 Track fast-path, Album lookup, Artist bulk lookup은 각각 다른 identity 판단을 위한 최소 read다. write 내부 Track reread는 제거한다.
   - TrackArtist와 AlbumArtist는 두 collection을 하나의 fetch join으로 합치면 credit 수의 곱만큼 row가 증가할 수 있으므로, DB canonical detail이 필요한 경로에서는 현재의 Track+Album, Track credit, Album credit 3개 query를 유지한다. first-load 성공 경로는 뒤에서 설명하는 in-memory snapshot 조립으로 이 3개 query 자체를 생략한다.
   - Catalog `IDENTITY` PK 때문에 신규 Artist/Album/Track/credit INSERT batching은 별도 ADR/migration milestone으로 보류한다.

### Enrichment persistence query-reduction design

1. **Persisted subject context 재사용**
   - `TrackSelectionService`가 가진 `ImportedTrack`은 Track과 Album이 commit된 Catalog row임을 보장한다.
   - selection 전용 processing entry point는 이 context에서 만든 Track/Album subject를 사용해 `ObservationWriteService.requireSubject()`의 Track/Album existence query를 생략한다.
   - 다른 caller가 raw `SubjectRef`만 전달하는 generic entry point는 기존 validation을 유지해 polymorphic subject orphan 생성을 막는다.

2. **Observation idempotency 유지**
   - 기존 Observation 및 Assertion bulk lookup은 재시도, 이전 partial import 및 concurrent processing을 위해 유지한다.
   - 신규/기존 여부를 전달하는 marker를 다시 도입해 lookup을 무조건 생략하지 않는다. first import가 provider 또는 resolution 중간에 실패한 뒤 재호출되는 경로도 기존 row를 재사용해야 한다.
   - Alias lookup, Observation/Assertion unique, conflict translation과 1회 retry는 유지한다.

3. **Batch 판정**
   - Observation/Assertion INSERT는 SQL 로그 줄 수가 아니라 datasource-proxy의 `batch=true`, batch size, `executeBatch()` 횟수로 측정한다.
   - Album/Track subject별 transaction과 flush는 유지하므로 각 subject batch를 별도로 센다.

### Resolution and response query-reduction design

1. **Known Track/Album context 재사용**
   - selection 전용 resolve command는 `ImportedTrack`의 Track ID와 Album ID를 전달한다.
   - Album resolve의 `existsById()`와 Track resolve의 `findByIdWithAlbum()`을 생략한다. generic raw-subject resolve entry point의 validation은 유지한다.

2. **Inheritance 결과 재사용**
   - Track direct assertion은 한 번 조회한다.
   - `TagInheritanceService.synchronizeAlbumInheritance(...)`는 Album direct assertion과 existing inherited assertion을 조회해 동기화한 뒤, 이번 transaction의 canonical inherited assertion 목록을 반환한다.
   - Track resolution은 direct + returned inherited assertions를 사용하며 `findApprovedBySubject(TRACK, ...)`를 다시 호출하지 않는다.
   - stale delete, higher-confidence parent 선택, direct-over-inherited 및 duplicate retry 의미는 유지한다.

3. **최종 detail snapshot 조립**
   - `TagResolutionService.resolve(TRACK)`가 반환한 `ResolvedTagResult`는 flush가 끝난 현재 projection이다.
   - identity attach가 채택한 Recording/Release Group ID는 immutable `ImportedTrack`/`ImportedAlbum` snapshot에도 copy-on-write 방식으로 반영해 DB canonical identity와 같은 값을 유지한다.
   - `TrackSelectionService`는 HIDDEN을 제외하고 기존 score/tagId 순서를 유지해 `SystemTagDetail`로 변환하고, 갱신된 `ImportedTrack` snapshot과 합쳐 `TrackDetail`을 반환한다.
   - 따라서 first-load 성공 경로의 visible resolved, Track+Album, TrackArtist, AlbumArtist 최종 reread 4개를 제거한다.
   - 이미 resolved된 fast path는 DB canonical state가 필요하므로 resolved row 1회와 Catalog detail 3회 조회를 유지한다.

4. **의도적으로 유지하는 query**
   - Album/Track resolver의 taxonomy read는 서로 다른 transaction snapshot 의미를 유지하기 위해 각각 실행한다.
   - Observation existing/alias/assertion bulk lookup과 resolution existing resolved lookup은 idempotency 및 manual/automatic row 동기화를 위해 유지한다.

### Conflict translation and recovery

Enrichment/Resolution의 constraint-name translation pattern을 따른다. Hibernate cause chain의 constraint name을 검사해 세 Catalog Spotify-ID unique constraint만 dedicated Catalog duplicate exception으로 변환한다. constraint name이 없거나 대상이 아니면 원래 failure를 유지한다.

`TrackImportService`는 dedicated Catalog duplicate만 catch한다.

1. Track을 canonical read한다.
2. Track이 있으면 반환한다. 이는 Track duplicate 및 경쟁 요청이 이미 commit한 경우다.
3. Track이 없고 conflict가 Artist 또는 Album Spotify ID duplicate이면 `create()`를 정확히 한 번 재시도한다.
4. 재시도 failure는 catch하지 않고 전파한다.

이 bounded policy는 현재 구현의 one-retry boundary를 유지한다. 3개 이상의 동시 요청은 test로 보장하지만 multi-attempt retry loop는 실제 contention 측정 전에는 추가하지 않는다.

## Files

### Production

- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/CatalogWriteService.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/TrackImportService.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/model/ImportedTrack.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/model/ImportedAlbum.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/detail/TrackDetailReadService.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/selection/TrackSelectionService.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/CatalogExternalIdentityWriteService.java` (lock semantics 검증 통과 시에만)
- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/catalog/TrackJpaRepository.java` (lock semantics 검증 통과 시에만)
- `tagnote-core/src/main/java/com/tagnote/application/enrichment/ObservationProcessingService.java`
- `tagnote-core/src/main/java/com/tagnote/application/enrichment/ObservationWriteService.java`
- `tagnote-core/src/main/java/com/tagnote/application/resolution/TagResolutionService.java`
- `tagnote-core/src/main/java/com/tagnote/application/resolution/TagResolutionWriteService.java`
- `tagnote-core/src/main/java/com/tagnote/application/resolution/TagInheritanceService.java`
- persisted Catalog subject / selection resolve context application model (구현 중 최소 package 확정)
- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/model/CatalogUpsertResult.java` (삭제)
- Catalog conflict application port/exception package (기존 enrichment/resolution naming을 확인해 최소 추가)
- Catalog Hibernate conflict translator infrastructure package

### Tests

- `tagnote-core/src/test/java/com/tagnote/application/catalog/importer/TrackImportServiceTest.java`
- `tagnote-core/src/test/java/com/tagnote/infrastructure/persistence/catalog/CatalogJpaRepositoryTest.java`
- `tagnote-core/src/test/java/com/tagnote/infrastructure/persistence/catalog/CatalogImportConcurrencyTest.java`
- Catalog conflict translator unit test
- `tagnote-core/src/test/java/com/tagnote/application/catalog/selection/FirstVerticalSliceIntegrationTest.java` (wiring/regression 필요 시)
- end-to-end import phase SQL/JDBC execution 계측 test (test-support 위치는 기존 JDBC batch instrumentation을 재사용해 확정)
- resolved Track selection query-count / HIDDEN-only projection regression test
- Catalog external identity attach H2/Oracle lock/concurrency regression test
- `ObservationProcessingService` / `EnrichmentJpaRepositoryTest` persisted-subject validation/query regression
- `TagResolutionServiceTest`, `SubjectTagResolvedJpaRepositoryTest`, resolution/inheritance concurrency regression
- first-load end-to-end JDBC execution count regression test

### Documents

- `agents/server/decisions/ADR-009-catalog-optimistic-create-conflict-recovery.md`
- `agents/server/decisions/ADR-008-enrichment-sequence-jdbc-batching.md`
- `agents/server/plans/active/CATALOG-IMPORT-002.md`
- `agents/server/plans/completed/ENRICHMENT-PERF-001.md`
- `agents/server/plans/completed/ENRICHMENT-001.md`
- 완료 시 `agents/server/progress.md`

## Implementation Steps

1. Existing fast path, first create snapshot, existing Album canonical credit, duplicate recovery의 characterization tests를 먼저 갱신한다.
2. Catalog constraint translator와 dedicated duplicate exception을 추가하고 expected Catalog unique와 non-duplicate failure 구분 unit test를 작성한다.
3. `CatalogWriteService.upsert()`를 `create()`로 변경하고 Track pre-read 및 `CatalogUpsertResult`를 제거한다.
4. `TrackImportService`를 direct create result와 constraint-specific recovery로 변경한다.
5. 기존/동시 import JPA tests를 새 create semantics로 갱신한다.
6. 3개 동일 Track 동시 import 및 shared Artist/Album을 가진 서로 다른 Track 동시 import를 추가한다.
7. `TrackDetailReadService.findIfResolved(...)`로 resolved projection existence/read를 통합하고 HIDDEN-only 및 first-load fallback 회귀를 검증한다.
8. Catalog external identity attach의 H2/Oracle lock scope와 concurrency를 검증한다. Album lock이 첫 query로 보장되는 경우에만 두 번째 Album read를 제거한다.
9. persisted `ImportedTrack` context를 받는 selection 전용 Observation 처리 경로를 추가하고 subject existence query 2개를 제거한다. generic entry point validation 회귀를 함께 검증한다.
10. selection 전용 resolution context로 Album existence와 Track+Album subject query를 제거한다.
11. `TagInheritanceService`가 synchronized inherited assertions를 반환하게 하고 Track final assertion reread를 제거한다. stale/update/direct-over-inherited/concurrency 회귀를 검증한다.
12. final Track resolve 결과와 `ImportedTrack`으로 first-load `TrackDetail`을 조립해 최종 canonical reread 4개를 제거한다.
13. Catalog, enrichment persistence, resolution, detail의 SQL/JDBC execution을 phase별 및 end-to-end로 계측하고 약 27회(±1회) 목표를 실제 baseline으로 확정한다.
14. API/Swagger contract, provider partial success 및 external HTTP transaction boundary가 변경되지 않았는지 review한다.
15. ADR-008과 active enrichment plan의 stale `upsert`/result marker 참조를 최소 변경한다.
16. 사용자 검증 결과와 diff review가 통과하면 progress를 갱신하고 Plan을 `plans/completed/`로 이동한다.

## Acceptance Criteria

- [ ] Existing Track fast path는 Spotify metadata와 Catalog create를 호출하지 않는다.
- [ ] 정상 최초 import에서 write service 내부 Track existence SELECT가 없다.
- [ ] 정상 최초 import는 write snapshot을 반환하며 canonical reread가 없다.
- [ ] Catalog import 계측은 최초 fast path와 write 내부 query를 구분하며, `create()` 진입 후 Track Spotify ID existence SELECT가 없음을 검증한다.
- [ ] resolved Track selection은 projection existence/read를 한 번만 실행하고 HIDDEN-only projection도 enrichment를 재실행하지 않는다.
- [ ] first-load Track은 resolved projection query 한 번 뒤 기존 enrichment flow로 진행한다.
- [ ] external identity attach는 H2/Oracle에서 Album lock 보장을 증명한 경우에만 두 번째 Album read를 제거하며, 증명하지 못하면 기존 두-query lock/read를 유지한다.
- [ ] selection의 persisted subject 처리에서는 Album/Track existence query가 없고, generic Observation 처리에서는 subject validation이 유지된다.
- [ ] Album resolve의 subject existence query와 Track resolve의 Track+Album subject query가 selection context에서 제거된다.
- [ ] Track direct assertion과 synchronized inherited assertions를 재사용해 final approved assertion reread가 없다.
- [ ] first-load 성공 response는 기존 `ImportedTrack`과 final resolved 결과로 조립하며 resolved/Track/credit 최종 4-query reread가 없다.
- [ ] in-memory response snapshot은 identity attach에서 채택한 Recording/Release Group ID까지 반영해 기존 canonical reread 응답과 내부적으로 동일하다.
- [ ] existing resolved Track은 DB canonical Catalog/credit/resolved state를 반환하고 HIDDEN-only projection도 enrichment를 재실행하지 않는다.
- [ ] Catalog, enrichment persistence, resolution, detail의 query/round-trip baseline을 phase별로 기록한다. Observation/Assertion/Resolved INSERT batch는 JDBC `executeBatch()` 기준으로 검증한다.
- [ ] 사용자 제공 baseline과 동일한 31 Observation first-load에서 DB execution은 목표 약 27회(±1회)이며, 실제 계측 차이가 있으면 SQL 분류와 이유를 Plan에 기록하고 Human Review한다.
- [ ] Observation/Assertion/Resolved unique, retry, manual/automatic synchronization, direct-over-inherited, provider partial success 의미가 유지된다.
- [ ] `CatalogUpsertResult`와 existing marker 분기가 제거된다.
- [ ] Track/Album/Artist Spotify ID unique constraint는 유지된다.
- [ ] 같은 Track을 2개 및 3개 요청이 동시에 import해도 하나의 Catalog Track으로 수렴하고 모든 요청이 해당 Track을 반환한다.
- [ ] shared Artist/Album을 가진 서로 다른 Track의 동시 import가 parent/credit 중복 없이 성공한다.
- [ ] Track duplicate는 canonical read로 복구된다.
- [ ] Artist/Album duplicate는 Track이 없을 때 한 번만 create 재시도한다.
- [ ] 대상 외 `DataIntegrityViolationException`은 recovery/retry 없이 전파된다.
- [ ] 두 번째 create failure는 전파된다.
- [ ] 기존 Album artist credit은 DB canonical state를 반환한다.
- [ ] external HTTP는 DB transaction 밖에 있고 API/Swagger contract는 변경되지 않는다.
- [ ] 사용자가 대상 테스트, `test`, `check`, `scripts/verify.sh` 결과를 전달한다.
- [ ] diff review에서 transaction, N+1, unique/FK, idempotency, concurrency, layer violation 문제가 없다.

## User Verification Commands

```powershell
.\gradlew.bat :tagnote-core:test --tests '*TrackImportServiceTest' --tests '*TrackSelectionServiceTest' --tests '*TrackDetailReadServiceTest' --tests '*CatalogJpaRepositoryTest' --tests '*CatalogImportConcurrencyTest' --tests '*Catalog*ConflictTranslatorTest' --tests '*ObservationProcessingServiceTest' --tests '*EnrichmentJpaRepositoryTest' --tests '*ObservationProcessingConcurrencyTest' --tests '*TagResolutionServiceTest' --tests '*TagInheritanceServiceTest' --tests '*TrackTagResolutionIntegrationTest' --tests '*SubjectTagResolvedJpaRepositoryTest' --tests '*TagInheritanceConcurrencyTest' --tests '*TagResolutionConcurrencyTest' --tests '*FirstVerticalSliceIntegrationTest'
.\gradlew.bat test
.\gradlew.bat check
```

WSL/Git Bash:

```bash
./scripts/verify.sh
```

## Implementation Notes

- End-to-end implementation과 regression/query instrumentation 작성 완료.
- first-load query probe는 사용자 trace와 같이 Album 12개 + Track 19개 Observation을 사용하며 subject validation, final credit reread 및 final approved assertion reread 제거를 구조적으로 검증한다.
- Catalog external identity의 별도 Album `FOR UPDATE` read는 H2/Oracle 모두에서 첫 join-lock이 Album row까지 동일하게 보호한다는 근거를 확정하지 못해 유지했다. 이 조건부 1-query 절감은 완료 주장에 포함하지 않는다.
- Gradle/test/verify는 프로젝트 규칙에 따라 사용자가 실행하며, 결과 확인 전에는 Plan을 completed로 이동하지 않는다.

## Completion

구현과 테스트 코드 작성 후 사용자가 검증 명령 결과를 전달하고, Acceptance Criteria와 diff review가 통과한 경우에만 이 Plan을 `agents/server/plans/completed/`로 이동하고 `progress.md`에 완료 상태를 기록한다.
