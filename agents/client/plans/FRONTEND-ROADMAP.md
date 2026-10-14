# TagNote React Frontend Roadmap

## 1. 문서 상태

- 상태: Draft / Human Review 대기
- 완료된 별도 마일스톤: `I18N-001` 한국어/영어 전역 UI 다국어화 (2026-10-14, 사용자 검증 완료)
- 대상: `tag-note-react/`
- 기준 문서:
  - `agents/client/frontend_spec.md`
  - `agents/client/design_spec.md`
  - 실행 서버의 `/v3/api-docs`, `/swagger-ui/index.html`
- 목적: 현재 React 구현을 폐기하지 않고, 서버 계약과 디자인 명세에 맞춰 작은 마일스톤으로 완성한다.

이번 초안의 API 조사는 서버 코드의 Swagger interface(`TrackApi`, `CatalogSearchApi`, `CatalogImportApi`)와 실제 Controller/DTO를 기준으로 했다. 실행 중인 서버의 OpenAPI JSON은 이번 작업에서 확인하지 않았으므로 M0에서 반드시 다시 대조한다.

이 문서는 전체 작업 순서와 의존성만 관리한다. 실제 구현에 착수할 때는 아래 순서에서 하나의 작은 마일스톤만 선택해 `plans/active/`에 상세 Plan을 작성하고 Human Review를 받은 뒤 시작한다.

## 2. 조사한 현재 상태

### 2.1 이미 존재하는 기반

- Vite, React 19, React Router 7 기반 SPA와 주요 route가 구성되어 있다.
- Home, 검색 결과, Track 상세, Board 상세/작성/수정, 인증, Contact 화면이 존재한다.
- Board/Comment/File/Like/Notification UI 및 service 초안이 존재한다.
- `apiClient`, `authStore`, `notificationStore`, route/storage util이 존재한다.
- 디자인 자산 `assets/img_1.png`와 페이지별 CSS가 존재한다.

따라서 신규 프로젝트로 다시 만들지 않는다. 긴 page와 feature의 현재 동작을 특성화한 후 hook/model/component로 점진 분리한다.

### 2.2 현재 서버 API 계약 요약

| 영역 | Endpoint | 인증 | 프론트엔드 사용 방향 |
| --- | --- | --- | --- |
| Catalog 검색 | `GET /api/catalog/search?keyword=&page=` | 불필요 | Track/Album/Artist 통합 검색의 기준 API |
| Catalog 선택 | `POST /api/catalog/import` | 불필요 | `TRACK`, `ALBUM`만 import 및 System Tag 조회 |
| Legacy Track 검색 | `GET /api/tracks?keyword=&page=` | 불필요 | 기존 Track 전용 화면 호환 경로 |
| Track 상세 | `GET /api/tracks/{trackId}` | 불필요 | Spotify track 상세 조회 |
| 검색 랭킹 | `GET /api/tracks/ranking` | 불필요 | 홈 인기 키워드 |
| Deprecated Track import | `POST /api/tracks/import` | 불필요 | 신규 구현에서는 사용하지 않음 |
| Board | `/api/boards...` | 조회는 공개, 작성/작성자 확인/수정/삭제는 USER | Track별 목록, 상세, User Tag 조회, CRUD |
| File | `/api/files...` | 조회 공개, 업로드/수정은 USER | Board 첨부 이미지; 삭제 권한은 서버 계약 재확인 필요 |
| Comment | `/api/comments...` | 조회 공개, 작성/수정/삭제/작성자 목록은 USER | 댓글/답글 |
| Like | `/api/likes...` | USER | 좋아요, 취소, 상태 |
| Notification | `/api/notification...` | USER | SSE, 목록, 개수, 읽음 처리 |
| User Tag | `GET /api/board-user-tags/recent` | 불필요 | 홈 최근 태그 |
| Auth | OAuth2, `/api/user/register`, token 재발급, logout | 역할별 상이 | 로그인/가입/토큰 수명주기 |

공통 JSON 성공 응답은 `{ "success": true, "response": ... }`이다. Controller advice 오류는 `{ "success": false, "response": { "exceptionCode", "message", "status" } }`이고 JWT filter 오류는 wrapper 없는 `{ "exceptionCode", "message", "status" }`일 수 있으므로 두 형태를 모두 정규화해야 한다.

### 2.3 구현 전에 해소할 계약 차이

