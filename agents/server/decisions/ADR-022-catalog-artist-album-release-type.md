# ADR-022 — Artist Album 목록 발매 유형

## Decision

ADR-021의 발매 분류를 Artist Album 목록 항목에도 적용한다. `CatalogReleaseType.classify`를 단일 분류 규칙으로 사용하며, 각 항목의 nullable `releaseType`은 `ALBUM`, `COMPILATION`, `SINGLE_EP` 중 하나다. Spotify `album_type=single`과 `compilation`은 목록 메타데이터로 분류한다. `album_type=album`은 Spotify Album Track 목록에서 전체 곡 수와 재생 시간을 확인한다. 분류에 필요한 Track 조회가 실패하거나 metadata가 불완전하면 해당 항목의 `releaseType`만 null로 반환한다.

## Reason

Artist Album 목록에도 Album 상세와 같은 발매 분류가 필요하다. 현재 사용하는 Spotify Java SDK 8.0.0의 `AlbumSimplified`에는 총 곡 수 접근자가 없으므로 `album` 유형의 곡 수 확인에는 Spotify Album Track 목록 조회가 필요하다. 기존 분류 함수를 재사용해 두 API의 기준을 일치시킨다.

## Consequence

- `GET /api/catalog/artists/{spotifyArtistId}/albums`의 item에 nullable `releaseType`이 추가된다.
- `album_type=album` 항목마다 Spotify Album Track 조회가 추가될 수 있다. 30분 이상이 확인되면 후속 페이지 조회를 중단한다.
- 이 읽기 API는 DB 조회, 저장 및 DB transaction을 추가하지 않는다. 목록 순서, 전체 건수와 나머지 필드는 유지한다.
