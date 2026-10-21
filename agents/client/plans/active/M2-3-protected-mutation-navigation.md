# M2-3 — 인증 필요 mutation의 로그인 유도

## Status and dependency

- 상태: 2026-10-20 Human Review 승인, `active/` 등록. M2-2 완료·사용자 검증 후 순차 착수한다. 구현·검증은 아직 시작하지 않았다.
- 대상: `tag-note-react/`. `agents/client/api-contract.md`의 관련 계약을 착수 시 재확인하며, M2-1, M2-2의 사용자 검증 완료에 의존한다.
- 최신 서버 worktree의 인증 권한 선언을 사용하되, 실제 실행 응답을 확인했다고 표현하지 않는다.

## Goal and non-goals

비로그인 사용자가 현재 화면에서 인증 필요 mutation을 시작할 때 로그인 route로 안내한다. 로그인 여부는 `authStore`가 제공하고, navigation은 page 또는 page가 전달한 UI handler에서 수행한다. 서비스에 navigation이나 toast 책임을 추가하지 않는다.

Board/Comment/File의 request/response 모델, 좋아요 낙관적 업데이트, 파일 연속 mutation 복구, 이메일 로그인, 디자인 변경은 이 Plan에서 다루지 않는다. 로그인 뒤 사용자의 이전 mutation을 자동 재실행하지 않는다.

## Contract evidence and current paths

최신 서버 worktree의 관련 Controller/권한 선언과 `agents/client/api-contract.md`의 도메인별 API 목록을 구현 전 확인한다. User 인증 방식은 `tagnote-api/.../domain/user/UserApi.java`, `filter/JwtAuthorizationFilter.java`, `agents/server/decisions/ADR-015-google-immediate-account-and-canonical-auth.md`를 기준으로 한다. 개별 mutation의 권한은 최신 Board/Comment/Like/File 코드와 대조해 확정하며, 확인되지 않은 endpoint 권한을 이 Plan에서 추측하지 않는다.

| React 진입점 | 현재 동작 | 이 Plan의 UI 책임 |
| --- | --- | --- |
| `TrackDetailPage`의 글 작성 버튼과 `/boards/new` 직접 진입 | 글 작성 route로 이동; `BoardEditPage`는 인증 상태를 확인하지 않음 | 비로그인 시 로그인 route 안내, 직접 진입도 보호 |
| `BoardDetailPage`의 좋아요 버튼 | `tokenStorage`를 직접 읽고 비로그인에도 mutation 요청 가능 | `useAuthStore`를 읽어 클릭 전에 안내 |
| `CommentSection`의 댓글·답글 작성 | `isAuthenticated` prop은 소유자 조회에만 사용, 작성 요청은 실행 가능 | page가 전달한 로그인 handler를 사용하여 요청 전에 안내 |
| 기존 작성자 수정·삭제 및 Board/File mutation | 현재 노출 조건과 서버 오류 처리에 의존 | 비로그인 상태와 만료 후 재렌더링에서 요청 전에 동일 정책 적용; 기능 자체는 변경하지 않음 |

## Existing behavior to characterize first

`TrackDetailPage`, `BoardDetailPage`, `BoardEditPage`, `CommentSection`의 비로그인 클릭·직접 URL 진입·로그인 중 상태 변경을 React Testing Library로 특성화한다. 현재 공개 조회가 가능한 페이지와 로그인 뒤 기존 화면으로 돌아오는 동작을 기록한다. `BoardDetailPage`의 `Boolean(tokenStorage.getAccessToken())`가 store 갱신에 구독하지 않는 점을 회귀 위험으로 둔다.

## Implementation steps and file responsibilities

1. `src/pages/TrackDetailPage.test.jsx`, `BoardDetailPage.test.jsx`, `BoardEditPage.test.jsx`, `src/features/comments/CommentSection.test.jsx` 등 필요한 화면 테스트에 비로그인 mutation과 직접 URL 진입을 추가한다. HTTP 호출 여부와 route 이동을 함께 확인한다.
2. `TrackDetailPage.jsx`, `BoardDetailPage.jsx`, `BoardEditPage.jsx`는 `useAuthStore`로 인증 여부를 읽는다. 공개 조회는 그대로 허용하고 인증이 필요한 action만 로그인으로 이동시킨다.
3. `CommentSection.jsx`는 page가 제공한 `onLoginRequired` 같은 UI handler를 받아 댓글·답글 작성 진입에 사용한다. feature가 service 응답을 검사해 스스로 route를 조합하거나 service에 navigation을 넣지 않는다. 수정·삭제도 비인증 상태에서 요청하지 않게 한다.
4. 로그인 유도는 `src/utils/routes.js`의 `routes.login`을 사용한다. 필요한 안내 문구는 `src/i18n/locales/{ko,en}/auth.json` 또는 관련 namespace에 둔다. 페이지가 사용자의 작성 중 입력을 조용히 폐기하지 않도록 이동 전 동작을 테스트로 확정한다.
5. 이미 요청이 시작된 뒤 M2-1의 `AuthExpiredError`가 오면 각 UI가 같은 로그인 안내로 연결한다. 모든 API 오류를 로그인 요구로 취급하지 않는다.

## State and error rules

- 비로그인 mutation 클릭에서는 서버 요청을 보내지 않고 로그인 route로 이동한다.
- 공개 화면은 로그인 요구로 막지 않는다. 로그인 완료 뒤 mutation을 자동 재전송하지 않는다.
- 인증 만료와 권한 부족/검증 오류를 같은 상황으로 취급하지 않는다. `ApiError.exceptionCode`의 확인된 값만 사용한다.
- 화면 밖 서비스는 endpoint 호출만 담당하며 브라우저 저장소를 page에서 직접 새로 읽지 않는다.

## Acceptance criteria

- 글 작성, 좋아요, 댓글·답글의 비로그인 진입과 직접 `/boards/new` 접근이 로그인 안내로 이어지고 해당 mutation 요청은 발생하지 않는다.
- 로그인 사용자의 기존 mutation 호출과 공개 Board/Comment 조회가 유지된다.
- 인증 상태 갱신 후 버튼 상태가 store를 따라 다시 렌더링된다.
- 인증 만료, 권한 부족, 일반 오류를 동일한 로그인 이동으로 오분류하지 않는다.
- service에 navigation을 추가하지 않고, 새 UI 문구는 ko/en 번역 리소스를 사용한다.

## User verification and review

구현 후 사용자가 `cd tag-note-react`에서 `npm run test`, `npm run lint`, `npm run build`를 실행하고 결과를 전달한다. 브라우저에서는 비로그인 글 작성·좋아요·댓글/답글, `/boards/new` 직접 진입, 로그인 상태의 같은 action, access 만료 중 action을 확인한다. diff에서 공개 조회 차단, 중복 navigation, Board/Comment/File 기능 변경, page의 token 직접 판독을 검토한다.

## Open decisions and rollback

- 로그인 후 원래 화면으로 돌아올지, 입력 내용을 보존할지는 현재 LoginPage의 route state와 OAuth redirect 계약을 확인한 뒤 Human Review에서 정한다. 이 Plan의 필수 기준에는 mutation 자동 재실행을 넣지 않는다.
- 개별 mutation의 인증 권한이 최신 서버 코드에서 확정되지 않으면 그 진입점은 구현 범위를 정하기 전에 충돌로 보고한다.
- 문제가 생기면 UI guard와 handler 연결만 되돌린다. 서비스 계약과 기존 mutation payload는 변경하지 않는다.
