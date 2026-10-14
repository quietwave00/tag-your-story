# BOARD-TRACK-TAG-001 Migration Runbook

## Purpose

legacy `board.track_id` Spotify 문자열을 내부 Catalog Track FK로 전환하고 Album Board를 위한 nullable `album_id` FK와 exactly-one constraint를 추가한다.

외부 API를 migration transaction 안에서 호출하지 않는다. 아래 preflight가 실패하면 migration을 중단한다.

## Preflight

```sql
select count(*) as board_count from board;

select b.board_id, b.track_id
from board b
left join track t on t.spotify_id = b.track_id
where b.track_id is null
   or trim(b.track_id) = ''
   or t.track_id is null;
```

두 번째 query 결과는 반드시 0건이어야 한다. 미매칭 Board를 삭제하거나 임의 Track에 연결하지 않는다. 필요한 Catalog Track은 migration 전에 별도 승인된 import 절차로 확보한다.

Spotify ID unique 제약 때문에 하나의 legacy Board는 최대 하나의 Track에 매칭되어야 한다.

```sql
select b.board_id, count(t.track_id) as matched_tracks
from board b
join track t on t.spotify_id = b.track_id
group by b.board_id
having count(t.track_id) <> 1;
```

결과는 0건이어야 한다.

## Migration

운영 DB 버전이 CHECK constraint를 실제 enforce하는지 먼저 확인한다. enforce하지 않는 버전이면 이 SQL을 실행하지 않고 대체 DB 제약을 별도 승인받는다.

```sql
alter table board add column catalog_track_id bigint null;

update board b
join track t on t.spotify_id = b.track_id
set b.catalog_track_id = t.track_id;

select count(*)
from board
where catalog_track_id is null;
```

마지막 결과가 0일 때만 다음 단계로 진행한다.

```sql
alter table board
    add column album_id bigint null,
    add constraint fk_board_album foreign key (album_id) references album(album_id),
    add index idx_board_album_created (album_id, created_at);

alter table board drop column track_id;
alter table board change column catalog_track_id track_id bigint null;

alter table board
    add constraint fk_board_track foreign key (track_id) references track(track_id),
    add constraint ck_board_exactly_one_subject check (
        (track_id is not null and album_id is null)
        or (track_id is null and album_id is not null)
    ),
    add index idx_board_track_created (track_id, created_at),
    add index idx_board_user_created (user_id, created_at),
    add index idx_board_status_created (status, created_at),
    add index idx_board_like_count (like_count);
```

인덱스가 이미 존재하는 환경에서는 이름과 column 구성을 확인한 뒤 중복 생성 구문만 제외한다.

## Verification

```sql
select count(*)
from board
where (track_id is null and album_id is null)
   or (track_id is not null and album_id is not null);

select count(*)
from board b
left join track t on t.track_id = b.track_id
left join album a on a.album_id = b.album_id
where (b.track_id is not null and t.track_id is null)
   or (b.album_id is not null and a.album_id is null);
```

두 query는 모두 0을 반환해야 한다. 전체 Board row 수는 preflight의 `board_count`와 같아야 한다.

legacy Board의 의미 보존은 migration 전에 별도로 보관한 `(board_id, spotify track id)` snapshot과 다음 결과를 비교한다.

```sql
select b.board_id, t.spotify_id
from board b
join track t on t.track_id = b.track_id
where b.album_id is null;
```

## Rollback

운영 반영 전 backup/restore 절차를 우선한다. FK 전환 이후 단순 column rollback이 필요하면 Track Board만 존재하는지 확인한 뒤 `track.spotify_id`로 별도 문자열 column을 복원한다. Album Board가 한 건이라도 생성된 뒤에는 Track 전용 schema로 자동 rollback하지 않고 데이터 보존 방안을 별도 승인받는다.
