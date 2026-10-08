# ENRICHMENT-001 — MusicBrainz / Discogs / Last.fm External Enrichment

- Status: Implemented — User verification pending
- Scope: 기존 Track 선택 수직 슬라이스에 MusicBrainz/Discogs/Last.fm entity matching과 raw genre/style/community tag 수집을 병렬로 연결
- Non-goal: provider retry scheduler, enrichment status 영속화, 범용 external entity mapping, fuzzy/ML matching

## Goal

Spotify Track 선택으로 생성되거나 재사용된 Catalog metadata를 이용해 MusicBrainz, Discogs, Last.fm을 병렬 호출하고, 보수적으로 동일 entity를 판정한 결과의 raw genre/style/community tag를 기존 Tag 계산 파이프라인에 입력한다.

```text
Imported Track / Album / Artist credits
        │
        ├──────── MusicBrainz entity matching + genre 수집
        ├──────── Discogs album matching + genre/style 수집
        └──────── Last.fm Track/Album matching + community top tag 수집
                         │
                         ▼
             provider별 성공 결과만 취합
                         │
                         ▼
       ExternalTagObservation → Assertion → Resolver
```

이번 마일스톤은 실제 외부 수집을 기존 `ExternalTagProvider` 경계에 연결하는 가장 작은 수직 변경이다. 이미 구현된 Observation/Assertion/Album 상속/Resolver를 다시 설계하지 않는다.

## Context

현재 구현 상태는 다음과 같다.

- `POST /api/tracks/import`는 Spotify metadata를 외부 transaction에서 조회한 뒤 Artist/Album/Track과 전체 artist credit을 짧은 Catalog transaction으로 저장한다.
- `TrackSelectionService`는 `List<ExternalTagProvider>`를 순차 호출하고, 결과를 Observation/Assertion/Resolver에 연결한다.
- production `ExternalTagProvider` bean은 없고 Fake provider는 테스트 fixture에만 있다.
- `ExternalTagObservation`, `TagAssertion`, `SubjectTagResolved`, Album → Track 상속과 멱등성 제약은 구현 및 검증이 완료되었다.
- `track.musicbrainz_id`, `album.musicbrainz_id`, `artist.musicbrainz_id` 컬럼과 index는 있으나 값을 갱신하는 도메인 동작과 MusicBrainz client는 없다.
- `ImportedTrack`/`ImportedAlbum`은 현재 저장된 MusicBrainz ID를 전달하지 않는다.
- `ExternalTagInput`은 `source`, `rawName`, `externalRef`, `evidenceType`, `confidence`를 이미 수용한다.
- resolved projection이 하나라도 있으면 현재 선택 흐름은 provider를 다시 호출하지 않는다.

관련 기준:

- `agents/server/server_spec.md`의 Artist / Album / Track, Track 선택, MusicBrainz Entity Matching, External Tag 수집, transaction 및 provider 장애 정책
- `agents/server/server_tag_feature_architecture.md`의 MusicBrainz/Discogs/Last.fm 역할, 병렬 호출, Entity Matching, External Client, transaction 경계
- `agents/server/decisions/ADR-001-catalog-multi-artist-credits.md`
- `agents/server/decisions/ADR-004-lastfm-community-tag-evidence.md`
- `agents/server/decisions/ADR-006-lastfm-ranked-top-tag-selection.md`
- `agents/server/progress.md`
- MusicBrainz 공식 API: `https://musicbrainz.org/doc/MusicBrainz_API`
- MusicBrainz 공식 Search API: `https://musicbrainz.org/doc/MusicBrainz_API/Search`
- Discogs 공식 Developer API: `https://www.discogs.com/developers/`
- Last.fm 공식 API: `https://www.last.fm/api`
- Last.fm Track Top Tags: `https://www.last.fm/api/show/track.getTopTags`
- Last.fm Album Top Tags: `https://www.last.fm/api/show/album.getTopTags`

공식 API 제약상 MusicBrainz는 의미 있는 User-Agent와 애플리케이션 전체 기준 초당 1회 이하 호출이 필요하다. Discogs database search는 인증 token, User-Agent 및 rate-limit 대응이 필요하다. Last.fm top tag 조회는 API key와 rate-limit/error-code 대응이 필요하며 상업적 또는 연구 목적 사용 전 provider 문의가 필요할 수 있다. 이 제약은 provider 병렬화와 별개로 각 client 내부에서 지켜야 한다.

### Policy Gate — Human Review 승인 항목

아래 정책은 구현 전 승인 대상이다. 승인 없이 다른 수치나 fuzzy rule을 임의로 넣지 않는다.

#### Evidence confidence 제안

| Provider evidence | Subject | EvidenceType | Base confidence |
|---|---|---|---:|
| MusicBrainz Recording genre | Track | `EXPLICIT_GENRE` | `0.90` |
| MusicBrainz Release Group genre | Album | `EXPLICIT_GENRE` | `0.75` |
| Discogs genre | Album | `EXPLICIT_GENRE` | `0.70` |
| Discogs style | Album | `EXPLICIT_STYLE` | `0.85` |
| Last.fm Track top tag | Track | `COMMUNITY_TAG` | `0.65` |
| Last.fm Album top tag | Album | `COMMUNITY_TAG` | `0.60` |

- MusicBrainz의 일반 `tags`는 mood, location 등 genre/style이 아닌 값이 섞일 수 있으므로 이번 마일스톤에서는 `genres`만 사용한다.
- MusicBrainz genre의 `count <= 0`은 제외한다.
- Last.fm top tag는 count로 필터링하지 않고, 빈 이름과 정규화 기준 중복을 제외한 provider 응답 상위 5개까지만 수집한다. 유효 tag가 5개 미만이면 반환된 tag만 수집하며 count는 confidence에도 반영하지 않는다.
- Last.fm top tag는 genre/style/mood/개인 분류가 섞일 수 있으므로 `EXPLICIT_GENRE`나 `EXPLICIT_STYLE`로 추측하지 않고 `COMMUNITY_TAG`로 기록한다.
- provider vote/count/popularity는 confidence에 반영하지 않는다.
- entity matching은 통과/실패의 gate로만 사용하고 matching score를 evidence confidence에 곱하지 않는다.
- 같은 provider/evidence type의 base confidence는 고정하여 기존 Assertion unique key와 `putIfAbsent` 처리에서 입력 순서에 따른 점수 차이가 생기지 않게 한다.
- 수치는 `tag.enrichment.evidence-confidence` 설정으로 관리하며 adapter에 하드코딩하지 않는다.

#### Entity matching 통과 기준 제안

공통 문자열 비교는 별도 music entity normalizer를 사용한다.

- Unicode NFKC
- trim, `Locale.ROOT` lowercase, 연속 공백 축약
- punctuation과 edition/remix 표시는 임의로 제거하지 않음. 단, 아래 Discogs Album 전용 canonical identity 규칙은 예외로 한다.
- System Tag `TagNameNormalizer`를 재사용하지 않음
- Levenshtein, 임의 가중 fuzzy score, MusicBrainz/Discogs search score 또는 Last.fm autocorrect 결과 단독 채택은 하지 않음

MusicBrainz Recording:

