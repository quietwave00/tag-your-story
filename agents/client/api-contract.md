# TagNote React API 계약 기준선

이 문서는 최신 서버 worktree를 정적으로 조사한 도메인·API별 기록이다. 실행 서버 계약을 검증한 결과가 아니며, 구현 범위는 해당 active Plan을 따른다. 새 세션의 읽기 순서와 계약 대조 규칙은 `agents/client/AGENTS.md`를 따른다.

## 근거와 갱신 규칙

- 참조 서버: `D:\p\tag-your-story`의 `migration-server` worktree(`feature/migration-server`). 이 문서의 항목은 작성 당시 그 worktree에서 확인한 정적 기록이다. 이후 작업에서는 **그 worktree의 현재 코드**를 다시 확인한다. 이 클라이언트 worktree 안의 서버 코드를 최신 기준으로 사용하지 않는다.
- 근거 순서: 최신 서버 worktree의 공개 `*Api.java` 문서 → 구현 Controller → Request/Response DTO → 인증 필터·예외 처리·관련 ADR. 공개 `*Api`가 없는 도메인은 그 사실을 표시한다.
- 실행 서버의 OpenAPI와 실제 HTTP 응답은 이 문서 작성 시 확인하지 않았다. 이 환경에서 Swagger 접속을 시도하지 않는다. 사용자가 curl 응답을 전달하면 별도의 실행 근거로 기록한다. 정적 문서와 Controller/DTO 또는 실행 응답이 충돌하면 임의로 선택하지 말고 보고한다.
- 공개 API를 추가·수정·삭제하는 작업에서는 이 문서의 해당 endpoint·메서드·인증·요청·응답·오류 및 근거 날짜를 같은 변경에서 갱신한다. 삭제된 API는 호출자 확인 후 계약 목록에서 제거하거나 제거 사실을 기록한다. 명시되지 않은 endpoint, 응답 필드, 인증 동작은 계약으로 취급하지 않는다.
- 2026-10-27 갱신 근거: 현재 접근 가능한 `/mnt/d/p/tag-your-story` 서버의 Catalog/Comment/Notification/User `*Api.java` → Controller/DTO → 관련 서비스와 예외 처리 코드를 정적으로 대조했다. 지정된 `migration-server` worktree 및 실행 서버의 Swagger/HTTP 응답은 확인하지 못했으므로 아래 신규·변경 항목을 배포 검증 완료 계약으로 간주하지 않는다.
- 서버 파일 경로의 기준은 `tagnote-api/src/main/java/com/tagnote/api/`이다. 아래 `domain/...`, `oauth/...`, `filter/...`는 이 경로에 상대적이다.

## 공통 wire 계약

- 성공 응답: `{ "success": true, "response": ... }`. `response`의 내부 구조는 API별 DTO를 확인한다.
- Controller 예외: `{ "success": false, "response": { "exceptionCode", "message", "status" } }`. JWT 필터/인가 오류: wrapper 없는 `{ "exceptionCode", "message", "status" }`. `CustomExceptionManager`와 `CustomExceptionDeniedHandler` 근거다. token 만료·위변조·invalid refresh는 실제 HTTP 401이며 다른 custom error는 HTTP 200일 수 있다. `status` 필드와 HTTP status를 혼동하지 않는다.
- 인증 요청은 `Authorization` 헤더의 JWT를 읽는다. `JwtAuthorizationFilter`는 `Authorization` 쿼리 파라미터도 읽는다. 일반 cookie를 API 인증에 사용하는 동작은 확인되지 않았다. 로그인/OAuth가 발행한 token cookie를 React가 저장소로 옮기는 것은 별도 클라이언트 동작이다.
- UI page가 1-based라도 API page가 0-based인 경로는 service 또는 mapper 한 곳에서 변환한다. 아래에서 명시하지 않은 page 기준은 추측하지 않는다.
- React 호출은 `tag-note-react/src/services/`, 인증 공유 상태는 `src/store/authStore.js`, 브라우저 저장소는 `src/utils/`를 통한다. 실제 호출자와 임시 alias를 확인한 후에만 제거한다.

