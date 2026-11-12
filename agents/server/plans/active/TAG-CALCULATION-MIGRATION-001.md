# TAG-CALCULATION-MIGRATION-001 — 태그 계산 파이프라인 재설계와 점진적 전환

> 상태: 2026-11-10 사용자와 Human Review 중인 설계 초안. 상세 metadata와 동기 태그 import의 독립 호출은 ADR-032, 명시적 provider 근거에 따른 신규 타입은 ADR-033으로 확정했다. ADR-031의 비동기 작업·공개 진행 상태 설계는 대체됐으며 구현되지 않았다. MusicBrainz 장르 관계 수집은 별도 Plan으로 분리했다.

## 새 세션 인수인계: 무엇을 다시 계산하는가

- **최종 목표:** 기존 System Tag 계산 정책을 재설계하고, 새 정책으로 기존 Track/Album의 저장된 태그 결과까지 안전하게 **재계산**한다. 신규 import에만 새 규칙을 적용하는 것으로 끝내지 않는다. Board·Search 등 기존 소비자가 읽는 결과와 수동 관리 상태를 보존하며 점진적으로 전환한다.
- **계산 경계:** 외부 응답을 `Observation`으로 보존 → 이름을 Tag/alias에 매핑하고 타입·근거를 검증해 `Assertion`을 결정 → 가중치와 Track 직접/Album 참고 규칙으로 `subject_tag_resolved`를 다시 만든다. `subject_tag_resolved`는 원본이 아닌 조회용 projection이다. 원본 Observation이 충분한 대상은 재조회 없이 재해석할 수 있는지 확인하고, 원본이 없거나 실패한 provider만 별도 수집·재시도 정책을 적용한다.
- **현재 단계:** 이 파일은 구현 승인서가 아니라 Human Review 중인 방향과 조사 과제를 담은 초안이다. 첫 마일스톤은 현행 코드·데이터·테스트 기준선과 첫 구현 경계를 확정하는 것이다. 구현, DB 이관, 전체 재계산은 아직 시작하지 않았다. 새 세션에서는 아래 `이번 마일스톤`부터 검토하고 작은 구현 Plan을 별도 승인받아야 한다.
- **함께 읽을 기준:** `agents/server/server_spec.md`, `server_tag_feature_architecture.md`, `current_state.md`, `conventions.md`, `progress.md`와 관련 ADR을 확인한다. 특히 ADR-030(장르 표시), ADR-032(동기 import), ADR-033(신규 타입), ADR-029(Album/provider 재사용), ADR-010/027(기존 자동 생성)을 읽는다. ADR-031의 비동기 설계는 ADR-032로 대체됐다. 문서의 목표 구조를 현재 구현 상태로 간주하지 않는다.

## 이미 확인된 한계와 재계산 전 확인할 위험

- **외부 근거 부족:** 기존 `Observation`이 모든 Track의 직접 장르를 보장하지 않는다. Album 참고값은 직접 Track 장르가 아니다. MusicBrainz 503·TIMEOUT, provider별 빈 결과, 12초 예산으로 누락된 근거는 저장 데이터 재계산만으로 복구되지 않는다. `logs/enrichment-comparison.md`와 두 diagnostics JSONL은 표본 진단 자료이며 전체 데이터의 정확도나 원인 확정을 뜻하지 않는다.
- **기존 데이터의 의미 불일치:** `UNCLASSIFIED`에는 과거 MusicBrainz/Discogs의 명시적 genre/style과 Last.fm의 일반 community tag가 섞일 수 있다. 동일 이름에 서로 다른 타입 근거가 들어올 수 있으므로 이름만으로 일괄 승격하지 않는다. 기존 `NEW` Observation, Assertion, 완료 marker, 이미 계산된 projection의 재처리 범위를 조사해야 한다.
- **결과 보존:** 새 Resolver가 과거 결과를 다시 쓰면 관리자 `MANUAL_FIXED`/`HIDDEN`, Tag 병합·alias, Board가 참조하는 Tag ID, Catalog subject identity와 충돌할 수 있다. 재계산 대상·순서·멱등성·되돌리기와 구/신 결과 비교 기준을 구현 전에 확정한다. 공개 태그 GET은 저장 결과를 읽는 계약이므로 재계산을 GET에 숨겨 넣지 않는다.
- **계층의 별도 근거:** import에 장르 이름이 쌓여도 부모·자식 관계는 저절로 증명되지 않는다. MusicBrainz 장르 관계 수집·원본 저장은 별도 [MUSICBRAINZ-GENRE-GRAPH-001](MUSICBRAINZ-GENRE-GRAPH-001.md)에서 다룬다. 이 수집은 태그 재계산의 선행 조건이 아니다.
- **아직 결정하지 않은 것:** 기존 저장분의 재계산 범위와 트리거, 타입 충돌 해결, 부분 실패 재시도, `systemTags`의 직접/Album 참고 출처 표현은 아래 쟁점과 후속 작은 Plan에서 결정한다. 계층 edge 자동 승인·다중 부모는 별도 장르 그래프 Plan의 후속 결정이다. 확정된 것처럼 구현하지 않는다.

