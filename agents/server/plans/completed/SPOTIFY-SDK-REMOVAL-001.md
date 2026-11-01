# SPOTIFY-SDK-REMOVAL-001 — Spotify Java SDK 제거

> 상태: 2026-10-30 사용자 테스트 완료 확인 및 완료 처리 요청에 따라 completed.

## 구현 기록 (2026-10-30)

- `SpotifyHttpClient`와 Infrastructure JSON DTO로 SDK 호출을 교체하고 기존 SDK 의존성·중간 DTO를 제거했다. Redis 토큰 키, 401 1회 갱신, 429 최대 2회 재시도, 2초/5초 timeout을 적용했다.
- Track 상세 provider port를 추가하고 클래스 수준 read-only transaction을 제거했다. Artist Album 목록의 `total_tracks`로 6곡 이하 `album`의 추가 Track 요청을 생략했다.
- 검색·상세·import JSON fixture와 HTTP 인증·오류 경계 테스트를 작성했다. Swagger 및 클라이언트 `api-contract.md`와 공개 필드를 대조했고 계약 변경은 없다.
- 변경 범위 `git diff --check`는 통과했다. Codex는 프로젝트 규칙에 따라 Gradle 테스트·전체 검증을 실행하지 않았다. 2026-10-30 사용자가 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`를 모두 완료했다고 확인하고 Plan 완료 처리를 요청했다. 사용자 확인을 세 명령의 검증 근거로 기록한다.

## Goal

Spotify Java SDK 8.0.0에 의존하는 서버 호출을 직접 Spotify HTTP 호출과 프로젝트 소유 DTO로 바꾸고 의존성을 제거한다. 공개 API의 Request/Response, 검색 정렬·페이지, import, Album 발매 분류 및 오류 계약은 유지한다. 후속 `CATALOG-ARTIST-ALBUM-INDEX-001`이 사용할 아티스트 앨범 목록의 `album_type`·`total_tracks`를 제공한다.

## 조사 결과와 범위

- 현재 `SpotifyWebClient`가 Client Credentials 토큰 발급, Track 검색, Catalog 통합 검색, Track/Album/Artist 단건, Artist Album 목록, Album Track 목록을 SDK로 호출한다. SDK 모델이 `TrackService`, `TrackInfo`, `SpotifyCatalogSearchInfo`와 Spotify adapter·테스트에도 노출되어 있다.
- 서버 루트의 `tagnote-core/build.gradle`에서 `spotify-web-api-java:8.0.0`을 제거해야 한다. 서버 루트 Gradle 모듈의 production/test 코드에 SDK 타입 import가 남지 않아야 한다. 별도 `tag-note-client` worktree에 있는 서버 코드 복사본은 이 마일스톤의 변경 대상이 아니다.
- 현재 Artist Album 목록의 `AlbumSimplified`는 `total_tracks` 접근자를 제공하지 않는다. 직접 HTTP 응답에는 이 필드를 담아 후속 인덱스에서 6곡 이하를 추가 Track 조회 없이 판정할 수 있게 한다.
- ADR-001/005/018/021/022의 Artist credit 순서, Catalog 검색 호출·정렬, `releaseType` 규칙과 부분 실패 정책은 유지한다. SDK 제거로 ADR-022의 "SDK 제약 때문에 album 유형마다 Track 조회" 근거가 달라지므로 착수 시 다음 사용 가능한 번호의 ADR(현재 예상 ADR-023)에 새 HTTP 경계와 호출 절감 결정을 기록한다.
- 이 Plan은 Spotify 호출 전환과 의존성 제거만 다룬다. 아티스트 전체 발매 인덱스·Redis 스냅샷·유형별 필터 API·React 탭 변경은 이미 승인된 후속 Plan의 범위다.

## 호출별 보존 계약

| 현재 사용처 | Spotify HTTP | 보존할 동작 |
| --- | --- | --- |
| 토큰 발급 | `POST https://accounts.spotify.com/api/token` | Client Credentials, Redis 토큰 캐시, 서버에서만 secret 사용 |
| 기존 Track 검색 | `GET /v1/search?type=track` | `limit=10`, `offset=page*10`, 대표 Artist·Album·이미지·total |
| Catalog 검색/화면 window | `GET /v1/search?type=...` | 기존 type 조합, `album:` 제목 필터, Spotify 각 타입 total·원본 순서 및 호출 횟수 |
| Track 단건/선택 import | `GET /v1/tracks/{id}` | 첫 Artist 이름·ID, Album 이름·ID, ISRC, duration, 전체 Track/Album Artist credit |
| Album 단건/선택 import | `GET /v1/albums/{id}` | Album identity·발매일·Artist 순서, 초기 Track page와 `releaseType` 계산 |
| Artist 단건·Album 목록 | `GET /v1/artists/{id}`, `GET /v1/artists/{id}/albums` | 장르·이미지·Spotify 기본 목록 범위·순서·total, 목록의 `album_type`·`total_tracks` 제공 |
| Album Track 목록 | `GET /v1/albums/{id}/tracks` | page size 10, 디스크/트랙 순서, duration과 후속 페이지 분류 |

## 설계

