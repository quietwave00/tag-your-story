> Characterization Testing 용도

# TagNote Server Current State Baseline

기준일: 2026-08-11

이 문서는 리팩토링 전에 보호해야 할 현재 서버 동작을 코드 기준으로 기록한 baseline이다.
대상 범위는 `board`, `search`, `user`, `spotify`, `track`이다.
분석은 `tagnote-api`와 `tagnote-core`의 현재 구현을 기준으로 하며, 명세와 다른 점도 함께 기록한다.

## 1. 현재 package 구조

현재 서버는 Gradle 멀티모듈 구조다.

- `tagnote-api`
  - Spring Boot 진입점과 HTTP Controller 계층
  - 대상 범위 패키지:
    - `com.tagnote.api.domain.board`
    - `com.tagnote.api.domain.tracks`
    - `com.tagnote.api.domain.user`
- `tagnote-core`
  - JPA Entity, Service, Repository, Redis, 외부 API client
  - 대상 범위 패키지:
    - `com.tagnote.core.domain.board`
    - `com.tagnote.core.domain.boardusertag`
    - `com.tagnote.core.domain.usertag`
    - `com.tagnote.core.domain.user`
    - `com.tagnote.core.domain.tracks`
    - `com.tagnote.core.domain.tracks.webclient`
    - `com.tagnote.core.domain.tracks.util`

현재 구현에는 별도 `search` 도메인 패키지가 없다.
검색 기능은 `TrackController`와 `TrackService.search()`에 포함되어 있다.

현재 구현에는 별도 `spotify` 도메인 패키지도 없다.
Spotify 연동은 `com.tagnote.core.domain.tracks.webclient.SpotifyWebClient`가 담당한다.

### 계층 관찰

- `board`는 `Controller -> Facade -> Service -> Repository` 구조다.
- `user`는 `Controller -> Service -> Repository/Redis/JwtUtil` 구조다.
- `track/search`는 `Controller -> Service -> SpotifyWebClient/SearchKeywordTracker` 구조다.
- 명세의 "Application Service"에 해당하는 조율 책임이 일부 기능에서는 `Facade`, 일부 기능에서는 `Service`에 섞여 있다.

## 2. Controller -> Service -> Repository 호출 흐름

### 2.1 board

#### 게시글 생성

- `POST /api/boards`
- 흐름:
  - `BoardController.create`
  - `BoardFacade.create`
  - `UserService.getCacheByUserId`
  - `UserTagService.makeUserTagList`
  - `BoardUserTagService.makeBoardUserTagList`
  - `BoardService.create`
  - `BoardRepository.save`

관찰 사항:

- Controller는 Repository를 직접 호출하지 않는다.
- `BoardFacade`가 여러 도메인 서비스 조합을 담당한다.
- 게시글 생성 시 내부 Track Entity 조회는 없다.
- `trackId`는 문자열 그대로 `BoardEntity.trackId`에 저장된다.

#### 게시글 목록 조회

- `GET /api/boards/{trackId}`
- 흐름:
  - `BoardController.getBoardListByTrackId`
  - `BoardFacade.getBoardListByTrackId`
  - `BoardService.getBoardListByTrackIdSortedCreatedAt` 또는 `getBoardListByTrackIdSortedLike`
  - `BoardRepository.findByStatusAndTrackIdOrderByCreatedAtDesc` 또는 `findByStatusAndTrackIdOrderByLikeCountDesc`
  - 결과 `Board`의 `boardUserTagList`를 순회해 태그명 추출
  - `BoardService.getBoardListByTrackId`로 API 응답용 조합

관찰 사항:

- 정렬 기준은 `CREATED_AT`, `LIKE` 두 가지다.
- 페이지 크기는 서비스 내부에서 `8`로 고정되어 있다.
- 목록 응답용 태그 조합은 별도 조회가 아니라 `BoardEntity -> Board -> BoardUserTag` 변환 결과를 사용한다.

