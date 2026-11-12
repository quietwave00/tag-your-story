# ENRICHMENT-DIAGNOSTICS-001 — 외부 태그 진단과 수집·계산 고도화

> 상태: 2026-11-09 사용자 요청으로 계획 종료 및 completed 보관. 재실행·전체 검증과 ADR-030에 따른 정책 조정이 남아 있어 수용 기준 통과로 표시하지 않는다.

## Goal

여러 Spotify Track/Album을 수동으로 import하며 MusicBrainz, Discogs, Last.fm의 원본 후보·태그가 수집, 식별, Observation/Assertion, Resolver의 어느 단계에서 채택 또는 탈락했는지 한 파일에서 비교할 수 있게 한다. Album, Single/EP, Compilation과 Track 직접 근거/Album 상속 근거의 차이를 기록한다. 첫 Track의 Track·Album 근거를 모두 수집하되 같은 Album의 이후 Track에서는 완료된 Album 근거를 재사용한다. 진단에서 확인한 MusicBrainz ISRC 404 중단과 Last.fm 미매칭 태그 손실을 승인된 ADR-026~028에 따라 개선한다.

## 조사 결과와 관련 결정

- 현재 성공한 provider는 `ExternalEnrichmentCollector`의 DEBUG 로그에 source, subject ID, status만 남긴다. 실패 로그에도 provider 내부의 검색 단계와 후보 수가 없다.
- `ImportedAlbum`/`AlbumEntity`에는 Spotify `album_type`, `total_tracks`, `CatalogReleaseType`이 없다. 기존 읽기 화면의 분류는 `CatalogReleaseType.classify`와 ADR-021/022를 따른다.
- `TrackSelectionService`와 `AlbumSelectionService`는 기존 resolved 결과 또는 빈 결과 완료 marker에서 조기에 반환한다. 같은 Spotify ID의 반복 import가 외부 API를 다시 호출한다는 보장은 없다.
- 관련 ADR: 004, 006, 007, 010, 011, 012, 021, 022, 026, 027, 028. ADR-026의 ISRC 404 metadata fallback, ADR-028의 해당 경로 첫 유효 후보 선택, ADR-027의 Last.fm 신규 Tag 생성만 기존 matching/bootstrap 정책에서 변경한다. 그 외 ISRC 후보 판정, confidence, Resolver, 완료 marker 정책은 유지한다. Album provider 수집 결과 재사용은 승인된 별도 구현 범위다.
- 수집된 진단 로그 1,438행·95개 실행 중 계산 완료 66건에서 Spotify 원본 `album_type=single` Track 6건·Album 9건은 모두 확정 태그가 없었다. 반면 원본 `album`이 `SINGLE_EP`로 분류된 Track 7건·Album 4건은 모두 확정 태그가 있었다. 따라서 발매 분류값 `SINGLE_EP` 전체를 무태그로 간주하거나 별도 ISRC 검색 순서를 적용하지 않는다.
- Last.fm Track `track.getTopTags` 39회 중 비어 있지 않은 응답은 1회였다. `Cher / Believe` 수동 조회는 원본 10개·매핑 10개, `Rhye / 3 Days`는 원본·매핑 모두 0개였다. Last.fm 신규 Tag 생성은 실제 응답이 있는 경우에만 효과가 있으며 빈 응답을 보완하지 않는다.
- `Billboard Style`의 ISRC `ZZA0Z2601215` 조회는 HTTP 404였지만 제목·Artist 검색에서 제목·Artist·길이가 같은 Recording 후보 2개가 나왔다. 기존 구현은 404에서 중단되고, 독립 검색을 해도 유일 후보 규칙 때문에 선택하지 못한다. fallback과 해당 경로의 후보 선택을 함께 구현해야 한다.
- 진행 중인 `CATALOG-SUBJECT-TAGS-001`의 POST import/GET read 계약은 유지한다. GET에 외부 조회나 진단용 DB write를 추가하지 않는다.

## Scope

### 1. 진단 실행과 파일

