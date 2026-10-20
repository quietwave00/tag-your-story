# AUTH-003 운영 migration runbook

운영 DB는 Oracle이다. AUTH-002 migration 및 사용자 검증 완료 후, AUTH-003 배포 전 쓰기 트래픽을 중지하고 백업/복구 지점을 확보한다. Google pending identity는 Redis에만 존재하며 기존 30일 TTL로 만료시킨다. pending row 생성·변경 migration은 없다.

## Preflight

```sql
select user_id, nickname from users where length(nickname) > 20;
select role, count(*) from users group by role;
select user_status, count(*) from users group by user_status;
select count(*) from users where provider='GOOGLE' and (subject is null or subject<>user_key);
```

첫 번째 쿼리와 마지막 쿼리는 0건이어야 한다. `ROLE_GUEST` 또는 `ROLE_PENDING_USER` 저장 row가 있으면 해당 user_id를 조사하고 별도 승인된 데이터 처리 후 진행한다. 이 runbook은 역할을 임의 승격하지 않는다. 과거 enum과 무관한 비활성 status row는 유지한다.

## 적용

```sql
alter table users modify (nickname varchar2(20 char));
```

기존 `uk_users_nickname` unique 제약은 유지한다. Oracle DDL은 자동 commit이므로 실패 시 자동 rollback되지 않는다. 운영 스키마의 컬럼 타입과 제약 이름을 먼저 확인한다.

## 검증

```sql
select data_length, char_length from user_tab_columns
where table_name='USERS' and column_name='NICKNAME';
select constraint_name, status from user_constraints
where table_name='USERS' and constraint_name='UK_USERS_NICKNAME';
select count(*) from users where length(nickname) > 20;
```

배포 후 새 Google 로그인과 동일 subject 재로그인, 기존 LOCAL/다른 Google 이메일 충돌, 비활성 계정 거부를 점검한다. 이전 pending token을 사용한 `/api/user/register` 호출은 가입을 완료하지 않아야 하며, 사용자는 Google 재로그인을 시작한다.

## 되돌리기

코드 rollback이 필요하면 nickname 컬럼을 이전 길이로 확장한다. AUTH-003 이후 새로 생성된 Google 계정을 pending row로 되돌리거나 병합하지 않는다. 배포 이후 account write가 있었다면 rollback 전에 데이터와 클라이언트 호환성을 검토한다.
