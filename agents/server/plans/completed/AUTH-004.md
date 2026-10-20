# AUTH-004 — 이메일 발송 기반과 가입 코드 저장소

상태: Completed. 2026-10-20 사용자가 구현 및 전체 테스트 완료를 확인하고 Human Review 완료 처리를 요청했다.

2026-10-20 완료 기록: SMTP 발송 기반과 Redis 가입 challenge 저장소를 사용자 검증 결과에 따라 완료 처리했다.

2026-10-16 구현 기록: SMTP port/adapter, HMAC 코드 검증값, Redis Lua 원자 예약·활성화·검증·폐기, 발송 제한 및 실패 보상 정책과 테스트를 작성했다. 공개 API와 Google 로그인 계약은 변경하지 않았다. `./gradlew`와 `scripts/verify.sh`는 실행하지 않았다. AUTH-003 결과 및 이번 단계의 사용자 검증 결과가 필요하므로 완료 처리하지 않는다.

## Goal

자체 가입의 6자리 이메일 인증에 필요한 서버 내부 발송·임시 저장 기반을 마련한다. 아직 공개 가입 API나 DB 사용자를 만들지 않는다.

## Changes

1. 외부 메일 발송 port와 SMTP adapter를 Infrastructure에 둔다. 발송 주소·접속 정보는 환경 설정/secret으로 받으며 로그에 코드·비밀번호·접속 secret을 기록하지 않는다. 테스트에는 fake sender를 쓴다.
2. 가입 challenge 저장소를 Redis에 구현한다. 불투명한 challengeId, 정규화 email, 비밀번호 해시, 코드 검증값, 실패 횟수, 발송 횟수와 만료 시각을 보관한다. 원문 비밀번호·코드는 저장하지 않는다. 6자리 코드 검증값은 작은 코드 공간을 고려해 서버 비밀키 기반으로 만든다.
3. 코드 TTL 10분, 실패 5회 폐기, 재발송 60초 간격과 발송 횟수 상한, 이메일/IP별 rate limit에 필요한 원자 연산을 설계한다. 재발송은 이전 코드를 무효화한다. Redis 만료 시 가입 정보가 자연히 사라진다.
4. 메일 전송은 DB 트랜잭션 밖에서 수행한다. 발송 실패와 Redis 기록 실패의 순서·보상 정책을 정의하고 테스트한다. 메일 서비스 설정이 없으면 성공으로 가장하지 않고 명확한 서비스 오류를 반환할 준비를 한다.
5. 서버 설정과 운영 runbook에 SMTP/secret 설정, 비운영 테스트 방식과 장애 진단을 기록한다.

## Acceptance Criteria

- 원문 코드·비밀번호가 DB·Redis·로그에 없다.
- 6자리 코드, 만료, 실패 제한, 재발송 교체와 cooldown이 동시 요청에서도 일관된다.
- SMTP 장애가 성공으로 보고되지 않고 DB 트랜잭션을 열린 채 외부 전송을 기다리지 않는다.
- 공개 API·기존 Google 로그인 계약은 변경되지 않는다.

## Verification / Review

- 테스트: fake sender, Redis TTL·원자성, 코드 검증값, 재발송/실패 횟수/제한, 발송 장애.
- 사용자 실행: `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`. Codex는 빌드·테스트를 실행하지 않고 코드·diff를 정적으로 검토한다.
