# ADR-007 — Last.fm Metadata Track Lookup

## Decision

ADR-004의 Last.fm Track 요청에서 저장된 MusicBrainz Recording MBID를 우선 재사용하는 결정을 대체한다.

- Last.fm `track.getTopTags`는 MusicBrainz Recording MBID를 전달하지 않고 Spotify catalog의 대표 Artist명과 Track title로 조회한다.
- `autocorrect=0`을 유지하고, 응답 Artist/Track identity가 요청 metadata와 normalized exact match인 경우에만 evidence를 채택한다.
- 원문 Track identity가 exact match이지만 top tag가 비어 있고, 승인된 trailing edition qualifier를 제거한 canonical title이 원문과 다를 때만 canonical title로 한 번 fallback 조회한다.
- canonical fallback은 remaster/remastered, deluxe/expanded/anniversary edition 등 기존 Discogs에서 승인된 edition qualifier 규칙을 공유하며 Live/Remix/Acoustic 등 녹음의 의미가 달라지는 표시는 제거하지 않는다.
- MusicBrainz Recording MBID는 MusicBrainz identity 재사용에만 사용하며 Last.fm client port에 전달하지 않는다.

## Reason

MusicBrainz가 채택한 Recording MBID가 Last.fm의 Track resource에 항상 연결되는 것은 아니다. 실제 provider 호출에서 유효한 Recording MBID를 Last.fm `track.getTopTags`에 단독으로 전달했을 때 HTTP 400이 반환되어 Track evidence만 유실되었다.

최초 import에서는 MusicBrainz와 Last.fm이 병렬 실행되므로 새로 발견한 MBID를 Last.fm이 같은 요청에서 사용할 수도 없다. Spotify에서 이미 확보한 Artist/Track metadata로 항상 같은 요청 경로를 사용하고 응답 identity를 별도로 검증하는 것이 최초/재시도 동작을 일관되게 만든다.

Last.fm은 edition suffix가 포함된 Track identity를 반환하면서도 top tag는 빈 배열로 반환할 수 있다. 원문 응답을 우선하고 evidence가 없을 때만 제한된 edition qualifier를 제거하면, 원문 Track의 tag를 보존하면서 canonical Track에 집약된 community tag를 보충할 수 있다.

## Consequence

- Last.fm Track 수집은 MusicBrainz 성공 여부와 Catalog MBID 저장 시점에 영향받지 않는다.
- Last.fm client port에서 Recording MBID 인자를 제거한다.
- exact Track의 top tag가 빈 edition title은 Last.fm Track HTTP 요청을 최대 한 번 추가로 수행한다.
- edition qualifier 규칙은 provider-neutral `MusicEditionTitleNormalizer`로 추출하고 Discogs와 Last.fm이 공유한다.
- 동명 Track 오매칭은 기존 `autocorrect=0` 및 Artist/Track normalized exact response validation으로 거부한다.
- Last.fm Album 요청, community tag 선택, confidence, Observation/Assertion/Resolver 정책은 변경하지 않는다.
