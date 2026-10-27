# Progress

## 현재 진행: CATALOG-DETAIL-001 구현, 사용자 검증 대기 (2026-10-27)

- [CATALOG-DETAIL-001](plans/active/CATALOG-DETAIL-001.md): 사용자 승인에 따라 Album·Artist 공개 상세 조회를 Catalog 경계에 추가했다. 팔로워 수와 클라이언트 소스 변경은 제외했다. Swagger, Server Spec, API 계약 및 관련 테스트를 작성했으며 사용자 실행 검증 결과를 기다린다.

- [CATALOG-SEARCH-002](plans/completed/CATALOG-SEARCH-002.md): 선택적 `type=TRACK|ALBUM|ARTIST` 검색, Spotify 단일 타입 요청, 타입별 결과·total, Swagger·Server Spec·테스트를 작성했다. 기존 통합 검색과 `GET /api/tracks`는 유지한다. 2026-10-27 사용자 확인으로 완료 처리했다. Codex는 검증 명령을 실행하지 않았고 명령별 출력은 전달받지 않아 개별 통과 여부는 미확인이다.

- [AUTH-ME-001](plans/completed/AUTH-ME-001.md): 인증 사용자 닉네임 조회 API와 Google callback 조회를 구현하고 Swagger·클라이언트 계약을 갱신했다. 사용자 실행에서 발견된 컴파일 오류와 익명 principal SpEL 오류를 수정했으며, 2026-10-27 사용자 확인으로 완료 처리했다. 최종 검증 명령별 출력은 전달되지 않아 개별 통과 여부는 미확인이다.
- [NOTIFICATION-REF-001](plans/completed/NOTIFICATION-REF-001.md): 단일 서버 SSE 전달 경계를 Application port와 Infrastructure transport로 분리하고 다중 연결·heartbeat·최소 payload·재접속 조회를 구현했다. 2026-10-27 사용자 확인으로 완료 처리했다. 최종 검증 명령별 출력은 전달되지 않아 개별 통과 여부는 미확인이다.
- [COMMENT-REF-001](plans/completed/COMMENT-REF-001.md): 기존 8개 Comment API 계약을 특성화하고 최상위 댓글·답글 작성의 Board/User 조회 조율을 Application 서비스로 이동했다. Swagger interface와 요청·응답 Schema를 추가하고 저장·알림·조회 정책을 유지했다. 2026-10-26 사용자가 테스트 결과 확인 및 완료 처리를 요청했다. Codex는 검증 명령을 실행하지 않았고 `test`·`check`·`verify.sh`의 개별 출력은 전달받지 않아 명령별 통과 여부는 미확인으로 기록했다.
- [LIKE-REF-001](plans/completed/LIKE-REF-001.md): 기존 Like API 호환, DB unique·atomic counter 정합성, 마이페이지 좋아요 목록, Swagger와 migration runbook을 구현했다. 최초 테스트에서 발견된 status JSON 필드 `liked` 불일치를 정정했고, 2026-10-26 사용자가 확인 완료 및 완료 처리를 요청했다. 최종 검증 명령별 출력과 운영 migration 적용 결과는 전달되지 않아 완료 기록에 미확인으로 남겼다.

## User/Auth 마이그레이션 로드맵 (2026-10-14)

기존 `AUTH-001-proposal.md`를 독립 Plan으로 분할해 구현했으며, 2026-10-20 사용자가 AUTH-001~008 전체 테스트 완료와 Human Review 완료 처리를 확인했다. 모든 Auth Plan을 `plans/completed/`로 이동했다. 프론트 구현은 서버 단계와 별도로 진행한다.