## 인증 · 사용자

근거: `domain/user/UserApi.java`, `UserController.java`, `domain/auth/EmailLoginApi.java`, `EmailSignupApi.java`와 각 Controller/DTO, `oauth/GoogleOauthOpenApi.java`, OAuth handler, `filter/JwtAuthorizationFilter.java`, `agents/server/decisions/ADR-015-google-immediate-account-and-canonical-auth.md`.

`AUTH-EMAIL-001` 착수 시 지정된 `migration-server` worktree는 현재 경로에서 찾지 못했다. 2026-10-26 사용자가 제공한 LOCAL 계정·이메일 로그인·가입 계약을 작업 근거로 사용하고, 접근 가능한 `/mnt/d/p/tag-your-story`의 `EmailLoginApi`/`EmailSignupApi` → Controller/DTO → 서비스·예외 코드를 정적으로 대조했다. 그 서버 트리를 지정 worktree의 최신 상태라고 간주하지 않으며 실행 응답도 미검증이다.

- `POST /api/auth/login` — 공개. email/password 입력, 성공 `response.userId`, `response.nickname` 및 token cookie. `AUTH-EMAIL-001`에서 React 이메일 로그인 화면/서비스를 구현했다. 실행 응답은 미검증이다.
- `GET /api/auth/email-availability?email=` — 공개. email 사용 가능 여부 조회. 현재 React 호출 없음.
- `POST /api/auth/signup/email/start` — 공개. email/password/passwordConfirm 입력, `challengeId` 반환. `AUTH-EMAIL-001`에서 React 호출을 구현했다. 실행 응답은 미검증이다.
- `POST /api/auth/signup/email/resend` — 공개. `challengeId` 입력. `AUTH-EMAIL-001`에서 React 호출을 구현했다. 실행 응답은 미검증이다.
- `POST /api/auth/signup/email/confirm` — 공개. `challengeId`와 6자리 code 입력, `userId`/`nickname` 및 token cookie. `AUTH-EMAIL-001`에서 React 호출을 구현했다. 실행 응답은 미검증이다.
- 사용자 제공 LOCAL 계정 규칙: 가입 UI는 email/password/passwordConfirm만 받고 nickname은 서버가 자동 생성한다. 이메일은 trim·소문자화, 최대 255자이며 Gmail 점/+ 주소를 합치지 않는다. 비밀번호는 8자 이상, 영문자·숫자를 각각 포함하고 UTF-8 72바이트 이하이다. 코드는 10분 유효, 재발송은 60초 대기 및 최초 발송 포함 최대 5회다. 실행 서버 응답으로 검증한 사실은 아니다.
- `GET /oauth2/authorization/google` — 공개. Google 동의 화면으로 302 redirect. Request body 없음.
- `GET /login/oauth2/code/google?code=&state=` — Google 서버가 호출하는 callback. 성공·실패 모두 frontend로 302 redirect한다. 기존·신규 Google 사용자는 성공 시 즉시 ACTIVE USER이며 Authorization/RefreshToken cookie를 받는다. 실패 redirect의 `error_code`는 `GOOGLE_EMAIL_UNVERIFIED`, `GOOGLE_IDENTITY_INVALID`, `GOOGLE_EMAIL_CONFLICT_LOCAL`, `GOOGLE_EMAIL_CONFLICT_GOOGLE`, `USER_INACTIVE`, `GOOGLE_SIGNUP_CONFLICT` 중 하나다. 로컬 서버 redirect 설정은 `http://localhost:5173/token`으로 React callback route와 일치시켰다. 실제 브라우저 redirect와 cookie 전달은 미검증이다.
- `GET /api/user/me` — USER AccessToken 인증. 요청 body·query 없음. 현재 인증된 활성 사용자의 저장된 닉네임만 `{ "success": true, "response": { "nickname": "..." } }`로 반환한다. 내부 userId/email 등은 응답에 없다. JWT 필터가 요청마다 DB에서 ACTIVE ROLE_USER를 확인하고 principal에 넣은 닉네임을 반환하므로 추가 프로필 조회 쿼리는 없다. 만료·위변조 token/탈퇴 계정은 기존 필터의 401, 미인증은 403, 없는 계정은 404 정책을 따른다. React Google callback은 cookie 동기화 뒤 이를 호출해 이전 계정의 로컬 닉네임을 갱신한다. 2026-10-27 현재 서버 코드·Swagger 명세 기준이며 실행 응답은 미검증이다.
- `POST /api/user/reissue/accessToken` — 별도 Authorization 헤더 없이 `{ "refreshToken": "..." }`, 성공 `response.newAccessToken`. `UserApi`는 `GOOGLE_RELOGIN_REQUIRED`를 HTTP 401로 문서화하지만 `CustomExceptionManager` 구현은 이 코드를 HTTP 200 wrapped error로 반환한다. 충돌로 유지한다.
- `POST /api/user/reissue/refreshToken` — USER 인증. `response.newRefreshToken`; 이전 refresh token 무효화. React `authService`에 함수가 있으나 실제 호출자는 별도 확인한다.
- `POST /api/user/logout` — USER 인증. 서버 refresh token을 제거하지만 access token은 만료 전까지 유효하다. React는 logout 후 로컬 상태도 clear한다.
- `PATCH /api/user/me/nickname` — USER 인증. nickname 1~20자, 공백 불가; `response.nickname`. 기존 pending 가입의 대체 endpoint가 아니다.
- `DELETE /api/user/me` — USER 인증. 탈퇴 및 cookie 만료. React 마이 페이지에서 확인 후 호출한다.
- `GET /api/users/me/liked-boards?page=` — USER 인증. 접근 가능한 `tagnote-api`의 `LikeApi`/`LikeController`/`LikedBoardResponse` 기준으로 0-based page, 크기 8, `response.items`의 `boardId`·`author`·`content`, `response.totalCount`를 반환한다. 지정 `migration-server` worktree는 현재 환경에 없어 최신 계약 확인 및 실행 응답 검증은 보류한다.
- 현재 접근 가능한 `BoardApi`/`BoardController`와 `BoardUserTagController`에 사용자별 작성 태그 목록 API는 없다. `/api/boards/user-tags`는 태그 이름 기준 공개 목록이므로 내 태그 조회로 사용할 수 없다.
- `/api/user/register`와 `/api/user/check-registration` — 최신 Controller에 없다. 전자는 ADR-015에서 제거했다. React `authService` 및 `NicknamePage`에 과거 호출이 남아 있으므로 M2-2에서 실제 호출자를 확인해 정리한다. pending user → nickname 등록은 현재 서버 계약이 아니다.

