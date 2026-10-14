# BOARD-TRACK-TAG-001 — Board Catalog Track/Album 및 Resolved System Tag 연동

- Status: Completed (2026-10-14, user-reported tests passed)
- Approved: 2026-10-14
- Scope: 기존 Track Board API와 Spotify 기반 `trackId` 호환성을 유지하면서 Board가 내부 Catalog Track 또는 Album 하나를 대상으로 하고 해당 subject의 resolved System Tag를 조회하도록 연결한다.
- Non-goal: 이 계획 승인 단계에서는 production code를 수정하지 않는다.

## Goal

기존 Track Board HTTP 경로와 Spotify 기반 `trackId` 호환성을 유지하고 Album Board 경로를 가산적으로 추가하면서 다음을 달성한다.

- Board가 내부 Catalog `TrackEntity` 또는 `AlbumEntity` 중 정확히 하나를 FK로 참조한다.
- 인증 사용자는 Spotify `trackId` 또는 `albumId` 중 정확히 하나를 대상으로 Board를 작성할 수 있다.
- Board 생성은 인증된 `ROLE_USER`만 가능하다.
- 기존 owner-scoped exact-name UserTag를 같은 트랜잭션에서 연결한다.
- 비인증 사용자도 Board 목록·상세·개수·UserTag 기준 조회가 가능하다.
- Board 조회 응답에 대상 Track 또는 Album의 visible resolved System Tag를 포함한다.
- Board 조회에서 페이지 크기나 결과 수에 비례하는 SELECT를 제거한다.
- `Controller -> Application Service -> Repository/Domain` 구조로 Board endpoint를 점진적으로 전환한다.

가장 작은 reviewable 범위는 Board endpoint vertical slice만 전환하는 것이다. Like, Comment, File 전체 구조는 재작성하지 않는다.

## 조사 기준과 관련 결정

- `AGENTS.md`
- `agents/server/current_state.md`의 Board, Track, Tag baseline
- `agents/server/server_spec.md`의 Layered Architecture, Board, UserTag, Catalog Track/Album, System Tag, 권한, 트랜잭션 및 JPA 조회 정책
- `agents/server/server_tag_feature_architecture.md`의 resolved projection 및 read 성능 정책
- `agents/server/progress.md`
- `agents/server/conventions.md`
- `ADR-002-user-owned-custom-tags.md`
- `ADR-003-user-tag-exact-name-playlist.md`
- `ADR-005-spotify-multi-subject-catalog-search.md`
- `ADR-010-seedless-system-tag-bootstrap-and-preview.md`

`current_state.md`는 2026-08-11 기준 baseline이므로 현재 코드보다 오래되었다. 현재 실제 코드에는 Catalog Track/Album, 두 subject의 System Tag pipeline, owner-scoped UserTag와 관련 DB 제약이 이미 존재한다. 구현 시 현재 코드와 `progress.md`를 함께 기준으로 삼는다.

이번 계획은 다음 결정을 변경하지 않는다.

- UserTag는 `(user_id, name)` exact-name identity다.
- System Tag와 UserTag는 Entity, Repository, Service 및 identity를 공유하지 않는다.
- 사용자 노출용 Track/Album System Tag는 `subject_tag_resolved`에서 읽는다.
- HIDDEN resolved tag는 노출하지 않는다.
- System Tag Resolver 및 bootstrap 정책은 Board 계층에서 복제하지 않는다.

현재 Server Spec의 “Board는 반드시 하나의 Track을 대상으로 작성한다”는 규칙을 Track 또는 Album 하나로 확장해야 한다. 구현 전에 `ADR-013-board-catalog-subject.md`를 작성하고 Server Spec의 Board 관계, 작성/조회 흐름, API 및 핵심 관계를 함께 정렬한다. ADR은 Track/Album 외의 Artist 또는 임의 polymorphic subject까지 범위를 넓히지 않는다.

## AS-IS

### 호출 구조

```text
BoardController
  -> BoardFacade
    -> BoardService / BoardWriteService
      -> BoardRepository / BoardUserTagRepository
```

### Layered Architecture 위반

- `BoardFacade`와 `BoardService`에 use case 조율, read model 조합, transaction 및 persistence 접근이 섞여 있다.
- Domain 패키지의 Service가 Spring Data Repository를 직접 사용한다.
- `BoardEntity.toBoard()`가 작성자와 UserTag LAZY 관계를 순회하며 persistence entity를 서비스/API 모델로 변환한다.
- `BoardController`가 legacy core service model인 `Board`, `BoardList`에 직접 의존한다.
- Board API용 Swagger interface가 없고 Controller와 실제 DTO를 기준으로 한 인증·오류 contract 문서가 없다.

### Board와 Catalog Track/Album의 충돌

- `BoardEntity.trackId`는 FK가 아닌 문자열 컬럼이다.
- 해당 문자열은 기존 Search 및 프론트 흐름에서 Spotify Track ID 의미로 사용된다.
- 현재 Catalog `TrackEntity`는 내부 `Long trackId`와 unique `spotifyId`를 별도로 가진다.
- 현재 Catalog `AlbumEntity`도 내부 `Long albumId`와 unique `spotifyId`를 별도로 가지며 ALBUM subject tag pipeline이 구현되어 있다.
- 현재 Board에는 Album 대상과 `albumId` 입력을 표현할 방법이 없다.
- Board가 문자열을 그대로 저장하면 Catalog 무결성, System Tag 조회와 향후 subject 기반 query를 보장할 수 없다.
- 반대로 공개 Board API의 `trackId`를 즉시 내부 PK로 재해석하면 기존 Search 결과와 클라이언트가 깨진다.

따라서 공개 경계에서는 `trackId`와 `albumId` 모두 Spotify ID 의미를 사용한다. Application 경계에서 요청에 존재하는 정확히 하나의 ID를 Catalog Track 또는 Album으로 해석하고 DB에는 해당 내부 FK 하나만 저장한다.

### JPA 및 Repository 문제

