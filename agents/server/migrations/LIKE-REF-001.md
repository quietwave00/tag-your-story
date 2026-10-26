# LIKE-REF-001 운영 migration

`init_schema.sql`은 로컬 MySQL 기준이다. 아래 운영 SQL은 Oracle 기준이며 실제 제약·인덱스 이름과 데이터 타입을 preflight에서 확인한 뒤 적용한다. `likes`를 `board_like`로 이름 변경하여 기존 `like_id`, User/Board FK와 row를 보존한다. 애플리케이션 쓰기를 중지하고 백업 및 복구 지점을 확보한 뒤 진행한다. Oracle DDL은 자동 commit된다.

## Preflight

```sql
select count(*) as old_count from likes;
select user_id, board_id, count(*) as duplicates
from likes group by user_id, board_id having count(*) > 1;
select l.like_id from likes l left join users u on u.user_id = l.user_id
left join board b on b.board_id = l.board_id
where u.user_id is null or b.board_id is null;
select b.board_id, b.like_count, count(l.like_id) as actual_count
from board b left join likes l on l.board_id = b.board_id
group by b.board_id, b.like_count
having b.like_count <> count(l.like_id);
select constraint_name, constraint_type from user_constraints where table_name = 'LIKES';
select index_name from user_indexes where table_name = 'LIKES';
select column_name, data_type, nullable from user_tab_columns where table_name = 'LIKES';
```

중복 쌍·orphan·counter 차이가 있으면 migration을 중단한다. 중복은 실제 사용자 의도를 확인하여 보존할 행을 결정하고, 제거할 `like_id`를 감사 기록에 남긴 뒤 별도 승인된 데이터 정정으로 처리한다. counter는 정정 후 관계 수와 비교하고, 불일치 원인을 조사해 승인받은 Board만 재계수한다. 임의 행 삭제나 전체 counter 덮어쓰기를 이 migration에 넣지 않는다. preflight의 세 이상 조회가 모두 0건이 될 때만 계속한다.

## Oracle 적용

```sql
alter table likes rename to board_like;
alter table board_like add (created_at timestamp(6));
update board_like set created_at = systimestamp where created_at is null;
commit;
alter table board_like modify (created_at not null);
alter table board_like add constraint uk_board_like_user_board unique (user_id, board_id);
create index idx_board_like_user_created on board_like (user_id, created_at);
create index idx_board_like_board_created on board_like (board_id, created_at);
```

기존 FK/PK는 이름 변경 후에도 남는다. 위 SQL 전후에 `user_constraints`에서 FK가 각각 `USERS`와 `BOARD`를 참조하고 PK가 `LIKE_ID`인지 확인한다. `created_at`이 없던 과거 행은 backfill 시각으로 표시되므로 과거 실제 좋아요 순서를 복원할 수 없다. 동률은 `like_id DESC`로 정렬한다. 배포 이후 애플리케이션은 새 행의 시각을 기록한다.

로컬 MySQL 운영 환경에서는 위 Oracle SQL을 그대로 실행하지 않는다. 쓰기 중지·동일한 preflight·백업 후 `RENAME TABLE likes TO board_like`, `ALTER TABLE board_like ADD COLUMN created_at datetime(6) NULL`, `UPDATE ... SET created_at = CURRENT_TIMESTAMP(6)`, `MODIFY ... NOT NULL`, 같은 unique/index 추가를 해당 DB 문법으로 적용한다.

## 후검증

```sql
select count(*) as new_count from board_like;
select count(*) as null_created_at from board_like where created_at is null;
select user_id, board_id, count(*) from board_like
group by user_id, board_id having count(*) > 1;
select b.board_id, b.like_count, count(l.like_id) as actual_count
from board b left join board_like l on l.board_id = b.board_id
group by b.board_id, b.like_count
having b.like_count <> count(l.like_id);
select constraint_name, constraint_type from user_constraints where table_name = 'BOARD_LIKE';
select index_name from user_indexes where table_name = 'BOARD_LIKE';
```

`new_count`는 `old_count`와 같고, 나머지 이상 조회는 0건이어야 한다. PK, 두 FK, unique, 두 index를 확인한 후 애플리케이션을 배포하고 단일 등록·중복 409·재취소·목록을 확인한다.

## 복구

새 버전에서 Like 쓰기를 중지한 뒤 백업 복구 지점으로 DB와 애플리케이션을 함께 되돌린다. 새 테이블에 배포 후 Like row가 생겼으면 복구 전에 차이를 추출해 보존 방안을 승인받는다. 테이블만 되돌리면 신규 좋아요가 유실될 수 있다.