1. ISRC가 있으면 구분자 제거/대문자화한 exact ISRC lookup을 우선한다.
2. exact ISRC lookup 후보가 하나이고 artist credit 한 명 이상이 exact이면 title과 duration 차이에 관계없이 채택한다. MusicBrainz Recording length는 연결 Track 길이의 중앙값이고 remaster 표기는 provider 간 제목 표현이 다를 수 있으므로, 유일한 ISRC 후보의 duration/title은 거부 조건으로 사용하지 않는다.
3. exact ISRC lookup 후보가 여러 개이면 normalized title exact, artist credit 한 명 이상 exact, duration 차이 `<= 3,000ms`를 모두 만족하는 유일 후보만 채택한다.
4. ISRC가 없거나 유효 후보가 없으면 title + 전체 artist name으로 recording search한다.
5. fallback 후보는 normalized title exact, artist overlap, duration 차이 `<= 3,000ms`를 모두 만족하는 유일 후보만 채택한다.
6. artist가 다른 단일 ISRC 후보, 동률, 필수 비교값 누락으로 안전하게 하나를 고를 수 없음, 상충하는 결과는 `NOT_FOUND/AMBIGUOUS`로 취급하고 MBID와 tag를 저장하지 않는다.

MusicBrainz Release Group:

- 채택된 Recording에 연결된 Release Group 후보만 사용한다.
- normalized album title exact와 album artist overlap을 필수로 한다.
- 양쪽 release year가 있으면 동일 연도만 허용한다.
- 유일 후보일 때만 `album.musicbrainz_id`로 채택한다.
- Recording은 채택됐지만 Release Group이 불명확하면 Track MBID/genre만 사용하고 Album MBID/genre는 생략한다.

Discogs Album:

- MusicBrainz 성공에 의존하지 않고 Spotify album title과 전체 album artist name으로 Master를 먼저 검색하고, release year는 복수 후보 판별에 사용한다.
- Catalog에 저장하는 Spotify 원문 title/artist는 변경하지 않고 Discogs 검색·비교용 canonical identity만 별도로 계산한다.
- Discogs 검색에는 trailing qualifier를 제거한 canonical album title을 사용한다. 제거 대상은 remaster/remastered, deluxe/super deluxe, expanded/special/legacy/anniversary edition, bonus track/international version, reissue/re-release, mono/stereo 및 explicit/clean 판본 표시로 제한한다.
- 괄호·대괄호 또는 ` - `, ` – `, ` — ` 뒤의 전체 trailing qualifier가 허용 패턴과 일치할 때만 제거한다. 제목 중간 문자열이나 일반 부제는 제거하지 않는다.
- Live, Remix/Remixed, Acoustic/Unplugged, Instrumental, Demo, Soundtrack/Score, Karaoke/Tribute/Cover, Re-recorded/Taylor's Version, EP/Single, Part/Volume/Act는 의미가 달라지는 표기로 간주하여 제거하지 않는다.
- Discogs 비교에서 curly quote와 dash variant를 표준 quote/dash로 정규화하고, Discogs가 동명이인 구분용으로 붙이는 Artist 끝의 숫자 suffix `(2)`, `(3)` 등은 Discogs 후보 측에서만 제거한다.
- title과 artist overlap은 먼저 원문 normalized exact 후보를 우선하고, 없을 때만 위 canonical exact 후보를 사용한다.
- release year는 database search 선필터나 필수 거부 조건으로 사용하지 않는다. title+artist 후보가 복수일 때 동일 연도 후보가 존재하면 그 후보군으로 좁히는 보조 판별값으로만 사용한다.
- 조건을 만족하는 Master 후보가 하나이면 채택하고 Release는 검색하지 않는다.
- 조건을 만족하는 Master 후보가 없을 때만 Release를 검색하며, 유일한 exact Release 후보만 fallback으로 채택한다.
- Master 후보가 복수이면 Release로 fallback하지 않고 ambiguity로 거부한다. Release 후보가 복수인 경우에도 임의의 낮은 ID나 검색 순서로 해소하지 않는다.
- Master 요청의 timeout/HTTP 실패를 Master 후보 없음으로 간주하지 않으며 Release fallback으로 장애를 숨기지 않는다.
- 선택한 Master 또는 Release detail의 `genres`, `styles`만 Album evidence로 사용한다.

2026-09-10 Human Review에서 Master 우선/Release fallback, Discogs 전용 trailing edition qualifier 정제, Artist 숫자 suffix 및 Unicode punctuation 정규화, year의 복수 후보 보조 판별 정책을 승인했다.

Last.fm Track / Album:

- MusicBrainz와 Discogs 성공에 의존하지 않고 top-level branch를 병렬 시작한다.
- `autocorrect=0`으로 요청하고 Spotify의 대표 Artist(`position=0`)와 Track/Album title을 사용한다.
- Track/Album 모두 MusicBrainz MBID를 Last.fm에 전달하지 않고 항상 Spotify catalog의 대표 Artist와 title로 조회한다.
- artist/title 요청에서는 응답이 돌려준 Artist와 Track/Album title이 normalized exact일 때만 채택한다. 응답 identity가 누락되거나 다르면 `NOT_FOUND`로 처리한다.
- Track 원문 identity가 exact match이지만 tag가 비어 있고 approved edition qualifier를 제거한 title이 다를 때만 canonical title로 한 번 fallback한다. Discogs와 공유하는 qualifier 제거 로직은 Live/Remix/Acoustic 등을 보존한다.
- stable provider entity ID가 없는 경로의 `externalRef`는 `lastfm:track:{sha256}` 또는 `lastfm:album:{sha256}` 형식으로 만들며, hash 입력은 subject type과 normalized representative artist/title을 구분자를 포함해 결정적으로 직렬화한다.
- count와 무관하게 빈 이름과 정규화 기준 중복을 제외한 응답 상위 5개의 raw tag를 `COMMUNITY_TAG` evidence로 보존한다. approved alias exact unique match 여부는 기존 pipeline에서 결정한다.

2026-09-11 Human Review에서 마이너 Track/Album coverage를 위해 Last.fm `count >= 20`, 상위 10개 정책을 count 미사용, subject별 상위 5개 정책으로 변경했다.

2026-09-15 Human Review에서 Last.fm이 MusicBrainz Recording MBID를 Track resource로 해석하지 못해 HTTP 400을 반환한 실제 오류를 근거로, Last.fm Track을 항상 Spotify catalog의 대표 Artist/title로 조회하도록 변경했다.

2026-09-15 Human Review에서 exact edition title identity에 top tag가 비어 있는 Last.fm 응답을 보완하기 위해, 빈 tag에 한해 기존 approved edition canonical title로 1회 fallback하는 정책을 추가했다.

`3,000ms`, 동일 연도 조건, Last.fm maximum tag count 및 confidence 표가 Human Review에서 변경되면 이 Plan의 Policy Gate를 먼저 갱신한다. Last.fm source와 `COMMUNITY_TAG` evidence 의미는 ADR-004를, count 미사용과 상위 5개 선택은 ADR-006을, MusicBrainz MBID와 분리된 Artist/Track metadata 조회는 ADR-007을 따른다. provider agreement bonus나 count 기반 confidence처럼 Resolver 의미를 추가로 변경하려면 구현 전에 별도 ADR을 작성한다.