## Catalog · Track

근거: `domain/catalog/CatalogSearchApi.java`, `CatalogImportApi.java`, 각 Controller/DTO; `domain/tracks/TrackApi.java`, `TrackController.java`와 DTO.

- `GET /api/catalog/search?keyword=&page=` — 공개. `keyword`와 0-based `page` 필수. `type` 생략 시 TRACK/ALBUM/ARTIST를 Spotify Search 한 요청으로 조회하며, 현재 page에서 타입당 최대 10건(합계 최대 30건)을 정렬한다. `response.totalCount`는 세 타입의 Spotify total 합계다.
- `GET /api/catalog/search?keyword=&page=&type=TRACK|ALBUM|ARTIST` — 공개. 선택적 `type`을 지정하면 3-page window에서 해당 타입 최대 30개를 모아 동일 Spotify ID 중복을 제거하고 정렬한다. Album 후보는 Spotify `q=album:{keyword}`, `type=album` 제목 필터 검색을 사용하며 Track·Artist는 일반 keyword 검색을 사용한다. `response.items`는 요청 타입의 현재 page 최대 10건, `response.totalCount`는 첫 window의 해당 검색 Spotify total 참고 값이다. 타입별 page는 독립적 0-based, 허용 범위 0~100이다. `type`이 허용 값 밖·빈 문자열이거나 page 범위 밖이면 HTTP 400이다. React 검색 화면은 2026-10-28 이후 이 타입별 API 대신 screen API를 사용한다. Album 제목 필터는 모든 정확 제목을 첫 page에 보장하지 않으며 제목 외 정보만으로 찾던 Album을 줄일 수 있다. 서버 코드·Swagger 정적 계약 기준, 실행 미검증.
- 2026-10-27 홈 기본 검색은 `/tracks?keyword=`로 이동한다. (2026-10-29 클라이언트 경로는 `/search?keyword=`로 변경되었고, 서버 API 경로는 동일하다.) 당시 세 영역은 타입별 `/api/catalog/search`를 호출했으나 2026-10-28 screen API 단일 호출로 전환했다. 화면에서 사용하지 않던 React `trackService`의 `/api/tracks` 검색 메서드는 제거했다. 실행 서버 응답은 아직 확인하지 않았다.
- 두 검색의 항목 필드는 `subjectType`, `spotifyId`, `title`, nullable `artistName`, `albumName`, `imageUrl`이다. 이미지가 없으면 `NO_IMAGE`다. TRACK/ALBUM/ARTIST의 nullable 필드 의미는 타입마다 다르다. Spotify 오류는 기존 예외 처리에 따라 HTTP 200의 `success=false`, `response.exceptionCode=SPOTIFY_EXCEPTION`, `response.status=503`일 수 있다. 검색은 Catalog 저장이나 enrichment를 실행하지 않는다.
- `GET /api/catalog/search/screen?keyword=&trackPage=0&albumPage=0&artistPage=0` — 공개. 세 page는 독립적이고 0-based, 기본값 0, 크기 10, 허용 범위 0~100이다. `response.tracks`, `response.albums`, `response.artists`는 각각 `items`, 참고용 Spotify `totalCount`, `hasNext`를 포함한다. item 필드는 기존 Catalog 검색 항목과 같다. 서버는 Track·Artist 일반 검색과 Album `album:` 제목 필터 검색의 연속 3페이지로 타입별 최대 30개 후보를 모아 동일 Spotify ID 중복을 제거하고 정렬한 뒤 선택 page 최대 10개만 반환한다. 첫 window cache miss는 최대 6회 Spotify 검색을 수행한다. `hasNext`는 totalCount 변동을 사용하지 않으며 window 끝이 차 있으면 다음 빈 page를 한 번 조회할 수 있다. 클라이언트는 그때 직전 page를 유지한다. 오류는 page 입력 HTTP 400 또는 기존 `SPOTIFY_EXCEPTION` wrapped error다. React `TracksPage`는 이 API를 단일 요청으로 사용하고 `catalogSearchService`에서 UI page를 0-based로 변환한다. 2026-10-28 서버 코드·Swagger 정적 계약 기준, 실행 미검증.
- `GET /api/catalog/albums/{spotifyAlbumId}` — 공개. Spotify Album ID로 상세 메타데이터를 조회한다. `response`는 `spotifyId`, `title`, `imageUrl`, nullable `releaseDate`, nullable `totalTracks`, nullable `releaseType` (`ALBUM`/`COMPILATION`/`SINGLE_EP`), Spotify 순서의 `artists[{spotifyId,name}]`이다. `releaseType`은 Spotify compilation을 우선 보존하고, Spotify single 또는 album의 6곡 이하·총 재생 시간 30분 미만을 `SINGLE_EP`로 묶는다. Spotify metadata가 부족하거나 분류용 추가 Track 페이지 조회가 실패하면 null이다. 이미지가 없으면 `NO_IMAGE`다. 저장·enrichment는 실행하지 않는다. 2026-10-29 서버 코드·Swagger 정적 계약 기준이며 실행 응답은 미검증이다.
- `GET /api/catalog/artists/{spotifyArtistId}` — 공개. Spotify Artist ID로 상세 메타데이터를 조회한다. `response`는 `spotifyId`, `name`, `imageUrl`, `genres`이며 팔로워 수는 없다. 이미지가 없으면 `NO_IMAGE`, 장르가 없으면 빈 목록이다. 저장·enrichment는 실행하지 않는다. Spotify 조회 실패는 두 상세 API 모두 기존 정책에 따라 HTTP 200의 `success=false`, `response.exceptionCode=SPOTIFY_EXCEPTION`, `response.status=503`일 수 있다. 2026-10-27 서버 코드·Swagger 정적 계약 기준이며 실행 응답은 미검증이다.
- `GET /api/catalog/artists/{spotifyArtistId}/albums?page=` — 공개. `page`는 선택적 0-based, 기본값 0, 크기 10. Spotify 기본 포함 범위(Album, Single, Compilation, Appears On)와 순서를 유지한다. `response.items` 필드는 `spotifyId`, `title`, `imageUrl`, nullable `releaseDate`, nullable `releaseType` (`ALBUM`/`COMPILATION`/`SINGLE_EP`)이다. Album 상세와 동일한 분류 규칙을 사용하며, Spotify `album` 항목은 곡 수·재생 시간 확인을 위해 Album Track을 추가 조회할 수 있다. 개별 분류 조회 실패나 metadata 부족 시 해당 항목의 `releaseType`만 null이다. `response.totalCount`는 Spotify total이고 이미지 부재 시 `NO_IMAGE`다. DB 조회·저장·enrichment는 실행하지 않는다. 2026-10-30 서버 코드·Swagger 정적 계약 기준이며 실행 응답은 미검증이다.
- `GET /api/catalog/artists/{spotifyArtistId}/albums?releaseType=ALL|ALBUM|SINGLE_EP|COMPILATION&page=0` — 공개. `releaseType` 명시 시 `page`는 분류 후 목록의 0-based 페이지(크기 10)다. HTTP 200 `response.items`는 선택 유형의 최대 10건, `totalCount`는 실제 수집된 항목 중 선택 유형의 건수이며 `classificationComplete`, `unclassifiedCount`를 포함한다. Spotify 원본 offset 창이 짧을 수 있으므로 `ALL.totalCount`는 원본 Spotify `total`보다 작을 수 있다. 분류 불가 항목은 ALL에만 포함된다. 인덱스 구축 중 HTTP 202, `Retry-After: 1`, `response.status=BUILDING`이며 재요청한다. 원본 목록 수집 실패는 기존 `SPOTIFY_EXCEPTION` wrapped error다. 빈·허용 밖 `releaseType`과 음수 page는 HTTP 400이다. React Artist 화면은 탭과 현재 page만 서버에 보내며 전체 원본 목록을 스캔하지 않는다. 2026-11-02 서버 코드·Swagger 정적 계약 기준, 실행 미검증.
- `GET /api/catalog/albums/{spotifyAlbumId}/tracks?page=` — 공개. `page`는 선택적 0-based, 기본값 0, 크기 10. Spotify 디스크·트랙 순서의 `response.items`는 `spotifyId`, `title`, nullable `artistName`, `discNumber`, `trackNumber`, `durationMs`를 포함한다. `response.totalCount`는 Spotify total이다. 두 목록 모두 음수·정수가 아닌 page·offset 범위 초과는 HTTP 400, Spotify 장애는 기존 `SPOTIFY_EXCEPTION` wrapped error를 따른다. 저장·enrichment는 실행하지 않는다. 2026-10-27 서버 코드·Swagger 정적 계약 기준이며 실행 응답은 미검증이다.
- 2026-10-28 React `CatalogSubjectPage`는 위 Artist·Album 상세와 관련 목록 4개 endpoint를 `catalogDetailService`로 호출한다. 접근 가능한 `tagnote-api`의 공개 `CatalogDetailApi` → Controller → DTO를 재확인했다. 지정된 `migration-server` worktree 및 실행 응답은 확인하지 못했다.
- 2026-10-28 React의 Track·Album·Artist 상세 브라우저 주소는 `/{type}/{spotifyId}/{nameSlug}` 형식이다. `slug`는 클라이언트 표시용이고 API 요청에는 포함하지 않는다. 서버 조회는 기존 ID 기반 endpoint를 사용한다.
- `POST /api/catalog/import` — 공개. `{ "subjectType": "TRACK|ALBUM", "spotifyId": "..." }`; `response.subjectType`과 해당 `track` 또는 `album`. ARTIST import는 지원하지 않는다. 부분 provider 실패 시 성공한 import 결과를 버리지 않는 서버 정책이 있다.
- `GET /api/catalog/tracks/{spotifyTrackId}/tags` 및 `GET /api/catalog/albums/{spotifyAlbumId}/tags` — 공개. path는 각각 Spotify Track/Album ID이고 요청 body·query는 없다. 성공 `response`는 `systemTags[{tagId,name,score}]`, `previewTags[{name,source}]`, `tagDisplayStatus`(`CONFIRMED|PREVIEW|EMPTY`)만 포함한다. Track은 TRACK subject의 visible resolved 태그(기존 Album 상속 포함), Album은 ALBUM subject의 visible resolved 태그를 반환하며 HIDDEN은 제외한다. 확정 태그가 없을 때만 저장된 검증 전 preview를 최대 5개 제공하고, 모두 없으면 두 목록은 빈 배열과 `EMPTY`다. 화면은 먼저 `POST /api/catalog/import`에 `subjectType=TRACK|ALBUM`과 `spotifyId`를 보내 Catalog 저장·외부 근거 수집·태그 계산을 트리거하고, 완료 후 태그 GET을 호출한다. GET은 저장 결과만 읽으며 외부 요청·DB 쓰기·Resolver 계산을 하지 않는다. import 전 Catalog 대상이 없으면 기존 정책의 HTTP 200, `success=false`, `response.exceptionCode=TRACK_NOT_FOUND|ALBUM_NOT_FOUND`, `response.status=404`다. 처리되지 않은 오류는 기존 error code 체계를 따른다. 상세 메타데이터는 기존 `GET /api/tracks/{trackId}` 또는 `GET /api/catalog/albums/{spotifyAlbumId}`로 별도 조회한다. 2026-11-02 서버 코드·Swagger 정적 계약 기준, 실행 미검증.
- `GET /api/tracks?keyword=&page=` — 공개, page 0-based. `response.trackDataList`, `totalCount`. 현재 `trackService`가 사용한다.
- `GET /api/tracks/{trackId}` — 공개. `response`는 기존 `trackId`, `artistName`, `title`, `albumName`, `imageUrl`에 nullable `spotifyArtistId`, `spotifyAlbumId`를 더한다. Artist ID는 표시하는 첫 Artist, Album ID는 같은 Track의 Album이다. 같은 Spotify Track 단건 응답에서 읽으며 DB 조회나 추가 Spotify 호출은 없다. `trackService`가 사용하며 ID가 있으면 Track 화면의 이름에서 각 상세 경로로 이동한다. 2026-10-29 서버 코드·Swagger 정적 계약 기준이며 실행 응답은 미검증이다.
- `GET /api/tracks/ranking` — 공개. Redis 검색 횟수 점수 역순의 `response.keywordList`, 최대 10개. 현재 `trackService`가 사용한다. 2026-10-29 서버 코드·Swagger 정적 계약 기준이며 실행 응답은 미검증이다.
- `POST /api/tracks/import` — 공개, `spotifyTrackId` 입력. deprecated이며 현재 React 직접 호출은 발견되지 않았다. 신규 Catalog 구현에서 재사용하지 않는다.

