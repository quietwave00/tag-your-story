# TAG-BOOTSTRAP-001 — Seedless Initial System Tags and Raw Preview

## Status

- State: Complete — user confirmed full `test`, `check`, and `scripts/verify.sh` success on 2026-09-22
- Scope: 최초 Track import의 seedless Tag 생성·즉시 resolution·raw preview 및 bounded query
- Related decision: `ADR-010-seedless-system-tag-bootstrap-and-preview.md`
- Existing active plan `CATALOG-IMPORT-002.md`는 별도 검증 대기 상태로 유지한다. 이 Plan은 그 구현을 되돌리거나 완료 처리하지 않는다.

## Goal

빈 `tag`/`tag_alias` DB에서도 검증된 MusicBrainz/Discogs genre/style로 `APPROVED` Assertion과 resolved System Tag를 최초 import 요청 안에서 생성한다. 신뢰 가능한 명시적 장르 근거가 없고 resolved 결과도 없지만 `NEW` Observation은 있는 경우에만 출처가 명시된 raw preview를 제공한다. Last.fm community tag의 품질 경계, provider partial success, Album/Track 분리 트랜잭션, 멱등성 및 쿼리량을 유지한다.

`tag_parent`는 Tag 간 taxonomy 관계로서 import와 무관하므로 이 Plan에서 생성·추론·조회하지 않는다. 최초 계산을 위한 scheduler/batch도 추가하지 않는다.

## Source of Truth and Conflict

- `agents/server/server_spec.md`
- `agents/server/server_tag_feature_architecture.md`
- `agents/server/current_state.md`
- `agents/server/conventions.md`
- `agents/server/progress.md`
- `agents/server/decisions/ADR-002-user-owned-custom-tags.md`
- `agents/server/decisions/ADR-003-user-tag-exact-name-playlist.md`
- `agents/server/decisions/ADR-004-lastfm-community-tag-evidence.md`
- `agents/server/decisions/ADR-006-lastfm-ranked-top-tag-selection.md`
- `agents/server/decisions/ADR-008-enrichment-sequence-jdbc-batching.md`
- `agents/server/decisions/ADR-009-catalog-optimistic-create-conflict-recovery.md`
- `agents/server/decisions/ADR-010-seedless-system-tag-bootstrap-and-preview.md`
- `agents/server/plans/completed/TAG-CORE-001.md` through `TAG-CORE-004.md`
- `agents/server/plans/completed/TAG-SLICE-001.md`
- `agents/server/plans/completed/ENRICHMENT-001.md`
- `agents/server/plans/active/CATALOG-IMPORT-002.md`

기존 명세의 approved-alias-only Assertion 승격 및 resolved-only 화면 규칙은 ADR-010과 충돌한다. 구현과 함께 명세를 명시적으로 갱신한다. ADR-004/006의 Last.fm 순위·count·confidence 정책과 UserTag 분리는 유지한다.

## Current State

`POST /api/tracks/import`는 Catalog 저장, 병렬 provider 수집, Observation/Assertion 저장, Album/Track Resolver, `systemTags` 응답을 동기 수행한다. 그러나 `ObservationWriteService`는 approved alias와 exact unique match한 Observation에만 Assertion을 만든다. 빈 taxonomy에서는 모든 raw 이름이 `NEW`이고 resolved는 비어 있다.

`ExternalTagObservation`은 source/raw/normalized name/external reference만 보존한다. 미매칭 Observation의 원래 `EvidenceType`과 confidence는 저장하지 않아, 나중에 DB row만으로 Discogs genre와 style을 안전하게 구분하여 재계산할 수 없다. resolved가 0건이면 정상 완료와 미처리를 구분하지 못해 반복 선택 때 provider를 재호출할 수 있다.

## Target Flow

```text
TrackSelectionService.select()                     [transaction 없음]
  → Catalog import/reuse
  → 기존 visible/hidden resolved 조회
  → 이미 완료 marker가 있고 resolved가 0건이면 Observation preview 조회 후 반환
  → provider 병렬 수집                       [transaction 없음]
  → Album + Track 입력의 normalized name bulk 준비
  → approved alias bulk 조회
  → ACTIVE canonical Tag normalized name bulk 조회
  → 신뢰 가능한 명시적 genre/style의 누락 Tag만 자동 생성 [짧은 transaction]
  → Album/Track Observation + Assertion 처리      [기존 분리 transaction]
  → Album resolve → Track inheritance/resolve     [기존 분리 transaction]
  → 완료 marker 기록(조건 충족 시)
  → visible resolved가 있으면 systemTags, 없으면 raw preview
```

첫 요청의 raw preview는 이미 수집한 입력과 matching 결과에서 `NEW`로 남은 값만 사용해 추가 SELECT 없이 조립한다. 기존 완료 Track의 preview는 Track/Album의 `NEW` Observation을 한 번의 subject-pair bulk query로 읽는다. 현재 동기식 import는 계산이 끝난 뒤 한 번만 응답하므로 `PREVIEW`는 "계산 중"이 아니라 "확정 결과가 없고 미매칭 raw 근거만 있음"을 뜻한다. 나중에 비동기 처리로 전환하면 계산 중 상태는 별도로 설계해야 한다.