## Target State

```text
TrackController
  → TrackSelectionService                         [transaction 없음]
      → TrackImportService
          → Spotify HTTP                          [transaction 없음]
          → CatalogWriteService                   [짧은 transaction]
      → ExternalEnrichmentCollector               [transaction 없음]
          → CompletableFuture + 전용 fixed executor
              ├─ MusicBrainzExternalTagProvider
              │    → MusicBrainzClient
              │    → MusicBrainzEntityMatchingService
              │    → Recording / Release Group genre + accepted identity 반환
              ├─ DiscogsExternalTagProvider
              │    → DiscogsClient
              │    → DiscogsAlbumMatchingService
              │    → Master / Release genre + style
              └─ LastFmExternalTagProvider
                   → LastFmClient
                   → LastFmEntityMatchingService
                   → Track / Album community top tags
      → 성공 provider의 CollectedExternalTags만 취합
      → CatalogExternalIdentityWriteService       [짧은 transaction]
      → 기존 ObservationProcessingService        [짧은 transaction]
      → 기존 TagResolutionService                 [짧은 transaction]
      → TrackDetailReadService                     [readOnly transaction]
```

- provider 한 개의 timeout/not-found/failure는 다른 provider의 성공 데이터를 폐기하지 않는다.
- top-level provider 호출만 병렬화한다. MusicBrainz branch 내부의 candidate search → entity lookup처럼 선후관계가 있는 요청은 순차로 수행한다.
- 새 provider는 `ExternalTagProvider` 구현과 provider 설정을 추가해 collector에 참여시킬 수 있다. `TrackSelectionService`에 provider별 `if/switch`를 추가하지 않는다.
- client는 HTTP/JSON mapping과 provider protocol만 담당하고, 동일 entity 판단 규칙은 Application matching service에 둔다.
- Controller, Domain Entity, Resolver는 HTTP client/response DTO를 알지 않는다.

## Scope

### In Scope

- MusicBrainz Recording 및 Release Group candidate/lookup client와 JSON DTO/mapper
- Discogs database search 및 Master/Release detail client와 JSON DTO/mapper
- Last.fm `track.getTopTags`/`album.getTopTags` client와 JSON DTO/mapper
- MusicBrainz Recording/Release Group의 보수적 entity matching
- Discogs Album의 보수적 entity matching
- Last.fm Track/Album의 autocorrect 없는 exact entity validation
- MusicBrainz Recording MBID를 Track, Release Group MBID를 Album에 idempotent하게 연결
- MusicBrainz Recording/Release Group genre → provider-neutral `ExternalTagInput`
- Discogs Album genre/style → provider-neutral `ExternalTagInput`
- Last.fm Track/Album community top tag → provider-neutral `ExternalTagInput`
- `ExternalTagSource.LASTFM`, `AssertionSource.LASTFM`, `EvidenceType.COMMUNITY_TAG` 추가와 기존 string enum persistence 회귀 검증
- 안정적인 lineage용 `externalRef`
  - `musicbrainz:recording:{mbid}`
  - `musicbrainz:release-group:{mbid}`
  - `discogs:master:{id}` 또는 `discogs:release:{id}`
  - `lastfm:track:{sha256}` 또는 `lastfm:album:{sha256}`
- `CompletableFuture.allOf`와 전용 fixed-size executor를 사용한 provider 병렬 호출
- provider별 connect/read timeout, 전체 first-load budget, MusicBrainz 요청 간격/User-Agent, Discogs token/User-Agent, Last.fm API key/count limit 설정
- timeout/not-found/failure의 provider-local 결과 변환과 성공 결과의 deterministic aggregation
- 기존 Observation/Assertion/Resolver 연결 및 public API 회귀 검증
- HTTP response fixture mapping, matching, 병렬/partial success, transaction, JPA identity update 테스트

### Provider Result Semantics

collector 내부 결과는 최소한 다음을 구분한다.

```text
SUCCESS     = 채택된 entity에서 1개 이상의 유효 genre/style/community tag 수집
EMPTY       = entity는 채택됐지만 유효 genre/style/community tag 없음
NOT_FOUND   = 유일하게 채택 가능한 entity 없음 또는 ambiguity
TIMEOUT     = provider/client budget 초과
FAILED      = 429/5xx/역직렬화/예상하지 못한 provider 오류
```

- `SUCCESS`와 `EMPTY`는 정상 branch 종료다.
- `NOT_FOUND`, `TIMEOUT`, `FAILED`는 import 요청 전체 예외로 전파하지 않고 provider/source/subject/provider status를 구조화 로그로 남긴다.
- 이번 마일스톤은 상태를 DB에 영속화하지 않으므로 이 enum은 orchestration 결과와 테스트 의미로만 사용한다.
- programmer error와 내부 DB write 실패까지 provider 장애로 숨기지 않는다. 외부 client 계열 예외만 partial-failure로 격리한다.

## Do Not Touch

- Spotify search/metadata adapter와 Spotify Catalog identity 정책
- 공개 endpoint, 인증, request/response 필드, HTTP status, 기존 `error_code`/`custom_error_code`
- 기존 `GET /api/tracks/{trackId}`의 Spotify ID 의미
- Artist/Album/Track artist credit 관계와 ADR-001
- `ExternalTagObservation`, `TagAssertion`, `SubjectTagResolved` schema/unique key
- Tag normalization, Alias Matching, Assertion 생성 규칙, Resolver minimum/max/canonical 정책
- Album → Track inheritance weight와 lineage
- UserTag, Board, Search Ranking, Like, Notification
- `subject_enrichment_status` 또는 provider attempt 테이블
- enrichment retry scheduler/batch와 backoff 정책
- `external_entity_mapping` 범용 테이블 및 Discogs ID 별도 영속화
- Artist MusicBrainz matching/`artist.musicbrainz_id` 갱신
- MusicBrainz 일반 tag, rating, relationship graph 수집
- Discogs track-level tag 해석
- Last.fm user별 personal tag, scrobble, rating, similar/recommendation 수집
- Last.fm count를 confidence로 정규화하거나 provider agreement bonus를 적용하는 Resolver 변경
- provider response cache, Redis/distributed lock, queue/event bus
- WebFlux/reactive chain, virtual thread 전환, 기존 공용 async executor의 전면 개편
- active `USER-TAG-001` 범위와 관련 파일

## 변경/생성 파일

구현 착수 시 package 충돌을 다시 확인하되 예상 범위는 다음과 같다.

### 변경 파일

- `tagnote-core/src/main/java/com/tagnote/application/catalog/selection/TrackSelectionService.java`
  - 순차 provider loop를 collector 호출로 교체하고 기존 pipeline 순서는 유지
- `tagnote-core/src/main/java/com/tagnote/application/enrichment/port/ExternalTagProvider.java`
  - provider 식별과 provider-local 결과를 반환하는 최소 contract로 확장
- `tagnote-core/src/main/java/com/tagnote/application/enrichment/model/CollectedExternalTags.java`
  - deterministic merge helper가 필요할 때만 보완
