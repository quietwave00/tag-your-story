# ADR-004 — Last.fm Community Tag Evidence

## Decision

Last.fm을 MusicBrainz, Discogs와 독립적으로 실행되는 세 번째 External Enrichment provider로 추가한다.

- `ExternalTagSource`와 `AssertionSource`에 `LASTFM`을 추가한다.
- Last.fm `track.getTopTags`와 `album.getTopTags`의 값은 provider가 명시적으로 분류한 genre/style이 아니므로 `EvidenceType.COMMUNITY_TAG`로 기록한다.
- Last.fm count는 Human Review로 승인된 최소 품질 기준을 통과시키는 gate로만 사용하고 confidence에 곱하거나 provider agreement bonus로 사용하지 않는다.
- Track/Album entity는 MusicBrainz 성공에 의존하지 않고 Spotify의 title과 대표 Artist를 기준으로 exact match한다. 이미 저장된 Recording MBID는 Track 요청에서만 우선 재사용하며, Release Group MBID를 Last.fm Album MBID로 간주하지 않는다.
- Last.fm이 안정적인 Track/Album ID를 제공하지 않는 경로의 `external_ref`는 subject type과 정규화된 Artist/title로부터 만든 결정적 SHA-256 reference를 사용한다.
- 기존 Resolver의 `max(confidence)`, minimum score, direct-over-inherited 정책은 유지한다. 세 provider의 일치 개수에 따른 가산점이나 다수결은 이 마일스톤에 포함하지 않는다.

## Reason

Last.fm은 공식 API로 Track과 Album의 community top tag 및 count를 제공하므로 MusicBrainz genre와 Discogs genre/style을 보완할 수 있다. 반면 Last.fm tag에는 genre뿐 아니라 mood, 청취 상태, 개인 분류가 섞일 수 있다. 이를 `EXPLICIT_GENRE` 또는 `EXPLICIT_STYLE`로 저장하면 evidence provenance가 실제 provider 의미와 달라지고, 내부 taxonomy의 approved alias와 일치했다는 사실을 provider의 명시적 분류로 오해하게 된다.

Last.fm count를 confidence에 직접 반영하거나 provider 일치 수를 합산하면 기존 Resolver 의미와 유명 entity 편향이 함께 변경된다. 이번 마일스톤은 source 다양성과 raw evidence 보존을 늘리는 데 한정하고, corroboration 계산은 실제 수집 데이터로 효과를 검증한 뒤 별도 결정으로 다룬다.

## Consequence

- `EvidenceType`과 provider source enum의 값이 추가되지만 enum은 문자열 컬럼에 저장되므로 신규 테이블이나 컬럼은 만들지 않는다.
- Last.fm community tag는 approved alias exact unique match일 때만 Assertion으로 승격하고, 나머지는 NEW Observation으로 보존한다.
- Last.fm Track evidence는 direct assertion으로, Album evidence는 기존 Album resolve 및 Track inheritance 경로로 처리한다.
- Last.fm API key, provider timeout, 최소 count, 최대 수집 tag 수와 Track/Album base confidence 설정이 추가된다.
- provider top-level 병렬 실행 수에 맞춰 전용 executor 기본 크기를 3으로 변경한다.
- Last.fm API의 상업적 또는 연구 목적 사용에는 provider 문의가 필요할 수 있으므로 배포 전 적용 가능한 이용 조건을 확인한다.
- 향후 provider agreement bonus, count 정규화 또는 새로운 Resolver 수식이 필요하면 별도 ADR과 마일스톤으로 결정한다.
