# TAG-CORE-002 — Observation & Assertion Pipeline

## Goal

`TAG-CORE-001`의 taxonomy, normalizer, matcher, `SubjectRef`를 재사용하여 외부 Provider가 반환한 raw tag를 유실 없이 저장하고, 해석 가능한 observation을 내부 Tag 근거인 assertion으로 변환하는 최소 evidence pipeline을 구현한다.

```text
provider-neutral raw tag input
→ ExternalTagObservation
→ approved TagAlias exact matching
→ MATCHED / NEW
→ MATCHED observation만 TagAssertion 생성
```

이번 마일스톤은 외부 HTTP Client를 구현하지 않는다. 이미 확보된 provider-neutral 입력을 받아 저장·매칭·assertion 생성까지 수행하는 내부 Application 경계를 완성한다.

## Context

- `TAG-CORE-001`에서 `Tag`, `TagAlias`, `TagNameNormalizer`, `TagMatchingService`, `SubjectRef`가 제공된다는 전제다.
- 기존 Catalog의 Track/Album 내부 PK가 subject identity다.
- Spotify ID 기반 Catalog upsert와 외부 Spotify 호출 경계는 변경하지 않는다.
- Observation은 외부 원본, Assertion은 내부 taxonomy 해석 근거다.
- NEW는 raw tag 하나의 미해석 상태이며 Subject 전체의 처리 실패 상태가 아니다.
- MusicBrainz/Discogs 중 성공한 Provider 입력은 다른 Provider 실패와 독립적으로 보존되어야 한다.

## Target State

```text
ExternalTagInput
  source / rawName / externalRef / evidenceType / confidence
        │
        ▼
SubjectRef + normalized name
        │
        ▼
ExternalTagObservation
  ├─ approved alias unique match → MATCHED + matchedTag
  └─ no match / ambiguous       → NEW + matchedTag null
        │
        ▼
MATCHED only
        │
        ▼
TagAssertion(APPROVED)
```

- raw tag는 매칭 여부와 무관하게 저장한다.
- fuzzy match는 하지 않는다.
- exact match된 신뢰 가능한 explicit genre/style evidence만 자동 APPROVED assertion으로 만든다.
- 같은 입력을 반복 처리해도 Observation과 Assertion row가 증가하지 않는다.

## Scope

### In Scope

- `ExternalTagObservation` JPA/Domain 모델
- `ExternalTagSource`: `MUSICBRAINZ`, `DISCOGS`
- `ObservationStatus`: `NEW`, `MATCHED`, `IGNORED`, `PROMOTED`
- Observation 상태와 matched Tag 불변식
- `TagAssertion` JPA/Domain 모델
- `AssertionSource`: `MUSICBRAINZ`, `DISCOGS`, `ADMIN`
- `EvidenceType`: `EXPLICIT_GENRE`, `EXPLICIT_STYLE`
- `AssertionStatus`: `PENDING`, `APPROVED`, `REJECTED`
- Assertion approve/reject 상태 전이
- provider-neutral `ExternalTagInput`
- Subject 존재 검증 후 `SubjectRef` 생성
- Observation 저장, alias bulk matching, assertion 생성 orchestration
- DB unique 제약 기반 멱등성
- unique 충돌 rollback 후 제한적 재시도
- Domain/JPA/Application/concurrency test

### Matching and Assertion Policy

- `TAG-CORE-001`의 동일 normalizer와 matcher를 재사용한다.
- approved alias가 정확히 한 Tag와 match될 때만 Observation을 `MATCHED`로 전환한다.
- match가 없거나 ambiguous이면 `NEW`로 저장한다.
- `MATCHED`이면 `matched_tag_id`가 필수다.
- `NEW`이면 `matched_tag_id`는 null이다.
- `IGNORED` observation에서는 assertion을 생성하지 않는다.
- exact match된 MusicBrainz/Discogs explicit evidence는 자동 `APPROVED` assertion으로 생성한다.
- raw tag 10건 중 7건만 MATCHED여도 7건의 assertion을 보존하며 3건의 NEW가 처리를 막지 않는다.

## Do Not Touch

