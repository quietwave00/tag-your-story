# LIKE-REF-001 — Like 책임 분리와 내 좋아요 게시글 목록

- Status: Completed (Human Review 승인: 2026-10-20, 완료 확인: 2026-10-26)
- Plan 변경 (2026-10-21, 사용자 요청): DB unique는 유지하고 중복 등록 처리를 단순화한다. 중복 Like는 `LIKE_ALREADY_EXISTS`/HTTP 409로 반환하며 재시도나 성공 응답 복구를 하지 않는다.
- Plan 보완 (2026-10-21): Like insert는 `save`를 기본으로 하고, 명시적 flush는 실제 409 오류 변환에 필요함이 통합 테스트에서 확인될 때만 사용한다.
- Milestone: 기존 Like API를 유지하며 Like 쓰기 정합성을 확보하고, 마이페이지용 내 좋아요 게시글 목록을 추가한다.
- 이 Plan 작성 단계에서는 production code를 수정하지 않는다.

## Goal

기존 좋아요 등록·취소·상태 조회 API의 성공 응답과 인증 방식을 유지하면서 Like 유스케이스를 목표 Layered Architecture로 점진적으로 옮긴다. 한 사용자의 한 Board 좋아요 관계를 DB가 유일하게 보장하고, `board.like_count`가 실제 관계 변경에만 따라 움직이도록 한다. 인증 사용자가 마이페이지에서 **내가 좋아요한 글**을 페이지 조회할 수 있게 한다.

본인 게시글 좋아요는 허용한다. 현재처럼 본인 글에는 LIKE 알림을 만들지 않는다.

## Context와 조사 근거

- `AGENTS.md`, `agents/server/conventions.md`, `agents/server/progress.md`
- `agents/server/current_state.md`의 Board/Like 관련 baseline: `LIKE` 정렬, `likeCount`, Board 응답과 보호 동작. 문서에는 Like 등록·취소 자체의 상세 분석이 없으며 현재 코드가 우선이다.
- `agents/server/server_spec.md`의 Board counter, BoardLike, Like 흐름·트랜잭션·unique, API, JPA 조회·권한 정책
- 관련 ADR: `ADR-013-board-catalog-subject.md`(Board Track/Album 대상), `ADR-014-auth-account-identity.md`(기존 user ID), `ADR-015-google-immediate-account-and-canonical-auth.md`(현재 DB role/status 인증), `ADR-016-withdrawn-account-tombstone.md`(Like FK 보존, 탈퇴 작성자 표시)
- 실제 코드: 루트 `tagnote-api`, `tagnote-core`의 Like Controller/AOP/Facade/Service/Entity/Repository, Board Service/Query/Entity/Repository, schema, Notification listener 및 테스트. `tag-note-client/`의 별도 코드 복사본은 대상이 아니다.

명세의 `POST/DELETE /api/boards/{boardId}/likes`와 현재 공개 `POST/DELETE /api/likes`는 다르다. 명세 경로를 추가하고 기존 경로도 유지한다. 명세의 물리 테이블 `board_like`와 현행 `likes`도 다르므로 데이터 보존 migration으로 정렬한다. 현재 Like 테이블에는 `created_at`과 `(user_id, board_id)` unique가 없다.

## AS-IS → TO-BE 분석