- `BoardRepository extends JpaRepository<BoardEntity, Long>`이지만 실제 Board PK는 String UUID다.
- `UserEntity.boardList`와 `BoardEntity.boardUserTagEntityList`는 명세에서 피하도록 한 큰 역방향 컬렉션이다.
- `BoardEntity.toBoard()`가 `userEntity`, `boardUserTagEntityList`, 각 `userTag`를 접근한다.
- Track별 page query는 위 관계를 fetch하지 않으므로 결과 Board 수에 따라 추가 SELECT가 발생할 수 있다.
- Album별 Board page/count query는 존재하지 않는다.
- 컬렉션 fetch join을 page query에 직접 추가하면 중복 row, 메모리 pagination 또는 count query 문제가 생긴다.
- 상세 조회는 Board와 UserTag 이름을 별도 경로에서 가져오지만 공통 bulk read model 조합 경계가 없다.
- UserTag 이름 기준 조회는 현재 fetch join으로 이전 N+1 문제를 일부 제거했지만 pagination이 없고 System Tag를 함께 조합하지 않는다.

### 권한 문제

- 생성은 `@PreAuthorize("hasRole('USER')")`와 `@CurrentUserId`로 인증 사용자를 사용한다.
- 공개 조회 method에는 인증 annotation이 없어 비인증 접근이 가능하다.
- 수정과 삭제는 `ROLE_USER`만 확인하고 현재 사용자 ID를 use case에 전달하지 않는다.
- 별도 작성자 확인 API가 존재하지만 수정·삭제 transaction 내부에서 작성자 권한을 다시 검증하지 않는다.

### 보호할 기존 동작

- API base path `/api/boards`
- 기존 method/path/query parameter 및 `ApiResult` envelope
- 목록 정렬 `CREATED_AT`, `LIKE`
- page size 8 및 0-based page
- `POST` 상태만 공개 조회
- 삭제 시 `REMOVAL` soft delete
- UserTag exact-name 및 `(user_id, name)` identity
- 같은 생성 요청 안의 완전히 동일한 UserTag 이름 중복 제거
- 수정 요청의 빈 `userTagList`는 기존 태그 유지
- UserTag 이름 조회 결과가 없을 때 `USER_TAG_NOT_FOUND`
- 공개 HTTP `trackId` 값은 Spotify Track ID 문자열
- 신규 공개 HTTP `albumId` 값은 Spotify Album ID 문자열
- 생성 요청은 `trackId`, `albumId` 중 정확히 하나만 허용

## Target State

```text
Presentation
  BoardController implements BoardApi
        |
Application
  BoardCommandService
  BoardTransactionalWriter
  BoardQueryService
  Board read models
        |
Infrastructure / Persistence
  BoardJpaRepository
  BoardUserTag bulk query
  TrackJpaRepository
  AlbumJpaRepository
  SubjectTagResolvedJpaRepository
        |
Domain
  BoardEntity --LAZY ManyToOne--> TrackEntity (nullable)
  BoardEntity --LAZY ManyToOne--> AlbumEntity (nullable)
  BoardEntity --LAZY ManyToOne--> UserEntity
  BoardUserTagEntity --> Board/UserTag
```

### Board ↔ Catalog Subject

- 공개 요청과 path의 `trackId`는 호환성을 위해 Spotify Track ID 문자열로 유지한다.
- 신규 요청과 path의 `albumId`도 Spotify Album ID 문자열로 정의한다.
- 생성 요청은 `trackId`, `albumId` 중 정확히 하나만 가져야 한다. 둘 다 없거나 둘 다 있으면 HTTP 400으로 거부한다.
- Presentation/Application 내부 command에서는 의미를 드러내도록 `spotifyTrackId`, `spotifyAlbumId`로 명명한다.
- Track Board 생성은 기존 `TrackSelectionService`, Album Board 생성은 기존 `AlbumSelectionService`를 호출하여 대상 Catalog subject를 확보한다.
- 외부 enrichment가 필요한 경우 Board DB write transaction 밖에서 수행한다.
- 실제 Board transaction에는 `BoardSubject(TRACK, catalogTrackId)` 또는 `BoardSubject(ALBUM, catalogAlbumId)`를 전달한다.
- `BoardEntity.track`과 `BoardEntity.album`은 각각 nullable 단방향 `ManyToOne(fetch = LAZY)`로 두되 domain factory와 DB CHECK constraint로 정확히 하나만 non-null임을 보장한다.
- `TrackEntity`, `AlbumEntity`에는 Board 역방향 컬렉션을 추가하지 않는다.

### System Tag 조회

```text
Board.track.trackId 또는 Board.album.albumId
  -> SubjectType.TRACK 또는 SubjectType.ALBUM
  -> subject_tag_resolved
  -> Tag
```

- assertion이나 observation을 조회하지 않는다.
- `ResolvedStatus.HIDDEN`은 제외한다.
- score 내림차순, 동일 score에서는 tagId 오름차순으로 반환한다.
- System Tag를 Board 테이블이나 별도 Board 연결 테이블에 복제하지 않는다.
- Resolver 및 enrichment를 Board read 요청에서 실행하지 않는다.

### 인증과 인가

- `POST`, `PATCH`, `DELETE`, 작성자 확인 API는 `ROLE_USER`가 필요하다.
- `PATCH`, `DELETE`는 현재 사용자 ID를 Application 계층에 전달하고 Board 작성자와 일치하는지 같은 use case 안에서 검증한다.
- 목록·상세·개수·UserTag 기준 조회에는 인증을 요구하지 않는다.

### 점진적 전환

- Board HTTP endpoint는 새 Application 서비스로 전환한다.
- 기존 Like, Comment, File이 사용하는 Board 조회/카운터 기능은 이번 마일스톤에서 전면 재작성하지 않는다.
- 기존 `BoardService`에는 이들 legacy consumer가 실제 사용하는 최소 기능만 남긴다.
- 새 Board endpoint 경로에서는 `BoardFacade`와 entity 컬렉션 기반 조합을 사용하지 않는다.

## 변경할 파일

### Presentation

