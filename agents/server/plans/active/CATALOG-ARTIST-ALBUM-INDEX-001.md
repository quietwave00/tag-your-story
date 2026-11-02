# CATALOG-ARTIST-ALBUM-INDEX-001 — 아티스트 발매 인덱스와 유형별 페이지

> 상태: 2026-11-02 구현·정적 검토 진행, 사용자 실행 검증 대기. Spotify SDK 제거 선행 Plan 완료 처리.

> 선행 경계 구현 메모 (2026-10-30): `SpotifyHttpClient.artistAlbums`의 Infrastructure DTO가 `album_type`과 `total_tracks`, 원본 순서와 total을 읽는다(ADR-023). 현재 `SpotifyCatalogDetailAdapter`는 이를 nullable `releaseType`으로 변환한다. 본 Plan 착수 시 필요한 provider-neutral 인덱스 입력 모델만 확장하고 사용자 실행 검증 결과를 먼저 확인한다.

## Goal

아티스트 화면의 `ALL`, `ALBUM`, `SINGLE_EP`, `COMPILATION` 탭이 서버에서 분류된 결과의 정확한 0-based 페이지와 건수를 받도록 한다. Spotify 원본 페이지의 10건을 필터 결과 페이지로 오인하지 않는다. 같은 아티스트의 탭 전환·다음 페이지에서 Spotify 전체 목록을 다시 수집하지 않는다.

## 선행 조건

- 별도 마일스톤에서 Spotify Java SDK 의존성과 사용 코드를 제거하고 Spotify HTTP 호출을 프로젝트의 provider-neutral 모델로 매핑한다. 토큰 발급·캐시, timeout, Spotify 오류 변환, 호출 제한과 관련 특성화 테스트를 그 마일스톤에서 검증한다.
- SDK 제거 후의 아티스트 앨범 목록 provider는 Spotify 원본 응답의 `album_type`, `total_tracks`, 목록 순서와 전체 건수를 제공해야 한다. 이 필드가 빠졌다면 SDK 제거 마일스톤에서 보완하고 본 Plan을 시작한다.
- 착수 시 SDK 제거 후 실제 코드와 ADR을 다시 확인한다. 이 Plan은 현재 SDK 8.0.0 사용 코드를 구현 기준으로 삼지 않는다.

## 확인한 현재 상태와 설계 변경

- 현재 `GET /api/catalog/artists/{spotifyArtistId}/albums?page=`는 Spotify 원본 페이지를 그대로 반환하고 각 item의 `releaseType`을 계산한다. React 화면은 원본 페이지를 반복 호출해 클라이언트에서 타입별 10건을 모은다.
- ADR-021은 `compilation -> COMPILATION`, `single -> SINGLE_EP`, `album`은 6곡 이하 또는 30분 미만이면 `SINGLE_EP`, 그 외 `ALBUM`으로 정했다. 이 규칙은 유지한다.
- ADR-022와 현재 active Plan은 원본 페이지·Spotify 전체 건수 계약을 명시한다. 유형별 인덱스·페이지 계약은 이를 확장하므로 구현 시 SDK 제거 ADR 다음 번호의 새 ADR을 작성한다. 기존 Plan을 검증 없이 completed로 이동하지 않는다.
- Spotify Artist Albums 응답의 `album_type`과 `total_tracks`를 선행 마일스톤에서 마련한 Spotify HTTP provider를 통해 읽는다. 본 Plan은 새 Spotify HTTP 클라이언트를 중복 구현하거나 SDK 경로를 추가하지 않는다.

## API Contract

```http
GET /api/catalog/artists/{spotifyArtistId}/albums?releaseType=ALL&page=0
GET /api/catalog/artists/{spotifyArtistId}/albums?releaseType=ALBUM&page=0
GET /api/catalog/artists/{spotifyArtistId}/albums?releaseType=SINGLE_EP&page=0
GET /api/catalog/artists/{spotifyArtistId}/albums?releaseType=COMPILATION&page=0
```

- `releaseType`을 생략하면 기존 원본 Spotify 페이지 계약을 유지한다. 화면은 명시적으로 네 값 중 하나를 보낸다. 허용하지 않는 값, 빈 값 및 음수 page는 HTTP 400이다.
- 준비된 인덱스에 대한 HTTP 200 응답은 기존 `ApiResult` envelope와 `items`(최대 10개), `totalCount`(선택 타입의 건수)를 유지하고 `classificationComplete`, `unclassifiedCount`를 추가한다. page는 **분류 후 목록**의 0-based page다. 빈 마지막 page는 빈 `items`와 동일한 `totalCount`를 반환한다.
- 인덱스 생성 중에는 HTTP 202, `Retry-After: 1`, `ApiResult` envelope의 `response.status=BUILDING`을 반환한다. 화면은 현재 Artist 상세를 유지하고 목록 로딩 상태에서 재요청한다. 생성 완료 시 네 탭 모두 같은 스냅샷으로 조회한다.
- 목록 수집에 실패하면 완성되지 않은 스냅샷을 게시하지 않고 기존 `SPOTIFY_EXCEPTION` 오류 계약을 따른다. 개별 앨범의 트랙 조회 실패는 `releaseType=null`로 보존하고 `classificationComplete=false`로 표시한다. `ALL`에는 그 앨범을 포함하고 유형별 목록에는 확정된 유형만 포함한다. 이때 유형별 `totalCount`는 **확정된 항목 수**이며 완전한 전체 건수로 표시하지 않는다. 불완전한 스냅샷은 짧게 캐시하고 재시도한다.

