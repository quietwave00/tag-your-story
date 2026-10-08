# CATALOG-001 — Spotify Track Catalog Import

- Status: Completed (2026-08-20)
- Scope: Spotify에서 선택한 Track의 Artist / Album / Track과 전체 Artist credit 저장 및 재사용
- Non-goal: Board 연결, Catalog 상세조회 전환, MusicBrainz / Discogs enrichment, Tag 구현

## Goal

Spotify 검색 결과에서 선택한 `spotifyTrackId`를 명시적인 Import command로 전달받아 다음을 보장한다.

1. Spotify에서 Track 상세 메타데이터를 조회한다.
2. Artist, Album, Track과 Spotify에 등록된 Track / Album Artist 전체를 내부 Catalog에 저장한다.
3. 동일 Spotify ID를 가진 Catalog row를 중복 생성하지 않는다.
4. 이미 Import된 Track은 Spotify를 다시 호출하지 않고 기존 Catalog 데이터를 재사용한다.
5. 외부 HTTP 호출 중 DB transaction을 열지 않는다.
6. 동시에 같은 Track이 처음 Import되어도 최종 Artist, Album, Track 및 Artist 연결이 중복되지 않는다.
7. 기존 Track 검색, 상세조회, 랭킹 및 Board API contract를 유지한다.

가장 작은 reviewable milestone은 Catalog 저장과 Import API까지다. 검색 GET 요청에는 저장을 추가하지 않고, Board 연결과 외부 enrichment는 후속 milestone로 남긴다.

## Context

현재 검색은 `SEARCH-REF-001`에서 다음 경계로 분리되었다.

```text
GET /api/tracks
  -> TrackSearchService
  -> TrackSearchProvider
  -> SpotifyTrackSearchAdapter
  -> SpotifyWebClient
```

- 검색 결과의 `trackId`는 내부 Track PK가 아니라 Spotify Track ID다.
- 검색은 DB mutation을 수행하지 않으며 이 동작을 유지한다.
- Spotify SDK 타입은 `SpotifyTrackSearchAdapter` 밖의 Search Application 모델로 노출되지 않는다.
- 검색 경로에는 JPA transaction이 없다.

현재 상세조회는 legacy 경로에 남아 있다.

```text
GET /api/tracks/{trackId}
  -> TrackService
  -> SpotifyWebClient.getDetailTrackInfo(trackId)
```

현재 Catalog Entity와 Repository는 존재하지 않는다. `BoardEntity.trackId`도 내부 Track FK가 아니라 Spotify ID 문자열이다.

기존 `server_spec.md`의 Catalog schema는 Album과 Track에 단일 `artist_id`를 뒀지만 이 구조는 Spotify가 반환하는 복수 Artist credit을 보존할 수 없다. 2026-08-19 Human Review에서 `album_artist`, `track_artist` 연결 테이블을 Artist 관계의 유일한 source of truth로 사용하고 배열 순서 `position=0`을 대표 Artist로 해석하는 변경을 승인했다.

승인된 작업 순서는 다음과 같다.

1. `server_spec.md`, System Tag Catalog 모델과 ERD를 연결 테이블 구조로 정렬한다.
2. `agents/server/decisions/ADR-001-catalog-multi-artist-credits.md`에 결정과 영향을 기록한다.
3. 승인된 명세와 ADR을 기준으로 production 구현과 테스트를 완성한다.
4. 병렬 DB 통합 테스트와 전체 검증이 통과한 뒤 progress를 갱신하고 Plan을 completed로 이동한다.

초기 빈 placeholder였던 `agents/decisions/ADR-000-subject.md`는 제거한다. 구현 중 승인된 범위를 바꾸는 추가 설계 결정이 필요하면 먼저 보고하고 Human Review를 거친다.

## Target State

```text
Presentation
  TrackController
  TrackApi
  ImportTrackRequest
  CatalogTrackResponse
        |
Application
  TrackImportService
  CatalogWriteService
  SpotifyTrackMetadataProvider
  SpotifyTrackMetadata
  ImportedTrack
        |
Domain Catalog
  ArtistEntity
  AlbumEntity
  TrackEntity
  AlbumArtistEntity
  TrackArtistEntity
        |
Infrastructure
  SpotifyTrackMetadataAdapter
      -> legacy SpotifyWebClient.getDetailTrackInfo()
  ArtistJpaRepository
  AlbumJpaRepository
  TrackJpaRepository
  AlbumArtistJpaRepository
  TrackArtistJpaRepository
        |
Database
  artist / album / track / album_artist / track_artist
```

- `TrackImportService`는 기존 Track fast path, Spotify metadata 조회, Catalog 저장 및 동시성 복구 순서를 조율한다.
- `TrackImportService`에는 transaction을 두지 않는다.
- `CatalogWriteService`만 Artist / Album / Track과 Artist credit 연결을 짧은 transaction으로 저장한다.
- `SpotifyTrackMetadataProvider`는 provider-neutral metadata를 반환한다.
- `SpotifyTrackMetadataAdapter`만 Spotify SDK 응답을 해석하며 기존 `SpotifyWebClient.getDetailTrackInfo()`를 재사용한다.
- Artist, Album, Track은 각각 독립 Aggregate Root로 취급한다.
- Album / Track과 Artist의 관계는 속성을 가진 명시적 연결 Entity로 모델링하며 JPA `@ManyToMany`는 사용하지 않는다.