- `tagnote-core/src/main/java/com/tagnote/domain/enrichment/observation/ExternalTagSource.java`
- `tagnote-core/src/main/java/com/tagnote/domain/enrichment/assertion/AssertionSource.java`
- `tagnote-core/src/main/java/com/tagnote/domain/enrichment/assertion/EvidenceType.java`
  - `LASTFM` source와 `COMMUNITY_TAG` evidence 추가
- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/model/ImportedTrack.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/model/ImportedAlbum.java`
  - 기존 MusicBrainz ID를 provider가 재사용할 수 있도록 nullable field 추가
- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/CatalogTrackReadService.java`
  - Track/Album MusicBrainz ID mapping
- `tagnote-core/src/main/java/com/tagnote/domain/catalog/track/TrackEntity.java`
- `tagnote-core/src/main/java/com/tagnote/domain/catalog/album/AlbumEntity.java`
  - null → accepted MBID 설정, 동일 값 재적용 허용, 다른 non-null 값 overwrite 거부
- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/catalog/TrackJpaRepository.java`
- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/catalog/AlbumJpaRepository.java`
  - 동시 identity 연결 시 Track과 공유 Album row를 명시적으로 잠그는 조회 추가
- `tagnote-core/src/main/resources/application-tag.yml`
  - provider enablement, timeout/budget, matching tolerance, confidence, User-Agent/token/API-key placeholder
- `tagnote-api/src/main/java/com/tagnote/api/domain/tracks/TrackApi.java`
  - contract 변경 없이 enrichment partial-result 의미만 문서화
- 영향받는 기존 selection/import/detail tests
- `agents/server/progress.md`
  - 구현, Acceptance Criteria, 사용자 검증 완료 후에만 갱신
- `agents/server/plans/active/ENRICHMENT-001.md`
  - 완료 후 사용자 검증 결과를 기록하고 `plans/completed/`로 이동

### 생성 파일

예상 책임 기준 이름이며 구현 시 불필요하게 세분화하지 않는다.

- `application/enrichment/ExternalEnrichmentCollector.java`
- `application/enrichment/model/ProviderEnrichmentResult.java`
- `application/enrichment/model/ProviderEnrichmentStatus.java`
- `application/enrichment/model/CatalogExternalIdentityMatch.java`
- `application/enrichment/config/ExternalEnrichmentProperties.java`
- `application/enrichment/matching/MusicEntityNameNormalizer.java`
- `application/enrichment/matching/MusicEditionTitleNormalizer.java`
- `application/enrichment/matching/DiscogsAlbumIdentityNormalizer.java`
- `application/enrichment/matching/MusicBrainzEntityMatchingService.java`
- `application/enrichment/matching/DiscogsAlbumMatchingService.java`
- `application/enrichment/matching/LastFmEntityMatchingService.java`
- `application/catalog/importer/CatalogExternalIdentityWriteService.java`
- `application/enrichment/provider/MusicBrainzExternalTagProvider.java`
- `application/enrichment/provider/DiscogsExternalTagProvider.java`
- `application/enrichment/provider/LastFmExternalTagProvider.java`
- `application/enrichment/port/MusicBrainzCatalogClient.java`
- `application/enrichment/port/DiscogsCatalogClient.java`
- `application/enrichment/port/LastFmCatalogClient.java`
  - Application provider가 Infrastructure 구현에 역의존하지 않도록 provider별 client port 정의
- `infrastructure/external/enrichment/ExternalEnrichmentConfig.java`
- `infrastructure/external/musicbrainz/MusicBrainzClient.java`
- MusicBrainz HTTP response DTO/mapper
- `infrastructure/external/discogs/DiscogsClient.java`
- Discogs HTTP response DTO/mapper
- `infrastructure/external/lastfm/LastFmClient.java`
- Last.fm HTTP response DTO/mapper
- 위 구성요소의 unit/integration test와 최소 JSON fixture

Spring Boot 3.2의 blocking `RestClient`와 기존 `spring-boot-starter-web`을 우선 사용한다. 실제 구현 확인 결과 추가 HTTP/runtime dependency가 필요하지 않으면 `build.gradle`은 변경하지 않는다.

### 생성하지 않는 파일

- ADR-004 외 신규 ADR: provider agreement bonus, 범용 mapping/status schema 또는 기존 Resolver 의미의 추가 변경이 요구될 때만 다음 번호 ADR을 먼저 작성한다.
- 신규 public DTO/Controller/API
- 신규 DB migration/schema 파일
- 실제 운영 secret을 포함한 설정 파일

## 데이터 흐름

### 이미 resolved projection이 있는 Track

```text
POST /api/tracks/import
→ Catalog Track 재사용
→ hasResolvedProjection = true
→ MusicBrainz/Discogs/Last.fm 호출 없음
→ 기존 resolved systemTags 반환
```

기존 빠른 경로를 유지한다.

### 최초 또는 resolved projection이 없는 Track

```text
Spotify metadata 조회                           [transaction 없음]
→ Catalog import/upsert                         [짧은 transaction, commit]
→ ImportedTrack + Album + 전체 artist credit
→ ExternalEnrichmentCollector                  [transaction 없음]
   ├─ MusicBrainz future
   │   ├─ 저장된 Recording/Release Group MBID가 있으면 matching 재사용
   │   ├─ 없으면 ISRC 우선, title/artist/duration fallback matching
   │   └─ accepted MBID + Recording/Release Group genres 반환
   ├─ Discogs future
   │   ├─ album title/all artists/year matching
   │   └─ selected Master/Release genres/styles mapping
   └─ Last.fm future
       ├─ representative artist/title 조회 및 exact response validation
       ├─ exact Track tag가 비면 canonical edition title로 1회 fallback
       ├─ Track/Album top tags 응답 순서 기준 subject별 상위 5개 선택
       └─ accepted community tags mapping
→ 전체 budget 내 완료한 provider 결과 취합
→ accepted MusicBrainz MBID 저장                  [별도 짧은 transaction]
→ Album inputs Observation/Assertion            [기존 짧은 transaction]
→ Album resolve                                 [기존 짧은 transaction]
→ Track inputs Observation/Assertion            [기존 짧은 transaction]
→ Track inheritance + resolve                   [기존 짧은 transaction]
→ resolved projection 조회 및 기존 response 반환
```

### Partial failure

```text
MusicBrainz SUCCESS ─┐
Discogs TIMEOUT    ──┼→ MusicBrainz/Last.fm inputs만 저장/resolve → 정상 응답
Last.fm SUCCESS    ──┘
```

```text
MusicBrainz FAILED ──┐
Discogs SUCCESS   ───┼→ Discogs Album + Last.fm Track inputs 저장
Last.fm SUCCESS   ───┘  → Album resolve/inheritance
                        → Track resolve → 정상 응답
```

세 provider가 모두 실패하거나 matching되지 않아도 Spotify Catalog import 자체는 성공하고 기존 응답 shape로 빈 `systemTags`를 반환한다.

### Deterministic aggregation

- future 완료 순서가 아니라 `MUSICBRAINZ → DISCOGS → LASTFM` 고정 순서로 provider 결과를 merge한다.
- provider 내부 raw 값은 응답의 원본 문자열을 보존하되, null/blank와 정책상 제외된 count만 걸러낸다.
- 동일 provider/source/normalized name/external ref 중복은 mapper에서 한 번 정리하고, DB unique 제약이 최종 방어한다.
- provider가 subject ID를 만들지 않는다. Track/Album subject는 항상 imported Catalog 내부 PK로 결정한다.

