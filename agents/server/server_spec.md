# TagNote Server Spec

> 문서 역할: TagNote 서버 전체 상위 설계 명세  
> 태그 수집/정규화/Assertion/Resolver 상세 구현은 `agents/server/_tag_feature_architecture.md`를 우선 참조한다.

---

## 1. 목적

TagNote는 헤비 리스너를 위한 음악 정보/커뮤니티 서비스다.

다른 음악 커뮤니티와의 핵심 차별점은 **음악 메타데이터를 기반으로 장르/스타일/씬/국가/시대 정보를 구조화하고 Resolver를 통해 System Tag로 제공하는 것**이다.

태그는 명확히 두 종류로 분리한다.

- **System Tag**: 외부 음악 메타데이터 + 내부 taxonomy + Resolver를 통해 계산되는 구조화 태그
- **User Tag**: 사용자가 게시글에 직접 작성하는 자유로운 커스텀 태그

주요 기능:

- Spotify 기반 음악 검색
- Track / Album / Artist 내부 카탈로그 구축
- MusicBrainz / Discogs / Last.fm 기반 외부 태그 수집
- System Tag 계산 및 조회
- 게시글 작성 / 조회 / 수정 / 삭제
- User Tag 작성 및 게시글 연결
- 좋아요
- 알림
- 태그 기반 탐색

---

## 2. 개발 역할 및 원칙

당신은 TagNote 서버를 설계하고 구현하는 시니어 백엔드 엔지니어다.

구현 시 다음 원칙을 우선한다.

- Clean Code
- 읽기 쉬운 명명
- 명확한 책임 분리
- Layered Architecture
- DDD 관점의 도메인 모델링
- 과도한 추상화 방지
- 비즈니스 규칙을 Controller / Repository에 두지 않음
- DB 제약조건을 데이터 정합성의 최종 방어선으로 활용
- N+1 및 불필요한 SELECT 방지
- 불필요한 JPA 양방향 연관관계 지양
- 외부 API 장애가 전체 서비스 장애로 전파되지 않도록 설계
- 주요 쓰기 로직에서 동시성과 idempotency 고려
- MVP에서 Kafka / Microservice / Graph DB 등 과설계 지양

---

## 3. 아키텍처

Layered Architecture를 사용한다.

```text
Presentation
    ↓
Application
    ↓
Domain
    ↓
Infrastructure
```

실제 호출 구조:

```text
Controller
   ↓
Application Service
   ├─ Domain Entity / Domain Service
   ├─ Repository
   └─ External API Client
```

### Presentation

- Controller
- Request / Response DTO
- 요청 validation
- 인증 사용자 처리
- HTTP status / error mapping

### Application

- Use Case orchestration
- 트랜잭션 경계
- Repository 호출
- 외부 Client 호출 순서 제어
- Domain Service 실행

예:

```text
TrackSearchService
TrackImportService
BoardService
LikeService
NotificationService
TagResolutionService
```

### Domain

- Entity
- Aggregate
- Value Object
- Domain Service
- 비즈니스 규칙

### Infrastructure

- JPA / DB
- Spotify Client
- MusicBrainz Client
- Discogs Client
- Last.fm Client
- Scheduler / Batch
- Cache
- 외부 응답 DTO / Mapper

---

## 4. 도메인

### 4.1 태그 기능

상세 구현:

```text
D:\p\tag-your-story\agents\server\_tag_feature_architecture.md
```

#### System Tag

음악 장르/스타일/씬/국가/시대 정규화용 태그.

```text
GENRE
STYLE
SCENE
COUNTRY
ERA
```

사용자 화면에서는 최종 계산 결과인 `subject_tag_resolved`를 조회한다.

#### User Tag

사용자가 게시글에 직접 추가하는 자유 태그.

System Tag와 User Tag는 같은 테이블에 섞지 않는다.

---

## 5. User