| 파일 | 책임 |
|---|---|
| `tagnote-api/src/main/java/com/tagnote/api/domain/board/BoardController.java` | 새 Application 서비스만 호출하고 현재 사용자 ID를 write use case에 전달한다. Swagger annotation은 새 interface에 둔다. |
| `tagnote-api/src/main/java/com/tagnote/api/domain/board/dto/request/CreateBoardRequest.java` | 기존 `trackId`와 신규 `albumId`를 받으며 정확히 하나만 존재하는지 class-level validation한다. 두 값 모두 Spotify ID 의미로 Application command에 매핑한다. |
| `tagnote-api/src/main/java/com/tagnote/api/domain/board/dto/request/UpdateBoardRequest.java` | 기존 request JSON shape와 빈 목록 의미를 유지한다. 현재 사용자 ID는 Controller에서 별도로 전달한다. |
| `tagnote-api/src/main/java/com/tagnote/api/domain/board/dto/response/BoardResponse.java` | 새 `BoardView`를 기존 필드로 매핑하고 `subjectType`, 대상에 따른 `trackId` 또는 `albumId`, visible `systemTags`를 가산적으로 노출한다. 비대상 ID는 null이다. |
| `tagnote-api/src/main/java/com/tagnote/api/domain/board/dto/response/CreateBoardResponse.java` | 생성 응답의 기존 필드를 유지하고 subject type, Track/Album ID 및 System Tag를 가산적으로 노출한다. |
| `tagnote-api/src/main/java/com/tagnote/api/domain/board/dto/response/DetailBoardResponse.java` | 상세 응답의 기존 필드를 유지하고 subject type, Track/Album ID 및 System Tag를 가산적으로 노출한다. |
| `tagnote-api/src/main/java/com/tagnote/api/domain/board/dto/response/BoardListResponse.java` | Application page model을 기존 `boardResponseList`, `totalCount` shape로 매핑한다. |
| `tagnote-api/src/test/java/com/tagnote/api/domain/board/BoardControllerTest.java` | 기존 route/envelope characterization, 인증/익명 접근 및 System Tag 응답을 검증한다. |

### Domain 및 legacy Board 경계

| 파일 | 책임 |
|---|---|
| `tagnote-core/src/main/java/com/tagnote/core/domain/board/BoardEntity.java` | 문자열 Track 컬럼을 nullable LAZY Track/Album FK로 전환하고 정확히 하나의 target 불변식을 둔다. UserTag 역방향 컬렉션 의존을 제거한다. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/user/UserEntity.java` | 불필요한 `boardList` 양방향 컬렉션과 `addBoard`를 제거한다. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/boardusertag/BoardUserTagEntity.java` | 단방향 Board/UserTag 관계와 owner 불변식을 유지하고 Board 컬렉션 설정에 의존하지 않는다. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/board/dto/command/CreateBoardCommand.java` | 공개 `trackId`/`albumId`를 각각 Spotify ID 의미로 명확하게 전달하고 정확히 하나의 target만 허용한다. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/board/dto/command/UpdateBoardCommand.java` | 기존 content/UserTag 변경 의미를 유지하며 Application use case와 정렬한다. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/board/service/BoardService.java` | Like/Comment/File가 사용하는 최소 legacy 기능만 남기고 Board endpoint read 조합 책임을 제거한다. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/board/service/BoardFacade.java` | Controller 전환 완료 후 제거한다. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/board/service/BoardWriteService.java` | 새 Application write 경계 전환 후 제거한다. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/board/service/Board.java` | legacy consumer가 필요한 최소 모델로 축소하며 새 Board API response 모델로 사용하지 않는다. |

### Persistence 및 schema

| 파일 | 책임 |
|---|---|
| `tagnote-core/src/main/java/com/tagnote/core/domain/board/repository/BoardRepository.java` | 새 `BoardJpaRepository`로 교체하고 삭제한다. |
| `tagnote-core/src/main/java/com/tagnote/core/domain/boardusertag/repository/BoardUserTagRepository.java` | board ID 집합에 대한 UserTag 이름 bulk projection을 추가한다. 전체 package 이동은 하지 않는다. |
| `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/catalog/TrackJpaRepository.java` | Spotify ID에 대한 내부 Track ID 경량 조회를 추가한다. 기존 import query 동작은 변경하지 않는다. |
| `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/catalog/AlbumJpaRepository.java` | Spotify ID에 대한 내부 Album ID 경량 조회를 추가한다. 기존 import query 동작은 변경하지 않는다. |
| `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/resolution/SubjectTagResolvedJpaRepository.java` | 여러 Track/Album subject의 visible resolved System Tag를 subject type별 bulk query로 읽도록 확장한다. |
| `tagnote-core/src/main/java/com/tagnote/core/exception/ExceptionCode.java` | `TRACK_NOT_FOUND`, `ALBUM_NOT_FOUND`, `INVALID_BOARD_SUBJECT`, `BOARD_ACCESS_DENIED`를 기존 오류 체계에 맞춰 추가한다. |
| `tagnote-core/src/main/resources/db/init_schema.sql` | nullable `board.track_id`, `board.album_id`, 두 FK/index 및 exactly-one CHECK constraint를 추가한다. |

### 영향받는 테스트

- `tagnote-core/src/test/java/com/tagnote/domain/board/BoardEntityTest.java`
- `tagnote-core/src/test/java/com/tagnote/domain/board/fixture/BoardFixture.java`
- `tagnote-core/src/test/java/com/tagnote/domain/board/service/BoardFacadeTest.java` — 새 Application 테스트로 대체 후 제거
- `tagnote-core/src/test/java/com/tagnote/domain/board/service/BoardServiceCharacterizationTest.java`
- `tagnote-core/src/test/java/com/tagnote/domain/board/service/BoardServiceTest.java`
- `tagnote-core/src/test/java/com/tagnote/domain/board/service/BoardWriteServiceTest.java` — 새 Application 테스트로 대체 후 제거
- BoardEntity 생성 방식을 사용하는 Comment, File, Like 테스트 fixture