| 기준 | AS-IS | TO-BE / 이번 마일스톤 판단 |
| --- | --- | --- |
| Layered Architecture 위반 | Presentation의 `LikeLockAspect`가 쓰기 정책을 수행하고 Domain 패키지의 `LikeFacade`·`LikeService`가 Application 조율과 Spring Data 접근을 섞는다. | Controller는 요청·응답만 담당한다. Application Like command/query 유스케이스가 조율하고, Infrastructure Repository가 영속성을 담당한다. 기존 Board 전체 구조는 옮기지 않는다. |
| Application/Infrastructure 혼재 | `LikeService`가 JPA 저장·삭제, 알림 발행, 조회를 함께 수행한다. | 쓰기 순서·알림 발행은 Application, DB query와 atomic update는 Repository에 둔다. |
| Controller의 과도한 책임 | Controller 본문은 단순하지만 Controller 메서드에 결합된 AOP가 lock 획득을 맡는다. AOP unlock은 서비스 반환 시점이므로 외부 transaction commit보다 빠를 수 있다. | HTTP 메서드와 인증 사용자 전달만 남긴다. 정합성은 DB unique와 트랜잭션으로 보장한다. |
| 기존 구조와 향후 기능 충돌 | 좋아요한 글 목록 API가 없고, Like Entity의 `toLike()`는 Board/User를 서비스 모델로 재생성한다. Board는 이미 Track 또는 Album 하나를 대상으로 한다. | 목록은 Like → Board → 작성자만 읽는다. 클릭 후 기존 Board 상세 API가 Tag·Catalog 정보를 제공한다. Like 목록에서 Tag/Enrichment 경로를 호출하지 않는다. |
| JPA/Repository | `likes`에 unique/index/created_at이 없고 counter를 관계 저장 전에 변경한다. 취소는 관계가 없어도 counter를 감소시킨다. `LikeRepository`에는 미사용·잘못된 시그니처 메서드가 있다. | 관계 변경 성공 시에만 atomic counter update. 단방향 LAZY 참조 및 페이지 조회의 to-one fetch/projection을 사용한다. 사용자·Board 역방향 Likes 컬렉션을 만들지 않는다. |
| Characterization Test | Like HTTP·쓰기 직접 보호 테스트는 없다. Board 테스트는 `LIKE` 정렬과 counter 위임 일부만 보호한다. | 전환 전에 현행 정상 등록/취소/상태, 인증, 본인 글 알림 생략, 응답 envelope를 특성화 테스트로 고정한다. |
| Public API 위험 | 기존 `/api/likes` 본문 `boardId`, `/api/likes/status/{boardId}`, `ApiResult` 구조를 바꾸면 클라이언트가 깨진다. 동시 lock 오류와 중복 요청의 실패는 안정된 성공 계약이 아니다. | 기존 경로·DTO·정상 등록 응답 유지. 명세 경로와 내 목록은 추가한다. 중복 등록은 `LIKE_ALREADY_EXISTS`/HTTP 409, 없는 Like의 재취소는 변경 없는 성공으로 정의한다. 오류 체계 변경을 테스트·Swagger에 명시한다. |
| 최소 Enabling Refactor | 현재 Board 조회 조합은 User/System Tag까지 가져와 목록 요구보다 무겁다. | 전용 Like 목록 row/DTO를 두고 `boardId`, `author`, `content`만 조회한다. 기존 `BoardResponse`·Tag assembler는 사용하지 않는다. |
| 관계없는 cleanup | Board/Comment/File 레거시, 전체 Notification, Track/Album/Tag 구조에도 개선 여지가 있다. | 이 Plan에서 건드리지 않는다. Like 전환으로 참조가 사라지는 코드만 제거한다. |

## Target State

```text
Presentation: LikeController implements LikeApi
  ├─ POST/DELETE /api/likes (기존 body boardId)
  ├─ GET /api/likes/status/{boardId} (기존)
  ├─ POST/DELETE /api/boards/{boardId}/likes (명세 경로)
  └─ GET /api/users/me/liked-boards?page=0 (마이페이지)
             ↓
Application: LikeCommandService / LikeQueryService
             ↓
Infrastructure: Like JPA query·write + Board counter atomic update
             ↓
Domain: Like 관계, Board, User의 기존 단방향 관계
```

마이페이지 목록 항목은 정확히 `boardId`, `author`, `content` 세 필드다. `author`는 게시글 작성자의 표시 이름(`getDisplayNickname` 정책)이며 탈퇴 사용자는 ADR-016의 `탈퇴한 사용자`로 표시한다. 응답은 기존 `ApiResult` envelope 안에 `items`와 `totalCount`를 둔다. `page`는 0부터 시작하고 크기는 기존 Board 목록과 같은 8이다. `created_at DESC, like_id DESC`로 정렬하며 `POST` Board만 포함한다. 빈 목록은 빈 `items`와 `totalCount=0`이다. Track/Album 제목·ID, 태그, likeCount, 알림 정보는 목록 응답에 넣지 않는다.

상세 화면은 항목의 `boardId`로 기존 `GET /api/boards?boardId=...`를 호출한다. 해당 상세 API 및 DTO는 변경하지 않는다.

