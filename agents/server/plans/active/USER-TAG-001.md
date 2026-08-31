# USER-TAG-001 — User-Owned Custom Tag Identity

## Goal

게시글 작성자가 입력하는 개인 커스텀 `UserTag`를 사용자별 identity로 분리하고, 기존 태그명 기반 게시글 조회가 사용자별로 분리된 모든 태그 ID를 안전하게 포함하도록 전환한다.

```text
User A + "1월" → user_tag_id 10
User B + "1월" → user_tag_id 20
User A + "  1월  " 재입력 → user_tag_id 10 재사용
```

```text
공개 게시글 태그 조회 "1월"
→ normalized_name = "1월"인 UserTag 전체
→ user_tag_id 10, 20에 연결된 POST Board 전체
```

System Tag taxonomy와 Resolver는 공용 canonical identity를 유지한다. 이번 마일스톤은 개인 `UserTag`와 `BoardUserTag`만 다룬다.

## Context

- 현재 `UserTagEntity`에는 `userTagId`, `name`만 있고 사용자 소유 FK가 없다.
- 현재 Board 생성은 `findAllByNameIn`으로 다른 사용자가 만든 같은 이름의 UserTag를 전역 재사용한다.
- 현재 태그명 기반 Board 조회는 `findByName`으로 단일 ID를 선택한 뒤 그 ID의 Board만 조회한다.
- 실제 schema에는 `user_tag.name` index만 있어 동일 이름 row가 복수 생성될 수 있지만, 소유자가 없으므로 개인 identity로 해석할 수 없다.
- 현재 `findByName`은 결과를 `Optional` 하나로 기대하므로 동일 이름 row가 복수 존재하면 조회 개수 오류 위험이 있다.
- 문서의 `INDEX(name)`과 `UNIQUE(user_tag.normalized_name)`가 서로 충돌하며, 전역 unique는 사용자별 identity 요구사항과 맞지 않는다.
- `board_user_tag`의 `(board_id, user_tag_id)` unique와 역방향 조회 index는 명세에만 있고 실제 schema/JPA에는 없다.
- ADR-002에 따라 UserTag identity는 `(user_id, normalized_name)`으로 확정한다.

## Target State

### User-owned identity

```text
User
  └─ UserTag(owner, name, normalizedName)
       └─ BoardUserTag ── Board
```

- UserTag는 반드시 한 명의 User가 소유한다.
- 같은 사용자의 같은 normalized name은 하나의 UserTag만 존재한다.
- 다른 사용자의 같은 normalized name은 별도 UserTag와 별도 PK를 가진다.
- BoardUserTag에 연결되는 UserTag owner와 Board writer는 같아야 한다.
- Board와 UserTag에는 불필요한 역방향 컬렉션을 추가하지 않는다.
- UserTag를 Board 연결에서 제거해도 개인 태그 identity는 즉시 삭제하지 않는다.

### Query semantics

```text
개인 identity 조회
(owner user id, normalized name) → unique UserTag

공개 Board 조회
normalized name → 모든 owner의 UserTag → distinct POST Board
```

기존 공개 contract를 유지한다.

```http
GET /api/boards/user-tags?userTagName={name}
Authentication: 불필요
```

- Request/Response DTO와 `ApiResult<List<BoardResponse>>` 구조는 변경하지 않는다.
- 입력 이름을 UserTag normalization한 뒤 조회한다.
- 같은 normalized name을 가진 모든 사용자별 UserTag ID의 게시글을 포함한다.
- 같은 Board가 중복 반환되지 않도록 `distinct`를 보장한다.
- `BoardStatus.POST`만 반환한다.
- 결과가 없으면 기존과 같이 `USER_TAG_NOT_FOUND`를 반환하여 이번 identity 전환에서 API 오류 동작을 변경하지 않는다.

개인 태그 목록, 이름 변경, 삭제 API는 이번 마일스톤에서 추가하지 않는다.

## Scope

### In Scope