### 문서

| 파일 | 책임 |
|---|---|
| `agents/server/progress.md` | 구현, Acceptance Criteria, 사용자 검증 및 diff review가 끝난 뒤에만 완료 상태를 기록한다. |
| `agents/server/server_spec.md` | Board target을 Track 전용에서 `TRACK | ALBUM` exactly-one 관계로 확장하고 작성/조회/API/권한/핵심 관계를 정렬한다. |
| `agents/server/plans/active/BOARD-TRACK-TAG-001.md` | 승인된 현재 실행 계획이다. 완료 시 closure note와 검증 결과를 기록하고 `completed/`로 이동한다. |

## 새로 만들 파일

### Production

| 파일 | 책임 |
|---|---|
| `tagnote-api/src/main/java/com/tagnote/api/domain/board/BoardApi.java` | 모든 Board endpoint, method, request/response, parameter, 인증 여부와 주요 오류를 Swagger로 문서화한다. |
| `tagnote-api/src/main/java/com/tagnote/api/domain/board/dto/response/BoardSystemTagResponse.java` | `tagId`, `name`, `score` System Tag HTTP 응답이다. |
| `tagnote-core/src/main/java/com/tagnote/application/board/BoardCommandService.java` | 요청 target에 따라 Track/Album selection을 dispatch하고 transactional writer 호출 순서를 조율한다. 외부 I/O를 포함할 수 있으므로 class/method 전체에 DB transaction을 두지 않는다. |
| `tagnote-core/src/main/java/com/tagnote/application/board/BoardTransactionalWriter.java` | Board/UserTag/BoardUserTag 생성과 수정·삭제 권한 검사를 짧은 write transaction으로 수행한다. |
| `tagnote-core/src/main/java/com/tagnote/application/board/BoardQueryService.java` | 공개 Board 조회와 bulk read model 조합을 담당한다. `@Transactional(readOnly = true)` 범위에서 외부 호출을 하지 않는다. |
| `tagnote-core/src/main/java/com/tagnote/application/board/model/BoardView.java` | Presentation에 전달할 immutable Board read model이다. |
| `tagnote-core/src/main/java/com/tagnote/application/board/model/BoardPage.java` | Board 목록과 total count를 보존한다. |
| `tagnote-core/src/main/java/com/tagnote/application/board/model/BoardSystemTag.java` | Board 조회용 visible resolved System Tag다. |
| `tagnote-core/src/main/java/com/tagnote/application/board/model/BoardSubject.java` | `TRACK | ALBUM` type, 내부 catalog ID, 공개 Spotify ID를 하나의 immutable target으로 표현하고 exactly-one 의미를 고정한다. |
| `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/board/BoardJpaRepository.java` | String UUID PK를 사용하며 Board base/page/count/owner 조회를 제공한다. |
| `tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/board/BoardUserTagNameRow.java` | Board ID와 UserTag name을 담는 bulk query projection이다. |
| `agents/server/decisions/ADR-013-board-catalog-subject.md` | Board가 Track 또는 Album 정확히 하나를 대상으로 하며 두 nullable FK와 DB CHECK로 무결성을 보장하는 결정을 기록한다. |
| `agents/server/migrations/BOARD-TRACK-TAG-001.md` | 기존 Spotify 문자열 `board.track_id`를 Catalog Track FK로 변환하고 신규 nullable Album FK/check를 추가하는 운영 runbook과 preflight/rollback 기준을 기록한다. |

### Test

| 파일 | 책임 |
|---|---|
| `tagnote-core/src/test/java/com/tagnote/application/board/BoardCommandServiceTest.java` | Track/Album selection dispatch, selection 선행, transaction writer 위임, provider 실패 시 Board write 미호출을 검증한다. |
| `tagnote-core/src/test/java/com/tagnote/application/board/BoardTransactionalWriterTest.java` | 인증 writer, exactly-one Track/Album FK, UserTag owner, 수정·삭제 권한 및 태그 교체 의미를 검증한다. |
| `tagnote-core/src/test/java/com/tagnote/application/board/BoardQueryServiceTest.java` | base row, UserTag, System Tag bulk 결과 조합과 정렬을 검증한다. |
| `tagnote-core/src/test/java/com/tagnote/infrastructure/persistence/board/BoardJpaRepositoryTest.java` | FK, 상태 필터, 정렬, exact-name 조회와 고정 SELECT 수를 검증한다. |
| `tagnote-core/src/test/java/com/tagnote/infrastructure/persistence/board/BoardJpaTestConfiguration.java` | Board repository slice test에 필요한 최소 Entity/Repository 구성을 제공한다. |

구현 중 기존 테스트 구성으로 충분한 경우 별도 configuration 파일은 만들지 않는다. 단순 공통화만을 목적으로 추가 추상화나 utility를 만들지 않는다.

## 각 파일의 책임 경계

- Controller는 HTTP parameter, validation, 인증 사용자 추출과 response mapping만 안다.
- `BoardCommandService`는 target type에 맞는 Track/Album 확보와 transactional write의 호출 순서만 조율한다.
- `BoardTransactionalWriter`만 Board/UserTag 관련 write transaction을 가진다.
- `BoardQueryService`는 persistence 결과를 immutable read model로 조합한다.
- Repository는 query 및 projection만 담당한다.
- `BoardEntity`는 writer, exactly-one Track/Album target 관계와 content/status 변경 규칙만 가진다.
- System Tag 계산 및 정규화 정책은 기존 Resolver/Taxonomy 코드에만 남긴다.
- Board 계층은 `ExternalTagObservation`이나 `TagAssertion`을 직접 조회하지 않는다.
- Presentation DTO를 core/domain Entity가 직접 생성하지 않는다.

## 호출 흐름

### Track Board 생성