## DB/JPA 설계

이번 마일스톤은 신규 테이블/컬럼/unique/FK/index를 만들지 않는다.

사용하는 기존 컬럼:

| Entity | Column | 의미 |
|---|---|---|
| Track | `musicbrainz_id` | accepted MusicBrainz Recording MBID |
| Album | `musicbrainz_id` | accepted MusicBrainz Release Group MBID |

JPA 변경 원칙:

- `TrackEntity.attachMusicBrainzRecordingId(...)`, `AlbumEntity.attachMusicBrainzReleaseGroupId(...)`처럼 의미가 드러나는 동작으로만 갱신한다.
- 현재 값이 null이면 accepted ID를 저장한다.
- 현재 값과 같은 ID의 반복 처리는 no-op이다.
- 다른 non-null ID가 이미 있으면 overwrite하지 않고 identity conflict로 실패시켜 운영 로그에 남긴다.
- MBID에는 unique 제약을 추가하지 않는다. 여러 Spotify catalog row가 같은 Recording/Release Group에 대응할 수 있다.
- 기존 index만 유지한다.
- Artist MBID는 갱신하지 않는다.
- Discogs ID와 Last.fm deterministic entity reference는 `external_tag_observation.external_ref`에 lineage로 남기고 별도 Catalog column을 추가하지 않는다.
- 새 양방향 관계, EAGER 관계, cascade를 추가하지 않는다.

Catalog MBID write는 Track과 그 Album을 내부 PK로 한 번 조회해 같은 짧은 transaction에서 필요한 값만 dirty checking한다. provider 후보 수에 비례한 JPA SELECT나 Artist N+1을 만들지 않는다. Matching 입력의 artist credit은 이미 `CatalogTrackReadService`가 position 순으로 명시 조회한 `ImportedTrack`을 사용한다.

기존 정합성 제약은 그대로 최종 방어선으로 사용한다.

```text
UNIQUE external_tag_observation(
  subject_type, subject_id, source, normalized_name, external_ref
)

UNIQUE tag_assertion(
  subject_type, subject_id, tag_id, source, evidence_type
)

UNIQUE subject_tag_resolved(
  subject_type, subject_id, tag_id
)
```

## Transaction

`TrackSelectionService`, `ExternalEnrichmentCollector`, provider 및 HTTP client에는 class/method 범위 `@Transactional`을 붙이지 않는다.

경계는 다음과 같다.

1. Spotify metadata HTTP: transaction 없음
2. Catalog 최초 저장: 기존 `CatalogWriteService.create()`의 짧은 transaction
3. MusicBrainz/Discogs/Last.fm HTTP 및 entity matching: transaction 없음
4. accepted MusicBrainz ID 연결: `CatalogExternalIdentityWriteService`의 별도 짧은 transaction
5. Observation + Alias Matching + Assertion: 기존 `ObservationWriteService` transaction
6. Album/Track inheritance 및 resolution: 기존 resolution transaction
7. 상세 조회: 기존 read-only transaction

테스트 provider/client 진입 지점에서 `TransactionSynchronizationManager.isActualTransactionActive()`가 false인지 검증한다.

provider future는 HTTP/matching 결과만 반환하고 DB write를 수행하지 않는다. collector가 전체 budget 안에 채택한 결과만 future 종료 후 별도 MBID write transaction에 전달한다. 따라서 timeout 처리된 background branch가 뒤늦게 Catalog를 변경하지 않으며, 어떤 DB transaction도 외부 future 완료를 기다리지 않는다. `CompletableFuture.join()`은 transaction 없는 collector에서만 수행한다.

동시 최초 선택 시 두 요청이 같은 MBID를 설정해도 동일 값이면 수렴한다. 서로 다른 accepted MBID가 경합하면 마지막 값으로 덮지 않고 conflict를 노출한다. Observation/Assertion/Resolved 중복은 기존 DB unique와 제한적 재시도 경계를 재사용하며 분산 락을 추가하지 않는다.

## 병렬 호출 및 외부 Client 정책

- `CompletableFuture.supplyAsync(..., externalEnrichmentExecutor)`와 `allOf`를 사용한다.
- 전용 executor는 세 provider가 동시에 시작될 수 있도록 기본 3개 fixed worker로 시작하고 bean 이름을 명시한다. 기존 범용 `threadPoolTaskExecutor`를 암묵적으로 주입하지 않는다.
- executor lifecycle은 Spring bean이 관리하여 종료 시 shutdown한다.
- 전체 first-load budget과 provider connect/read timeout을 별도 설정한다.
- future timeout만 믿지 않고 HTTP client 자체 timeout도 설정해 background request가 무기한 남지 않게 한다.
- provider exception은 해당 future에서 `ProviderEnrichmentResult`로 변환하여 `allOf`가 다른 성공 future를 폐기하지 않게 한다.
- interrupt/cancellation 상태를 삼키지 않는다.
- MusicBrainz client는 애플리케이션 전체에서 요청 시작 간격 1초 이상을 보장하고 의미 있는 User-Agent/contact를 전송한다.
- MusicBrainz ISRC lookup은 운영 API에서 HTTP 200이 확인된 `artist-credits`만 요청하고 Recording 후보 매칭에만 사용한다. 공식 문서의 포괄적 설명과 달리 운영 ISRC resource는 `release-groups`와 `genres`를 유효한 `inc`로 인정하지 않으므로 ISRC 응답 detail 재사용 최적화는 적용하지 않는다. ISRC 또는 metadata fallback 후보가 채택되면 필수 Recording lookup은 `artist-credits+genres`만 사용한다. 응답 지연이 큰 `releases` lookup은 Release Group 후보가 필요할 때만 별도 optional 단계에서 실행한다.
- collector는 전체 first-load 절대 deadline을 provider에 전달한다. MusicBrainz는 Recording match/detail을 필수 단계로, Release Group detail을 optional 단계로 취급한다. optional 요청의 최악 비용(`minimum-request-interval + connect-timeout + read-timeout + completion-safety-margin`)보다 남은 시간이 적으면 Release Group 요청을 시작하지 않고 이미 확보한 Recording tag와 MBID를 반환한다.
- MusicBrainz branch의 필수 단계에서 처음 발생한 `TIMEOUT` 또는 HTTP `503`은 공유 재시도 슬롯으로 최대 한 번 재시도한다. `NOT_FOUND`, HTTP `400`/`429`, matching 실패, malformed response 및 그 밖의 실패는 재시도하지 않으며, 재시도도 app-wide request gate와 전체 deadline을 준수한다. optional Recording releases/Release Group 요청은 재시도하지 않는다.
- Discogs client는 token을 secret 환경변수로 받고 User-Agent를 전송하며 429를 `FAILED`로 분류한다. 이번 request path에서 즉시 재시도/backoff하여 first-load budget을 잠식하지 않는다.
- Last.fm client는 API key를 secret 환경변수로 받고 `autocorrect=0`, JSON format으로 호출한다. Last.fm error code `29` 또는 HTTP 429는 `FAILED`, entity 없음/invalid resource는 `NOT_FOUND`로 분류하며 이번 request path에서 즉시 재시도하지 않는다.
- provider bean은 `enabled` 설정으로 조건부 등록한다. test/local context가 실제 secret 부재만으로 시작 실패하지 않게 하고, enabled provider의 필수 설정 누락은 startup validation으로 실패시킨다.

