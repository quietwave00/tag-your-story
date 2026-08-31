# TAG-SLICE-001 — Track Selection to Resolved Tag Detail

- Status: Active (Human Review approved)
- Scope: Spotify Track 선택부터 Fake External Tag 기반 resolved system tag 상세 응답까지의 첫 수직 슬라이스
- Non-goal: 실제 MusicBrainz/Discogs 연동, provider confidence 정책, enrichment status, legacy Track Detail 전환

## Goal

Spotify Track 선택부터 resolved system tag가 포함된 Track 상세 응답까지 다음 흐름을 하나의 reviewable slice로 연결한다.

```text
Spotify Track 선택
→ Catalog Import/재사용
→ Fake External Tag 수집
→ Observation 저장 및 Alias Matching
→ Assertion 생성
→ Resolver 실행
→ Resolved Tag 저장
→ Track Detail 응답
```

Fake External Tag와 고정 confidence는 통합 테스트 fixture에만 존재한다. Production provider 정책이나 실제 MusicBrainz/Discogs 데이터로 취급하지 않는다.

## Context

현재 구현 상태:

- `POST /api/tracks/import`
  - Spotify ID로 Artist/Album/Track을 저장하거나 재사용한다.
  - Spotify HTTP와 Catalog write transaction이 분리되어 있다.
  - 응답에는 Catalog metadata와 Artist credit만 있고 system tag는 없다.
- Observation/Assertion 파이프라인이 구현되어 있다.
- Resolver 및 Album → Track 상속이 구현되어 있다.
- `subject_tag_resolved`를 API read model로 읽는 연결은 아직 없다.
- 기존 `GET /api/tracks/{trackId}`는 내부 Track PK가 아닌 Spotify ID를 받아 Spotify를 직접 조회한다.

관련 기준:

- `agents/server/server_spec.md`
- `agents/server/server_tag_feature_architecture.md`
- `agents/server/decisions/ADR-001-catalog-multi-artist-credits.md`
- `agents/server/progress.md`

## Target State

```text
TrackController
  → TrackSelectionService
      → TrackImportService
          → Spotify metadata
          → Catalog write transaction
      → ExternalTagProvider(s)
          → 외부 호출 구간, transaction 없음
      → ObservationProcessingService
          → Observation + Assertion transaction
      → TagResolutionService
          → inheritance + resolved transaction
      → TrackDetailReadService
          → Catalog + subject_tag_resolved 조회
  → CatalogTrackResponse(systemTags 포함)
```

Production에 실제 external provider가 아직 없으면 provider 목록은 비어 있으며, 기존 resolved projection 또는 Album evidence만으로 상세를 구성한다.

통합 테스트에서는 Fake provider가 Track용 raw tag를 반환하여 전체 흐름을 검증한다.

## Scope

### In Scope

- Track 선택/import 이후 Tag 파이프라인을 조율하는 Application Service
- 향후 외부 provider가 구현할 최소 provider port
- Track/Album 단위 external tag 입력 묶음
- Catalog 내부 PK를 `SubjectRef` identity로 사용하는 처리
- Album 입력이 있으면 Album 처리·resolve 후 Track resolve
- Track 입력의 Observation/Assertion 처리
- Track resolver 실행
- `subject_tag_resolved` 기반 system tag 상세 조회
- `POST /api/tracks/import` 응답에 다음 필드 추가:

```json
"systemTags": [
  {
    "tagId": 1,
    "name": "Ambient",
    "score": 0.9
  }
]
```

- Swagger contract 갱신
- Fake provider를 사용하는 first vertical slice 통합 테스트
- 반복 선택과 concurrent first selection의 멱등성 검증
- 기존 API 회귀 테스트

### 최소 Fake 시나리오

통합 테스트 taxonomy에 ACTIVE Tag와 APPROVED Alias를 저장한다.

Fake provider는 다음과 같은 고정 fixture를 반환한다.

- 매칭되는 Track raw tag 1건
- 선택적으로 미매칭 raw tag 1건
- 고정 confidence
- Spotify Track ID에 기반한 안정적인 `externalRef`