## Board

근거: `domain/board/BoardApi.java`, `BoardController.java`와 Request/Response DTO.

- `POST /api/boards` — USER. `content`, 비어 있지 않은 `userTagList`, `trackId` 또는 `albumId` 중 정확히 하나. 생성된 board 정보 반환.
- `GET /api/boards/{trackId}?order-type=CREATED_AT|LIKE&page=0` — 공개. Track Board 목록, page 0-based, 크기 8; `boardResponseList`, `totalCount`. 현재 접근 가능한 `BoardResponse`의 목록 항목에는 `boardId`, `content`, `createdAt`, `nickname`, `userTagNameList` 등이 있지만 `likeCount`와 `commentCount`는 없다. 목록에 두 수치를 표시하려면 공개 Board 상세(`likeCount`)와 Comment count(`count`)를 현재 페이지 항목별로 별도 조회해야 한다. 지정 `migration-server` 및 실행 응답은 미확인이다.
- `GET /api/boards/albums/{albumId}?order-type=CREATED_AT|LIKE&page=0` — 공개. Album Board 목록, 같은 정렬·page 계약. 현재 React의 Track 중심 호출과 구분한다.
- `GET /api/boards?boardId=` — 공개. 상세 DTO는 `content`, `nickname`, `likeCount`, `createdAt`, `userTagNameList`, `subjectType`, `trackId`, `albumId`, `systemTags`를 포함한다.
- `GET /api/boards/count/{trackId}` 및 `GET /api/boards/count/albums/{albumId}` — 공개. 각 대상 Board count.
- `GET /api/boards/user-tags?userTagName=` — 공개. 정확한 태그명에 해당하는 Board 목록.
- `GET /api/boards/auth/{boardId}` — USER. 작성자 여부 boolean.
- `PATCH /api/boards` — USER 및 작성자. `boardId`, `content`, `userTagList`.
- `DELETE /api/boards/{boardId}` — USER 및 작성자. soft delete.

