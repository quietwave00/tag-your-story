# AUTH-007 — 닉네임 변경 API

상태: Ready Plan. AUTH-006 완료 후 착수한다. 구현·검증 미실행.

## Goal

Google/LOCAL의 인증 사용자가 가입 후 자신의 nickname을 변경한다. 기존 게시글·댓글 응답의 nickname이 새 값을 반영한다.

## API / Rules

- `PATCH /api/user/me/nickname`, ROLE_USER, body `{ "nickname": "새닉네임" }`, 성공 응답에 확정 nickname.
- 요청에는 userId를 받지 않는다. 인증 principal의 userId로만 변경한다.
- 1~20자, 빈 값·공백 포함 금지. 자동 닉네임의 형용사+동물+숫자 형식은 수동 변경에 강제하지 않는다. 수동 nickname은 입력 그대로 저장한다.
- DB nickname unique가 최종 방어선이다. 다른 사용자와 중복되면 `NICKNAME_ALREADY_EXISTS`; 자기 현재 nickname을 다시 보내면 값 변화 없는 성공으로 처리한다.

## Changes

1. User Application use case에서 ACTIVE 사용자 확인, 닉네임 검증 및 원자적 갱신을 수행한다. Controller에 Repository나 비즈니스 규칙을 두지 않는다.
2. USER cache를 갱신·무효화해 이전 nickname이 로그인 사용자 정보에 남지 않게 한다. Board/Comment/Notification의 nickname 조회가 DB 현재 값 또는 같은 display 규칙을 읽는지 점검한다. 관련 없는 콘텐츠 로직은 바꾸지 않는다.
3. Request/Response 필드, 인증, 400/401/409 오류와 `error_code`/`custom_error_code`를 Swagger interface에 문서화한다.

## Acceptance Criteria

- 본인 nickname만 변경할 수 있고 다른 userId 변경은 불가능하다.
- 생성 nickname과 수동 nickname 모두 unique를 만족한다. 동시 중복 변경 중 하나만 성공한다.
- Board/Comment 등 기존 공개 응답이 변경 nickname을 표시하고 캐시가 이전 값을 반환하지 않는다.
- 기존 가입·로그인 계약이 유지된다.

## Verification / Review

- 테스트: 입력 경계, 자기 값 재요청, unique 경쟁, 타인 변경 차단, 캐시/콘텐츠 조회, Swagger.
- 사용자 실행: `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`. Codex는 빌드·테스트를 실행하지 않고 코드·diff를 정적으로 검토한다.
