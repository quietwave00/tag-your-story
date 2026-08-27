# TAG-CORE-004 — Album-to-Track Inheritance

## Goal

Album의 approved explicit assertions를 현재 Track에 lineage가 보존되는 inherited assertions로 전파하고, Track direct evidence 우선 정책과 `0.85` 상속 가중치를 적용하여 최종 `SubjectTagResolved`를 멱등 계산한다.

```text
Album approved assertions
→ Track inherited assertions (album score × 0.85)
+ Track direct assertions
→ direct 우선 Resolver
→ Track SubjectTagResolved
```

이번 마일스톤은 하나의 Track과 그 Track이 참조하는 Album 범위만 처리한다. Album 전체 수록곡 fan-out, 외부 Provider 연동, 사용자 API 노출은 포함하지 않는다.

## Context

- Catalog의 `TrackEntity`는 하나의 `AlbumEntity`를 `ManyToOne(LAZY)`로 참조한다.
- Artist/Album/Track은 독립 Aggregate이며 Artist credit은 ADR-001의 연결 Entity 구조를 유지한다.
- `TAG-CORE-002`의 Assertion에는 nullable `inherited_from_assertion_id`가 준비되어 있다.
- `TAG-CORE-003`의 Resolver와 `SubjectTagResolved` projection을 재사용한다.
- 태그 아키텍처는 MVP inheritance 방식으로 inherited assertion을 실제 저장하는 방식을 선택한다.
- 외부 HTTP 호출 동안 DB transaction을 열지 않는 기존 원칙은 그대로 유지한다.

## Target State

```text
TrackEntity 조회
  └─ AlbumEntity internal id
        │
        ▼
Album APPROVED direct assertions
        │
        ▼ × configured 0.85
Track inherited assertions
  inherited_from_assertion_id = Album assertion id
        │
        ├──────── Track APPROVED direct assertions
        ▼
TagResolver
  direct same-tag evidence exists → inherited보다 direct 우선
  otherwise inherited candidate 사용
        │
        ▼
Track SubjectTagResolved
```

- lineage를 통해 어떤 Album assertion에서 상속되었는지 추적할 수 있다.
- 동일 상속 계산을 반복해도 inherited assertion과 resolved row가 증가하지 않는다.
- Album evidence가 사라지거나 승인 상태가 바뀌면 더 이상 유효하지 않은 inherited assertion과 AUTO projection을 정리한다.

## Scope

### In Scope

- `TagInheritanceService`
- Track과 Album identity를 한 번에 조회하는 Catalog query
- Album approved direct assertion 조회
- Album → Track inheritance weight `0.85`
- inherited assertion 생성/갱신/삭제 diff
- `inherited_from_assertion_id` lineage
- Track direct assertion과 inherited assertion Resolver 입력 구성
- direct Track evidence 우선 정책
- inherited-only 결과의 `INHERITED_FROM_ALBUM` reason
- 기존 `application-tag.yml` 설정 재사용
- 하나의 Track 처리에 대한 transaction 경계
- 반복 실행 및 동시 실행 멱등성
- 상속/Resolver/Application/JPA integration test

### Inheritance Policy

- Track이 실제 참조하는 Album의 `APPROVED` direct assertion만 상속한다.
- Album의 inherited assertion은 다시 Track으로 전파하지 않는다.
- inherited confidence는 `album assertion confidence × album-to-track-inheritance-weight`다.
- 계산 결과는 0.0~1.0 범위를 유지한다.
- inherited assertion은 원본 Album assertion ID를 반드시 참조한다.
- 같은 canonical Tag에 Track direct evidence가 있으면 최종 결과는 direct evidence를 사용한다.
- direct evidence가 없을 때만 inherited candidate가 최종 결과가 될 수 있다.
- 같은 Tag의 같은 우선순위 후보는 `max(confidence)`를 사용한다.
- 더 이상 유효하지 않은 자동 inherited assertion은 제거한다.
- Track에 관리자가 직접 만든 assertion을 inheritance cleanup 대상으로 포함하지 않는다.

