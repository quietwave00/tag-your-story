# ADR-003 — UserTag Exact-Name Playlist Identity

## Decision

`UserTag`는 사용자가 입력한 이름을 변형하지 않고 그대로 사용하는 사용자 소유
플레이리스트형 분류다.

- UserTag identity는 `(user_id, name)`이다.
- `name`에는 trim, lowercase, Unicode normalization, 공백 축약을 적용하지 않는다.
- 같은 사용자가 정확히 같은 `name`을 다시 사용하면 기존 `user_tag_id`를 재사용한다.
- 대소문자, Unicode 표현 또는 앞뒤·연속 공백이 다른 이름은 서로 다른 태그다.
- 서로 다른 사용자의 같은 `name`은 서로 다른 `user_tag_id`를 가진다.
- DB는 `UNIQUE(user_id, name)`으로 동일 owner의 exact-name 중복만 방어한다.
- 동시 unique 충돌로 실패한 Board 생성·수정 요청을 자동 재시도하지 않는다.
- 기존 공개 `GET /api/boards/user-tags?userTagName=...`는 입력 이름과 정확히 같은
  이름을 가진 모든 사용자의 POST Board를 반환한다. 각 Board의 `trackId`를 통해
  해당 이름으로 분류된 Track을 조회할 수 있다.

이 결정은 ADR-002의 owner-scoped identity와 System Tag 분리 결정은 유지하되,
`normalized_name` identity, normalization 검색 및 unique 충돌 자동 복구 결정을
대체한다.

## Reason

UserTag는 공용 taxonomy나 의미 기반 alias가 아니라 사용자가 직접 이름 붙인 개인
컬렉션이다. 예를 들어 사용자가 여러 Track의 Board에 `"1월"`을 붙이면 해당
사용자의 `"1월"` 태그는 그 Board와 Track을 묶는 플레이리스트처럼 동작한다.

사용자가 입력한 문자열 자체가 이름의 의미이므로 `"Jazz"`, `"jazz"`,
`"  Jazz  "`를 서버가 임의로 같은 태그로 합치지 않는다. 중복 요청이나 Board
멱등성은 UserTag normalization/retry가 아니라 별도 클라이언트 또는 Board
idempotency 정책에서 다룬다.

## Consequence

- `user_tag.normalized_name`과 UserTag 전용 normalizer/value object를 제거한다.
- owner-scoped 생성·재사용과 조회는 `(user_id, name)` exact match를 사용한다.
- DB collation도 exact-name unique와 조회 의미가 유지되도록 case-sensitive binary
  비교를 사용한다.
- 같은 요청 안에서는 완전히 동일한 문자열만 중복 제거한다.
- blank/null 이름은 유효한 태그 이름이 아니므로 계속 거부하지만 유효한 입력값은
  저장 전에 변형하지 않는다.
- UserTag unique 충돌 translator와 Board write 재시도 조율을 제거한다.
- 기존 데이터 migration은 normalization 없이 Board 작성자와 원본 `name`을 기준으로
  owner별 UserTag를 생성하고 연결을 이전한다.
- System Tag taxonomy의 normalization, alias 및 Resolver 정책에는 영향이 없다.