```text
user

- user_id
- user_key
- email
- nickname
- role
- user_status
- created_at
- updated_at
```

### Role

```text
GUEST
PENDING_USER
USER
ADMIN
```

권장:

- `GUEST`는 미인증 요청의 권한 표현으로 사용
- 실제 DB User row에는 `PENDING_USER`, `USER` 위주로 저장

### UserStatus

```text
ACTIVE
SUSPENDED
WITHDRAWN
```

### 제약 / 인덱스

```text
PK(user_id)
UNIQUE(user_key)
UNIQUE(email)
UNIQUE(nickname)
INDEX(user_status)
```

`user_key`는 외부에 노출 가능한 UUID 성격의 식별자로 사용한다.

### User ↔ Board

User에 `boardList` 컬럼을 만들지 않는다.

```text
User 1
 ↑
 │ board.user_id FK
 │
Board N
```

JPA에서도 양방향 컬렉션을 필수로 두지 않는다.

사용자의 게시글 목록은 Repository query로 조회한다.

---

## 6. Music Catalog

### Artist

```text
artist

- artist_id
- name
- spotify_id
- musicbrainz_id
- created_at
- updated_at
```

제약:

```text
PK(artist_id)
UNIQUE(spotify_id)
INDEX(musicbrainz_id)
INDEX(name)
```

### Album

```text
album

- album_id
- title
- spotify_id
- musicbrainz_id
- release_year
- created_at
- updated_at
```

의미:

```text
album.musicbrainz_id
= MusicBrainz Release Group MBID
```

제약:

```text
PK(album_id)
UNIQUE(spotify_id)
INDEX(musicbrainz_id)
INDEX(title)
```

Album Artist 전체는 `album_artist` 연결 테이블에 Spotify 표시 순서와 함께 저장한다.

### Track

```text
track

- track_id
- title
- spotify_id
- musicbrainz_id
- isrc
- duration_ms
- album_id
- created_at
- updated_at
```

의미:

```text
track.musicbrainz_id
= MusicBrainz Recording MBID
```

제약:

```text
PK(track_id)
FK(album_id → album.album_id)
UNIQUE(spotify_id)
INDEX(musicbrainz_id)
INDEX(isrc)
INDEX(album_id)
INDEX(title)
```

Track Artist 전체는 `track_artist` 연결 테이블에 Spotify 표시 순서와 함께 저장한다.

### AlbumArtist

```text
album_artist

- album_artist_id
- album_id
- artist_id
- position
```

제약:

```text
PK(album_artist_id)
FK(album_id → album.album_id)
FK(artist_id → artist.artist_id)
UNIQUE(album_id, artist_id)
UNIQUE(album_id, position)
INDEX(artist_id, album_id)
```

### TrackArtist

```text
track_artist

- track_artist_id
- track_id
- artist_id
- position
```

제약:

```text
PK(track_artist_id)
FK(track_id → track.track_id)
FK(artist_id → artist.artist_id)
UNIQUE(track_id, artist_id)
UNIQUE(track_id, position)
INDEX(artist_id, track_id)
```

`position`은 Spotify Artist 배열의 0-based 표시 순서이며 `position=0`을 대표 Artist로 해석한다. 별도의 대표 Artist FK를 중복 저장하지 않는다. Album, Track, Artist에는 불필요한 양방향 Artist credit 컬렉션을 두지 않고 연결 Entity를 명시적 Repository query로 조회한다.

---

## 7. Board

```text
board

- board_id
- user_id
- track_id
- content
- status
- read_count
- like_count
- created_at
- updated_at
```

### BoardStatus

```text
PUBLISHED
HIDDEN
DELETED
```

정책:

- `PUBLISHED`: 일반 노출
- `HIDDEN`: 운영 정책에 의한 비노출
- `DELETED`: soft delete

관계:

```text
board.user_id → user.user_id
board.track_id → track.track_id
```

Board는 반드시 하나의 Track을 대상으로 작성한다.