#### 게시글 상세 조회

- `GET /api/boards?boardId=...`
- 흐름:
  - `BoardController.getDetailBoard`
  - `BoardFacade.getDetailBoard`
  - `BoardUserTagService.getUserTagNameByBoardId`
  - `BoardUserTagRepository.findUserTagNameByBoardId`
  - `BoardService.getDetailBoard`
  - `BoardRepository.findByBoardIdAndStatus`

관찰 사항:

- 태그명과 게시글 본문을 각각 다른 조회로 가져온다.

#### 게시글 수 조회

- `GET /api/boards/count/{trackId}`
- 흐름:
  - `BoardController.getBoardCountByTrackId`
  - `BoardFacade.getBoardCountByTrackId`
  - `BoardService.getBoardCountByTrackId`
  - `BoardRepository.countByTrackIdAndStatus`

#### 유저 태그명으로 게시글 목록 조회

- `GET /api/boards/user-tags?userTagName=...`
- 흐름:
  - `BoardController.getBoardListByUserTagName`
  - `BoardFacade.getBoardListByUserTagName`
  - `UserTagService.getUserTagIdByUserTagName`
  - `UserTagRepository.findByName`
  - `BoardService.getBoardListByUserTagId`
  - `BoardRepository.findBoardsByUserTagId`
  - 각 게시글마다 `BoardUserTagService.getUserTagNameByBoardId`
  - `BoardUserTagRepository.findUserTagNameByBoardId`

관찰 사항:

- 게시글 수만큼 태그명 재조회가 추가로 발생한다.
- 현재 구현은 이 경로에서 N+1 위험이 있다.

#### 작성자 여부 확인

- `GET /api/boards/auth/{boardId}`
- 흐름:
  - `BoardController.isWriter`
  - `BoardFacade.isWriter`
  - `BoardService.isWriter`
  - `BoardRepository.findByBoardIdAndUserEntity_UserId`

#### 게시글 수정

- `PATCH /api/boards`
- 흐름:
  - `BoardController.updateBoardAndUserTag`
  - `BoardFacade.updateBoardAndUserTag`
  - `BoardService.getBoardEntityByBoardId`
  - 태그가 비어 있지 않으면:
    - `BoardUserTagService.deleteUserTag`
    - `BoardUserTagRepository.deleteByBoard_BoardId`
    - `UserTagService.makeUserTagList`
    - `BoardUserTagService.makeBoardUserTagList`
    - `BoardService.updateBoardWithUserTag`
  - 태그가 비어 있으면:
    - `BoardService.updateBoard`

관찰 사항:

- 수정 요청에는 현재 사용자 검증이 없다.
- 권한 확인 API는 별도로 존재하지만 수정/삭제 내부에서 재검증하지 않는다.
- `userTagList`가 빈 리스트면 기존 태그를 유지하고, 값이 있으면 전부 삭제 후 재생성한다.

#### 게시글 삭제

- `DELETE /api/boards/{boardId}`
- 흐름:
  - `BoardController.delete`
  - `BoardFacade.delete`
  - `BoardService.delete`
  - `BoardRepository.findByBoardIdAndStatus`
  - `BoardEntity.delete`

관찰 사항:

- soft delete 방식이며 상태를 `REMOVAL`로 바꾼다.
- `BoardService.delete`는 모든 예외를 포괄해 `RuntimeException`으로 다시 던진다.

### 2.2 user

#### 액세스 토큰 재발급

- `POST /api/user/reissue/accessToken`
- 흐름:
  - `UserController.reissueAccessToken`
  - `UserService.reissueAccessToken`
  - `JwtUtil.getUserIdFromToken`
  - `UserService.getCacheByUserId`
  - `UserRepository.findCachedUserByUserId` 또는 `findByUserId`
  - `JwtUtil.generateAccessToken`

관찰 사항:

