# ADR-033 — 명시적 provider 분류로 System Tag 타입 초기화

## Decision

초기 장르 사전을 선행 조건으로 두지 않는다. 정확한 음악 identity 검증을 통과한 외부 근거에서 새 System Tag를 자동 생성할 때 MusicBrainz `EXPLICIT_GENRE`와 Discogs `EXPLICIT_GENRE`는 `TagType.GENRE`, Discogs `EXPLICIT_STYLE`은 `TagType.STYLE`로 초기화한다. Last.fm `COMMUNITY_TAG`는 장르·스타일 여부를 추측하지 않고 `TagType.UNCLASSIFIED`로 생성한다. 원래 `evidence_type`, provider, raw name, confidence는 Observation/Assertion에 보존한다.

기존 `APPROVED` alias와 `ACTIVE` canonical name의 유일 매칭을 먼저 적용하고, 기존 Tag의 타입을 새 provider 응답만으로 덮어쓰지 않는다. 동일 이름에 명시적 genre와 style이 함께 들어오거나 기존 타입과 충돌하면 타입을 자동 변경하지 않고 충돌 근거를 검토 대상으로 남긴다. 이름 형태만으로 `2025`, `3`, `beautiful`을 장르로 확정하지 않는다. Last.fm 원문이 이미 타입이 확인된 canonical Tag 또는 승인된 alias에 매칭되면 그 기존 타입을 사용한다.

이 결정은 ADR-010의 MusicBrainz/Discogs 신규 Tag를 모두 `UNCLASSIFIED`로 생성하는 부분을 대체한다. ADR-027의 Last.fm 신규 `UNCLASSIFIED` System Tag 생성과 기존 alias 우선·동시성·confidence 정책은 유지한다. ADR-030의 장르 타입 분류와 근거 가중치 분리는 이 초기 타입 규칙 위에서 적용한다. 기존 `UNCLASSIFIED` Tag를 일괄 변경하지 않으며, 저장된 Observation의 검증 가능한 evidence metadata와 타입 충돌을 조사한 뒤 별도 migration에서 다룬다.

## Reason

승인된 이름·alias만으로 타입을 확정하면 비어 있는 taxonomy에서는 모든 신규 태그가 `UNCLASSIFIED`로 남는다. MusicBrainz와 Discogs가 명시적으로 반환한 genre/style 구분은 초기 타입의 근거가 될 수 있다. Last.fm community tag에는 장르 외 표현이 섞이므로 같은 자동 타입 부여를 적용하지 않는다.

System Tag의 존재와 System Tag의 분류는 별개의 문제다. provider별 명시적 근거로 초기 타입을 부여하면 큰 사전 없이 시작하면서도, 출처가 제공하지 않은 부모·자식 관계나 분위기·시대 분류를 이름에서 추측하지 않을 수 있다.

## Consequence

- `TagBootstrapService`의 신규 Tag 타입 결정과 기존 Tag 충돌 처리, provider별 회귀 테스트가 필요하다. Last.fm이 먼저 `UNCLASSIFIED`를 만든 뒤 명시적 genre/style이 도착하는 경우와 genre/style이 서로 충돌하는 경우에는 기존 타입의 자동 승격·보류 기준을 구현 Plan에서 확정해야 한다. 이 기준 없이 입력 순서에 따라 최종 타입이 달라지는 구현은 허용하지 않는다.
- 새 타입 정책은 기존 Tag·Observation·Assertion·Resolved에 자동 소급 적용되지 않는다. 기존 `UNCLASSIFIED` 재분류 범위와 Oracle/H2 데이터 migration은 별도 작은 마일스톤에서 설계·검증한다.
- 이 ADR은 Tag 노드를 만드는 규칙이다. `tag_parent` 관계의 생성·승인은 결정하지 않는다. 외부 태그의 동시 출현만으로 파생 장르를 확정하지 않는다.