제약 / 인덱스:

```text
PK(board_id)
FK(user_id → user.user_id)
FK(track_id → track.track_id)

INDEX(user_id, created_at)
INDEX(track_id, created_at)
INDEX(status, created_at)
INDEX(like_count)
```

### Count 정책

`like_count`, `read_count`는 조회 성능용 denormalized counter다.

```text
board_like
= 좋아요 관계 source of truth

board.like_count
= 조회용 counter
```

좋아요 생성/삭제 시 counter는 atomic update를 사용한다.

---

## 8. UserTag

```text
user_tag

- user_tag_id
- user_id
- name
- created_at
- updated_at
```

제약:

```text
PK(user_tag_id)
FK(user_id → users.user_id)
UNIQUE(user_id, name)
INDEX(name, user_id)
```

`UserTag`는 전역 공유 taxonomy가 아니라 사용자별 플레이리스트형 개인 태그다.

- 같은 사용자가 정확히 같은 `name`을 반복 사용하면 기존 `user_tag_id`를 재사용한다.
- 서로 다른 사용자가 같은 `name`을 사용하면 서로 다른 `user_tag_id`를 생성한다.
- `name`은 trim, lowercase, Unicode normalization 또는 공백 축약 없이 입력값 그대로 저장하고 exact match로 판정한다.
- 대소문자, Unicode 표현 또는 공백이 다른 이름은 서로 다른 UserTag다.
- 운영 DB의 `name`은 case-sensitive binary collation을 사용해 exact unique/query 의미를 보장한다.
- Board에 연결되는 UserTag의 owner는 Board 작성자와 같아야 한다.
- 여러 Track의 Board에 같은 UserTag를 연결하면 이름별 개인 컬렉션처럼 조회할 수 있다.
- System Tag의 `tag`, `tag_alias`, `subject_tag_resolved`와 Entity, Repository, Service, 테이블을 공유하지 않는다.

### BoardUserTag

```text
board_user_tag

- board_user_tag_id
- board_id
- user_tag_id
- created_at
```

제약:

```text
PK(board_user_tag_id)
FK(board_id → board.board_id)
FK(user_tag_id → user_tag.user_tag_id)

UNIQUE(board_id, user_tag_id)
INDEX(user_tag_id, board_id)
```

---

## 9. BoardLike

```text
board_like

- like_id
- user_id
- board_id
- created_at
```

제약:

```text
PK(like_id)
FK(user_id → user.user_id)
FK(board_id → board.board_id)

UNIQUE(user_id, board_id)

INDEX(board_id, created_at)
INDEX(user_id, created_at)
```

핵심 규칙:

```text
한 사용자는 하나의 Board에 한 번만 좋아요 가능
```

---

## 10. Notification
```text
notification

- notification_id
- publisher_user_id
- subscriber_user_id
- type
- target_type
- target_id
- is_read
- created_at
```

### NotificationType

```text
LIKE
SUBSCRIBED_TAG
SYSTEM
```

### NotificationTargetType

```text
BOARD_LIKE
TAG
SYSTEM
```

```text
publisher_user_id = 좋아요를 누른 사용자
subscriber_user_id = 게시글 작성자
type = LIKE
target_type = BOARD
target_id = board_id
```

System 알림은 `publisher_user_id`가 NULL일 수 있다.

제약:

```text
PK(notification_id)
FK(publisher_user_id → user.user_id) NULLABLE
FK(subscriber_user_id → user.user_id)

INDEX(subscriber_user_id, is_read, created_at)
INDEX(type, created_at)
INDEX(target_type, target_id)
```

---

## 11. Tag Subscription

`SUBSCRIBED_TAG` 알림을 사용할 경우 구독 관계가 필요하다.
시스템 태그만 구독 대상. 유저 태그는 구독 불가.

```text
tag_subscription

- tag_subscription_id
- user_id
- tag_id
- created_at
```

