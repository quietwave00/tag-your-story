# SEARCH-REF-001 — Search Strangler Refactoring

- Status: Completed
- Scope: 기존 `GET /api/tracks` 검색 유스케이스의 계층 경계 생성
- Non-goal: 이번 계획 수립 단계에서는 production code를 수정하지 않는다.

## Goal

기존 Search HTTP API와 관찰 가능한 동작을 그대로 유지하면서, `TrackService`에 섞인 검색 조율 책임을 Application 계층으로 옮기고 Spotify SDK 및 HTTP 연동을 Infrastructure 경계 뒤로 숨긴다.

가장 작은 reviewable implementation milestone은 **검색 endpoint 하나만** strangler 경로로 전환하는 것이다. 상세 조회와 검색어 랭킹은 기존 경로에 남겨 이후 마일스톤에서 별도로 이동한다.

## 조사 기준과 제약

- 기준 문서:
  - `AGENTS.md`
  - `agents/server/current_state.md`의 Search / Spotify / Track baseline
  - `agents/server/server_spec.md`의 Layered Architecture, Music Catalog, Track 검색/선택/상세, 외부 provider, package 구조
  - `agents/server/progress.md`
  - `agents/server/conventions.md`
- `agents/server/conventions.md`는 조사 시점에 0바이트이므로 추가 convention은 확인되지 않았다.
- `agents/server/decisions/`는 작업 트리와 `HEAD`에 존재하지 않아 Search 리팩토링에 적용할 ADR은 없다.
- `agents/decisions/ADR-000-subject.md`는 빈 placeholder이며 이번 Search 계획의 결정 근거로 사용하지 않는다. ADR 생성/삭제는 이 milestone 범위가 아니다.

## AS-IS

### 호출 흐름

```text
GET /api/tracks?keyword={keyword}&page={page}
  -> TrackController.search
  -> TrackService.search                         (@Transactional(readOnly = true))
     -> SearchKeywordTracker.save
        -> StringRedisTemplate ZSET increment
     -> SpotifyWebClient.getTrackInfoByKeyword
        -> access token Redis 조회/생성
        -> Spotify Search API (limit 10, offset page * 10)
     -> TrackService.getTrackData
        -> Spotify SDK Track를 TrackData로 변환
  -> SearchTracksResponse.from
  -> ApiResult success envelope
```

### Layered Architecture 위반

- `TrackService`가 `domain.tracks.service`에 있으면서 Application use case 조율, Redis 기록, Infrastructure client 호출, provider DTO 변환을 동시에 담당한다.
- Application 계층이 별도로 없으며 Application 코드가 concrete `SpotifyWebClient`와 `SearchKeywordTracker`에 직접 의존한다.
- `SpotifyWebClient`, `TrackInfo`, `SearchKeywordTracker`가 `domain.tracks` 아래에 있어 Infrastructure 책임이 Domain처럼 배치되어 있다.
- `TrackService`가 Spotify SDK의 `Track`, `ArtistSimplified`, `AlbumSimplified`, `Image` 타입을 직접 알아 provider 종속 타입이 use case 내부로 누출된다.
- 검색에는 DB 접근이 없는데 class-level `@Transactional(readOnly = true)` 때문에 Spotify 외부 요청 중 불필요한 DB 트랜잭션이 열릴 가능성이 있다.

### Search Application Use Case와 Spotify Infrastructure 책임 혼재

- Application 책임: 검색어 기록을 먼저 실행하고 provider 검색을 호출하는 순서, 검색 결과와 total count 반환.
- Infrastructure 책임: Spotify 인증, pagination 변환(`limit=10`, `offset=page*10`), SDK 요청 실행, provider 예외 변환, Spotify 응답 해석.
- 현재 두 책임이 `TrackService`와 `SpotifyWebClient` 사이에 불완전하게 나뉘어 있다. 특히 Spotify SDK 응답을 서비스 공용 `TrackData`로 바꾸는 책임이 `TrackService`에 있다.

