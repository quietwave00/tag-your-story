# ENRICHMENT-PERF-001 Oracle Migration Runbook

## 목적

`external_tag_observation`, `tag_assertion`, `subject_tag_resolved`의 PK 생성을 Oracle
identity에서 Hibernate pooled sequence로 전환한다. 세 sequence는 애플리케이션의
`allocationSize=50`과 동일하게 `INCREMENT BY 50`을 사용한다.

이 migration은 기존 버전과 호환되는 rolling migration이 아니다. 기존 애플리케이션은
identity가 ID를 생성한다고 가정하고 새 애플리케이션은 ID를 명시해 INSERT하므로,
migration 시작 전 세 테이블을 쓰는 worker/API를 모두 중지하고 새 버전 검증이 끝날
때까지 writer를 하나만 유지한다.

참고한 공식 문서:

- Oracle `ALTER TABLE ... DROP IDENTITY`: https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-TABLE.html
- Oracle `CREATE SEQUENCE`: https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-SEQUENCE.html
- Hibernate 6 sequence migration guide: https://docs.hibernate.org/orm/6.0/migration-guide/

## 1. Preflight

운영 Oracle version에서 아래 DDL을 staging copy로 먼저 검증한다.

```sql
select banner_full
from v$version
where banner_full like 'Oracle Database%';
```

현재 최대 ID를 기록한다.

```sql
select 'EXTERNAL_TAG_OBSERVATION' table_name,
       coalesce(max(observation_id), 0) max_id
from external_tag_observation
union all
select 'TAG_ASSERTION', coalesce(max(assertion_id), 0)
from tag_assertion
union all
select 'SUBJECT_TAG_RESOLVED', coalesce(max(resolved_id), 0)
from subject_tag_resolved;
```

identity 여부와 explicit ID INSERT 허용 여부를 확인한다. `GENERATED ALWAYS`는 일반
explicit INSERT를 거부하며 `BY DEFAULT`는 허용한다. 이 migration은 두 경우 모두
identity property를 제거해 단일 ID generator만 남긴다.

대상 column이 이미 non-identity라서 `USER_TAB_IDENTITY_COLS`에 나타나지 않으면 해당
table의 `DROP IDENTITY` 문만 건너뛴다. 그 외 PK/FK/unique/index 정의가 예상과 다르면
아래 공통 DDL을 그대로 실행하지 말고 배포를 중단한다.

```sql
select table_name, column_name, generation_type, identity_options
from user_tab_identity_cols
where (table_name, column_name) in (
    ('EXTERNAL_TAG_OBSERVATION', 'OBSERVATION_ID'),
    ('TAG_ASSERTION', 'ASSERTION_ID'),
    ('SUBJECT_TAG_RESOLVED', 'RESOLVED_ID')
)
order by table_name;
```

변경 전 PK/FK/unique/index 정의를 저장한다.

```sql
select dbms_metadata.get_ddl('TABLE', table_name)
from user_tables
where table_name in (
    'EXTERNAL_TAG_OBSERVATION',
    'TAG_ASSERTION',
    'SUBJECT_TAG_RESOLVED'
);

select dbms_metadata.get_ddl('INDEX', index_name)
from user_indexes
where table_name in (
    'EXTERNAL_TAG_OBSERVATION',
    'TAG_ASSERTION',
    'SUBJECT_TAG_RESOLVED'
);
```

동일 이름의 sequence가 이미 있으면 배포를 중단하고 소유자와 사용처를 조사한다.

```sql
select sequence_name, increment_by, cache_size, last_number
from user_sequences
where sequence_name in (
    'EXTERNAL_TAG_OBSERVATION_SEQ',
    'TAG_ASSERTION_SEQ',
    'SUBJECT_TAG_RESOLVED_SEQ'
);
```

## 2. Migration

writer 중지를 확인한 후 실행한다. Oracle DDL은 implicit commit되므로 각 단계의 결과를
확인하고 다음 단계로 진행한다.

```sql
alter table external_tag_observation modify observation_id drop identity;
alter table tag_assertion modify assertion_id drop identity;
alter table subject_tag_resolved modify resolved_id drop identity;

alter table external_tag_observation modify (observation_id not null);
alter table tag_assertion modify (assertion_id not null);
alter table subject_tag_resolved modify (resolved_id not null);
```