제약:

```text
PK(tag_subscription_id)
FK(user_id → user.user_id)
FK(tag_id → tag.tag_id)

UNIQUE(user_id, tag_id)
INDEX(tag_id, user_id)
```

흐름:

```text
사용자가 System Tag 구독
↓
해당 Tag로 resolved된 Track, Album이 발견(계산)되었을 때
↓
SUBSCRIBED_TAG 알림 후보 생성
```

---

## 12. System Tag 데이터

상세 구조와 Resolver 정책은 `D:\p\tag-your-story\agents\server_tag_feature_architecture.md`를 따른다.

핵심 테이블:

```text
tag
tag_alias
tag_parent
tag_association

external_tag_observation
tag_assertion
subject_tag_resolved
```

선택:

```text
subject_enrichment_status
external_entity_mapping
```

MVP 제외:

```text
label
review text extraction
artist profile inheritance
label profile inheritance
fusion UI
graph recommendation
```

---

## 13. 주요 플로우

### 13.1 Track 검색

```text
User
↓
GET /api/tracks/search?q=
↓
Spotify API
↓
검색 결과 반환
```

검색 결과 표시 단계에서는 내부 DB 저장이 필수가 아니다.

### 13.2 Track 선택 / Import

```text
Spotify Track 선택
↓
spotify_id로 내부 Track 조회

존재
→ 기존 Track 사용

미존재
→ Spotify metadata 조회
→ Artist upsert
→ Album upsert
→ Track upsert
→ 외부 enrichment
```

Track import 완료 후 Board 작성 가능.

### 13.3 MusicBrainz Entity Matching

```text
Spotify Track
↓
ISRC / title / artist / duration
↓
MusicBrainz 검색
↓
Recording match
↓
track.musicbrainz_id 저장
↓
Release Group 확인
↓
album.musicbrainz_id 저장
```

MusicBrainz 역할:

```text
Identity
Relationship
Tag Evidence
```

최종 장르 판단자는 아니다.

### 13.4 External Tag 수집

```text
MusicBrainz / Discogs / Last.fm
↓
raw genre/style/community tag
↓
external_tag_observation
```

raw tag는 매칭 실패 여부와 관계없이 저장한다.

### 13.5 Matching

```text
external_tag_observation.normalized_name
↓
tag_alias.normalized_alias
↓
exact match
```

성공:

```text
MATCHED
```

실패:

```text
NEW
```

`NEW`는 음악 전체 처리 미완료가 아니라 raw tag 하나의 미해석 상태다.

### 13.6 Assertion

```text
MATCHED observation
↓
tag_assertion
```

Assertion의 의미:

```text
"왜 이 Subject에 이 Tag가 붙을 수 있는가?"
```

### 13.7 Resolver

```text
tag_assertion
+
inheritance rule
+
manual state
↓
TagResolver
↓
subject_tag_resolved
```

### 13.8 Track 상세 조회

```text
Track
Album
Artist
subject_tag_resolved
↓
Response
```

화면에서 `tag_assertion`을 직접 조회하지 않는다.

### 13.9 Board 작성

```text
인증 User
↓
Track 선택
↓
Track 확보
↓
content 작성
↓
작성자 user_id + 입력 name exact match 기준 find-or-create
↓
Board 저장
↓
BoardUserTag 저장
```

### 13.10 Board 조회

```text
Board
↓
작성자
User Tags
Like 상태
↓
Response
```

### 13.11 Like

등록:

```text
board_like insert
↓
board.like_count + 1
↓
LIKE notification 생성
```

취소:

```text
board_like delete
↓
board.like_count - 1
```
(Atomic update)

### 13.12 Notification Read

```text
PATCH /notifications/{id}/read
↓
is_read = true
```

본인 알림만 변경 가능하다.

---

## 14. Polymorphic Subject 처리

태그 기능은:

```text
subject_type + subject_id
```

를 사용한다.