## Goal

현재의 동기 import 안에 섞인 Catalog 생성, 외부 조회, 태그 자동 생성, Assertion, Album 상속, 완료 표식, projection 갱신의 내부 책임을 다시 설계한다. 화면의 metadata 조회와 태그 import는 독립 요청으로 유지한다(ADR-032). **System Tag는 외부 근거로 계산한 장르·스타일·씬·국가·시대 등의 태그**라는 기존 도메인 정의를 유지한다. 그중 장르에는 승인된 어휘 → 근거별 가중치 → Track 직접 장르 우선 → 직접 근거가 없을 때 Album 참고값 정책(ADR-030)을 적용한다.

“다시 이식”은 현행 동작을 먼저 특성화하고 새 경로를 옆에 구축한 뒤 전환한다는 뜻이다. 기존 Board·Search·Catalog와 외부 관측 데이터를 한 번에 삭제하거나 다시 작성하지 않는다.

## 조사한 현행 기준선

- `TrackSelectionService`와 `AlbumSelectionService`가 Catalog import 뒤 외부 수집, bootstrap, Observation/Assertion, Resolver, 완료 marker, 응답 생성까지 동기 조율한다. 기존 resolved 결과나 `EMPTY_COMPLETED`는 provider 재시도를 건너뛴다.
- `TagBootstrapService`는 MusicBrainz 명시적 genre, Discogs genre/style, Last.fm community tag의 미매칭 이름으로 `ACTIVE/UNCLASSIFIED` Tag를 생성할 수 있다(ADR-010/027). `UNCLASSIFIED`는 장르 판정이 아니므로 `2025`, `3`, `beautiful` 같은 이름의 장르 노출 위험이 있다.
- 현재 Resolver는 tag별 `max(confidence)`와 최소 점수를 적용하고 Album resolved tag를 Track에 `0.85`로 상속한다. Track에 직접 다른 장르가 있어도 Album 장르가 별도 tag로 남을 수 있다.
- 외부 수집의 전체 첫 요청 예산은 현재 12초다. MusicBrainz는 요청 간격 gate를 사용하지만 표본 로그에서 HTTP 503과 예산 부족 TIMEOUT이 발생했다. Album/provider 재사용과 lease는 ADR-029 경로로 구현 기록이 있으나 전체 사용자 검증은 미확인이다.
- 현행 공개 경로는 `POST /api/catalog/import`가 최초 계산을 유발하고 Track/Album 태그 GET이 저장 결과를 읽는다. 기존 `systemTags`, `previewTags`, `tagDisplayStatus` 계약은 전환 전 기준선이다.
- 현재 React 상세 화면은 metadata GET과 `useSubjectTags`를 독립적으로 시작한다. 태그 서비스는 POST import가 끝난 뒤 태그 GET을 호출한다. 화면의 metadata 선표시는 이미 이 구조로 가능하며 공개 수집 진행 상태가 필요하지 않다.
- `external_tag_observation`, `tag_assertion`, `subject_tag_resolved`와 진단 JSONL은 원본 근거와 전후 비교에 사용한다. 기존 완료 표식과 `UNCLASSIFIED` Tag를 새 정책이 이미 반영된 데이터로 간주하지 않는다.

## 목표 구조 초안

### 1. 독립적인 화면 요청과 동기 태그 import