## Do Not Touch

- Album의 모든 저장된 Track에 대한 일괄 fan-out
- 새 Track import 시 자동 enrichment/inheritance 연결
- MusicBrainz/Discogs HTTP, matching, timeout, retry
- Alias 승인 후 rematch
- Admin API와 Swagger
- Track detail API의 resolved tag 노출
- `subject_enrichment_status`, Scheduler/Batch
- Tag hierarchy parent inheritance
- Artist/Label profile inheritance
- 캐시, Kafka, 분산 락
- Catalog Artist credit 구조와 ADR-001
- 기존 Spotify ID upsert 및 Search/Board/UserTag contract

## 변경/생성 파일

### 변경 파일

- `tagnote-core/src/main/java/com/tagnote/domain/resolution/TagResolver.java`
  - direct와 inherited 후보 우선순위를 명시적으로 계산
- `tagnote-core/src/main/java/com/tagnote/application/resolution/TagResolutionService.java`
  - Track resolution 흐름에서 inheritance write service 조율
- `tagnote-core/src/main/java/com/tagnote/application/resolution/TagResolutionWriteService.java`
  - inherited assertion 반영 후 Track resolver 실행
- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/enrichment/TagAssertionJpaRepository.java`
  - Album direct 및 Track inherited bulk query/diff query 추가
- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/catalog/TrackJpaRepository.java`
  - Track과 Album을 한 번에 적재하는 명시적 query가 필요한 경우 최소 변경
- `tagnote-core/src/main/resources/application-tag.yml`
  - `album-to-track-inheritance-weight: 0.85`가 이미 존재하는지 확인하고 값 유지
- 기존 Resolver/Application/JPA test
  - direct/inherited 정책과 회귀 case 추가
- `agents/server/progress.md`
  - 구현 및 검증 완료 시 갱신
- `agents/server/plans/active/TAG-CORE-004.md`
  - 완료 시 `plans/completed/`로 이동

### 생성 — Application/Domain

- `tagnote-core/src/main/java/com/tagnote/application/resolution/TagInheritanceService.java`
- inheritance diff/result 표현에 필요한 최소 model

별도 계층, event bus, scheduler, generic graph abstraction은 생성하지 않는다.

### 생성 — Test

- `tagnote-core/src/test/java/com/tagnote/domain/resolution/TagResolverInheritanceTest.java`
- `tagnote-core/src/test/java/com/tagnote/application/resolution/TagInheritanceServiceTest.java`
- `tagnote-core/src/test/java/com/tagnote/application/resolution/TrackTagResolutionIntegrationTest.java`
- `tagnote-core/src/test/java/com/tagnote/infrastructure/persistence/resolution/TagInheritanceConcurrencyTest.java`

## 데이터 흐름

### Track Resolution

```text
1. TagResolutionService.resolveTrack(trackId)
2. TrackEntity와 AlbumEntity를 한 query로 조회
3. SubjectRef.track(trackId), SubjectRef.album(albumId) 생성
4. Album의 APPROVED direct assertions bulk 조회
5. 기존 Track inherited assertions bulk 조회
6. 원본 Album assertion별 expected inherited assertion 계산
7. 기존 inherited와 diff
   ├─ 신규 insert
   ├─ confidence/source 변경 update
   └─ stale inherited delete
8. Track APPROVED direct + inherited assertions bulk 조회
9. TagResolver 실행
10. direct same-tag 후보가 있으면 inherited 후보 억제
11. resolved AUTO projection diff/upsert/delete
12. flush 및 commit
13. transaction 밖에서 resolved result 반환
```

### Album Resolution

Album 자체의 direct resolution은 `TAG-CORE-003` 흐름을 그대로 사용한다. Album resolved projection을 다시 읽어 상속하지 않고, lineage source인 approved Album assertions로 inherited assertions를 만든다.

## DB/JPA 설계

새 테이블은 추가하지 않는다.

### inherited assertion