- MusicBrainz/Discogs HTTP Client, SDK DTO, entity matching
- 외부 Provider 병렬 호출, timeout, retry, partial response API
- Catalog의 `musicbrainz_id` 갱신
- `SubjectTagResolved`, `TagResolver`, score 계산
- Album → Track inherited assertion
- `MANUAL_FIXED`, `HIDDEN`
- Alias 승인 후 기존 NEW observation rematch
- Admin API와 Swagger
- Scheduler/Batch와 `subject_enrichment_status`
- `TagParent`, `TagAssociation`, merge canonical chain 계산
- Track 상세 API의 System Tag 노출
- 기존 Search/Track/Catalog/Board/UserTag 코드와 contract (`USER-TAG-001`과 별도 milestone)
- Resolver 전용 `application-tag.yml`

## 변경/생성 파일

### 변경 파일

- `tagnote-core/src/main/resources/db/init_schema.sql`
  - `external_tag_observation`, `tag_assertion` 테이블과 FK/unique/index 추가
- Catalog Track/Album Repository
  - Subject Entity를 한 번 조회하기 위한 기존 `findById`로 충분하지 않은 경우에만 최소 query 추가
- `agents/server/progress.md`
  - 구현과 검증 완료 시에만 갱신
- `agents/server/plans/active/TAG-CORE-002.md`
  - 완료 시 `plans/completed/`로 이동

### 생성 — Domain Enrichment

- `tagnote-core/src/main/java/com/tagnote/domain/enrichment/observation/ExternalTagObservationEntity.java`
- `ExternalTagSource.java`
- `ObservationStatus.java`
- `tagnote-core/src/main/java/com/tagnote/domain/enrichment/assertion/TagAssertionEntity.java`
- `AssertionSource.java`
- `EvidenceType.java`
- `AssertionStatus.java`

### 생성 — Application

- `tagnote-core/src/main/java/com/tagnote/application/enrichment/ObservationProcessingService.java`
- `tagnote-core/src/main/java/com/tagnote/application/enrichment/ObservationWriteService.java`
- `tagnote-core/src/main/java/com/tagnote/application/enrichment/model/ExternalTagInput.java`
- `tagnote-core/src/main/java/com/tagnote/application/enrichment/model/ObservationProcessingResult.java`

`ObservationProcessingService`는 transaction 밖에서 입력 정리와 재시도를 조율한다. `ObservationWriteService`는 별도 Spring Bean으로 실제 write transaction을 소유한다.

### 생성 — Infrastructure Persistence

- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/enrichment/ExternalTagObservationJpaRepository.java`
- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/enrichment/TagAssertionJpaRepository.java`

### 생성 — Test

- `ExternalTagObservationEntityTest`
- `TagAssertionEntityTest`
- `ObservationProcessingServiceTest`
- `EnrichmentJpaTestConfiguration`
- `EnrichmentJpaRepositoryTest`
- `ObservationProcessingConcurrencyTest`

## 데이터 흐름

```text
1. caller가 외부 HTTP 완료 후 ExternalTagInput 목록 전달
2. SubjectType에 따라 Track 또는 Album Entity를 한 번 조회
3. 조회된 내부 PK로 SubjectRef 생성
4. rawName normalize
5. observation unique key로 기존 row bulk 조회
6. 없는 observation 생성
7. normalized name 전체에 대한 APPROVED alias bulk 조회
8. TagMatchingService로 unique/unmatched/ambiguous 판정
9. unique match observation만 MATCHED 전이
10. MATCHED observation별 assertion unique key 확인
11. 없는 assertion을 APPROVED로 생성
12. flush 후 commit
13. 생성/재사용/NEW/MATCHED 개수 반환
```

Provider 하나의 실패는 이 service의 입력 목록에 해당 Provider 결과가 없다는 의미로 처리한다. 다른 성공 Provider 입력은 정상 저장한다.

## DB/JPA 설계

### external_tag_observation

| Column | Constraint / Meaning |
|---|---|
| `observation_id` | bigint identity PK |
| `subject_type` | TRACK/ALBUM, not null |
| `subject_id` | Catalog internal PK, not null |
| `source` | MUSICBRAINZ/DISCOGS, not null |
| `raw_name` | 외부 원본, not null |
| `normalized_name` | normalizer 결과, not null |
| `external_ref` | stable provider reference, not null |
| `status` | ObservationStatus, not null |
| `matched_tag_id` | nullable Tag FK |
| `observed_at` | not null |

제약:

```text
UNIQUE(subject_type, subject_id, source, normalized_name, external_ref)
INDEX(subject_type, subject_id)
INDEX(status, normalized_name)
FK(matched_tag_id → tag.tag_id)
```