일반 FK는 `subject_type`에 따라 대상 테이블이 달라지므로 직접 설정하지 않는다.

Observation / Assertion 생성 시 이미 조회하거나 생성한 Track / Album Entity로 `SubjectRef`를 만든다.

```java
Track track = trackRepository.findById(trackId)
    .orElseThrow();

SubjectRef subject = SubjectRef.track(track.getId());

tagAssertionService.create(subject, ...);
```

Album도 동일하다.

```java
Album album = albumRepository.findById(albumId)
    .orElseThrow();

SubjectRef subject = SubjectRef.album(album.getId());

tagAssertionService.create(subject, ...);
```

별도의 `existsById()`를 반복 실행하지 않는다.

```java
public record SubjectRef(
    SubjectType type,
    Long id
) {
    public static SubjectRef track(Long trackId) {
        return new SubjectRef(SubjectType.TRACK, trackId);
    }

    public static SubjectRef album(Long albumId) {
        return new SubjectRef(SubjectType.ALBUM, albumId);
    }
}
```

---

## 15. 트랜잭션 정책

### 외부 HTTP

외부 API 호출을 긴 DB 트랜잭션 안에 포함하지 않는다.

```text
외부 데이터 확보
↓
DB Transaction
↓
저장 / 계산
↓
commit
```

1. Spotify 검색
   → 검색 결과만 반환

2. 사용자 Track 선택

3. 내부 DB Track 확인

4. Track/Album/Artist가 없으면 저장
   → 짧은 Transaction

5. Spotify metadata 확보 후 MusicBrainz + Discogs + Last.fm 병렬 호출
   → Transaction 없음

6. 성공 결과 취합

7. @Transactional
   ├─ Observation 저장
   ├─ Alias Matching
   ├─ Assertion 저장
   ├─ Resolver 계산
   └─ Resolved 저장

8. COMMIT

9. resolved tag 포함 Response

### Board 생성

하나의 트랜잭션:

```text
Board insert
+
작성자 기준 UserTag find-or-create
+
BoardUserTag insert
```

### Like

하나의 트랜잭션:

```text
board_like insert/delete
+
board.like_count atomic update
```

Notification은 초기에는 같은 use case에서 처리할 수 있으나 규모가 커지면 AFTER_COMMIT 이벤트로 분리한다.

---

## 16. 동시성 / Idempotency

### Track Import

```text
UNIQUE(artist.spotify_id)
UNIQUE(album.spotify_id)
UNIQUE(track.spotify_id)
```

동일 Spotify Track의 동시 최초 import를 DB unique constraint로 최종 방어한다.

### UserTag

```text
UNIQUE(user_tag.user_id, user_tag.name)
UNIQUE(board_user_tag.board_id, board_user_tag.user_tag_id)
```

전역 `UNIQUE(user_tag.name)`를 사용하지 않는다. 같은 name이라도 owner가 다르면 서로 다른 UserTag row가 정상 데이터다.

### Like

```text
UNIQUE(user_id, board_id)
```

### Tag Pipeline

Observation / Assertion / Resolved는 태그 기능 문서의 unique constraint를 유지한다.

Resolver는 동일 입력에 대해 반복 실행해도 동일 결과를 만들어야 한다.

---

## 17. JPA 조회 정책

기본:

```text
ManyToOne → LAZY 우선
OneToMany → LAZY
```

목록 조회에서는 필요한 경우:

```text
fetch join
projection
query DTO
```

를 사용한다.

다음과 같은 큰 컬렉션을 무조건 Entity에 양방향으로 들고 있지 않는다.

```text
User.boardList
Board.likes
Board.boardUserTags
```

---

## 18. 삭제 정책

```text
User → WITHDRAWN
Board → DELETED
Like → physical delete 가능
Notification → 일정 기간 보관 후 삭제 가능
System Tag → DEPRECATED / MERGED 우선
```

---