1. 서버 `application-api.yml`의 OAuth 성공 redirect가 React의 `/token`이 아니라 기존 `/token.html`을 가리킨다. 로컬과 운영 redirect를 React route로 정렬해야 실제 인증 흐름을 검증할 수 있다.
2. `authService.checkRegisterUser()`에 정의된 `GET /api/user/check-registration`은 현재 서버에 없다. 사용하지 않는 alias라면 제거하고, 필요하다면 서버 계약을 먼저 추가한다.
3. 명시적인 Swagger API interface는 현재 Catalog/Track에 집중되어 있다. Board, Comment, File, Like, Notification, User는 OpenAPI 자동 노출 여부와 실제 schema/auth/error 설명을 실행 서버에서 다시 확인해야 한다.
4. 통합 검색은 `TRACK`, `ALBUM`, `ARTIST`를 반환하지만 import는 `TRACK`, `ALBUM`만 지원한다. Board는 현재 Spotify Track ID에만 연결된다. Album/Artist 선택 후 UX와 Artist 상세/import 범위는 구현 전에 Product 결정을 받아야 한다.
5. `MY TAGS`에 대응하는 전용 route/API가 없다. 현재 제공되는 것은 태그명 기준 공개 Board 목록이므로 개인 소유 태그 목록으로 오해해 구현하지 않는다.
6. React 코드에는 같은 API의 alias method가 여럿 남아 있고 page가 `tokenStorage`를 직접 읽는 곳이 있다. 기능별 마이그레이션에서 하나의 canonical service 함수와 `authStore` 접근으로 정리한다.
7. Board/File 연속 mutation은 부분 성공할 수 있다. Board 작성 성공 후 파일 업로드 실패, Board 수정 후 파일 삭제/추가 실패를 UI에서 구분하고 재시도 가능하게 해야 한다.

## 3. 권장 구현 순서

### M0. 계약 기준선과 프론트엔드 테스트 기반

목표: 이후 작업이 잘못된 API 가정이나 기존 동작 퇴보 위에서 진행되지 않게 한다.

범위:

- 실행 서버의 OpenAPI JSON을 저장하지 않고 조회해 endpoint, method, query/path/body, response, auth를 현재 코드와 대조한다.
- 위 2.3의 차이를 서버 변경 필요/프론트 변경 필요/Product 결정 필요로 분류한다.
- Vitest, React Testing Library, jsdom 기반 최소 테스트 명령을 추가한다.
- `apiClient`, route builder, token storage, page 변환(화면 1-based ↔ API 0-based)의 특성화 테스트를 우선 작성한다.
- 환경 변수와 OAuth/SSE 동작을 로컬 개발 기준으로 문서화한다.

Acceptance Criteria:

- 프론트엔드가 호출하는 모든 endpoint가 현재 서버 계약표와 매핑된다.
- 존재하지 않거나 deprecated된 endpoint가 명시된다.
- `npm run test`, `npm run lint`, `npm run build` 검증 진입점이 존재한다.
- production UI 동작은 변경하지 않는다.

### M1. API client와 인증 수명주기 정합성

의존성: M0

목표: 모든 후속 feature가 동일한 성공/오류/인증 계약을 사용하게 한다.

범위:

- wrapped/bare 오류 응답을 하나의 `ApiError`로 정규화한다.
- access token 재발급의 단일-flight 처리, 1회 retry, 실패 시 저장소/store 동시 clear를 정의한다.
- `authService` alias와 서버에 없는 endpoint를 정리한다.
- OAuth cookie → storage → `authStore` 동기화와 pending user → nickname 등록 흐름을 특성화한다.
- 서버 OAuth redirect를 React `/token`으로 바꾸는 작업은 서버 scope로 분리해 승인받는다.
- 인증 필요 mutation은 비로그인 시 로그인 route로 유도하되, service 안에서 navigation하지 않는다.

Acceptance Criteria:

- 정상 로그인, 신규 사용자 nickname 등록, logout, 만료 token 재발급, 재발급 실패가 테스트된다.
- page/component가 새로 `tokenStorage`를 직접 판독하지 않는다.
- HTTP 오류와 HTTP 200 `success=false`가 동일한 화면 오류 흐름으로 전달된다.

### M2. App shell과 디자인 토큰 정렬

의존성: M0. M1과 독립적으로 진행할 수 있으나 하나의 active milestone만 유지한다.

