# AUTH-ME-001 — 인증 사용자 닉네임 조회

> 상태: 2026-10-27 사용자 구현 요청으로 승인, 구현·테스트 코드 작성. 사용자 실행 검증 대기.

## Goal

Google OAuth redirect 뒤 클라이언트가 서버에 저장된 현재 닉네임을 조회할 수 있도록 `GET /api/user/me`를 제공하고 callback 화면에서 이를 사용한다. 기존 이메일 로그인·가입 응답과 닉네임 변경·탈퇴 계약은 유지한다.

## Decision과 범위

- ADR-014~016의 Google identity, ACTIVE ROLE_USER 인증, 탈퇴 정책을 유지한다.
- `GET /api/user/me`는 AccessToken 인증이 필요하고 요청 body·query는 없다. `ApiResult.response.nickname`만 반환하며 email, userId, provider, subject를 노출하지 않는다.
- JWT 필터가 요청마다 DB의 현재 role/status를 검사하고 현재 닉네임을 principal에 담는다. Controller는 그 값을 읽으므로 프로필 조회를 위한 별도 DB SELECT를 추가하지 않는다. 인증 실패는 기존 필터/인가 오류를 따른다.
- Google callback 클라이언트는 cookie를 token storage로 옮긴 뒤 `/me`를 조회해 닉네임을 저장한다. 이전 계정의 로컬 닉네임은 조회 전에 지운다. 프로필 조회 실패는 이전 닉네임을 복원하지 않고 기존 로그인 상태에 맞춰 이동한다.
- Swagger `UserApi`와 `tag-note-client/agents/client/api-contract.md`를 같은 변경에서 갱신한다. 다른 사용자 정보 필드, 공개 프로필 API, DB schema 변경은 포함하지 않는다.

## Acceptance Criteria

- USER token으로 요청하면 현재 사용자의 닉네임만 반환하며 임의 userId query로 다른 계정의 정보를 읽지 못한다.
- 미인증·비활성·탈퇴·만료 token은 기존 인증 경계의 오류를 따른다.
- Google callback 뒤 클라이언트가 `/me`의 닉네임을 표시하고 이전 계정 닉네임을 재사용하지 않는다.
- API/클라이언트 테스트, Swagger·계약 문서 및 범위 diff를 확인한다.

## Verification

Codex는 코드·diff·`git diff --check`만 정적으로 확인한다. 사용자가 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`와 클라이언트 `npm test`를 실행해 결과를 전달하면 Acceptance Criteria를 검토하고 Plan을 completed로 옮긴다.

2026-10-27 사용자 실행의 `:tagnote-api:compileTestJava`에서 기존 `PrincipalDetails`에 닉네임 생성자·getter가 없어 2건의 컴파일 오류가 발생했다. `PrincipalDetails`에 닉네임을 추가하고 JWT 필터가 DB에서 검증한 현재 닉네임을 전달하도록 수정했다. 재검증 결과를 기다린다.

2026-10-27 사용자 실행의 `:tagnote-api:test`에서 미인증 `/me` 요청 시 `@AuthenticationPrincipal(expression = "nickname")`이 익명 principal에 먼저 적용되어 SpEL 예외가 발생했다. Controller가 `PrincipalDetails`를 직접 받아 인가된 요청에서만 닉네임을 읽도록 수정했다. 재검증 결과를 기다린다.