## API Contract

### Endpoint

```text
POST /api/tracks/import
Content-Type: application/json
Authentication: 불필요
Authorization: 제한 없음
```

기존 Track 검색과 조회처럼 게스트 및 비로그인 사용자도 호출할 수 있는 공개 endpoint로 제공한다. Controller에 `ROLE_USER` 또는 다른 role을 요구하는 `@PreAuthorize`를 추가하지 않는다.

Request:

```json
{
  "spotifyTrackId": "4u7EnebtmKWzUH433cf5Qv"
}
```

- `spotifyTrackId`는 필수다.
- `null`, 누락 및 blank를 허용하지 않는다.

Response는 기존 `ApiResult` envelope를 사용한다.

```json
{
  "success": true,
  "response": {
    "catalogTrackId": 10,
    "spotifyTrackId": "4u7EnebtmKWzUH433cf5Qv",
    "title": "Track title",
    "isrc": "US...",
    "durationMs": 240000,
    "artists": [
      {
        "artistId": 3,
        "spotifyArtistId": "spotify-artist-a",
        "name": "Artist A",
        "position": 0
      },
      {
        "artistId": 8,
        "spotifyArtistId": "spotify-artist-b",
        "name": "Artist B",
        "position": 1
      }
    ],
    "album": {
      "albumId": 7,
      "spotifyAlbumId": "spotify-album-id",
      "title": "Album",
      "releaseYear": 2024,
      "artists": [
        {
          "artistId": 12,
          "spotifyArtistId": "spotify-album-artist",
          "name": "Album Artist",
          "position": 0
        }
      ]
    }
  }
}
```

내부 PK는 `catalogTrackId`로 노출하여 기존 Search / Detail API의 `trackId=Spotify ID`와 구분한다. Track Artist와 Album Artist 목록은 Spotify 표시 순서를 보존하여 각각 반환한다. `position=0`이 대표 Artist이며 별도의 대표 Artist FK를 중복 저장하지 않는다. 생성과 기존 row 재사용 모두 HTTP 200과 동일한 response contract를 사용한다.

주요 오류:

- request body 또는 `spotifyTrackId` 누락/blank: HTTP 400
- Spotify 실패: 기존 `SPOTIFY_EXCEPTION` 체계
- 제한된 동시성 복구 후에도 저장 실패: 서버 오류 전파. 이번 milestone에서 새 error code를 선행 추가하지 않는다.

Swagger는 `TrackApi` interface에 Endpoint, Method, Request, Response, 인증 불필요, validation 오류 및 Spotify 오류를 실제 구현과 일치하도록 문서화한다. Controller에는 Swagger 문서화 annotation을 추가하지 않는다.

## Scope

### In Scope

- `POST /api/tracks/import` 공개 API
- Import Request / Response DTO와 Swagger contract
- Spotify 상세 Track 응답을 provider-neutral metadata로 변환하는 adapter
- Artist / Album / Track Entity, Artist credit 연결 Entity와 JPA Repository
- `album_artist`, `track_artist` schema와 전체 Artist 순서 보존
- Spotify ID 기반 find-or-create 및 metadata update
- PK, FK, unique 제약 및 명세가 요구하는 인덱스
- Catalog Artist 관계를 연결 테이블로 정렬하는 Server Spec / Tag Architecture 문서와 ADR
- 외부 호출과 Catalog write transaction 분리
- 동일 Track 재Import의 멱등성
- concurrent first import의 unique 충돌 후 재조회 및 제한적 재시도
- Application, Spotify adapter, JPA, Controller 및 Swagger 테스트
- 완료 시 `progress.md` 갱신과 Plan 완료 디렉터리 이동

### Upsert Policy

이번 milestone의 upsert는 DB별 native upsert 문이 아니라 JPA 기반 find-or-create/update다.

- Import 시작 시 Track이 이미 존재하면 Catalog read model을 즉시 반환하고 Spotify를 호출하지 않는다.
- Track이 없을 때만 Spotify metadata를 조회한다.
- 저장 transaction 진입 후 Track을 다시 조회해 외부 호출 중 다른 요청이 저장했는지 확인한다.
- Track Artist와 Album Artist 전체의 Spotify ID를 합치고 중복 제거한 뒤 `findAllBySpotifyIdIn(...)` 한 번으로 조회한다.
- 필요한 Artist만 생성한다.
- Album은 Spotify ID로 조회하여 없으면 생성한다. 새 Album이면 Spotify Album Artist 배열 전체를 `album_artist`에 순서대로 저장한다.
- Track은 Spotify ID로 생성하고 Spotify Track Artist 배열 전체를 `track_artist`에 순서대로 저장한다.
- 이미 존재하는 Track은 fast path로 반환하므로 Artist credit을 refresh하지 않는다.
- 이미 존재하는 Album의 metadata와 Artist credit도 이 milestone에서 refresh하지 않는다. 별도의 metadata refresh 정책 없이 Import가 기존 Catalog 관계를 암묵적으로 바꾸지 않게 한다.
- `musicbrainz_id`는 Spotify Import가 설정하거나 `null`로 덮어쓰지 않는다.
- DB unique constraint를 데이터 정합성의 최종 방어선으로 사용한다.

