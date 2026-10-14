# AUTH-001 — 기존 JWT 인증 결함 수정

상태: Active Plan. 이 Plan부터 순서대로 구현한다. 구현·검증은 아직 시작하지 않았다.

## Goal

일반 API에서 refresh token을 access token처럼 사용할 수 없게 하고, access 재발급은 Redis에 저장된 현재 refresh token으로만 허용한다. 로그아웃은 서버의 refresh token을 폐기한다. Google 가입·DB identity·권한 enum은 이 단계에서 바꾸지 않는다.

## 현재 동작과 보호할 계약

- `JwtAuthorizationFilter`는 PENDING 외 모든 유효 JWT에 `ROLE_USER`를 부여한다. ACCESS 타입 검사가 없다.
- `POST /api/user/reissue/accessToken`은 body의 refresh token으로 새 access token을 만들지만 Redis 저장값을 확인하지 않는다. 현재 `ROLE_USER` 선행 인가가 있어 refresh-only 요청으로 재발급하기 어렵다.
- `POST /api/user/reissue/refreshToken`은 새 refresh token을 Redis에 덮어쓴다. `POST /api/user/logout`은 SecurityContext만 비운다.
- 두 재발급 endpoint의 path, Request/Response DTO와 `ApiResult` 구조는 유지한다. 익명 공개 조회와 기존 Google 로그인·pending 닉네임 완료는 이번 단계에서 유지한다.

## Changes

1. 기존 필터/재발급/로그아웃 동작의 특성화 테스트를 작성한다. 관련 ADR 목록을 확인한다. 현재 auth 관련 ADR은 없으며, 서버 명세의 기존 권한 정책을 유지한다.
2. 일반 보호 API는 서명·만료가 유효한 `AccessToken` 타입만 principal로 복원한다. 다른 타입·누락 타입은 401로 거부한다. 기존 `POST /api/user/register`의 PENDING 전용 처리는 AUTH-003까지 명시적 예외로 유지하며 다른 API에는 PENDING 권한을 부여하지 않는다. 미인증 공개 요청은 이 단계에서 기존 GUEST 처리와 호환한다.
3. access 재발급 endpoint는 ACCESS 선행 인가 없이 body의 `RefreshToken`을 전용으로 검증한다. 필터가 이 endpoint의 refresh token을 일반 API 권한으로 취급하거나 먼저 거부하지 않도록 경로를 분리한다. 토큰 subject의 계정이 활성 상태이고 Redis의 해당 userId에 저장된 토큰과 값이 같아야 한다. 만료·위조·타입 오류·Redis 부재·불일치는 401이다. 기존 Request/Response 구조는 유지한다.
4. refresh 회전은 이전 refresh token을 재발급에 사용할 수 없게 한다. 로그아웃은 인증 userId의 Redis refresh token을 삭제한다. 이미 발급된 access token은 자연 만료까지 유효하며 탈퇴 시 즉시 차단은 AUTH-008에서 처리한다.
5. User API용 Swagger interface에 세 endpoint의 인증, Request/Response, 401/error code를 실제 구현에 맞춰 기록한다. Controller에 Swagger 어노테이션을 쓰지 않는다.

## Acceptance Criteria

- REFRESH/PENDING/잘못된 타입 JWT로 일반 보호 API를 호출할 수 없다. 기존 PENDING 가입 완료 endpoint는 AUTH-003까지 작동한다.
- 현재 Redis refresh token만 access 재발급에 사용된다. 회전 전 토큰과 로그아웃 전 토큰은 거부된다.
- 재발급 endpoint는 별도 access token 없이 유효한 refresh token으로 작동한다.
- 기존 공개 조회, Google 로그인, pending 닉네임 완료, 재발급 Request/Response 계약이 유지된다.
- 토큰 검증과 Redis 호출에 불필요한 DB transaction을 두지 않는다.
- 필터/API/서비스 테스트와 Swagger가 실제 오류 응답과 일치한다.

## Verification / Review

- 테스트: ACCESS/REFRESH/PENDING 권한 매트릭스, Redis 일치·불일치·삭제, refresh 회전, 로그아웃, 기존 user Controller 회귀.
- 리뷰: 타입별 endpoint 경계, 토큰 subject와 Redis key, 인증 없는 재발급 경로가 다른 API를 열지 않는지, 기존 `error_code`/`custom_error_code` 체계.
- Codex는 코드·diff와 `git diff --check`만 확인한다. 사용자가 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`를 실행해 결과를 제공한 뒤에만 완료 처리한다.
