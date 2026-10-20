# AUTH-002 — 계정 identity와 이메일 정규화 기반

상태: Completed. 2026-10-20 사용자가 구현 및 전체 테스트 완료를 확인하고 Human Review 완료 처리를 요청했다.

2026-10-20 완료 기록: 계정 identity, 이메일 정규화, DB 제약 및 migration 문서를 사용자 검증 결과에 따라 완료 처리했다.

2026-10-15 사용자 실행 결과: 전달된 API 실패 목록과 달리 현재 저장된 API XML 리포트는 36건 통과다. 이어진 core XML 리포트는 4건 실패했으며 모두 email 없는 `User`의 관계 참조용 `toEntity()`에서 AUTH-002 정규화가 예외를 던진 회귀였다. null email의 참조 변환을 허용하고 실제 저장 시 Entity 콜백으로 검증하도록 수정했다. 재실행 결과를 기다린다.

## Goal

Google과 자체 가입이 하나의 `users` 테이블에서 충돌 없이 공존하도록 식별자·이메일·DB 제약을 준비한다. 공개 가입 흐름은 이 단계에서 변경하지 않는다.

## Decision / Scope

- 이메일은 서버의 단일 normalizer에서 `trim` 후 `Locale.ROOT`로 소문자화한다. 형식 검증, 저장, 조회, 중복 확인, 향후 로그인과 Google OAuth가 같은 함수를 사용한다. Gmail 점/`+` 별칭 처리는 하지 않는다.
- 계정은 provider `GOOGLE | LOCAL`, Google의 안정 식별자는 `(provider, subject)`다. 동일 정규화 이메일은 한 계정만 소유한다. 이메일 일치만으로 서로 다른 가입 방식을 연결하지 않는다.
- 기존 `user_key`의 실제 Google `sub` 의미와 외부 노출 여부를 확인하고 기존 계정의 provider/subject를 backfill한다. `user_key`를 임의로 재발급해 기존 참조를 깨지 않는다.
- 이 단계에서 DB/JPA와 migration runbook, 이메일 normalizer 및 identity 조회만 변경한다. 기존 Google pending 등록과 JWT 계약은 유지한다.

## Changes

1. `ADR-014-auth-account-identity.md`에 `## Decision`, `## Reason`, `## Consequence`로 identity·정규화·자동 연결 금지·기존 key 의미를 기록한다. `server_spec.md` User schema와 충돌을 명시적으로 정렬한다.
2. 기존 `users`의 `trim+lowercase(email)` 중복, `user_key` 중복, nickname 중복·null, Google subject 누락을 운영 migration 전에 점검한다. 중복 사용자 row를 임의 병합·삭제하지 않고 충돌 목록과 처리 절차를 남긴다.
3. 운영 DB와 `init_schema.sql`, JPA mapping을 맞춰 정규화 email unique, provider/subject unique, user_key unique, nickname unique를 둔다. LOCAL의 provider subject는 null 가능하다. password hash는 LOCAL 가입을 위해 nullable로 준비하되 이 단계에서 생성하지 않는다.
4. 기존 Google row backfill과 제약 적용 순서, rollback/preflight query를 문서화한다. 개발 DB와 운영 DB 변경 방식을 구분한다.
5. normalizer 단위 테스트와 DB unique·조회 통합 테스트를 추가한다. 기존 Google 로그인 및 Board/UserTag FK 관계를 특성화한다.

## Acceptance Criteria

- ` User@Example.com `과 `user@example.com`은 동일한 저장·조회 identity다.
- DB가 같은 정규화 이메일, 같은 Google subject, user_key, nickname 중복을 막는다.
- 기존 계정의 Google subject와 userId, Board/UserTag FK가 유지된다.
- 기존 데이터 충돌이 있으면 migration이 조용히 병합하지 않고 적용 전 중단한다.
- 공개 API와 기존 Google pending 플로우는 변경되지 않는다.

## Verification / Review

- 테스트: 정규화·형식 오류, provider subject/email unique 경쟁, 기존 row backfill 예시, FK 유지.
- 사용자 실행: `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`. Codex는 빌드·테스트를 실행하지 않고 코드·DDL·diff를 정적으로 검토한다.
