# TAG-CORE-003 — Resolver & Resolved Projection

## Goal

`TAG-CORE-002`에서 생성된 approved direct assertions를 내부 정책에 따라 결정적으로 계산하고, 사용자 조회용 read model인 `SubjectTagResolved`에 멱등 반영하는 Resolver 기반을 구현한다.

```text
Approved direct assertions
+ Tag canonical state
+ manual resolved state
→ TagResolver
→ ResolvedTagCandidate
→ SubjectTagResolved diff/upsert/delete
```

이번 마일스톤은 Track/Album 각각의 직접 evidence만 계산한다. Album → Track inheritance는 `TAG-CORE-004`로 분리한다.

## Context

- Observation은 외부 원본이고 Assertion은 evidence의 source of truth다.
- `SubjectTagResolved`는 Aggregate나 source of truth가 아니라 다시 계산 가능한 projection이다.
- 사용자 화면은 향후 Assertion이 아니라 Resolved projection만 조회해야 한다.
- Resolver는 외부 API를 호출하지 않고 내부 approved assertions와 taxonomy 상태만 입력으로 사용한다.
- `TAG-CORE-001`의 Tag 상태와 `TAG-CORE-002`의 Assertion/SubjectRef를 재사용한다.
- 같은 입력으로 Resolver를 반복 실행해도 row 중복이나 score drift가 없어야 한다.

## Target State

```text
SubjectRef
  ↓
approved direct assertions bulk 조회
  ↓
MERGED tag canonical ACTIVE tag 변환
  ↓
tag별 max(confidence)
  ↓
minimum score 필터
  ↓
manual state와 병합
  ↓
AUTO projection diff
  ├─ insert
  ├─ update
  └─ obsolete AUTO delete
  ↓
SubjectTagResolved
```

- 계산은 pure domain logic과 persistence orchestration으로 분리한다.
- `MANUAL_FIXED`와 `HIDDEN`은 자동 계산으로 변경하거나 삭제하지 않는다.
- MERGED Tag 자체는 projection에 저장하지 않는다.

## Scope

### In Scope

- `SubjectTagResolved` JPA/Domain 모델
- `ResolvedStatus`: `ACTIVE`, `HIDDEN`, `MANUAL_FIXED`
- `ResolutionReason`: `AUTO`, `ADMIN_APPROVED`, `INHERITED_FROM_ALBUM`
- `ResolvedTagCandidate`
- `CanonicalTagService`
- direct assertion 기반 `TagResolver`
- 동일 Tag assertion의 `max(confidence)` 정책
- 전용 Resolver 설정과 type-safe properties
- minimum score 적용
- 기존 manual resolved 상태 병합
- 기존 AUTO projection과 새 계산 결과의 diff/upsert/delete
- Resolver Application transaction 경계
- unique 기반 동시 실행 복구
- Resolver unit/JPA/Application/concurrency test

### Resolver Policy

- `APPROVED` assertion만 입력으로 사용한다.
- 이번 단계에서는 `inherited_from_assertion_id == null`인 direct assertion만 계산한다.
- Assertion의 Tag가 `ACTIVE`면 그대로 사용한다.
- `MERGED`면 최종 ACTIVE canonical Tag까지 따라간다.
- `CANDIDATE`, `DEPRECATED`, 유효한 canonical target이 없는 MERGED Tag는 자동 노출하지 않는다.
- canonical chain cycle을 감지하면 실패시키고 잘못된 projection을 commit하지 않는다.
- canonical Tag가 같은 여러 assertion은 `max(confidence)`를 사용한다.
- score가 설정된 minimum 미만이면 AUTO candidate에서 제외한다.
- direct automatic 결과의 reason은 `AUTO`다.
- `MANUAL_FIXED`, `HIDDEN` 기존 row는 자동 resolver가 덮어쓰거나 삭제하지 않는다.
- 새 계산에서 사라진 기존 ACTIVE/AUTO row는 삭제한다.

## Do Not Touch

- Album → Track inherited assertion과 0.85 상속
- direct Track evidence와 inherited evidence 우선순위 병합
- MusicBrainz/Discogs HTTP와 External Enrichment orchestration
- Observation/Assertion schema 또는 matching 정책 변경
- Alias approve/rematch와 Admin API
- `TagParent`, taxonomy parent 전파, association/fusion
- Track detail API와 Swagger의 resolved tag 노출
- `subject_enrichment_status`, Scheduler/Batch
- 캐시, Kafka, 분산 락
- 기존 Catalog/Search/Board/UserTag contract (`USER-TAG-001`과 별도 milestone)

## 변경/생성 파일

### 변경 파일

- `tagnote-core/src/main/resources/db/init_schema.sql`
  - `subject_tag_resolved` 테이블과 FK/unique/index 추가
- `tagnote-core/src/main/resources/application.yml`
  - Spring profile include에 `tag` 추가
- `agents/server/progress.md`
  - 구현 및 검증 완료 시 갱신
