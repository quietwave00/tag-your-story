# TAG-CORE-001 — Taxonomy & Subject Foundation

## Goal

기존 Spotify Catalog의 내부 `Track`·`Album` identity를 Tag 기능에서 안전하게 참조할 수 있는 기반을 만들고, 외부 raw tag 문자열을 내부 taxonomy의 approved alias에 exact match할 수 있는 최소 Tag Core를 구현한다.

이번 마일스톤의 완료 지점은 다음과 같다.

```text
raw tag string
→ TagNameNormalizer
→ APPROVED TagAlias exact lookup
→ matched Tag 또는 unmatched 결과
```

```text
TrackEntity / AlbumEntity
→ SubjectRef(TRACK | ALBUM, internal catalog id)
```

`ExternalTagObservation`, `TagAssertion`, `SubjectTagResolved`, `TagResolver`는 이 기반을 사용하는 `TAG-CORE-002`~`004`로 분리한다.

## Context

- `SEARCH-REF-001`, `SEARCH-RANK-001`이 완료되어 Search use case와 외부 Adapter 경계가 분리되어 있다.
- 기존 `CATALOG-001`에서 Spotify ID 기반 `Artist`, `Album`, `Track` upsert와 내부 Catalog PK가 구현되어 있다.
- Catalog는 외부 Spotify HTTP와 write transaction을 분리하고, DB unique 제약으로 중복 및 동시 import를 방어한다.
- ADR-001에 따라 Album/Track Artist 관계는 `album_artist`, `track_artist` 연결 Entity가 source of truth다.
- `current_state.md`는 Catalog 구현 전 characterization baseline이므로 현재 production 상태는 `progress.md`와 실제 코드를 함께 기준으로 판단한다.
- 현재 System Tag production 구현과 관련 DB 테이블은 없다.
- System Tag와 기존 `UserTag`는 별개의 개념이며 Entity, Repository, Service, 테이블을 공유하지 않는다.
- `server_tag_feature_architecture.md`의 개발 순서상 이번 범위는 Phase 2 Taxonomy의 최소 기반에 해당한다.

## Target State

### Taxonomy

```text
Tag
  └─ TagAlias

raw name
  ↓ normalize
normalized name
  ↓ APPROVED alias exact lookup
unique match → matched Tag
no match / ambiguous match → unmatched
```

### Subject Identity

```text
Catalog Track Entity → SubjectRef.track(trackId)
Catalog Album Entity → SubjectRef.album(albumId)
```

- Tag는 System Tag taxonomy의 canonical identity다.
- TagAlias는 외부 표현과 canonical Tag 사이의 exact-match 사전이다.
- Matcher는 외부 Provider나 JPA 구현 타입을 알지 않는다.
- `SubjectRef`는 이후 Observation, Assertion, Resolver에서 같은 타입을 재사용한다.
- MVP Subject는 `TRACK`, `ALBUM`만 허용한다.

## Scope

### In Scope

- `Tag` JPA/Domain 모델
- `TagType`: `GENRE`, `STYLE`, `SCENE`, `COUNTRY`, `ERA`
- `TagStatus`: `ACTIVE`, `CANDIDATE`, `DEPRECATED`, `MERGED`
- Tag merge 상태 불변식
- `TagAlias` JPA/Domain 모델
- `AliasSource`: `ADMIN`, `MUSICBRAINZ`, `DISCOGS`, `USER`
- `AliasStatus`: `PENDING`, `APPROVED`, `REJECTED`
- Alias 상태 전이 및 불변식
- `NormalizedTagName` Value Object
- `TagNameNormalizer`
- approved alias exact matching을 수행하는 `TagMatchingService`
- match 성공/실패/모호함을 표현하는 provider-neutral 결과 모델
- `SubjectType(TRACK, ALBUM)`과 `SubjectRef`
- 이미 조회한 `TrackEntity` 또는 `AlbumEntity`에서 `SubjectRef` 생성
- Tag/TagAlias DB schema, JPA mapping, Repository query
- Domain unit test와 JPA integration test
- 기존 Catalog/Search/API 회귀 검증

### Matching Policy

- trim, lowercase, Unicode normalization, 연속 공백 축약을 수행한다.
- 의미 있는 punctuation을 일괄 제거하지 않는다.
- `APPROVED` alias만 자동 matching에 사용한다.
- fuzzy matching은 사용하지 않는다.
- normalized alias가 정확히 한 canonical Tag와 연결될 때만 matched로 반환한다.
- approved alias가 없으면 unmatched로 반환한다.
- 복수 Tag의 approved alias가 같은 normalized value를 가지면 임의 선택하지 않고 ambiguous 결과를 반환한다.
- `MERGED` Tag의 canonicalization은 Resolver 마일스톤에서 구현한다. 이번 Matcher는 alias가 직접 참조하는 Tag와 상태를 결과에 명시한다.