| 순서 | Plan | 상태 | 완료 목표 |
| --- | --- | --- | --- |
| 1 | [AUTH-001](plans/completed/AUTH-001.md) | completed | ACCESS/REFRESH 타입 분리, Redis refresh 검증, 로그아웃 폐기 |
| 2 | [AUTH-002](plans/completed/AUTH-002.md) | completed | 정규화 이메일·provider identity·DB unique·기존 데이터 migration |
| 3 | [AUTH-003](plans/completed/AUTH-003.md) | completed | Google 즉시 가입, 형용사+동물+숫자 nickname, GUEST/PENDING_USER 제거 |
| 4 | [AUTH-004](plans/completed/AUTH-004.md) | completed | 메일 발송 adapter와 안전한 가입 challenge 저장 기반 |
| 5 | [AUTH-005](plans/completed/AUTH-005.md) | completed | 자체 이메일 가입·중복 확인·6자리 confirm |
| 6 | [AUTH-006](plans/completed/AUTH-006.md) | completed | 정규화 이메일·비밀번호 로그인 |
| 7 | [AUTH-007](plans/completed/AUTH-007.md) | completed | 닉네임 변경 API |
| 8 | [AUTH-008](plans/completed/AUTH-008.md) | completed | 탈퇴 API, 계정 비식별화, 게시글·댓글 공개 유지 |

공통 결정: 서버 이메일 `trim`+소문자화, Google/자체 계정 자동 연결 금지, 이미 Google로 가입한 이메일에 provider 안내 오류, 가입 전 이탈 시 DB 계정 없음, 닉네임 자동 생성과 변경 허용, 탈퇴 후 게시글·댓글 공개 유지. 관련 결정은 ADR-014~016과 서버 명세에 정렬했다.

- 2026-10-20: AUTH-001~008 완료.
  - 사용자가 `plans/active/` 전체 구현의 테스트 완료를 확인하고 Human Review 완료 처리를 요청했다.
  - JWT 타입 경계와 refresh token 검증·갱신·로그아웃 폐기, Google 즉시 가입, LOCAL 이메일 가입·로그인, 닉네임 변경, 회원 탈퇴·비식별화·공개 콘텐츠 보존을 완료했다.
  - SMTP와 Redis 가입 challenge, 이메일 인증 코드, Access/Refresh `Set-Cookie`, 토큰 갱신 전후 무효화 동작을 포함한 API 검증 결과를 확인했다.
  - 사용자 확인을 최종 검증 근거로 AUTH-001~008을 `plans/completed/`로 이동했다. 실행 명령별 원문 출력은 별도로 첨부되지 않았다.

- 2026-10-15: 사용자 요청으로 AUTH-003 구현 착수. ADR-015, Google 즉시 가입·canonical 인증·pending 역할 제거, OAuth Swagger 및 Oracle nickname migration runbook과 테스트를 작성했다. AUTH-002 사용자 재검증·운영 migration 결과와 AUTH-003 테스트/검증 결과는 아직 전달받지 못했으므로 Plan은 active에 둔다.

- 2026-10-16: 사용자 요청으로 AUTH-004 구현 착수. 내부 SMTP 발송과 Redis 가입 challenge, HMAC 코드 검증값, 재발송·실패 제한·발송 장애 보상, 설정·runbook 및 테스트를 작성했다. 사용자 실행 검증 결과 대기 중이며 Plan은 active에 둔다.
- 2026-10-16: 사용자 요청으로 AUTH-005 구현 착수. 이메일 가입 API, BCrypt 및 LOCAL 계정 저장, Redis 확인 선점·멱등 복구, 가용성 조회 제한, Swagger와 테스트를 작성했다. AUTH-004 및 AUTH-005 사용자 실행 검증 결과 대기 중이며 Plan은 active에 둔다.
- 2026-10-16: 사용자 요청으로 AUTH-006 구현 착수. LOCAL 로그인과 Redis 요청·실패 제한, 기존 token/cookie 경로 연결, Swagger와 테스트 코드를 작성했다. 기존 쿠키 전달과 CSRF 비활성화 정책은 유지했다. 사용자 실행 검증 결과 대기 중이며 Plan은 active에 둔다.
- 2026-10-16: 사용자 요청으로 AUTH-007 구현 착수. 인증 사용자 본인 닉네임 변경, DB unique 충돌 처리, USER 캐시 무효화, Swagger와 테스트 코드를 작성했다. 사용자 실행 검증 결과 대기 중이며 Plan은 active에 둔다.
- 2026-10-16: 사용자 요청으로 AUTH-008 구현 착수. ADR-016, 탈퇴 API와 식별자 tombstone, 쿠키/Redis 정리, 공개 작성자명·쓰기 상태 검사, Swagger, 운영 runbook 및 테스트 코드를 작성했다. 사용자 실행 검증 결과 대기 중이며 Plan은 active에 둔다.

