# ADR-014 — Account identity and normalized email

## Decision

- `users.user_id`는 기존 Board/UserTag FK의 내부 PK로 유지한다.
- 기존 Google `user_key`는 실제 Google `sub`다. 기존 값을 재발급하지 않고 `provider=GOOGLE`, `subject=user_key`로 backfill한다. 명세의 공개 UUID 설명을 대체하며 새 공개 API에 `user_key`를 노출하지 않는다.
- 새 LOCAL 계정은 별도의 내부 `user_key`를 갖고 `subject`는 null이다. Google identity는 `(provider, subject)`이며 provider/subject, user_key, nickname은 각각 unique다.
- 이메일은 단일 `EmailNormalizer`에서 trim 후 `Locale.ROOT` 소문자화하고, 저장·형식 검증·조회·중복 확인에 같은 결과를 쓴다. Gmail 점/`+` 별칭은 합치지 않는다. 정규화 이메일은 전체 계정에서 unique다.
- 이메일만 일치할 때 계정을 자동 연결하지 않는다. 이후 가입 유스케이스가 기존 이메일의 provider를 확인하고 명시적 오류를 반환한다.
- `password_hash`는 LOCAL 가입 준비를 위한 nullable 컬럼이며 AUTH-002는 비밀번호를 생성하지 않는다.

## Reason

현재 OAuth 코드는 Google `sub`를 `user_key`로 조회·저장한다. 공개 UUID로 가정하고 다시 발급하면 로그인과 기존 참조를 깨뜨린다. DB unique 제약이 없으면 정규화가 다른 이메일이나 중복 subject가 동시 가입에서 공존할 수 있다.

## Consequence

- 기존 데이터 충돌 preflight와 수동 해결 후 제약을 적용한다. 사용자 row 병합·삭제는 이 migration에 포함하지 않는다.
- 기존 Google pending/JWT API는 그대로 유지한다. LOCAL 가입·로그인과 Google 즉시 가입은 후속 Plan에서 구현한다.
- `server_spec.md`의 과거 `user_key` 공개 UUID 설명은 이 ADR로 대체한다.