## Matching and Trust Policy

1. 현재 `TagNameNormalizer`로 normalized name을 만든다. blank/길이 초과 등 DB에 저장할 수 없는 이름은 자동 생성하지 않고 안전하게 제외한다.
2. `APPROVED tag_alias`가 유일하게 가리키는 Tag를 우선한다. 여러 Tag에 매칭되면 ambiguous로 남기고 canonical-name fallback이나 자동 생성을 하지 않는다.
3. alias가 없으면 `tag.normalized_name`이 일치하는 ACTIVE canonical Tag를 재사용한다. CANDIDATE/DEPRECATED/MERGED Tag는 자동 활성화하지 않는다.
4. 그래도 없을 때 MusicBrainz `EXPLICIT_GENRE`, Discogs `EXPLICIT_GENRE`/`EXPLICIT_STYLE`에서만 ACTIVE/UNCLASSIFIED Tag를 생성한다. MusicBrainz/Discogs adapter가 이미 수행하는 음악 엔티티 exact validation을 통과한 입력만 사용한다.
5. Last.fm `COMMUNITY_TAG`만으로는 Tag를 생성하지 않는다. 같은 normalized name으로 승인된 alias 또는 ACTIVE canonical Tag가 있다면 기존 confidence의 Assertion을 만들 수 있다.
6. matched Observation에서만 APPROVED Assertion을 만들고 Resolver는 기존 `max(confidence)`, minimum score, Album inheritance, direct priority 및 HIDDEN/MANUAL_FIXED 정책을 그대로 사용한다. score 미달 결과를 강제로 resolved에 넣지 않는다.
7. 자동 Tag의 이름은 검증된 입력 중 결정적 source 우선순위와 입력 순서로 선택한다. 같은 normalized name을 여러 source가 반환해도 Tag row는 하나다. `tag_parent`나 의미 기반 synonym 추론은 수행하지 않는다.

## Persistence and Migration

- `tag.normalized_name`을 추가하고 전역 `UNIQUE`로 동시 중복을 막는다. 기존 Tag의 normalized name backfill 전에 충돌 목록과 slug/alias 충돌을 preflight로 보고한다. 서로 다른 기존 Tag가 같은 normalized name이면 임의 병합하지 않고 migration을 중단한다.
- `TagType.UNCLASSIFIED`를 추가한다. 자동 Tag에는 적용하되 기존 Tag 타입은 변경하지 않는다. 자동 생성 자체를 별도 승인 대기 상태로 두지 않는다. 승인된 것은 subject별 Assertion이고 자동 Tag는 ACTIVE다.
- `external_tag_observation.evidence_type`, `confidence`를 신규 수집 row에 저장하고 기존 row 재수집 시 원래 source evidence와 일치하는지 확인·보완한다. 기존 row에 값이 없으면 source만으로 임의 추정하지 않는다. Oracle/H2 schema 및 migration 절차를 함께 작성한다.
- Track별 최소 완료 marker는 “계산 성공했지만 resolved 0건”을 미처리와 구분한다. 모든 적용 가능한 provider가 정상 완료/명시적 NOT_FOUND로 종료하고 persistence/resolution이 성공한 경우에만 완료로 기록한다. transient failure가 있으면 재시도 가능하게 둔다. marker의 insert/update는 짧은 transaction이며 unique로 동시성을 방어한다.
- Album/Track Observation과 Resolution의 기존 transaction 분리를 유지한다. 자동 Tag 중복은 알려진 unique constraint만 번역하고 실패한 write transaction 밖에서 bulk 재조회 후 최대 한 번 재시도한다. 다른 무결성 오류는 전파한다.
- `Tag`는 현재 `IDENTITY` PK이므로 신규 고유 이름 `k`개에 대해 최대 `k`개의 개별 INSERT가 발생할 수 있다. 이를 숨기지 않고 계측한다. PK 전략 변경은 별도 migration 결정으로 보류한다.

## Public Response

- 기존 `systemTags` 구조와 의미는 유지한다. visible resolved가 있으면 이를 반환하며 raw 이름을 확정 목록에 섞지 않는다.
- `previewTags`를 가산적으로 추가한다: 원본 표시 이름과 source만 포함하며 Tag ID/score는 없다. `tagDisplayStatus`는 `CONFIRMED`, `PREVIEW`, `EMPTY` 중 하나다.
- visible resolved가 0건일 때만 `NEW` Observation의 preview를 제공한다. 점수 미달의 MATCHED 근거는 preview로 우회하지 않는다. normalized name으로 중복 제거하고 결정적 source 우선순위(명시적 genre/style 우선, Last.fm 후순위)와 provider 순서를 유지해 최대 5개를 제공한다.
- HIDDEN-only projection은 “resolved 없음”으로 취급하지 않는다. 관리자가 숨긴 Tag와 같은 normalized name을 raw preview로 재노출하지 않으며, 승인된 다른 visible Tag가 없더라도 HIDDEN 정책을 우선한다.
- Preview는 검증 전 외부 표현임을 Swagger와 DTO 필드 설명에 명시한다. Controller/Swagger interface를 함께 갱신하고 기존 endpoint·error envelope를 유지한다.

