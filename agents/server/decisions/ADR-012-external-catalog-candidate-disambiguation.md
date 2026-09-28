# ADR-012 — External Catalog Candidate Disambiguation

## Decision

MusicBrainz ISRC lookup이 같은 title과 Artist를 가진 복수 Recording을 반환하고 복수 후보가 모두 duration 허용 오차를 통과하면, Spotify duration과의 절대 차이가 가장 작은 단일 후보를 선택한다. 최소 차이가 동률이면 확정하지 않는다. ISRC가 없는 metadata 검색은 기존처럼 전체 조건을 통과한 후보가 정확히 하나일 때만 확정한다.

Discogs Album 검색은 다음 순서의 명시적 search attempt를 사용한다.

1. canonical Album title을 `release_title`로 검색
2. trailing 괄호가 slash로 구분된 Album 목록인 경우 괄호 앞 base title을 `release_title`로 검색
3. Track title과 Track Artist를 `track`/`artist`로 검색하고 앞 단계에서 만든 가장 구체적인 Album identity로 후보를 검증

각 attempt는 `MASTER`를 먼저 조회하고 일치 후보가 없을 때 `RELEASE`를 조회한다. 어느 단계에서든 복수 후보가 남으면 이후 fallback으로 임의 선택하지 않고 `NOT_FOUND`로 종료한다. Detail 응답도 해당 attempt가 사용한 Album identity와 Artist로 다시 검증한다.

Discogs는 전용 ISRC 검색 조건을 제공하지 않으므로 ISRC를 Discogs identity lookup에 사용하지 않는다.

## Reason

MusicBrainz ISRC `GBAYE9200070`은 Radiohead의 `Creep` Recording을 두 개 반환한다. 둘 다 title, Artist, 기존 3초 duration tolerance를 통과하지만 Spotify duration과의 차이는 서로 다르다. ISRC와 metadata가 모두 일치하는 후보 중 유일한 최소 duration 차이는 기존 오차 범위를 유지하면서 결정적으로 후보를 좁힐 수 있다.

Spotify의 box set title은 포함 Album 목록을 괄호 안에 덧붙일 수 있지만 Discogs는 같은 발매를 짧은 title로 저장할 수 있다. 예를 들어 Spotify의 `5 Album Set (Pablo Honey/The Bends/OK Computer/Kid A/Amnesiac)`은 Discogs에서 `5 Album Set`이다. 원문 exact 검색만 사용하면 존재하는 Release를 찾지 못한다.

모든 괄호 suffix를 제거하면 live, remix 또는 실제 title 일부를 손실할 수 있다. slash로 구분된 복수 항목이라는 제한된 형태만 별도 fallback으로 취급하고, Artist와 year preference 및 unique 조건을 유지한다.

## Consequence

- 같은 ISRC의 복수 Recording도 유일한 최소 duration 차이가 있으면 Recording identity와 tag를 수집할 수 있다.
- duration 정보가 없거나 최소 차이가 동률인 복수 후보는 계속 확정하지 않는다.
- Discogs 호출 수는 첫 검색 성공 시 유지되며 fallback이 필요한 경우에만 늘어난다.
- Discogs 검색 순서는 별도 search plan 모델로 표현되어 provider의 제어 흐름과 HTTP query 구성을 분리한다.
- Track title fallback은 후보 탐색 범위를 넓히지만 Album identity, Artist, year preference와 unique 조건을 통과한 경우에만 채택한다.