## Comment · Like

현재 접근 가능한 서버에는 `domain/comment/CommentApi.java`와 `domain/like/LikeApi.java`가 있고 각각 Controller가 구현한다. 근거: 두 `*Api.java` → Controller/DTO/권한 선언. Comment Swagger가 문서화한 page 기준과 오류 응답은 아래와 같다. 실행 응답은 미검증이다.

- `POST /api/comments` — USER. `boardId`, `content`; `CommentResponse`는 `commentId`, `parentId`, `nickname`, `content`, `createdDate`.
- `PATCH /api/comments` — USER. `commentId`, `content`.
- `PATCH /api/comments/status/{commentId}` — USER. 댓글 삭제.
- `GET /api/comments/{boardId}/{page}` — 공개. 0-based page당 최상위 댓글 20건과 각 `children` 목록. 음수 page는 현재 별도 검증 없이 실패한다.
- `GET /api/comments/auth/{boardId}` — USER. 작성 가능한 comment ID 목록.
- `GET /api/comments/count/{boardId}` — 공개. 댓글 수.
- `POST /api/comments/replies` — USER. `boardId`, `parentId`, `content`.
- `GET /api/comments/replies/{parentId}/{page}` — 공개. 0-based page당 답글 5건의 `CommentResponse` 목록.
- `POST /api/likes` — USER. `boardId` 입력, boolean 반환.
- `DELETE /api/likes` — USER. `boardId` 입력, 데이터 없는 성공 응답.
- `GET /api/likes/status/{boardId}` — USER. LikeApi는 `response.liked`로 설명하고 DTO의 내부 필드명은 `isLiked`다. 실제 JSON 필드명은 실행 응답 미확인으로 남기며 현재 React는 두 이름을 모두 읽는다.
- `POST/DELETE /api/boards/{boardId}/likes` — USER. LikeApi가 문서화한 게시글 경로이며 현재 React는 기존 `/api/likes` 경로를 사용한다.

