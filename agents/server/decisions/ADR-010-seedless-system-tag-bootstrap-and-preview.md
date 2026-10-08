# ADR-010 — Seedless System Tag Bootstrap and Raw Preview

## Decision

최초 Track import는 사전 `tag`/`tag_alias` seed나 정기 배치를 기다리지 않고, 현재 요청에서 수집한 근거로 System Tag를 계산한다.

- 기존 `APPROVED` alias의 유일한 매칭을 우선한다. 동일 normalized name이 서로 다른 Tag의 approved alias에 매칭되면 ambiguous로 취급하며 자동 Tag를 생성하지 않는다.
- alias가 없으면 활성 canonical Tag의 `normalized_name` exact match를 재사용한다. `tag.normalized_name`에는 전역 unique 제약을 적용한다. 기존 비활성 Tag와 이름이 충돌하면 임의로 활성화하거나 다른 Tag를 만들지 않는다.
- 위 두 매칭이 모두 없을 때, 정확한 음악 엔티티 식별을 통과한 MusicBrainz `EXPLICIT_GENRE`와 Discogs `EXPLICIT_GENRE`/`EXPLICIT_STYLE`만 normalized name으로 `ACTIVE` Tag를 자동 생성하고, 해당 Subject의 `APPROVED` Assertion을 즉시 만든다. 자동 Tag의 분류를 추측하지 않기 위해 `TagType.UNCLASSIFIED`를 사용한다.
- 미매칭 Last.fm `COMMUNITY_TAG`는 단독으로 Tag를 자동 생성하지 않는다. 이미 승인된 alias 또는 활성 canonical name과 매칭되면 기존처럼 Assertion으로 사용할 수 있다.
- 현재 Resolver의 `max(confidence)`, minimum score, Album → Track 상속 및 manual/HIDDEN 우선순위는 유지한다. 자동 Tag 생성 여부와 최종 resolved 노출 여부는 별개다.
- `ExternalTagObservation`은 나중의 정확한 재매칭을 위해 `evidence_type`과 수집 당시 `confidence`를 보존한다. 기존 row에 이 값이 없다면 출처만으로 Discogs genre/style을 추측해 승인하지 않는다.
- 공개 import 응답에서 확정 `systemTags`와 검증 전 `previewTags`를 별도 필드로 구분한다. 보이는 resolved tag가 없을 때만 `NEW` Observation을 normalized name 기준으로 중복 제거해 최대 5개 미리보기로 제공한다. 점수 미달의 MATCHED 근거를 raw fallback으로 우회 노출하지 않는다. 미리보기에는 System Tag ID와 score를 부여하지 않으며, `HIDDEN`으로 관리자가 숨긴 Tag의 raw 표현도 우회 노출하지 않는다.
- 현재 MVP는 import 요청에서 동기적으로 계산한다. 결과가 0개였던 성공 처리와 미처리를 구분하는 최소 완료 marker를 두어 반복 선택 때 외부 provider와 Resolver를 불필요하게 재실행하지 않는다. 정기 배치는 최초 결과 생성의 필수 단계가 아니다.
- `tag_parent` 생성과 계층 추론은 import/Resolver 범위 밖이다. 관리자가 별도 taxonomy 경로에서 관계를 승인하는 기존 계획을 유지한다.

이 결정은 ADR-004와 ADR-006의 Last.fm source/rank/count 정책을 유지한다. 다만 두 ADR 및 System Tag 설계 문서의 “모든 외부 이름은 approved alias가 있어야 Assertion으로 승격한다”는 부분은 위의 제한된 MusicBrainz/Discogs 자동 생성 정책으로 대체한다. ADR-002/003의 UserTag identity에는 영향이 없다.

## Reason

초기 taxonomy가 비어 있으면 현재의 approved-alias-only 정책은 유효한 MusicBrainz/Discogs 장르를 수집해도 `tag_assertion`과 `subject_tag_resolved`를 0건으로 만든다. 음악 장르 전체를 사전에 등록하는 것은 현실적이지 않다.

반대로 모든 외부 문자열을 확정 System Tag로 만들면 Last.fm의 비장르·개인 분류까지 승인된다. 이미 검증된 음악 엔티티의 명시적 genre/style만 자동 확정하고, 나머지는 출처가 표시된 raw 미리보기로 제공하면 최초 응답의 공백을 줄이면서 신뢰도를 구분할 수 있다.

`tag_parent`는 태그 간 의미 관계이고, 특정 Track/Album에 태그가 붙는다는 evidence와 독립적이다. 외부 이름의 동시 출현만으로 계층을 추론하지 않는다.

## Consequence

- `tag.normalized_name`과 `UNCLASSIFIED` 타입, Observation evidence metadata, 공개 응답의 preview 필드 및 Swagger 변경이 필요하다. 운영 DB/H2 schema와 기존 Tag normalized-name 충돌에 대한 migration preflight를 포함한다.
- 기존 Observation에 evidence metadata가 없으면 자동 backfill하지 않는다. 후속 재수집으로 원본 evidence를 확보하거나 별도 검증된 migration을 수행한다.
- 자동 Tag 생성은 `UNIQUE(tag.normalized_name)`으로 동시성을 방어하고, 알려진 duplicate에 한해 실패한 트랜잭션 밖에서 재조회·재시도한다.
- 입력 개수에 비례하는 SELECT나 artist/tag N+1을 만들지 않는다. alias와 canonical Tag를 bulk 조회하고 Album/Track 입력을 함께 준비한다. 신규 Tag의 `IDENTITY` INSERT 수는 고유한 새 이름 수에 비례하므로 계측에서 SELECT 수와 별도로 보고한다.
- `tag_parent`, 관리자 승인 API, 정기 rematch scheduler, 비동기 import 및 Resolver 수식 변경은 이 결정의 구현 마일스톤에 포함하지 않는다.
