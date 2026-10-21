# TagNote React API 계약 기준선

이 문서는 최신 서버 worktree를 정적으로 조사한 도메인·API별 기록이다. 실행 서버 계약을 검증한 결과가 아니며, 구현 범위는 해당 active Plan을 따른다. 새 세션의 읽기 순서와 계약 대조 규칙은 `agents/client/AGENTS.md`를 따른다.

## 근거와 갱신 규칙

- 기준 서버: `D:\p\tag-your-story`의 `feature/migration-server` worktree, 2026-10-20 커밋 `afbab28`. 이 클라이언트 worktree 안의 서버 코드는 최신 기준이 아니다.
- 근거 순서: 최신 서버 worktree의 공개 `*Api.java` 문서 → 구현 Controller → Request/Response DTO → 인증 필터·예외 처리·관련 ADR. 공개 `*Api`가 없는 도메인은 그 사실을 표시한다.
- 실행 서버의 OpenAPI와 실제 HTTP 응답은 이 문서 작성 시 확인하지 않았다. 이 환경에서 Swagger 접속을 시도하지 않는다. 사용자가 curl 응답을 전달하면 별도의 실행 근거로 기록한다. 정적 문서와 Controller/DTO 또는 실행 응답이 충돌하면 임의로 선택하지 말고 보고한다.
- 최신 서버 커밋이 바뀌면 작업 대상 API의 근거와 확인 날짜를 갱신한다. 명시되지 않은 endpoint, 응답 필드, 인증 동작은 계약으로 취급하지 않는다.
- 서버 파일 경로의 기준은 `tagnote-api/src/main/java/com/tagnote/api/`이다. 아래 `domain/...`, `oauth/...`, `filter/...`는 이 경로에 상대적이다.

## 공통 wire 계약

- 성공 응답: `{ "success": true, "response": ... }`. `response`의 내부 구조는 API별 DTO를 확인한다.
- Controller 예외: `{ "success": false, "response": { "exceptionCode", "message", "status" } }`. JWT 필터/인가 오류: wrapper 없는 `{ "exceptionCode", "message", "status" }`. `CustomExceptionManager`와 `CustomExceptionDeniedHandler` 근거다. token 만료·위변조·invalid refresh는 실제 HTTP 401이며 다른 custom error는 HTTP 200일 수 있다. `status` 필드와 HTTP status를 혼동하지 않는다.
- 인증 요청은 `Authorization` 헤더의 JWT를 읽는다. `JwtAuthorizationFilter`는 `Authorization` 쿼리 파라미터도 읽는다. 일반 cookie를 API 인증에 사용하는 동작은 확인되지 않았다. 로그인/OAuth가 발행한 token cookie를 React가 저장소로 옮기는 것은 별도 클라이언트 동작이다.
- UI page가 1-based라도 API page가 0-based인 경로는 service 또는 mapper 한 곳에서 변환한다. 아래에서 명시하지 않은 page 기준은 추측하지 않는다.
- React 호출은 `tag-note-react/src/services/`, 인증 공유 상태는 `src/store/authStore.js`, 브라우저 저장소는 `src/utils/`를 통한다. 실제 호출자와 임시 alias를 확인한 후에만 제거한다.

## 인증 · 사용자

근거: `domain/user/UserApi.java`, `UserController.java`, `domain/auth/EmailLoginApi.java`, `EmailSignupApi.java`와 각 Controller/DTO, `oauth/GoogleOauthOpenApi.java`, OAuth handler, `filter/JwtAuthorizationFilter.java`, `agents/server/decisions/ADR-015-google-immediate-account-and-canonical-auth.md`.