## Scope

1. 전환 전 Like HTTP/서비스 Characterization Test 작성.
2. Like 등록·취소·상태 조회를 Application 유스케이스로 전환. 새 명세 경로는 같은 유스케이스로 연결.
3. 중복 Like 방지, 실제 삭제 건수에 따른 counter 변경, DB 제약과 migration.
4. 마이페이지 내 좋아요 목록의 전용 query, 최소 DTO, Swagger 및 테스트.
5. Like 전환으로 실제 사용처가 없어진 AOP/Facade/Service·미사용 Repository 메서드만 정리.

## Do Not Touch

- Board CRUD·공개 목록·상세의 경로, DTO, `LIKE` 정렬과 page size
- Tag resolver, User/System Tag, Catalog import, External Enrichment
- Notification 전송·비동기 listener 구조 자체와 Comment/File 흐름
- 프론트엔드 및 `tag-note-client/` 복사본
- Like 목록을 위해 Board title 또는 새 Board 컬럼 추가
- 범위 밖 패키지 이동, 공용 프레임워크·불필요한 port/adapter 추상화

## 변경/생성 파일

정확한 클래스명은 구현 시작 시 기존 참조 검색으로 확정하되 변경 책임은 다음으로 제한한다.

| 파일/영역 | 계획 |
| --- | --- |
| `tagnote-api/.../like/LikeController.java`, 신규 `LikeApi.java`, 전용 목록 response DTO | 기존 경로 유지, 명세 경로·내 목록 추가, Swagger의 인증·파라미터·성공/오류/필드 제약 명시 |
| `tagnote-core/.../application/like/` | 쓰기 트랜잭션 조율, 상태 조회와 내 목록 query |
| `tagnote-core/.../domain/like/LikeEntity.java`, `.../infrastructure/persistence/like/` | 단방향 LAZY 매핑, 존재/삭제 건수/페이지 조회, DB 제약 대응 |
| `tagnote-core/.../infrastructure/persistence/board/BoardJpaRepository.java` | 현재 atomic counter query 재사용 또는 필요한 결과 건수 확인만 최소 보완 |
| `tagnote-core/src/main/resources/db/init_schema.sql`, 신규 `agents/server/migrations/LIKE-REF-001.md` | 기준 DDL 정렬, 운영 데이터 preflight·migration·검증·복구 절차 |
| 기존 `LikeFacade`·`LikeService`·`LikeLockAspect` 및 관련 테스트 | 사용처가 없어지는 범위에서 제거/대체 |
| `agents/server/server_spec.md` | 기존 경로 호환, 본인 글 허용, 내 목록 API와 응답 계약 정렬 |
| API·Application·JPA 관련 테스트 | 기존 계약, 정합성, 목록, 쿼리 수 검증 |

## 데이터 흐름

### 등록

인증 사용자 ID + 요청 `boardId` → ACTIVE USER 확인 → `POST` Board 확인 → `(user_id, board_id)` insert → insert 성공 시 `like_count + 1` atomic update → 본인 글이 아닌 경우 기존 LIKE 알림 이벤트 발행 → 기존 Boolean 성공 응답. 사전 중복 SELECT는 하지 않는다. 알려진 `(user_id, board_id)` unique 충돌은 `LIKE_ALREADY_EXISTS`/HTTP 409로 반환하고 관계·counter·알림을 변경하지 않는다.

### 취소

인증 사용자 ID + 요청 `boardId` → ACTIVE USER 확인 → 해당 관계 delete의 영향 행 수 확인 → 1행 삭제 시에만 `like_count - 1` atomic update → 기존 빈 성공 응답. 관계가 없으면 counter를 건드리지 않는다. soft deleted Board에 남은 Like도 관계 삭제가 가능해야 한다.

### 상태·마이페이지 조회

기존 상태 API는 `(user_id, board_id)` 존재 여부만 조회하고, 없는 Board에 대한 현재 `false` 동작을 유지한다. 마이페이지는 인증 user ID로 Like와 `POST` Board, 작성자를 페이지 조회한다. UserTag/System Tag/Catalog 조회는 수행하지 않는다. 요청에 임의 user ID를 받지 않는다.