목표: 페이지별 재작업 전에 전역 레이아웃과 시각 규칙을 확정한다.

범위:

- `globals.css`의 색상, typography, spacing, focus, motion token을 디자인 명세와 정렬한다.
- 최대 폭 1120px 중앙 레이아웃, header/navigation, page enter bloom, reduced-motion 대응을 확정한다.
- `img_1.png`만 전역/hero 종이 질감으로 사용하고 `img.png`는 기본 질감에서 제외한다.
- active underline, 과도한 line/grid/card/monospace, prompt성 문구를 제거한다.
- 공통 loading/error/empty/focus 표현과 반응형 기준을 정의한다.
- Header의 `MY TAGS` 의미는 2.3의 Product 결정 전까지 로그인 링크 이상의 기능으로 확장하지 않는다.

Acceptance Criteria:

- 360px, 768px, 1120px 이상 viewport에서 주요 layout이 가로 overflow 없이 동작한다.
- keyboard focus와 `prefers-reduced-motion`이 지원된다.
- 디자인 명세의 금지사항 점검표를 통과한다.

### M3. 홈과 Catalog 통합 검색

의존성: M0, M2

목표: 사용자가 검색하고 Track/Album/Artist 결과의 차이를 이해할 수 있게 한다.

범위:

- Home의 검색, 검색어 랭킹, 최근 User Tag를 각각 독립 loading/error/empty 상태로 유지한다.
- `GET /api/catalog/search`를 기준으로 `catalogService`와 response mapper를 추가한다.
- URL의 keyword/page를 source of truth로 사용하고 UI 1-based/API 0-based 변환을 한 곳에 둔다.
- 결과에 `subjectType`을 명확히 표시하고 nullable artist/album 필드를 안전하게 렌더링한다.
- TRACK/ALBUM 선택 시 `POST /api/catalog/import`를 호출해 확정 System Tag와 preview 상태를 구분한다.
- ARTIST는 import 미지원 상태를 명확히 표시한다. Artist 상세이나 Board 연결을 임의로 만들지 않는다.
- Album 결과의 상세 표시 위치와 route는 Human Review에서 확정한다.

Acceptance Criteria:

- 빈 검색어, 특수문자, 잘못된 page, 결과 없음, provider 부분 실패가 처리된다.
- 뒤로가기/새로고침 후 keyword/page가 복원된다.
- `CONFIRMED`, `PREVIEW`, `EMPTY` tag 표시가 구분되고 preview를 확정 태그로 표현하지 않는다.

### M4. Track 상세와 공개 Board 읽기 흐름

의존성: M3

목표: 검색 결과에서 Track 정보와 해당 Track의 태그 노트를 안정적으로 탐색한다.

범위:

- Track 상세 조회와 Board 목록 조회를 feature hook/model로 분리한다.
- Board 정렬 `CREATED_AT`/`LIKE`, pagination, count 계약을 검증한다.
- Board 상세, 파일 목록, 댓글 목록을 공개 읽기 흐름으로 조립한다.
- session storage에 의존한 back navigation은 직접 URL 진입/새로고침에서도 안전한 fallback을 제공한다.
- Board detail의 User Tag 클릭은 공개 태그명 Board 목록 route로 연결하되 개인 태그 기능으로 표현하지 않는다.

Acceptance Criteria:

- 직접 URL 진입, 검색에서 진입, Board에서 뒤로가기의 세 경로가 동작한다.
- Track/Board/File/Comment 각각의 부분 실패가 나머지 성공 데이터를 불필요하게 숨기지 않는다.
- 긴 콘텐츠, 이미지 없음, 태그 없음/많음, 빈 Board 목록이 디자인을 깨지 않는다.

### M5. 보호된 Board 작성·수정·삭제와 파일

의존성: M1, M4

목표: 인증 사용자에게 복구 가능한 Board mutation 흐름을 제공한다.

범위:

- `BoardEditPage` 상태를 feature form hook/model로 분리한다.
- User Tag 1~5개, 중복, 공백, 서버 validation을 동일 규칙으로 처리한다.
- 파일 PNG/JPEG, 최대 3개, object URL 해제, 업로드/수정/삭제를 검증한다.
- Board 생성과 파일 업로드의 부분 성공을 구분해 Board ID를 잃지 않고 파일만 재시도할 수 있게 한다.
- 수정 시 Board, 기존 파일 삭제, 신규 파일 추가의 실패 단계를 사용자에게 알린다.
- 작성자 확인 결과에 따라 수정/삭제 UI를 노출하고 mutation 후 cache/local state를 정리한다.