- 컨트롤러는 `ROLE_USER` 권한을 요구하면서도 요청 바디로 refresh token을 받는다.

#### 리프레시 토큰 재발급

- `POST /api/user/reissue/refreshToken`
- 흐름:
  - `UserController.reissueRefreshToken`
  - `UserService.reissueRefreshToken`
  - `UserService.getCacheByUserId`
  - `UserRepository.findCachedUserByUserId` 또는 `findByUserId`
  - `JwtUtil.generateRefreshToken`
  - `CommonRedisTemplate.set`

#### 로그아웃

- `POST /api/user/logout`
- 흐름:
  - `UserController.logout`
  - `UserService.logout`
  - `SecurityContextHolder.clearContext`

관찰 사항:

- 서버 측 refresh token 삭제는 수행하지 않는다.

#### 회원가입 완료

- `POST /api/user/register`
- 흐름:
  - `UserController.register`
  - `UserService.register`
  - `UserService.getCachedPendingUserById`
  - `UserRepository.findCachedUserByPendingUserId`
  - 서비스 모델 `User` 수정
  - `UserRepository.save`
  - `UserService.saveCache`
  - `UserRepository.saveCache`
  - `UserRepository.deletePendingUser`

관찰 사항:

- 회원가입 완료는 DB의 pending user row 갱신이 아니라 Redis의 pending user를 읽어 새 `users` row를 저장하는 흐름이다.
- `nickname` 중복 검증은 현재 서비스 코드에 보이지 않는다.

### 2.3 track / search / spotify

#### 트랙 검색

- `GET /api/tracks?keyword=...&page=...`
- 흐름:
  - `TrackController.search`
  - `TrackService.search`
  - `SearchKeywordTracker.save`
  - `StringRedisTemplate.opsForZSet().incrementScore`
  - `SpotifyWebClient.getTrackInfoByKeyword`
  - Spotify Search API 호출
  - `TrackService.getTrackData`로 DTO 변환

관찰 사항:

- 검색 키워드는 Spotify 호출 전에 저장된다.
- 내부 Track Repository나 DB 카탈로그 조회는 없다.
- 검색 결과는 Spotify 응답을 그대로 가공한 DTO다.

#### 트랙 상세 조회

- `GET /api/tracks/{trackId}`
- 흐름:
  - `TrackController.getDetail`
  - `TrackService.getDetail`
  - `SpotifyWebClient.getDetailTrackInfo`
  - Spotify Get Track API 호출
  - `TrackService.getTrackData`

관찰 사항:

- 내부 Track Entity 조회 없이 Spotify 단건 조회만 수행한다.
- 컨트롤러는 `DetailTrackResponse`를 import하지만 실제 반환은 `TrackData`다.

#### 검색어 랭킹 조회

- `GET /api/tracks/ranking`
- 흐름:
  - `TrackController.getKeywordRanking`
  - `TrackService.getKeywordRanking`
  - `SearchKeywordTracker.getTopSearchKeywordList`
  - Redis ZSET 역순 조회

관찰 사항:

- 현재 랭킹은 1위부터 5위까지 문자열 리스트만 반환한다.
- 점수나 검색 횟수는 응답에 포함되지 않는다.

#### Spotify access token

- `SpotifyWebClient.getAccessToken`
- 흐름:
  - Redis에서 `SPOTIFY_ACCESS_TOKEN` 조회
  - 없으면 `generateAccessToken`
  - Spotify Client Credentials 호출
  - Redis 저장

관찰 사항:

- Spotify access token 캐시 key는 빈 문자열 `""`를 사용한다.
- 예외는 모두 `CustomException(ExceptionCode.SPOTIFY_EXCEPTION)`로 변환된다.

## 3. 현재 Entity 관계

### 3.1 현재 존재하는 관계

#### UserEntity

