# MUSICBRAINZ-GENRE-GRAPH-001 — MusicBrainz 장르 그래프 DB 저장과 정적 JSON 생성

> 상태: 2026-11-10 사용자가 별도 active Plan 작성을 요청했다. 이 문서는 다음 세션의 HTML 스크래핑 작업을 위한 Human Review 초안이며, 수집 코드·DB DDL·운영 반영은 아직 수행하지 않았다. 첫 마일스톤은 MusicBrainz 원본 장르/관계의 수집·검증·별도 저장이다. System Tag 연결, `tag_parent` 승인, 시각화 구현은 후속 마일스톤이다.

> 2026-11-10 범위 구체화: 사용자 요청에 따라 **DB 저장과 동일 그래프의 정적 JSON 파일 생성**을 모두 필수 산출물로 정의한다. 이후 발견한 장르와 관계를 기존 그래프에 추가할 수 있는 identity·참조·갱신 규칙을 포함한다. 정기 재수집 자동화는 이번 목표가 아니다. 문서 수정은 구현 착수 또는 검증 완료를 뜻하지 않는다.

## Goal과 시작 URL

- 목록 출발점: **https://musicbrainz.org/genres** — MusicBrainz에서 사용할 수 있는 전체 장르 목록. 각 항목은 장르 상세 페이지 `/genre/{MBID}`로 연결된다.
- 각 상세 페이지의 장르 MBID·이름 및 장르 간 관계를 수집해, 이후 트리/그래프 시각화와 내부 System Tag 연결에 사용할 수 있는 **원본 방향 그래프**로 DB에 저장한다.
- DB에 저장 완료된 동일 그래프를 **`musicbrainz-genre-graph.json`**으로 내보낸다. 파일만으로 모든 노드와 관계를 읽을 수 있어야 하며, 파일 이용 시 스크래퍼 실행·외부 HTTP·DB 연결·관계 재계산이 필요하지 않아야 한다.
- 처리 흐름은 **HTML 수집 → 관계 방향 정규화·중복 제거·검증 → DB 저장 → 해당 DB 그래프 버전의 JSON 생성 및 일치 검증**이다. 신규 장르와 관계 추가 후에도 같은 export 경로를 사용한다.
- 별도 작업인 [TAG-CALCULATION-MIGRATION-001](TAG-CALCULATION-MIGRATION-001.md)의 Track/Album 태그 재계산을 이 수집 작업과 결합하지 않는다. 장르 관계 수집의 실패가 기존 import API에 영향을 주지 않게 한다.

## 착수 전 기준과 확인 사항

