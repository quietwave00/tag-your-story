# NOTIFICATION-REF-001 — SSE 전달 경계 리팩토링

> 상태: 2026-10-26 Human Review 승인, 구현 미착수. 이번 계획 수립 단계에서는 production code를 수정하지 않는다.

## Goal

Like와 최상위 Comment로 생성되는 Notification의 기존 저장·실시간 알림·목록 조회 동작을 유지하면서, SSE 연결 및 전송을 Infrastructure로 분리한다. 단일 서버에서 여러 탭/기기 연결을 안전하게 관리하고, 전송 실패가 저장된 알림 또는 원래 Like/Comment 결과를 되돌리지 않도록 한다. 이번 한 마일스톤은 **알림 전달 경계와 SSE 연결 수명**에 한정한다.

## 근거 문서와 ADR

- `AGENTS.md`, `agents/server/conventions.md`, `agents/server/progress.md`를 확인했다.
- `agents/server/current_state.md`의 기준 범위에는 Notification이 없다. 따라서 아래 AS-IS는 실제 코드를 기준으로 기록한다.
- `agents/server/server_spec.md` §10은 `LIKE/SUBSCRIBED_TAG/SYSTEM`, `target_type/target_id`를 기술하지만 실제 코드는 `COMMENT/LIKE`, `contentId`를 사용한다. 명세 안에서도 `BOARD_LIKE`와 `BOARD`가 불일치한다. 이번 리팩토링에서 이를 임의로 맞추거나 기존 데이터를 변환하지 않는다.
- 관련 결정: ADR-013(Board ID/대상), ADR-016(탈퇴 사용자와 Notification FK 보존). ADR-016의 탈퇴 수신자 차단과 탈퇴 작성자 표시 정책을 유지한다.
- `LIKE-REF-001`, `COMMENT-REF-001`에서 확정한 본인 글 Like/Comment 알림 제외, 답글 알림 미발행, commit 후 비동기 listener 경계를 유지한다.

## AS-IS

1. `POST` Like 또는 최상위 Comment의 쓰기 트랜잭션에서 `CommonEventPublisher`가 `NotificationAdaptor` 이벤트를 발행한다. `CommonEventListener`의 `@TransactionalEventListener` 기본 `AFTER_COMMIT`와 `@Async`가 처리한다. Listener는 `NotificationService.save()`의 `REQUIRES_NEW` 트랜잭션이 끝난 뒤 `send()`를 호출한다. 트랜잭션 없는 발행은 기본적으로 처리되지 않는다.
2. `NotificationService`가 사용자 상태·잠금, JPA 저장, SSE 구독 및 전송, JSON 직렬화를 함께 조율한다. `SseManager`와 `SseStorage`는 Domain 경로 아래 있으나 Spring MVC `SseEmitter` 및 프로세스 메모리에 의존한다. `NotificationManager`의 Redis Pub/Sub 메서드는 현재 SSE 전송 호출 경로에 연결되지 않는다.
3. `SseStorage`는 사용자당 emitter 하나를 정적 map에 둔다. 두 번째 탭은 기존 emitter를 돌려받는다. 완료/timeout callback은 사용자 ID로 삭제하므로 오래된 연결이 새로운 연결을 지울 수 있다. 명시적 timeout, `onError`, heartbeat, 전송 실패 제거, 이벤트 ID, replay가 없다.
4. SSE는 `init`/`subscribe`, `Notification` 이벤트를 보낸다. 현재 `Notification` 전체를 JSON 문자열로 직렬화한다. 이 객체의 `publisher`/`subscriber`에는 이메일·userKey·subject 등 내부 필드가 들어갈 수 있다. React 클라이언트는 `notificationId`, `contentId`, `type`, 표시 이름을 사용하고 `Notification` 이벤트를 구독한다.
5. 기존 HTTP API는 `GET /api/notification/subscription`, `GET /api/notification?page=`, `PATCH /api/notification`, `GET /api/notification/count`, `PATCH /api/notification/all`이다. 목록 0 기반 5건, `createdAt DESC`; count는 전체 건수이며 읽음 여부는 목록의 `isRead`로 구분한다. 전체 hard delete API는 없다.

## AS-IS / TO-BE 차이와 범위 판단