Acceptance Criteria:

- 비로그인 접근, 권한 없음, validation 실패, 중복 제출, 부분 성공, 재시도가 테스트된다.
- 제출 중 이중 요청이 발생하지 않는다.
- 새 파일 preview URL이 교체와 unmount 시 해제된다.

### M6. 댓글·답글·좋아요

의존성: M1, M4

목표: Board 상세의 커뮤니티 상호작용을 독립 feature로 완성한다.

범위:

- Comment 조회/count/작성/수정/삭제/답글과 pagination 상태를 hook으로 분리한다.
- 작성 가능한 comment ID 계약을 기준으로 소유자 action을 제어한다.
- Like 상태와 count 변경은 요청 실패 시 rollback되는 optimistic update 또는 성공 후 반영 중 하나로 일관되게 구현한다.
- mutation 후 전체 페이지 reload 대신 필요한 목록/count만 갱신한다.

Acceptance Criteria:

- 비로그인 읽기와 로그인 mutation 경계가 명확하다.
- 연속 클릭, 삭제된 댓글, 답글 pagination, mutation 실패가 일관되게 처리된다.
- 댓글 UI가 오래된 게시판형 row/table로 보이지 않는다.

### M7. 알림 SSE와 전역 상태

의존성: M1, M6

목표: 로그인 세션당 하나의 안정적인 알림 구독과 읽음 상태를 제공한다.

범위:

- SSE 연결을 layout-level hook으로 이동하고 중복 구독을 방지한다.
- EventSource query 인증 방식과 cookie 방식 중 서버 배포 계약에 맞는 하나를 확정한다.
- 초기 unread count, 수신 event, dropdown pagination, 개별/전체 읽음을 store와 동기화한다.
- 연결 오류, token 만료, 재연결 backoff, logout cleanup을 처리한다.
- timeout과 document listener를 unmount 시 모두 정리한다.

Acceptance Criteria:

- route 이동으로 SSE 연결이 중복되지 않는다.
- 수신/읽음/전체 읽음 후 badge가 서버 상태와 일치한다.
- logout 또는 만료 후 연결과 transient toast가 정리된다.

### M8. User Tag 탐색, Contact, 접근성 및 출시 검증

의존성: M2~M7

목표: 남은 탐색 경로를 닫고 상용 수준의 검증을 완료한다.

범위:

- 공개 User Tag 이름별 Board 목록을 별도 route로 제공한다.
- 개인 `MY TAGS`는 전용 서버 계약이 추가되기 전에는 공개 태그 검색과 구분한다.
- Contact에 GITHUB/SPOTIFY/INSTAGRAM/EMAIL 링크를 실제 값으로 확정한다.
- semantic heading, label, live region, keyboard, focus return, image alt를 점검한다.
- 느린 네트워크, offline, 400/401/403/404/5xx, HTTP 200 failure envelope를 화면별로 점검한다.
- 모바일/태블릿/데스크톱 시각 회귀와 디자인 금지사항을 최종 확인한다.

Acceptance Criteria:

- 모든 nav/action이 실제 route 또는 명확한 disabled 상태를 가진다.
- 주요 사용자 여정의 수동 검증표와 자동 테스트가 통과한다.
- console error, 미정리 listener/object URL/SSE, 중복 service alias가 남지 않는다.

## 4. 각 상세 Plan의 공통 형식

각 `plans/active/*.md`는 다음 항목을 포함한다.

1. Goal과 Non-goals
2. 확인한 OpenAPI endpoint 및 request/response 예시
3. 기존 동작 특성화 테스트
4. 변경 파일과 계층별 책임
5. 상태 및 오류 흐름
6. Acceptance Criteria
7. 사용자 검증 명령
8. 미결정 사항과 rollback 방법
9. 완료 후 diff review 결과

## 5. 공통 검증 명령

M0에서 test script를 추가하기 전:

```bash
cd tag-note-react
npm run lint
npm run build
```

M0 완료 후:

```bash
cd tag-note-react
npm run test
npm run lint
npm run build
```

브라우저 수동 검증은 해당 마일스톤의 사용자 여정과 viewport를 상세 Plan에 명시한다. 실행하지 않은 명령이나 확인하지 않은 서버 계약을 완료로 기록하지 않는다.
