# ADR-030 — 승인된 장르 어휘와 Track 직접 장르 우선 표시

> 2026-11-10 도메인 정정: 장르·스타일·씬·국가·시대 등 외부 근거로 계산하는 정보는 모두 System Tag다. 이 ADR은 그중 장르의 분류·Track/Album 표시 규칙만 결정한다. 별도 `genreTags` 모델을 요구하지 않는다. 명시적 provider 근거의 초기 타입은 ADR-033을 따른다.

## Decision

System Tag는 외부 근거로 계산한 장르·스타일·씬·국가·시대 등 구조화 태그를 포괄한다. 그중 장르 표시는 `GENRE`와 장르로 승인된 `STYLE` 타입의 System Tag에 한정한다. 기존 이름은 유일한 `APPROVED` alias 또는 `ACTIVE` canonical name에 정확히 매칭하고, 신규 이름의 초기 타입은 ADR-033의 명시적 provider 근거 규칙을 따른다. 장르·세부 장르와 분위기·시대·국가·씬·기타 표현은 구분한다. `UNCLASSIFIED`도 System Tag일 수 있지만 장르를 뜻하지 않는다. confidence는 특정 음악과 태그의 연관 강도이며 타입 분류의 근거가 아니다.

MusicBrainz·Discogs의 미매칭 명시적 genre/style은 ADR-033에 따라 각각 `GENRE`/`STYLE` System Tag로 생성될 수 있다. Last.fm의 미매칭 community tag는 ADR-027에 따라 `UNCLASSIFIED` System Tag로 생성된다. 원문은 `ExternalTagObservation`에도 남겨 후속 어휘 매핑·검토에 사용할 수 있게 한다. `2025`, `3`, `beautiful` 같은 Last.fm 이름은 별도 분류 근거가 없으면 장르로 표시하지 않는다. Last.fm의 상위 5개 수집, `count` 미사용 및 provider별 confidence 정책은 유지한다.

장르로 해석된 근거에는 기존 Resolver의 confidence, 최소 점수, 동일 태그의 최대 점수, Album 상속 가중치를 적용한다. Track에 표시 가능한 직접 장르가 하나라도 있으면 장르 화면에는 그 Track의 직접 장르를 우선 표시한다. 직접 장르가 없으면 Album에서 계산된 장르를 참고값으로 표시하고 출처를 `앨범 장르 기반`이라고 명시한다. 서로 다른 Track은 같은 Album 안에서도 서로 다른 직접 장르를 가질 수 있다. 기존 `systemTags` 안에서 장르 타입과 근거 출처를 어떻게 표현할지는 구현 Plan에서 명확히 정한다.

Track별 사용자 장르 제안·투표·관리 검토 기능은 이번 결정의 범위에 포함하지 않는다. 외부 Track 직접 근거의 추가 후보는 별도 표본 검증 후 결정한다.

이 결정은 ADR-010/027의 미매칭 System Tag 자동 생성 자체를 폐기하지 않는다. MusicBrainz/Discogs 신규 타입은 ADR-033이 ADR-010의 `UNCLASSIFIED` 정책을 대체하고, Last.fm `UNCLASSIFIED`는 ADR-027을 따른다. ADR-004/006의 원본 Last.fm Observation 정책과 ADR-029의 Album/provider 근거 재사용 정책은 유지한다.

## Reason

가중치는 장르가 특정 Track 또는 Album에 얼마나 강하게 연결되는지를 표현한다. `2025`, `3`, `beautiful`과 같은 이름이 장르인지 판별하지는 못한다. 현재는 Last.fm Track의 기본 confidence `0.65`가 Resolver 최소 점수 `0.50`을 넘으므로, 미분류 이름을 자동 승인하면 가중치만으로 비장르 표시를 막을 수 없다.

현재 Resolver의 Album 상속은 태그별로 결합된다. Track 직접 장르와 다른 Album 장르가 각각 점수를 통과하면 둘 다 남을 수 있다. Track 고유 장르를 우선하고 직접 근거가 없는 곡에만 Album 장르를 참고값으로 보여주려면 장르 표시에서 출처를 구분해야 한다.

## Consequence

- 명시적 provider 근거로 신규 장르·스타일 이름을 초기화하고, 기존 `UNCLASSIFIED` System Tag의 타입 분류 기준은 별도 작은 마일스톤에서 정하고 검증한다. 기존 데이터를 이름만 보고 일괄 장르로 승격하지 않는다.
- `TagBootstrapService`의 자동 생성 정책, 장르 표시용 조회·projection 및 출처 표시는 새 Plan과 테스트가 필요하다. 공개 API가 바뀌면 Swagger와 클라이언트 계약 문서도 함께 갱신한다.
- 기존 `subject_tag_resolved`와 완료 marker가 새 정책을 자동 반영한다고 가정하지 않는다. 재계산 범위와 데이터 이관이 확정되면 해당 마일스톤의 Oracle/H2 migration을 작성한다.
- 당시 active였던 `ENRICHMENT-DIAGNOSTICS-001` Plan의 Last.fm 자동 생성은 유지된다. 장르 타입 분류와 Track 직접/Album 참고 표시 정책은 후속 Plan에서 별도로 구현·검증한다.