- 2026-10-14: BOARD-TRACK-TAG-001 사용자 요청으로 completed 이동.
  - Board가 Catalog Track 또는 Album 중 정확히 하나를 FK로 참조하도록 ADR-013, JPA mapping, 기준 DDL과 migration runbook을 정렬했다.
  - Spotify `trackId`/`albumId` 기반 인증 Board 작성과 owner write 검증, 익명 조회 및 visible resolved System Tag bulk 조회를 구현했다.
  - Board HTTP endpoint를 Application command/query/write 경계로 전환하고 기존 Track API 호환 및 신규 Album 목록/count API를 유지했다.
  - 최초 JPA slice context의 누락된 `ArtistEntity` scan을 보완했으며, 사용자가 테스트 완료를 확인하고 Plan 완료 처리를 요청했다.
  - 전체 `test`, `check`, `scripts/verify.sh`의 개별 출력과 운영 DB migration preflight 결과는 전달받지 않아 Plan closure record에 미확인으로 남겼다.

- 2026-09-28: CATALOG-ALBUM-IMPORT-001 사용자 요청으로 completed 이동.
  - 통합 검색의 `subjectType`/`spotifyId`를 받는 Catalog import API와 Album 전용
    Spotify metadata/Catalog/enrichment/resolved tag 경로를 추가했다. 기존 Track import는 유지한다.
  - Album 빈 결과 완료 marker와 기준 DDL을 추가해 반복 선택에서 재수집을 피한다.
  - API 라우팅과 Album 선택 테스트를 추가했다. 최신 Gradle 및 전체 검증 결과는 전달받지 못했다.
  - 기존 `/api/tracks/import`는 호환 경로로 유지하고 Swagger/Java에서 deprecated로 표시했다.
  - MusicBrainz Album의 edition title을 첫 검색에 정규화하고 원문 fallback, 유일성/연도 유지 테스트를 추가했다.
  - 검증 결과를 완료로 주장하지 않고 미확인 상태를 Plan closure note에 남겼다.

- 2026-09-28: ENRICHMENT-MATCHING-001 사용자 요청으로 completed 이동.
  - 같은 ISRC의 MusicBrainz 복수 Recording은 metadata 허용 범위를 통과한 후보 중
    유일한 최소 duration 차이를 선택하도록 ADR-012와 matching test를 추가했다.
  - Discogs 검색을 canonical Album title, 제한된 box set base title, Track title 순서의
    search plan으로 분리하고 `release_title`/`track` query와 단계별 unique 검증을 구현했다.
  - 최신 Gradle 테스트·전체 검증 결과는 전달받지 못했다. Plan closure note에 미확인 상태를 남겼다.

- 2026-09-28: CATALOG-IMPORT-002 사용자 요청으로 completed 이동.
  - 이번 사용자 실행의 전체 `test`, `check`, `scripts/verify.sh` 성공을 확인했다.
  - 별도 Acceptance Criteria의 31 Observation DB execution 목표와 query baseline 기록·리뷰는 아직 확인되지 않았다. 미확인 상태를 Plan closure note에 남겼다.

- 2026-09-22: ALBUM-MBID-001 완료.
  - 사용자 요청으로 Album Release Group MBID 컬럼·index·attach/cache 제거를 승인했다.
  - ADR-011, 기준 DDL, Album 장르 observation 보존 경로와 관련 테스트를 갱신했다. 운영 오픈 전이므로 삭제 migration 문서는 두지 않는다.
  - 사용자가 전체 `test`, `check`, `scripts/verify.sh` 성공을 확인했다. 정적 diff 검토 후 Plan을 `plans/completed/`로 이동했다.