- `UserTagEntity`의 단방향 LAZY owner 관계
- UserTag 원본 `name`과 `normalizedName` 분리
- 개인 태그용 `NormalizedUserTagName` Value Object와 `UserTagNameNormalizer`
- `(user_id, normalized_name)` owner-scoped find-or-create
- 동일 요청 안의 normalized name 중복 제거
- DB composite unique 기반 동시 생성 방어와 제한적 복구
- Board 생성 시 인증 User를 UserTag owner로 전달
- Board 수정 시 기존 Board 작성자를 기준으로 UserTag owner 결정
- 다른 사용자의 UserTag를 Board에 연결하지 못하게 하는 불변식/검증
- 기존 태그명 기반 공개 Board 조회를 normalized name join query로 변경
- `board_user_tag` unique/index 및 JPA mapping 정렬
- 기존 전역 공유 UserTag 데이터의 owner별 분리 migration 절차 문서화
- Domain/Application/JPA/API characterization 및 integration test
- 관련 Server Spec과 ADR 정렬

### Normalization Policy

- trim
- lowercase with `Locale.ROOT`
- Unicode NFKC normalization
- 연속 공백 축약
- 의미 있는 punctuation 보존
- normalization 결과가 blank면 거부
- 원본 `name`은 화면 표시를 위해 보존
- 중복 판정과 검색에는 `normalized_name`만 사용

System Tag의 `NormalizedTagName`, `TagNameNormalizer`를 직접 재사용하지 않는다. 현재 규칙이 같더라도 System taxonomy alias 정책과 개인 태그 정책이 독립적으로 변경될 수 있도록 별도 Value Object/정규화 경계를 둔다.

## Do Not Touch

- System Tag `Tag`, `TagAlias`, taxonomy matcher
- `ExternalTagObservation`, `TagAssertion`, `SubjectTagResolved`, Resolver
- Album → Track inheritance
- MusicBrainz/Discogs/Spotify adapter
- Catalog Track/Album/Artist identity
- Board의 Spotify 문자열 `trackId`와 Catalog FK 전환
- 개인 태그 이름 변경/삭제/병합 API
- 개인 태그 추천, 인기 집계, 자동완성 ranking
- Tag subscription과 System Tag 탐색 API
- Board content, Like, Notification의 관련 없는 리팩토링
- 기존 공개 API endpoint 및 response 구조 변경

## 변경/생성 파일

정확한 package와 이름은 기존 충돌을 다시 확인하되 책임과 범위는 다음을 따른다.

### 변경 파일

- `agents/server/server_spec.md`
  - UserTag owner identity, schema, 조회 semantics 정렬
- `tagnote-core/src/main/resources/db/init_schema.sql`
  - `user_tag.user_id`, `normalized_name`, FK/unique/index 추가
  - `board_user_tag` unique/index 추가
- `tagnote-core/src/main/java/com/tagnote/core/domain/usertag/UserTagEntity.java`
- `tagnote-core/src/main/java/com/tagnote/core/domain/usertag/repository/UserTagRepository.java`
- `tagnote-core/src/main/java/com/tagnote/core/domain/usertag/service/UserTagService.java`
- `tagnote-core/src/main/java/com/tagnote/core/domain/usertag/service/UserTag.java`
- `tagnote-core/src/main/java/com/tagnote/core/domain/boardusertag/BoardUserTagEntity.java`
- `tagnote-core/src/main/java/com/tagnote/core/domain/boardusertag/repository/BoardUserTagRepository.java`
- `tagnote-core/src/main/java/com/tagnote/core/domain/boardusertag/service/BoardUserTagService.java`
- `tagnote-core/src/main/java/com/tagnote/core/domain/board/service/BoardFacade.java`
- Board tag-name lookup repository/service
- 영향받는 기존 Board/UserTag tests
- `agents/server/progress.md`
  - 구현과 모든 검증 완료 시에만 갱신
- `agents/server/plans/active/USER-TAG-001.md`
  - 완료 시 `plans/completed/`로 이동

### 생성 파일

- 개인 태그 normalization Value Object/Service와 unit test
- owner-scoped UserTag JPA integration/concurrency test
- UserTag data migration runbook 또는 운영 DDL 문서

새로운 공개 endpoint를 추가하지 않으므로 Swagger contract의 endpoint/DTO는 변경하지 않는다. 기존 Board API용 Swagger interface가 도입되는 경우 해당 interface에 실제 태그명 조회 contract를 문서화하고 Controller에는 Swagger annotation을 두지 않는다.

## Data Flow

### Board create

