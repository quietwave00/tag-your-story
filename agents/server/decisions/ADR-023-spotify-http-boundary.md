# ADR-023 — Spotify HTTP 경계와 Artist Album 곡 수

## Decision

- Spotify Java SDK 호출을 서버 소유 `SpotifyHttpClient`의 Client Credentials 및 Spotify Web API HTTP 호출로 대체한다. JSON DTO는 Infrastructure에만 두고 Application port에는 기존 provider-neutral 모델을 유지한다.
- 인증 URL과 API URL은 별도 설정으로 둔다. 연결 timeout은 2초, 읽기 timeout은 5초다. `spotifyAccessToken:` Redis 키를 유지하고 TTL은 `min(30분, expires_in - 60초)`로 한다.
- 401은 캐시된 토큰이 해당 요청에서 사용한 값일 때만 무효화한 뒤 한 번 재발급·재시도한다. 같은 프로세스의 토큰 발급은 lock으로 직렬화한다. 429는 `Retry-After`가 0~2초일 때 최대 두 번만 대기·재시도한다. 그 밖의 HTTP·네트워크 실패는 기존 `SPOTIFY_EXCEPTION`으로 변환한다.
- Artist Album 목록의 원본 `album_type`과 `total_tracks`를 DTO에 담는다. `album` 6곡 이하는 Track HTTP 조회 없이 `CatalogReleaseType.classify`로 분류한다. 7곡 이상은 ADR-021/022의 재생 시간 기준과 부분 실패 정책을 유지한다.
- Track 상세 서비스의 외부 HTTP 호출에서 read-only DB transaction을 제거하고 provider port로 분리한다.

## Reason

SDK의 `AlbumSimplified`에는 `total_tracks` 접근자가 없어 6곡 이하 Album에도 Track 목록 호출이 필요했다. Spotify Web API 응답에는 해당 필드가 있어 이 호출을 생략할 수 있다. 인증과 오류 정책을 한 HTTP 경계에 모으면 검색·상세·import의 실패 계약과 제한된 재시도를 일관되게 유지할 수 있다.

## Consequence

- ADR-022의 SDK 제약에 따른 `album` 항목별 Track 호출 근거를 이 결정으로 대체한다. 분류 규칙과 nullable `releaseType`은 그대로다.
- 공개 API 및 Swagger 계약은 변경하지 않는다. 기존 화면 검색 window cache와 검색 호출 수·정렬 입력도 유지한다.
- Redis 토큰 키는 이전 값과 호환된다. 재시도와 외부 HTTP의 최대 대기 시간은 늘 수 있으나 정해진 상한을 넘지 않는다.
- 사용자 실행 테스트·검증 결과를 받은 뒤 Plan 완료 여부를 판단한다.