1. 상세 metadata GET과 태그 로드 경로는 클라이언트에서 독립적으로 시작한다. metadata가 먼저 오면 화면에 즉시 표시한다. 태그 영역은 클라이언트 요청 상태로 로딩·오류를 표현한다.
2. 태그 로드 경로는 기존처럼 `POST /api/catalog/import`를 기다린 뒤 저장 결과 전용 태그 GET을 호출한다. POST 내부에서 Catalog identity 확보, provider별 Track 직접·Album 근거 수집, Observation/Assertion, Resolver를 조율한다. 사용자 요청과 무관한 영속 worker·작업 큐·GET polling을 추가하지 않는다.
3. Album/provider 근거는 ADR-029의 영속 저장·lease·fencing 및 Track 간 재사용을 활용한다. Track 직접 근거와 Album 근거, provider별 `SUCCESS`/`EMPTY`/`NOT_FOUND`와 `FAILED`/`TIMEOUT`을 분리해 부분 성공을 보존한다. 외부 HTTP 중 DB transaction을 열지 않는다.
4. MusicBrainz 503과 12초 예산 부족은 UI 상태 계산으로 해결하지 않는다. 실제 호출 수·gate 대기·HTTP timeout을 계측하고, 현재 동기 POST의 제한 시간 안에서 가능한 검색 단계와 재시도 정책을 결정한다. 같은 IP의 요청 간격과 User-Agent를 확인한다. 다음 POST에서 실패 branch를 다시 시도할지, 성공 결과·빈 결과의 조기 반환을 어떻게 다룰지 별도 수집 정책으로 결정한다.

### 2. 원본 근거와 의미 판정 분리

```text
정확한 음악 identity 검증
  → provider 원본 수집·Observation 저장
  → 승인된 Tag/alias와 정확히 매칭해 장르 여부 판정
  → 해당 subject에 Assertion 생성
  → 근거별 confidence 계산·Resolver 적용
  → Track 직접/Album 참고 표시용 projection 또는 read 조합
```

- `Observation`은 제공자가 실제로 반환한 이름, Track/Album 범위, evidence type, 외부 참조, 수집 시각을 보존한다. 원문이 `2025`, `3`, `beautiful`이라는 사실은 버리지 않는다.
- `tag`의 `GENRE`와 장르로 승인된 `STYLE`은 System Tag 중 장르 분류다. 신규 MusicBrainz/Discogs 명시적 genre/style은 ADR-033에 따라 타입을 초기화하고, Last.fm community tag는 `UNCLASSIFIED`로 시작한다. `SCENE`, `COUNTRY`, `ERA`와 장르가 아닌 표현도 System Tag 범위에 남는다. 외부 이름이 어떤 의미인지와 해당 음악에 얼마나 강하게 연결되는지는 별개의 판단이다. `2025`, `3`, `beautiful`을 Last.fm 이름만 보고 장르로 확정하지 않는다.
- confidence는 **해당 음악과 승인된 장르의 연관 강도**다. 장르인지 여부를 점수로 추정하지 않는다. 기존 provider별 기본값과 `max(confidence)`, minimum score를 기준선으로 특성화하고 변경 시 별도 ADR로 설명한다.
- Track 직접 **장르 타입의 System Tag**가 표시 기준을 통과하면 그 Track의 장르로 표시한다. 없으면 Album에서 확정된 장르 타입의 System Tag를 `앨범 장르 기반` 참고값으로 보여준다. 서로 다른 Track에 다른 직접 장르가 있어도 Album의 하나의 장르로 덮지 않는다. Album 근거를 Track의 직접 Assertion으로 복사하지 않는다. 장르가 아닌 System Tag의 Track/Album 규칙은 이 장르 fallback만으로 변경하지 않는다.
- Track별 사용자 제안·투표는 이 마이그레이션 범위 밖이다. Apple Music Song/ISRC 등 추가 제공자는 표본에서 identity 중복, 응답 밀도, 장르 세분성, 이용 조건을 확인한 뒤 별도 결정한다.

### 3. MusicBrainz 장르 관계 수집과의 경계

장르/관계 HTML 스크래핑과 외부 그래프 저장은 별도 [MUSICBRAINZ-GENRE-GRAPH-001](MUSICBRAINZ-GENRE-GRAPH-001.md)에서 다룬다. 원본 그래프를 수집했다는 사실만으로 Track/Album의 장르 Assertion이나 승인된 `tag_parent` 관계를 만들지 않는다. 태그 재계산은 이 수집 완료를 기다리지 않는다.

### 4. 화면과 API 계약

```text
상세 화면 진입
  ├─ Track 또는 Album metadata GET ───────────────→ metadata부터 표시
  └─ POST /api/catalog/import ──완료 후──태그 GET → 장르·태그 표시
```