```text
인증 userId
→ User 조회
→ raw UserTag names normalize + request 내부 distinct
→ (userId, normalized names) bulk 조회
→ 없는 UserTag만 생성
→ Board writer 설정
→ owner가 writer와 같은 UserTag만 BoardUserTag 생성
→ Board + BoardUserTag 저장
```

- 이름별 SELECT를 반복하지 않는다.
- 새 UserTag 저장을 `BoardUserTag`의 cascade persist에 암묵적으로 의존하지 않는다.
- UserTag와 BoardUserTag 저장은 Board 생성 transaction 안에서 원자적으로 처리한다.

### Board update

```text
Board 조회
→ Board writer 확인
→ writer 기준 UserTag bulk find-or-create
→ 기존 BoardUserTag 교체
→ unique owner invariant 검증
```

현재 update authorization 결함을 관련 없이 전면 리팩토링하지 않는다. 다만 잘못된 owner의 UserTag가 연결되지 않도록 조회된 Board writer를 owner 기준으로 반드시 사용한다.

### Public lookup by tag name

```text
raw query name normalize
→ BoardUserTag join UserTag
→ user_tag.normalized_name exact match
→ BoardStatus.POST
→ distinct Board 목록
```

`normalized_name`으로 단일 UserTag ID를 먼저 선택하지 않는다.

## DB/JPA Design

### user_tag

| Column | Constraint / Meaning |
|---|---|
| `user_tag_id` | bigint identity PK |
| `user_id` | owner User FK, not null |
| `name` | 원본 display name, not null |
| `normalized_name` | 중복 판정/검색용 이름, not null |
| `created_at`, `updated_at` | audit timestamp |

```text
PK(user_tag_id)
FK(user_id → users.user_id)
UNIQUE(user_id, normalized_name)
INDEX(normalized_name, user_id)
```

JPA 관계:

```text
UserTagEntity --ManyToOne(LAZY)--> UserEntity
```

- UserEntity에 UserTag collection을 추가하지 않는다.
- cascade와 orphan removal을 사용하지 않는다.

### board_user_tag

```text
PK(board_user_tag_id)
FK(board_id → board.board_id)
FK(user_tag_id → user_tag.user_tag_id)
UNIQUE(board_id, user_tag_id)
INDEX(user_tag_id, board_id)
```

- Board/UserTag 관계는 단방향 LAZY를 유지한다.
- `CascadeType.PERSIST`를 제거하고 Application orchestration이 UserTag 저장 순서를 명시한다.

## Existing Data Migration

기존에는 하나의 UserTag가 여러 작성자의 Board에 연결될 수 있으므로 단순히 `user_id NOT NULL`을 추가할 수 없다.

Migration은 다음 순서를 보장해야 한다.

```text
1. 기존 user_tag.name을 normalization
2. board_user_tag → board.user_id를 기준으로
   distinct (user_id, normalized_name) UserTag row 생성
3. 각 board_user_tag를 해당 Board writer 소유 UserTag로 재연결
4. 중복 (board_id, user_tag_id) 제거
5. owner가 없는 orphan UserTag 처리 내역 기록 후 제거 또는 격리
6. user_id NOT NULL / FK / composite unique 적용
7. row count와 owner mismatch가 0인지 검증
```

- migration 전후 `board_user_tag`가 표현하는 Board별 태그 이름 집합이 같아야 한다.
- display name 충돌 시 같은 사용자의 최초 사용 값을 보존하는 것을 기본으로 하되, 실제 데이터 확인 후 migration 문서에서 확정한다.
- 운영이 `ddl-auto=validate`이므로 `init_schema.sql` 변경만으로 기존 DB가 migration된다고 간주하지 않는다.

## Transaction and Concurrency

- Board create/update Application transaction 안에서 owner-scoped UserTag와 BoardUserTag를 저장한다.
- `(user_id, normalized_name)` unique가 동시 생성의 최종 방어선이다.
- 같은 사용자가 같은 태그를 동시에 처음 생성해 unique 충돌이 발생하면 rollback된 별도 transaction 뒤 기존 row를 제한적으로 재조회한다.
- 서로 다른 사용자의 같은 normalized name은 충돌하지 않아야 한다.
- `(board_id, user_tag_id)` unique로 요청 중복과 동시 연결 중복을 방어한다.
- 불필요한 `existsById()` 또는 이름별 반복 SELECT를 추가하지 않는다.