## DB/JPA 설계

- 명세의 `board_like(like_id, user_id, board_id, created_at)`에 맞춰 기존 `likes` 데이터를 보존하며 물리 테이블을 migration한다. 새 relation 저장소를 병행 운영하거나 이중 쓰기하지 않는다.
- PK 및 User/Board FK, `UNIQUE(user_id, board_id)`, `INDEX(user_id, created_at)`, `INDEX(board_id, created_at)`를 기준 DDL과 운영 DB에 적용한다.
- preflight에서 중복 쌍, orphan FK, `board.like_count`와 실제 관계 수 차이를 보고한다. 자동으로 임의 중복 행을 지우거나 counter를 덮어쓰지 않는다. 충돌 해결·재계수 방식을 운영 migration 문서에 명시하고 확인 후 제약을 적용한다.
- 현행 Like에는 `created_at`이 없어 과거 좋아요 시각을 복원할 수 없다. backfill 시점을 기록하고 동률은 `like_id DESC`로 안정 정렬한다.
- 목록은 page content와 count에 필요한 query만 수행한다. 작성자 조회는 page query의 to-one fetch 또는 DTO projection으로 해결하고, Board 수만큼 추가 SELECT를 발생시키지 않는다. 큰 컬렉션 fetch join과 Entity 직접 API 반환은 금지한다.

## Transaction

- 쓰기 transaction은 Application Like command에서 시작해 Like 관계 변경과 Board atomic counter update까지 포함한다. Like insert는 `save`를 기본으로 한다. 현재 `GenerationType.IDENTITY`의 실제 insert/오류 시점을 통합 테스트로 확인하고, 알려진 unique 충돌을 409로 변환하는 데 꼭 필요한 경우에만 명시적 flush를 사용한다. unique 충돌이 발생하면 전체 쓰기를 롤백하므로 `like_count`는 증가하지 않는다.
- 사용자 상태 검사는 현재 `ActiveUserWriteGuard`의 DB 기준을 따른다. 사용자 탈퇴와 Like 쓰기의 경계를 유지한다.
- Like 중복 판정을 위해 Redis lock, 사용자 row lock 추가 획득, 사전 `exists` SELECT, 재조회·재시도 계층을 만들지 않는다. 기존 `ActiveUserWriteGuard`의 사용자 row lock은 탈퇴와 쓰기 경계를 위해 그대로 사용하되 Like 중복 제어 수단으로 취급하지 않는다.
- insert에서 알려진 `(user_id, board_id)` unique 충돌만 기존 `CustomException`/`custom_error_code` 형식의 `LIKE_ALREADY_EXISTS`(HTTP 409)로 변환한다. 다른 제약·DB 오류는 중복으로 가장하지 않는다. counter update 실패도 전체 쓰기를 롤백한다.
- 기존 LIKE 알림은 성공한 신규 Like에 대해서만 발행한다. 비동기 listener의 기존 commit 후 동작을 유지하며 외부 HTTP 호출을 transaction에 넣지 않는다.
- 상태·목록은 read-only transaction으로 처리한다.

## 테스트 계획

1. 구현 변경 전에 기존 `/api/likes` 등록·취소·상태의 method/body/auth/`ApiResult`, 본인 글 알림 생략, 없는 Board 상태 조회를 Characterization Test로 고정한다.
2. 신규 경로와 기존 경로가 같은 쓰기 유스케이스를 호출하는지, 중복 등록이 `LIKE_ALREADY_EXISTS`/409이고 재취소가 성공인지, 두 경우 모두 counter/알림을 중복 변경하지 않는지 검증한다.
3. 동시 요청과 강제 실패에서 unique, 관계 수, counter 및 rollback을 JPA 통합 테스트로 검증한다. 두 동시 등록 중 한 건만 성공하고 다른 건은 알려진 unique 충돌로 409가 되며 `like_count`는 한 번만 증가하는지, 다른 제약 위반은 중복 오류로 변환하지 않는지 검증한다. `save`만으로 충돌 변환 시점을 확보할 수 있는지도 확인한다.
4. 내 목록의 사용자 격리, `POST` 필터, 탈퇴 작성자 표시, 정렬·동률·0-based 페이지·`totalCount`·빈 목록을 검증한다.
5. 목록 항목에 세 필드만 있는지, 태그·Catalog 조회가 없는지, 페이지 크기에 따라 SQL 실행 수가 증가하지 않는지 검증한다.
6. migration preflight/후검증 SQL과 기준 DDL을 리뷰하고, 기존 Board `LIKE` 정렬 회귀를 확인한다.