- `agents/server/server_spec.md`, `server_tag_feature_architecture.md`, `current_state.md`, `conventions.md`, `progress.md`와 ADR 목록을 확인한다. 특히 ADR-010/027/030/033의 System Tag 생성·타입 정책과 `server_tag_feature_architecture.md`의 `tag_parent` 관리자 승인·DAG 규칙을 읽는다.
- 현행 코드/DB에 `tag_parent`가 실제로 있는지, Oracle/H2 스키마·migration 관리 방식·기존 Tag identity와 정규화 규칙을 조사한다. 설계 문서의 테이블 목록을 구현 완료로 간주하지 않는다.
- 사용자가 지정한 구현 경로는 HTML 스크래핑이다. 전체 순회 전에 소수 장르로 파서와 운영 비용을 확인한다. 대량 수집이 사이트 제한이나 안정성 문제에 걸리면 [공식 PostgreSQL 덤프](https://metabrainz.org/datasets/postgres-dumps) 추출을 대안으로 검토하고 경로 변경을 보고한다. 공식 JSON 덤프의 entity 목록에는 Genre가 없다. 웹 API의 genre lookup은 관계 `inc`를 지원하지 않는다.
- 페이지 스크래핑을 선택하면 실행 시점의 [robots.txt](https://musicbrainz.org/robots.txt)와 사이트 이용 조건을 확인한다. 현재 robots.txt의 범용 `Crawl-delay: 2`와 허용/금지 경로를 반영하고, 식별 가능한 User-Agent, 단일 요청 속도 제한, 503/429 대기·재시도 상한, 중단/재개 체크포인트를 설계한다. 상세 페이지의 DOM 구조와 링크/라벨은 변경될 수 있으므로 표본 파서를 먼저 검증한다.

## 저장 모델 후보: 원본 그래프와 내부 Tag 계층 분리

| 데이터 | 최소 내용 | 제약/의미 |
| --- | --- | --- |
| 외부 장르 노드 | MusicBrainz genre MBID, 원문 이름, 상세 URL, 수집 시각 | MBID가 identity. 이름만으로 병합하지 않음 |
| 외부 관계 edge | 양 끝 MBID, 관계 유형, 방향, 원본 URL/출처, 수집 시각 | `(출발 MBID, 도착 MBID, 유형)` 중복 방지. 원본을 재수집할 수 있어야 함 |
| 수집 상태 | 목록 스냅샷 식별자, 장르별 성공/실패, 마지막 성공 시각 | 부분 실패와 완전한 수집을 구별. 재실행 가능 |
| 내부 매핑 | 외부 MBID ↔ 내부 `tag_id`, 매핑 근거/검토 상태 | 후속 마일스톤. 동일 이름 자동 연결 금지 |

- `subgenre of: house`가 `deep house` 페이지에 있으면 **`house → deep house` (`SUBGENRE`)**로 정규화한다. `house` 페이지의 `subgenres: deep house`는 같은 edge의 반대쪽 표시이므로 한 건만 저장한다.
- `influenced by`와 그 역표시인 `influenced genres`는 **영향을 준 장르 → 영향을 받은 장르** (`INFLUENCES`)로, `fusion of`는 별도 유형으로 저장한다. 이 관계들을 부모·자식으로 해석하지 않는다. 원본에 표시된 관계 유형과 정규화 방향을 함께 추적한다. MusicBrainz의 [장르 간 관계 유형](https://musicbrainz.org/relationships/genre-genre)을 기준으로 샘플을 양쪽 페이지에서 검산한다.
- 저장은 단일 `parent_id` 트리로 제한하지 않는다. `SUBGENRE`만 계층 후보인 방향 그래프이며, 원본에 여러 부모가 있으면 모두 보존한다. cycle·자기참조·사라진 edge와 dangling MBID는 검증 보고서에 기록하고 임의 수정하지 않는다. 화면에서 트리 형태가 필요하면 이 그래프의 검증된 일부를 투영한다.
- 수집된 전체 외부 장르를 곧바로 `ACTIVE` System Tag로 seed하거나 Track/Album Assertion으로 만들지 않는다. 현재 `tag_parent`는 관리자 승인 관계라는 설계이므로, 원본 edge의 자동 승인과 `tag_parent` 투영은 충돌 검토 후 별도 ADR/Plan에서 결정한다.

## 정적 JSON 산출물과 DB 일치 규칙

- 필수 산출물은 DB의 노드·관계 데이터, `musicbrainz-genre-graph.json`, 수집·그래프 검증 보고서다. JSON은 DB에서 확정된 그래프 버전을 읽어 생성하는 배포 가능한 데이터 파일이다.
- JSON에는 `schemaVersion`, `graphVersion`, 수집 범위와 완전성 상태, `nodes`, `edges`를 포함한다. 노드에는 안정적인 `id`, 원문 이름, 출처, 외부 ID, 원본 URL을 담고, edge에는 `source`, `target`, `type`, 출처 근거를 담는다. 필요한 수집 시각과 원본 관계 라벨도 보존한다.
- 파일의 노드 ID는 DB의 자동 증가 PK나 배열 위치에 의존하지 않는다. MusicBrainz 노드는 `musicbrainz:genre:{MBID}` 형식을 사용하며 이름 변경이나 재출력으로 ID를 바꾸지 않는다. DB에서는 MBID identity와 unique 제약을 유지한다.
- 노드 ID 및 `(source, target, type)` 순서로 결정적으로 정렬한다. 같은 그래프 버전을 다시 출력하면 실행 시각 같은 가변 메타데이터 때문에 파일 내용이 바뀌지 않게 한다.
- export 도중 데이터가 바뀌어 노드와 edge가 서로 다른 버전에서 섞이지 않도록 동일 버전의 일관된 읽기를 보장한다. 참조 대상 노드가 파일에 모두 존재하는지 검사한다. 상세 확인에 실패한 참조 노드는 미확인 상태를 명시한다.
- 건수뿐 아니라 노드 ID·이름·출처와 edge 집합·근거가 해당 DB 버전과 같은지 검증한 뒤 임시 파일을 최종 파일로 원자적으로 교체한다. export 실패 시 기존 정상 파일을 보존하고, DB와 파일의 버전 불일치를 보고한다. export만 재실행할 수 있어야 한다.
- 부분 수집 파일은 완전한 전체 그래프와 구분해 저장하고 완전성 상태를 명시한다. 실패한 수집으로 기존 정상 전체 파일을 덮어쓰지 않는다. 그래프의 cycle 등 원본 이상과 수집 완전성은 별도로 보고한다.
- 파일 저장 위치와 보관·배포 절차를 구현 runbook에 명시한다. JSON schema 계약과 작은 예제 fixture를 저장소에 포함한다.

## 신규 장르·관계 확장 규칙

- 노드 목록과 edge 목록을 분리한다. 고정된 장르 enum, 고정 깊이, 단일 `parent_id`에 의존하지 않아 새 장르가 기존 장르의 하위 장르 또는 여러 장르와의 관계로 추가될 수 있어야 한다.
- 신규 MusicBrainz 장르는 새 MBID로 추가하고, 기존 장르에 연결하는 edge를 추가한다. 기존 ID를 재발급하거나 전체 노드를 삭제 후 재생성하지 않는다. 이름이 같다는 이유로 다른 MBID를 병합하지 않는다.
- 이번 마일스톤에는 오프라인 입력을 통한 **MusicBrainz 노드·관계의 증분 upsert와 JSON 재출력**을 포함한다. 전체 목록을 다시 수집하지 않고도 출처가 확인된 신규 노드와 연결 관계를 반영할 수 있게 한다. 동일 추가 입력을 반복 적용해도 중복되지 않아야 한다.
- 증분 입력에서 빠진 기존 노드·edge는 삭제하지 않는다. 이름 수정은 동일 identity에 적용하고, 관계 삭제·방향 변경은 명시적인 변경 대상으로 검증한다. 부분 실패나 입력 누락을 관계 삭제로 해석하지 않는다.
- 새로운 관계도 기존 방향 정규화와 출처 보존 규칙을 적용한다. 원본의 다중 부모·cycle·자기참조는 보고하고 임의 수정하지 않는다. 이 데이터에 추가됐다는 사실이 내부 `tag_parent` 승인이나 System Tag 생성을 의미하지 않는다.
- MusicBrainz MBID가 없는 장르를 향후 수용할 수 있도록 JSON identity에는 출처 namespace를 둔다. 예를 들어 `local:genre:{UUID}`를 추가해도 기존 ID·참조는 유지할 수 있어야 한다. 실제 로컬 장르 등록·검토·MusicBrainz 병합 정책 및 DB 지원은 후속 마일스톤에서 결정하며, 가짜 MBID를 만들지 않는다.
- 새 관계 유형을 추가할 때는 방향·의미·원본 라벨을 명시적으로 정의한다. 알 수 없는 관계를 기존 유형으로 추측 변환하지 않는다. JSON 구조의 호환되지 않는 변경은 `schemaVersion`을 올리고, 데이터 추가·수정은 `graphVersion`으로 식별한다.

## 이번 마일스톤 범위

1. 목록/상세 HTML의 표본을 조사한다. `deep house`, `house` 및 역방향·영향 관계 표본으로 MBID, 이름, 관계 방향, 누락·중복 여부를 기록한다. robots.txt·이용 조건·요청 간격을 확인한 뒤 전체 수집 범위를 확정한다.
2. 수집 경로에 맞춘 최소 Oracle/H2 스키마와 migration runbook을 작성한다. 노드·edge·수집 상태의 PK, unique, FK, 인덱스, 재실행/업데이트/삭제 정책을 정한다. 원본 데이터를 변경할 때는 이전 스냅샷과 비교할 수 있는 방법을 포함한다.
3. 표본 수집과 전체 수집을 같은 코드 경로로 실행할 수 있게 한다. 속도 제한, 재시도 상한, 실패 기록, 체크포인트, 멱등 upsert를 구현한다. 외부 HTTP 요청 동안 DB transaction을 열지 않는다.
4. 관리자 또는 오프라인 실행 경로에서 원본 그래프를 적재하고, 노드/edge 수, 중복 제거 수, 실패/누락 수, 역방향 검산, cycle/자기참조 결과를 보고한다. 기존 import API·Tag/Assertion/Resolved 테이블에는 쓰지 않는다.
5. 저장 완료된 그래프 버전에서 정적 JSON을 생성하고 DB와 내용 일치를 검증한다. JSON만 읽는 소비자가 노드·유형별 관계를 복원할 수 있게 schema 계약과 예제를 제공한다.
6. 신규 MusicBrainz 장르·관계의 오프라인 증분 추가, 멱등 upsert, 기존 관계 보존 및 JSON 재출력을 구현한다. 별도 관리자 UI·공개 API·정기 재수집 scheduler는 이번 범위에 포함하지 않는다.

## Acceptance Criteria / 검증

- `https://musicbrainz.org/genres`의 표본 장르를 MBID로 식별하고, 개별 페이지의 `subgenre of`/`subgenres`를 동일한 방향 edge 한 건으로 저장한다. `influenced genres`를 `SUBGENRE`로 저장하지 않는다.
- 같은 입력을 두 번 적재해도 노드·edge 건수가 늘지 않는다. 중단 뒤 재개, 실패한 페이지 재시도, 원본 관계 변경의 동기화 결과를 검증한다.
- 수집 실패가 기존 태그 계산 API와 저장된 System Tag 결과를 변경하지 않는다. DB 제약과 수집 통계로 부분 수집을 식별할 수 있다.
- DB와 JSON의 동일 `graphVersion`에 대해 노드 및 유형별 edge 집합과 출처 근거가 일치한다. JSON만 로드해 관계를 복원하는 테스트는 DB와 외부 HTTP 없이 통과해야 한다.
- 같은 버전 재출력의 결정성, export 실패 시 기존 파일 보존, 부분 수집 파일 구분, 모든 edge의 노드 참조 무결성을 검증한다.
- 기존 그래프에 신규 장르와 기존 장르로 연결되는 edge를 추가한 후 기존 ID·관계가 유지되고, DB와 새 JSON에 동일하게 반영된다. 같은 입력 재적용, 다중 부모 추가, 동명·다른 MBID, 누락된 참조, 명시적 관계 변경을 검증한다.
- 구현 시 parser/정규화/멱등성/실패 재개를 검증하는 테스트를 작성한다. Codex는 Gradle·`./scripts/verify.sh`를 직접 실행하지 않고 사용자에게 실행 명령을 안내하며 전달받은 결과로만 완료 여부를 갱신한다.
- 현재는 문서 작성만 완료했다. 수집 코드, DB 변경, JSON 생성·증분 추가 구현, 테스트 실행, 실제 MusicBrainz 전체 수집, 시각화는 미실행이다.

## 후속 결정과 범위 밖

- 외부 장르 MBID와 내부 System Tag의 안전한 매핑, 기존 Tag 이름/alias 충돌 처리, `tag_parent` 자동 승인 여부, 여러 부모의 화면 표현은 후속 taxonomy 마일스톤에서 결정한다.
- 영향·융합 edge의 시각화 방식과 Track/Album 장르 표시·재계산 정책은 각각 별도 마일스톤이다. 관계 데이터만으로 개별 음악에 장르 Assertion을 생성하지 않는다.