### Controller 책임과 결합

- Controller에 repository 접근이나 검색 비즈니스 로직은 없다.
- 다만 하나의 legacy `TrackService`가 검색/상세/랭킹을 모두 제공하며, Controller가 core service DTO인 `TrackData`, `SearchTrackList`, `RankingList`에 직접 결합한다.
- `SearchTracksResponse.trackDataList`가 `List<TrackData>`라 Presentation response가 Application/legacy DTO를 그대로 노출한다.
- 이번 milestone에서는 검색 endpoint의 입력 전달과 HTTP response mapping만 Controller에 남긴다. 상세/랭킹의 DTO 결합은 건드리지 않는다.

### 기존 Track/Search 구조와 향후 Catalog 충돌

- 현재 응답의 `trackId`는 내부 Track PK가 아니라 Spotify track id다.
- 현재 상세 `GET /api/tracks/{trackId}` 역시 Spotify id를 받아 Spotify를 직접 조회한다.
- 명세의 미래 Catalog는 내부 `Track.track_id`와 별도의 unique `spotify_id`를 사용하고, 상세 endpoint는 내부 Catalog와 resolved tag를 조회한다.
- 현재 Search DTO를 미래 Catalog `Track` 도메인 모델로 재사용하면 외부 id와 내부 id의 의미가 충돌한다.
- 따라서 새 검색 모델 내부에서는 `spotifyTrackId`를 명시적으로 사용하고, 기존 HTTP field `trackId`로의 호환 mapping은 Presentation 계층에서만 수행한다.

### JPA / Repository 접근

- 현재 검색 경로는 JPA Entity나 Repository를 사용하지 않으며 N+1/SELECT도 발생하지 않는다.
- 검색 결과 표시 단계에서 DB 저장은 필수가 아니라는 명세와 일치한다.
- 이번 milestone에 Catalog Repository, Track Entity, upsert, FK, schema 변경을 추가하지 않는다.
- 새 Search Application Service에는 `@Transactional`을 붙이지 않아 Redis/Spotify 외부 I/O 동안 DB 트랜잭션을 열지 않는다.

### Characterization Test가 보호하는 behavior

현재 테스트가 보호하는 검색 동작은 다음과 같다.

- `TrackControllerTest`
  - `GET /api/tracks`
  - query parameter 이름 `keyword`, `page`
  - `ApiResult` success envelope
  - response의 `trackDataList`, `totalCount`
  - 각 item의 `trackId`, `artistName`, `title`, `albumName`, `imageUrl`
- `TrackServiceTest`
  - `SearchKeywordTracker.save(keyword)`가 Spotify 검색보다 먼저 호출됨
  - Spotify 배열 순서와 `totalCount` 유지
  - 첫 번째 artist 이름 사용
  - album 첫 번째 image URL 사용
  - image가 없으면 문자열 `NO_IMAGE` 사용
- `SearchKeywordTrackerTest`
  - Redis key `search_keyword:`
  - `incrementScore(..., 1)`
  - ranking 조회 범위 `0..4` 및 점수 제거

추가로 실제 코드가 암묵적으로 고정하는 동작은 Spotify 검색 page를 `offset = page * 10`으로 바꾸고 `limit = 10`을 사용하는 것이다.

### 기존 public API 변경 위험

- 명세 초안은 `GET /api/tracks/search?q=...`와 `spotifyTrackId`를 제안하지만 현재 public API는 `GET /api/tracks?keyword=...&page=...`와 item field `trackId`다.
- 이번 milestone에서 명세 초안 경로로 즉시 교체하면 기존 client가 깨진다.
- `trackId`를 내부 Catalog id로 재해석하거나 응답 field를 `spotifyTrackId`로 변경해도 호환성이 깨진다.
- `totalCount`, `ApiResult` envelope, JSON field 순서와 무관한 field 이름/shape를 유지해야 한다.
- provider 예외는 기존 `CustomException(ExceptionCode.SPOTIFY_EXCEPTION)` 전파를 유지한다.