MySQL unique에서 null이 중복 방지에 참여하지 않으므로 `external_ref`는 blank/null을 허용하지 않는다. Provider별 stable reference 생성 규칙은 External Enrichment 연동 Plan에서 확정한다.

### tag_assertion

| Column | Constraint / Meaning |
|---|---|
| `assertion_id` | bigint identity PK |
| `subject_type`, `subject_id` | SubjectRef scalar 값 |
| `tag_id` | not null Tag FK |
| `source` | AssertionSource, not null |
| `evidence_type` | EvidenceType, not null |
| `confidence` | 0.0~1.0, not null |
| `status` | AssertionStatus, not null |
| `inherited_from_assertion_id` | nullable self FK; 이번 범위에서는 항상 null |
| `created_at` | not null |

제약:

```text
UNIQUE(subject_type, subject_id, tag_id, source, evidence_type)
INDEX(subject_type, subject_id, status)
INDEX(tag_id)
FK(tag_id → tag.tag_id)
FK(inherited_from_assertion_id → tag_assertion.assertion_id)
```

### JPA 관계와 조회

- Observation → matched Tag는 단방향 `ManyToOne(LAZY)`, nullable이다.
- Assertion → Tag와 inherited assertion은 단방향 `ManyToOne(LAZY)`다.
- 역방향 collection, cascade, orphan removal은 사용하지 않는다.
- `subject_type + subject_id`에는 polymorphic FK를 걸지 않는다.
- Subject는 Application 진입 시 Catalog Entity 조회로 검증한다.
- Observation 기존 key, approved alias, assertion 기존 key는 각각 collection bulk query로 조회한다.
- raw tag 또는 alias 수에 비례하는 반복 SELECT를 만들지 않는다.

## Transaction

```text
[외부 호출 — 후속 caller 책임]
MusicBrainz / Discogs HTTP
→ transaction 없음

[ObservationProcessingService]
입력 validation / 재시도 조율
→ @Transactional 없음

[ObservationWriteService.process]
Subject 검증
+ Observation upsert
+ Alias matching
+ Assertion upsert
→ 하나의 짧은 transaction
```

- write service는 별도 Bean으로 분리해 transaction proxy가 적용되게 한다.
- 필요한 `saveAndFlush`/`flush`로 unique 충돌을 write transaction 경계 안에서 발생시킨다.
- 실패한 transaction 안에서 충돌을 catch하고 저장을 계속하지 않는다.
- rollback 후 orchestration service가 동일 provider-neutral 입력으로 전체 write를 한 번만 재시도한다.
- 재시도도 실패하면 예외를 전파한다.

## 기존 API 영향

- Controller와 Swagger 변경이 없다.
- 기존 Search/Detail/Ranking/Import/Board/UserTag contract를 유지한다.
- Observation과 Assertion은 사용자 API에 직접 노출하지 않는다.
- 기존 Track import fast path와 Spotify ID 의미를 변경하지 않는다.

## 향후 연결 구조

- MusicBrainz/Discogs Adapter는 `ExternalTagInput`만 생성해 이 service를 호출한다.
- `TAG-CORE-003`은 APPROVED assertion을 Resolver 입력으로 사용한다.
- `TAG-CORE-004`는 `inherited_from_assertion_id`를 사용해 Album assertion lineage를 Track에 확장한다.
- Alias 승인/rematch는 기존 NEW observation을 재사용하며 외부 API를 재호출하지 않는다.

## 테스트 계획

### Domain Unit Test

- NEW observation은 matched Tag 없이 생성 가능
- MATCHED 전이에는 matched Tag 필수
- IGNORED observation은 자동 assertion 대상이 아님
- raw/normalized name이 분리 보존됨
- blank external reference와 범위 밖 confidence 거부
- Assertion approve/reject 상태 전이

### Application Test

- 존재하는 Track/Album에서 SubjectRef를 생성하고 처리
- 존재하지 않는 Subject 거부
- 7 MATCHED/3 NEW에서 7개 assertion 생성
- 미매칭과 ambiguous observation이 NEW로 보존됨
- 같은 입력 반복 처리 시 row 증가 없음
- matching query가 입력 크기에 비례해 증가하지 않음
- Provider별 입력을 분리 처리해 partial success 보존

### JPA/Concurrency Test

- Observation/Assertion PK/FK/unique/index 실제 동작
- `external_ref` not-null 동작
- 동일 Subject 동시 처리 후 중복 row 없음
- unique 충돌 rollback 후 한 번 재시도
- 재시도 실패 전파
- LAZY 관계와 bulk query에서 N+1 부재