Hibernate 6의 기본 pooled optimizer는 DB sequence 값을 현재 ID block의 상한으로
해석한다. 기존 데이터가 있는 sequence의 첫 block이 겹치지 않도록 공식 migration
guide의 보수적 기준인 `MAX(id) + 1 + allocationSize`를 사용한다. 빈 테이블은 1에서
시작한다.

```sql
declare
    start_value number;
begin
    select case when count(*) = 0 then 1 else max(observation_id) + 51 end
      into start_value
      from external_tag_observation;
    execute immediate
        'create sequence external_tag_observation_seq start with ' || start_value ||
        ' increment by 50 cache 50 nocycle';
end;
/

declare
    start_value number;
begin
    select case when count(*) = 0 then 1 else max(assertion_id) + 51 end
      into start_value
      from tag_assertion;
    execute immediate
        'create sequence tag_assertion_seq start with ' || start_value ||
        ' increment by 50 cache 50 nocycle';
end;
/

declare
    start_value number;
begin
    select case when count(*) = 0 then 1 else max(resolved_id) + 51 end
      into start_value
      from subject_tag_resolved;
    execute immediate
        'create sequence subject_tag_resolved_seq start with ' || start_value ||
        ' increment by 50 cache 50 nocycle';
end;
/
```

## 3. Post-check 및 배포

세 physical sequence와 애플리케이션 `allocationSize=50`의 일치를 확인한다.

```sql
select sequence_name, increment_by, cache_size, cycle_flag
from user_sequences
where sequence_name in (
    'EXTERNAL_TAG_OBSERVATION_SEQ',
    'TAG_ASSERTION_SEQ',
    'SUBJECT_TAG_RESOLVED_SEQ'
)
order by sequence_name;
```

다음 조건을 확인한다.

```sql
select table_name, column_name, nullable, identity_column
from user_tab_columns
where (table_name, column_name) in (
    ('EXTERNAL_TAG_OBSERVATION', 'OBSERVATION_ID'),
    ('TAG_ASSERTION', 'ASSERTION_ID'),
    ('SUBJECT_TAG_RESOLVED', 'RESOLVED_ID')
)
order by table_name;
```

- 세 row 모두 `NULLABLE='N'`, `IDENTITY_COLUMN='NO'`
- 세 sequence 모두 `INCREMENT_BY=50`, `CACHE_SIZE=50`, `CYCLE_FLAG='N'`
- preflight에서 저장한 PK/FK/unique/index가 그대로 존재
- 새 애플리케이션의 `ddl-auto=validate` 통과

새 버전 기동 후 각 테이블에 실제 애플리케이션 경로로 최초 row를 생성하고, 생성된
ID가 preflight `MAX(id)`보다 큰지 확인한다. sequence의 `NEXTVAL`을 직접 소비한 값은
Hibernate가 실제로 부여하는 첫 ID와 같지 않을 수 있으므로 이 검증을 대체하지 않는다.

검증이 끝난 뒤 writer traffic을 재개한다. sequence는 transaction rollback과 무관하게
증가하므로 ID gap은 정상이며 순서나 멱등성 판단에 사용하지 않는다.

## 4. Application rollback

새 버전에서 문제가 발생하면 먼저 writer를 다시 중지한다. 새 버전이 생성한 row를
보존한 상태에서 구버전으로 되돌리려면 세 column에 identity를 복구한 뒤 external
sequence를 제거한다. `START WITH LIMIT VALUE`가 현재 최대 ID 이후로 identity high
water mark를 이동시키는지 운영 Oracle version의 staging copy에서 사전 검증해야 한다.

```sql
alter table external_tag_observation modify (
    observation_id generated by default as identity
        (start with limit value increment by 1 cache 20 nocycle)
);
alter table tag_assertion modify (
    assertion_id generated by default as identity
        (start with limit value increment by 1 cache 20 nocycle)
);
alter table subject_tag_resolved modify (
    resolved_id generated by default as identity
        (start with limit value increment by 1 cache 20 nocycle)
);

drop sequence external_tag_observation_seq;
drop sequence tag_assertion_seq;
drop sequence subject_tag_resolved_seq;
```

rollback 후 `USER_TAB_IDENTITY_COLS`, `MAX(id)`와 구버전 smoke insert를 확인한 뒤
writer를 재개한다. DB rollback 없이 애플리케이션만 구버전으로 되돌리면 ID 생성이
실패하므로 허용하지 않는다.