설정 key 초안:

```yaml
tag:
  enrichment:
    executor-size: 3
    total-first-load-budget-ms: 12000
    musicbrainz:
      enabled: false
      base-url: https://musicbrainz.org/ws/2
      connect-timeout-ms: 2000
      read-timeout-ms: 5000
      minimum-request-interval-ms: 1100
      completion-safety-margin-ms: 250
      user-agent: ${MUSICBRAINZ_USER_AGENT:}
    discogs:
      enabled: false
      base-url: https://api.discogs.com
      connect-timeout-ms: 500
      read-timeout-ms: 2500
      user-agent: ${DISCOGS_USER_AGENT:}
      token: ${DISCOGS_TOKEN:}
    lastfm:
      enabled: false
      base-url: https://ws.audioscrobbler.com/2.0
      connect-timeout-ms: 500
      read-timeout-ms: 2500
      api-key: ${LASTFM_API_KEY:}
      maximum-tags-per-subject: 5
    matching:
      duration-tolerance-ms: 3000
    evidence-confidence:
      musicbrainz-recording-genre: 0.90
      musicbrainz-release-group-genre: 0.75
      discogs-genre: 0.70
      discogs-style: 0.85
      lastfm-track-community-tag: 0.65
      lastfm-album-community-tag: 0.60
```

`3000ms` budget은 최초 제안이었으나, 2026-09-07 사용자 로컬 검증에서 MusicBrainz HTTP timeout이 확인되어 `12000ms`로 조정했다. MusicBrainz는 ISRC 검색, fallback 검색, Recording 및 Release Group 조회를 순차 수행할 수 있고 요청 간격도 준수해야 하므로 connect/read timeout을 각각 `2000ms`/`5000ms`, 최소 요청 간격을 `1100ms`로 설정한다. 2026-09-11 timeout 재현에 따라 전체 budget을 다시 늘리는 대신 필수 단계에서 처음 발생한 TIMEOUT을 1회 재시도하며, optional Recording releases/Release Group lookup은 `250ms` 완료 여유를 포함한 잔여 budget gate를 통과할 때만 실행한다. 같은 날 운영 API 검증에서 ISRC `inc=release-groups`와 `inc=genres`가 각각 HTTP 400으로 거부되고 `inc=artist-credits`는 HTTP 200임을 확인했다. 또한 Queen Recording에서 `releases` 포함 lookup은 20초 timeout, `artist-credits+release-groups+genres` lookup은 HTTP 200을 확인했다. 따라서 ISRC detail 재사용 최적화는 철회하고, 필수 Recording genre 조회와 optional releases 조회를 분리한다. 2026-09-11 Human Review에서 MusicBrainz 일시적 throttling/과부하에 대응하기 위해 필수 단계의 첫 HTTP `503`도 TIMEOUT과 같은 슬롯으로 1회 재시도하도록 승인했다. 이 값들은 코드 상수로 이동하지 않는다.

## 기존 API 영향

public API contract 변경은 없다.

- `POST /api/tracks/import`
  - request, response field, 인증, HTTP status 유지
  - 최초 unresolved Track은 외부 enrichment budget만큼 응답 시간이 늘 수 있음
  - provider 일부/전체 실패는 기존 Catalog 성공을 실패로 바꾸지 않으며 `systemTags`는 성공 evidence로 계산된 부분 결과 또는 빈 목록
- `GET /api/tracks`, `GET /api/tracks/{trackId}`, `GET /api/tracks/ranking` 변경 없음
- Board/UserTag API 변경 없음
- Observation, Assertion, provider status, matching score/사유는 외부 응답에 노출하지 않음
- Swagger는 endpoint/DTO를 바꾸지 않고 import의 partial-result 의미만 실제 동작과 일치하게 갱신

## Tag 계산 시스템 연결

- provider mapper의 최종 출력은 기존 `ExternalTagInput`뿐이다.
- MusicBrainz/Discogs/Last.fm response DTO를 Observation/Assertion/Resolver로 전달하지 않는다.
- `rawName`은 provider 원문, `externalRef`는 채택된 외부 entity identity, `source`와 `evidenceType`은 위 Policy Gate를 따른다.
- approved alias exact match는 기존 `ObservationProcessingService`가 수행한다.
- unmatched raw 값은 NEW Observation으로 그대로 보존한다.
- Discogs Album evidence는 기존 Album resolve → inherited assertion → Track resolve 흐름을 사용한다.
- Last.fm Track community tag는 direct Track assertion으로, Album community tag는 기존 Album resolve → inherited assertion → Track resolve 흐름으로 처리한다.
- Resolver max(confidence), minimum score, direct-over-inherited, HIDDEN/MANUAL_FIXED 정책은 변경하지 않는다.

Last.fm source/evidence 의미는 ADR-004로 확정한다. 이후 새 provider를 추가할 때 필요한 것은 provider-specific client/matching/mapper와 `ExternalTagProvider` bean이며, 현재 enum/source/schema 밖의 identity를 영속화하거나 Resolver 의미를 변경해야 하면 별도 milestone/ADR을 검토한다.

## 테스트 계획

### Matching Unit Test

MusicBrainz:

- 단일 exact ISRC 후보는 artist가 일치하면 title/duration 차이와 관계없이 채택
- ISRC lookup은 `artist-credits`만 사용하고 후보 채택 후 Recording lookup으로 genre/Release Group 후보를 수집
- 복수 ISRC 후보에서 title/artist/duration 유일 후보 채택
- ISRC 없음/실패 시 title + artist fallback
- duration 경계 `3,000ms` 포함 및 초과 거부
- title 또는 artist 불일치 거부
- 동률/ambiguous 후보 거부
- Recording match 성공, Release Group ambiguous이면 Track만 성공
- 저장된 MBID가 있으면 search를 건너뛰고 lookup에 사용
- 필수 단계에서 처음 발생한 TIMEOUT 또는 HTTP `503`은 deadline 안에서 공유 슬롯으로 한 번만 재시도하고 다른 상태 및 optional 요청은 재시도하지 않음
- optional Recording releases 또는 Release Group 최악 비용보다 잔여 budget이 적으면 조회를 생략하고 Recording tag/MBID를 보존

Discogs:

- Discogs 전용 canonical title/artist 정제 허용·금지 패턴 검증
- Queen `A Night At The Opera (2011 Remaster)`가 `A Night At The Opera` Master와 매칭되는 회귀 검증
- 원문 title+artist exact 후보를 canonical 후보보다 우선
- title + artist의 유일 Master를 우선 채택하고 year는 복수 후보 축소에만 사용
- exact Master 후보가 없을 때만 Release를 검색하고 유일한 exact Release를 fallback으로 채택
- 전체 album artist credit 중 한 명의 exact overlap 허용
- title/artist 불일치 거부, year 불일치 단독으로 유일 후보를 거부하지 않음
- Discogs Artist 숫자 suffix와 Unicode quote/dash 정규화
- 복수 Master 또는 복수 Release 후보 ambiguity 거부, Master ambiguity 시 Release fallback 금지
- Master 채택 시 Release 미호출, Master 없음 시 Release 호출 순서 검증
- MusicBrainz 결과 없이 독립적으로 matching