| 기준 | 이번 판단 |
| --- | --- |
| Layered Architecture 위반 | Spring MVC emitter와 프로세스별 연결 저장을 Domain 경로에서 Infrastructure로 옮긴다. Controller에는 연결·직렬화 로직을 두지 않는다. |
| Application Use Case / Infrastructure 혼재 | Application의 전달 유스케이스는 저장 성공 이후 전달 여부·순서를 조율하고, SSE adapter는 연결·직렬화·전송만 담당한다. 기존 JPA 저장·조회 전체를 옮기는 작업은 이번 범위가 아니다. |
| Controller 과도한 책임 | Controller는 인증된 사용자 ID를 전달하고 `SseEmitter`를 반환한다. 기존 목록 DTO 변환은 SSE 변경과 무관하므로 건드리지 않는다. |
| 향후 기능 충돌 | hard delete 이후 이미 대기 중인 비동기 이벤트가 새 알림을 만들 수 있다. 삭제 기능의 동시성/시점 정책은 별도 설계가 필요하다. count 의미와 읽음 구분은 유지한다. |
| JPA/Repository | `save()`의 수신자 잠금·ACTIVE 확인·별도 트랜잭션을 유지한다. 목록 publisher LAZY/N+1 가능성과 인덱스 부재는 기록하되 이번에 쿼리/DDL을 바꾸지 않는다. |
| Characterization Test | 기존 테스트는 탈퇴 수신자 차단, 단일 emitter 구독, 일부 읽음 권한만 보호한다. 실제 SSE 전송 테스트는 주석 처리되어 있고 HTTP/SSE 계약 테스트가 부족하다. 변경 전에 보강한다. |
| Public API 위험 | endpoint/method/auth/이벤트명/`init` 데이터/목록·읽음 응답은 유지한다. SSE `Notification` JSON의 내부 사용자 객체 제거는 **의도적인 raw payload 계약 변경**이다. 아래 계약과 클라이언트 검증을 Human Review에서 승인받아야 한다. |
| 최소 Enabling Refactor | 전달 port, 저장 후 전달 Application 조율, 연결 ID별 registry, 최소 SSE payload가 필요하다. |
| 무관한 cleanup | Redis Pub/Sub 제거·활성화, Like/Comment/Board 재구성, Notification 조회·읽음 계층 이동, 전체 삭제 구현은 수행하지 않는다. |

## Target State와 전달 보장 범위

1. DB에 commit된 Notification을 알림 내역의 기준으로 둔다. 원래 Like/Comment 트랜잭션 commit 후 별도 Notification 저장 트랜잭션을 완료하고, 그 다음 네트워크 전송을 시도한다. 전송 성공은 브라우저 표시까지 보장하지 않는다. 저장 실패는 SSE로 보내지 않고 실패를 관찰 가능한 로그/지표로 남긴다. 이 단계는 프로세스 장애·비동기 작업 유실에 대한 영속 재시도를 제공하지 않는다.
2. `NotificationDeliveryPort`는 Application이 전달할 최소 불변 payload를 받고, 구현은 Infrastructure의 SSE transport가 담당한다. Application/Domain은 `SseEmitter`, `ObjectMapper`, 연결 map을 알지 않는다. 기존 `NotificationService.save()`의 수신자 잠금, publisher 상태 확인, `REQUIRES_NEW` 경계는 보존한다.
3. 새 구독은 매번 별도 emitter를 만들고 사용자 ID 아래 연결 ID별로 등록한다. callback 및 전송 실패는 `(userId, connectionId)`의 해당 emitter만 제거한다. 등록과 최초 `init` 전송의 순서·실패 cleanup을 명확히 하고 동시 구독/종료 race를 테스트한다. 연결은 명시적 timeout, `onCompletion`/`onTimeout`/`onError`, 주기적 heartbeat로 관리한다. heartbeat는 클라이언트의 알림 이벤트로 처리되지 않는 SSE comment를 사용한다.
4. 전송은 현재 프로세스의 해당 사용자 모든 살아 있는 연결에 시도한다. 한 연결의 실패가 다른 연결 전송을 막지 않는다. 연결별 전송은 직렬화하고, 느린 연결·과도한 연결 수가 무제한 메모리/작업 증가를 만들지 않도록 per-user 연결 상한과 초과 시 가장 오래된 연결 종료 정책을 명시한다. timeout·heartbeat 주기·상한은 설정으로 두고 운영값을 문서화한다. heartbeat 작업은 기존 비동기 listener executor와 분리한다.
5. `Notification` 이벤트 payload는 `{notificationId, contentId, pubNickname, type, isRead}`로 제한한다. 표시 이름은 `getDisplayNickname()` 정책을 사용한다. 이메일, userKey, subject, subscriber 객체를 보내지 않는다. React가 사용하는 메시지/게시글 이동 필드는 유지하지만 raw SSE JSON은 변경된다. 이 계약 변경은 본 Plan의 Human Review 범위에 포함한다.
6. 재접속 후 놓친 알림은 기존 목록 API 조회로 확인한다. 클라이언트의 EventSource `open` 시점(최초 연결과 재연결 모두)에 count를 새로 조회하고, 알림 탭이 열려 있으면 목록도 새로 조회한다. 이번에는 event ID·`Last-Event-ID` replay나 Redis fan-out을 추가하지 않는다. 배포 지원 범위는 **단일 서버 인스턴스**로 명시한다. 다중 인스턴스에서는 로컬 emitter만으로 모든 구독자에게 실시간 전달할 수 없으므로, 다중 인스턴스 배포 전 영속 outbox/재시도와 노드 간 fan-out 설계를 별도 마일스톤으로 승인받아야 한다. Redis Pub/Sub만으로 영속 전달 보장을 주장하지 않는다.
7. 기존 query-string access token 방식은 이 마일스톤에서 변경하지 않는다. URL 기록 노출 위험을 별도 Auth/클라이언트 계약 작업으로 기록하고, 운영 검토 시 HTTPS와 URL 로그 정책을 확인한다.