## 19. Batch / Scheduler

### NEW Observation 재매칭

매일:

```text
NEW external_tag_observation
↓
approved tag_alias match
↓
MATCHED
↓
tag_assertion
↓
Resolver
```

### External Enrichment 재시도

대상:
subject_enrichment_status
```text
TIMEOUT
FAILED
```

`NOT_FOUND`는 빈번한 자동 재시도 대상에서 제외한다.

### Notification 정리

추후:
선택적으로 오래된 읽은 알림을 일정 기간 뒤 삭제한다.

---

## 20. API 공통 정책

Base:

```text
/api
```

Admin:

```text
/api/admin
```

ErrorCode 예:

```text
USER_NOT_FOUND
TRACK_NOT_FOUND
BOARD_NOT_FOUND
BOARD_ACCESS_DENIED
BOARD_DELETED
DUPLICATE_LIKE
TAG_NOT_FOUND
INVALID_USER_TAG
EXTERNAL_PROVIDER_TIMEOUT
EXTERNAL_PROVIDER_NOT_FOUND
```

외부 Provider의 raw error message를 사용자에게 그대로 노출하지 않는다.

---

## 21. API 초안

### Track 검색

```http
GET /api/tracks/search?q={keyword}
```

Response:

```json
[
  {
    "spotifyTrackId": "spotify-id",
    "title": "Track",
    "artistName": "Artist",
    "albumTitle": "Album",
    "imageUrl": "..."
  }
]
```

### Track Import

```http
POST /api/tracks/import
```

Request:

```json
{
  "spotifyTrackId": "spotify-id"
}
```

Response:

```json
{
  "trackId": 100,
  "title": "Track",
  "artist": {
    "artistId": 10,
    "name": "Artist"
  },
  "album": {
    "albumId": 20,
    "title": "Album"
  },
  "systemTags": [
    {
      "tagId": 1,
      "name": "Ambient",
      "score": 0.91
    }
  ],
  "enrichmentStatus": "SUCCESS"
}
```

### Track 상세

```http
GET /api/tracks/{trackId}
```

### Board 생성

```http
POST /api/boards
```

Request:

```json
{
  "trackId": 100,
  "content": "이 곡의 후반부가 정말 좋다.",
  "userTags": [
    "새벽감성",
    "비오는날"
  ]
}
```

### Board 상세

```http
GET /api/boards/{boardId}
```

Response 예:

```json
{
  "boardId": 1000,
  "content": "이 곡의 후반부가 정말 좋다.",
  "author": {
    "userKey": "uuid",
    "nickname": "listener"
  },
  "track": {
    "trackId": 100,
    "title": "Track",
    "artistName": "Artist"
  },
  "userTags": [
    {
      "userTagId": 20,
      "name": "새벽감성"
    }
  ],
  "readCount": 120,
  "likeCount": 15,
  "likedByMe": true,
  "createdAt": "..."
}
```

### Board 목록

```http
GET /api/boards
```

Query 후보:

```text
cursor
size
sort
trackId
tagId
userTagId
userKey
```

대량 목록은 cursor pagination을 우선 검토한다.

### Board 수정

```http
PATCH /api/boards/{boardId}
```

수정 가능:

```text
content
userTags
```

Track 변경은 기본적으로 허용하지 않는다.

### Board 삭제

```http
DELETE /api/boards/{boardId}
```

### Like

```http
POST   /api/boards/{boardId}/likes
DELETE /api/boards/{boardId}/likes
```

### UserTag 검색

```http
GET /api/user-tags/search?q={keyword}
```

- 인증 사용자의 개인 UserTag만 검색한다.
- `(현재 user_id, name)` exact match 범위에서 조회하며 다른 사용자의 UserTag ID를 반환하지 않는다.

### UserTag 기준 공개 Board 조회

```http
GET /api/boards/user-tags?userTagName={name}
```