## Target State

### Milestone 종료 시 구조

```text
Presentation
  TrackController
  SearchTracksResponse / SearchTrackItemResponse
        |
Application
  TrackSearchService
  TrackSearchProvider port
  SearchKeywordRecorder port
  TrackSearchResult / TrackSearchItem
        |
Infrastructure
  SpotifyTrackSearchAdapter -> legacy SpotifyWebClient
  legacy SearchKeywordTracker implements SearchKeywordRecorder
```

- `TrackController.search`는 새 `TrackSearchService`만 호출하고 application 결과를 Presentation DTO로 변환한다.
- `TrackSearchService`는 검색어 기록 후 provider 검색을 호출하는 순서만 조율한다.
- `TrackSearchProvider`는 provider-neutral 검색 output을 반환하며 Spotify SDK 타입을 노출하지 않는다.
- `SpotifyTrackSearchAdapter`는 legacy `SpotifyWebClient`를 감싸 Spotify SDK/`TrackInfo`를 application 검색 모델로 변환한다.
- `SearchKeywordTracker`는 legacy 위치와 Redis 동작을 유지하되 `SearchKeywordRecorder` port 구현체가 된다.
- legacy `TrackService`는 상세조회와 랭킹만 유지한다. 검색 메서드와 검색용 mapping 책임은 제거한다.
- 새 검색 경로에는 JPA transaction, Entity, Repository가 없다.

이 구조는 완성된 전체 Catalog 아키텍처가 아니라, 기존 검색을 유지한 채 후속 Catalog/Track Import가 독립적으로 들어올 수 있게 만드는 첫 seam이다.

## 변경할 파일

### Production

| 파일 | 변경 책임 |
|---|---|
| `tagnote-api/src/main/java/com/tagnote/api/domain/tracks/TrackController.java` | 검색만 `TrackSearchService`로 라우팅한다. 상세/랭킹은 legacy `TrackService`를 유지한다. |
| `tagnote-api/src/main/java/com/tagnote/api/domain/tracks/dto/response/SearchTracksResponse.java` | application 검색 결과를 기존 JSON shape로 변환한다. core `TrackData` 직접 노출을 제거한다. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/tracks/service/TrackService.java` | `search`와 검색 전용 Spotify SDK mapping을 제거하고 상세/랭킹 legacy 책임만 남긴다. 상세의 mapping은 기존 동작 그대로 유지한다. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/tracks/util/SearchKeywordTracker.java` | Redis key/score/ranking 동작 변경 없이 `SearchKeywordRecorder` port를 구현한다. |

`SpotifyWebClient.java`는 이번 milestone에서 변경하지 않는다. 새 adapter가 현재 메서드를 사용해 외부 provider 세부사항을 격리한다.

### Test

| 파일 | 변경 책임 |
|---|---|
| `tagnote-api/src/test/java/com/tagnote/api/domain/tracks/TrackControllerTest.java` | 검색 mock을 `TrackSearchService`로 전환하면서 기존 route/query/response characterization을 그대로 유지한다. 상세/랭킹은 legacy mock으로 유지한다. |
| `tagnote-core/src/test/java/com/tagnote/domain/tracks/service/TrackServiceTest.java` | 새 use case/adapter로 이동한 검색 assertion을 제거하고 상세/랭킹 characterization만 유지한다. |
| `tagnote-core/src/test/java/com/tagnote/domain/tracks/util/SearchKeywordTrackerTest.java` | 기존 Redis 동작을 계속 보호하고 port 구현 계약이 컴파일됨을 확인한다. |

## 새로 만들 파일

### Production

