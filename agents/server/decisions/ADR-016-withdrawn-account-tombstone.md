# ADR-016 — Withdrawn account tombstone and public author name

## Decision

- 탈퇴한 계정의 `user_id`와 콘텐츠 FK를 유지하고 `user_status=WITHDRAWN`으로 둔다. `ACTIVE` 사용자만 탈퇴할 수 있으며 상태 변경과 식별자 정리는 같은 DB transaction에서 확정한다.
- 탈퇴 시 `user_key='withdrawn:' || user_id`, `email='withdrawn+' || user_id || '@invalid.local'`로 치환하고 Google `subject=user_key`, LOCAL `subject=null`로 둔다. `password_hash`와 `nickname`은 null로 지운다. 이는 ADR-014의 활성 Google 계정 `user_key=sub` 보존 규칙에 대한 탈퇴 상태 예외다. 원래 email과 Google subject의 신규 가입은 새 user_id를 받는다.
- 공개 Board/Comment의 작성자명은 저장된 nickname이 아닌 현재 user status로 결정한다. `WITHDRAWN`은 `탈퇴한 사용자`로 표시한다. 콘텐츠 본문과 연결 태그는 유지한다.
- DB의 현재 상태가 인증과 쓰기의 기준이다. Redis token/cache 제거는 DB commit 뒤에 시도하며 실패해도 탈퇴를 되돌리지 않는다.
- 탈퇴 상태에서 기존 access token과 중복 탈퇴를 시도하면 `ACCOUNT_WITHDRAWN`/HTTP 401, row가 없으면 `USER_NOT_FOUND`/HTTP 404다. 탈퇴 외 비활성 상태의 기존 403 정책은 유지한다.

## Reason

기존 Board, Comment, UserTag, Like, Notification은 user_id를 참조한다. row 삭제는 FK와 집계를 깨뜨린다. 기존 email, Google subject, user_key를 그대로 두면 재가입을 막고 식별 정보가 남는다. nickname을 모든 탈퇴자에게 같은 문자열로 저장하면 unique 제약과 충돌한다.

## Consequence

- 기존 user_id를 통한 과거 콘텐츠 소유권을 새 계정에 자동 연결하지 않는다.
- DB의 unique/not-null 및 Google subject check를 유지할 수 있다. 운영 DB의 nullable nickname과 실제 constraint를 배포 전에 확인한다.
- 비식별화 이전 값은 애플리케이션 DB에서 복원할 수 없다. 운영 복구는 별도 승인된 백업 절차를 따른다.
