# ADR-015 — Google immediate account and canonical authentication

## Decision

- Google userinfo의 `sub`, `email_verified=true`, 유효한 email을 확인한 뒤 `(GOOGLE, sub)`를 계정 identity로 사용한다. 기존 subject가 있으면 저장된 이메일을 변경하지 않는다. 새 subject의 정규화 이메일이 LOCAL 또는 다른 Google 계정에 있으면 provider별 오류로 거부하고 자동 연결하지 않는다.
- 새 Google 계정은 형용사 + 동물 + 4자리 난수 닉네임으로 ACTIVE `ROLE_USER` row를 즉시 만든다. `(provider, subject)`, email, nickname DB unique가 최종 경쟁 조건을 막으며 닉네임 충돌만 별도 write transaction으로 최대 5번 재시도한다. commit 후에만 token을 발급한다.
- GUEST와 PENDING_USER 역할, pending JWT 발급 및 `/api/user/register`를 제거한다. 미인증 요청은 Spring Security anonymous다. 기존 pending JWT는 AccessToken 검증에서 거부한다. 기존 Redis pending key는 별도 삭제 없이 원래 30일 TTL로 만료된다.
- AccessToken의 subject는 user ID만 가리키며, 매 요청 DB의 현재 role/status를 조회해 ACTIVE `ROLE_USER`만 복원한다. 365일 USER cache의 status는 인증 판단에 사용하지 않는다.

## Reason

이전 pending 가입은 닉네임 입력을 요구하고 Redis에 가입 중 identity를 보관했다. JWT 필터가 토큰 subject만으로 USER 역할을 부여하면 탈퇴·중지 후에도 오래된 access token이 권한을 유지한다.

## Consequence

- Google callback 성공은 즉시 USER cookie 2개와 frontend redirect이며, 실패는 `error_code` query가 있는 frontend redirect다. 과거 pending 클라이언트는 Google 재로그인을 시작해야 한다.
- 인증된 요청마다 users PK 조회가 한 번 발생한다. user status를 즉시 반영하는 정확성을 위해 허용한다.
- 기존 nickname이 20자를 넘으면 운영 DB 컬럼 축소 전에 수동 해결한다. 기존 user row를 자동 변경하거나 삭제하지 않는다.