- 테이블: `users`
- 주요 필드:
  - `userId`
  - `userKey`
  - `email`
  - `nickname`
  - `role`
  - `userStatus`
- 관계:
  - `@OneToMany(mappedBy = "userEntity") List<BoardEntity> boardList`

관찰 사항:

- 명세는 User에 `boardList` 컬렉션을 두지 않는 방향이지만, 현재 구현은 양방향 컬렉션을 가진다.
- DB schema에는 `user_key` 인덱스만 있고 unique 제약은 없다.

#### BoardEntity

- 테이블: `board`
- 주요 필드:
  - `boardId` UUID 문자열
  - `content`
  - `status`
  - `count`
  - `trackId`
  - `likeCount`
- 관계:
  - `@ManyToOne(fetch = LAZY) UserEntity userEntity`
  - `@OneToMany(mappedBy = "board", cascade = ALL) List<BoardUserTagEntity> boardUserTagEntityList`

관찰 사항:

- `trackId`는 단순 문자열 컬럼이다.
- Track Entity와의 FK나 JPA 연관이 없다.
- `count` 필드는 spec의 `read_count`와 이름이 다르다.
- `status` 값은 `POST`, `REMOVAL`만 존재한다.

#### UserTagEntity

- 테이블: `user_tag`
- 주요 필드:
  - `userTagId`
  - `name`

관찰 사항:

- schema에는 `name` 인덱스만 있고 unique 제약은 없다.

#### BoardUserTagEntity

- 테이블: `board_user_tag`
- 주요 필드:
  - `boardUserTagId`
  - `board`
  - `userTag`
- 관계:
  - `@ManyToOne(fetch = LAZY) BoardEntity board`
  - `@ManyToOne(fetch = LAZY, cascade = PERSIST) UserTagEntity userTag`

관찰 사항:

- schema에는 `(board_id, user_tag_id)` unique 제약이 없다.
- 동일 게시글에 동일 태그가 중복 저장될 수 있다.

### 3.2 현재 없는 관계

다음은 명세에는 존재하지만 현재 분석 대상 구현에는 없다.

- `TrackEntity`
- `AlbumEntity`
- `ArtistEntity`
- `Board -> Track` FK
- `Track -> Album` FK
- `Track -> Artist` FK

즉, 현재 board는 내부 음악 카탈로그를 참조하지 않고 외부 Spotify track id 문자열만 저장한다.

## 4. 현재 public API

### 4.1 board API

#### `POST /api/boards`

- 인증: `ROLE_USER`
- 요청:
  - `content: String`
  - `trackId: String`
  - `userTagList: List<String>`
- 응답:
  - `boardId`
  - `nickname`
  - `content`
  - `createdAt`
  - `userTagList`

#### `GET /api/boards/{trackId}`

- 인증: 없음
- 요청:
  - path `trackId`
  - query `order-type`
  - query `page`
- 응답:
  - `boardResponseList`
  - `totalCount`
- 각 게시글 항목:
  - `boardId`
  - `content`
  - `createdAt`
  - `nickname`
  - `userTagNameList`

#### `GET /api/boards`

- 인증: 없음
- 요청:
  - query `boardId`
- 응답:
  - `content`
  - `nickname`
  - `likeCount`
  - `createdAt`
  - `userTagNameList`

#### `GET /api/boards/count/{trackId}`

- 인증: 없음
- 요청:
  - path `trackId`
- 응답:
  - 게시글 개수 래핑 객체

#### `GET /api/boards/user-tags`

- 인증: 없음
- 요청:
  - query `userTagName`
- 응답:
  - `List<BoardResponse>`

#### `GET /api/boards/auth/{boardId}`

- 인증: `ROLE_USER`
- 요청:
  - path `boardId`
- 응답:
  - `Boolean`

#### `PATCH /api/boards`

- 인증: `ROLE_USER`
- 요청:
  - `boardId`
  - `content`
  - `userTagList`
- 응답:
  - `BoardResponse`