## Test Plan

### Characterization

- 기존 Board 생성/수정 response의 UserTag 이름 유지
- 기존 `GET /api/boards/user-tags` endpoint, query parameter, response envelope 유지
- 태그명 조회 결과가 없을 때 기존 `USER_TAG_NOT_FOUND` 동작 유지

### Domain/Application

- 다른 사용자의 동일 `"1월"`은 다른 UserTag ID 생성
- 같은 사용자의 `"1월"`, `" １월 "` 등 normalization 동등 입력은 같은 ID 재사용
- 같은 요청의 중복 이름은 하나의 BoardUserTag만 생성
- UserTag owner와 Board writer 불일치 거부
- Board 수정도 원 작성자 소유 UserTag를 사용
- 의미 있는 punctuation 보존
- blank tag 거부

### JPA/Concurrency

- `(user_id, normalized_name)` unique 실제 동작
- 다른 user의 같은 normalized name 저장 성공
- UserTag owner FK 실제 동작
- `(board_id, user_tag_id)` unique 실제 동작
- owner-scoped bulk query 한 번으로 기존 태그 조회
- 같은 user의 동시 최초 생성에서 최종 row 한 건
- 다른 user의 동시 동일 이름 생성에서 user별 row 한 건씩
- 공개 normalized name 조회가 여러 owner의 Board를 모두 반환
- 공개 조회가 `POST`만 반환하고 Board를 중복 반환하지 않음
- 조회에서 Board/UserTag N+1이 발생하지 않음

### Migration Verification

- migration 전후 Board별 display tag name 집합 동일
- owner mismatch 0건
- `(user_id, normalized_name)` 중복 0건
- `(board_id, user_tag_id)` 중복 0건
- FK orphan 0건

## Acceptance Criteria

- [ ] `UserTag`가 owner User FK를 가진다.
- [ ] 같은 사용자의 같은 normalized name은 같은 UserTag ID를 재사용한다.
- [ ] 다른 사용자의 같은 normalized name은 다른 UserTag ID를 가진다.
- [ ] 전역 `UNIQUE(normalized_name)`를 사용하지 않는다.
- [ ] DB가 `UNIQUE(user_id, normalized_name)`으로 중복을 최종 방어한다.
- [ ] UserTag owner와 Board writer가 항상 일치한다.
- [ ] BoardUserTag 중복이 DB unique로 방지된다.
- [ ] 공개 태그명 조회가 같은 이름을 가진 모든 사용자의 POST Board를 반환한다.
- [ ] 공개 조회가 단일 UserTag ID를 임의 선택하지 않는다.
- [ ] 기존 Board tag endpoint와 response contract가 유지된다.
- [ ] 태그 생성과 조회가 normalization 정책을 공유한다.
- [ ] 이름별 반복 SELECT와 N+1이 없다.
- [ ] 기존 전역 공유 데이터의 owner별 migration 절차와 검증이 존재한다.
- [ ] System Tag 모델과 identity 정책을 변경하지 않는다.
- [ ] 개인 태그 관리 API와 관련 없는 리팩토링을 선행하지 않는다.
- [ ] 전체 테스트와 검증 명령이 통과한다.
- [ ] 관련 없는 사용자 변경을 포함하지 않는다.
- [ ] 완료 시 progress 갱신과 Plan 이동이 함께 이루어진다.

## Verification

```bash
./gradlew :tagnote-core:test --tests '*UserTag*'
./gradlew :tagnote-core:test --tests '*BoardFacade*'
./gradlew :tagnote-api:test --tests '*BoardControllerTest'
./gradlew test
./gradlew check
./scripts/verify.sh
git diff --check
```

추가 리뷰:

- Entity annotation과 `init_schema.sql`의 FK/unique/index 대조
- Server Spec의 UserTag schema와 동시성 제약이 같은 owner-scoped 정책인지 확인
- Board 생성/수정에서 인증 User 또는 Board writer가 owner 기준으로 전달되는지 확인
- global name-only UserTag lookup이 production code에 남아 있지 않은지 확인
- 공개 조회가 단일 ID 선택 없이 normalized name join을 사용하는지 확인
- System Tag production file이 diff에 포함되지 않았는지 확인
- migration 전후 데이터 보존 검증 확인
