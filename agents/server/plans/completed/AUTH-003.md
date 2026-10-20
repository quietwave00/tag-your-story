# AUTH-003 — Google 즉시 가입과 임시 역할 제거

상태: Completed. 2026-10-20 사용자가 구현 및 전체 테스트 완료를 확인하고 Human Review 완료 처리를 요청했다.

2026-10-20 완료 기록: Google 즉시 가입, 임시 역할 제거 및 DB canonical 인증을 사용자 검증 결과에 따라 완료 처리했다.

2026-10-15 구현 기록: Google 즉시 가입 및 닉네임 생성·충돌 재시도, provider별 이메일 충돌, pending/GUEST 제거, DB canonical role/status 인증, OAuth 오류 redirect·Swagger, nickname DDL·Oracle runbook, 관련 테스트를 작성했다. `./gradlew`와 `scripts/verify.sh`는 실행하지 않았다. AUTH-002 사용자 재검증 결과 및 운영 migration 적용 여부를 먼저 확인해야 한다.

2026-10-15 사용자 검증 결과: `:tagnote-api:compileJava`가 `GoogleOauthOpenApi.java`의 닫는 괄호 누락으로 실패했다. OpenAPI 생성 코드를 지역 변수로 분리해 문법 오류를 수정했다. 사용자 재실행 결과 대기 중이다. `CommentEntity`의 Lombok 경고와 `javax.annotation.meta.When` 경고는 이번 컴파일 실패 원인과 별개다.

2026-10-15 재실행 결과: Swagger 문법 오류는 해소되었고 `PrincipalDetails`의 Lombok `@AllArgsConstructor`와 명시적 3인자 생성자가 중복되어 `:tagnote-api:compileJava`가 실패했다. 중복 Lombok 생성자 선언을 제거했다. 사용자 재실행 결과 대기 중이다.

2026-10-15 재실행 결과: API 테스트 42건 중 익명 Board 쓰기 차단 2건이 anonymous principal의 `String`에 `@CurrentUserId`의 `userId` SpEL을 적용하면서 실패했다. ROLE_GUEST를 되살리지 않고 null userId를 가진 anonymous principal을 Security 설정과 WebMvc 테스트 설정에 지정했다. 사용자 재실행 결과 대기 중이다.

## Goal

Google OAuth 성공 시 랜덤 닉네임으로 즉시 USER 계정을 생성한다. `GUEST`와 `PENDING_USER`를 서버에서 제거한다.

## Changes

1. 기존 Google 재로그인, pending 생성·닉네임 완료, 익명 공개 API를 특성화 테스트로 고정한다. ADR-014와 실제 DB identity를 읽고 충돌이 있으면 보고한다.
2. Google userinfo HTTP 호출은 DB transaction 밖에서 마치고 `sub`와 `email_verified=true`, 비어 있지 않은 email을 확인한다. provider subject로 기존 계정을 찾고, 없으면 정규화 이메일 충돌을 확인한다. 기존 LOCAL 계정이나 다른 Google subject와 충돌하면 자동 연결 없이 provider별 오류를 반환한다. 기존 subject의 Google 이메일이 바뀌어도 계정 이메일을 임의로 덮어쓰지 않는다.
3. 신규 계정 nickname은 서버 형용사 목록 + 동물 종류 목록 + 4~6자리 난수로 만든다(예: `푸른여우4821`). 기존 1~7자 닉네임 제한을 1~20자로 정렬하고 DB unique를 보장한다. 충돌 시 별도 트랜잭션에서 제한된 횟수만 재시도한다. 성공한 DB commit 뒤에만 USER 토큰을 발급한다. 동일 subject 동시 callback은 하나의 계정으로 귀결된다.
4. Google pending Redis/JWT, 닉네임 완료 `/api/user/register`, `ROLE_PENDING_USER`, `ROLE_GUEST`와 GUEST 전용 test endpoint의 서버 의존성을 제거한다. 미인증 요청은 anonymous로 처리한다. 과거 pending JWT는 거부하고 Google 재로그인을 안내하며 Redis pending 정보는 기존 TTL로 만료시킨다. DB pending 계정 migration은 없다.
5. 저장된 role과 DB canonical status에 따라 JWT principal을 복원하고 비활성 계정의 로그인·인증을 거부한다. 365일 USER cache의 오래된 status로 인증을 허용하지 않는다. 명세 권한 절과 ADR을 정렬한다. Google callback/오류의 Swagger 문서 및 기존 API 계약 변경을 기록한다.

## Acceptance Criteria

- 신규 Google 가입은 닉네임 입력 없이 바로 ACTIVE USER이며 재로그인 시 같은 userId·nickname을 사용한다.
- 검증되지 않은 이메일, 기존 다른 provider/subject의 정규화 이메일 충돌은 계정을 만들지 않는다.
- GUEST/PENDING_USER 역할·pending token 발급·가입 완료 API가 서버에서 사라지고 익명 조회/USER 보호가 유지된다.
- 과거 pending JWT가 USER 권한을 얻지 못한다. 가입 경쟁과 닉네임 unique 충돌에서 중복 row가 없다.
- Controller/Service/JPA 테스트와 Swagger가 실제 응답·오류와 일치한다.

## Verification / Review

- 테스트: provider 충돌, 이메일 정규화, subject 재로그인, 닉네임 형식·중복·동시성, 익명/USER 권한, old pending 거부.
- 사용자 실행: `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`. Codex는 빌드·테스트를 실행하지 않고 코드·diff를 정적으로 검토한다.