## 인덱스와 분류

1. 아티스트별 **하나의 불변 스냅샷**을 만든다: Spotify 원본 순서의 `allItems`와 `ALBUM`/`SINGLE_EP`/`COMPILATION`별 원본 위치 목록, `classificationComplete`, `builtAt`, 포맷 버전. 네 개의 전체 앨범 DTO 목록을 중복 저장하지 않는다.
2. 선행 마일스톤의 Spotify HTTP provider로 Artist Albums의 offset 창을 `total` 범위 끝까지 조회한다. 한 창이 10건보다 적거나 비어 있어도 다음 offset을 읽는다. `total` 변경·응답 누락·창 크기 초과를 검사해 불완전한 인덱스 게시를 막고, 원래 목록 순서와 중복을 유지한다(ADR-025).
3. `album_type=compilation`은 추가 Track 호출 없이 `COMPILATION`, `single`은 ADR-021대로 추가 호출 없이 `SINGLE_EP`다. `album_type=album`에서 `total_tracks<=6`이면 추가 호출 없이 `SINGLE_EP`다. `total_tracks>=7`만 Track 페이지의 재생 시간을 합산하고 30분 이상 확인 시 더 읽지 않는다. 기존 `CatalogReleaseType.classify`를 단일 규칙으로 사용한다.
4. 스냅샷은 `artistId`와 분류 정책/형식 버전을 포함한 한 Redis key에 원자적으로 게시하고 TTL을 둔다(초안: 완전 15분, 불완전 1분). 경계가 정해진 작업 실행기로 구축하며 같은 서버 프로세스의 동시 cache miss는 한 빌드로 합친다. Redis에 부분적으로 작성한 타입별 목록을 노출하지 않는다. DB 조회·쓰기·트랜잭션은 추가하지 않는다.
5. 서버는 저장된 위치 목록에서 선택 타입의 `page*10`부터 최대 10건을 자른다. `ALL`은 `allItems`를 사용한다. 화면의 기존 클라이언트 누적 목록·타입 필터·페이지별 추가 수집을 제거하고 탭과 page만 API에 보낸다.

## 구현 순서

1. SDK 제거 ADR 다음 번호의 새 ADR에 원본 목록 계약과 유형별 인덱스 계약의 관계, 분류 실패 시 불완전 상태, 캐시 TTL/동시성 정책을 기록한다.
2. 선행 마일스톤에서 구현한 Spotify HTTP provider의 Artist Albums 응답을 인덱스 빌더에 연결한다. `album_type`, `total_tracks`, 원본 순서/total의 매핑을 검증하고 필요한 provider-neutral 모델만 이 Plan에서 확장한다.
3. Application의 인덱스 빌더와 read service, Redis store, 단일 빌드 조율을 구현한다. 외부 HTTP 동안 DB transaction을 열지 않는다.
4. Controller/API interface/DTO에 선택적 `releaseType`, 상태 응답, 타입별 page/count를 추가한다. Swagger, Server Spec, 클라이언트 api-contract를 실제 동작에 맞게 갱신한다.
5. React Artist 화면을 서버 타입별 페이지 호출로 전환하고 탭별 현재 page를 유지한다. Album 상세·Track 목록과 다른 Catalog API는 유지한다.

## Acceptance Criteria / 검증