## Do Not Touch

- `ExternalTagObservation` Entity, 테이블, Repository, 상태 전이
- `TagAssertion` Entity, 테이블, Repository
- `SubjectTagResolved` Entity, 테이블, Repository
- `TagResolver`, score 계산, projection 갱신
- Album → Track inheritance
- `MANUAL_FIXED`, `HIDDEN` 처리
- MusicBrainz/Discogs HTTP client와 외부 응답 DTO
- MusicBrainz entity matching과 Catalog MBID 갱신
- 외부 Provider 병렬 호출 및 partial success
- `subject_enrichment_status`
- Alias 승인 후 Observation rematch
- Scheduler/Batch
- Tag Admin API와 Swagger
- `TagParent`, `TagAssociation`, hierarchy/fusion
- Artist/Label profile inheritance
- 캐시, Kafka, 분산 락
- 기존 `TrackImportService`와 Spotify ID upsert 정책
- 기존 `Artist`, `Album`, `Track`, Artist credit Entity
- Track 상세 API의 System Tag 노출
- Board의 문자열 Spotify `trackId`와 Board→Catalog FK 전환
- 기존 `UserTag`, `BoardUserTag` (`USER-TAG-001`의 사용자 소유 identity 전환과 별도 milestone)
- `current_state.md` baseline 재작성
- Resolver 전용 `application-tag.yml`; Resolver 구현 마일스톤에서 생성한다.

## 변경/생성 파일

정확한 package와 클래스명은 구현 시작 시 기존 naming 충돌 여부를 다시 확인하되 책임과 범위는 아래를 따른다.

### 변경 파일

- `tagnote-core/src/main/resources/db/init_schema.sql`
  - `tag`, `tag_alias` 테이블과 PK/FK/unique/index 추가
- `tagnote-core/src/test/java/com/tagnote/infrastructure/persistence/catalog/CatalogJpaTestConfiguration.java`
  - 기존 설정을 함께 재사용하는 것이 필요한 경우에만 Entity/Repository scan 범위 변경
- `agents/server/progress.md`
  - 모든 구현과 검증 완료 시 milestone 완료 상태 기록
- `agents/server/plans/active/TAG-CORE-001.md`
  - 완료 시 `plans/completed/`로 이동

### 생성 — Domain Taxonomy

- `tagnote-core/src/main/java/com/tagnote/domain/taxonomy/tag/TagEntity.java`
- `tagnote-core/src/main/java/com/tagnote/domain/taxonomy/tag/TagType.java`
- `tagnote-core/src/main/java/com/tagnote/domain/taxonomy/tag/TagStatus.java`
- `tagnote-core/src/main/java/com/tagnote/domain/taxonomy/alias/TagAliasEntity.java`
- `tagnote-core/src/main/java/com/tagnote/domain/taxonomy/alias/AliasSource.java`
- `tagnote-core/src/main/java/com/tagnote/domain/taxonomy/alias/AliasStatus.java`
- `tagnote-core/src/main/java/com/tagnote/domain/taxonomy/matching/NormalizedTagName.java`
- `tagnote-core/src/main/java/com/tagnote/domain/taxonomy/matching/TagNameNormalizer.java`
- `tagnote-core/src/main/java/com/tagnote/domain/taxonomy/matching/TagMatchingService.java`
- `tagnote-core/src/main/java/com/tagnote/domain/taxonomy/matching/TagMatchResult.java`

### 생성 — Domain Subject

- `tagnote-core/src/main/java/com/tagnote/domain/enrichment/subject/SubjectType.java`
- `tagnote-core/src/main/java/com/tagnote/domain/enrichment/subject/SubjectRef.java`

`SubjectRef`는 후속 enrichment에서 사용될 위치에 두되 Observation 또는 Assertion 구현을 선행하지 않는다.

### 생성 — Infrastructure Persistence

- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/taxonomy/TagJpaRepository.java`
- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/taxonomy/TagAliasJpaRepository.java`

### 생성 — Test