```text
POST /api/boards + authenticated userId
  -> BoardController
  -> BoardCommandService.create
  -> TrackSelectionService.select(spotifyTrackId)
     -> 기존 Catalog/import/enrichment 흐름
     -> 이 구간에는 Board write transaction 없음
  -> BoardTransactionalWriter.create(userId, BoardSubject(TRACK, catalogTrackId), content, tagNames)
     -> User 조회
     -> Track 조회
     -> owner + exact names로 UserTag bulk 조회
     -> 없는 UserTag 저장
     -> Board 저장
     -> BoardUserTag 저장
     -> commit
  -> BoardView + visible resolved System Tags
  -> CreateBoardResponse
```

- 기존 Track이 이미 완료 상태이면 `TrackSelectionService`의 fast path를 사용한다.
- provider 일부 실패는 기존 Track selection의 partial success 정책을 따른다.
- Track 확보 자체가 실패하면 Board transaction을 시작하지 않는다.
- UserTag unique 충돌을 이유로 전체 Board write를 자동 재시도하지 않는다.

### Album Board 생성

```text
POST /api/boards + authenticated userId + albumId
  -> BoardController
  -> BoardCommandService.create
  -> AlbumSelectionService.select(spotifyAlbumId)
     -> 기존 Album Catalog/import/enrichment 흐름
     -> 이 구간에는 Board write transaction 없음
  -> BoardTransactionalWriter.create(userId, BoardSubject(ALBUM, catalogAlbumId), content, tagNames)
     -> User 조회
     -> Album 조회
     -> owner + exact names로 UserTag bulk 조회
     -> 없는 UserTag 저장
     -> Board(album FK, track FK null) 저장
     -> BoardUserTag 저장
     -> commit
  -> BoardView + ALBUM visible resolved System Tags
  -> CreateBoardResponse
```

- 기존 Album이 완료 상태이면 `AlbumSelectionService`의 fast path를 사용한다.
- Album Board에 Track resolved tag를 섞거나 Album의 태그를 임의로 Track subject로 변환하지 않는다.
- Album → Track 상속 정책은 Track tag resolution에만 그대로 적용하며 Album Board는 ALBUM subject 결과를 직접 읽는다.

### Track별 목록

```text
GET /api/boards/{spotifyTrackId}?order-type=...&page=...
  -> BoardQueryService
  -> Spotify ID로 internal trackId 조회
  -> Board page + writer + Track 조회
  -> page board IDs의 UserTag 이름 bulk 조회
  -> distinct track IDs의 visible System Tag bulk 조회
  -> Map 기반 read model 조합
  -> BoardListResponse
```

### Album별 목록

```text
GET /api/boards/albums/{spotifyAlbumId}?order-type=...&page=...
  -> BoardQueryService
  -> Spotify ID로 internal albumId 조회
  -> Album 대상 Board page + writer + Album 조회
  -> page board IDs의 UserTag 이름 bulk 조회
  -> SubjectType.ALBUM + distinct album IDs의 visible System Tag bulk 조회
  -> Map 기반 read model 조합
  -> BoardListResponse
```

### 상세

```text
GET /api/boards?boardId=...
  -> Board + writer + Track 또는 Album 단건 조회
  -> UserTag 이름 조회
  -> Board subject type/id에 맞는 visible System Tag 조회
  -> DetailBoardResponse
```

### UserTag 이름 기준 공개 조회

```text
GET /api/boards/user-tags?userTagName=...
  -> exact name으로 모든 owner의 POST Board 조회
  -> Board IDs의 전체 UserTag bulk 조회
  -> distinct Track/Album subject IDs의 System Tag bulk 조회
  -> 중복 없는 BoardView 목록
```

### 수정

```text
PATCH /api/boards + authenticated userId
  -> BoardTransactionalWriter
  -> POST Board + writer 조회
  -> writer == current user 검증
  -> content 수정
  -> userTagList가 비어 있으면 기존 태그 유지
  -> 비어 있지 않으면 writer 기준 exact-name UserTag bulk find-or-create
  -> 기존 BoardUserTag 교체
  -> commit
```

Track/Album 대상 및 subject type 변경은 허용하지 않는다.

### 삭제

```text
DELETE /api/boards/{boardId} + authenticated userId
  -> BoardTransactionalWriter
  -> POST Board + writer 조회
  -> writer == current user 검증
  -> status = REMOVAL
  -> commit
```

## JPA 및 쿼리 정책

- 컬렉션 fetch join과 pagination을 결합하지 않는다.
- page base query에는 to-one인 writer와 해당 Track/Album target만 fetch join 또는 entity graph를 사용한다.
- UserTag와 System Tag는 각각 한 번의 bulk query로 읽는다.
- 여러 Board가 같은 Track/Album을 가리키면 `(subjectType, subjectId)`별 System Tag 결과를 한 번만 조합한다.
- `BoardEntity`와 `UserEntity`에 API 조회 편의를 위한 역방향 컬렉션을 두지 않는다.
- `BoardUserTagEntity`의 `(board_id, user_tag_id)` unique 및 `(user_tag_id, board_id)` index를 유지한다.
- `UserTagEntity`의 `(user_id, name)` exact unique 및 binary collation을 유지한다.
- `subject_tag_resolved`는 `SubjectType.TRACK`과 Track ID 집합 또는 `SubjectType.ALBUM`과 Album ID 집합을 각각 bulk 조회한다. 서로 다른 subject type의 같은 숫자 PK를 같은 key로 취급하지 않는다.
- HIDDEN 제외와 System Tag 정렬은 query와 Application 조합 결과가 같은 contract를 만들도록 테스트한다.

목표 SELECT 상한:

| Use case | 최대 SELECT 구성 |
|---|---|
| Track별 Board page | Track ID 1 + page content 1 + count 1 + UserTag 1 + System Tag 1 |
| Album별 Board page | Album ID 1 + page content 1 + count 1 + UserTag 1 + System Tag 1 |
| Board 상세 | Board 1 + UserTag 1 + System Tag 1 |
| UserTag 이름 기준 목록 | Board 1 + UserTag 1 + Track System Tag 1(해당 시) + Album System Tag 1(해당 시) |
| Board count | count 1 |

