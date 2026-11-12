# ADR-027 — Last.fm community tag의 System Tag 자동 생성

## Decision

정확한 Track/Album identity 검증을 통과해 수집된 Last.fm `COMMUNITY_TAG`도, 기존 approved alias와 ACTIVE canonical Tag에 매칭되지 않으면 새 System Tag를 생성할 수 있다. ADR-006의 subject별 상위 5개, 빈 이름·정규화 중복 제외, `count` 미사용 정책은 유지한다.

새 Tag는 다른 provider의 자동 생성 Tag와 동일하게 원래 태그 이름, `ACTIVE` 상태, 내부 `TagType.UNCLASSIFIED`로 저장한다. `UNCLASSIFIED`는 화면에 추가하는 태그 이름이 아니며 Last.fm 문자열을 장르나 스타일로 추측하지 않기 위한 내부 분류다. Last.fm 출처와 `COMMUNITY_TAG` evidence type을 보존하고 해당 Track/Album에 `APPROVED` Assertion을 만든다. 기존 Last.fm Track `0.65`, Album `0.60` confidence, Resolver 최소 점수 `0.50`, Album → Track 상속 가중치 `0.85`는 변경하지 않는다.

ADR-010의 alias 우선, ambiguous alias 차단, 비활성 canonical 이름 충돌 차단, normalized name 전역 unique 및 동시 생성 충돌 처리 규칙을 그대로 적용한다. 이 결정은 ADR-010의 “미매칭 Last.fm 태그는 단독으로 Tag를 생성하지 않는다”와 ADR-004/006의 alias-only Assertion 제한을 대체한다. MusicBrainz/Discogs와 User Tag 정책은 유지한다.

## Reason

Last.fm만 응답한 음악에서도 유효한 상위 community tag를 System Tag로 제공하려는 제품 목표를 반영한다. 기존 정책은 Last.fm의 비장르·개인 분류가 섞일 수 있다는 이유로 미매칭 이름의 자동 생성을 막았다. 이 위험은 태그 종류를 장르로 단정하지 않고 `COMMUNITY_TAG` 출처와 기존의 상대적으로 낮은 confidence를 유지하는 방식으로 표현한다.

신뢰도는 특정 음악에 태그가 붙는 근거의 점수이고 `TagType`은 Tag 자체의 내부 분류다. 기존 모델에서 `TagType`은 필수이므로 confidence를 낮추는 것으로 분류값을 대체할 수 없다. Album `0.60` 근거는 Track 상속 시 `0.51`이 되어 현재 `0.50` 기준을 통과한다. confidence를 더 낮추면 이 상속 태그가 사라질 수 있다.

## Consequence

- Last.fm만 반환한 이름도 새 System Tag와 Assertion을 만들 수 있다. 태그 이름은 Last.fm 원문을 사용하며 내부 분류명은 표시 태그로 추가하지 않는다.
- 응답이 빈 Track/Album에는 이 변경만으로 태그가 생기지 않는다. 동일 normalized name의 중복 Tag는 만들지 않는다.
- 기존 `NEW` Observation과 `EMPTY_COMPLETED` subject의 일괄 재처리는 이번 변경 범위에 포함하지 않는다. 완료 marker를 유지하므로 기존 빈 결과는 자동으로 갱신되지 않고, 새 정책은 이후 실제 수집·계산이 수행되는 subject에 적용된다.
- 정책의 효과와 새 태그의 품질을 검증할 때 신규 Tag 수, Last.fm 단독 생성 수, direct/Album 상속 최종 태그 수를 분리해 측정한다.
- 2026-11-06 사용자 요청으로 active `ENRICHMENT-DIAGNOSTICS-001`의 고도화 범위에 포함됐다. 구현과 사용자 실행 검증은 아직 남아 있다.
