# COMMENT-REF-001 — Comment 작성 유스케이스 경계 이동

상태: Human Review 승인, 구현 전

## Goal

기존 Comment API의 관찰 가능한 동작을 특성화 테스트로 고정하고, 최상위 댓글 및 답글 작성의 Board·User 조회와 호출 순서 조율을 Application 계층으로 옮긴다. 공개 API와 기존 저장·알림 정책은 유지한다. 이번 Plan은 Comment 전체 구조 교체가 아닌 첫 번째 작은 마일스톤이다.

## 근거 문서와 결정

- `AGENTS.md`, `agents/server/conventions.md`, `agents/server/progress.md`
- `agents/server/current_state.md`: 2026-08-11 baseline은 Board·Search·User·Spotify·Track 범위이며 Comment 전용 기록은 없다.
- `agents/server/server_spec.md`: Layered Architecture 원칙은 정의하지만 Comment는 2차 확장 목록에만 있고 상세 정책은 없다.
- ADR-013: Comment가 참조하는 Board는 Catalog Track 또는 Album 중 정확히 하나를 대상으로 하며, 기존 Comment용 BoardService 경로가 남아 있다.
- ADR-014·015: 사용자 내부 ID와 ACTIVE 인증 상태가 쓰기 경계에 영향을 준다.
- ADR-016: 탈퇴 사용자의 댓글은 공개로 유지하고 작성자명은 `탈퇴한 사용자`로 표시한다.

이번 세션의 Comment 정책은 게시글 댓글 작성·수정·삭제, 댓글 아래 답글 한 단계, 스레드 참여자에 대한 향후 `@` 호출이다. 기존 코드와 충돌하는 정책은 이 호환 리팩토링에서 암묵적으로 적용하지 않는다.

## AS-IS

| 관점 | 실제 구현과 차이 |
| --- | --- |
| 계층 | `CommentController → CommentFacade → CommentService → CommentRepository`. Facade가 Board·User를 조회하고 Service가 Repository, 활성 사용자 잠금, 댓글 알림 발행을 함께 수행한다. Application 패키지의 Comment 유스케이스 경계가 없다. |
| Controller | 8개 endpoint의 요청 변환과 응답 조립을 담당한다. Repository 직접 접근은 없고, 주된 문제는 작성 유스케이스가 legacy Facade로 연결되는 점이다. |
| 구조·정책 | `parent_id`는 있으나 답글의 부모가 최상위 댓글인지, 같은 Board에 속하는지 검증하지 않는다. 수정·삭제는 인증 역할만 확인하며 작성자 소유권은 확인하지 않는다. |
| JPA·조회 | `CommentEntity`의 parent, user, board는 LAZY다. 자식 컬렉션과 `toComment()` 변환을 통한 조회는 N+1 및 불필요한 연관 조회 위험이 있다. 부모와 children의 양방향 관계도 있다. |
| 현재 결과 | 최상위 목록은 `POST` 부모를 ID 내림차순으로 20개 조회하며 `POST` 자식을 함께 반환한다. 답글 목록은 `POST`를 5개씩 조회한다. count는 부모·답글을 모두 포함한 `POST` 개수다. 작성자 댓글 ID 조회는 상태 필터가 없다. |
| 알림 | 최상위 댓글 작성자가 게시글 작성자와 다를 때만 기존 COMMENT 알림 이벤트를 발행한다. 답글에는 해당 발행이 없다. |
| 테스트 | `CommentServiceTest`는 최상위 댓글 작성 한 건만 검증한다. HTTP 계약, 다른 쓰기·읽기 흐름, 알림 및 오류 동작의 특성화가 부족하다. |
| Swagger | Comment 전용 API interface가 없다. 기존 Controller·DTO의 실제 계약을 기준으로 문서화가 필요하다. |

현재 공개 경로는 `POST /api/comments`, `PATCH /api/comments`, `PATCH /api/comments/status/{commentId}`, `GET /api/comments/{boardId}/{page}`, `GET /api/comments/auth/{boardId}`, `GET /api/comments/count/{boardId}`, `POST /api/comments/replies`, `GET /api/comments/replies/{parentId}/{page}`다. 기존 `ApiResult` wrapper, Request·Response 필드와 인증 경계가 기준이다.