- **metadata:** Track 상세 조회와 Album 상세 GET은 태그 import와 독립적으로 실행한다. 이 응답이 먼저 오면 태그 요청 종료를 기다리지 않고 제목·아티스트·표지를 표시한다. 태그 요청의 지연·실패가 metadata 화면을 지우지 않는다.
- **POST import:** 현재의 `{subjectType: TRACK|ALBUM, spotifyId}`와 HTTP 200 `ApiResult` 계약을 유지한다. Catalog 저장과 동기 외부 수집·태그 계산을 마치거나 정해진 요청 예산 안에서 부분 결과를 확정한 뒤 반환한다. provider 한 곳의 실패로 다른 곳의 성공 결과를 폐기하지 않는다.
- **태그 GET:** POST가 끝난 뒤 호출한다. 저장된 `systemTags`, `previewTags`, `tagDisplayStatus`를 읽으며 외부 HTTP·DB write·재계산은 하지 않는다. 기존 Catalog 대상 부재 오류를 유지한다. POST 응답에 이미 태그가 들어 있더라도 기존 화면의 태그 GET 경로를 기준으로 계약을 특성화한다.
- **화면 상태:** 태그 import 요청이 진행 중이면 해당 영역의 클라이언트 로딩 상태를 사용한다. 서버는 `enrichmentStatus`, `nextPollAfterSeconds`, 작업 ID 또는 태그 GET polling 계약을 추가하지 않는다. 태그 요청이 실패하면 metadata를 유지한 채 태그 영역의 오류로 처리한다.
- **System Tag 분류와 출처:** 기존 `systemTags`에 장르·스타일·씬·국가·시대 등의 System Tag를 유지한다. Track 직접 장르와 `앨범 장르 기반` 참고값을 구분하려면 이 기존 결과에 타입·근거 출처를 어떻게 제공할지 API Human Review에서 정한다. 새 `genreTags` 응답을 전제로 설계하지 않는다. 실제 계약이 바뀌면 DTO·Swagger interface·Server Spec·클라이언트 계약 문서를 같은 구현 마일스톤에서 갱신한다. 현재 metadata/태그 분리 호출 자체를 위해서는 API 변경이 필요하지 않다. 클라이언트 소스는 현행 `conventions.md` 제한을 따른다.

## 데이터 마이그레이션 및 cutover 초안

1. **기준선 보존:** 기존 태그/alias/Observation/Assertion/resolved/완료 marker의 수와 상태를 집계한다. 진단 JSONL replay의 동일 Track/Album 표본을 고정하고 직접 장르, Album 참고값, 미분류 노출, provider별 `503`/TIMEOUT, 요청 수와 지연을 기록한다. 기존 원본 JSONL은 수정하지 않는다.
2. **타입 전환:** 신규 명시적 genre/style은 ADR-033으로 초기화한다. 기존 `UNCLASSIFIED` 이름을 자동으로 `GENRE`로 바꾸지 않는다. 저장된 provider/evidence metadata가 확인되는 row만 충돌을 조사해 재분류하고, 숫자·연도·분위기·중의적 이름은 개별 검토한다. 기존 Tag ID, Board 참조, 구독·관리 상태를 조사해 재분류·병합·숨김 규칙을 정한다.
3. **동기 수집 경로의 상태 정리:** ADR-029의 `album_provider_evidence`를 우선 재사용한다. Track 직접 branch의 실패 기록과 다음 POST 재시도 여부를 조사하고, `ALREADY_RESOLVED`/`EMPTY_COMPLETED`가 실패 branch를 영구히 가리지 않도록 최소 정책을 설계한다. 장르 어휘 정책이 바뀌었을 때는 원본 Observation 재해석과 provider 재조회 여부를 분리한다. 필요한 테이블·컬럼·unique·FK·index는 실제 스키마 preflight 후 Oracle/H2 DDL로 확정한다. 공개 작업 상태용 테이블은 만들지 않는다.
4. **Shadow 비교:** 새 결과를 구 결과와 나란히 계산하되 공개 응답은 그대로 둔다. Track 직접 장르 비율, Album 참고 비율, 비장르 표시 건수, 빈 결과·중복·provider 실패를 비교한다. MusicBrainz 503을 장르 정책 개선 효과로 잘못 집계하지 않는다.
5. **제한적 전환:** 작은 대상부터 새 읽기 경로로 전환하고, 실패 시 이전 읽기 경로로 되돌릴 수 있게 한다. 새 경로의 기존 Observation 재계산과 신규 수집을 구분한다. 완료 marker 재설정, 오래된 projection 정리, 기존 `UNCLASSIFIED` 데이터 처리 순서가 확인되기 전에는 구 테이블을 삭제하지 않는다.
6. **운영 적용 문서:** 각 구현 마일스톤의 실제 DDL, preflight, 데이터 변환, 후검증, 되돌리기 절차는 별도 `agents/server/migrations/` runbook에 기록한다. 기존 `ENRICHMENT-DIAGNOSTICS-001.md`는 Album/provider 테이블의 당시 DDL 기록으로 보존한다.