| 파일 | 책임 |
|---|---|
| `tagnote-core/src/main/java/com/tagnote/application/catalog/search/TrackSearchService.java` | 검색 use case. keyword 기록을 먼저 실행한 뒤 provider 검색 결과를 반환한다. DB transaction을 열지 않는다. |
| `tagnote-core/src/main/java/com/tagnote/application/catalog/search/port/TrackSearchProvider.java` | Spotify 등 외부 검색 provider에 대한 output port. provider SDK 타입을 허용하지 않는다. |
| `tagnote-core/src/main/java/com/tagnote/application/catalog/search/port/SearchKeywordRecorder.java` | 검색어 기록에 대한 output port. Redis 타입/자료구조를 노출하지 않는다. |
| `tagnote-core/src/main/java/com/tagnote/application/catalog/search/model/TrackSearchItem.java` | 검색 결과 item. 외부 식별자임을 드러내는 `spotifyTrackId`, artist/title/album/image 값을 가진다. JPA Entity가 아니다. |
| `tagnote-core/src/main/java/com/tagnote/application/catalog/search/model/TrackSearchResult.java` | 순서가 유지된 item 목록과 Spotify total count를 담는 application 결과다. |
| `tagnote-core/src/main/java/com/tagnote/infrastructure/external/spotify/SpotifyTrackSearchAdapter.java` | `TrackSearchProvider` 구현. legacy `SpotifyWebClient` 호출 및 Spotify SDK 응답을 application model로 mapping한다. 첫 artist/첫 image/`NO_IMAGE` 규칙을 보존한다. |
| `tagnote-api/src/main/java/com/tagnote/api/domain/tracks/dto/response/SearchTrackItemResponse.java` | `spotifyTrackId`를 기존 public field `trackId`로 명시적으로 mapping하여 Catalog 내부 id와 의미 충돌을 막는다. |

### Test

| 파일 | 책임 |
|---|---|
| `tagnote-core/src/test/java/com/tagnote/application/catalog/search/TrackSearchServiceTest.java` | keyword 기록 선행, 동일 keyword/page 전달, provider 결과 순서/total count 보존, 실패 시 호출 순서를 검증한다. |
| `tagnote-core/src/test/java/com/tagnote/infrastructure/external/spotify/SpotifyTrackSearchAdapterTest.java` | Spotify SDK/`TrackInfo`에서 provider-neutral model로의 mapping, 첫 artist, 첫 image, `NO_IMAGE`, total count, legacy client argument 전달을 검증한다. |

파일명과 package 선언은 위 경로를 기준으로 맞춘다. 현재 `com.tagnote.core.domain.*` 전체를 한 번에 이동하지 않고 새 seam만 명세의 `application`/`infrastructure` 방향으로 만든다.

## 각 파일의 책임 경계

- Presentation은 HTTP parameter와 기존 JSON contract만 안다.
- Application은 검색 순서와 provider-neutral 모델만 안다.
- Application port는 Spotify, Redis, Spring Data 타입을 import하지 않는다.
- Spotify adapter만 Spotify SDK 타입을 해석한다.
- legacy Spotify client는 인증, Redis token cache, HTTP 요청과 예외 변환을 계속 담당한다.
- keyword tracker는 Redis ZSET 구현을 계속 담당한다.
- 미래 Catalog Entity/Repository는 어떤 검색 DTO에도 의존하지 않는다.

의존 방향은 다음 정적 규칙을 만족해야 한다.

```text
TrackController -> TrackSearchService -> TrackSearchProvider (interface)
                                         ^
                                         |
                              SpotifyTrackSearchAdapter

TrackSearchService -> SearchKeywordRecorder (interface)
                                      ^
                                      |
                         SearchKeywordTracker
```

## 호출 흐름

### 검색 성공