최초 Import의 Catalog 선행 조회 수는 입력 Artist 수에 비례해 증가하지 않아야 한다. Track Artist와 Album Artist가 같거나 목록 사이에 중복 ID가 있으면 Artist 조회와 생성은 하나로 합친다. 동일 Track / Album 안에 Spotify Artist ID가 중복되면 첫 occurrence만 보존하고 position을 연속적으로 정규화한다.

## Do Not Touch

- 기존 `GET /api/tracks` path, query, response 및 검색어 기록 동작
- 기존 Search Application 모델과 `SpotifyTrackSearchAdapter`
- `GET /api/tracks/{trackId}`의 Spotify ID 의미와 response shape
- `GET /api/tracks/ranking`
- legacy `TrackService`의 상세조회와 랭킹 책임
- Spotify access token cache와 기존 예외 변환
- `BoardEntity.trackId` 타입과 Board API
- Board와 내부 Track 사이의 FK
- Track 상세 API의 Catalog 조회 전환
- MusicBrainz / Discogs client와 enrichment
- Tag, Observation, Assertion 및 Resolver
- Artist credit의 role 분류, performer / composer / producer 등 Spotify가 제공하지 않는 상세 역할
- 이미지 영속화
- Catalog 삭제 및 관리자 수정 API
- 전체 package 일괄 이동
- Flyway / Liquibase 도입
- 관련 없는 schema, Entity 및 legacy code 정리

## Artist / Album / Track 책임과 관계

### Artist

- 내부 Artist identity를 제공한다.
- Spotify Artist ID를 Catalog 생성의 외부 identity seed로 가진다.
- 이름과 향후 MusicBrainz Artist MBID를 보관한다.
- Album / Track 또는 Artist credit 컬렉션을 소유하지 않는다.

### Album

- 내부 Album identity를 제공한다.
- Spotify Album ID로 중복을 막는다.
- 제목, release year 및 향후 MusicBrainz Release Group MBID를 보관한다.
- Artist 단일 FK를 갖지 않는다.
- Album Artist 전체는 `AlbumArtistEntity`가 순서와 함께 연결한다.

### Track

- Catalog Import와 향후 Board / Tag 연결의 기준 Aggregate다.
- Spotify Track ID로 중복을 막는다.
- 제목, ISRC, duration을 보관한다.
- Album을 단방향 `ManyToOne(LAZY)`으로 참조한다.
- Artist 단일 FK를 갖지 않는다.
- Track Artist 전체는 `TrackArtistEntity`가 순서와 함께 연결한다.

### AlbumArtist / TrackArtist

- `AlbumArtistEntity`와 `TrackArtistEntity`는 JPA `@ManyToMany`가 아니라 속성을 가진 명시적 연결 Entity다.
- 양쪽 부모를 `ManyToOne(LAZY)`으로 참조한다.
- Spotify 배열 순서를 0-based `position`으로 보존한다.
- `position=0`을 표시상 대표 Artist로 해석하되 별도 대표 Artist FK나 boolean을 중복 저장하지 않는다.
- Spotify API가 제공하지 않는 performer / composer / producer 역할은 추측하거나 저장하지 않는다.
- 연결 Entity는 별도 Aggregate Root가 아니며 `CatalogWriteService`가 Album / Track 생성과 같은 transaction에서 생명주기를 조율한다.

Artist, Album, Track을 각각 독립 Aggregate Root로 유지하며 다음 양방향 컬렉션을 만들지 않는다.

```text
ArtistEntity.albums
ArtistEntity.tracks
ArtistEntity.albumArtists
ArtistEntity.trackArtists
AlbumEntity.artistCredits
AlbumEntity.tracks
TrackEntity.artistCredits
```

목록과 Artist credit은 명시적인 Repository query로 조회한다.

```text
AlbumArtistJpaRepository.findAllByAlbumIdOrderByPosition(...)
AlbumArtistJpaRepository.findAllByArtistId(...)
TrackArtistJpaRepository.findAllByTrackIdOrderByPosition(...)
TrackArtistJpaRepository.findAllByArtistId(...)
TrackJpaRepository.findByAlbumId(...)
```

Import response 조립에 필요한 Track / Album Artist 조회만 이번 milestone에 구현한다. Artist 기준 Album / Track 목록과 Album 기준 Track 목록 API는 후속 milestone에서 필요한 query를 추가한다. 양방향 컬렉션 없이도 연결 테이블과 명시적 join, projection 및 pageable query로 조회할 수 있다.

## 데이터 흐름

### 이미 Import된 Track

```text
POST /api/tracks/import
  -> TrackImportService
  -> CatalogTrackReadService.findBySpotifyId()
  -> Track + Album 기본 정보 조회
  -> track_artist + Artist 순서 조회
  -> album_artist + Artist 순서 조회
  -> ImportedTrack
  -> CatalogTrackResponse
```

- Spotify API 호출 없음
- Catalog write 없음

### 최초 Import

