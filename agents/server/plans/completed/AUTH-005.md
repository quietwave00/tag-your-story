# AUTH-005 — 자체 이메일 가입과 6자리 확인

상태: Completed. 2026-10-20 사용자가 구현 및 전체 테스트 완료를 확인하고 Human Review 완료 처리를 요청했다.

2026-10-20 완료 기록: 이메일 가입 시작·재발송·확인과 LOCAL 계정 생성을 사용자 검증 결과에 따라 완료 처리했다.

2026-10-16 구현 기록: 이메일 가입 시작·재발송·확인·사용 가능 여부 API와 분리된 Swagger interface를 작성했다. BCrypt 해시, 기존 Google 닉네임 생성기, provider별 중복 오류, IP/이메일 조회 제한, Redis 원자 선점·완료 표시·소유자 잠금과 별도 DB write transaction을 연결했다. DB commit 후 Redis 완료 표시 실패는 challenge 기반 내부 user_key로 복구한다. 관련 단위·Redis·Web MVC 테스트를 추가했다. `./gradlew`와 `scripts/verify.sh`는 실행하지 않았다. AUTH-004 및 이번 단계의 사용자 검증 결과가 필요하므로 완료 처리하지 않는다.

## Goal

이메일·비밀번호·비밀번호 재확인으로 가입을 시작하고 6자리 이메일 코드 확인이 성공할 때만 LOCAL USER 계정을 생성한다.

## API / Flow

- `POST /api/auth/signup/email/start`: email, password, passwordConfirm. 이메일을 서버에서 trim+소문자화하고 비밀번호 일치·정책·계정 중복을 검사한다. 기존 Google 계정이면 `GOOGLE_ACCOUNT_EXISTS`로 Google 가입 사실을 알린다. challengeId를 반환하고 코드를 발송한다.
- `POST /api/auth/signup/email/resend`: challengeId. cooldown/횟수 제한을 적용하고 이전 코드를 무효화한다.
- `POST /api/auth/signup/email/confirm`: challengeId, 6자리 code. 성공 시 비밀번호 해시, 정규화 email, 형용사+동물 종류+숫자 nickname으로 ACTIVE LOCAL USER를 만들고 기존 토큰 정책에 따라 로그인 상태를 반환한다.
- `GET /api/auth/email-availability?email=`은 start/confirm과 동일한 normalizer와 provider별 오류를 사용한다. Google 계정이 있으면 가입 수단을 알리고, rate limit으로 대량 조회를 제한한다.

## Changes

1. challenge 발행·재발송·확인 use case를 Application에 둔다. Controller는 입력 validation과 HTTP 매핑만 담당한다. Google과 LOCAL에 동일한 email unique 규칙을 적용한다.
2. 인증 전 이탈은 Redis TTL로 끝나고 `users` row와 이메일 선점이 남지 않는다. confirm 때 unique를 다시 검사하고 DB 제약으로 경쟁을 막는다.
3. 동시 confirm은 계정 하나만 만들고, DB commit 뒤 Redis 삭제가 실패해도 재시도가 다른 계정을 만들지 않도록 완료 marker 또는 동등한 멱등 정책을 둔다. DB 실패 시 challenge를 무조건 소모하지 않는다.
4. 발송 제한과 `GOOGLE_ACCOUNT_EXISTS`가 계정 존재를 드러내는 점을 고려해 IP/이메일별 rate limit과 오류 응답을 일관되게 적용한다.
5. 분리된 Swagger interface에 각 endpoint의 인증 없음, body/query, 성공 응답, 만료·오입력·중복·발송 실패의 `error_code`/`custom_error_code`를 문서화한다.

## Acceptance Criteria

- 인증 전 이탈·만료·재시작에 DB USER가 없다. 올바른 코드 한 번의 확인으로만 LOCAL 계정이 생성된다.
- Google 계정과 정규화 이메일이 같으면 자동 연결 없이 Google 가입 안내 오류를 반환한다.
- 재발송 전 코드, 만료 코드, 5회 실패 코드는 거부된다. 동시·반복 confirm에서 중복 계정이 없다.
- 신규 nickname 형식과 unique가 Google 가입과 같다. 성공 후 USER 토큰은 저장된 계정에만 발급된다.

## Verification / Review

- 테스트: 입력 검증, 이메일 정규화, Google 충돌, 이탈/TTL, 발송 실패, 재발송, 경쟁 confirm, DB unique, API/Swagger.
- 사용자 실행: `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`. Codex는 빌드·테스트를 실행하지 않고 코드·diff를 정적으로 검토한다.