Last.fm:

- 저장된 Recording/Release Group MBID 유무와 무관하게 Track/Album을 representative artist/title로 조회
- Last.fm Track 요청에 `mbid`를 전달하지 않고 `artist`/`track`을 전달
- exact Track tag가 비고 approved edition qualifier가 있을 때만 canonical title로 1회 fallback
- Live/Remix/Acoustic 등 의미 변경 qualifier는 fallback title에서 제거하지 않음
- representative artist/title normalized exact 응답만 채택
- `autocorrect=0` 유지 및 corrected/mismatched identity 거부
- Track/Album branch가 MusicBrainz/Discogs 결과 없이 독립적으로 실행
- deterministic SHA-256 `externalRef`의 반복 입력 안정성 및 subject type 구분

Normalizer:

- 대소문자, NFKC, trim, 연속 공백
- punctuation/remix/edition 문자열을 임의로 소거하지 않음
- System Tag normalizer와 별도 정책임을 테스트

### Client / Mapper Test

- MusicBrainz ISRC lookup의 허용 `inc` 고정, recording search/lookup, Release Group genre JSON fixture mapping
- MusicBrainz `genres` 중 null/blank/`count <= 0` 제외
- Discogs search와 Master/Release detail genre/style fixture mapping
- Last.fm Track/Album top tag JSON fixture mapping
- Catalog에 Recording MBID가 있어도 Last.fm Track 요청은 `mbid`를 제외하고 `artist`/`track`을 전달
- exact edition title tag가 비어 있으면 canonical title로 한 번만 재조회하고 exact tag가 있으면 재조회하지 않음
- Last.fm count와 무관한 응답 순서, 빈 이름/중복 제외, subject별 상위 5개 제한 및 5개 미만 수집
- Last.fm raw tag를 `COMMUNITY_TAG`로 매핑하고 genre/style로 추측하지 않음
- stable `externalRef`, source, evidence type, 설정 confidence 검증
- required User-Agent, JSON accept header, Discogs token과 Last.fm API key 전달
- 404 → NOT_FOUND, client timeout → TIMEOUT, 429/5xx/malformed JSON → FAILED 분류 및 HTTP `503`만 retryable 표시
- MusicBrainz request gate는 단일 production constructor를 유지하고, 느린 실제 sleep assertion 대신 대기 interrupt/timeout 변환으로 회귀 검증

### Parallel Collector Unit Test

- 세 provider가 모두 완료되기 전에 각각 시작됐음을 latch/barrier로 검증하여 순차 실행 회귀 방지
- 완료 순서와 무관한 deterministic merge
- 한 provider throw/timeout + 다른 provider success 시 성공 inputs 보존
- 세 provider 실패 시 빈 결과 반환
- external client 계열 예외만 격리하고 내부 programming error를 숨기지 않음
- provider 실행 중 활성 transaction 없음
- 설정된 전체 budget 이후 무기한 대기하지 않음
- collector의 동일 절대 deadline이 provider에 전달되고 optional subject 생략 후 필수 subject 결과가 정상 merge됨

시간 차이를 재는 flaky stopwatch assertion 대신 동시 진입 latch와 제어 가능한 future를 사용한다.

### Catalog JPA Test

- null Track/Album MBID에 accepted ID 저장
- 같은 ID 반복 적용은 idempotent
- 다른 non-null ID는 overwrite하지 않음
- Track accepted + Album unmatched인 부분 identity 저장
- write transaction 밖에서 외부 client가 호출됨
- 추가 schema 없이 기존 index/column mapping 유지

### Application / Vertical Integration Test

실제 인터넷 대신 stub HTTP server 또는 mock HTTP exchange와 실제 JPA Tag pipeline을 사용한다.

- 최초 Track 선택 시 세 provider branch 호출
- MusicBrainz Track genre → MATCHED Observation → APPROVED Assertion → Track resolved
- Discogs Album style → Album Assertion → inherited Track Assertion → resolved
- Last.fm Track community tag → Track Assertion → resolved
- Last.fm Album community tag → Album Assertion → inherited Track Assertion → resolved
- 미매칭 raw genre/style → NEW Observation 보존
- provider 하나 timeout이어도 다른 provider 결과와 정상 API 응답 유지
- 동일 선택 반복/동시 선택에서 MBID, Observation, Assertion, Resolved가 기존 unique 정책으로 수렴
- 이미 resolved Track은 세 provider를 호출하지 않음
- public import response는 `systemTags` 외 기존 필드와 정렬을 유지

실제 MusicBrainz/Discogs/Last.fm을 호출하는 live integration test는 기본 test suite에 포함하지 않는다.

### Regression Test

- 기존 `TrackSelectionServiceTest`, `FirstVerticalSliceIntegrationTest`
- `TrackImportServiceTest`, Catalog JPA/concurrency tests
- `TrackControllerTest`와 Swagger contract
- Observation/Assertion/Resolution/Inheritance 대상 tests
- provider bean disabled 상태의 Spring context load

## 위험 요소

1. **False positive entity match**
   - 잘못된 외부 entity는 raw evidence 전체를 오염시킨다.
   - Discogs/Last.fm canonical qualifier는 승인된 trailing 패턴에만 적용하고 Live/Remix 등 의미 변경 표기는 보존한다.
   - 원문 exact 및 unique gate를 우선하고 ambiguity는 빈 결과로 처리한다. recall보다 precision을 우선한다.

2. **MusicBrainz rate limit과 최초 응답 시간 충돌**
   - ISRC fallback 및 Release Group lookup은 branch 내부 복수 호출이 될 수 있다.
   - app-wide 1초 간격, 최소 request 수, HTTP/total budget을 적용하고 budget 초과는 partial result로 종료한다.

3. **Discogs physical release ambiguity**
   - 동일 album의 국가/연도/format별 pressing이 다수 존재한다.
   - Album 단위 evidence에는 유일 Master를 우선하고, exact Master가 없을 때만 Release를 조회한다.
   - 복수 Master 또는 복수 Release를 검색 순서로 임의 선택하지 않는다. 이번 milestone은 복잡한 pressing score를 만들지 않는다.
   - Spotify와 Discogs의 원발매/재발매 연도 의미가 다를 수 있으므로 year는 검색 선필터가 아니라 복수 후보 보조 판별값으로만 사용한다.

4. **Provider confidence의 제품 정책성**
   - 수치에 따라 Resolver 노출 결과가 달라진다.
   - Policy Gate 승인과 configuration test 없이 구현하지 않는다.

5. **Last.fm community tag noise와 인기 편향**
   - top tag에는 genre/style 외 mood, 개인 분류, 청취 상태가 섞이고 인기 entity일수록 count가 풍부하다.
   - `COMMUNITY_TAG` provenance를 유지하고 count와 무관한 subject별 상위 5개 제한과 approved alias exact unique match를 적용한다. count 가중치나 provider agreement bonus는 만들지 않는다.