```text
POST /api/tracks/import
  -> TrackImportService
  -> TrackJpaRepository.findDetailBySpotifyId()
  -> 없음
  -> SpotifyTrackMetadataProvider.getTrack(spotifyTrackId)
  -> SpotifyTrackMetadataAdapter
  -> SpotifyWebClient.getDetailTrackInfo()
  -> Spotify SDK Track를 provider-neutral metadata로 변환
  -> 외부 호출 종료

  -> CatalogWriteService.upsert(metadata)
     [@Transactional 시작]
     -> Track 재조회
     -> Track / Album Artist 전체 ID 통합 및 중복 제거
     -> Artist 일괄 조회 후 필요한 Artist 생성
     -> Album find-or-create
     -> 신규 Album이면 AlbumArtist 전체 저장
     -> Track 생성
     -> TrackArtist 전체 저장
     -> flush
     [commit]

  -> ImportedTrack 반환
```

### Query Budget

기존 Track fast path:

```text
Track + Album 기본 정보 조회 1회
Track Artist 목록 조회 1회
Album Artist 목록 조회 1회
Spotify 호출 0회
쓰기 0회
```

최초 Import의 정상 경로:

```text
Track fast-path 조회 1회
Spotify 호출 1회
transaction 내부 Track 재조회 1회
Artist 일괄 조회 1회
Album 조회 1회
기존 Album이면 Album Artist 목록 조회 1회
필요한 Artist / Album / Track / credit INSERT
```

이 조회들은 Artist 수에 따라 증가하지 않는 고정된 query다. Artist credit 목록은 row 수만 늘고 Artist별 SELECT를 발생시키지 않는다. 두 개의 collection을 한 fetch join으로 곱집합 조회하지 않고 목적별 bulk query로 분리한다. DB별 native `MERGE` 도입보다 정합성, portability 및 reviewability를 우선한다.

### Concurrent First Import

```text
Request A --+
             +-> 동일 Spotify Track 조회/저장
Request B --+

DB UNIQUE 충돌
  -> 실패한 transaction 전체 rollback
  -> spotifyTrackId로 기존 Track 재조회
  -> 존재하면 기존 결과 반환
```

서로 다른 Track이 같은 신규 Artist 또는 Album을 동시에 생성하는 부모 row 경합도 고려한다. unique 충돌 후 대상 Track이 아직 없다면 동일 metadata로 Catalog write transaction을 한 번만 재시도한다. `track_artist`와 `album_artist`의 unique 제약도 중복 연결의 최종 방어선으로 사용한다. 두 번째 실패는 숨기지 않고 전파한다. Redis 또는 distributed lock은 도입하지 않는다.

## DB / JPA 설계

### artist

| Column | Policy |
|---|---|
| `artist_id` | bigint identity PK |
| `name` | not null |
| `spotify_id` | not null, unique |
| `musicbrainz_id` | nullable |
| `created_at` | not null |
| `updated_at` | not null |

제약과 인덱스:

- `PK(artist_id)`
- `UNIQUE(spotify_id)`
- `INDEX(musicbrainz_id)`
- `INDEX(name)`

### album

| Column | Policy |
|---|---|
| `album_id` | bigint identity PK |
| `title` | not null |
| `spotify_id` | not null, unique |
| `musicbrainz_id` | nullable; 향후 Release Group MBID |
| `release_year` | nullable |
| `created_at` | not null |
| `updated_at` | not null |

제약과 인덱스:

- `PK(album_id)`
- `UNIQUE(spotify_id)`
- `INDEX(musicbrainz_id)`
- `INDEX(title)`

### track

| Column | Policy |
|---|---|
| `track_id` | bigint identity PK |
| `title` | not null |
| `spotify_id` | not null, unique |
| `musicbrainz_id` | nullable; 향후 Recording MBID |
| `isrc` | nullable |
| `duration_ms` | not null |
| `album_id` | not null FK |
| `created_at` | not null |
| `updated_at` | not null |

제약과 인덱스:

- `PK(track_id)`
- `FK(album_id -> album.album_id)`
- `UNIQUE(spotify_id)`
- `INDEX(musicbrainz_id)`
- `INDEX(isrc)`
- `INDEX(album_id)`
- `INDEX(title)`

### album_artist

| Column | Policy |
|---|---|
| `album_artist_id` | bigint identity PK |
| `album_id` | not null FK |
| `artist_id` | not null FK |
| `position` | non-negative int, not null |

제약과 인덱스:

- `PK(album_artist_id)`
- `FK(album_id -> album.album_id)`
- `FK(artist_id -> artist.artist_id)`
- `UNIQUE(album_id, artist_id)`
- `UNIQUE(album_id, position)`
- `INDEX(artist_id, album_id)`

### track_artist

| Column | Policy |
|---|---|
| `track_artist_id` | bigint identity PK |
| `track_id` | not null FK |
| `artist_id` | not null FK |
| `position` | non-negative int, not null |

제약과 인덱스:

- `PK(track_artist_id)`
- `FK(track_id -> track.track_id)`
- `FK(artist_id -> artist.artist_id)`
- `UNIQUE(track_id, artist_id)`
- `UNIQUE(track_id, position)`
- `INDEX(artist_id, track_id)`

### JPA 관계와 조회

```text
TrackEntity --ManyToOne(LAZY)--> AlbumEntity

AlbumArtistEntity --ManyToOne(LAZY)--> AlbumEntity
AlbumArtistEntity --ManyToOne(LAZY)--> ArtistEntity

TrackArtistEntity --ManyToOne(LAZY)--> TrackEntity
TrackArtistEntity --ManyToOne(LAZY)--> ArtistEntity
```