## File · Notification · User Tag

Notification에는 `domain/notification/NotificationApi.java`가 있고 Controller가 이를 구현한다. File과 User Tag는 이 갱신에서 공개 `*Api` 명세를 확인하지 않았다. 근거: `NotificationApi` → Controller/DTO 및 `FileController`, `BoardUserTagController`.

- `POST /api/files` — USER. multipart `fileList`, `boardId`; `fileId`, `filePath`, `fileLevel` 목록.
- `PATCH /api/files` — USER. 같은 multipart 항목으로 파일 추가/수정.
- `GET /api/files/main/{trackId}?page=` — 공개. `boardId`, `filePath` 목록.
- `GET /api/files/{boardId}` — 공개. `fileId`, `filePath` 목록.
- `DELETE /api/files` — JSON `boardId`, `fileIdList`. 현재 Controller에 `@PreAuthorize`가 없고 `SecurityConfig`도 기본 허용이며 삭제 service 경로에서 소유자 검사를 확인하지 못했다. 의도된 권한은 미확정이다. 클라이언트에서 임의로 권한을 확정하지 않는다.
- `GET /api/notification/subscription` — USER, `text/event-stream`. 연결마다 `init` 이벤트의 data `subscribe`를 보낸다. 이후 `Notification` 이벤트 data는 `{notificationId, contentId, pubNickname, type, isRead}` JSON이며 `type`은 `LIKE|COMMENT`다. 전체 User 객체·email·userKey는 포함하지 않는다. heartbeat는 SSE comment이고 알림 이벤트가 아니다. 재접속 replay/event ID는 없으므로 누락분은 목록 API로 조회한다. 실시간 전달 범위는 단일 서버 인스턴스다. JWT 필터의 `Authorization` query 지원은 코드로 확인했지만 cookie만으로 연결하는 모드는 확인되지 않았다.
- `GET /api/notification?page=` — USER. 필수 0-based page, 5건씩 최신순. `notificationId`, `contentId`, `pubNickname`, `type`, `isRead` 목록. 탈퇴한 발행자의 표시 이름은 `탈퇴한 사용자`다.
- `PATCH /api/notification` — USER. body `{ "notificationId": number }`로 본인 알림을 읽음 처리한다. `NOTIFICATION_NOT_FOUND`, `NO_READ_PERMISSION`은 기존 wrapped `response.exceptionCode` 오류 체계를 따른다.
- `GET /api/notification/count` — US ER. `response.count`는 읽음 여부와 무관한 **전체 알림 수**다. 미읽음 수 API로 해석하지 않는다.
- `PATCH /api/notification/all` — USER. 전체 읽음 처리.
- `GET /api/board-user-tags/recent` — 공개. `boardId`, `userTag` 목록.