## Query Budget and Measurement

Catalog import의 기존 약 27회 first-load 추정은 **미매칭 Observation만 있던 입력** 기준이며, 신규 Tag/Assertion/Resolved 쓰기가 발생하는 새 경로에 그대로 적용하지 않는다.

- Album + Track 전체 입력에서 approved alias 조회 최대 1회, canonical Tag normalized-name 조회 최대 1회로 제한한다. 이름 수에 비례하는 SELECT는 0회다.
- Observation/Assertion 중복 조회, Album/Track Resolver 조회는 기존 bulk/subject별 경계를 유지한다. matching을 두 subject에서 다시 수행해 alias SELECT를 반복하지 않는다.
- 첫 응답 preview 조립의 추가 SELECT는 0회, 완료된 기존 Track의 preview 조회는 최대 1회다. 완료 marker 조회는 resolved가 비었을 때만 수행한다.
- 새로운 Tag `k`개, Assertion `a`개, resolved `r`개에 따른 DML 및 sequence/batch 실행은 SELECT 예산과 분리하여 datasource-proxy로 측정한다. Hibernate SQL 로그 줄 수를 DB execution 수로 보고하지 않는다.
- 기존 Track, 기존 Album의 새 Track, 빈 taxonomy 첫 import, 모두 NEW인 Last.fm-only import, 같은 이름 동시 import를 각각 계측한다. 상세 수치 상한은 구현 전 characterization baseline과 사용자 실행 결과로 확정한다.

## Tests and Acceptance Criteria

- [x] 빈 Tag/Alias DB에서 검증된 MusicBrainz 또는 Discogs 명시적 genre/style 한 건이 Tag, MATCHED Observation, APPROVED Assertion, resolved System Tag와 최초 응답 `systemTags`까지 생성한다.
- [x] Last.fm-only 미매칭 입력은 Tag/Assertion/resolved를 자동 생성하지 않고 출처가 표시된 `previewTags`를 반환한다.
- [x] 이미 존재하는 ACTIVE canonical Tag 또는 approved alias를 재사용하며 ambiguous alias와 비활성 Tag를 임의로 자동 승격하지 않는다.
- [x] 여러 provider가 같은 normalized name을 반환해도 Tag 하나와 source별 멱등 Assertion으로 수렴한다. 같은 이름의 동시 Track import에서도 global Tag duplicate가 없다.
- [x] minimum score, Album inheritance, HIDDEN/MANUAL_FIXED, provider partial success 및 generic Observation API의 subject validation이 퇴행하지 않는다.
- [x] 기존 미매칭 Observation의 evidence metadata를 추측하여 승인하지 않는다. 신규 수집 및 안전한 재수집은 metadata를 보존한다.
- [x] 확정 0건 완료 Track의 반복 선택은 provider를 재호출하지 않고 Observation preview를 반환한다. transient failure는 완료로 고정하지 않는다.
- [x] Track/Album 입력 수 또는 태그 수가 증가해도 추가 SELECT 수가 선형 증가하지 않는다. JDBC batch와 신규 Tag IDENTITY INSERT를 구분한 query regression test를 추가한다.
- [x] `tag_parent` 테이블/서비스/배치/Resolver 전파를 추가하지 않는다.
- [x] API Swagger, server spec, System Tag architecture 및 migration runbook이 구현과 일치한다.
- [x] 사용자 실행 대상 테스트, 전체 `test`, `check`, `scripts/verify.sh`와 diff review가 통과한다. 그 전에는 이 Plan을 completed로 옮기거나 progress에 완료를 기록하지 않는다.

## Implementation Sequence

1. 현행 import/alias/Observation/Resolver/API의 특성화 테스트와 datasource-proxy phase별 baseline을 추가한다.
2. Tag normalized identity·UNCLASSIFIED·Observation evidence metadata의 schema/entity/migration을 추가하고 기존 데이터 preflight를 검증한다.
3. Album/Track 통합 bulk matching과 신뢰 가능한 Tag find-or-create를 구현한다. 충돌 rollback·재조회 경계를 테스트한다.
4. 기존 Observation/Assertion 흐름에 matching 결과를 전달하고 범용 호출 경로는 유지한다.
5. resolved 0건 완료 marker와 preview 응답·Swagger를 연결한다.
6. 빈 taxonomy, 기존 데이터, 동시성, 실패/부분 성공, query budget을 검증하고 문서 diff를 리뷰한다.

## Verification Handoff

Codex는 Gradle/verify를 직접 실행하지 않는다. 구현 후 사용자에게 대상 테스트 명령과 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`를 안내하고 전달받은 결과로만 완료 여부를 갱신한다.

## Completion Record

The user confirmed successful `./gradlew test`, `./gradlew check`, and `./scripts/verify.sh` after the query-probe and API-module fixture corrections. Codex reviewed the scoped diff and `git diff --check` without running Gradle. The separate `CATALOG-IMPORT-002` plan remains active for its own acceptance review.