- Entity는 `BaseTime`을 상속한다.
- cascade persist/remove 및 orphan removal은 사용하지 않는다.
- `CatalogWriteService`가 Artist -> Album -> AlbumArtist -> Track -> TrackArtist 저장 순서를 명시적으로 제어한다.
- Catalog read는 Track / Album 기본 정보, Track Artist 목록, Album Artist 목록을 고정된 bulk query로 조립한다.
- Artist credit query는 연결 Entity에서 Artist를 fetch join하여 Artist별 추가 SELECT를 만들지 않는다.
- OSIV가 꺼져 있어도 transaction 밖에서 response mapping이 가능해야 한다.
- `AlbumEntity`와 `TrackEntity`에 Artist collection을 두지 않아 multiple bag fetch와 collection pagination 문제를 피한다.

## MusicBrainz ID Lifecycle

`CATALOG-001`에서는 `artist.musicbrainz_id`, `album.musicbrainz_id`, `track.musicbrainz_id`를 `null`로 저장한다.

```text
Spotify Catalog Import commit
  -> musicbrainz_id = null

후속 External Enrichment
  -> Track의 ISRC / title / artist / duration으로 MusicBrainz HTTP 조회
  -> track.musicbrainz_id = Recording MBID
  -> album.musicbrainz_id = Release Group MBID
  -> 신뢰할 수 있게 식별된 경우 artist.musicbrainz_id = Artist MBID
  -> 별도의 짧은 transaction으로 갱신
```

- Spotify Import는 MusicBrainz ID를 작성하거나 초기화하지 않는다.
- MusicBrainz HTTP 호출을 Catalog transaction 안에서 실행하지 않는다.
- 미매칭 또는 provider 실패 시 `null`을 유지하고 Catalog Import 성공은 보존한다.
- Spotify 재Import는 이미 저장된 MusicBrainz ID를 덮어쓰지 않는다.
- 매칭, 재시도 및 잘못된 매칭 교정 정책은 External Enrichment milestone에서 결정한다.

## Transaction

`TrackImportService`에는 `@Transactional`을 붙이지 않는다.

```text
Track 존재 여부 조회
  -> transaction 종료
Spotify HTTP
  -> 외부 호출 종료
CatalogWriteService.upsert()
  -> 새 write transaction
```

`CatalogWriteService.upsert()`만 `@Transactional`을 가진다. 별도 Spring Bean으로 분리해 self-invocation 때문에 transaction proxy가 무효화되지 않게 한다.

저장 transaction은 Artist / Album / Track과 Artist credit 연결의 원자적 생성에만 사용한다. Track 또는 Album row만 저장되고 credit이 일부 누락된 상태로 commit되어서는 안 된다. `saveAndFlush` 또는 명시적 flush로 unique 충돌이 write service 경계 안에서 발생하게 한다. 실패한 transaction 안에서 재조회하거나 저장을 계속하지 않는다. `TrackImportService`가 rollback 이후 `DataIntegrityViolationException`을 처리한다.

## 기존 API 영향

기존 API contract 변경은 없다.

- `GET /api/tracks`: 기존 Spotify 검색, 게스트/비로그인 접근 유지
- `GET /api/tracks/{trackId}`: 기존 Spotify 상세조회, 게스트/비로그인 접근 유지
- `GET /api/tracks/ranking`: 기존 랭킹, 공개 접근 유지
- Search response의 `trackId`: 계속 Spotify Track ID
- Board request의 `trackId`: 계속 Spotify ID 문자열

신규 `POST /api/tracks/import`도 동일하게 공개 접근을 허용한다. 신규 response의 내부 ID만 `catalogTrackId`로 구분한다. 검색 GET 요청에서 Catalog 저장을 암묵적으로 실행하지 않는다.

## 향후 Tag / External Enrichment 연결

이번 milestone에서는 enrichment를 실행하지 않지만 다음 identity seed를 저장한다.

- Track: 내부 PK, Spotify ID, title, ISRC, duration
- Album: 내부 PK, Spotify ID, title, release year
- Artist: 내부 PK, Spotify ID, name
- Track / Album Artist credit: 내부 Artist 연결과 Spotify 표시 순서
- nullable MusicBrainz ID

후속 MusicBrainz matching은 Track의 ISRC, title, artist, duration으로 Recording MBID를 식별할 수 있다. Album의 `musicbrainz_id`는 Release가 아니라 Release Group MBID로 고정한다.

Tag / Observation 계층은 안정적인 내부 Catalog PK를 subject identity로 사용할 수 있다. 이번 milestone에서는 해당 테이블, port 또는 service를 선행 구현하지 않는다.

## 변경 / 생성 파일

정확한 package와 클래스명은 구현 시작 시 기존 naming과 충돌 여부를 다시 확인하되 책임과 범위는 아래를 따른다.

### 변경 파일