- `tag.enrichment.diagnostics.enabled=false`를 기본값으로 하는 opt-in 설정을 추가한다. 활성화 시 전용 logger가 `logs/enrichment-diagnostics.jsonl`에 한 사건당 JSON 한 줄을 기록한다. 크기·보관 개수를 제한하고 `logs/`를 git 추적에서 제외한다. 파일 기록 실패가 import 성공/실패 판단을 바꾸지 않도록 한다.
- 한 import마다 `runId`를 생성한다. 병렬 provider 작업에는 이 ID와 subject context를 명시적으로 전달하거나 안전하게 복사하고, worker 재사용 시 이전 context가 남지 않게 한다.
- API key, Discogs token, Authorization header, Spotify token, 전체 요청 URL, 사용자 식별 정보, HTTP 전체 body는 기록하지 않는다. 사람이 비교할 수 있는 metadata 필드와 bounded 후보/태그 배열만 기록한다. 배열의 `totalCount`와 `truncated`를 함께 남긴다.
- 설정이 꺼져 있으면 전용 파일을 만들지 않고, 진단용 Spotify 추가 호출·후보 직렬화도 수행하지 않는다.

### 2. 입력 및 발매 유형

- `import_started`에 `runId`, 요청 `subjectType`, Catalog/Spotify Track·Album ID, 원문 title, Track/Album artist 이름, ISRC 유무, 재생 시간, 발매 연도를 기록한다.
- 진단 모드에서만 기존 Spotify Album 상세 읽기 경로를 사용해 원본 `album_type`, `total_tracks`와 ADR-021의 nullable `releaseType`(`ALBUM`, `COMPILATION`, `SINGLE_EP`, `UNKNOWN`)을 기록한다. 추가 조회 실패는 `releaseType=UNKNOWN`과 사유로 남기고 원래 import 결과에는 영향을 주지 않는다. 분류 규칙을 진단 코드에 다시 구현하지 않는다.
- 기존 resolved/완료 marker로 외부 수집이 생략되면 `import_skipped`에 `ALREADY_RESOLVED` 또는 `EMPTY_COMPLETED`를 기록한다. 이 경우 provider가 빈 결과를 반환한 것으로 표시하지 않는다.

### 3. 제공자별 원본 응답과 판정

- MusicBrainz: `ISRC`/metadata 검색 종류, 검색식의 필드값(키 제외), 원본 Recording 후보의 ID/title/artist/duration/score, matching 통과 수와 선택·거절 사유, Recording genre, 연결 Release Group 후보 및 genre를 기록한다.
- Discogs: 각 search attempt의 `MASTER`/`RELEASE`, `release_title`/`track`, title/artist 입력, 원본 후보의 ID/title/year, artist/title/year 필터별 잔여 수, 선택·거절 사유, 선택한 상세의 genre/style을 기록한다. `API 결과 0건`, `후보는 있었지만 검증 후 0건`, `복수 후보`, `상세 재검증 실패`를 구별한다.
- Last.fm: `track.getTopTags`/`album.getTopTags`, 원문/edition fallback 여부, 응답 artist/title, 반환 tag의 name/count/순서, identity 검증 결과, 수집된 상위 5개와 제외 이유, API error code/message를 기록한다.
- 각 provider의 최종 status, Track/Album별 반환 tag 수, 외부 reference, 경과 시간과 timeout 여부를 기록한다. 한 provider의 실패가 다른 provider 진단 기록을 지우지 않는다.

### 4. 태그 계산 결과

- `enrichment_completed`에 provider별 status와 Track/Album raw tag 수를 기록한다.
- `tag_calculation_completed`에 bootstrap의 기존 Tag 매칭/신규 생성/미매칭 수, Observation `NEW`/`MATCHED` 수, Assertion 생성/재사용 수, Track 직접/Album 상속 근거 수, 최종 resolved tag의 ID/name/score/reason/status, preview 수를 기록한다. 로그는 실제 계산 결과를 읽으며 도메인 규칙을 중복 구현하지 않는다.
- 상세 출력은 이미 존재하는 System Tag 이름과 provider raw tag만 사용하고, 진단 때문에 새로운 DB query를 입력 수에 비례해 만들지 않는다.

### 5. 반복 표본 수집