- Spotify 원본 페이지에 유형이 `1/2/7`개로 섞여 있어도 우리 API 첫 페이지는 선택 유형 최대 10개를 채워 반환하며, 다음 page는 중복·누락 없이 이어진다. 10개 미만이면 해당 개수와 정확한 종료 상태를 반환한다.
- 6/7곡, 30분 미만/정확히 30분, single, compilation, 후속 Track 페이지, 불완전한 metadata, 개별 Track 오류를 검증한다. `single`/`compilation` 및 `album` 6곡 이하는 추가 Track 호출이 0회다.
- 완성된 캐시에서 네 탭 전환과 page 조회는 Spotify HTTP 0회, DB SELECT 0회다. 동시 첫 요청은 한 빌드로 합쳐지고 스냅샷은 부분 게시되지 않는다.
- 구축 중 202→완료 200, 목록 조회 실패 재시도, API의 불완전 분류 상태(`classificationComplete`, `unclassifiedCount`) 및 기존 `releaseType` 생략 계약을 테스트한다. 화면은 실제 API를 타입별로 호출하며 클라이언트 전체 페이지 스캔을 하지 않는다.
- 변경 diff와 Swagger/API 계약을 리뷰한다. Codex는 빌드·테스트를 실행하지 않고 사용자에게 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh` 및 클라이언트 테스트 명령을 안내한다. 사용자 결과 확인 후에만 Plan을 completed로 옮긴다.

## 예상 비용과 범위 밖

- 최초 인덱스 구축은 아티스트의 모든 Spotify 발매 페이지를 읽으므로 큰 디스코그래피에서는 오래 걸릴 수 있다. HTTP 202로 요청 타임아웃을 피하고 이후 탭 전환 비용을 캐시 조회로 제한한다.
- Spotify `include_groups=compilation`은 Artist 관계 그룹 필터이며 `album_type=compilation`과 동일하다고 가정하지 않는다. 정확성이 확인되기 전에는 인덱스 구축에서 사용하지 않는다.
- Spotify SDK 제거 자체와 Spotify HTTP 클라이언트 전면 마이그레이션은 선행 마일스톤의 범위다. Catalog DB 저장, System Tag/Import 변경, Album 상세 분류 규칙 변경은 이 마일스톤의 범위 밖이다.

## Plan 조정 기록

- 2026-10-30: 사용자가 SDK 제거를 선행 마일스톤으로 정하고 Plan 작성을 요청했다. ADR 번호 충돌을 피하려고 이 Plan의 고정 번호 `023`만 "SDK 제거 ADR 다음 번호"로 변경했다. 구현 범위·API 계약·수용 기준은 그대로다.
- 2026-11-02: ADR-024를 작성하고 서버 인덱스·Redis 저장·HTTP 202 계약·React Artist 화면의 유형별 페이지 호출을 구현했다. 테스트와 정적 검토를 추가했으며 사용자 실행 검증 결과를 기다린다. Plan은 active로 유지한다.
- 2026-11-02: 사용자 요청으로 Artist 화면의 `incompleteClassification` 문구와 번역 키를 제거했다. 불완전 분류 상태는 API 응답에 유지한다. 이후 클라이언트 소스는 사용자 지시에 따라 수정하지 않는다(`agents/server/conventions.md`).
- 2026-11-02: 사용자 리뷰에 따라 원본/인덱스 선택을 Application Service로 옮기고 Controller의 private 검증 메서드를 제거했다. 추가했던 `CatalogArtistAlbumsEndpoint`는 불필요한 Presentation 계층 분리여서 제거했다. Controller에는 요청 검증 위임과 HTTP 202 응답 변환만 남겼다. API 계약은 유지하고 회귀 테스트를 보강했다. 사용자 실행 검증은 대기 중이다.
- 2026-11-02: 사용자 지시로 Java `var` 사용 금지를 `agents/server/conventions.md`에 추가하고 서버 운영·테스트 Java 소스의 기존 선언을 명시적 타입으로 교체했다. BUILDING일 때의 HTTP 202·`Retry-After` 변환은 Presentation 응답 DTO의 `toHttpResponse`에 두어 Controller의 조건문을 제거했다. 사용자 실행 검증은 대기 중이다.
- 2026-11-02: 사용자가 로컬 8080의 `releaseType=COMPILATION` 요청에서 원본 10건·Spotify 전체 건수 60건, 분류 상태 필드가 없는 응답을 전달했다. 이는 현재 인덱스 API 계약과 일치하지 않는다. 현 작업 환경에서는 8080 연결이 거부되어 실행 프로세스를 확인할 수 없었다. COMPILATION 필터 결과에 해당 유형만 포함되는 서비스 테스트 단언을 보강했고, 실행 서버의 코드 버전·재시작 및 사용자 실행 검증 확인을 기다린다.
- 2026-11-02: 사용자가 최신 인덱스 구축 중 `ArtistAlbumIndexService.build`의 페이지 일관성 검사(92행)에서 `SPOTIFY_EXCEPTION`이 발생한다고 전달했다. 스택만으로 실제 페이지 크기 불일치와 이전 페이지 반복을 구별할 수 없어, 각각 artist ID·page·total·수집 건수·예상/실제 건수를 기록하는 로그를 추가했다. 불완전한 스냅샷 비게시 정책은 유지하며 사용자 실행 로그를 기다린다.
- 2026-11-02: 사용자가 Spotify `total=60`, 두 번째 offset 창에서 5건이 반환된 로그를 전달했다. 기존 '비마지막 창은 항상 10건' 가정이 실제 응답과 충돌했다. ADR-025에 따라 수집 건수가 아니라 offset으로 전체 창을 탐색하고, 짧은 창과 동일한 내용의 창도 원본 순서대로 보존하도록 수정했다. 변경된 목록 건수 의미를 Server Spec·Swagger·클라이언트 API 계약에 반영하고 회귀 테스트를 추가했다. 사용자 실행 검증은 대기 중이다.