6. **Partial success 후 자동 재시도 공백**
   - resolved projection이 생기면 현재 빠른 경로가 실패 provider를 다시 호출하지 않는다.
   - 이번 milestone은 사용자 응답의 partial success까지만 보장한다. provider별 상태 영속화와 TIMEOUT/FAILED scheduler 재시도는 후속 milestone이며, 이 제한을 완료 보고에도 남긴다.

7. **정상 EMPTY와 미실행 구분**
   - `subject_enrichment_status`가 없으므로 DB만 보고 정상 무태그/NOT_FOUND/실패를 구분할 수 없다.
   - 이번에는 구조화 로그와 in-memory result로만 구분한다. EMPTY 반복 호출 억제/재시도 정책은 상태 milestone에서 해결한다.

8. **동시 최초 enrichment 중복 외부 호출**
   - 동일 Track 동시 선택은 provider를 중복 호출할 수 있다.
   - DB unique로 저장 결과는 수렴시키되 외부 호출 방지만을 위해 분산 락을 도입하지 않는다. 실제 rate 문제는 후속 상태/claim 설계 근거로 측정한다.

9. **설정/secret 누락**
   - Discogs token, provider User-Agent, Last.fm API key가 없으면 해당 client가 동작할 수 없다.
   - 조건부 bean과 enabled provider startup validation을 분리하고 secret을 repository에 저장하지 않는다.

10. **외부 response 변경/저품질 데이터**
   - client DTO는 필요한 필드만 수용하고 unknown field를 허용한다.
   - null/blank tag name을 provider 매핑 경계에서 제외하고 fixture/provider tests로 회귀를 막는다. count는 태그 선택에 사용하지 않는다.

11. **작업 트리의 기존 변경**
    - 현재 repository에는 이번 범위 밖의 사용자 변경과 대량 untracked frontend dependency가 있다.
    - 구현 시 예상 파일만 수정하고 관련 없는 formatting/cleanup 또는 사용자 변경 복구를 하지 않는다.

## Acceptance Criteria

- [ ] Policy Gate의 matching/Last.fm 상위 5개 기준과 여섯 가지 base confidence가 Human Review로 승인되거나 Plan에 승인된 값으로 갱신되어 있다.
- [ ] production MusicBrainz/Discogs/Last.fm provider가 실제 HTTP response를 provider-neutral `ExternalTagInput`으로 변환한다.
- [ ] MusicBrainz Recording과 Release Group, Discogs Album, Last.fm Track/Album은 각 exact/unique 기준을 통과한 경우에만 evidence를 생성한다.
- [ ] MusicBrainz 필수 단계의 첫 TIMEOUT 또는 HTTP `503`은 request gate와 deadline 안에서 공유 슬롯으로 1회만 재시도하고, 두 번째 실패는 provider-local `TIMEOUT`/`FAILED`로 종료한다.
- [ ] accepted MusicBrainz Recording/Release Group MBID가 기존 Track/Album 컬럼에 idempotent하게 저장되고 상충 ID를 덮어쓰지 않는다.
- [ ] MusicBrainz, Discogs, Last.fm top-level 호출은 크기 3의 전용 executor에서 병렬 시작되며 MusicBrainz branch 내부 rate limit은 준수한다.
- [ ] 외부 HTTP를 기다리는 동안 활성 DB transaction이 없다.
- [ ] provider 하나의 NOT_FOUND/TIMEOUT/FAILED가 다른 provider의 Observation/Assertion/Resolved 결과를 폐기하거나 import 전체를 실패시키지 않는다.
- [ ] MusicBrainz genre, Discogs genre/style, Last.fm community tag의 raw 문자열 및 stable external reference가 Observation에 보존된다.
- [ ] Last.fm count는 수집 gate, `COMMUNITY_TAG` confidence, Resolver score에 사용되지 않고 subject별 응답 상위 5개만 수집된다.
- [ ] unmatched raw 값은 NEW이고 matched 값만 기존 assertion/resolver로 연결된다.
- [ ] Discogs 및 Last.fm Album evidence가 기존 Album → Track inheritance 경로로 연결되고 Last.fm Track evidence는 direct Track 경로로 연결된다.
- [ ] future 완료 순서와 반복/동시 요청에 관계없이 저장 결과가 기존 unique 제약으로 수렴한다.
- [ ] 이미 resolved projection이 있는 Track은 Spotify/MusicBrainz/Discogs/Last.fm을 재호출하지 않고 기존 상세를 반환한다.
- [ ] public API endpoint/request/response/error contract는 변경되지 않고 Swagger 설명이 실제 partial-result 동작과 일치한다.
- [ ] 신규 DB table/column/relation, retry scheduler, generic mapping, Artist matching이 추가되지 않는다.
- [ ] 대상 테스트와 전체 검증을 사용자가 실행하여 통과 결과를 전달한다.
- [ ] diff review에서 transaction boundary, partial success, N+1, idempotency, 계층 의존 및 범위 밖 변경이 없음을 확인한다.

## Verification

Codex는 Gradle 및 `scripts/verify.sh`를 실행하지 않는다. 구현 후 사용자가 다음 명령을 실행하고 결과를 전달한다.

우선 대상 테스트:

```bash
./gradlew :tagnote-core:test --tests '*ExternalEnrichmentCollectorTest' --tests '*ExternalHttpFailureTranslatorTest' --tests '*MusicBrainz*Test' --tests '*Discogs*Test' --tests '*LastFm*Test' --tests '*CatalogJpaRepositoryTest' --tests '*CatalogImportConcurrencyTest' --tests '*TrackSelectionServiceTest' --tests '*FirstVerticalSliceIntegrationTest'
./gradlew :tagnote-api:test --tests '*TrackControllerTest'
```

전체 검증:

```bash
./gradlew test
./gradlew check
./scripts/verify.sh
```

Codex가 직접 수행할 정적 검토:

```bash
git diff --check -- agents/server/plans/active/ENRICHMENT-001.md <승인된-구현-파일들>
git diff --stat -- <승인된-구현-파일들>
git diff -- <승인된-구현-파일들>
```

검증 시 실제 외부 API 호출이나 실제 token을 요구하지 않는다. HTTP fixture/stub으로 mapping, timeout, partial failure와 병렬성을 재현한다. 사용자 테스트 결과, Acceptance Criteria 및 diff review가 모두 확인되기 전에는 `progress.md` 갱신이나 completed 이동을 하지 않는다.

## Completion Record — 2026-09-18

- MusicBrainz, Discogs, Last.fm provider와 보수적 entity matching, provider-local 실패 처리,
  병렬 collector 및 외부 identity write 경계를 구현했다.
- `LASTFM`/`COMMUNITY_TAG`를 포함한 raw evidence, partial-success, Album 상속 및
  외부 HTTP transaction 분리 정책을 기존 pipeline에 연결했다.
- 사용자가 대상 테스트와 전체 `test`, `check`, `scripts/verify.sh` 통과를 확인했다.
- Acceptance Criteria 및 범위 diff를 정적 검토했으며, 관련 Plan을 completed로 이동한다.