- 2026-09-22: TAG-BOOTSTRAP-001 완료.
  - seedless System Tag 생성, Observation evidence metadata, 0건 완료 marker,
    raw preview 및 API/Swagger 연결을 작성했다.
  - 대상 테스트와 migration runbook을 추가했다.
  - 첫 사용자 테스트에서 완료 marker의 제한된 JPA test context 매핑, 동시 Tag slug
    unique 복구, SQL 계측 assertion 오류를 확인해 수정했다. 재검증 결과 대기 중이다.
  - 재실행에서 JDBC batch 5건과 동시 import가 통과했다. 남은 2건은 Observation
    조회를 canonical Tag 조회로 함께 세던 테스트 계측 조건을 수정했다.
  - 사용자가 전체 `test`, `check`, `scripts/verify.sh` 성공을 확인했다. 정적 diff 검토 후 Plan을 `plans/completed/`로 이동했다.

- 2026-09-18: CATALOG-SEARCH-001 완료.
  - Spotify Track/Album/Artist 통합 검색, provider-neutral adapter, 결정적 ranking,
    분리된 Swagger API 및 회귀 테스트를 완료했다.
  - 사용자 실행 대상 테스트와 전체 `test`, `check`, `scripts/verify.sh` 통과를 확인했고,
    실행 기록을 `plans/completed/CATALOG-SEARCH-001.md`로 이동했다.

- 2026-09-18: ENRICHMENT-001 완료.
  - MusicBrainz/Discogs/Last.fm 외부 enrichment, exact matching, provider-local partial
    success, 병렬 collector 및 Catalog external identity write를 기존 tag pipeline에 연결했다.
  - 사용자 실행 대상 테스트와 전체 `test`, `check`, `scripts/verify.sh` 통과를 확인했고,
    실행 기록을 `plans/completed/ENRICHMENT-001.md`로 이동했다.

- 2026-09-18: ENRICHMENT-PERF-001 완료.
  - enrichment write sequence/JDBC batching, H2 schema, Oracle migration runbook 및 Catalog
    생성 snapshot 최적화를 완료했다.
  - 사용자 실행 대상 테스트와 전체 `test`, `check`, `scripts/verify.sh` 통과 및 Oracle
    migration review를 확인했고, 실행 기록을 `plans/completed/ENRICHMENT-PERF-001.md`로 이동했다.

- 2026-09-18: CATALOG-IMPORT-002 Human Review 승인.
  - 최초 Catalog read fast path는 유지하고, Spotify metadata 이후 write service의 Track 재조회는 제거하는 optimistic create 마일스톤을 승인.
  - `uk_track_spotify_id`, `uk_album_spotify_id`, `uk_artist_spotify_id`의 알려진 duplicate만 복구 대상으로 제한하고, Track canonical read 또는 부모 충돌 후 1회 create 재시도를 적용하기로 결정.
  - ADR-009와 `plans/active/CATALOG-IMPORT-002.md`를 추가했으며 production code와 자동 검증은 미실행.

- 2026-08-11: `board`, `search`, `user`, `spotify`, `track` 현재 구현 baseline 분석 완료.
  - `agents/server/current_state.md`에 package 구조, 호출 흐름, Entity 관계, public API, spec 충돌, 보호 동작, characterization test 제안 정리.
  - production code 변경 없음.
- 2026-08-11: characterization test baseline 구현 시작.
  - 추가 테스트:
    - `tagnote-api/src/test/java/com/tagnote/api/domain/board/BoardControllerTest.java`
    - `tagnote-api/src/test/java/com/tagnote/api/domain/tracks/TrackControllerTest.java`
    - `tagnote-api/src/test/java/com/tagnote/api/domain/user/UserControllerTest.java`
    - `tagnote-api/src/test/java/com/tagnote/api/support/WebMvcMethodSecurityTestConfig.java`
    - `tagnote-core/src/test/java/com/tagnote/domain/board/service/BoardFacadeTest.java`
    - `tagnote-core/src/test/java/com/tagnote/domain/board/service/BoardServiceCharacterizationTest.java`
    - `tagnote-core/src/test/java/com/tagnote/domain/tracks/service/TrackServiceTest.java`
    - `tagnote-core/src/test/java/com/tagnote/domain/user/service/UserServiceTest.java`
    - `tagnote-core/src/test/java/com/tagnote/domain/tracks/util/SearchKeywordTrackerTest.java`
  - production code 변경 없음.
  - `./gradlew test` 실행을 시도했으나 현재 실행 환경에 `JAVA_HOME`과 `java`가 없어 검증이 차단됨.