#### `DELETE /api/boards/{boardId}`

- 인증: `ROLE_USER`
- 요청:
  - path `boardId`
- 응답:
  - 성공 시 빈 응답

### 4.2 user API

#### `GET /api/user/test`

- 인증: `ROLE_GUEST`
- 응답:
  - `"success"`

#### `POST /api/user/reissue/accessToken`

- 인증: `ROLE_USER`
- 요청:
  - `refreshToken`
- 응답:
  - `newAccessToken`

#### `POST /api/user/reissue/refreshToken`

- 인증: `ROLE_USER`
- 응답:
  - `newRefreshToken`

#### `POST /api/user/logout`

- 인증: `ROLE_USER`
- 응답:
  - 성공 시 빈 응답

#### `POST /api/user/register`

- 인증: `ROLE_PENDING_USER`
- 요청:
  - `nickname`
- 응답:
  - `nickname`

### 4.3 track/search API

#### `GET /api/tracks`

- 인증: 없음
- 요청:
  - query `keyword`
  - query `page`
- 응답:
  - `trackDataList`
  - `totalCount`
- 각 항목:
  - `trackId`
  - `artistName`
  - `title`
  - `albumName`
  - `imageUrl`

#### `GET /api/tracks/{trackId}`

- 인증: 없음
- 요청:
  - path `trackId`
- 실제 응답:
  - `TrackData`
  - 필드: `trackId`, `artistName`, `title`, `albumName`, `imageUrl`

#### `GET /api/tracks/ranking`

- 인증: 없음
- 응답:
  - 랭킹 문자열 리스트

## 5. server_spec.md와 충돌하는 부분

### 5.1 아키텍처 / 계층

- 명세는 `Controller -> Application Service -> Domain/Repository/External Client` 흐름을 기대한다.
- 현재 `board`는 `Facade`가 조율층 역할을 수행하고 있고, `track/search`는 `TrackService`가 직접 외부 Spotify client와 Redis tracker를 호출한다.
- 현재 `tracks` 기능에는 명세상 `TrackSearchService`, `TrackImportService`에 해당하는 분리된 유스케이스 계층이 없다.

### 5.2 User 모델

- 명세 테이블명은 `user`지만 현재 schema는 `users`다.
- 명세는 `user_key`, `email`, `nickname` 모두 unique 제약을 요구한다.
- 현재 schema는 `user_key` 일반 인덱스만 있고 unique 제약이 없다.
- 명세는 User에 `boardList` 컬렉션을 두지 않는 방향인데, 현재 구현은 `UserEntity.boardList`를 가진다.
- 명세의 Role 값은 `GUEST`, `PENDING_USER`, `USER`, `ADMIN`이지만 현재 코드는 `ROLE_*` prefix를 사용하는 Spring Security 스타일 enum이다.

### 5.3 Music Catalog / Track

- 명세는 `Artist`, `Album`, `Track` 내부 카탈로그와 각 FK/인덱스를 요구한다.
- 현재 분석 대상 구현에는 `TrackEntity`, `AlbumEntity`, `ArtistEntity`가 없다.
- 현재 `track/search` 기능은 Spotify 응답 DTO 기반이며 DB 영속화가 없다.
- 명세의 "Track 선택/Import" 단계가 현재 공개 API와 서비스 흐름에 없다.

### 5.4 Board 모델

- 명세는 `board.user_id -> user.user_id`, `board.track_id -> track.track_id` FK를 요구한다.
- 현재는 `board.user_id` FK만 있고 `track_id`는 문자열 컬럼이다.
- 명세 BoardStatus는 `PUBLISHED`, `HIDDEN`, `DELETED`인데 현재는 `POST`, `REMOVAL`이다.
- 명세는 `read_count`를 요구하지만 현재 필드명은 `count`다.
- 명세는 `INDEX(user_id, created_at)`, `INDEX(track_id, created_at)`, `INDEX(status, created_at)`, `INDEX(like_count)`를 요구하지만 현재 init schema에는 해당 인덱스가 없다.