- `tagnote-core/src/test/java/com/tagnote/domain/taxonomy/matching/TagNameNormalizerTest.java`
- `tagnote-core/src/test/java/com/tagnote/domain/taxonomy/matching/TagMatchingServiceTest.java`
- `tagnote-core/src/test/java/com/tagnote/domain/taxonomy/tag/TagEntityTest.java`
- `tagnote-core/src/test/java/com/tagnote/domain/taxonomy/alias/TagAliasEntityTest.java`
- `tagnote-core/src/test/java/com/tagnote/domain/enrichment/subject/SubjectRefTest.java`
- `tagnote-core/src/test/java/com/tagnote/infrastructure/persistence/taxonomy/TaxonomyJpaTestConfiguration.java`
- `tagnote-core/src/test/java/com/tagnote/infrastructure/persistence/taxonomy/TaxonomyJpaRepositoryTest.java`

새로운 아키텍처 결정을 내리지 않는 한 ADR은 생성하지 않는다. 구현 중 명세와 다른 unique 정책 또는 상태 모델을 선택해야 한다면 먼저 보고하고, Scope에 영향을 주는 결정을 임의로 적용하지 않는다.

## 데이터 흐름

### Alias Matching

```text
raw tag string
→ TagNameNormalizer.normalize(rawName)
→ NormalizedTagName
→ TagAliasJpaRepository가 normalized_alias + APPROVED를 bulk 조회
→ TagMatchingService가 distinct Tag 기준으로 판정
   ├─ 0개: unmatched
   ├─ 1개: matched Tag
   └─ 2개 이상: ambiguous
```

- Repository는 `TagAlias`와 `Tag`를 함께 적재해 alias별 추가 SELECT를 만들지 않는다.
- Matcher는 JPA query를 직접 실행하지 않는다. Application 또는 테스트 호출자가 조회한 후보를 전달한다.
- 단건 raw tag 처리 API는 만들지 않으며 후속 Observation 처리에서 목록 단위로 재사용할 수 있어야 한다.

### SubjectRef 생성

```text
이미 조회한 TrackEntity
→ SubjectRef.track(track.getTrackId())

이미 조회한 AlbumEntity
→ SubjectRef.album(album.getAlbumId())
```

- `SubjectRef` 생성 자체가 Repository를 호출하지 않는다.
- 후속 Application Service는 Catalog Entity를 한 번 조회한 뒤 `SubjectRef`를 만든다.
- `existsById()`를 추가로 반복하는 구조를 만들지 않는다.
- Spotify ID는 Catalog import/upsert에만 사용하고 Tag subject identity에는 사용하지 않는다.

## DB/JPA 설계

### tag

| Column | Constraint / Meaning |
|---|---|
| `tag_id` | bigint identity PK |
| `name` | canonical display name, not null |
| `slug` | stable canonical slug, not null |
| `type` | `TagType`, not null |
| `status` | `TagStatus`, not null |
| `merged_into_tag_id` | nullable self FK |
| `description` | nullable |
| `created_at`, `updated_at` | `BaseTime` |

제약과 인덱스:

- `PK(tag_id)`
- `UNIQUE(slug)`
- `FK(merged_into_tag_id → tag.tag_id)`
- `INDEX(type, status)`
- `INDEX(name)`

Domain invariant:

- `MERGED`이면 `merged_into_tag_id`가 필수다.
- `MERGED`가 아니면 `merged_into_tag_id`는 null이다.
- 자기 자신을 merge 대상으로 지정할 수 없다.
- merge chain canonicalization과 cycle 검증은 Resolver/관리 기능 마일스톤으로 미룬다.

### tag_alias

| Column | Constraint / Meaning |
|---|---|
| `alias_id` | bigint identity PK |
| `tag_id` | not null FK |
| `alias` | 원본 alias, not null |
| `normalized_alias` | normalization 결과, not null |
| `source` | `AliasSource`, not null |
| `status` | `AliasStatus`, not null |

제약과 인덱스:

- `PK(alias_id)`
- `FK(tag_id → tag.tag_id)`
- `UNIQUE(tag_id, normalized_alias)`
- `INDEX(normalized_alias, status)`

문서의 composite unique를 유지한다. 전역 `UNIQUE(normalized_alias)`를 임의로 추가하지 않는다. 동일 normalized alias가 복수 Tag에 승인된 경우 Matcher가 ambiguous로 처리한다.

### JPA 관계와 조회

```text
TagEntity --ManyToOne(LAZY)--> mergedIntoTag
TagAliasEntity --ManyToOne(LAZY)--> TagEntity
```

- Tag와 TagAlias에 역방향 `OneToMany` 컬렉션을 두지 않는다.
- cascade와 orphan removal을 사용하지 않는다.
- Entity를 API response로 직접 반환하지 않는다.
- alias exact lookup은 `normalized_alias + APPROVED` 조건으로 조회한다.
- 목록 matching을 고려해 normalized name collection bulk query를 제공한다.
- alias lookup query는 Tag를 fetch join 또는 EntityGraph로 함께 적재한다.
- matching 과정에서 alias 수에 비례하는 Tag SELECT를 발생시키지 않는다.