| File | Responsibility |
|---|---|
| `tagnote-api/src/main/java/com/tagnote/api/domain/tracks/TrackController.java` | 공개 Import endpoint를 Application Service에 연결한다. role annotation을 추가하지 않는다. |
| `tagnote-api/src/main/java/com/tagnote/api/domain/tracks/TrackApi.java` | 신규 endpoint, 인증 불필요, request/response 및 주요 오류를 문서화한다. |
| `tagnote-core/src/main/java/com/tagnote/core/exception/ValidExceptionManager.java` | DTO validation 및 누락된 request body를 공통 `ApiResult` 형식의 HTTP 400으로 반환한다. |
| `tagnote-core/src/main/resources/db/init_schema.sql` | Artist / Album / Track / Artist credit 테이블, FK, unique 및 index를 추가한다. |
| `tagnote-api/src/test/java/com/tagnote/api/domain/tracks/TrackControllerTest.java` | 공개 Import route, validation 및 response contract를 검증한다. |
| `agents/server/server_spec.md` | Album / Track의 단일 `artist_id`를 연결 테이블 기반 전체 Artist credit으로 변경하고 제약 및 조회 의미를 명시한다. |
| `agents/server/server_tag_feature_architecture.md` | Catalog 모델, Aggregate 설명과 ERD의 단일 Artist 관계를 동일한 연결 테이블 구조로 정렬한다. |
| `agents/server/progress.md` | 모든 구현과 검증이 완료된 시점에 milestone 완료 상태를 기록한다. |
| `agents/server/plans/active/CATALOG-001.md` | 완료 시 `plans/completed/`로 이동한다. |

### 생성 — Architecture Decision

- `agents/server/decisions/ADR-001-catalog-multi-artist-credits.md`

ADR에는 단일 `artist_id` 대신 `album_artist` / `track_artist`를 관계의 source of truth로 선택한 결정, `position=0`의 대표 의미, 명시적 연결 Entity 사용 이유와 기존 명세 변경 결과를 `Decision / Reason / Consequence` 형식으로 기록한다. 초기 빈 placeholder인 `agents/decisions/ADR-000-subject.md`는 정식 ADR 생성 시 제거한다.

### 생성 — Presentation

- `tagnote-api/src/main/java/com/tagnote/api/domain/tracks/dto/request/ImportTrackRequest.java`
- `tagnote-api/src/main/java/com/tagnote/api/domain/tracks/dto/response/CatalogTrackResponse.java`
- `tagnote-api/src/main/java/com/tagnote/api/domain/tracks/dto/response/CatalogAlbumResponse.java`
- `tagnote-api/src/main/java/com/tagnote/api/domain/tracks/dto/response/CatalogArtistResponse.java`

### 생성 — Application

- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/TrackImportService.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/CatalogWriteService.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/CatalogTrackReadService.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/model/SpotifyTrackMetadata.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/model/SpotifyArtistMetadata.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/model/ImportedTrack.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/importer/port/SpotifyTrackMetadataProvider.java`

`SpotifyTrackMetadata`는 Track Artist와 Album Artist 전체 목록을 구분하고 Spotify 배열 순서를 보존한다.

### 생성 — Domain Catalog

- `tagnote-core/src/main/java/com/tagnote/domain/catalog/artist/ArtistEntity.java`
- `tagnote-core/src/main/java/com/tagnote/domain/catalog/album/AlbumEntity.java`
- `tagnote-core/src/main/java/com/tagnote/domain/catalog/album/AlbumArtistEntity.java`
- `tagnote-core/src/main/java/com/tagnote/domain/catalog/track/TrackEntity.java`
- `tagnote-core/src/main/java/com/tagnote/domain/catalog/track/TrackArtistEntity.java`

### 생성 — Infrastructure

- `tagnote-core/src/main/java/com/tagnote/infrastructure/external/spotify/SpotifyTrackMetadataAdapter.java`
- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/catalog/ArtistJpaRepository.java`
- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/catalog/AlbumJpaRepository.java`
- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/catalog/AlbumArtistJpaRepository.java`
- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/catalog/TrackJpaRepository.java`
- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/catalog/TrackArtistJpaRepository.java`

기존 `SpotifyWebClient`에는 단건 조회 기능이 있으므로 변경하지 않는 것을 우선한다.

### 생성 — Test

- `tagnote-core/src/test/java/com/tagnote/application/catalog/importer/TrackImportServiceTest.java`
- `tagnote-core/src/test/java/com/tagnote/application/catalog/importer/CatalogTrackReadServiceTest.java`
- `tagnote-core/src/test/java/com/tagnote/application/catalog/importer/CatalogWriteServiceTest.java` 또는 동등한 JPA 통합 테스트
- `tagnote-core/src/test/java/com/tagnote/infrastructure/external/spotify/SpotifyTrackMetadataAdapterTest.java`
- `tagnote-core/src/test/java/com/tagnote/infrastructure/persistence/catalog/CatalogJpaTestConfiguration.java`
- `tagnote-core/src/test/java/com/tagnote/infrastructure/persistence/catalog/CatalogJpaRepositoryTest.java`
- `tagnote-core/src/test/java/com/tagnote/infrastructure/persistence/catalog/CatalogImportConcurrencyTest.java`

## 테스트 계획

### Application Unit Test

- 기존 Track이 있으면 Spotify provider와 write service를 호출하지 않는다.
- Track이 없으면 Spotify metadata를 받은 뒤 write service를 호출한다.
- Spotify 실패 시 write service를 호출하지 않는다.
- unique 충돌 후 동일 Track을 재조회해 기존 결과를 반환한다.
- 부모 Artist / Album 경합으로 Track이 아직 없으면 write를 한 번만 재시도한다.
- 제한된 재시도도 실패하면 예외를 전파한다.