- `POST /api/auth/login` — 공개. email/password 입력, 성공 `response.userId`, `response.nickname` 및 token cookie. 현재 React의 이메일 로그인 화면/서비스는 없다.
- `GET /api/auth/email-availability?email=` — 공개. email 사용 가능 여부 조회. 현재 React 호출 없음.
- `POST /api/auth/signup/email/start` — 공개. email/password/passwordConfirm 입력, `challengeId` 반환. 현재 React 호출 없음.
- `POST /api/auth/signup/email/resend` — 공개. `challengeId` 입력. 현재 React 호출 없음.
- `POST /api/auth/signup/email/confirm` — 공개. `challengeId`와 6자리 code 입력, `userId`/`nickname` 및 token cookie. 현재 React 호출 없음.
- `GET /oauth2/authorization/google` — 공개. Google 동의 화면으로 302 redirect. Request body 없음.
- `GET /login/oauth2/code/google?code=&state=` — Google 서버가 호출하는 callback. 성공·실패 모두 frontend로 302 redirect한다. 기존·신규 Google 사용자는 성공 시 즉시 ACTIVE USER이며 Authorization/RefreshToken cookie를 받는다. 실패 redirect의 `error_code`는 `GOOGLE_EMAIL_UNVERIFIED`, `GOOGLE_IDENTITY_INVALID`, `GOOGLE_EMAIL_CONFLICT_LOCAL`, `GOOGLE_EMAIL_CONFLICT_GOOGLE`, `USER_INACTIVE`, `GOOGLE_SIGNUP_CONFLICT` 중 하나다. 서버 redirect 설정은 `/token.html`, React callback route는 `/token`으로 불일치한다. 실제 브라우저 redirect는 미검증이다.
- `POST /api/user/reissue/accessToken` — 별도 Authorization 헤더 없이 `{ "refreshToken": "..." }`, 성공 `response.newAccessToken`. `UserApi`는 `GOOGLE_RELOGIN_REQUIRED`를 HTTP 401로 문서화하지만 `CustomExceptionManager` 구현은 이 코드를 HTTP 200 wrapped error로 반환한다. 충돌로 유지한다.
- `POST /api/user/reissue/refreshToken` — USER 인증. `response.newRefreshToken`; 이전 refresh token 무효화. React `authService`에 함수가 있으나 실제 호출자는 별도 확인한다.
- `POST /api/user/logout` — USER 인증. 서버 refresh token을 제거하지만 access token은 만료 전까지 유효하다. React는 logout 후 로컬 상태도 clear한다.
- `PATCH /api/user/me/nickname` — USER 인증. nickname 1~20자, 공백 불가; `response.nickname`. 기존 pending 가입의 대체 endpoint가 아니다.
- `DELETE /api/user/me` — USER 인증. 탈퇴 및 cookie 만료. 현재 React 호출 없음.
- `/api/user/register`와 `/api/user/check-registration` — 최신 Controller에 없다. 전자는 ADR-015에서 제거했다. React `authService` 및 `NicknamePage`에 과거 호출이 남아 있으므로 M2-2에서 실제 호출자를 확인해 정리한다. pending user → nickname 등록은 현재 서버 계약이 아니다.

## Catalog · Track

근거: `domain/catalog/CatalogSearchApi.java`, `CatalogImportApi.java`, 각 Controller/DTO; `domain/tracks/TrackApi.java`, `TrackController.java`와 DTO.

- `GET /api/catalog/search?keyword=&page=` — 공개, page 0-based. `response.items`와 `totalCount`; 항목은 `subjectType`, `spotifyId`, `title`, nullable `artistName`, `albumName`, `imageUrl`. TRACK/ALBUM/ARTIST의 nullable 필드가 다르므로 같은 모양으로 가정하지 않는다. 현재 React 전용 catalog service 없음.
- `POST /api/catalog/import` — 공개. `{ "subjectType": "TRACK|ALBUM", "spotifyId": "..." }`; `response.subjectType`과 해당 `track` 또는 `album`. ARTIST import는 지원하지 않는다. 부분 provider 실패 시 성공한 import 결과를 버리지 않는 서버 정책이 있다.
- `GET /api/tracks?keyword=&page=` — 공개, page 0-based. `response.trackDataList`, `totalCount`. 현재 `trackService`가 사용한다.
- `GET /api/tracks/{trackId}` — 공개. `TrackData` DTO. 현재 `trackService`가 사용한다. 세부 필드는 해당 DTO에서 다시 확인한다.
- `GET /api/tracks/ranking` — 공개. `response.keywordList`, 최대 5개. 현재 `trackService`가 사용한다.
- `POST /api/tracks/import` — 공개, `spotifyTrackId` 입력. deprecated이며 현재 React 직접 호출은 발견되지 않았다. 신규 Catalog 구현에서 재사용하지 않는다.

## Board

근거: `domain/board/BoardApi.java`, `BoardController.java`와 Request/Response DTO.

- `POST /api/boards` — USER. `content`, 비어 있지 않은 `userTagList`, `trackId` 또는 `albumId` 중 정확히 하나. 생성된 board 정보 반환.
- `GET /api/boards/{trackId}?order-type=CREATED_AT|LIKE&page=0` — 공개. Track Board 목록, page 0-based, 크기 8; `boardResponseList`, `totalCount`.
- `GET /api/boards/albums/{albumId}?order-type=CREATED_AT|LIKE&page=0` — 공개. Album Board 목록, 같은 정렬·page 계약. 현재 React의 Track 중심 호출과 구분한다.
- `GET /api/boards?boardId=` — 공개. 상세 DTO는 `content`, `nickname`, `likeCount`, `createdAt`, `userTagNameList`, `subjectType`, `trackId`, `albumId`, `systemTags`를 포함한다.
- `GET /api/boards/count/{trackId}` 및 `GET /api/boards/count/albums/{albumId}` — 공개. 각 대상 Board count.
- `GET /api/boards/user-tags?userTagName=` — 공개. 정확한 태그명에 해당하는 Board 목록.
- `GET /api/boards/auth/{boardId}` — USER. 작성자 여부 boolean.
- `PATCH /api/boards` — USER 및 작성자. `boardId`, `content`, `userTagList`.
- `DELETE /api/boards/{boardId}` — USER 및 작성자. soft delete.