- `agents/server/plans/active/TAG-CORE-003.md`
  - 완료 시 `plans/completed/`로 이동

### 생성 — Configuration

- `tagnote-core/src/main/resources/application-tag.yml`
- `tagnote-core/src/main/java/com/tagnote/application/resolution/config/TagResolutionProperties.java`

전용 설정 파일:

```yaml
tag:
  resolution:
    album-to-track-inheritance-weight: 0.85
    minimum-score: 0.50
```

`album-to-track-inheritance-weight`는 `TAG-CORE-004`에서 사용하지만 Resolver 정책 설정의 단일 위치를 확정하기 위해 같은 전용 파일에 둔다. 숫자를 Domain Service에 하드코딩하지 않는다.

### 생성 — Domain Resolution

- `tagnote-core/src/main/java/com/tagnote/domain/resolution/SubjectTagResolvedEntity.java`
- `ResolvedStatus.java`
- `ResolutionReason.java`
- `ResolvedTagCandidate.java`
- `TagResolver.java`
- `CanonicalTagService.java`
- canonicalization/result model에 필요한 최소 Value Object

### 생성 — Application

- `tagnote-core/src/main/java/com/tagnote/application/resolution/TagResolutionService.java`
- `tagnote-core/src/main/java/com/tagnote/application/resolution/TagResolutionWriteService.java`
- `tagnote-core/src/main/java/com/tagnote/application/resolution/model/ResolvedTagResult.java`

### 생성 — Infrastructure Persistence

- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/resolution/SubjectTagResolvedJpaRepository.java`

기존 `TagJpaRepository`, `TagAssertionJpaRepository`에는 Resolver 입력을 한 번에 읽기 위한 최소 bulk query만 추가한다.

### 생성 — Test

- `CanonicalTagServiceTest`
- `TagResolverTest`
- `SubjectTagResolvedEntityTest`
- `TagResolutionServiceTest`
- `ResolutionJpaTestConfiguration`
- `SubjectTagResolvedJpaRepositoryTest`
- `TagResolutionConcurrencyTest`

## 데이터 흐름

```text
1. TagResolutionService.resolve(SubjectRef)
2. transaction 밖 orchestration에서 제한적 재시도 경계 설정
3. TagResolutionWriteService의 새 transaction 시작
4. Subject Catalog Entity 한 번 조회 및 검증
5. Subject의 APPROVED direct assertions와 Tag 상태 bulk 조회
6. 기존 resolved rows와 Tag를 bulk 조회
7. CanonicalTagService가 ACTIVE canonical Tag로 변환
8. TagResolver가 tag별 max score와 reason 계산
9. minimum score 미만 제거
10. MANUAL_FIXED/HIDDEN key를 자동 결과에서 제외
11. 기존 AUTO와 새 AUTO 결과 diff
12. 신규 insert, 변경 update, obsolete AUTO delete
13. flush 및 commit
14. transaction 밖에서 read model 반환
```

Resolver는 Repository, EntityManager, HTTP Client를 직접 호출하지 않는다. Application Service가 내부 데이터로 Resolver input을 조립한다.

## DB/JPA 설계

### subject_tag_resolved

| Column | Constraint / Meaning |
|---|---|
| `resolved_id` | bigint identity PK |
| `subject_type`, `subject_id` | SubjectRef scalar 값, not null |
| `tag_id` | canonical Tag FK, not null |
| `score` | 0.0~1.0, not null |
| `status` | ResolvedStatus, not null |
| `resolution_reason` | ResolutionReason, not null |
| `last_resolved_at` | not null |

제약:

```text
UNIQUE(subject_type, subject_id, tag_id)
INDEX(subject_type, subject_id, score)
FK(tag_id → tag.tag_id)
```

### JPA 관계와 조회

- Resolved → Tag는 단방향 `ManyToOne(LAZY)`다.
- Tag와 Subject에 역방향 collection을 두지 않는다.
- `subject_type + subject_id`는 polymorphic scalar이며 직접 FK를 만들지 않는다.
- assertions와 Tag는 fetch join/projection으로 고정된 query에 적재한다.
- 기존 resolved와 Tag도 한 번에 적재한다.
- response/result mapping이 transaction 밖에서도 가능하도록 필요한 값을 Application model로 변환한다.
- subject의 assertion 또는 resolved row 수에 비례하는 SELECT를 허용하지 않는다.

## Transaction

```text
[TagResolutionService]
재시도 조율
→ @Transactional 없음