## Target State

- 최상위 댓글과 답글 작성은 `CommentCommandService`가 Board와 현재 ACTIVE User 조회를 조율하고 기존 `CommentService`에 저장을 위임한다.
- Controller는 작성 endpoint에서 Application 서비스를 호출하고 기존 Request·Response 변환만 한다.
- 수정·삭제·조회는 이 마일스톤에서 legacy Facade 경로를 유지한다.
- 기존 `CommentService`의 Repository 접근, 트랜잭션, 활성 사용자 잠금, 알림 발행 위치는 이 단계에서 유지한다. 따라서 Infrastructure 책임의 완전 분리는 후속 마일스톤으로 명시한다.
- `CommentApi` interface에 8개 현행 endpoint의 OpenAPI 계약을 기록하고 Controller가 이를 구현한다. Controller에는 Swagger 문서화 어노테이션을 추가하지 않는다.
- 댓글 한 단계 제한, `@` 호출, 소유권 정책 변경은 후속 기능 마일스톤에서 별도로 설계·검증한다. `@` 참여자 판별은 닉네임 문자열 대신 스레드의 사용자 ID를 기준으로 검토한다.

## 변경할 파일과 책임

| 파일 | 책임 |
| --- | --- |
| `tagnote-api/src/main/java/com/tagnote/api/domain/comment/CommentController.java` | 작성·답글 작성 호출을 Application 서비스로 전환하고 `CommentApi`를 구현한다. 나머지 endpoint의 경로와 동작은 유지한다. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/comment/service/CommentFacade.java` | 작성 관련 Board·User 조율을 제거하고 현재 수정·삭제·조회 경로를 유지한다. |
| `tagnote-core/src/test/java/com/tagnote/domain/comment/CommentServiceTest.java` | 기존 작성 테스트를 보강해 저장·상태·알림의 현행 동작을 확인한다. |

## 새로 만들 파일과 책임

| 파일 | 책임 |
| --- | --- |
| `tagnote-core/src/main/java/com/tagnote/application/comment/CommentCommandService.java` | 최상위 댓글·답글 작성에 필요한 Board·User 조회와 기존 저장 서비스 호출을 조율한다. 새 쓰기 트랜잭션을 중복으로 열지 않는다. |
| `tagnote-api/src/main/java/com/tagnote/api/domain/comment/CommentApi.java` | 각 endpoint의 method·path·인증, path parameter, 요청·응답 DTO, 실제 주요 오류와 기존 `error_code`·`custom_error_code` 형식을 문서화한다. DTO 필드의 의미·필수 여부·기존 제약도 실제 구현에 맞춰 기술한다. |
| `tagnote-api/src/test/java/com/tagnote/api/domain/comment/CommentControllerTest.java` | HTTP 라우팅·인증·validation·`ApiResult` JSON 형식을 특성화한다. |
| `tagnote-core/src/test/java/com/tagnote/application/comment/CommentCommandServiceTest.java` | 작성 조율의 호출 순서와 Board/User 조회 오류 전파를 확인한다. |

문서화 중 DTO 필드 주석만으로 부족하면 기존 Comment Request·Response DTO에 Schema 어노테이션을 추가할 수 있다. 이 경우 파일별 변경 사유를 diff 리뷰에 명시하며 필드·validation·serialization은 바꾸지 않는다.

## 호출 흐름

```text
작성 / 답글 작성
CommentController
  → CommentCommandService (Application: Board·ACTIVE User 조회)
  → 기존 CommentService (쓰기 트랜잭션, 잠금, 저장, 현행 알림)
  → CommentRepository (JPA)

수정 / 삭제 / 조회
CommentController
  → 기존 CommentFacade
  → 기존 CommentService
  → CommentRepository
