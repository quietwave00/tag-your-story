# AUTH-006 — 자체 이메일·비밀번호 로그인

상태: Ready Plan. AUTH-005 완료 후 착수한다. 구현·검증 미실행.

## Goal

LOCAL 계정이 정규화 이메일과 비밀번호로 로그인하고 기존 access/refresh 발급·회전·로그아웃 경로를 사용한다.

## Changes

1. `POST /api/auth/login`에 email, password를 받는다. AUTH-002의 email normalizer를 재사용하고 저장된 password hash만 검증한다. Google 계정에는 비밀번호가 없으며 `GOOGLE_ACCOUNT_EXISTS` 또는 동등한 명시적 provider 안내 오류를 반환한다. 존재하지 않는 LOCAL 이메일과 틀린 비밀번호는 동일한 일반 인증 오류로 처리한다.
2. 로그인 성공 시 ACTIVE 여부를 확인하고 기존 JWT/Redis refresh 저장 경로와 연결한다. 휴면·탈퇴 상태는 로그인할 수 없다. 실패 횟수 제한과 rate limit을 적용한다.
3. AUTH-001의 재발급·로그아웃 검증을 재사용한다. Google/LOCAL 토큰의 권한과 만료 정책을 같게 유지한다.
4. 로그인 API의 인증 없음, Request/Response, 주요 오류를 Swagger interface에 기록한다. 현재 서버의 쿠키 전달 방식을 유지하고 쿠키·CSRF 정책의 별도 변경이 필요하면 근거와 계약 영향을 보고한다.

## Acceptance Criteria

- 공백·대소문자가 다른 이메일 입력은 같은 LOCAL 계정으로 로그인한다.
- Google 계정에는 비밀번호 로그인이 허용되지 않고 Google 가입 사실을 알린다.
- 틀린 비밀번호·비활성 계정은 토큰을 받지 않는다. 성공 토큰은 기존 보호 API/refresh/logout에서 동작한다.
- 기존 Google 로그인과 가입 API는 변경되지 않는다.

## Verification / Review

- 테스트: 비밀번호 검증, 정규화, provider 안내, 실패 제한, JWT/refresh/logout 회귀, Swagger.
- 사용자 실행: `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`. Codex는 빌드·테스트를 실행하지 않고 코드·diff를 정적으로 검토한다.