```text
GET /api/tracks?keyword=rock&page=0
  -> TrackController.search
  -> TrackSearchService.search("rock", 0)
     -> SearchKeywordRecorder.record("rock")
        -> SearchKeywordTracker
        -> Redis ZSET increment
     -> TrackSearchProvider.search("rock", 0)
        -> SpotifyTrackSearchAdapter
        -> SpotifyWebClient.getTrackInfoByKeyword("rock", 0)
        -> Spotify Search API (limit 10, offset 0)
        -> Spotify SDK Track -> TrackSearchItem(spotifyTrackId=...)
     <- TrackSearchResult(items, totalCount)
  -> SearchTracksResponse.from
     -> SearchTrackItemResponse(trackId=spotifyTrackId, ...)
  -> 기존 ApiResult success JSON
```

### 실패

- keyword 기록 실패 시 기존과 같이 Spotify 검색을 호출하지 않고 예외를 전파한다.
- keyword 기록 성공 후 Spotify가 실패하면 이미 증가한 ranking을 rollback하지 않는다. 기존 호출 순서와 동일하다.
- Spotify 예외는 legacy client가 기존 `SPOTIFY_EXCEPTION`으로 변환하며 adapter/application/controller가 재해석하지 않는다.

## 기존 API 영향

HTTP public API 영향은 **없어야 한다**.

유지 항목:

- method/path: `GET /api/tracks`
- query: required `keyword: String`, required `page: int`
- 인증: 없음
- success envelope: 기존 `ApiResult`
- response: `trackDataList`, `totalCount`
- item fields: `trackId`, `artistName`, `title`, `albumName`, `imageUrl`
- `trackId` 값의 의미: Spotify track id
- page 동작: 10개 단위, `offset = page * 10`
- mapping: 첫 artist, 첫 album image, image 없음은 `NO_IMAGE`
- provider error mapping: `SPOTIFY_EXCEPTION`

명세의 `GET /api/tracks/search?q=...`는 이번 milestone에 추가하거나 교체하지 않는다. 필요하면 기존 endpoint 유지 하의 별도 호환성/버전 전략으로 다룬다.

## 테스트 계획

1. 기존 characterization suite를 변경 전 실행해 baseline을 확인한다.
2. `TrackSearchServiceTest`에서 `InOrder`로 recorder가 provider보다 먼저 호출됨을 검증한다.
3. recorder가 예외를 던질 때 provider가 호출되지 않는지 검증한다.
4. provider가 예외를 던질 때 recorder 호출은 이미 완료되었고 같은 예외가 전파되는지 검증한다.
5. `SpotifyTrackSearchAdapterTest`에서 다음을 검증한다.
   - keyword/page가 legacy client에 그대로 전달됨
   - Spotify item 순서와 total count 유지
   - 첫 artist 이름 사용
   - 첫 album image URL 사용
   - image 없음은 `NO_IMAGE`
6. `TrackControllerTest`로 기존 route, required query 이름, success envelope, 모든 JSON field를 회귀 검증한다.
7. 기존 `TrackServiceTest`의 상세/랭킹 테스트와 `SearchKeywordTrackerTest`를 유지한다.
8. 가능하면 전체 suite 외에 변경 범위의 module test를 먼저 실행해 빠르게 피드백을 얻는다.

실제 Spotify/Redis 네트워크에 의존하는 통합 테스트는 이 milestone에 추가하지 않는다. adapter와 tracker 경계는 mock 기반으로 characterization한다.

## 위험 요소

- **Bean wiring ambiguity**: adapter와 port 구현체가 정확히 하나씩 등록되는지 확인해야 한다.
- **JSON shape drift**: `TrackData`에서 새 Presentation item DTO로 바꾸면서 field 이름이나 null 직렬화가 달라질 수 있다. Controller characterization test가 최종 방어선이다.
- **ID 의미 혼동**: 내부 모델에서 다시 `trackId`라고 부르면 미래 Catalog PK와 충돌한다. application model은 반드시 `spotifyTrackId`를 사용한다.
- **호출 순서 회귀**: 검색 성공 후 keyword를 기록하도록 순서가 바뀌면 실패한 검색의 ranking 반영 behavior가 달라진다.
- **트랜잭션 재도입**: 새 service에 class-level `@Transactional`을 복사하면 외부 호출 중 DB transaction을 유지하는 문제가 남는다.
- **SDK 누출**: application model/port가 `se.michaelthelin.spotify.*` 또는 legacy `TrackInfo`를 import하면 seam이 무효화된다.
- **legacy 상세 mapping 손상**: `TrackService`에서 공용 mapping을 정리하다 상세조회 behavior를 바꿀 수 있으므로 상세 characterization을 반드시 유지한다.
- **문서/실행 환경**: 현재 환경에는 `java`와 `JAVA_HOME`이 없어 조사 시점의 `./gradlew test`가 실행되지 않았다. 구현 완료 선언 전에 Java 17 환경에서 검증해야 한다.