```

## 기존 API 영향

공개 endpoint, HTTP method, role 요구, path·request·response DTO, `ApiResult`, 페이징 크기, 기존 예외와 상태별 조회 결과를 변경하지 않는다. `CommentResponse.createdDate`를 포함한 실제 변환 결과를 테스트로 확인하고, 발견한 결함을 이 리팩토링에 섞어 수정하지 않는다. Swagger는 현행 계약의 문서화이며 계약 변경의 근거로 사용하지 않는다.

## 테스트 계획

1. 리팩토링 전 특성화: 8개 endpoint의 라우팅, 인증, validation, 성공 JSON, 기존 오류 형식과 주요 결과를 기록한다.
2. 서비스 특성화: 최상위 댓글·답글 작성, 수정, soft delete, 목록·답글 페이징, count, 작성자 ID 목록, 존재하지 않는 Board·Comment, 게시글 작성자 본인 댓글의 알림 제외를 확인한다.
3. ADR-016 회귀: 탈퇴 작성자의 기존 댓글은 조회되고 작성자명이 `탈퇴한 사용자`로 표시되는지 확인한다.
4. Application 경계 전환 후 같은 특성화 테스트를 재실행하고 작성 조율 단위 테스트를 추가한다.
5. 깊이 제한 부재, 다른 Board의 부모 허용, 수정·삭제 소유권 검사 부재는 향후 정책 변경을 위한 별도 관찰 사항으로 기록한다. 현행 허용 동작을 이번 수용 기준에서 바꾸지 않는다.

## 위험 요소

- `toComment()`가 LAZY 연관관계와 자식 컬렉션을 읽으므로 트랜잭션 경계나 변환 위치 변경 시 조회 수 또는 응답이 달라질 수 있다.
- 이벤트 발행 시점 변경은 저장 실패 및 알림 동작을 바꿀 수 있다. 이번에는 기존 Service 위치를 유지한다.
- `BoardService`는 POST Board만 조회한다. Application 이동 중 이 필터와 `BOARD_NOT_FOUND` 결과를 유지한다.
- 기존 수정·삭제의 소유권 검사 부재와 답글 깊이 제한 부재는 요청 정책과 충돌한다. 호환 리팩토링에 섞으면 행동 변경이므로 후속 마일스톤으로 분리한다.
- Comment 전용 상세 명세가 없으므로 테스트와 OpenAPI는 현재 Controller·DTO·Service를 기준으로 작성하고 추정한 계약을 추가하지 않는다.

## Do Not Touch

- Comment 깊이 제한·부모 Board 일치·소유권 검사 추가, `@` 파싱·참여자 목록·Notification 기능 확장
- Comment 조회 쿼리 최적화, 카운트 의미 변경, 삭제 상태 조회 규칙 변경
- DB schema·constraint·migration, Board·User·Notification 구조 변경
- 미사용 DTO·import 제거, 전역 패키지 이동, 무관한 formatting·rename·cleanup
- 복제된 `tag-note-client/` 내부 서버 코드와 프론트엔드

## Acceptance Criteria

1. 8개 Comment endpoint의 공개 계약과 현행 주요 성공·오류 결과가 특성화 테스트로 확인된다.
2. 최상위 댓글·답글 작성의 Board·User 조율이 Application 서비스에 있고 Controller는 이를 호출한다.
3. 기존 저장, ACTIVE 사용자 확인, 댓글 알림, soft delete, 조회 결과 및 응답 형식이 유지된다.
4. `CommentApi`의 OpenAPI 설명이 실제 Controller·DTO·오류 체계와 일치한다.
5. 대상 테스트, 전체 테스트·check·verify, 정적 diff 리뷰를 통과하고 사용자 실행 결과를 확인한다.

## Verification

Codex는 코드 읽기, diff 리뷰와 `git diff --check`만 직접 수행한다. 컴파일·빌드·테스트·자동 검증은 사용자가 실행하고 결과를 전달한다.

```bash
./gradlew test
./gradlew check
./scripts/verify.sh
```

구현·검증·리뷰가 완료되고 사용자 실행 결과가 확인되면 이 파일을 `plans/completed/`로 옮기고 `progress.md`를 업데이트한다. 현재는 구현과 검증 모두 미실행이다.
