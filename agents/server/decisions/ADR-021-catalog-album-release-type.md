# ADR-021 — Catalog Album Release Type

## Decision

Catalog Album 상세 읽기 응답에 `ALBUM`, `COMPILATION`, `SINGLE_EP` 발매 분류를 추가한다. Spotify `album_type=compilation`을 우선 `COMPILATION`으로 보존한다. `album_type=single`은 `SINGLE_EP`로 묶는다. `album_type=album`은 전체 Track 수가 6 이하이거나 총 재생 시간이 30분 미만이면 `SINGLE_EP`, 그 외 `ALBUM`으로 분류한다. Spotify metadata가 부족하거나 분류를 위한 추가 Track 페이지가 실패하면 분류를 추측하지 않고 null로 반환하며, 이미 조회한 상세 정보는 제공한다.

## Reason

Spotify 공개 Web API의 `album_type`은 `album`, `single`, `compilation`만 제공한다. 사용자는 Spotify 화면의 싱글/EP 묶음을 하나의 유형으로 원하며, 6곡 이하 또는 30분 미만이라는 분류 기준을 제공했다. 이 기준을 상세 읽기 응답에만 적용하면 Catalog identity나 System Tag 정책을 바꾸지 않고 표시 요구를 충족한다.

## Consequence

- Album 상세 응답에 nullable `releaseType`이 추가된다.
- 7곡 이상인 `album_type=album`은 전체 재생 시간 확인을 위해 Spotify Track 페이지를 추가 조회할 수 있다.
- Catalog 검색·import·저장과 Album 태그 상속에는 유형별 제한을 추가하지 않는다. ADR-005의 태그 정책을 유지한다.