## ADR 및 기존 Plan 충돌

- ADR-030은 System Tag 중 장르 타입의 분류·표시 방향이다. ADR-010/027의 미매칭 `ACTIVE/UNCLASSIFIED` System Tag 자동 생성은 유지하며, 기존 태그별 Album 상속 표시와 새 Track 장르 우선·Album 참고 규칙의 차이는 후속 마일스톤에서 해결한다.
- ADR-033은 ADR-010의 MusicBrainz/Discogs 신규 `UNCLASSIFIED` 타입만 대체한다. ADR-027의 Last.fm `UNCLASSIFIED` 자동 생성은 유지한다. 기존 `UNCLASSIFIED`의 안전한 타입 이관은 별도 migration이다.
- Server Spec·System Tag 아키텍처의 장르/스타일/씬/국가/시대를 포괄하는 System Tag 정의를 유지한다. 앞서 장르만 System Tag라고 적었던 잘못된 문안은 폐기했다. ADR-030은 System Tag 전체를 장르로 축소하는 결정이 아니다.
- ADR-026/028의 MusicBrainz identity fallback과 ADR-029의 Album/provider 근거 재사용은 무조건 폐기하지 않는다. 표본·코드 검증으로 정확성 및 비용을 확인한다.
- ADR-032는 ADR-031의 비동기 worker·공개 진행 상태 결정을 대체한다. 현행 metadata GET과 태그 import의 독립 호출을 유지하므로 이 마이그레이션에서 작업 큐·polling을 구현하지 않는다.
- 2026-11-09 사용자 요청으로 이전 네 Plan은 completed에 **종료 기록**으로 보관했다. 이는 그 Plan의 미검증 Acceptance Criteria를 통과했다는 뜻이 아니다. 필요한 검증·결함은 새 마일스톤에서 명시적으로 인수한다.

## 이번 마일스톤: 설계 기준선과 첫 전환 경계 확정

이 문서의 첫 마일스톤은 구현 착수가 아니라 사용자가 리뷰할 수 있는 설계와 마이그레이션 경계를 확정하는 작업이다. 나머지 단계를 한 번에 구현하지 않는다.

1. 현재 POST import, 두 태그 GET, provider 상태/완료 marker, Observation·Assertion·Resolved의 실제 코드와 DB 제약을 특성화 표로 정리한다. 기존 테스트가 보호하는 동작과 미검증 동작을 구분한다.
2. 기준선 replay에서 Track 직접/Album 상속, 미분류 태그, provider 실패를 별도로 집계한다. 사용자 환경의 실행 결과만 검증 근거로 사용한다.
3. ADR-032의 독립 화면 요청과 동기 POST를 기준으로 metadata 선표시, 태그 로딩, 부분 provider 실패 및 후속 POST 재시도를 특성화한다. ADR-033의 타입 초기화와 동일 이름 충돌, 기존 `UNCLASSIFIED` 이관 범위를 검토한다. 기존 `systemTags`의 타입·출처 표시 방식을 Human Review에서 결정한다.
4. 첫 구현 마일스톤 하나만 상세 Plan으로 분리하고, 그때 필요한 Oracle/H2 migration, 특성화 테스트, Swagger/클라이언트 계약, 전환·되돌리기 절차를 작성한다.

## Acceptance Criteria / 검증 상태

- 현행 동작과 목표 정책의 차이, ADR 충돌, 데이터 전환·rollback 경계가 문서에서 추적 가능하다.
- 첫 구현 마일스톤의 변경 대상, API 영향, 마이그레이션 preflight, 수용 지표가 Human Review로 확정된다.
- 코드 구현·DB 변경·Gradle 검증은 아직 수행하지 않는다. Codex는 이후 테스트 코드를 작성하더라도 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`를 직접 실행하지 않으며 사용자 실행 결과로만 검증·Plan 완료 여부를 갱신한다.

## 함께 결정할 쟁점

1. 장르 확인 전의 미매칭 외부 이름을 화면의 일반 `previewTags`에 계속 보여줄지, 장르 화면에서는 숨길지.
2. 기존 `systemTags`에 Track 직접/Album 참고 출처를 어떤 필드로 넣을지, 기존 저장된 태그를 새 정책 재계산 중에 표시할지.
3. 기존 `UNCLASSIFIED` Tag를 어떤 evidence metadata로 재분류하고, 서로 다른 provider의 genre/style 타입 주장이 충돌하면 어떻게 할지.
4. 부분 실패한 provider를 동일 POST 안에서 어디까지 재시도하고, 이후 POST에서 어떤 조건으로 다시 시도할지.