검증 결과:

- 매칭 tag는 Observation → Assertion → Resolved로 이어진다.
- 미매칭 tag는 NEW Observation으로 보존된다.
- 응답은 Assertion이 아닌 Resolved Tag만 포함한다.

## Do Not Touch

- 실제 MusicBrainz/Discogs HTTP Client 또는 응답 mapper
- MusicBrainz entity matching 및 MBID 저장
- Provider별 confidence/vote 정책
- 병렬 provider 호출 및 partial-failure 정책
- `subject_enrichment_status` 테이블과 enrichment retry scheduler
- Tag/Admin CRUD 및 alias 승인/rematch
- Taxonomy/Resolver 정책 변경
- Artist/Album/Track Entity 구조
- Catalog schema와 기존 unique/FK/index
- Album 전체 Track fan-out
- Board → Catalog Track FK migration
- 기존 `GET /api/tracks/{trackId}`의 Spotify ID 의미와 응답
- Search, Ranking, Board, UserTag API
- active `USER-TAG-001` 범위

## Artist / Album / Track 책임과 관계

### Artist

- Spotify Artist identity와 표시 metadata를 보존한다.
- Album/Track의 tag subject로 사용하지 않는다.
- 기존 `track_artist`, `album_artist` credit 조회만 재사용한다.

### Album

- Track이 참조하는 독립 Catalog Aggregate다.
- Fake slice에서는 Album tag 입력이 필수는 아니다.
- provider가 Album evidence를 반환하면 Album Observation/Assertion/Resolved를 먼저 처리할 수 있는 구조는 제공한다.
- Album resolved evidence는 기존 `TagInheritanceService`를 통해 Track으로 상속된다.

### Track

- Track 선택, Catalog import, Tag subject 및 최종 상세 조회의 기준 identity다.
- 외부 요청에서는 Spotify ID를 사용하고, Tag 파이프라인에서는 Catalog 내부 `track_id`를 사용한다.
- Track → Album은 기존 단방향 `ManyToOne(LAZY)`를 유지한다.

ADR-001에 따라 대표 Artist를 별도 컬럼으로 추가하지 않고 `position=0` credit을 사용한다.

## 변경/생성 파일

승인 후 실제 코드 구조와 import를 다시 확인하되, 예상 범위는 다음과 같다.

### 변경

- `tagnote-api/src/main/java/com/tagnote/api/domain/tracks/TrackController.java`
  - import endpoint를 `TrackSelectionService`로 연결
- `tagnote-api/src/main/java/com/tagnote/api/domain/tracks/TrackApi.java`
  - resolved system tag 응답 문서화
- `tagnote-api/src/main/java/com/tagnote/api/domain/tracks/dto/response/CatalogTrackResponse.java`
  - `systemTags` 추가
- `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/resolution/SubjectTagResolvedJpaRepository.java`
  - Track detail용 Tag fetch query 보완
- `tagnote-api/src/test/java/com/tagnote/api/domain/tracks/TrackControllerTest.java`
  - 기존 필드 및 신규 `systemTags` contract 검증

### 생성

- `tagnote-core/src/main/java/com/tagnote/application/catalog/selection/TrackSelectionService.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/detail/TrackDetailReadService.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/detail/model/TrackDetail.java`
- `tagnote-core/src/main/java/com/tagnote/application/catalog/detail/model/SystemTagDetail.java`
- `tagnote-core/src/main/java/com/tagnote/application/enrichment/port/ExternalTagProvider.java`
- `tagnote-core/src/main/java/com/tagnote/application/enrichment/model/CollectedExternalTags.java`
- `tagnote-api/src/main/java/com/tagnote/api/domain/tracks/dto/response/SystemTagResponse.java`
- `tagnote-core/src/test/java/com/tagnote/application/catalog/selection/TrackSelectionServiceTest.java`
- `tagnote-core/src/test/java/com/tagnote/application/catalog/detail/TrackDetailReadServiceTest.java`
- `tagnote-core/src/test/java/com/tagnote/application/catalog/selection/FirstVerticalSliceIntegrationTest.java`