- 일반 import는 기존 fast path를 유지한다. 같은 ID를 다시 분석할 때는 테스트 전용 수동 진단 진입점에서 Spotify metadata와 `ExternalTagProvider`를 다시 읽어 전용 파일에 수집·matching 결과를 기록한다. 이 진입점은 Observation/Assertion/Resolved/완료 marker를 쓰지 않으며 공개 API를 추가하지 않는다.
- 수동 진단의 provider 결과와 실제 import의 최종 계산 결과를 서로 구분하는 `mode=PROVIDER_ONLY|IMPORT_PIPELINE`을 기록한다. `PROVIDER_ONLY`를 확정 System Tag 결과로 표현하지 않는다.

### 6. Album 근거 재사용 (2026-11-04 추가 승인 범위)

- Album에 대해 처음 계산되는 Track에서는 각 provider의 Track 직접 근거와 Album 근거를 모두 조회한다. 같은 Album의 다음 Track에서는 완료된 Album/provider 근거를 저장 결과에서 재사용하고 해당 Track의 직접 근거만 새로 조회한다. Last.fm Track 호출을 낮은 표본 성공률만으로 제거하지 않는다.
- provider별 `SUCCESS`/`EMPTY`/`NOT_FOUND`와 `FAILED`/`TIMEOUT`을 구분해 재사용 가능 상태를 기록한다. 일부 provider만 실패했으면 성공한 Album 근거는 유지하고 실패한 branch만 후속 요청에서 재시도한다. 원본 raw tag와 Observation/Assertion/Resolved 간 정합성을 보장하지 못한 상태는 완료로 기록하지 않는다.
- 서로 다른 Track이 같은 Album을 동시에 최초 선택할 때 Album 조회를 조율한다. 외부 HTTP 동안 DB transaction을 열지 않으며, 동시 실행과 다중 인스턴스에서 중복 요청·중복 쓰기·부분 실패를 어떻게 처리할지는 구현 전에 새 ADR로 확정한다.
- Album 근거를 재사용한 Track도 기존 Album 상속과 Track 직접 우선순위로 resolve한다. Album 근거가 이후 갱신되면 이미 계산된 Track의 상속 결과를 갱신할 경로를 포함한다. 공개 API 응답과 수동 `PROVIDER_ONLY`의 무DB-write 정책은 유지한다.
- Spotify `album_type=single`, `total_tracks=1`도 현재는 Track/Album을 별도 subject로 계산한다. 양쪽 provider 근거가 0개이면 재사용만으로 태그가 생기지 않는다. Artist 태그 추정이나 다른 발매의 Recording 태그 복사는 이번 범위에 포함하지 않는다.

### 7. MusicBrainz Recording 검색·선택 (2026-11-06 추가 승인 범위)

- 발매 유형과 무관하게 Spotify가 제공한 ISRC를 먼저 조회한다. ISRC가 없거나 조회 결과가 HTTP 404/빈 후보이거나 기존 검증을 통과한 후보가 없으면 제목·Artist metadata 검색을 수행한다. 인증 오류, rate limit, 서버 오류, timeout은 404와 동일하게 취급하지 않는다(ADR-026).
- ISRC 404 뒤 metadata 검색에서 기존 제목·Artist·재생 시간 검증을 통과한 후보가 복수이면 MusicBrainz 응답 순서의 첫 유효 Recording을 선택한다. 원본 첫 후보가 검증에서 탈락하면 다음 유효 후보를 택한다. ISRC 조회의 복수 후보 선택과 ISRC 부재·그 밖의 후보 미일치 뒤 metadata 검색의 유일 후보 정책은 유지한다(ADR-012/028).
- 진단에는 ISRC HTTP status, metadata fallback 이유, 원본·유효 후보 수, 선택 Recording ID와 `FIRST_VALID_AFTER_ISRC_404` 사유를 남긴다. 필수 Recording 선택 실패는 다른 provider의 성공 결과를 폐기하지 않는다.

### 8. Last.fm community tag의 System Tag 생성 (2026-11-06 추가 승인 범위)