## Comment · Like

이 두 도메인에는 공개 `*Api` 문서가 없다. 근거: `domain/comment/CommentController.java`, `domain/like/LikeController.java`와 각 DTO/권한 선언. page 기준 등 명시되지 않은 값은 구현 전 재확인한다.

- `POST /api/comments` — USER. `boardId`, `content`; `CommentResponse`는 `commentId`, `parentId`, `nickname`, `content`, `createdDate`.
- `PATCH /api/comments` — USER. `commentId`, `content`.
- `PATCH /api/comments/status/{commentId}` — USER. 댓글 삭제.
- `GET /api/comments/{boardId}/{page}` — 공개. `comment`와 `children`을 가진 댓글 목록.
- `GET /api/comments/auth/{boardId}` — USER. 작성 가능한 comment ID 목록.
- `GET /api/comments/count/{boardId}` — 공개. 댓글 수.
- `POST /api/comments/replies` — USER. `boardId`, `parentId`, `content`.
- `GET /api/comments/replies/{parentId}/{page}` — 공개. 답글 `CommentResponse` 목록.
- `POST /api/likes` — USER. `boardId` 입력, boolean 반환.
- `DELETE /api/likes` — USER. `boardId` 입력, 데이터 없는 성공 응답.
- `GET /api/likes/status/{boardId}` — USER. `isLiked` 반환.

## File · Notification · User Tag

이 도메인에는 공개 `*Api` 문서가 없다. 근거: `domain/file/FileController.java`, `domain/notification/NotificationController.java`, `domain/boardusertag/BoardUserTagController.java`와 DTO/권한 선언.

- `POST /api/files` — USER. multipart `fileList`, `boardId`; `fileId`, `filePath`, `fileLevel` 목록.
- `PATCH /api/files` — USER. 같은 multipart 항목으로 파일 추가/수정.
- `GET /api/files/main/{trackId}?page=` — 공개. `boardId`, `filePath` 목록.
- `GET /api/files/{boardId}` — 공개. `fileId`, `filePath` 목록.
- `DELETE /api/files` — JSON `boardId`, `fileIdList`. 현재 Controller에 `@PreAuthorize`가 없고 `SecurityConfig`도 기본 허용이며 삭제 service 경로에서 소유자 검사를 확인하지 못했다. 의도된 권한은 미확정이다. 클라이언트에서 임의로 권한을 확정하지 않는다.
- `GET /api/notification/subscription` — USER, SSE. JWT 필터의 `Authorization` query 지원은 코드로 확인했지만 cookie만으로 연결하는 모드는 확인되지 않았다.
- `GET /api/notification?page=` — USER. `notificationId`, `contentId`, `pubNickname`, `type`, `isRead` 목록.
- `PATCH /api/notification` — USER. `notificationId` 읽음 처리.
- `GET /api/notification/count` — USER. 미읽음 수.
- `PATCH /api/notification/all` — USER. 전체 읽음 처리.
- `GET /api/board-user-tags/recent` — 공개. `boardId`, `userTag` 목록.

## 현재 React 대조와 미확정 사항

- 조사 당시 API 호출은 `src/services/apiClient.js`, `authService.js`, `trackService.js`, `boardService.js`, `commentService.js`, `fileService.js`, `likeService.js`, `notificationService.js`, `userTagService.js`에 있었다. 서비스 밖의 직접 `fetch`/`EventSource`는 발견되지 않았다. 실제 호출자와 alias 사용은 각 Plan 착수 시 다시 확인한다.
- React의 access 재발급은 refresh token을 Authorization 헤더에도 보내며 `newAccessToken` 외의 필드도 허용한다. 최신 `UserApi`/DTO와 차이가 있다.
- `TokenPage`, `HomePage`, `NicknamePage`에는 제거된 pending 사용자 등록 흐름이 남아 있다. `/api/user/register`, `/api/user/check-registration`의 새 대체 endpoint를 가정하지 않는다.
- `UserApi`의 `GOOGLE_RELOGIN_REQUIRED` HTTP 401 문서와 `CustomExceptionManager`의 HTTP 200 구현은 충돌한다. Google OAuth 실제 redirect와 cookie 동작은 브라우저에서 검증하지 않았다.
- File 삭제 권한, SSE cookie 인증, 공개 `*Api`가 없는 Comment/File/Like/Notification/User Tag의 실행 schema·status는 미확정이다. 해당 API 사용 시 정적 근거와 사용자 제공 curl 응답을 구분한다.