### 5.5 UserTag / BoardUserTag

- 명세는 `board_user_tag(board_id, user_tag_id)` unique 제약을 요구한다.
- 현재 schema에는 unique 제약이 없다.
- 명세는 중복 없는 연결을 기대하지만 현재는 동일 게시글에 동일 태그 중복 삽입 가능성이 있다.

### 5.6 Spotify / 외부 API 사용 방식

- 명세는 Spotify를 검색 소스이자 Track import 진입점으로 본다.
- 현재는 Spotify 검색과 상세조회만 있고 import 단계가 없다.
- 명세는 외부 API 장애가 전체 쓰기 흐름으로 전파되지 않도록 설계를 요구하지만 현재 검색/상세조회는 Spotify 예외를 바로 `CustomException`으로 전파한다.

## 6. 리팩토링 전 보호해야 하는 기존 동작

### 6.1 board

- 게시글 생성 시 `trackId` 문자열이 그대로 저장된다.
- 게시글 생성 응답에는 작성자 nickname과 유저 태그 목록이 포함된다.
- 게시글 목록 조회는 `order-type=CREATED_AT` 또는 `LIKE`에 따라 정렬이 바뀐다.
- 게시글 목록 조회의 페이지 크기는 8이다.
- 게시글 상세 조회는 게시글 본문, 작성자 nickname, likeCount, 작성시각, 유저 태그 목록을 반환한다.
- 게시글 수정 시 `userTagList`가 비어 있지 않으면 기존 태그를 전부 지우고 새 목록으로 교체한다.
- 게시글 수정 시 `userTagList`가 빈 리스트이면 기존 태그를 유지하고 내용만 수정한다.
- 게시글 삭제는 hard delete가 아니라 `status=REMOVAL` soft delete다.
- 작성자 확인 API는 `boardId + userId` 매칭 여부를 Boolean으로 반환한다.

### 6.2 user

- access token 재발급은 요청의 refresh token에서 userId를 추출한다.
- refresh token 재발급은 인증된 현재 사용자의 userId를 사용한다.
- user cache가 있으면 DB보다 cache를 우선한다.
- 로그아웃은 SecurityContext만 정리하고 refresh token cache는 삭제하지 않는다.
- 회원가입 완료는 pending user cache를 읽어 정식 user row를 저장하고 pending cache를 삭제한다.

### 6.3 track/search/spotify

- 트랙 검색은 호출될 때마다 검색어 랭킹을 먼저 증가시킨다.
- 트랙 검색 결과는 Spotify 응답의 첫 번째 아티스트 이름과 앨범 첫 이미지 URL을 사용한다.
- 앨범 이미지가 없으면 `NO_IMAGE`를 반환한다.
- 트랙 상세 조회는 내부 DB가 아니라 Spotify 단건 조회를 사용한다.
- 검색어 랭킹은 Redis ZSET 기준 상위 5개 키워드만 문자열 리스트로 반환한다.
- Spotify access token은 Redis 캐시를 우선 사용한다.

## 7. 필요한 characterization test 제안

이번 baseline 작업에서는 production code를 수정하지 않는다.
아래 테스트는 후속 리팩토링 전에 추가가 필요한 보호 테스트 목록이다.

### 7.1 board API / 서비스

- `BoardFacade.create`
  - 유저 캐시 조회, 유저 태그 생성, 중간 엔티티 생성, 게시글 저장이 현재 순서대로 수행되는지 검증
- `BoardService.create`
  - 생성된 게시글 응답에 `userTagNameList`가 포함되는지 검증
- `BoardFacade.getBoardListByTrackId`
  - `order-type=CREATED_AT`와 `LIKE`가 각각 다른 repository 메서드를 타는지 검증