## 신규 기능을 위한 최소 Enabling Refactor

이번 milestone에서 허용되는 enabling refactor는 다음뿐이다.

- 검색 전용 Application Service 생성
- Spotify 검색 output port와 adapter 생성
- keyword 기록 port 생성
- 외부 Spotify id와 미래 내부 Track id를 분리하는 application model 생성
- 검색 응답을 Presentation DTO로 명시적으로 mapping
- legacy `TrackService.search` 제거 및 검색 endpoint 전환
- 검색 경로에서 불필요한 JPA transaction 제거

이 seam은 후속 Track Import가 `spotifyTrackId`로 Catalog를 조회/upsert하더라도 기존 검색 결과 모델이나 HTTP contract를 내부 Track Entity로 오해하지 않게 한다.

## 이번 Milestone과 관계없는 cleanup

다음 항목은 발견되었지만 이번 milestone에서 수정하지 않는다.

- 사용되지 않는 `DetailTrackResponse`, `DetailTrack` 정리
- Spotify access token cache의 빈 key `""` 수정
- `SpotifyWebClient` 이름 변경, 전체 package 이동 또는 HTTP client 재작성
- `TrackService` 상세조회와 ranking의 후속 계층 이동
- ranking endpoint/response 개선 및 Redis를 source of truth로 볼지 재설계
- validation 추가(빈 keyword, 음수 page) 또는 pagination 정책 변경
- logging/error message 개선
- 전체 서버 package 일괄 재배치
- 기존 board/user/comment/file/notification 코드 리팩토링

## Do Not Touch

- `GET /api/tracks/{trackId}` 상세조회 path, Spotify id 의미, `TrackData` response shape
- `GET /api/tracks/ranking`과 상위 5개 문자열 목록 behavior
- `SpotifyWebClient`의 token cache, limit/offset, 예외 mapping 구현
- Redis key `search_keyword:`와 score 증가량 `1`
- `CacheSpec.SPOTIFY_ACCESS_TOKEN` 및 `CommonRedisTemplate`
- Artist/Album/Track JPA Entity, Repository, schema, migration
- Board의 문자열 `trackId`, Board-Track FK 및 Board API
- Track Import, MusicBrainz, Discogs, System Tag/Resolver
- 명세 초안 endpoint `/api/tracks/search?q=...` 신규 노출
- 관련 없는 production code와 테스트
- placeholder ADR 및 conventions 문서

## Acceptance Criteria

- `GET /api/tracks?keyword={keyword}&page={page}`의 status, envelope, field 이름, 값의 의미가 변경 전과 동일하다.
- 검색 시 keyword recorder가 Spotify provider보다 정확히 먼저 호출된다.
- Spotify 검색은 기존처럼 10개 단위 pagination을 사용한다.
- 결과 순서, total count, 첫 artist, 첫 image, `NO_IMAGE` fallback이 유지된다.
- Application service와 port에는 Spotify SDK, Redis, Spring Data, JPA 타입 import가 없다.
- Controller와 Application service는 concrete `SpotifyWebClient` 및 `SearchKeywordTracker`에 의존하지 않는다.
- Spotify SDK -> application model mapping은 Infrastructure adapter에만 존재한다.
- 새 검색 경로에 `@Transactional`과 Repository/JPA 접근이 없다.
- `TrackService`는 검색 provider 조율 책임을 더 이상 갖지 않고 상세/랭킹의 기존 behavior는 유지한다.
- Catalog Entity/Repository/schema와 다른 public endpoint는 변경되지 않는다.
- 변경/신규 테스트가 위 behavior와 계층 경계를 보호한다.
- 구현 milestone 종료 시 `agents/server/progress.md`를 업데이트한다.
- diff review에서 명세 불일치, API 회귀, transaction boundary, 불필요한 query, 계층 역참조가 없어야 한다.