Hibernate Statistics 기반 integration test에서 Board가 1개일 때와 8개일 때 SELECT 수가 같아야 한다. INSERT 수처럼 입력 수에 비례하는 정상 쓰기는 SELECT 계측과 분리한다.

## DB 및 migration

### 목표 schema

```text
board
- board_id varchar PK
- user_id bigint not null FK -> users.user_id
- track_id bigint null FK -> track.track_id
- album_id bigint null FK -> album.album_id
- content
- status
- count
- like_count
- created_at
- updated_at

INDEX(user_id, created_at)
INDEX(track_id, created_at)
INDEX(album_id, created_at)
INDEX(status, created_at)
INDEX(like_count)
CHECK((track_id is not null and album_id is null)
   or (track_id is null and album_id is not null))
```

이번 마일스톤에서는 기존 `count`를 `read_count`로 rename하지 않고 기존 BoardStatus enum도 전면 교체하지 않는다.

### 기존 데이터 변환

현재 모든 legacy Board의 `board.track_id`는 Spotify Track ID 문자열이고 Album Board는 없으므로 단순 column type 변경을 하지 않는다.

runbook은 다음 순서를 포함한다.

1. 현재 Board row 수와 null/blank/중복 의미를 기록한다.
2. `board.track_id = track.spotify_id`로 매칭되지 않는 Board를 조회한다.
3. 매칭되지 않는 Board가 한 건이라도 있으면 migration을 중단하고 해당 Track을 먼저 Catalog에 확보하거나 명시적인 데이터 처리 승인을 받는다.
4. 임시 bigint column에 매칭된 `track.track_id`를 기록한다.
5. 모든 Board가 정확히 하나의 Track에 매칭됐는지 검증한다.
6. nullable `album_id`를 추가하고 Album FK/index를 추가한다.
7. Track FK/index와 exactly-one CHECK constraint를 추가한다.
8. legacy 문자열 column을 제거하고 bigint column을 nullable `track_id`로 전환한다.
9. 변환 전후 Board row 수와 Board별 Spotify Track 의미가 동일하며 모든 row가 exactly-one constraint를 만족하는지 검증한다.

외부 API를 migration SQL이나 DB transaction 안에서 호출하지 않는다. unresolved legacy row를 임의 삭제하거나 임의 Track에 연결하지 않는다.

## 기존 API 영향

### 유지

- `POST /api/boards`
- `GET /api/boards/{trackId}`
- `GET /api/boards?boardId=...`
- `GET /api/boards/count/{trackId}`
- `GET /api/boards/user-tags?userTagName=...`
- `GET /api/boards/auth/{boardId}`
- `PATCH /api/boards`
- `DELETE /api/boards/{boardId}`
- 기존 query parameter 이름과 `ApiResult` envelope
- 공개 `trackId` 값의 Spotify Track ID 의미
- page size 8과 정렬 방식
- 생성 시 non-empty `userTagList`
- 수정 시 empty `userTagList`는 기존 태그 유지
- 익명 Board read

### 가산 변경

- `POST /api/boards`가 기존 `trackId` 외에 신규 `albumId`를 선택적으로 받는다. 정확히 하나만 제공해야 한다.
- `GET /api/boards/albums/{albumId}?order-type=...&page=...`를 익명 Album Board 목록 API로 추가한다.
- `GET /api/boards/count/albums/{albumId}`를 익명 Album Board count API로 추가한다.
- Board 응답에 `subjectType: TRACK | ALBUM`, `trackId`, `albumId`를 명시적으로 포함한다. 대상이 아닌 ID는 null이다.
- Board 응답에 `systemTags[{tagId, name, score}]`를 포함한다.
- `BoardApi` Swagger interface를 추가한다.
- `TRACK_NOT_FOUND`, `ALBUM_NOT_FOUND`, `INVALID_BOARD_SUBJECT`, `BOARD_ACCESS_DENIED` 오류를 문서화한다.

새 응답 필드는 하위 호환적인 additive change로 취급한다. `systemTags`는 결과가 없을 때 `null`이 아니라 빈 배열로 반환한다.

Track Board 생성 예:

```json
{
  "content": "track board",
  "trackId": "spotify-track-id",
  "userTagList": ["1월"]
}
```

Album Board 생성 예:

```json
{
  "content": "album board",
  "albumId": "spotify-album-id",
  "userTagList": ["정주행"]
}
```

`trackId`와 `albumId`가 모두 존재하거나 모두 없는 요청은 `INVALID_BOARD_SUBJECT` validation 오류로 처리한다.

### 행동 수정

- PATCH/DELETE는 로그인 여부뿐 아니라 작성자 소유권까지 확인한다.
- 존재하지 않거나 확보할 수 없는 Spotify Track/Album에는 Board를 만들지 않는다.
- DB에는 Spotify ID 문자열 대신 내부 Track 또는 Album FK 정확히 하나를 저장한다.

## Swagger 계획

`BoardApi`에 다음을 명시한다.

- endpoint와 HTTP method
- required path/query parameter
- 인증 필요 여부
- 요청 DTO field 의미와 제약
- 공개 `trackId`/`albumId`가 각각 Spotify Track/Album ID라는 의미
- create request에서 두 ID가 상호 배타적이라는 validation
- System Tag가 resolved/visible projection이라는 의미
- success response envelope와 기존 DTO field
- validation 400
- 미인증/권한 오류
- `BOARD_NOT_FOUND`, `TRACK_NOT_FOUND`, `ALBUM_NOT_FOUND`, `INVALID_BOARD_SUBJECT`, `USER_TAG_NOT_FOUND`, `BOARD_ACCESS_DENIED`
- provider 실패가 Track/Album 확보에 영향을 주는 경우 기존 `SPOTIFY_EXCEPTION` contract

Swagger annotation은 Controller에 중복하지 않는다.

## 테스트 계획

### Characterization

- 구현 변경 전 기존 Board Controller/Service 테스트가 보호하는 route, envelope, page size, 정렬, soft delete 및 UserTag 수정 의미를 확인한다.
- 테스트 expectation을 변경해야 하는 경우 Target State에 명시된 Track/Album target FK, System Tag 또는 소유권 수정에 직접 필요한 변경인지 검토한다.