- 인증은 필요하지 않다.
- `userTagName`을 변형하지 않고 정확히 같은 `name`을 가진 모든 사용자의 UserTag를 대상으로 조회한다.
- 단일 `user_tag_id`를 임의로 선택하지 않는다.
- `POST` 상태 Board만 중복 없이 반환한다.
- 응답 Board의 `trackId`를 통해 해당 이름으로 분류된 Track을 확인할 수 있다.

### 사용자 프로필

```http
GET /api/users/{userKey}
```

### 사용자 게시글

```http
GET /api/users/{userKey}/boards
```

### 알림 목록

```http
GET /api/notifications
```

Query:

```text
cursor
size
unreadOnly
```

### 알림 읽음

```http
PATCH /api/notifications/{notificationId}/read
PATCH /api/notifications/read-all
```

### System Tag 구독

선택:

```http
POST   /api/tags/{tagId}/subscriptions
DELETE /api/tags/{tagId}/subscriptions
```

---

## 22. Admin API
### +) NEW Observation 재매칭과 관련된 기능인 Alias 승인 - 어드민 기능

```text
TagAliasApproved
↓
동일 normalized_name NEW observation 조회
↓
MATCHED
↓
Assertion
↓
Resolved 재계산
```

```http
POST  /api/admin/tags
POST  /api/admin/tags/{tagId}/aliases
PATCH /api/admin/tag-aliases/{aliasId}/approve
PATCH /api/admin/tag-aliases/{aliasId}/reject
POST  /api/admin/tags/{tagId}/merge

GET   /api/admin/external-tags/unmatched
```

추후:

```text
Board hide
User suspend
System notification
```

---

## 23. 권한

### GUEST

가능:

```text
Track 검색
Track 상세 조회
Board 목록 / 상세 조회
```

불가:

```text
Board 작성
Like
Tag 구독
Notification
```

### PENDING_USER

회원가입 중 추가 정보가 완성되지 않은 상태.

### USER

일반 서비스 기능 사용 가능.

---

## 24. 외부 Provider 장애 정책

### Spotify

검색과 Track Import의 핵심 provider.

Spotify metadata를 얻지 못하면 신규 Track Import는 실패할 수 있다.

### MusicBrainz / Discogs / Last.fm

Enrichment provider.

하나가 실패해도:

```text
성공한 provider 데이터
↓
Observation
↓
Assertion
↓
Resolver
↓
Partial Result
```

을 제공한다.

### Evidence Confidence 정책 결정 시점

`confidence`의 결합 방식과 노출 기준을 구현하는 것과, 외부 Provider 응답에 최초 `confidence`를 부여하는 것은 별개의 결정이다.

구현 순서상 정책 확정 시점은 다음과 같다.

```text
Tag Core
→ max(confidence), minimum score, inheritance 계산 구조 구현
→ First Vertical Slice
   - Fake External Tag에 테스트용 고정 confidence 사용 가능
   - production Provider 정책으로 간주하지 않음
→ [Policy Gate / Human Review]
   - MusicBrainz/Discogs/Last.fm evidence별 기본 confidence 확정
   - Entity Matching confidence와 Tag Evidence confidence의 결합 여부 확정
   - Provider vote/count 반영 여부 확정
→ External Enrichment 구현 착수
```

따라서 MusicBrainz/Discogs/Last.fm HTTP Adapter와 응답 매핑을 구현하기 전에 다음 항목이 Plan 또는 필요 시 ADR로 승인되어야 한다.

- Provider별 evidence 종류와 기본 confidence
- Entity Matching 통과 기준
- Entity Matching confidence를 Tag Evidence confidence에 곱할지 여부
- 외부 vote/count/popularity 반영 여부와 정규화 방식
- 누락되거나 신뢰할 수 없는 evidence의 제외 정책

정책 확정 전에는 실제 Provider 값을 임의로 하드코딩하지 않는다. First Vertical Slice의 Fake 값은 Observation → Assertion → Resolver → Track Detail 연결을 검증하기 위한 fixture로만 사용한다.