기존 `tag_assertion`을 다음과 같이 사용한다.

```text
subject_type = TRACK
subject_id = current track id
tag_id = album assertion tag id
source = album assertion source
evidence_type = album assertion evidence type
confidence = album confidence × configured weight
status = APPROVED
inherited_from_assertion_id = album assertion id
```

- `inherited_from_assertion_id` self FK를 lineage source로 사용한다.
- Track은 하나의 Album만 참조하므로 같은 source/evidence/tag inherited key의 부모 Album은 하나다.
- 기존 assertion unique 제약을 유지한다.
- direct assertion은 `inherited_from_assertion_id IS NULL`, inherited assertion은 `IS NOT NULL`로 구분한다.
- cleanup query는 current Track의 inherited assertion만 대상으로 하며 direct assertion을 삭제하지 않는다.

### JPA 관계와 조회

- Track → Album 관계는 기존 단방향 LAZY mapping을 유지한다.
- Track/Album에 assertion collection을 추가하지 않는다.
- Assertion의 inherited-from self relation은 단방향 LAZY다.
- Track+Album, Album assertions, Track assertions, existing resolved를 각각 고정된 bulk query로 조회한다.
- assertion별 Tag 또는 parent assertion SELECT가 발생하지 않도록 fetch join/projection을 사용한다.
- OSIV가 꺼진 상태에서 transaction 밖 결과 mapping이 가능해야 한다.

## Transaction

```text
[TagResolutionService.resolveTrack]
재시도 조율
→ @Transactional 없음

[TagResolutionWriteService.resolveTrack]
Track/Album 검증
+ inherited assertion diff
+ direct/inherited Resolver 계산
+ resolved projection diff
→ 하나의 짧은 transaction
```

- 외부 HTTP 호출은 없다.
- inherited assertion과 resolved projection이 서로 다른 상태로 commit되지 않도록 같은 transaction에서 처리한다.
- unique 충돌은 전체 transaction rollback 후 바깥 orchestration에서 한 번 재시도한다.
- 실패한 transaction 내부에서 복구하지 않는다.
- 여러 Track을 일괄 처리하거나 Album 전체 fan-out lock을 잡지 않는다.
- 동일 Track 단위의 짧은 transaction만 사용한다.

## 기존 API 영향

- Controller와 Swagger 변경이 없다.
- 기존 Track detail은 계속 Spotify 기반 contract를 유지한다.
- resolved 결과를 아직 사용자 응답에 추가하지 않는다.
- Search/Ranking/Import/Board/UserTag API의 endpoint와 DTO를 변경하지 않는다.
- Track import가 자동으로 inheritance를 실행하도록 연결하지 않는다.

## 향후 Tag / External Enrichment 연결

- External Enrichment가 Album assertions 저장을 완료한 후 현재 Track에 `resolveTrack`을 호출할 수 있다.
- 새 Track import 시 기존 Album evidence를 재사용하는 연결은 후속 orchestration 마일스톤에서 추가한다.
- Album 전체 수록곡 fan-out은 필요성과 query 규모가 확인된 후 Scheduler/Batch 범위로 검토한다.
- Track detail API는 후속 read API 마일스톤에서 `SubjectTagResolved`만 조회한다.

## 테스트 계획

### Domain Unit Test

- Album confidence `0.80 × 0.85 = 0.68`
- 설정값 변경 시 계산값 반영
- Track explicit 0.95와 inherited 0.68이면 0.95 선택
- direct가 없으면 inherited 선택 및 reason 설정
- 동일 Tag direct/inherited 후보 순서가 바뀌어도 같은 결과
- 같은 우선순위에서는 max confidence
- Album inherited assertion의 재상속 금지

### Application Integration Test

- Track의 실제 Album assertion만 상속
- inherited assertion에 원본 Album assertion ID 보존
- 반복 실행 시 inherited/resolved row 증가 없음
- Album assertion confidence 변경 시 inherited score 갱신
- Album assertion reject/delete 시 stale inherited 제거
- stale inherited 제거 후 obsolete AUTO resolved 제거
- direct assertion과 manual resolved 상태 보존
- 다른 Track과 다른 Album 데이터 미변경