- identity 검증을 통과한 Track/Album 응답의 상위 5개 `COMMUNITY_TAG`를 기존 approved alias 및 ACTIVE canonical Tag에 먼저 매칭한다. 미매칭 이름도 기존 MusicBrainz/Discogs bootstrap 경로로 `ACTIVE` System Tag와 해당 subject의 `APPROVED` Assertion을 생성한다(ADR-027). ambiguous alias와 비활성 canonical 이름 충돌에서는 새 Tag를 만들지 않는다.
- 새 Tag의 표시는 원래 태그 이름을 사용하고 내부 `TagType.UNCLASSIFIED`만 지정한다. Last.fm Track `0.65`·Album `0.60` confidence, Resolver 최소 점수 `0.50`, Album 상속 가중치 `0.85`, `count` 미사용 및 Track 직접/Album 상속 우선순위는 유지한다. 동일 normalized name은 하나의 Tag로 수렴시킨다.
- Last.fm 응답이 비면 태그를 생성하지 않는다. 신규 Tag 수, Last.fm 단독 생성 수, Track 직접/Album 상속 최종 태그 수를 기존 진단에서 구분한다.

### 9. 기존 빈 결과 처리와 범위 경계

- 기존 `NEW` Observation 및 `EMPTY_COMPLETED` Track/Album의 일괄 재처리·완료 표식 무효화는 사용자 결정에 따라 수행하지 않는다. 새 정책은 실제 provider 수집·계산이 수행되는 신규 subject에 적용한다. 기존 완료 표식의 조기 반환과 수동 `PROVIDER_ONLY`의 무DB-write 성질은 유지한다.
- 두 제공자 개선은 원본 근거가 존재할 때의 수집·매칭·bootstrap 범위다. Spotify 원본 `single`의 무태그 결과를 항상 해결한다는 수용 기준은 두지 않는다. Artist 기반 추정, 다른 발매의 Recording 태그 복사, Last.fm 응답이 빈 경우의 태그 생성은 범위 밖이다.

## Acceptance Criteria

1. Track, Album, Spotify `single`, `album`으로 표기된 EP, Compilation 표본에서 원본 발매 정보와 nullable 분류가 남는다. 정보가 부족하면 `UNKNOWN`이다.
2. Discogs 원본 후보에 Master ID가 있지만 필터에서 탈락하는 경우, API 0건과 구별되는 단계·사유가 한 `runId`로 추적된다.
3. MusicBrainz Recording genre와 Release Group genre, Last.fm Track/Album tag를 별도 subject로 비교할 수 있다.
4. 성공·부분 실패·timeout·빈 응답·기존 결과 재사용 각각의 로그가 구분된다. 병렬 import 사이에 `runId`가 섞이지 않는다.
5. 진단 설정을 끄면 진단 전용 HTTP·DB write·파일 생성이 없고, 일반 logging·응답 계약·Resolver 정책이 유지된다. Album 재사용으로 의도적으로 감소하는 provider HTTP 호출은 설정과 무관한 승인된 변경으로 별도 검증한다. secret은 로그에 나타나지 않는다.
6. 수동 진단 재실행은 provider HTTP만 수행하고 Catalog/Tag 데이터를 변경하지 않는다.
7. 첫 Track은 Track·Album 조회가 모두 실행되고, 같은 Album의 후속 Track은 완료된 Album/provider branch를 재호출하지 않으면서 Track 직접 근거를 조회한다. 재사용 로그에 source, 상태, Album ID와 재사용 이유를 남긴다.
8. Album provider의 부분 실패·timeout은 성공한 다른 provider 결과를 폐기하지 않으며, 실패 branch는 후속 선택에서 재시도할 수 있다. 빈 완료 결과와 미처리 상태를 혼동하지 않는다.
9. 같은 Album의 동시 최초 Track 선택과 Album 근거 갱신 뒤 기존 Track 재계산에서 결과가 수렴한다. 외부 HTTP를 기다리는 동안 DB transaction이 열리지 않는다.
10. 1곡 싱글은 Album 근거가 있으면 Track에 상속되고 양쪽 근거가 비면 빈 결과를 유지한다. 재사용 최적화가 근거 없는 태그를 만들지 않는다.
11. MusicBrainz ISRC 404에서는 metadata 검색이 실행되고, 기존 title/Artist/duration 검증을 통과한 복수 후보 중 응답 순서의 첫 유효 후보를 선택한다. 검증 불합격 후보는 건너뛴다. ISRC 조회 성공·다른 HTTP 실패·ISRC 부재의 기존 판정 정책은 유지한다.
12. Last.fm 단독으로 identity 검증된 미매칭 상위 community tag가 있으면 중복 없는 ACTIVE System Tag와 APPROVED Assertion이 생기고, 기존 confidence 및 Resolver 기준에 따라 Track 직접/Album 상속 결과가 계산된다. 빈 응답, ambiguous alias, 비활성 이름 충돌에서는 새 Tag가 생기지 않는다.
13. 기존 `EMPTY_COMPLETED`·`ALREADY_RESOLVED` subject의 일반 import는 정책 변경만을 이유로 재조회하지 않는다. 기존 데이터 일괄 재처리 없이 신규 subject의 결과와 provider별 부분 성공을 검증한다.