## 변경할 파일과 책임

| 파일 | 변경 책임 |
| --- | --- |
| `tagnote-api/src/main/java/com/tagnote/api/domain/notification/NotificationController.java` | 구독을 새 SSE transport에 연결. 기존 경로·ROLE_USER·`text/event-stream` 유지. |
| `tagnote-api/src/main/java/com/tagnote/api/domain/notification/dto/request/NotificationReadRequest.java` | Swagger에서 필드 의미·필수 여부를 명시. DTO JSON 구조는 유지. |
| `tagnote-api/src/main/java/com/tagnote/api/domain/notification/dto/response/NotificationResponse.java` | Swagger에서 필드 의미를 명시. DTO JSON 구조는 유지. |
| `tagnote-api/src/main/java/com/tagnote/api/domain/notification/dto/response/NotificationCountResponse.java` | Swagger에서 전체 건수 의미를 명시. DTO JSON 구조는 유지. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/event/listener/CommonEventListener.java` | 기존 AFTER_COMMIT 이벤트를 Application 전달 유스케이스에 위임. 이벤트 발생 순서 유지. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/notification/service/NotificationService.java` | SSE 구독/전송/직렬화 책임 제거. 기존 저장 트랜잭션·사용자 상태 확인·조회/읽음 메서드는 유지. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/notification/service/NotificationFacade.java` | 구독 위임을 새 transport로 옮기고 기존 조회·읽음 위임 유지. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/notification/sse/SseManager.java` | 교체 후 참조가 없을 때 제거. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/notification/sse/SseStorage.java` | 교체 후 참조가 없을 때 제거. 정적 map을 남기지 않는다. |
| `tagnote-core/src/test/java/com/tagnote/domain/notification/service/NotificationServiceTest.java` | 저장·탈퇴 정책 특성화 및 SSE 책임 이동에 맞는 테스트 갱신. |
| `tagnote-core/src/test/java/com/tagnote/domain/notification/sse/SseManagerTest.java` | 새 Infrastructure adapter 테스트로 대체. |
| `tag-note-client/tag-note-react/src/features/notifications/NotificationCenter.jsx` 및 해당 테스트 | 최소 SSE payload의 메시지·클릭 이동 호환 확인. 필요한 경우 재접속 후 목록 refresh만 추가. 알림 UI 재설계는 하지 않는다. |
| `tag-note-client/tag-note-react/src/services/notificationService.js` 및 해당 테스트 | 기존 EventSource URL·인증을 유지하며 `open` callback을 전달해 재접속 후 동기화를 지원. |

## 새로 만들 파일과 책임

| 파일 | 책임 |
| --- | --- |
| `tagnote-api/src/main/java/com/tagnote/api/domain/notification/NotificationApi.java` | 5개 기존 endpoint의 method·인증·query/body·response·주요 오류 및 SSE 이벤트 payload를 Controller와 분리해 Swagger로 기술. |
| `tagnote-core/src/main/java/com/tagnote/application/notification/NotificationDeliveryService.java` | commit 후 Notification 저장 호출 → 저장 성공 시 전달 port 호출. 전송 실패를 저장 실패와 구분. |
| `tagnote-core/src/main/java/com/tagnote/application/notification/NotificationDeliveryPort.java` | 프레임워크 타입이 없는 전달 계약. |
| `tagnote-core/src/main/java/com/tagnote/application/notification/NotificationDelivery.java` | 알림 ID, 수신자 ID, 게시글 ID, 표시 이름, 타입, 읽음 상태만 담는 전달 값. |
| `tagnote-core/src/main/java/com/tagnote/infrastructure/notification/sse/SseNotificationTransport.java` | delivery port 구현, SSE 이벤트 직렬화·전송·heartbeat·연결 수명. Presentation의 구독 반환을 위한 `SseEmitter` 생성 진입점 제공. |
| `tagnote-core/src/main/java/com/tagnote/infrastructure/notification/sse/SseConnectionRegistry.java` | 사용자/연결 ID별 thread-safe 저장과 조건부 삭제·상한 처리. |
| `tagnote-api/src/test/java/com/tagnote/api/domain/notification/NotificationSseControllerTest.java` | 인증·MIME type·초기 이벤트·기존 endpoint 특성화. |
| `tagnote-core/src/test/java/com/tagnote/application/notification/NotificationDeliveryServiceTest.java` | 저장 성공/실패, 탈퇴 상태, 전송 실패와 순서. |
| `tagnote-core/src/test/java/com/tagnote/infrastructure/notification/sse/SseNotificationTransportTest.java` | 여러 연결, callback race, heartbeat, timeout/오류 cleanup, 한 연결 실패 시 다른 연결 전달, payload 필드. |

`NotificationManager`와 그 Redis DTO는 기존 전송 경로와 무관하므로 이번에 수정하지 않는다. 새 설정값은 기존 설정 파일 구조를 확인한 뒤 필요한 파일만 변경 목록에 추가하고, 추가 사유를 리뷰 기록에 남긴다.

## 호출 흐름

```text
Like/최상위 Comment 쓰기 transaction
  → CommonEventPublisher.publishEvent
  → AFTER_COMMIT + Async CommonEventListener
  → NotificationDeliveryService
  → 기존 NotificationService.save (REQUIRES_NEW; ACTIVE/권한 확인; DB commit)
  → NotificationDeliveryPort
  → SseNotificationTransport → 현재 인스턴스의 해당 사용자 연결 모두에 전송