[TagResolutionWriteService.resolve]
Subject 검증
+ assertion/tag 조회
+ pure Resolver 계산
+ resolved diff/upsert/delete
→ 하나의 짧은 transaction
```

- 외부 HTTP 호출은 존재하지 않는다.
- 별도 Bean으로 transaction proxy를 보장한다.
- unique 충돌은 write transaction 전체를 rollback한다.
- 실패한 transaction 안에서 재조회/복구하지 않는다.
- rollback 후 orchestration service가 resolve를 한 번 재시도한다.
- manual row는 자동 diff/delete 대상 query에서 명시적으로 제외하거나 병합 시 보존한다.
- flush를 transaction 경계 안에서 수행한다.

## 기존 API 영향

- Controller와 Swagger는 변경하지 않는다.
- Resolved projection은 아직 Track detail API에 노출하지 않는다.
- Search/Detail/Ranking/Import/Board/UserTag contract를 유지한다.
- Assertion을 사용자 API에서 직접 조회하지 않는다.

## 향후 연결 구조

- `TAG-CORE-004`는 Album approved assertion으로 inherited assertion을 만든 뒤 동일 Resolver를 재사용한다.
- External Enrichment는 Observation/Assertion 저장 commit 후 별도 resolution 호출 경계를 재사용한다.
- Track detail API는 후속 API 마일스톤에서 `SubjectTagResolved`와 Tag만 읽는다.
- Alias rematch는 영향받은 Subject에 동일 `TagResolutionService.resolve`를 호출한다.

## 테스트 계획

### Domain Unit Test

- 동일 Tag 여러 assertion에서 max confidence
- minimum score 경계값 포함/제외
- ACTIVE Tag 직접 사용
- MERGED chain의 최종 ACTIVE canonical 변환
- canonical chain cycle 탐지
- CANDIDATE/DEPRECATED/invalid MERGED 제외
- 입력 순서와 무관한 결정적 결과
- 동일 입력 반복 계산 시 score drift 없음

### Application Test

- approved direct assertion만 입력 사용
- pending/rejected/inherited assertion 제외
- 신규 AUTO insert
- score/reason 변경 update
- obsolete AUTO delete
- MANUAL_FIXED/HIDDEN 보존
- manual key와 같은 AUTO candidate 미적용
- 결과 mapping이 transaction 밖에서 가능

### JPA/Concurrency Test

- resolved FK/unique/index 실제 동작
- 동일 resolve 반복 시 row 수 불변
- 같은 Subject 동시 resolve 후 중복 row 없음
- unique conflict rollback 후 제한적 재시도
- assertion/Tag/resolved 조회 N+1 부재
- 다른 Subject의 projection을 변경하지 않음

## 위험 요소

- **Projection stale data**: upsert만 하고 obsolete AUTO를 지우지 않으면 오래된 태그가 노출된다.
- **Manual state 손실**: bulk delete 범위가 넓으면 HIDDEN/MANUAL_FIXED가 삭제될 수 있다.
- **Canonical cycle**: 잘못된 merge graph에서 무한 순회하지 않도록 방문 집합과 실패 정책이 필요하다.
- **동시성**: 계산 후 insert 사이 경합은 DB unique와 transaction 밖 재시도로 방어한다.
- **설정 누락**: `application-tag.yml` profile include와 type-safe binding을 함께 검증해야 한다.
- **범위 혼합**: inheritance를 같이 구현하면 direct Resolver 자체의 정확성을 독립적으로 리뷰하기 어렵다.

## Acceptance Criteria

- [ ] Resolver는 외부 API와 Repository를 직접 호출하지 않는다.
- [ ] approved direct assertions만 계산 입력이 된다.
- [ ] 동일 canonical Tag는 max confidence로 계산된다.
- [ ] minimum score가 전용 설정 파일에서 적용된다.
- [ ] MERGED Tag는 최종 ACTIVE canonical Tag로 저장된다.
- [ ] invalid/cyclic canonical chain이 잘못된 projection을 commit하지 않는다.
- [ ] 동일 입력 반복 실행에 row 중복과 score drift가 없다.
- [ ] 기존 AUTO의 insert/update/delete diff가 정확하다.
- [ ] MANUAL_FIXED/HIDDEN이 자동 재계산에서 보존된다.
- [ ] resolved FK/unique/index가 schema와 JPA에 일치한다.
- [ ] Resolver 조회에서 N+1이 없다.
- [ ] 외부 HTTP가 transaction 안에 없다.
- [ ] Album inheritance와 API 노출은 포함되지 않는다.
- [ ] 기존 API와 Catalog/Taxonomy/Enrichment 동작이 유지된다.
- [ ] 전체 테스트와 검증이 통과한다.
- [ ] 완료 시 progress 갱신과 Plan 이동이 함께 이루어진다.

## Verification

```bash
./gradlew :tagnote-core:test --tests '*CanonicalTagServiceTest'
./gradlew :tagnote-core:test --tests '*TagResolverTest'
./gradlew :tagnote-core:test --tests '*TagResolutionServiceTest'
./gradlew :tagnote-core:test --tests '*SubjectTagResolvedJpaRepositoryTest'
./gradlew :tagnote-core:test --tests '*TagResolutionConcurrencyTest'
./gradlew test
./gradlew check
./scripts/verify.sh
git diff --check
```

추가로 `application-tag.yml` binding, schema/JPA 대조, AUTO delete 범위, manual 상태 보존, SQL query 수와 API diff 부재를 검토한다.