## Transaction

이번 마일스톤에는 외부 HTTP 호출과 공개 write use case가 없다.

- `TagNameNormalizer`, `TagMatchingService`, `SubjectRef`는 transaction을 사용하지 않는다.
- Repository read test와 향후 호출을 고려해 matching용 조회는 read-only transaction 안에서 완료할 수 있어야 한다.
- Tag/Alias fixture 저장을 제외한 production 생성 use case는 구현하지 않는다.
- 향후 Admin Tag/Alias write transaction과 Observation pipeline transaction을 이번 단계에서 선행 구현하지 않는다.
- Entity의 상태 변경 메서드는 불변식을 지키지만, 실제 저장 transaction 책임은 후속 Application Service에 둔다.

## 기존 API 영향

Controller 및 Swagger 변경은 없다.

다음 contract를 그대로 유지한다.

- `GET /api/tracks`
- `GET /api/tracks/{trackId}`
- `GET /api/tracks/ranking`
- `POST /api/tracks/import`
- Search response의 Spotify Track ID 의미
- Catalog import response의 내부 `catalogTrackId`
- Board의 문자열 Spotify `trackId`
- 기존 UserTag API와 저장 구조

이번 마일스톤의 Tag taxonomy는 아직 사용자 API에 노출되지 않는다.

## 향후 연결 구조

후속 마일스톤은 이번 결과를 다음 순서로 재사용한다.

```text
TAG-CORE-001 결과
Tag / TagAlias / Normalizer / Matcher / SubjectRef
        │
        ▼
TAG-CORE-002 Observation & Assertion
        │
        ▼
TAG-CORE-003 Resolver & Resolved Projection
        │
        ▼
TAG-CORE-004 Album → Track Inheritance
```

- 후속 External Enrichment adapter는 raw name을 provider-neutral 입력으로 변환한다.
- Observation은 이번 `TagNameNormalizer`를 재사용한다.
- Observation matching은 이번 approved alias bulk query와 `TagMatchingService`를 재사용한다.
- Assertion과 Resolved는 이번 `SubjectRef`를 subject identity로 사용한다.
- Resolver 설정은 Resolver 구현 시 전용 `application-tag.yml`에 둔다.
- 후속 상세 범위는 `TAG-CORE-002.md`, `TAG-CORE-003.md`, `TAG-CORE-004.md`에서 관리한다.

## 테스트 계획

### Domain Unit Test

- 앞뒤 공백 제거
- lowercase 변환
- Unicode normalization
- 연속 공백 축약
- `R&B`, `2-Step`, `Drum 'n' Bass`의 의미 있는 punctuation 보존
- 같은 의미의 입력이 동일한 `NormalizedTagName`을 생성
- blank raw name 거부
- `MERGED` 상태와 merge 대상 불변식
- 자기 자신 merge 거부
- Alias의 원본 값과 normalized 값 분리 보존
- Alias approve/reject 상태 전이
- `APPROVED` alias 한 건이면 matched
- 후보가 없으면 unmatched
- 복수 Tag 후보이면 ambiguous이고 임의 Tag를 선택하지 않음
- 같은 Tag의 중복 후보는 distinct Tag 한 건으로 처리
- null이 아닌 양수 ID만 `SubjectRef`로 생성
- MVP에서 TRACK/ALBUM 외 SubjectType이 없음

### JPA Integration Test

- Tag와 TagAlias를 저장하고 조회할 수 있음
- `tag.slug` unique 제약이 실제 DB에서 동작
- `(tag_id, normalized_alias)` unique 제약이 실제 DB에서 동작
- 존재하지 않는 Tag를 참조하는 Alias FK가 실제 DB에서 거부됨
- `normalized_alias + status` 조회가 APPROVED만 반환
- 여러 normalized name을 하나의 bulk query로 조회
- Alias와 Tag를 함께 적재해 transaction 밖 mapping에서 lazy-loading 오류가 없음
- Alias별 추가 Tag SELECT가 발생하지 않음
- Entity annotation과 `init_schema.sql`의 제약 및 index가 일치

### Regression Test

- 기존 Catalog import와 concurrency test
- 기존 Search/Ranking/Track 테스트
- 기존 Board/UserTag 테스트
- 기존 Controller contract 테스트

## 위험 요소