- 2026-08-11: Search Strangler Refactoring 계획 수립 완료.
  - `SEARCH-REF-001`에 기존 `GET /api/tracks` behavior를 유지하며 Search Application use case와 Spotify Infrastructure 경계를 만드는 최소 milestone을 정리.
  - Track 상세/랭킹, Catalog JPA 모델, Track Import, public API 변경은 후속 milestone로 제외.
  - production code 변경 없음.
  - 현재 실행 환경에 `JAVA_HOME`과 `java`가 없어 `./gradlew test`, `./gradlew check` 검증은 실행할 수 없음.
- 2026-08-12: SEARCH-REF-001 완료.
  - 기존 `GET /api/tracks`를 `TrackSearchService`와 provider/recorder port 경유로 전환.
  - Spotify SDK 검색 결과 mapping을 `SpotifyTrackSearchAdapter`로 격리하고 기존 HTTP response contract 유지.
  - 검색 경로의 legacy `TrackService.search`와 불필요한 read-only transaction 경계를 제거.
  - 신규 Search 테스트, 기존 Track characterization tests, 전체 Gradle `check`, `scripts/verify.sh` 통과.
  - 갱신된 API 명세 규칙에 따라 Springdoc 2.5.0과 `TrackApi` Swagger interface를 추가하고 Track 검색/상세/랭킹 contract 및 Search response schema를 문서화.
  - Acceptance Criteria, scope diff review 및 사용자 기능 확인 완료.
  - 실행 기록을 `agents/server/plans/completed/SEARCH-REF-001.md`로 이동.
- 2026-08-20: SEARCH-RANK-001 완료.
  - 검색어 기록/랭킹 조회 포트를 분리하고 Redis 구현체를 Infrastructure로 이동.
  - `TrackService`가 concrete Redis 구현체 대신 `SearchKeywordRankingReader`에 의존하도록 변경.
  - 대상 core 테스트와 랭킹 Controller 회귀 테스트 통과.
  - 전체 `test`, `check`, `scripts/verify.sh` 통과.
  - 실행 기록을 `agents/server/plans/completed/SEARCH-RANK-001.md`로 이동.
- 2026-08-20: CATALOG-001 완료.
  - 공개 `POST /api/tracks/import`와 Swagger contract를 추가하고 validation 오류를 HTTP 400으로 정렬.
  - Spotify Track/Album Artist 전체 credit을 provider-neutral metadata로 변환하여 내부 Artist, Album, Track 및 연결 Entity로 저장.
  - 외부 Spotify 호출과 짧은 Catalog write transaction을 분리하고 Spotify ID unique 제약 기반 멱등성/동시성 복구 구현.
  - 단방향 LAZY 관계, Artist credit EntityGraph 조회, Catalog PK/FK/unique/index 및 multi-artist ADR/명세 정렬 완료.
  - 동일 Track 및 공유 Album/Artist 병렬 Import 통합 테스트를 포함한 전체 `test`, `check`, `scripts/verify.sh` 통과.
  - 실행 기록을 `agents/server/plans/completed/CATALOG-001.md`로 이동.
- 2026-08-31: USER-TAG-001 요구사항 및 구현 계획 정렬.
  - 개인 커스텀 UserTag를 전역 공유 ID가 아닌 `(user_id, normalized_name)` 사용자 소유 identity로 정의.
  - 같은 이름이라도 사용자별로 다른 UserTag ID를 생성하고, 같은 사용자 내부에서만 기존 ID를 재사용하도록 ADR-002와 Server Spec 정렬.
  - 기존 공개 태그명 Board 조회는 단일 ID 선택 대신 normalized name에 해당하는 모든 사용자별 UserTag를 조회하도록 계획.
  - 기존 공유 데이터 migration, composite unique/FK, 동시성, N+1 및 API 회귀 검증을 `plans/active/USER-TAG-001.md`에 명시.
  - production code 변경 없음.