## 현재 React 대조와 미확정 사항

- 조사 당시 API 호출은 `src/services/apiClient.js`, `authService.js`, `trackService.js`, `boardService.js`, `commentService.js`, `fileService.js`, `likeService.js`, `notificationService.js`, `userTagService.js`에 있었다. 타입별 검색 작업에서 `catalogSearchService.js`를 추가했다. 서비스 밖의 직접 `fetch`/`EventSource`는 발견되지 않았다. 실제 호출자와 alias 사용은 각 Plan 착수 시 다시 확인한다.
- React의 access 재발급은 refresh token을 Authorization 헤더에도 보내며 `newAccessToken` 외의 필드도 허용한다. 최신 `UserApi`/DTO와 차이가 있다.
- React 알림 상태의 `unreadCount`에는 현재 `/api/notification/count`의 전체 알림 수를 넣는다. 서버 count는 읽지 않은 알림 수가 아니므로 UI 표시 의미를 별도로 검토해야 한다.
- `TokenPage`, `HomePage`, `NicknamePage`에는 제거된 pending 사용자 등록 흐름이 남아 있다. `/api/user/register`, `/api/user/check-registration`의 새 대체 endpoint를 가정하지 않는다.
- `UserApi`의 `GOOGLE_RELOGIN_REQUIRED` HTTP 401 문서와 `CustomExceptionManager`의 HTTP 200 구현은 충돌한다. Google OAuth의 로컬 redirect 설정은 2026-10-26 React `/token` 경로에 맞췄으며, 실제 redirect와 cookie 동작은 브라우저에서 검증하지 않았다.
- File 삭제 권한, SSE cookie 인증, Comment/File/Like/Notification/User Tag의 실행 schema·status는 미확정이다. Comment·Like·Notification에는 현재 접근 가능한 서버의 `*Api.java` 명세가 있으나 실행 응답은 별도로 검증하지 않았다. 해당 API 사용 시 정적 근거와 사용자 제공 curl 응답을 구분한다.
