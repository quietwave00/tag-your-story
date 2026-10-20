# AUTH-008 운영 적용 기록

운영 Oracle의 기존 `users` row와 콘텐츠 FK를 유지한다. `WITHDRAWN`은 기존 `user_status varchar2(255)`에 저장할 수 있으므로 상태 컬럼 DDL 변경은 없다. `nickname`은 null을 허용해야 한다. Google의 `(provider, subject)` 제약과 `subject = user_key`, email/user_key/nickname unique 제약은 AUTH-002 적용 상태를 전제로 한다. 이전 단계의 사용자 검증·운영 적용 여부를 확인한 뒤 배포한다.

## Preflight (읽기 전용)

```sql
select constraint_name, search_condition_vc from user_constraints
where table_name = 'USERS' and constraint_type = 'C';
select column_name, nullable, data_length from user_tab_columns
where table_name = 'USERS' and column_name in
('EMAIL','USER_KEY','SUBJECT','NICKNAME','PASSWORD_HASH','USER_STATUS');
select index_name, uniqueness from user_indexes where table_name = 'USERS';
select user_status, count(*) from users group by user_status;
select count(*) from users where email like 'withdrawn+%@invalid.local'
   or user_key like 'withdrawn:%';
```

마지막 쿼리의 기존 tombstone 형태 row는 user_id별 충돌 여부를 조사한다. `nickname`이 NOT NULL이면 쓰기 트래픽을 중지하고 백업/복구 지점 확보 후 `alter table users modify (nickname null);`을 적용한다. Oracle DDL은 자동 commit된다. 다른 unique/check 제약이 없는지 preflight 결과로 확인한다. `init_schema.sql`은 로컬 MySQL 기준 DDL이며 운영 DB에 실행하지 않는다.

## 배포와 검증

애플리케이션 배포 후 테스트 계정을 탈퇴시켜 `users.user_id`가 그대로이고 `user_status='WITHDRAWN'`, `email='withdrawn+'||user_id||'@invalid.local'`, `user_key='withdrawn:'||user_id`, Google `subject=user_key` / LOCAL `subject is null`, `password_hash/nickname is null`인지 확인한다. 이전 email과 Google subject의 신규 가입은 별도 user_id여야 한다. 이전 access/refresh token 및 LOCAL/Google 로그인을 거부하는지 확인한다. Board/Comment/UserTag/Like/Notification FK와 공개 조회, 작성자명 `탈퇴한 사용자`를 점검한다. Redis 장애 시에도 DB 상태가 인증의 최종 기준이다.

```sql
select user_id, provider, subject, user_key, email, nickname, password_hash, user_status
from users where user_id = :withdrawn_user_id;
select count(*) from board where user_id = :withdrawn_user_id;
select count(*) from user_tag where user_id = :withdrawn_user_id;
select count(*) from likes where user_id = :withdrawn_user_id;
```

## 되돌리기

코드만 이전 버전으로 되돌리면 이전 버전은 `WITHDRAWN` enum을 해석하지 못할 수 있으므로, 탈퇴 row가 발생한 뒤에는 이전 버전 단독 롤백을 하지 않는다. 새 버전의 탈퇴 endpoint를 먼저 차단하고 호환 코드를 배포한다. 비식별화된 신원값은 애플리케이션이나 DB만으로 복원할 수 없다. 법적·운영적 복구가 승인된 경우에만 백업의 신원값을 user_id에 맞춰 복원하고 unique 충돌 및 재가입 여부를 수동 확인한다. FK row를 삭제하거나 신규 계정에 과거 콘텐츠를 자동 연결하지 않는다.
