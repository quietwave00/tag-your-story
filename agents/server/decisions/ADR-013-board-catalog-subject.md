# ADR-013 — Board Catalog Track/Album Subject

## Decision

Board는 Catalog `TRACK` 또는 `ALBUM` 중 정확히 하나를 대상으로 작성한다.

- 공개 Board 생성 요청은 기존 `trackId` 또는 신규 `albumId` 중 정확히 하나를 받는다.
- 두 공개 ID는 통합 Catalog 검색/import와 동일하게 Spotify ID 의미를 가진다.
- Application 계층은 대상 type에 따라 기존 `TrackSelectionService` 또는 `AlbumSelectionService`로 Catalog subject를 확보한 뒤 Board write를 시작한다.
- `board.track_id`와 `board.album_id`는 각각 nullable FK이며, domain factory와 DB CHECK constraint가 정확히 하나만 non-null임을 보장한다.
- Board 수정에서 subject type이나 대상 Track/Album은 변경할 수 없다.
- Board의 System Tag는 대상에 따라 `(TRACK, track_id)` 또는 `(ALBUM, album_id)`의 visible `subject_tag_resolved` projection에서 조회한다.
- Track/Album FK를 Board Entity에 단방향 LAZY 관계로 두며 Catalog Entity에는 Board 역방향 컬렉션을 추가하지 않는다.
- Artist와 그 밖의 polymorphic subject는 Board 대상에 포함하지 않는다.

## Reason

통합 Catalog 검색과 선택 흐름은 이미 Track과 Album을 독립된 subject로 지원하고 두 대상의 resolved System Tag를 계산한다. Board를 Track에만 제한하면 사용자가 Album 전체에 대한 글과 개인 UserTag를 남길 수 없고, Album resolved tag도 커뮤니티 조회에 연결되지 않는다.

단일 `subject_type + subject_id` 컬럼은 두 Catalog 테이블에 대한 실제 FK를 만들 수 없다. Track/Album 두 nullable FK와 exactly-one CHECK를 사용하면 현재 필요한 두 대상만 명확하게 표현하면서 DB가 orphan과 양쪽 동시 연결을 방어할 수 있다.

공개 ID를 내부 PK로 즉시 변경하면 기존 Track 검색과 클라이언트 contract가 깨진다. 공개 경계에서는 Spotify ID를 유지하고 내부 transaction에는 Catalog PK만 전달하여 외부 identity와 DB 관계를 분리한다.

## Consequence

- `board.track_id`는 기존 Spotify 문자열에서 nullable Catalog Track FK로 migration하고 `board.album_id` FK를 추가한다.
- 기존 Board는 모두 Track Board이므로 `track.spotify_id`로 내부 FK를 채운 뒤 exactly-one constraint를 적용한다. 매칭되지 않는 legacy row가 있으면 migration을 중단한다.
- Board 생성 전 Track/Album selection 및 enrichment가 실행될 수 있지만 Board write transaction은 selection 완료 후에만 시작한다.
- Track Board 목록/count 기존 API는 유지하고 Album Board 목록/count API를 가산적으로 추가한다.
- Board response는 `subjectType`, `trackId`, `albumId`와 대상 subject의 visible System Tag를 제공한다. 대상이 아닌 ID는 null이다.
- UserTag의 `(user_id, name)` exact-name identity와 Board writer owner 불변식은 Track/Album Board에 동일하게 적용한다.
- UserTag 이름 기준 공개 Board 조회는 Track과 Album Board를 함께 반환할 수 있으므로 System Tag bulk lookup key에 subject type을 포함한다.
- Album → Track 상속 계산 정책은 변경하지 않는다. Album Board는 ALBUM resolved 결과를 직접 조회한다.