GET /api/notification/subscription (ROLE_USER)
  → NotificationController → SseNotificationTransport.open(userId)
  → 연결 ID 등록 → init 이벤트 → SseEmitter 반환
  → heartbeat / completion / timeout / error에서 연결 ID 기준 cleanup
```

SSE 전송과 heartbeat는 DB transaction을 열지 않는다. user status 재확인은 기존 전송 정책을 유지하되 Application에 두고, 매 heartbeat마다 DB 조회하지 않는다. 탈퇴 직후 이미 전송 중인 이벤트와의 race는 테스트와 리뷰에서 확인한다.

## 기존 API 영향

- 기존 HTTP endpoint, method, ROLE_USER 인증, 목록/읽음/count의 `ApiResult`와 오류 체계를 유지한다. `count`는 전체 알림 수다.
- `GET /subscription`의 `text/event-stream`, `init`/`subscribe`, `Notification` 이벤트명, 알림의 의미·게시글 링크를 유지한다. Swagger는 새 `NotificationApi` interface에 작성하고 Controller가 구현한다.
- **SSE `Notification` 이벤트의 raw JSON만 최소 필드 형태로 변경한다.** 기존 전체 `Notification` 객체의 중첩 `publisher`/`subscriber` JSON에 의존하는 소비자가 있다면 깨질 수 있으므로 구현 전에 사용처를 검색하고 React 테스트로 확인한다. 이 변경은 보안상 필요한 계약 변경으로 Swagger/이벤트 문서에도 기록한다. Notification HTTP DTO는 변경하지 않는다.

## 테스트 계획

1. 변경 전 Controller/SSE/이벤트 특성화: 인증, `init`, 이벤트명, payload의 클라이언트 사용 필드, 본인 글 Like/Comment 제외, 답글 미발행, rollback 시 listener 미호출, 저장 후 전송, 탈퇴 수신자 차단을 고정한다.
2. Application 단위 테스트: 저장 성공 후 한 번 전달, 저장 null/예외 때 미전달, 전송 예외가 DB 저장을 되돌리지 않음, subscriber ACTIVE 검사와 탈퇴 정책 유지.
3. SSE adapter 테스트: 같은 사용자 2개 이상 emitter가 각자 받음, 다른 사용자는 받지 않음, 오래된 callback이 새 연결을 삭제하지 않음, 초기 전송 실패 cleanup, heartbeat가 알림으로 해석되지 않음, timeout/error/send IOException 후 제거, 한 연결 실패 시 다른 연결 전달, 상한/초과 정책, 최소 JSON 필드에 개인정보가 없음.
4. API·클라이언트 테스트: 기존 구독 인증/미디어 타입, `Notification` 수신 후 메시지·게시글 이동, 최초 연결/재연결 `open` 시 count 및 열린 탭의 목록 재조회, 중복 표시 방지. 실제 multi-node 보장은 이 테스트의 성공으로 주장하지 않는다.

## 위험 요소와 대응

| 위험 | 대응/한계 |
| --- | --- |
| commit 후 비동기 작업이 프로세스 장애로 유실 | 현재도 존재한다. DB 알림 저장 전 유실 가능성을 이 마일스톤에서 없앴다고 주장하지 않는다. 다중 인스턴스/운영 신뢰성 단계에서 outbox와 재시도 설계 필요. |
| DB 저장 성공 후 SSE 실패 | 알림은 목록에서 조회 가능; 연결 제거·로그/지표 기록; 원래 쓰기와 DB 저장을 롤백하지 않는다. |
| 중복/순서 | 전달은 best-effort 실시간 push이며 클라이언트 수신 보장은 없다. UI는 notificationId 기준 중복 표시를 막는다. 날짜만으로 정렬되는 목록의 동순위 문제는 별도 조회 작업에서 처리한다. |
| 여러 탭 및 race | 연결 ID별 조건부 삭제와 thread-safe registry; 종료 callback·등록·전송 동시성 테스트. |
| 느린 연결/자원 고갈 | 명시적 timeout·heartbeat·연결 상한·실패 cleanup. per-connection 전송 동기화와 executor/스케줄러 용량 리뷰. |
| 개인정보 노출 | raw domain 객체 직렬화 중단, 최소 payload 사용. 이 변화는 공개 SSE 계약 변경으로 승인 필요. |
| query token | 기존 인증 방식을 유지하므로 URL 노출 위험이 남는다. 별도 Auth 계약 변경 전 운영 로그·HTTPS 정책을 확인한다. |
| multi-node | 로컬 registry로는 다른 노드의 연결에 전송되지 않는다. 이 마일스톤의 배포 전제는 단일 인스턴스다. |
| hard delete와 지연 이벤트 | 삭제 뒤 늦게 저장된 알림이 다시 나타날 수 있다. hard delete 설계에서 시점/동시성 정책을 별도 확정한다. |

## Do Not Touch

- Notification 목록/count/단건·전체 읽음 구현 및 의미, hard delete 신규 API, unread 전용 API.
- Like/Comment/Board 쓰기 정책과 이벤트 발행 조건, Notification JPA schema/DDL 및 기존 FK.
- Redis Pub/Sub 연결/제거, 영속 outbox, multi-node fan-out, `Last-Event-ID` replay.
- query token → cookie/header 인증 전환, 무관한 DTO·패키지 rename, SSE 외 프론트 UI 정리.

## Acceptance Criteria

1. 기존 Like·최상위 Comment 알림의 생성 조건, 저장 순서, 탈퇴 사용자 차단, 목록 조회 가능성, 기존 HTTP API가 유지된다.
2. `SseEmitter`/연결 map/JSON 직렬화가 Domain·Application에 없고, Application은 전달 port만 사용한다. Controller에 비즈니스 로직이 없다.
3. 한 사용자 여러 연결이 각각 `Notification`을 받으며, 다른 사용자에게 전달되지 않는다. 연결 종료·오류·timeout·전송 실패가 정확한 연결만 제거한다.
4. 명시적 timeout·heartbeat·연결 상한이 있고, 여러 연결 중 하나의 실패가 다른 연결의 전달을 막지 않는다.
5. DB 저장이 끝나기 전에 SSE를 보내지 않는다. 실패한 저장은 전송하지 않는다. SSE 실패가 이미 commit된 저장을 되돌리지 않는다.
6. SSE payload가 승인된 최소 필드만 포함하며 이메일·userKey·subject·subscriber 내부 객체를 포함하지 않는다. 기존 React 알림 표시·링크 이동이 동작한다.
7. 단일 인스턴스 지원 범위, 유실 가능성, 다중 인스턴스 전제, query token 위험이 운영/리뷰 기록에 명시된다.
8. 관련 테스트와 diff 리뷰를 통과하고 범위 밖 변경이 없다.

## Verification

- Codex는 코드·diff·`git diff --check`만 정적으로 확인한다. 이 계획 수립 단계에서 빌드·테스트·검증 스크립트를 실행하지 않는다.
- 구현 후 사용자가 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`를 실행하고 결과를 전달한다. 전달된 결과와 Acceptance Criteria, 리뷰를 확인한 뒤에만 `plans/completed/`로 이동하고 `progress.md`를 갱신한다.
- 2026-10-26 Human Review 승인으로 `agents/server/plans/active/NOTIFICATION-REF-001.md`에 확정했다. 구현과 자동 검증은 아직 시작하지 않았다.