## Implementation Steps

1. 전용 진단 event 모델/recorder, opt-in 설정, bounded JSONL file appender와 파일 보관 정책을 추가한다.
2. `TrackSelectionService`/`AlbumSelectionService`의 시작·조기 반환·최종 결과와 collector의 병렬 context/상태를 연결한다.
3. 세 client/provider에 원본 응답 요약 및 matching 단계별 판정 사건을 추가한다. 요청·응답 mapping과 matching 규칙 자체는 유지한다.
4. 진단 모드에서만 Spotify Album 상세 분류를 읽고, 읽기 실패를 진단 사건으로 기록한다. 기존 Catalog 저장 모델과 공개 API는 수정하지 않는다.
5. 반복 조회용 테스트 전용 provider-only 진입점과 사용 방법을 추가한다.
6. Album/provider 상태와 재사용·재시도·동시 최초 선택·기존 Track 갱신의 경계를 코드와 스키마 기준으로 설계하고, 필요한 새 ADR을 작성한다. 구현 전 active Plan과 ADR의 정합성을 다시 확인한다.
7. provider의 Track/Album 수집을 분리해 완료된 Album 근거를 재사용하고, 실패 branch만 재시도하며, Album 갱신 시 기존 Track 상속을 동기화한다.
8. MusicBrainz ISRC HTTP 404를 metadata fallback으로 연결하고, fallback 원인을 후보 매칭에 전달해 404 경로에서만 첫 유효 후보를 선택한다. 기존 ISRC 복수 후보 및 다른 metadata 검색 경로는 특성화 테스트로 보호한다.
9. Last.fm `COMMUNITY_TAG`를 bootstrap 생성 대상에 포함한다. 기존 alias/canonical 우선순위, DB unique·동시 생성 처리, assertion provenance, Track/Album confidence와 Resolver 결과를 유지한다.
10. fixture 기반 테스트로 JSON 형식, 단계별 후보 수·거절 사유, Album/Single/EP 분류, 병렬 context 격리, 비활성 모드 무영향, secret 미기록, 수동 재조회 무DB-write, provider별 재사용·부분 실패·동시성·1곡 싱글 빈 결과를 확인한다. ISRC 404·빈 후보·검증 후보 없음, 404가 아닌 HTTP 실패, 첫 후보 탈락과 복수 유효 후보, 기존 ISRC/metadata 판정, Last.fm 단독 신규 Tag·alias 충돌·동시 생성·빈 응답·기존 완료 표식 유지도 테스트한다. 운영 API의 응답 구조는 바꾸지 않으므로 Swagger 변경은 없다.
11. diff 및 트랜잭션 경계, 추가 HTTP/SELECT, 파일 기록 장애, Sentry 전송 여부를 리뷰한다. 사용자가 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`를 실행하고 결과를 전달하면 Acceptance Criteria를 확인한다. 통과 후에만 Plan을 `completed/`로 옮기고 `progress.md`를 갱신한다.

## 승인된 진단 범위

- 진단 모드에서 Spotify Album 상세를 추가 조회한다. 이 조회는 발매 유형을 기존 데이터 재사용 사례에서도 기록하기 위해 필요하며, 실패해도 import는 계속한다.
- 파일에는 원본 HTTP body 대신 비교에 필요한 필드와 최대 개수 제한을 둔 후보/태그를 기록한다. 원본 body 전체 보존은 범위 밖이다.

## Approval Record

- 2026-11-04: 사용자가 이 Plan을 `active/`로 승격하도록 요청했다. 위 Scope와 Acceptance Criteria를 승인된 구현 범위로 기록한다. 진단 모드의 추가 Spotify Album 상세 조회와 bounded 응답 필드 기록을 포함한다. 구현·사용자 실행 검증은 아직 시작하지 않았다.
- 2026-11-04: 사용자가 같은 Album의 이후 Track에서 Album 근거를 재사용하는 개선을 후속 Roadmap이 아닌 구현 범위에 포함하도록 요청했다. Section 6과 Acceptance Criteria 7–10을 추가했다. 진단 설정 비활성 시 HTTP 호출 수 불변이라는 기존 기준은 새 재사용 정책과 충돌하므로 진단 전용 부작용 불변과 승인된 provider 호출 감소를 분리했다. provider 상태 저장·동시성·기존 Track 갱신의 상세 설계는 새 ADR에서 확정해야 한다.
- 2026-11-06: 사용자가 진단 결과를 토대로 ADR-026~028의 MusicBrainz ISRC 404 fallback·첫 유효 후보 선택과 Last.fm community tag 자동 생성을 이 active Plan의 고도화 범위에 포함하도록 요청했다. Section 7–9 및 Acceptance Criteria 11–13을 추가했다. 이전의 "후속 Plan에서 구현" 표기는 이 승인으로 대체한다. 기존 `NEW` Observation·`EMPTY_COMPLETED` 일괄 재처리는 명시적으로 제외한다.

## Implementation Record

- 2026-11-04: 진단 JSONL recorder, import/provider context, 후보·tag 판정 event, release 분류 조회 및 테스트 전용 `PROVIDER_ONLY` 실행 코드를 작성했다. 정적 diff 검토만 수행했고 사용자 실행 검증 결과는 아직 받지 않았다. Plan은 active 상태를 유지한다.
- 일반 import 진단은 `ENRICHMENT_DIAGNOSTICS_ENABLED=true`일 때 활성화된다. 기본 출력은 `logs/enrichment-diagnostics.jsonl`이며 `tag.enrichment.diagnostics.file`로 변경할 수 있다.
- 반복 표본은 `DIAGNOSTIC_RUN=true`, `DIAGNOSTIC_SPOTIFY_TRACK_ID` 또는 `DIAGNOSTIC_SPOTIFY_ALBUM_ID`를 설정하고 `./gradlew :tagnote-core:test --tests com.tagnote.application.enrichment.diagnostics.ProviderOnlyDiagnosticRunTest`를 사용한다. 필요한 Spotify/provider 자격 정보와 provider 활성화 환경변수는 기존 설정을 따른다. 이 테스트는 import 선택 서비스와 DB write service를 호출하지 않는다.
- Last.fm Track 응답 자체가 빈 것인지 DTO mapping에서 비는 것인지 확인하는 별도 수동 테스트는 `LASTFM_DIAGNOSTIC_RUN=true`와 `LASTFM_API_KEY`를 설정하고 `./gradlew :tagnote-core:test --tests com.tagnote.infrastructure.external.lastfm.LastFmLiveDiagnosticTest --info`로 실행한다. `LASTFM_DIAGNOSTIC_ARTIST`와 `LASTFM_DIAGNOSTIC_TRACK`을 함께 설정하면 지정 표본도 비교한다. 기본 표본은 Last.fm 공식 API 예시의 `Cher / Believe`다.
- 사용자 실행 결과: `Cher / Believe`는 원본 10개·client mapping 10개, `Rhye / 3 Days`는 원본 0개·client mapping 0개였다. 해당 표본의 Last.fm Track 태그 공백은 DTO mapping 손실이 아니라 API 응답의 빈 태그 목록으로 확인됐다. 전체 `test`/`check`/`verify.sh` 결과는 아직 받지 않았으므로 Plan은 active로 유지한다.
- MusicBrainz Track `NOT_FOUND`의 실패 단계를 확인하는 수동 테스트 `MusicBrainzLookupDiagnosticTest`를 추가했다. `MUSICBRAINZ_DIAGNOSTIC_RUN=true`와 `MUSICBRAINZ_USER_AGENT`로 기준 ISRC를 조회하고, `MUSICBRAINZ_DIAGNOSTIC_SPOTIFY_TRACK_ID` 및 Spotify 인증 정보를 더하면 실제 표본의 ISRC·metadata 조회를 각각 수행해 상태·HTTP status·후보 수·매칭 ID를 비교한다. 사용자 실행 결과는 아직 받지 않았다.
- 사용자 실행 결과: Spotify Track `35ve61xDrQy5Q4U20CCttN` (`Billboard Style`, ISRC `ZZA0Z2601215`)의 MusicBrainz ISRC lookup은 HTTP 404/`NOT_FOUND`였다. 독립 metadata 검색은 score 100, title/artist/duration이 같은 Recording 후보 2개를 반환해 현재 unique 정책으로는 선택할 수 없었다. 기준 ISRC `GBAYE9200070`은 후보 2개 중 duration 차이가 더 작은 하나를 선택했다. ISRC 404 시 metadata fallback이 실행되지 않는 제어 흐름과 fallback 후에도 동률 후보를 확정할 수 없는 식별 문제를 분리해 확인했다. 전체 검증 결과는 아직 받지 않았다.
- ADR-026/028은 ISRC 404 metadata fallback과 해당 경로의 첫 유효 후보 선택을 정의한다. ADR-027은 Last.fm community tag의 System Tag 생성을 정의한다. 2026-11-06 범위 승인 전에는 후속 작업이었으며, 지금은 이 Plan의 미구현 단계 8–9에 포함된다.
- 2026-11-06: ADR-026/028의 ISRC HTTP 404 fallback과 첫 유효 metadata 후보 선택, ADR-027의 Last.fm `COMMUNITY_TAG` bootstrap 생성을 코드에 반영하고 특성화 테스트를 추가했다. 전체 Gradle 검증은 사용자 실행 전이며, Album/provider branch 영속 재사용은 아직 구현되지 않았다. 다중 인스턴스 lease·부분 실패·기존 Track 재계산 경계는 ADR-029에 기록했다. Plan은 active 상태를 유지한다.
- 2026-11-06: Album/provider branch의 영속 상태와 lease를 `album_provider_evidence`에 연결하고 Oracle migration runbook `agents/server/migrations/ENRICHMENT-DIAGNOSTICS-001.md`를 작성했다. Collector는 Track 직접/Album branch를 분리하고 완료된 Album 근거만 재사용하며, 선택 서비스는 Observation/Assertion/Resolved와 기존 Track 상속 갱신 뒤 branch를 완료한다. 실패 branch는 release 후 재시도할 수 있도록 했다. 관련 테스트 코드를 추가했지만 사용자의 `test`/`check`/`verify.sh` 실행 결과는 아직 받지 않았으므로 Plan은 active다.
- 사용자 실행 `FirstVerticalSliceIntegrationTest`에서 Spring test context 생성이 `NoSuchBeanDefinitionException`으로 실패했다. 상세 missing bean 이름이 포함된 전체 stack trace는 전달되지 않았지만, `TrackSelectionService` 생성자 의존성 중 test slice에 등록되지 않은 `CatalogDetailService`를 `@MockBean`으로 보강했다. ADR-027 적용 후 기대값이 낡은 Last.fm preview 테스트도 ACTIVE System Tag/Assertion/Resolved 기대값으로 갱신했다. 재실행 결과를 기다리며 Plan은 active다.
- 사용자 재실행에서 컨텍스트 오류는 해소됐고 `FirstVerticalSliceIntegrationTest`의 31개 Last.fm fixture가 확정 System Tag로 생성되어 이전 `systemTags=[]` 기대값과 충돌했다. ADR-027에 맞춰 31개 확정 태그 및 Tag INSERT 기대값을 갱신했다. 전체 `test`에서 수동 `ExternalCatalogLookupManualTest` 세 건은 API 자격 정보가 없어 실패했으므로 다른 수동 진단 테스트처럼 `EXTERNAL_CATALOG_LOOKUP_RUN=true` opt-in으로 전환했다. 사용자 재실행 결과를 기다린다.
- 기존 JSONL의 `IMPORT_PIPELINE` 표본 106건에서 고유 Track 36개·Album 32개를 추출하고, 현재 `POST /api/catalog/import` 계약으로 curl을 호출하는 `scripts/replay-enrichment-diagnostics.py`와 사용 문서를 추가했다. `ENRICHMENT_DIAGNOSTICS_FILE`로 서버 출력을 `logs/enrichment-diagnostics-tobe.jsonl`에 분리하고, 원본 runId·metadata·provider raw tag·resolved tag 비교를 JSONL로 남긴다. 현재 작업 환경의 `127.0.0.1:8080`에는 API가 없어 첫 표본의 실제 curl은 연결 거부(HTTP 000)였으며 tobe 진단 사건은 생성되지 않았다. API 주소/실행 환경 확인 후 재실행이 필요하다.
- 재실행 시 기존 배치의 결과를 이어 쓰도록 replay state를 추가했다. curl 직전 pending 대상과 진단 파일 offset을 기록하고, 중단 후 재실행에서 완료된 서버 runId를 복구한다. 완료된 subject는 다시 호출하지 않으며, 중단된 JSONL 행은 무시한다. 상태 복구용 Python 단위 테스트를 추가했으며 실행 결과는 아직 받지 않았다.
- PowerShell 재실행에서 API가 기본 `enrichment-diagnostics.jsonl`에 기록해 `tobe`가 비어 있었고, Linux Python은 Java Instant의 나노초 시각을 파싱하지 못해 완료 run을 놓쳤다. 시각 파싱과 출력 파일 오설정 진단을 수정했다. 기본 파일에 기록된 첫 Track run의 실제 서버 이벤트 35개를 `tobe`로 복사하고 replay 결과·비교 파일에서 완료 요청으로 복구했다. 나머지 67개는 API 프로세스를 `ENRICHMENT_DIAGNOSTICS_FILE` 절대 경로로 재시작한 후 실행해야 한다.
- 현재 작업 트리에는 위 replay 스크립트가 없어 `scripts/replay-enrichment-diagnostics.py`를 다시 작성하고 사용법·Python 단위 테스트를 추가했다. 원본 파일의 2026-11-06 실행이 개선 전 기준선에 섞이지 않도록 기본 cutoff를 2026-11-05로 설정했으며, 이 기준으로 고유 subject 68개 중 63개를 manifest로 추출했다(계산 완료 60, 기존 결과 재사용만 기록된 3). 검색은 원본 artist/album/title로 각각 확인하고 import는 로그의 Spotify ID로 수행한다. 실행 결과는 provider 원본 태그·상태, bootstrap, resolved 차이와 `REUSED_EXISTING`/진단 누락을 구분한다. 현 작업 환경의 `127.0.0.1:8080`은 연결 거부이므로 실제 API replay와 사용자 실행 검증은 아직 완료되지 않았다.
- 2026-11-06: 사용자가 Windows IDE 실행 시 환경변수 설정 없이 replay를 수행하도록 요청했다. 이번 수동 진단 실행을 위해 `application-tag.yml`의 `diagnostics.enabled`를 `true`, 파일을 `D:/p/tag-your-story/logs/enrichment-diagnostics-tobe.jsonl`로 직접 지정했다. 이는 위 Scope의 opt-in 기본값과 달리 현재 설정으로 API를 실행하는 모든 환경에 적용되는 임시 기본값 변경이다. 원본 `enrichment-diagnostics.jsonl`은 유지한다. 실행 후 기본값을 원래 opt-in으로 되돌릴 필요가 있으며, 실제 API replay와 사용자 검증은 아직 완료되지 않았다.

## Closure Record (2026-11-09)

사용자 요청에 따라 기존 active Plan을 completed로 보관한다. 이 이동은 수용 기준 통과를 의미하지 않는다. Replay 비교의 최종 검증, `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`의 사용자 실행 결과, 임시 진단 설정 원복 여부 및 운영 migration 적용은 미확인이다. 이 Plan의 Last.fm 미매칭 태그 자동 생성과 태그별 Album 상속 표시는 ADR-030의 새 방향과 충돌한다. 후속 마이그레이션 Plan에서 충돌을 해결하고 기존 정책·데이터의 전환을 설계한다.