1. Infrastructure에 Spotify 전용 인증·HTTP 경계를 둔다. 기존 Spring `RestClient` 패턴을 사용하고 토큰 URL과 API URL을 설정으로 분리한다. 인증 provider는 `grant_type=client_credentials`와 Basic 인증으로 토큰을 받고 Redis에 보관한다. 현재 cache key 호환을 유지하며 TTL은 `expires_in`에서 안전 여유를 뺀 값과 기존 30분 중 작은 값으로 설정한다. secret·token을 로그에 남기지 않는다.
2. Spotify JSON DTO는 Infrastructure에만 둔다. 필요한 필드만 역직렬화하되 `tracks`, `albums`, `artists`의 `items`·`total`, `external_ids.isrc`, Album `tracks.items`, Artist Albums `album_type`·`total_tracks`와 nullable 필드를 명시적으로 매핑한다. Application/Domain에 SDK 타입이나 Spotify 원본 JSON DTO를 노출하지 않는다.
3. HTTP 공통 경계에서 query/path 인코딩, Bearer 헤더, 연결·읽기 timeout, 오류 변환을 관리한다. 만료 토큰으로 인한 401은 캐시 무효화 후 한 번만 재발급·재시도한다. 429는 `Retry-After`를 읽어 상한 있는 대기/재시도로 처리하고, 한도를 넘거나 다른 Spotify/네트워크 실패 시 기존 `SPOTIFY_EXCEPTION` 계약으로 변환한다. 무한 재시도나 여러 요청의 동시 토큰 재발급을 허용하지 않는다.
4. 기존 provider port와 검색·상세·import 조율은 유지하면서 SDK 모델을 프로젝트 소유 모델로 치환한다. `TrackService`의 Spotify 상세 호출도 provider port를 통해 Infrastructure와 분리한다. 현재 해당 서비스의 클래스 수준 read-only transaction은 외부 HTTP 동안 DB transaction을 열지 않도록 이 변경 범위에서 제거한다. Redis 랭킹 조회에 DB transaction은 필요하지 않다.
5. Artist Album 목록은 원본 응답의 `total_tracks`를 사용한다. `compilation`·`single`과 `album` 6곡 이하는 추가 Track 호출 없이 기존 `CatalogReleaseType.classify`로 분류한다. 7곡 이상만 기존의 30분 기준 Track 페이지를 읽는다. 기존 `releaseType=null`·목록 부분 성공 정책은 유지한다.
6. 모든 호출이 새 경계로 옮겨진 뒤 `SpotifyWebClient`, SDK 전용 중간 DTO/mapper, SDK 의존성을 제거한다. 다른 모듈에 SDK 의존이 없는지 확인한다. 공개 Controller/Swagger DTO는 API 계약 변경이 없으므로 유지한다.

## 구현 순서

1. 현재 active `CATALOG-ARTIST-ALBUM-RELEASE-TYPE-001`의 동작과 테스트를 기준선으로 확인한다. 검색, 단건, import, 분류의 SDK 응답을 대표하는 JSON fixture와 특성화 테스트를 먼저 작성한다.
2. 토큰 provider와 HTTP 공통 경계를 구현하고 토큰 캐시·401·429·timeout·오류 응답 테스트를 추가한다.
3. 단건 Track/Album/Artist, Album Track, Artist Album 목록 순서로 JSON DTO와 provider adapter를 전환한다. 각 전환에서 API 필드·Artist 순서·누락값·호출 횟수를 대조한다.
4. 기존 Track 검색과 Catalog 통합·타입별·화면 window 검색을 전환한다. `album:` 질의, type 조합, 10개 페이지와 total, 정렬 입력 순서를 대조한다.
5. SDK import·의존성을 제거하고 Server Spec/current_state·새 ADR·후속 Album Index Plan의 provider 선행 조건을 실제 구조에 맞게 갱신한다. API Contract가 바뀌지 않았는지 Swagger와 클라이언트 `api-contract.md`를 대조한다.
6. 사용자 실행 검증 결과와 diff 리뷰 후 completed로 옮긴다. 이후 Album Index Plan 착수 시 새 provider의 `album_type`·`total_tracks`를 재확인한다.

## Acceptance Criteria

- Spotify Client Credentials 발급과 캐시가 정상 동작하며 비밀값이 로그나 클라이언트 응답에 노출되지 않는다. 401 재발급은 1회, 429 재시도는 상한이 있고, 실패 시 기존 `SPOTIFY_EXCEPTION` 응답 형식을 유지한다.
- 기존 Track/Catalog 검색, Track/Album/Artist 상세, Artist Album/Album Track 목록, Track/Album import의 필드·순서·페이지·오류 계약을 특성화 테스트와 비교해 유지한다. 기존 검색 window cache 동작도 유지한다.
- Album 상세의 6곡·30분 경계, Artist Album 목록의 `single`·`compilation` 및 부분 실패 정책은 유지한다. 새 Artist Album provider가 `album_type`과 `total_tracks`를 제공하며 6곡 이하 `album`에 분류용 Track HTTP 호출이 없다.
- SDK 타입 import가 서버 루트 Gradle 모듈의 production/test 코드에 없고 서버 빌드에 SDK 의존성이 없다. 외부 Spotify HTTP 동안 DB transaction이 열리지 않으며 SDK 제거만으로 DB SELECT가 늘지 않는다.
- Swagger·Server Spec·클라이언트 API 계약의 외부 동작을 대조하고 `git diff --check`를 통과한다. Codex는 Gradle·`verify.sh`를 실행하지 않으며 사용자에게 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh` 실행을 요청해 결과를 확인한다.

## 위험과 제외 범위

- 직접 HTTP 전환은 SDK의 JSON 기본값·빈 배열·오류 처리와 다른 결과를 낼 수 있다. 대표 응답 fixture와 경계 테스트로 전환 전후를 비교하고 한 호출씩 이전한다.
- Spotify API 429 및 토큰 재발급 때문에 응답 시간이 늘어날 수 있으므로 재시도 상한과 timeout을 둔다. 정확한 상한·설정값은 구현 전 운영 설정을 확인해 ADR에 기록한다.
- 아티스트 발매 전체 수집과 타입별 API, Redis 인덱스, 클라이언트 필터 변경, 관련 없는 SDK/HTTP 클라이언트 교체는 포함하지 않는다.