### 변경하지 않음

- `tagnote-core/src/main/resources/db/init_schema.sql`
- Catalog/Tag Entity
- `TrackImportService`의 Spotify ID upsert 정책
- Resolver와 inheritance 구현

구현 과정에서 schema 변경이 필요하다고 확인되면 임의로 추가하지 않고 Plan 변경 사유를 먼저 보고한다.

## 데이터 흐름

### 기존 resolved Track

```text
POST /api/tracks/import
→ TrackImportService
→ spotify_id로 기존 Track 조회
→ Spotify 호출 없음
→ resolved projection 조회
→ systemTags 포함 Track Detail 반환
```

resolved row가 존재하면 external provider를 다시 호출하지 않는다. HIDDEN row도 “이미 resolution을 수행한 상태”로 판정하되 응답에서는 제외한다.

### 최초 Track

```text
POST /api/tracks/import
→ Spotify metadata 조회                 [transaction 없음]
→ CatalogWriteService.upsert           [짧은 transaction]
→ commit
→ ExternalTagProvider.collect          [transaction 없음]
→ Album Observation/Assertion 처리     [필요한 경우 transaction]
→ Album resolve                        [필요한 경우 transaction]
→ Track Observation/Assertion 처리     [transaction]
→ Track inheritance + resolve          [transaction]
→ commit
→ TrackDetailReadService
→ subject_tag_resolved + Tag 조회
→ systemTags 포함 응답
```

Track resolve는 Track 직접 입력이 없어도 실행한다. 새 Track이 이미 evidence를 가진 Album을 공유할 수 있기 때문이다.

### 반복 선택

```text
같은 spotifyTrackId
→ Catalog row 재사용
→ 기존 resolved 조회
→ provider/Observation/Resolver 재실행 없이 상세 반환
```

아직 resolved row가 전혀 없는 경우에는 provider 수집을 다시 시도할 수 있다. “정상 처리됐지만 결과가 0개”인 상태를 영속적으로 구분하려면 `subject_enrichment_status`가 필요하므로 이번 마일스톤에서는 다루지 않는다.

## DB/JPA 설계

신규 테이블이나 컬럼은 추가하지 않는다.

기존 정합성 제약을 그대로 재사용한다.

```text
UNIQUE artist.spotify_id
UNIQUE album.spotify_id
UNIQUE track.spotify_id

UNIQUE external_tag_observation(
  subject_type,
  subject_id,
  source,
  normalized_name,
  external_ref
)

UNIQUE tag_assertion(
  subject_type,
  subject_id,
  tag_id,
  source,
  evidence_type
)

UNIQUE subject_tag_resolved(
  subject_type,
  subject_id,
  tag_id
)
```

조회 정책:

- Track + Album 기본 정보는 기존 fetch query를 재사용한다.
- Track Artist와 Album Artist는 각각 Artist fetch query로 읽는다.
- resolved row와 Tag는 한 번의 fetch join으로 읽는다.
- 두 Artist collection을 하나의 다중 fetch join으로 묶지 않는다.
- Entity에 새로운 양방향 collection을 추가하지 않는다.
- API DTO mapping은 transaction 밖에서도 lazy loading 없이 가능해야 한다.
- system tag는 score 내림차순, 동일 score에서는 tag ID 순으로 결정적으로 정렬한다.
- `HIDDEN`은 응답에서 제외하고 `ACTIVE`, `MANUAL_FIXED`는 노출한다.

## Transaction

`TrackSelectionService`에는 `@Transactional`을 붙이지 않는다.

트랜잭션 경계:

1. Spotify metadata 조회: transaction 없음
2. Catalog 저장: 기존 `CatalogWriteService.upsert()` transaction
3. Fake/향후 external provider 호출: transaction 없음
4. Observation + matching + Assertion: 기존 `ObservationWriteService` transaction
5. Album resolution: 별도 짧은 transaction
6. Track inheritance + resolution: 같은 기존 resolution transaction
7. Track Detail 조회: `readOnly` transaction