## 위험 요소와 대응

- 기존 중복 Like 또는 counter drift가 unique 추가를 막을 수 있다: migration preflight에서 중단하고 데이터 해결 후 재실행한다.
- 현재 Redis AOP lock을 제거하면 경쟁 요청의 `LOCKED_RESOURCE` 오류가 사라진다: 중복은 `LIKE_ALREADY_EXISTS`/409로 명확히 반환한다. 클라이언트는 이를 무조건 재시도할 장애로 취급하지 않고 좋아요 상태를 갱신한다. 이 오류 동작 변경은 Swagger/회귀 테스트에 명시한다.
- `LikeEntity.createLike`와 `Board.toEntity()`의 Entity 재생성이 현행 JPA 상태와 충돌할 수 있다: 관리 Entity 또는 필요한 FK 참조를 쓰고 불필요한 변환을 제거한다.
- 기존 Like 시각이 없으므로 과거 데이터의 최근순은 정확한 실제 시각이 아니다: backfill 및 동률 규칙을 문서화한다.
- 탈퇴 사용자 Like FK 및 작성자 표시가 깨질 수 있다: ADR-016 통합 테스트로 확인한다.

## Acceptance Criteria

- 기존 세 Like API의 경로, 요청 DTO, 정상 응답 envelope, 인증 및 본인 글 알림 생략이 유지된다.
- 명세의 Board 하위 좋아요 경로와 마이페이지 목록 API가 추가되고 Swagger interface가 실제 Controller/DTO와 일치한다.
- 내 목록 항목은 `boardId`, `author`, `content`만 포함하며 인증 사용자 자신의 공개 Board만 반환한다. 상세 이동은 기존 Board 상세 API를 이용한다.
- 중복 등록은 `LIKE_ALREADY_EXISTS`/409를 반환하며 자동 재시도를 권하지 않는다. 취소 재요청은 기존 성공 형식을 유지한다. 중복·동시 요청 후에도 `(user_id, board_id)` 중복이 없고 관계 수와 `like_count`가 일치한다.
- 좋아요 쓰기에는 하나의 적절한 transaction이 사용되고, 목록에는 게시글 수에 비례하는 SELECT와 불필요한 Tag/Catalog query가 없다.
- migration의 preflight·데이터 보존·제약·후검증이 리뷰되고 관련 테스트, diff 리뷰, Human Review가 완료된다.

## Verification

Codex는 코드 읽기와 `git diff --check` 등 정적 검사만 수행한다. 구현 후 사용자가 다음을 실행하고 결과를 전달한다.

```bash
./gradlew test
./gradlew check
./scripts/verify.sh
```

명령 결과와 Acceptance Criteria 검토가 확인되기 전에는 이 Plan을 completed로 옮기지 않는다. 완료 시 `agents/server/progress.md`를 갱신한다.

## Completion record (2026-10-26)

- 기존 Like API 호환, Board 하위 좋아요 경로, 내 좋아요 목록, DB unique·counter 정합성, Swagger, migration runbook과 관련 테스트 코드를 구현했다.
- 최초 사용자 테스트에서 기존 상태 응답의 JSON 필드가 `isLiked`가 아닌 `liked`임을 확인했고 테스트·Swagger·서버 명세를 정정했다.
- 2026-10-26 사용자가 수정 후 확인 완료와 Plan 완료 처리를 명시적으로 요청했다. 이를 Acceptance Criteria 검토 및 Human Review 완료 확인으로 기록한다.
- `./gradlew test`, `./gradlew check`, `./scripts/verify.sh` 각각의 최종 실행 원문과 운영 DB migration 적용 결과는 전달되지 않았으므로 개별 실행 성공 또는 운영 적용 완료로 기록하지 않는다.
- 구현 범위의 정적 `git diff --check`는 Codex가 확인했다. 운영 migration은 별도 runbook에 따른다.