### Spotify Adapter Test

- Track / Album / Artist Spotify ID와 이름을 올바르게 매핑한다.
- Track Artist와 Album Artist 배열 전체를 서로 구분하여 보존한다.
- 각 Artist 배열의 원래 순서를 `position`으로 보존한다.
- 같은 배열의 중복 Spotify Artist ID는 첫 occurrence만 남기고 position을 연속적으로 정규화한다.
- ISRC를 Spotify external IDs에서 추출한다.
- duration을 보존한다.
- release date에서 release year를 안전하게 변환한다.
- release date 또는 ISRC가 없으면 nullable 값으로 처리한다.
- Track 또는 Album Artist 목록이 비었거나 필수 identity가 누락되면 명시적으로 실패 처리한다.
- Spotify SDK 타입이 Application port 밖으로 노출되지 않는다.
- 기존 `SPOTIFY_EXCEPTION`을 재해석하지 않는다.

### JPA / Integration Test

- 신규 metadata가 Artist / Album / Track과 모든 Artist credit으로 저장된다.
- `track_artist`, `album_artist` FK와 position이 올바르게 저장된다.
- 동일 metadata를 다시 저장해도 row 수가 증가하지 않는다.
- `spotify_id` unique 제약이 실제 DB에서 동작한다.
- `(track_id, artist_id)`, `(track_id, position)`, `(album_id, artist_id)`, `(album_id, position)` unique 제약이 실제 DB에서 동작한다.
- Artist lookup이 Spotify ID bulk query 한 번으로 수행되고 Artist별 반복 SELECT가 없다.
- 기존 MusicBrainz ID가 Spotify upsert로 지워지지 않는다.
- Track / Album Artist가 여러 명이어도 순서가 유지된 read model을 transaction 밖에서 response로 변환할 수 있다.
- Artist credit read가 Artist별 SELECT를 발생시키지 않는다.
- 동일 Spotify Track의 병렬 Import 후 각 Spotify ID당 row가 한 건만 존재한다.
- 동일 Spotify Track의 병렬 Import 후 연결 row도 중복되지 않는다.
- 서로 다른 Track이 동일 Artist / Album을 공유해도 Artist와 Album row가 중복되지 않는다.

### Controller / Swagger Test

- `POST /api/tracks/import` route와 JSON field 이름을 검증한다.
- blank 또는 missing `spotifyTrackId`는 HTTP 400이다.
- 비로그인, 게스트 권한 및 일반 사용자 요청 모두 role 제한 없이 endpoint에 접근할 수 있다.
- `ApiResult`, `catalogTrackId`, Track Artist 목록과 Album Artist 목록 response shape 및 순서를 검증한다.
- 기존 GET 검색, 상세 및 랭킹 characterization test를 그대로 통과시킨다.
- OpenAPI 문서에 endpoint, 인증 불필요, request, response 및 주요 오류가 노출되는지 확인한다.

## 위험 요소

- **명세 변경**: 기존 spec의 단일 `artist_id` 관계를 제거한다. production 구현과 동시에 `server_spec.md`, Tag Architecture ERD 및 ADR을 일관되게 갱신하지 않으면 Source of Truth가 충돌한다.
- **Album Artist와 Track Artist 혼동**: compilation 등에서는 두 목록이 다르다. provider metadata, 연결 테이블, read model 및 response에서 독립 목록으로 유지한다.
- **Credit 순서 정합성**: 같은 parent 안에서 Artist 중복이나 position 충돌이 생기면 표시 순서가 모호해진다. adapter 정규화와 두 종류의 DB unique 제약으로 방어한다.
- **Credit 역할 한계**: Spotify Artist 배열은 상세 역할 정보를 제공하지 않는다. 모든 등록 Artist와 순서는 보존하지만 performer / featuring 등의 역할을 추측하지 않는다.
- **기존 Album metadata**: 새 Track이 기존 Album을 참조할 때 Album Artist 목록을 암묵적으로 refresh하지 않는다. Spotify credit 변경 반영은 별도 metadata refresh milestone이 필요하다.
- **동시성**: Application 선조회만으로 중복을 막을 수 없다. DB unique와 rollback 후 재조회가 최종 방어선이다.
- **Flush 시점**: commit까지 unique 위반이 드러나지 않으면 잘못된 transaction 안에서 복구할 수 있다. write service에서 flush하고 transaction 밖에서만 복구한다.
- **Lazy loading**: OSIV가 비활성화되어 있다. 단건 read query에서 response에 필요한 관계를 명시적으로 적재한다.
- **ID 의미 혼동**: 기존 `trackId`는 Spotify ID다. 내부 PK는 신규 API에서 `catalogTrackId`로 구분한다.
- **기존 상세조회 transaction**: legacy `TrackService`는 read-only transaction 안에서 Spotify를 호출한다. 알려진 문제지만 이번 milestone에서 수정하지 않는다.
- **Production schema 배포**: local `init_schema.sql`과 JPA metadata 변경만으로 Oracle `ddl-auto=validate` 환경에 schema가 자동 적용되지 않는다. 실제 운영 배포 전에 별도 DDL 적용 절차가 필요하다. migration tool 도입은 별도 milestone이다.
- **Provider metadata 결손**: Track / Album Artist 목록이 비었거나 필수 identity가 없는 응답은 불완전한 Catalog를 저장하지 않고 실패 처리한다.
- **과도한 refresh**: 기존 Track fast path에서 Spotify를 다시 조회하면 재사용 요구와 응답 성능을 해친다. metadata refresh 정책은 별도 기능이다.
- **공개 write endpoint 남용**: Import는 DB write와 Spotify 호출을 유발하지만 요청에 따라 게스트/비로그인 접근을 허용한다. rate limiting, abuse prevention 및 quota 정책은 이번 milestone에 포함하지 않으며 운영 필요성이 확인되면 별도 milestone로 다룬다.