- `BoardService.getBoardListByTrackIdSortedCreatedAt`
  - 페이지 크기 8 고정 동작 검증
- `BoardFacade.getDetailBoard`
  - 태그명 조회와 게시글 조회가 둘 다 수행되어 최종 응답에 합쳐지는지 검증
- `BoardFacade.updateBoardAndUserTag`
  - `userTagList` 비어 있지 않음: 기존 연결 삭제 후 새 태그로 교체
  - `userTagList` 빈 리스트: 태그 삭제 없이 content만 수정
- `BoardService.delete`
  - 삭제 시 row 삭제가 아니라 `BoardStatus.REMOVAL`로 바뀌는지 검증
- `BoardFacade.getBoardListByUserTagName`
  - 태그명으로 게시글 조회 후 각 게시글에 태그명이 다시 주입되는지 검증

### 7.2 user API / 서비스

- `UserService.reissueAccessToken`
  - refresh token에서 userId 추출 후 cache 우선 조회로 새 access token을 발급하는지 검증
- `UserService.reissueRefreshToken`
  - 새 refresh token 생성 후 Redis에 저장하는지 검증
- `UserService.logout`
  - SecurityContext가 비워지는지 검증
- `UserService.register`
  - pending user cache 조회
  - nickname/role 변경
  - DB 저장
  - USER cache 저장
  - pending cache 삭제
  - 위 순서의 현재 동작 검증

### 7.3 track/search/spotify

- `TrackService.search`
  - 검색어 저장이 Spotify 조회 전에 수행되는지 검증
  - Spotify 응답이 `TrackData` 리스트와 `totalCount`로 매핑되는지 검증
- `TrackService.getDetail`
  - Spotify 단건 조회 결과가 `TrackData`로 변환되는지 검증
- `TrackService.getKeywordRanking`
  - tracker 결과가 그대로 `RankingList`로 래핑되는지 검증
- `TrackService.getTrackData`
  - 첫 번째 artist 이름 사용
  - 앨범 이미지가 없으면 `NO_IMAGE` 사용
- `SpotifyWebClient.getAccessToken`
  - 캐시 miss 시 토큰 생성 후 Redis 저장
  - 캐시 hit 시 생성 로직 없이 반환

### 7.4 controller 레벨

- `BoardController`
  - endpoint path, request parameter 이름, security annotation 유지 검증
- `TrackController`
  - `/api/tracks`, `/api/tracks/{trackId}`, `/api/tracks/ranking` 라우팅 유지 검증
  - 상세 조회가 현재 `TrackData` shape를 반환하는지 검증
- `UserController`
  - `/api/user/reissue/accessToken`, `/api/user/reissue/refreshToken`, `/api/user/register` 라우팅 및 role 제한 검증

## 8. 현재 테스트 상태 요약

현재 확인된 기존 테스트:

- `BoardServiceTest`
  - 게시글 생성
  - 작성자 확인
  - 게시글 목록 태그 조합
- `BoardUserTagServiceTest`
  - 중간 엔티티 리스트 생성

현재 확인된 공백:

- `TrackService` 테스트 없음
- `UserService` 테스트 없음
- 대상 Controller 테스트 없음
- spec 충돌을 보호하는 characterization test 없음

## 9. baseline 결론

현재 TagNote 서버는 board/user 일부 기능은 DB 중심으로 구현되어 있지만, track/search는 아직 Spotify DTO 조회 중심 구현이다.
명세가 기대하는 내부 music catalog, board-track FK, unique 제약, 분리된 application layer는 아직 baseline에 반영되지 않았다.

따라서 리팩토링 또는 아키텍처 정렬 전에 우선 보호해야 할 것은 다음 두 가지다.

- 현재 외부에 노출된 API shape와 observable behavior
- 현재 구현이 암묵적으로 의존하는 캐시, soft delete, 문자열 `trackId`, 태그 교체 규칙
