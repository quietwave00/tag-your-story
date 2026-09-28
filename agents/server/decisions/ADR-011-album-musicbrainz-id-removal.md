# ADR-011 — Remove Album MusicBrainz ID

## Decision

`album.musicbrainz_id`와 해당 index를 제거한다. Catalog Album은 Spotify ID로 식별한다. MusicBrainz Release Group ID는 Album Catalog identity로 저장하거나 재사용하지 않는다.

최초 Track enrichment에서 Recording에 연결된 Release Group이 기존 보수적 매칭을 통과하면 해당 Release Group genre를 `ALBUM`의 external observation으로 수집하는 기능은 유지한다. 출처 식별자는 observation의 `external_ref`에 남긴다. Track의 Recording MBID 저장은 유지한다.

Album 태그 표시에는 `subject_type=ALBUM`, 해당 `album_id`의 visible `subject_tag_resolved` projection을 사용한다. `APPROVED` assertion은 Resolver 입력이며, raw assertion 목록을 그대로 확정 태그로 노출하지 않는다. 새 Album 전용 공개 조회 API는 이 결정에 포함하지 않는다.

이 결정은 기존 서버 명세·System Tag 아키텍처·완료된 `CATALOG-001`/`ENRICHMENT-001` 및 active `CATALOG-IMPORT-002`의 Album Release Group MBID 저장·재사용 정책을 대체한다.

## Reason

현재 Album 상세 정보는 Spotify ID로 조회하고, System Tag는 Album subject의 resolved projection에서 읽는다. Release Group ID 컬럼은 이 조회에 필요하지 않다. 기존 MusicBrainz Album matching은 optional이며 결과가 없는 Album에서도 다른 provider의 Album evidence와 Resolver는 동작한다. 별도 Album MBID cache를 유지하는 비용보다 현재 Catalog의 단순한 identity 경계를 우선한다.

## Consequence

- Album Entity, import snapshot, identity attach, schema의 컬럼/index와 관련 lock/read를 제거한다.
- MusicBrainz Album genre를 새로 수집할 때는 저장된 Release Group ID를 재사용할 수 없으므로 Recording 연결 후보 조회가 다시 필요할 수 있다. 기존 resolved Track fast path는 enrichment를 재실행하지 않는다.
- 운영 오픈 전이므로 별도 삭제 migration은 작성하지 않는다. 기준 DDL에서 column/index를 제거하고 개발 DB는 필요에 따라 재생성한다. 기존 `ALBUM` observation/assertion/resolved 정책은 변경하지 않는다.
- 기존 Release Group 매칭 자체와 태그 evidence 정책은 변경하지 않는다. Album tag read API는 별도 Plan과 Swagger 계약으로 다룬다.