### Controller

- 인증된 USER의 POST 성공
- 익명 POST/PATCH/DELETE 차단
- 익명 목록·상세·count·UserTag 조회 성공
- 기존 path/query/envelope 유지
- Track create, Album create 및 둘 다/둘 다 아님 validation
- `subjectType`, `trackId`, `albumId`, `systemTags` response shape
- 익명 Album 목록/count route
- 요청 validation과 주요 오류 Swagger contract 정렬

### Application command

- target에 따라 `TrackSelectionService` 또는 `AlbumSelectionService` 하나만 호출됨
- Track/Album selection이 transactional writer보다 먼저 호출됨
- selection 실패 시 writer 미호출
- 인증 userId가 Board writer로 사용됨
- Track/Album selection 결과의 subject type과 내부 PK가 정확한 Board FK로 전달됨
- UserTag owner가 Board writer와 일치
- 동일 요청의 완전히 같은 tag 이름 중복 제거
- 빈 update tag 목록은 기존 태그 유지
- non-empty update tag 목록은 기존 연결 교체
- 작성자가 아닌 사용자의 수정·삭제 거부
- UserTag unique 충돌 자동 재시도 없음

### Application query

- CREATED_AT/LIKE 분기와 page size 8
- 상세/목록/UserTag 조회에서 UserTag 이름 조합
- 여러 Track/Album의 System Tag를 `(subjectType, subjectId)`별로 올바르게 조합
- HIDDEN 제외
- score 내림차순 및 tagId 오름차순
- resolved 결과가 없으면 빈 목록
- UserTag exact-name 조회 및 Board distinct

### JPA integration

- Board의 exactly-one Track/Album FK 저장과 orphan 차단
- String UUID repository ID 타입
- POST 상태 필터
- Track별 CREATED_AT/LIKE 정렬
- Album별 CREATED_AT/LIKE 정렬
- exact-name UserTag 조회
- to-one fetch와 collection bulk query
- page 결과가 1개/8개여도 SELECT 수 고정
- 여러 Board/Track/Album의 UserTag 및 System Tag bulk 조회

### Regression

- Like atomic counter update
- Comment/File이 POST Board를 계속 조회할 수 있음
- soft-deleted Board가 공개 read에서 제외됨
- 기존 UserTag repository integration tests
- Track/Album selection/import/System Tag tests

### Migration

- 모든 legacy `board.track_id`가 `track.spotify_id`에 정확히 매칭됨
- 미매칭 Board 0건
- 변환 후 null/FK 위반 0건
- 변환 전후 Board row 수 동일
- 변환 전후 Board별 Spotify Track 의미 동일
- 모든 Board의 Track/Album FK가 정확히 하나만 non-null

## 위험 요소

- legacy Board의 Spotify ID에 대응하는 Catalog Track이 없으면 FK migration을 진행할 수 없다.
- 미매칭 데이터를 자동 import하려 하면 외부 HTTP와 migration transaction이 결합되므로 금지한다.
- 생성 시 Track/Album이 미수입 상태라면 기존 selection/enrichment 시간이 포함될 수 있다. Board write transaction은 그 이후에만 시작해야 한다.
- Track/Album selection 성공 후 Board write가 실패하면 Catalog/Tag 결과는 남을 수 있다. Catalog 확보는 독립적으로 유효한 멱등 작업이므로 이를 rollback 대상으로 삼지 않는다.
- UserTag 기준 조회는 기존 contract상 pagination이 없어 결과 집합 자체가 커질 수 있다. 이번 마일스톤은 contract를 유지하고 N+1만 제거한다.
- BoardEntity는 Comment/File/Like가 참조하므로 관계 변경 후 관련 fixture와 회귀 테스트가 필요하다.
- System Tag를 Board별 응답에 포함하면 같은 Track/Album의 태그가 여러 Board item에 반복될 수 있다. DB 조회는 subject별로 중복하지 않되 response shape 단순성과 기존 목록 구조를 우선한다.
- 두 nullable FK 방식은 DB CHECK 지원이 필요하다. 운영 DB의 CHECK enforcement를 migration preflight에서 확인하고 지원되지 않는 DB 버전이면 임의로 application 검증만 사용하지 말고 대체 DB 제약을 먼저 승인받는다.
- 작업 트리에 기존 사용자 변경이 많으므로 구현 diff는 이 Plan에 명시된 경로로 한정해 검토한다.

## 최소 Enabling Refactor

이번 기능을 위해 필요한 최소 리팩토링은 다음으로 제한한다.

- Board의 문자열 Track 저장을 exactly-one Catalog Track/Album FK로 전환
- Board HTTP endpoint용 Application command/query 경계 생성
- 외부 Track/Album selection과 Board write transaction 분리
- entity collection 순회 대신 base row + bulk UserTag/System Tag 조합
- String UUID에 맞는 Board repository 타입 정렬
- Board API Swagger interface 추가
- write 요청의 작성자 소유권 검사

위 항목을 넘어선 공통화, package 전면 이동, enum/schema rename은 수행하지 않는다.

## Do Not Touch

- Search endpoint와 검색 응답의 Spotify ID 의미
- Catalog import/enrichment/Resolver 알고리즘
- Observation, Assertion, taxonomy normalization
- Album → Track 상속 계산 정책 자체(Album Board는 ALBUM resolved 결과를 직접 조회)
- UserTag exact-name identity와 normalization 금지 정책
- BoardStatus의 `POST`/`REMOVAL` 전면 변경
- `count`를 `readCount`로 rename하는 schema cleanup
- Board ID UUID 형식 변경
- cursor pagination 도입
- UserTag 기준 조회 pagination 변경
- Like, Comment, Notification, File의 기능 리팩토링
- BoardLike 및 Notification 신규 기능
- System Tag filter/subscription API
- 프론트엔드 코드
- 관련 없는 formatting, rename, 추상화 또는 package 전면 이동

