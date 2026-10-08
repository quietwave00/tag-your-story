# USER-TAG-001 Existing Data Migration Runbook

## Purpose

기존 전역 공유 `user_tag`를 Board 작성자별 `(user_id, name)` exact-name
identity로 분리한다. `name`은 사용자가 입력한 값을 변형하지 않고 보존한다.
운영 DB는 `ddl-auto=validate`이므로 애플리케이션 배포 전에 별도로 수행한다.

## Preconditions

- 쓰기 트래픽을 중지한다.
- DB backup 또는 point-in-time recovery 지점을 만든다.
- migration 전 Board별 태그 이름을 audit table로 보존한다.

```sql
create table migration_user_tag_001_before as
select but.board_id, ut.name
from board_user_tag but
join user_tag ut on ut.user_tag_id = but.user_tag_id;
```

## Migration

대상 MySQL의 실제 constraint/index 이름을 확인한 뒤 maintenance window에서
수행한다. `name` 비교와 unique는 입력값 그대로의 대소문자·Unicode byte 차이를
보존하기 위해 `utf8mb4_bin` collation을 사용한다.

```sql
alter table user_tag
    add column user_id bigint null,
    modify name varchar(255) character set utf8mb4 collate utf8mb4_bin not null;

create table migration_user_tag_001_identity (
    user_id bigint not null,
    name varchar(255) character set utf8mb4 collate utf8mb4_bin not null,
    first_created_at datetime(6) not null,
    new_user_tag_id bigint null,
    primary key (user_id, name)
);

insert into migration_user_tag_001_identity (user_id, name, first_created_at)
select b.user_id, ut.name, min(but.created_at)
from board_user_tag but
join board b on b.board_id = but.board_id
join user_tag ut on ut.user_tag_id = but.user_tag_id
group by b.user_id, ut.name;

insert into user_tag (user_id, name, created_at, updated_at)
select user_id, name, first_created_at, current_timestamp(6)
from migration_user_tag_001_identity;

update migration_user_tag_001_identity i
join user_tag ut on ut.user_id = i.user_id and ut.name = i.name
set i.new_user_tag_id = ut.user_tag_id;

update board_user_tag but
join board b on b.board_id = but.board_id
join user_tag old_ut on old_ut.user_tag_id = but.user_tag_id
join migration_user_tag_001_identity i
  on i.user_id = b.user_id and i.name = old_ut.name
set but.user_tag_id = i.new_user_tag_id;

delete duplicate_link
from board_user_tag duplicate_link
join board_user_tag kept_link
  on kept_link.board_id = duplicate_link.board_id
 and kept_link.user_tag_id = duplicate_link.user_tag_id
 and kept_link.board_user_tag_id < duplicate_link.board_user_tag_id;

delete ut
from user_tag ut
left join migration_user_tag_001_identity i
  on i.new_user_tag_id = ut.user_tag_id
where i.new_user_tag_id is null;

alter table user_tag
    modify user_id bigint not null,
    add constraint fk_user_tag_owner foreign key (user_id) references users(user_id),
    add constraint uk_user_tag_owner_name unique (user_id, name),
    add index idx_user_tag_name_owner (name, user_id);

alter table board_user_tag
    add constraint uk_board_user_tag_board_tag unique (board_id, user_tag_id),
    add index idx_board_user_tag_tag_board (user_tag_id, board_id);
```

## Verification

아래 query가 모두 `0`을 반환해야 한다.

```sql
select count(*)
from board_user_tag but
join board b on b.board_id = but.board_id
join user_tag ut on ut.user_tag_id = but.user_tag_id
where ut.user_id <> b.user_id;

select count(*)
from (
    select user_id, name
    from user_tag
    group by user_id, name
    having count(*) > 1
) duplicate_identity;

select count(*)
from (
    select board_id, user_tag_id
    from board_user_tag
    group by board_id, user_tag_id
    having count(*) > 1
) duplicate_link;

select count(*)
from board_user_tag but
left join board b on b.board_id = but.board_id
left join user_tag ut on ut.user_tag_id = but.user_tag_id
where b.board_id is null or ut.user_tag_id is null;
```

`migration_user_tag_001_before`와 migration 후 결과의 Board별 exact `name` 집합도
대조한다. mismatch가 있으면 배포하지 않고 backup으로 복구한다. 검증 완료 후에만
staging/audit table을 제거한다.

```sql
drop table migration_user_tag_001_identity;
drop table migration_user_tag_001_before;
```