---

## 25. Cache

MVP에서는 DB read model을 우선 사용한다.

Redis는 필수 의존성이 아니다.

추후 후보:

```text
인기 Track 상세
Resolved Tag list
인기 Board 목록
```

Cache는 source of truth가 아니다.

---

## 26. 패키지 구조

```text
com.tagnote
│
├─ presentation
│  ├─ user
│  ├─ track
│  ├─ board
│  ├─ tag
│  ├─ like
│  └─ notification
│
├─ application
│  ├─ user
│  ├─ catalog
│  ├─ board
│  ├─ taxonomy
│  ├─ enrichment
│  ├─ resolution
│  ├─ like
│  └─ notification
│
├─ domain
│  ├─ user
│  ├─ catalog
│  │  ├─ artist
│  │  ├─ album
│  │  └─ track
│  ├─ board
│  ├─ usertag
│  ├─ taxonomy
│  ├─ enrichment
│  ├─ resolution
│  ├─ like
│  └─ notification
│
├─ infrastructure
│  ├─ persistence
│  ├─ external
│  │  ├─ spotify
│  │  ├─ musicbrainz
│  │  └─ discogs
│  ├─ scheduler
│  └─ cache
│
└─ shared
   ├─ config
   ├─ exception
   ├─ security
   └─ util
```

---

## 27. MVP 범위

필수:

```text
User

Artist
Album
Track

System Tag pipeline

Board
UserTag
BoardUserTag

Like

기본 Notification

Spotify search
Track detail
Board CRUD
```

선택:

```text
System Tag subscription
Notification fan-out
Redis cache
```

2차 확장:

```text
Comment
User Follow
Report / Block
Label
Review extraction
Recommendation graph
User activity feed
```

---

## 28. 핵심 데이터 관계

```text
User
  │
  ├────< Board >──── Track
  │                    │
  │                    ├─ Album
  │                    └─ Artist
  │
  ├────< BoardLike >── Board
  │
  ├────< Notification
  │
  └────< TagSubscription >──── SystemTag


Board
  │
  └────< BoardUserTag >──── UserTag


Track / Album
  │
  └──── ExternalTagObservation
             │
             ▼
        TagAssertion
             │
             ▼
     SubjectTagResolved
             │
             ▼
          SystemTag
```

---

## 29. 핵심 설계 원칙 요약

1. Spotify는 사용자 음악 검색의 단일 진입점이다.
2. Artist / Album / Track을 별도 Catalog Entity로 관리한다.
3. MusicBrainz는 Identity / Relationship / Tag Evidence 역할을 한다.
4. Discogs는 Album Genre / Style evidence를 보강한다.
5. Last.fm은 Track / Album Community Tag evidence를 보강하며 provider count를 confidence에 직접 반영하지 않는다.
6. 외부 raw tag는 Observation에 보존한다.
7. System Tag와 User Tag를 분리한다.
8. Assertion은 근거이고 Resolved는 사용자 노출용 read model이다.
9. Board는 하나의 Track을 참조한다.
10. Board의 System Tag는 Track의 resolved tag를 통해 조회한다.
11. BoardUserTag는 Board와 작성자 소유 UserTag의 N:M 관계이며, UserTag identity는 입력값을 보존하는 `(user_id, name)` 단위다.
12. User가 Board 목록 FK를 소유하지 않는다.
13. Like 관계가 source of truth이고 `board.like_count`는 조회용 counter다.
14. Notification은 `target_type + target_id`로 대상의 의미를 명시한다.
15. 외부 Provider 실패는 가능한 범위에서 partial success로 처리한다.
16. DB unique / FK / index를 데이터 정합성의 최종 방어선으로 사용한다.
17. MVP에서는 읽기 쉬운 Layered Architecture를 유지하고 과도한 인프라 추상화를 피한다.