## Acceptance Criteria

- [x] `POST /api/tracks/import`에 Spotify Track ID를 전달해 내부 Artist / Album / Track과 Artist credit 전체를 생성할 수 있다.
- [x] Import endpoint는 `ROLE_USER`를 요구하지 않으며 비로그인 및 게스트도 호출할 수 있다.
- [x] Import response가 내부 `catalogTrackId`, 순서가 보존된 Track Artist 목록과 Album Artist 목록을 반환한다.
- [x] 기존 Track 재Import 시 Spotify API가 호출되지 않는다.
- [x] Artist / Album / Track의 Spotify ID에 DB unique 제약이 존재한다.
- [x] Track -> Album FK와 `album_artist`, `track_artist`의 parent / Artist FK가 존재한다.
- [x] Album과 Track에는 중복 source of truth가 되는 단일 `artist_id`가 없다.
- [x] `(parent_id, artist_id)`와 `(parent_id, position)` unique 제약이 Album / Track Artist 연결에 모두 존재한다.
- [x] `position=0`이 대표 Artist이며 Spotify 배열의 전체 Artist 순서가 보존된다.
- [x] 명세의 Catalog index가 schema와 JPA mapping에 반영된다.
- [x] 동일 Track을 반복 Import해도 row 수가 증가하지 않는다.
- [x] 동일 Track의 concurrent first import 후에도 각 Catalog row가 한 건만 존재한다.
- [x] 동일 Track의 concurrent first import 후에도 Artist credit 연결이 중복되지 않는다.
- [x] 외부 Spotify HTTP 요청 동안 Catalog write transaction이 열려 있지 않다.
- [x] 최초 Import의 Artist 조회는 Track Artist와 Album Artist ID를 중복 제거한 단일 bulk query로 수행한다.
- [x] Import당 Catalog 선행 조회 수가 입력 아티스트 수에 비례해 증가하지 않는다.
- [x] JPA 관계는 단방향 LAZY이며 불필요한 역방향 컬렉션이 없다.
- [x] Import 결과 조회에서 lazy-loading 오류와 N+1이 발생하지 않는다.
- [x] MusicBrainz ID는 nullable이며 Spotify Import가 기존 값을 덮어쓰지 않는다.
- [x] 기존 Search / Detail / Ranking 및 Board API contract가 변하지 않는다.
- [x] Swagger interface가 신규 공개 API와 주요 오류를 실제 구현대로 문서화한다.
- [x] `server_spec.md`, `server_tag_feature_architecture.md`와 신규 ADR이 연결 테이블 기반 Artist 관계로 일치한다.
- [x] MusicBrainz / Discogs / Tag / Board FK 구현이 포함되지 않는다.
- [x] 모든 신규 및 기존 테스트와 검증 명령이 통과한다.
- [x] diff review에서 관련 없는 변경이 없다.
- [x] 완료 후 Plan 이동과 `progress.md` 갱신이 함께 이루어진다.

## Verification

완료 검증 결과:

- PASS: Catalog/Spotify 대상 테스트
- PASS: 동일 Track 및 공유 Album/Artist 병렬 Import 통합 테스트
- PASS: `./gradlew test`
- PASS: `./gradlew check`
- PASS: `./scripts/verify.sh`
- 실행 환경: Windows JDK 17, Gradle wrapper

구현 중 빠른 검증:

```bash
./gradlew :tagnote-core:test
./gradlew :tagnote-api:test
```

최종 필수 검증:

```bash
./gradlew test
./gradlew check
./scripts/verify.sh
```

추가 검토:

- 비로그인 및 게스트 사용자의 Import API 접근 확인
- OpenAPI UI 또는 generated spec에서 Import contract와 인증 불필요 표시 확인
- schema의 PK / FK / unique / index와 Entity annotation 대조
- Spotify Track / Album Artist 배열의 ID, 개수 및 순서가 DB 연결 row와 response에 모두 보존되는지 대조
- Album / Track에 단일 `artist_id`가 없고 연결 테이블만 Artist 관계의 source of truth인지 확인
- 동일 Track 및 부모 Artist / Album 경합 동시성 테스트 반복 실행
- SQL 로그로 기존 Track fast path와 최초 Import의 query budget, Artist별 추가 SELECT 부재 확인
- 외부 Spotify 호출 시 활성 Catalog write transaction이 없음을 테스트
- `git diff --check`
- scope diff와 기존 API characterization test 검토