Comment/File/Like에서 Board target 타입 변경 때문에 필요한 컴파일 수정과 fixture 갱신은 허용하지만 동작 변경이나 구조 재설계는 하지 않는다. Artist Board와 임의 subject 확장은 포함하지 않는다.

## Acceptance Criteria

- [x] Board row가 유효한 Catalog Track 또는 Album FK를 정확히 하나 가진다.
- [x] 공개 HTTP `trackId`는 기존 Spotify Track ID 의미를 유지한다.
- [x] 공개 HTTP `albumId`는 Spotify Album ID 의미를 사용한다.
- [x] create request는 `trackId`, `albumId` 중 정확히 하나만 허용한다.
- [x] Track Board는 TRACK resolved tag만, Album Board는 ALBUM resolved tag만 반환한다.
- [x] Board 생성의 외부 Track/Album selection 중 Board DB transaction이 열리지 않는다.
- [x] Board/UserTag/BoardUserTag 저장은 하나의 짧은 transaction이다.
- [x] 인증되지 않은 사용자는 Board를 생성·수정·삭제할 수 없다.
- [x] 작성자가 아닌 사용자는 Board를 수정·삭제할 수 없다.
- [x] 비인증 사용자는 기존 Board read endpoint를 사용할 수 있다.
- [x] 비인증 사용자는 신규 Album Board 목록/count endpoint를 사용할 수 있다.
- [x] Board 응답은 대상 Track/Album의 visible resolved System Tag만 반환한다.
- [x] HIDDEN 태그와 Assertion/Observation은 Board 응답에 노출되지 않는다.
- [x] System Tag는 score 내림차순, 동점 tagId 오름차순이다.
- [x] resolved 결과가 없으면 `systemTags`는 빈 배열이다.
- [x] 목록 SELECT 수가 Board 수에 비례하지 않는다.
- [x] UserTag owner/writer 불변식과 DB unique 제약이 유지된다.
- [x] 기존 정렬, page size, soft delete, exact-name 조회 동작이 유지된다.
- [x] 기존 생성/수정 UserTag 의미가 유지된다.
- [x] Board Swagger 명세가 실제 Controller/DTO와 일치한다.
- [ ] migration preflight에서 미매칭 legacy Board가 0건이다.
- [x] 관련 테스트 완료를 사용자가 확인하고 Plan 완료 처리를 요청했다.
- [x] 범위 diff review에서 관련 없는 cleanup이나 후속 기능 구현이 없다.

## Verification

구현 후 사용자가 실행할 명령:

```bash
./gradlew :tagnote-core:test --tests '*Board*'
./gradlew :tagnote-core:test --tests '*BoardJpaRepositoryTest'
./gradlew :tagnote-api:test --tests '*BoardControllerTest'
./gradlew test
./gradlew check
./scripts/verify.sh
```

Codex는 위 Gradle 및 verify 명령을 직접 실행하지 않는다. 사용자가 전달한 결과만 검증 상태와 Plan 완료 판단에 사용한다.

Codex 정적 검토:

```bash
git diff --check -- \
  agents/server \
  tagnote-api/src/main/java/com/tagnote/api/domain/board \
  tagnote-api/src/test/java/com/tagnote/api/domain/board \
  tagnote-core/src/main/java/com/tagnote/application/board \
  tagnote-core/src/main/java/com/tagnote/core/domain/board \
  tagnote-core/src/main/java/com/tagnote/core/domain/boardusertag \
  tagnote-core/src/main/java/com/tagnote/infrastructure/persistence/board \
  tagnote-core/src/main/resources/db/init_schema.sql
```

정적 review 항목:

- Controller에 비즈니스 로직 또는 Repository 접근이 없는가
- 외부 Track/Album selection 동안 Board write transaction이 열리지 않는가
- Board FK와 migration 절차가 실제 schema/JPA mapping과 일치하는가
- collection fetch pagination 또는 N+1이 없는가
- System Tag 조회가 `subject_tag_resolved`만 사용하는가
- UserTag owner/writer 불변식과 exact-name 정책이 유지되는가
- update/delete 소유권 검사가 transaction 내부에서 수행되는가
- API 및 Swagger contract가 일치하는가
- Like/Comment/File에 불필요한 동작 변경이 없는가
- 이번 milestone과 관계없는 cleanup이 없는가

## 완료 절차

1. 구현과 테스트 코드 작성
2. Codex의 범위 제한 정적 diff review
3. 사용자가 대상 테스트, 전체 `test`, `check`, `verify.sh` 실행
4. 사용자 실행 결과와 migration preflight 결과 확인
5. Acceptance Criteria 확인
6. `progress.md` 갱신
7. 이 Plan에 closure note와 검증 결과 기록
8. `agents/server/plans/completed/BOARD-TRACK-TAG-001.md`로 이동

## Completion Record

- 2026-10-14: 사용자가 Board JPA/UserTag JPA 테스트를 포함한 테스트 완료를 확인하고 Plan 완료 처리를 요청했다.
- 최초 JPA slice context 실패는 Catalog credit Entity가 참조하는 `ArtistEntity` 누락이 원인이었으며, Board/UserTag JPA test configuration의 entity scan을 보완한 뒤 재검증했다.
- Board가 Track 또는 Album 중 정확히 하나를 참조하도록 Catalog FK, domain factory와 DB CHECK를 정렬했다.
- 인증 write 및 owner 검사, 익명 read, UserTag 연결, visible resolved System Tag bulk 조회와 Track/Album API를 구현했다.
- Board endpoint를 Application command/query/write 경계로 전환하고 Like/Comment/File용 legacy BoardService는 필요한 최소 기능만 유지했다.
- ADR-013, Server Spec, 기준 schema, migration runbook 및 Swagger contract를 구현과 정렬했다.
- 범위 정적 리뷰를 완료했다. 기존 작업 트리의 CRLF 변경으로 전역 `git diff --check`는 trailing-whitespace 오탐을 보고했다.
- 전체 `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`의 개별 출력과 운영 DB migration preflight 결과는 별도로 전달되지 않아 미확인 상태로 남긴다.
