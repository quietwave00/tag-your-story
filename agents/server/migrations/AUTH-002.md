# AUTH-002 운영 migration runbook

운영 DB는 Oracle(`prod` profile)이다. `init_schema.sql`은 로컬 MySQL 형태의 기준 DDL이며 운영 DB에 실행하지 않는다. AUTH-001 검증 결과는 아직 기록되지 않았으므로 배포 순서를 확인하고 AUTH-002 배포 전에 이 절차를 수행한다. 쓰기 트래픽을 중지하고 백업/복구 지점을 만든다. Oracle DDL은 자동 commit되므로 각 단계의 실패를 트랜잭션 rollback으로 취소할 수 없다.

## Preflight (기존 schema, 읽기 전용)

모든 쿼리는 0건이어야 한다. 결과의 `user_id`를 충돌 목록에 기록하고 소유권을 확인해 별도 수동 처리한다. 이 runbook은 row 병합·삭제나 임의 nickname 변경을 하지 않는다. `lower(trim(email))`은 운영 데이터의 ASCII 이메일을 위한 SQL 근사값이다. 비 ASCII 이메일이 있으면 Java `Locale.ROOT` 결과와 대조한 뒤 별도 승인된 정리 절차를 만든다.

```sql
select lower(trim(email)) as normalized_email, count(*) as rows_count,
       listagg(user_id, ',') within group (order by user_id) as user_ids
from users group by lower(trim(email)) having count(*) > 1;

select user_key, count(*) as rows_count,
       listagg(user_id, ',') within group (order by user_id) as user_ids
from users group by user_key having count(*) > 1;

select nickname, count(*) as rows_count,
       listagg(user_id, ',') within group (order by user_id) as user_ids
from users where nickname is not null
group by nickname having count(*) > 1;

select user_id, email, user_key, nickname from users
where email is null or trim(email) is null or instr(trim(email), '@') = 0
   or length(trim(email)) > 255 or user_key is null or trim(user_key) is null;

select user_id, email from users where regexp_like(email, '[^ -~]');
```

마지막 비 ASCII 목록은 0건이거나 Java normalizer와 비교 완료되어야 한다. nickname null은 기존 schema에서 허용되지 않았지만 새 schema에서는 허용된다. 실제 null 건수도 확인한다.

```sql
select count(*) as null_nicknames from users where nickname is null;
select count(*) as users_before from users;
select count(*) as board_orphans from board b left join users u on u.user_id=b.user_id where u.user_id is null;
select count(*) as tag_orphans from user_tag t left join users u on u.user_id=t.user_id where u.user_id is null;
```

## 적용 (Oracle)

preflight 충돌이 해결되고 애플리케이션 쓰기가 멈춘 뒤 실행한다. 기존 제약·인덱스 이름을 `user_constraints`/`user_indexes`에서 확인해 동일 제약을 중복 추가하지 않는다. 각 unique 생성 오류는 적용 중단 신호다.

```sql
alter table users add (provider varchar2(16), subject varchar2(255), password_hash varchar2(255));
update users set provider = 'GOOGLE', subject = user_key;
update users set email = lower(trim(email));
commit;

select user_id from users where provider <> 'GOOGLE' or subject <> user_key
   or email <> lower(trim(email));

alter table users modify (provider not null);
alter table users add constraint uk_users_email unique (email);
alter table users add constraint uk_users_user_key unique (user_key);
alter table users add constraint uk_users_nickname unique (nickname);
alter table users add constraint uk_users_provider_subject unique (provider, subject);
alter table users add constraint ck_users_provider_subject check (
    (provider = 'GOOGLE' and subject is not null and subject = user_key)
    or (provider = 'LOCAL' and subject is null)
);
alter table users add constraint ck_users_email_normalized check (email = lower(trim(email)));
create index idx_users_status on users (user_status);
```

Google subject는 ASCII 및 case-sensitive identity다. 운영 Oracle column collation이 case-insensitive이면 subject와 user_key의 binary collation 지원 여부를 확인하고 unique 제약을 binary 비교로 생성해야 한다. 지원되지 않으면 적용을 중단한다. 기존 `idx_user_key`는 unique index 생성 후 불필요하면 별도 검토 후 제거한다. `idx_users_status`가 이미 있으면 중복 생성하지 않는다. `nickname`의 null 허용 여부는 현재 Oracle schema를 확인한다. NOT NULL이면 `alter table users modify (nickname null)`을 실행한다.

## 검증

```sql
select count(*) as users_after from users;
select count(*) as invalid_identity from users
where provider is null or (provider='GOOGLE' and (subject is null or subject<>user_key))
   or (provider='LOCAL' and subject is not null) or email<>lower(trim(email));
select count(*) as board_orphans from board b left join users u on u.user_id=b.user_id where u.user_id is null;
select count(*) as tag_orphans from user_tag t left join users u on u.user_id=t.user_id where u.user_id is null;
```

`users_after`는 `users_before`와 같고 나머지는 0이어야 한다. Google sample의 `(user_id,user_key)` 및 Board/UserTag의 `(row_id,user_id)`를 백업 snapshot과 비교한다. OAuth 기존 가입과 pending 등록 회귀는 사용자 실행 테스트로 확인한다.

## 되돌리기

새 코드 배포 전이라면 추가한 constraint를 역순으로 drop한 뒤 백업의 원본 email을 `user_id` 기준으로 복원하고 신규 세 컬럼을 drop한다. 신규 코드가 이미 LOCAL row를 썼거나 운영 쓰기를 재개했다면 이 단순 rollback을 실행하지 않고 백업 복원 계획과 데이터 변경 목록을 재검토한다. `user_id`, `user_key`, Board/UserTag FK는 어느 단계에서도 재발급하지 않는다.