- **Alias 모호성**: 명세의 `UNIQUE(tag_id, normalized_alias)`만으로 동일 alias의 복수 Tag 승인을 막을 수 없다. 이번 Matcher는 ambiguous를 명시적으로 반환하며 임의 선택하지 않는다.
- **Normalization 과잉**: punctuation을 과도하게 제거하면 `R&B`, `2-Step` 등의 의미가 손실된다. 최소 normalization만 적용하고 테스트로 고정한다.
- **MERGED 범위**: Tag 상태 불변식은 필요하지만 canonical chain 계산까지 포함하면 Resolver scope를 선행하게 된다. 이번에는 직접 merge 상태 표현까지만 구현한다.
- **Polymorphic FK 부재**: `SubjectRef`는 DB FK가 아니다. 후속 Application Service가 이미 조회한 Catalog Entity에서 생성해야 한다.
- **계층 배치**: 현재 프로젝트에는 legacy `com.tagnote.core.domain`과 신규 `com.tagnote.domain`이 공존한다. 완료된 Catalog의 layer-first 패키지 구조를 재사용한다.
- **Schema 배포**: 운영은 Oracle `ddl-auto=validate`이므로 `init_schema.sql`만으로 production schema가 자동 변경되지 않는다. 운영 DDL/migration은 별도 작업이다.
- **과도한 선행 구현**: Observation/Assertion/Resolver 편의를 이유로 관련 Entity, Repository 또는 설정을 미리 추가하지 않는다.

## Acceptance Criteria

- [ ] `Tag`와 `TagAlias`가 기존 `UserTag`와 분리된 모델 및 테이블로 존재한다.
- [ ] Tag type/status와 Alias source/status가 명세의 MVP enum을 따른다.
- [ ] `MERGED` 상태와 merge 대상의 기본 불변식이 Domain method로 보호된다.
- [ ] `tag.slug`와 `(tag_id, normalized_alias)` 중복이 DB unique 제약으로 방지된다.
- [ ] `TagAlias → Tag` FK가 schema와 JPA에 존재한다.
- [ ] `TagAlias → Tag`, `Tag → merged target` 관계는 단방향 LAZY다.
- [ ] Tag와 TagAlias에 불필요한 역방향 컬렉션이 없다.
- [ ] normalization이 trim, lowercase, Unicode, 연속 공백을 처리한다.
- [ ] 의미 있는 punctuation을 과도하게 제거하지 않는다.
- [ ] approved alias exact match만 성공한다.
- [ ] alias가 없으면 unmatched 결과를 반환한다.
- [ ] 복수 canonical Tag가 매칭되면 ambiguous 결과를 반환하며 임의 선택하지 않는다.
- [ ] alias 목록 조회에서 N+1이 발생하지 않는다.
- [ ] `SubjectRef`가 Track/Album 내부 PK와 타입을 함께 보존한다.
- [ ] `SubjectRef`가 Spotify ID를 사용하지 않는다.
- [ ] `ExternalTagObservation`, `TagAssertion`, `SubjectTagResolved`, `TagResolver`, inheritance를 구현하지 않는다.
- [ ] Controller와 Swagger contract 변경이 없다.
- [ ] 기존 Search/Track/Catalog/Board/UserTag 동작이 유지된다.
- [ ] 전체 테스트와 검증 명령이 통과한다.
- [ ] diff review에서 관련 없는 변경이 없다.
- [ ] 완료 시 `progress.md` 갱신과 Plan 이동이 함께 이루어진다.

## Verification

구현 중 빠른 검증:

```bash
./gradlew :tagnote-core:test --tests '*TagNameNormalizerTest'
./gradlew :tagnote-core:test --tests '*TagMatchingServiceTest'
./gradlew :tagnote-core:test --tests '*TagEntityTest'
./gradlew :tagnote-core:test --tests '*TagAliasEntityTest'
./gradlew :tagnote-core:test --tests '*SubjectRefTest'
./gradlew :tagnote-core:test --tests '*TaxonomyJpaRepositoryTest'
```

최종 필수 검증:

```bash
./gradlew test
./gradlew check
./scripts/verify.sh
git diff --check
```

추가 리뷰:

- `tag`, `tag_alias` Entity annotation과 `init_schema.sql`의 PK/FK/unique/index 대조
- SQL 로그로 approved alias bulk lookup의 query 수와 N+1 부재 확인
- `TagMatchingService`에 Repository/JPA 의존이 들어가지 않았는지 확인
- `SubjectRef` 생성 과정에 Repository 조회나 Spotify ID 의존이 없는지 확인
- Observation/Assertion/Resolved/Resolver 관련 production file이 diff에 포함되지 않았는지 확인
- 기존 Catalog Entity 및 API/Swagger diff가 없는지 확인
- `git status --short`로 사용자 기존 변경과 이번 milestone 변경을 구분하여 검토