### JPA/Concurrency Test

- inherited self FK 실제 동작
- 존재하지 않는 parent assertion 참조 거부
- 동일 Track 동시 resolve 후 inherited/resolved 중복 없음
- shared Album의 서로 다른 Track 동시 처리 독립성
- fixed query count 및 N+1 부재
- transaction rollback 시 inherited와 resolved 모두 원상 복구

### Regression Test

- `TAG-CORE-001` normalization/matching
- `TAG-CORE-002` observation/assertion 멱등성
- `TAG-CORE-003` direct resolver/manual state
- 기존 Catalog/Search/Track/Board/UserTag/API tests

## 위험 요소

- **Lineage 손실**: inherited assertion이 parent ID 없이 저장되면 stale cleanup과 디버깅이 불가능하다.
- **Direct 삭제 사고**: inherited cleanup 조건에 `inherited_from_assertion_id IS NOT NULL`이 빠지면 Track direct evidence가 삭제될 수 있다.
- **Stale inheritance**: Album assertion 상태/score 변경 시 기존 inherited row를 diff하지 않으면 잘못된 score가 남는다.
- **우선순위 오류**: 단순 max만 적용하면 낮은 direct score가 높은 inherited score에 덮일 수 있다. 동일 Tag에서는 direct 존재 자체를 먼저 판정한다.
- **Canonical merge**: direct와 inherited가 다른 legacy Tag ID여도 canonical Tag가 같을 수 있으므로 canonicalization 이후 우선순위를 적용한다.
- **동시성**: 같은 Track의 병렬 resolution은 DB unique와 rollback 후 재시도로 방어한다.
- **Scope 확장**: Album 전체 Track fan-out과 import 자동 연결은 이번 마일스톤에 포함하지 않는다.

## Acceptance Criteria

- [ ] 현재 Track이 참조하는 Album의 approved direct assertion만 상속한다.
- [ ] inherited confidence가 전용 설정의 `0.85`를 적용한다.
- [ ] inherited assertion이 원본 Album assertion ID를 보존한다.
- [ ] Album inherited assertion을 다시 Track으로 상속하지 않는다.
- [ ] 같은 canonical Tag에 direct Track evidence가 있으면 inherited보다 우선한다.
- [ ] direct가 없을 때 inherited 결과가 `INHERITED_FROM_ALBUM` reason으로 생성된다.
- [ ] 반복 실행 시 inherited assertion과 resolved row가 증가하지 않는다.
- [ ] 변경/삭제된 Album evidence에 맞춰 stale inherited와 AUTO projection이 정리된다.
- [ ] direct assertion과 MANUAL_FIXED/HIDDEN 결과가 보존된다.
- [ ] inherited assertion과 projection이 하나의 짧은 transaction에서 원자적으로 반영된다.
- [ ] JPA 관계는 단방향 LAZY이며 N+1이 없다.
- [ ] Album 전체 fan-out, 외부 API, 사용자 API 노출이 포함되지 않는다.
- [ ] 기존 API와 Catalog/Tag Core 이전 단계 동작이 유지된다.
- [ ] 전체 테스트와 검증이 통과한다.
- [ ] 완료 시 progress 갱신과 Plan 이동이 함께 이루어진다.

## Verification

```bash
./gradlew :tagnote-core:test --tests '*TagResolverInheritanceTest'
./gradlew :tagnote-core:test --tests '*TagInheritanceServiceTest'
./gradlew :tagnote-core:test --tests '*TrackTagResolutionIntegrationTest'
./gradlew :tagnote-core:test --tests '*TagInheritanceConcurrencyTest'
./gradlew test
./gradlew check
./scripts/verify.sh
git diff --check
```

추가로 inheritance lineage, cleanup query 범위, direct 우선순위, configuration binding, transaction atomicity, SQL query 수와 기존 API diff 부재를 검토한다.