## 위험 요소

- **Polymorphic integrity**: DB FK로 Track/Album을 강제할 수 없어 Application 조회 검증이 필수다.
- **external_ref 규칙**: provider별 안정적인 값이 없으면 idempotency가 깨진다. 임시 랜덤값을 생성하지 않는다.
- **Alias ambiguity**: 복수 approved 후보를 임의로 고르면 잘못된 assertion이 영속화된다. NEW로 fail-closed 한다.
- **Unique 충돌 복구**: rollback-only transaction 내부에서 복구하지 않는다.
- **Source/evidence 의미**: Provider 값을 추측해 다른 evidence type으로 승격하지 않는다.
- **범위 확장**: Resolver를 함께 구현하면 evidence와 serving 검토가 섞이므로 금지한다.

## Acceptance Criteria

- [x] raw tag가 match 여부와 관계없이 Observation에 저장된다.
- [x] MATCHED/NEW 상태와 matched Tag 불변식이 보호된다.
- [x] approved alias unique match만 Assertion으로 이어진다.
- [x] unmatched/ambiguous 값은 NEW로 보존된다.
- [x] NEW가 다른 matched 결과의 성공을 막지 않는다.
- [x] Assertion에 subject, tag, source, evidence type, confidence를 추적할 수 있다.
- [x] Observation과 Assertion unique/FK/index가 schema와 JPA에 일치한다.
- [x] 같은 입력 반복 및 동시 처리에도 중복 row가 없다.
- [x] Subject 검증에 별도 반복 `existsById()`가 없다.
- [x] 외부 HTTP 호출이 write transaction 안에 없다.
- [x] 입력 크기에 비례하는 반복 SELECT와 N+1이 없다.
- [x] Resolver/Resolved/inheritance/API/provider client가 포함되지 않는다.
- [x] 기존 API와 Catalog 동작이 유지된다.
- [x] 전체 테스트와 검증이 통과한다.
- [x] 완료 시 progress 갱신과 Plan 이동이 함께 이루어진다.

## Verification

완료 검증 결과:

- PASS: Domain/Application/JPA/동시성 대상 테스트
- PASS: WSL OpenJDK 17 `./gradlew test`
- PASS: WSL OpenJDK 17 `./gradlew check`
- PASS: `./scripts/verify.sh`
- PASS: TAG-CORE-002 범위 diff 및 공백 검사
- 참고: 전역 `git diff --check`는 이번 범위 밖 기존 CRLF 작업 트리 변경 때문에 실패하며 해당 사용자 변경은 수정하지 않음

```bash
./gradlew :tagnote-core:test --tests '*ExternalTagObservationEntityTest'
./gradlew :tagnote-core:test --tests '*TagAssertionEntityTest'
./gradlew :tagnote-core:test --tests '*ObservationProcessingServiceTest'
./gradlew :tagnote-core:test --tests '*EnrichmentJpaRepositoryTest'
./gradlew :tagnote-core:test --tests '*ObservationProcessingConcurrencyTest'
./gradlew test
./gradlew check
./scripts/verify.sh
git diff --check
```

추가로 schema/JPA 대조, SQL query 수, transaction proxy 경계, 기존 API diff 부재를 검토한다.

## Follow-up — Persistence Conflict Translation

동시성 복구의 물리 DB 제약조건 지식을 Application orchestration에서 제거한다.

- [x] Observation/Assertion duplicate 의미 예외 추가
- [x] Application port와 Infrastructure translator로 DB 제약조건 이름 해석 격리
- [x] write transaction 전체 범위의 무결성 예외를 의미 예외로 번역
- [x] Processing service가 duplicate 의미 예외만 재시도
- [x] retry warn 로그에 Subject와 conflict type 기록
- [x] 알 수 없는 FK/NOT NULL 등 무결성 오류는 원래 예외로 유지
- [x] translator 및 orchestration 테스트 갱신
- [x] 사용자 테스트 검증 완료

사용자 검증 명령:

```bash
./gradlew :tagnote-core:test --tests '*ObservationProcessingServiceTest'
./gradlew :tagnote-core:test --tests '*HibernateEnrichmentConflictTranslatorTest'
./gradlew :tagnote-core:test --tests '*ObservationProcessingConcurrencyTest'
./gradlew test
./gradlew check
```
