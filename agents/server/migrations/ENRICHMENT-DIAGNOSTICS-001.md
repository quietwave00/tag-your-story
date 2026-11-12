# ENRICHMENT-DIAGNOSTICS-001 Oracle Migration

Album/provider 근거 재사용 상태를 저장한다. 새 애플리케이션을 시작하기 전에 staging에서 DDL을 검증하고 운영에 적용한다. 기존 `external_tag_observation`과 완료 marker는 변경하지 않으며 backfill하지 않는다.

```sql
create table album_provider_evidence (
    album_id number(19) not null,
    source varchar2(32 char) not null,
    status varchar2(32 char) not null,
    raw_tags clob,
    lease_token varchar2(36 char),
    lease_until timestamp(6),
    completed_at timestamp(6),
    constraint pk_album_provider_evidence primary key (album_id, source),
    constraint fk_album_provider_evidence_album foreign key (album_id) references album (album_id),
    constraint ck_album_provider_evidence_status check
        (status in ('IN_PROGRESS', 'SUCCESS', 'EMPTY', 'NOT_FOUND', 'FAILED', 'TIMEOUT'))
);

create index idx_album_provider_evidence_lease
    on album_provider_evidence (status, lease_until);
```

배포 전 기존 테이블 이름 충돌을 확인한다. 기존 Album/Track의 상태는 자동 이관하지 않는다. 새 수집이 시작되는 branch부터 상태가 채워진다.

```sql
select table_name from user_tables where table_name = 'ALBUM_PROVIDER_EVIDENCE';
select constraint_name, constraint_type from user_constraints
where table_name = 'ALBUM_PROVIDER_EVIDENCE';
select index_name from user_indexes where table_name = 'ALBUM_PROVIDER_EVIDENCE';
```
