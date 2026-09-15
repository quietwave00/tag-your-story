# ADR-006 — Last.fm Ranked Top Tag Selection

## Decision

ADR-004의 Last.fm minimum count gate 결정을 대체한다.

- Last.fm `track.getTopTags`와 `album.getTopTags`가 반환한 순서를 provider의 연관도 순위로 취급한다.
- Track과 Album 각 subject에서 빈 이름과 정규화 기준 중복을 제외한 상위 5개까지 수집한다.
- 반환된 유효 tag가 5개 미만이면 존재하는 tag만 수집한다.
- Last.fm `count`는 eligibility gate, confidence, Resolver score에 사용하지 않는다.
- 수집된 tag는 기존과 같이 `COMMUNITY_TAG` Observation으로 보존하고 approved alias exact unique match일 때만 Assertion으로 승격한다.

## Reason

Last.fm은 `count`로 top tag를 정렬하지만 고유 사용자 수와의 관계나 정확한 산정식, 품질 임계값을 공개하지 않는다. `count >= 20`은 실제 수집 데이터로 교정하지 않은 휴리스틱이며, tag 수가 적은 마이너 Track/Album의 유효한 evidence를 전부 제거할 수 있다.

인기와 무관하게 provider가 상위로 반환한 소수의 후보를 수집하고, 내부 taxonomy 승격은 기존 approved alias exact unique match에 맡기는 것이 coverage와 noise를 더 단순하게 균형 잡는다.

## Consequence

- Last.fm minimum count 설정과 validation을 제거한다.
- subject별 최대 수집 개수의 기본값을 10에서 5로 낮춘다.
- count가 낮거나 0인 tag도 상위 5개에 포함되면 Observation 후보가 될 수 있다.
- Last.fm evidence의 base confidence, `COMMUNITY_TAG` provenance, exact entity matching, Observation/Assertion/Resolver 정책은 변경하지 않는다.