## Verification

구현 시 다음 순서로 검증한다.

```bash
./gradlew :tagnote-core:test --tests 'com.tagnote.application.catalog.search.TrackSearchServiceTest' --tests 'com.tagnote.infrastructure.external.spotify.SpotifyTrackSearchAdapterTest'
./gradlew :tagnote-core:test --tests 'com.tagnote.domain.tracks.service.TrackServiceTest' --tests 'com.tagnote.domain.tracks.util.SearchKeywordTrackerTest'
./gradlew :tagnote-api:test --tests 'com.tagnote.api.domain.tracks.TrackControllerTest'
./gradlew test
./gradlew check
git diff --check
git diff -- tagnote-api/src/main tagnote-core/src/main tagnote-api/src/test tagnote-core/src/test agents/server
```

추가 정적 확인:

```bash
rg -n 'se\.michaelthelin\.spotify|StringRedisTemplate|JpaRepository|@Transactional' \
  tagnote-core/src/main/java/com/tagnote/application/catalog/search
rg -n 'SpotifyWebClient|SearchKeywordTracker' \
  tagnote-api/src/main/java/com/tagnote/api/domain/tracks/TrackController.java \
  tagnote-core/src/main/java/com/tagnote/application/catalog/search
```

첫 번째 정적 확인은 결과가 없어야 한다. 두 번째는 concrete infrastructure 의존이 다시 생기지 않았음을 확인하며 결과가 없어야 한다.

Java 17과 유효한 `JAVA_HOME`이 없는 환경에서는 Gradle 검증을 완료한 것으로 간주하지 않는다.

## Approved Addendum — Swagger Contract

2026-08-11 사용자 요청으로 갱신된 `AGENTS.md`의 API 명세 규칙을 현재 Track API에 적용한다.

- Spring Boot 3.2 호환 `springdoc-openapi-starter-webmvc-ui` 의존성을 `tagnote-api`에 추가한다.
- Swagger annotation은 `TrackApi` 명세 interface에 두고 `TrackController`가 구현한다.
- 기존 검색/상세/랭킹 endpoint, parameter, 인증 및 response contract는 변경하지 않는다.
- Search response DTO 필드 의미와 주요 실제 오류 응답을 문서화한다.
- 다른 Controller의 Swagger 전환과 공통 API 문서 구조 재설계는 범위에 포함하지 않는다.

## Completion Record

- Completed: 2026-08-12
- 기존 `GET /api/tracks`의 endpoint, query parameter, 응답 envelope와 field 의미를 유지했다.
- Search orchestration을 `TrackSearchService`로 이동하고 Spotify/Redis 구현을 output port 뒤로 격리했다.
- Spotify SDK mapping을 `SpotifyTrackSearchAdapter`로 이동했으며 검색 경로에 JPA/Repository/transaction을 추가하지 않았다.
- Track 상세 조회와 검색어 랭킹은 승인 범위대로 legacy `TrackService`에 유지했다.
- 신규 Search 테스트, 기존 characterization tests, 전체 Gradle `check`, `scripts/verify.sh`가 통과했다.
- Springdoc 및 `TrackApi` 명세 interface를 추가하고 로컬 Swagger UI/OpenAPI 문서 노출을 확인했다.
- Acceptance Criteria와 scope diff 검토를 완료했으며 사용자가 최종 기능 테스트 완료를 확인했다.
