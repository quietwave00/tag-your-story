# AUTH-008 — 회원 탈퇴 API와 콘텐츠 보존

상태: Ready Plan. AUTH-007 완료 후 착수한다. 구현·검증 미실행.

## Goal

인증 사용자가 탈퇴하면 즉시 더 이상 로그인·API 사용을 할 수 없게 하고, 기존 게시글·댓글은 공개 상태로 유지하면서 작성자 식별 정보를 제거한다.

## API / Policy

- `DELETE /api/user/me`, ROLE_USER. 요청의 userId는 받지 않는다. 성공 시 빈 `ApiResult`와 인증 쿠키 만료를 반환한다.
- `UserStatus.WITHDRAWN`을 추가한다. Board/Comment/UserTag/Like/Notification이 참조하는 user row와 FK는 유지한다. 게시글·댓글 및 연결된 태그의 공개 상태는 유지하고 작성자명은 `탈퇴한 사용자`로 표시한다.
- email, Google subject/user_key, password hash, nickname 같은 로그인·식별 값은 삭제 또는 고유한 비식별 tombstone 값으로 치환한다. 기존 unique/not-null 제약을 만족하는 DDL·JPA·migration을 함께 설계한다. 정규화 이메일과 Google subject를 해제하므로 같은 신원으로 다시 가입하면 새 userId가 된다.

## Changes

1. 기존 Board/Comment/Notification author 응답, UserTag/Like FK와 User cache/Redis token을 특성화한다. ADR-014의 identity 규칙과 충돌하는 결정이 있으면 새 ADR을 작성한다.
2. 한 DB transaction에서 ACTIVE→WITHDRAWN과 식별 정보 정리를 확정한다. 중복 탈퇴 요청의 401/404 처리와 DB 실패 경로를 일관되게 정한다.
3. Redis refresh token·USER cache를 폐기하고 쿠키를 만료시킨다. 기존 access JWT는 AUTH-003의 status 확인으로 즉시 거부된다. Redis 삭제 실패나 오래된 cache가 계정을 재활성화하지 못하도록 canonical status 확인을 최종 방어선으로 둔다.
4. 게시글·댓글 공개 조회는 본문을 유지하되 withdrawn author를 `탈퇴한 사용자`로 표시한다. 탈퇴자에게 새 Like/Notification/Tag write나 알림 전송이 발생하지 않는지 점검한다. 본문·태그에 사용자가 직접 적은 개인정보 처리 요청은 별도 기능으로 분리한다.
5. Swagger interface에 인증, 성공 응답, 재요청·미인증 오류와 기존 오류 코드를 문서화한다. 운영 migration과 rollback/preflight를 기록한다.

## Acceptance Criteria

- 탈퇴 직후 기존 access/refresh token과 Google/LOCAL 로그인이 거부된다.
- 동일 이메일/Google subject의 재가입은 새 계정으로 가능하며 과거 userId와 자동 연결되지 않는다.
- 게시글·댓글은 공개 유지되고 작성자명은 일관되게 `탈퇴한 사용자`다. FK와 집계가 깨지지 않는다.
- PII 필드가 정의된 방식으로 비식별화되고 USER cache/Redis 토큰이 정리된다.
- 탈퇴와 인증·조회가 동시에 일어나도 WITHDRAWN 계정에 새 write 권한을 주지 않는다.

## Verification / Review

- 테스트: token 즉시 거부, Redis 실패, provider 재로그인·재가입, FK/unique, Board/Comment 표시, 동시 요청, Swagger.
- 사용자 실행: `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`. Codex는 빌드·테스트를 실행하지 않고 코드·diff를 정적으로 검토한다.