- 2026-09-01: USER-TAG-001 완료.
  - ADR-003으로 ADR-002의 normalization identity를 대체하고 UserTag를 `(user_id, name)` exact-name identity로 재정의.
  - 사용자 입력 이름을 변형 없이 보존하고 같은 이름으로 작성된 여러 Track의 Board를 플레이리스트형으로 조회하도록 명세와 active plan 수정.
  - UserTag normalization 컬럼·타입·서비스와 unique 충돌 자동 재시도를 제거하고 exact name bulk find-or-create 및 공개 조회로 전환.
  - owner/name unique, binary collation, BoardUserTag owner 불변식, migration runbook과 관련 테스트를 갱신.
  - 사용자 실행 테스트와 전체 `test`, `check`, `scripts/verify.sh` 통과를 확인.
  - 관련 범위 정적 diff 검토를 통과하고 migration 절차를 `agents/server/migrations/USER-TAG-001.md`에 기록.
  - 실행 기록을 `agents/server/plans/completed/USER-TAG-001.md`로 이동.
- 2026-08-31: TAG-CORE-001 완료.
  - 기존 `UserTag`와 분리된 System Tag taxonomy의 `Tag`, `TagAlias` 모델 및 DB 제약을 구현.
  - merge/alias 상태 불변식, Unicode 기반 이름 정규화, approved alias exact matching과 unmatched/ambiguous 결과를 구현.
  - Catalog 내부 PK 기반 `SubjectRef(TRACK | ALBUM)`와 Tag를 fetch join하는 bulk alias 조회를 추가하고 단일 쿼리로 N+1 부재를 검증.
  - 대상 테스트, 전체 `test`, `check`, `scripts/verify.sh`와 TAG-CORE-001 범위 diff 검사를 통과.
  - 전역 `git diff --check`는 이번 범위 밖의 기존 CRLF 작업 트리 변경 때문에 실패했으며, 해당 사용자 변경은 수정하지 않음.
  - 실행 기록을 `agents/server/plans/completed/TAG-CORE-001.md`로 이동.
- 2026-08-31: TAG-CORE-002 완료.
  - MusicBrainz/Discogs provider-neutral raw tag를 유실 없이 저장하는 `ExternalTagObservation`과 내부 taxonomy 근거인 `TagAssertion` 모델을 구현.
  - approved alias exact unique match만 MATCHED 및 자동 APPROVED assertion으로 승격하고 unmatched/ambiguous 값은 NEW로 보존.
  - Catalog 내부 PK 기반 Subject 단건 검증, observation/alias/assertion bulk 조회, DB composite unique 기반 멱등성과 rollback 후 1회 재시도를 구현.
  - 단방향 LAZY 관계, FK/unique/index, 입력 크기에 비례하지 않는 재처리 SELECT 및 동시 처리 중복 방지를 검증.
  - 대상 테스트와 WSL OpenJDK 17 전체 `test`, `check`, `scripts/verify.sh`, TAG-CORE-002 범위 diff 검사를 통과.
  - 전역 `git diff --check`는 이번 범위 밖의 기존 CRLF 작업 트리 변경 때문에 실패했으며, 해당 사용자 변경은 수정하지 않음.
  - 실행 기록을 `agents/server/plans/completed/TAG-CORE-002.md`로 이동.
- 2026-08-31: TAG-CORE-002 persistence conflict translation 후속 개선 완료.
  - DB unique constraint 이름 해석을 Application orchestration에서 Infrastructure translator로 이동.
  - Observation/Assertion duplicate 의미 예외만 제한적으로 재시도하고 retry warn 로그에 Subject와 conflict type을 기록.
  - 테스트 코드 갱신 후 사용자 실행 테스트 통과를 확인하고 Plan을 `plans/completed/TAG-CORE-002.md`로 이동.
- 2026-08-31: TAG-CORE-003 완료.
  - approved direct assertion을 ACTIVE canonical Tag로 결정하고 동일 Tag의 `max(confidence)` 및 설정 기반 minimum score를 적용하는 Resolver를 구현.
  - `SubjectTagResolved` projection의 AUTO insert/update/obsolete delete와 `MANUAL_FIXED`/`HIDDEN` 보존을 구현.
  - canonical merge cycle 실패 정책, 단방향 LAZY 관계, bulk 조회, DB FK/unique/index 및 unique 충돌 rollback 후 1회 재시도를 구현.
  - Track/Album direct resolution, 멱등성, N+1 부재, 수동 상태 보존 및 동시 실행 테스트를 포함한 전체 테스트와 검증 통과를 사용자 실행 결과로 확인.
  - MusicBrainz/Discogs HTTP 수집과 Album → Track inheritance는 후속 마일스톤 범위로 유지.
  - 실행 기록을 `agents/server/plans/completed/TAG-CORE-003.md`로 이동.