Observation/Assertion이 commit된 뒤 Resolver가 실패할 수 있으나 기존 unique 제약과 멱등성으로 재실행 가능하다. 기존 승인된 서비스 경계를 하나의 긴 transaction으로 다시 합치지 않는다.

## 중복 저장 및 Spotify Upsert

- Spotify ID upsert 전략은 기존 `TrackImportService`와 `CatalogWriteService`를 그대로 사용한다.
- 이미 import된 Track은 Spotify를 재호출하지 않는다.
- concurrent first import는 Catalog unique 충돌 rollback 후 재조회/1회 재시도를 유지한다.
- Fake fixture의 `externalRef`는 랜덤값을 사용하지 않는다.
- 같은 slice를 반복해도 Observation, Assertion, Resolved row 수가 증가하지 않아야 한다.
- 각 파이프라인의 기존 제한적 unique 충돌 재시도를 재사용한다.
- Redis/distributed lock은 추가하지 않는다.

## 기존 API 영향

변경되는 API:

- `POST /api/tracks/import`
  - endpoint, 인증, request, HTTP status와 기존 응답 필드는 유지
  - `systemTags`만 가산적으로 추가
  - Assertion, Observation, resolution reason은 노출하지 않음

변경하지 않는 API:

- `GET /api/tracks`
- `GET /api/tracks/{trackId}`
- `GET /api/tracks/ranking`
- Board API 전체

기존 GET 상세를 내부 Catalog ID 기반으로 전환하는 작업은 이미지 URL 등 현재 Catalog에 없는 필드와 기존 Spotify ID 계약을 함께 다뤄야 하므로 별도 마일스톤으로 남긴다.

## 향후 Tag / External Enrichment 연결

`ExternalTagProvider`는 provider-neutral `CollectedExternalTags`만 반환한다.

향후 실제 provider 연결 시:

- MusicBrainz adapter는 Track/Album raw evidence를 반환
- Discogs adapter는 주로 Album genre/style evidence를 반환
- provider 응답은 기존 `ExternalTagInput`으로 변환
- provider별 confidence 정책은 별도 Policy Gate 승인 후 적용
- 병렬 호출 및 partial success는 실제 provider 마일스톤에서 구현
- 현재 Fake fixture는 production package나 runtime bean으로 이동하지 않음

새 `FAKE` source enum도 production에 추가하지 않는다.

## 테스트 계획

### Application Unit Test

- 기존 resolved Track은 provider를 호출하지 않고 상세를 반환한다.
- 신규 Track은 Catalog import 후 provider를 호출한다.
- provider 호출 시 활성 write transaction이 없다.
- Album inputs가 있으면 Album processing/resolve가 Track보다 먼저 실행된다.
- Track resolve는 direct 입력이 없어도 호출된다.
- 최종 응답은 resolver return 객체가 아니라 resolved projection 재조회 결과를 사용한다.
- provider가 반환한 subject ID를 신뢰하지 않고 imported Track/Album 내부 ID를 사용한다.

### First Vertical Slice Integration Test

- Fake Spotify metadata로 Artist/Album/Track 생성
- ACTIVE Tag + APPROVED Alias 준비
- Fake Track raw tag 수집
- Observation MATCHED 저장
- APPROVED Assertion 생성
- Resolver 결과 저장
- Track detail에 `tagId/name/score` 노출
- 미매칭 raw tag가 NEW로 남고 matched 결과를 막지 않음
- 응답에서 Assertion이 직접 노출되지 않음
- 같은 Spotify ID를 두 번 선택해도 모든 row 수가 불변
- 두 번째 선택은 Spotify와 Fake provider를 호출하지 않음

### JPA/Concurrency Test

- resolved + Tag 조회에서 N+1이 없음
- HIDDEN 제외 및 MANUAL_FIXED 보존
- 동일 Track 동시 최초 선택 후 Catalog/Observation/Assertion/Resolved 중복 없음
- Track/Album/Artist credit 순서와 기존 unique 제약 유지
- rollback 후 재시도가 잘못된 partial row를 만들지 않음

### API Regression Test

- import의 기존 Catalog/Artist/Album 필드 유지
- `systemTags` Swagger/JSON 일치
- 기존 GET 상세의 Spotify ID 및 `TrackData` shape 유지
- Search/Ranking endpoint 유지
- 비인증 import 접근 유지

## 위험 요소

- **Legacy detail 충돌:** 기존 GET은 Spotify ID 기반이므로 이번에 내부 ID 상세로 바꾸지 않는다.
- **0개 결과 상태:** resolved row가 없으면 “미실행”과 “성공했지만 매칭 없음”을 구분할 수 없다. enrichment status는 후속 범위다.
- **Fake 정책 유출:** fixture confidence가 production 기본값이나 provider 정책으로 이동하지 않도록 한다.
- **Polymorphic FK:** Observation/Assertion/Resolved의 subject FK는 DB에서 강제되지 않으므로 기존 Subject 검증을 반드시 통과한다.
- **중복 orchestration:** 기존 Tag Core 로직을 Selection Service에 복사하지 않고 서비스 호출로만 조율한다.
- **N+1:** Artist credit과 resolved Tag를 Entity 순회로 개별 조회하지 않는다.
- **동시성:** 각 하위 서비스가 별도 transaction이므로 전체 slice 동시 실행 후 최종 수렴을 통합 테스트로 확인한다.
- **active Plan 충돌:** `USER-TAG-001` 파일과 schema를 수정하지 않는다.

## Acceptance Criteria

- [ ] Spotify Track 선택으로 기존 Catalog import/upsert가 실행된다.
- [ ] 기존 Track은 Spotify ID 기준으로 재사용되며 중복 저장되지 않는다.
- [ ] Artist/Album/Track 관계와 전체 Artist credit이 ADR-001대로 유지된다.
- [ ] Fake External Tag와 confidence는 테스트 fixture에만 존재한다.
- [ ] external provider 호출 중 DB write transaction이 열려 있지 않다.
- [ ] raw tag가 Observation에 보존된다.
- [ ] approved alias exact match만 Assertion으로 이어진다.
- [ ] 미매칭 raw tag는 NEW로 남고 matched 결과를 막지 않는다.
- [ ] Resolver가 기존 정책과 minimum score를 그대로 사용한다.
- [ ] 최종 응답은 Assertion이 아닌 `subject_tag_resolved`를 조회한다.
- [ ] import 응답에 `systemTags(tagId, name, score)`가 추가된다.
- [ ] HIDDEN tag는 노출되지 않고 MANUAL_FIXED는 보존된다.
- [ ] 반복 및 동시 선택에도 Catalog/Observation/Assertion/Resolved가 중복되지 않는다.
- [ ] JPA 관계는 단방향 LAZY를 유지하고 N+1이 없다.
- [ ] 기존 Search/Detail/Ranking/Board/UserTag API가 유지된다.
- [ ] 실제 MusicBrainz/Discogs, confidence 정책, status/scheduler가 포함되지 않는다.
- [ ] 대상 테스트와 전체 사용자 검증이 통과한다.
- [ ] diff review 후 `progress.md`를 갱신하고 Plan을 completed로 이동한다.

## Verification

구현 후 사용자가 실행할 대상 검증:

```bash
./gradlew :tagnote-core:test --tests '*TrackSelectionServiceTest'
./gradlew :tagnote-core:test --tests '*TrackDetailReadServiceTest'
./gradlew :tagnote-core:test --tests '*FirstVerticalSliceIntegrationTest'
./gradlew :tagnote-api:test --tests '*TrackControllerTest'
```

최종 검증:

```bash
./gradlew test
./gradlew check
./scripts/verify.sh
```

Codex 정적 검토:

```bash
git diff --check
git diff --stat
git diff -- agents/server tagnote-core tagnote-api
```

검토 항목:

- API/Swagger 실제 응답 일치
- Fake fixture의 production 유입 여부
- 외부 호출 시 transaction 비활성
- unique/FK/index 유지
- resolved 조회 정렬과 HIDDEN 필터
- 반복/동시 실행 멱등성
- active `USER-TAG-001`과 변경 파일 충돌 부재