- 2026-08-31: TAG-CORE-004 완료.
  - Album approved direct assertion을 현재 Track의 inherited assertion으로 전파하고 `inherited_from_assertion_id` lineage를 보존.
  - Album → Track 상속 confidence에 `album-to-track-inheritance-weight=0.85`를 적용하고, stale inherited assertion diff insert/update/delete를 구현.
  - Track resolution에서 direct assertion과 inherited assertion을 함께 Resolver 입력으로 구성하되 같은 canonical Tag의 direct evidence를 inherited보다 우선하도록 구현.
  - inherited-only 결과는 `INHERITED_FROM_ALBUM` reason으로 `SubjectTagResolved`에 저장하고, AUTO-managed projection cleanup에 포함.
  - inherited assertion self-FK cascade delete, 단방향 LAZY 관계, fetch join 기반 고정 쿼리 및 unique 충돌 rollback 후 1회 재시도 경계를 유지.
  - 대상 테스트와 전체 검증 통과를 사용자 실행 결과로 확인.
  - 실행 기록을 `agents/server/plans/completed/TAG-CORE-004.md`로 이동.
- 2026-08-31: TAG-SLICE-001 완료.
  - `POST /api/tracks/import`를 Track 선택 Application 흐름에 연결하여 Catalog import/재사용, provider-neutral external tag 수집, Observation/Assertion, Album 상속, Resolver 및 resolved 상세 조회를 조율.
  - Fake External Tag와 고정 confidence는 통합 테스트 fixture에만 유지하고 production provider 목록이 비어 있는 구조를 구현.
  - import 응답에 `systemTags(tagId, name, score)`를 가산적으로 추가하고 Swagger contract, HIDDEN 제외, MANUAL_FIXED 노출 및 결정적 정렬을 반영.
  - 반복 선택 시 기존 resolved projection을 재사용하고, 동시 최초 선택에서도 Catalog/Observation/Assertion/Resolved row가 하나로 수렴하도록 검증.
  - 사용자 실행 대상 테스트와 전체 검증 통과를 확인하고 실행 기록을 `agents/server/plans/completed/TAG-SLICE-001.md`로 이동.
- 2026-09-01: ENRICHMENT-001 provider 범위 정렬.
  - MusicBrainz/Discogs에 Last.fm Track/Album community top tag provider를 추가하도록 Server Spec, System Tag 아키텍처와 active plan을 갱신.
  - ADR-004로 `LASTFM` source, `COMMUNITY_TAG` evidence, count gate-only 정책과 기존 Resolver `max(confidence)` 유지 결정을 기록.
  - Last.fm API key, exact entity validation, deterministic external reference, executor 크기 3, fixture/partial-success 테스트 범위를 Plan에 반영.
  - production code 변경 및 구현 검증 없음. ENRICHMENT-001은 Human Review 대기 상태 유지.
- 2026-09-15: ENRICHMENT-PERF-001 Human Review 승인.
  - ExternalTagObservation/TagAssertion/SubjectTagResolved의 `IDENTITY` PK를 allocation 50 sequence로 전환하고 JDBC batch size 50을 적용하는 범위를 승인.
  - Oracle/H2 sequence `INCREMENT BY 50` 일치, 기존 ID 이상의 안전한 시작 block, identity column migration preflight를 필수 수용 기준으로 확정.
  - 대상 Entity에 `@Version`이 없어 `batch_versioned_data`는 추가하지 않고, 현재 flush 구조에서 `order_inserts/order_updates`도 측정 근거 없이 활성화하지 않도록 결정.
  - Album/Track Observation transaction 통합은 risk 대비 추가 효과가 작아 보류하고, 신규 Catalog 생성 snapshot 반환으로 직후 재조회를 제거하는 범위를 포함.
  - ADR-008과 `plans/active/ENRICHMENT-PERF-001.md`를 추가했으며 구현과 자동 검증은 미실행.
