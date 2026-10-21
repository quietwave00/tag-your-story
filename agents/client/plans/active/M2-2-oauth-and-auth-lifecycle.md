# M2-2 — OAuth 쿠키와 인증 수명주기 정합성

## Status and dependency

- 상태: 2026-10-20 Human Review 승인, `active/` 등록. M2-1 완료·사용자 검증 후 순차 착수한다. 구현·검증은 아직 시작하지 않았다.
- 대상: `tag-note-react/`. `agents/client/api-contract.md` 인증 계약을 착수 시 재확인하며, M2-1 완료·사용자 검증에 의존한다.
- 사용자의 지시에 따라 최신 서버의 `*Api` 문서, Controller/DTO, ADR을 근거로 삼는다. 실행 OpenAPI 또는 OAuth 브라우저 응답을 확인했다고 기록하지 않는다.

## Goal and non-goals

현재 Google OAuth 성공·실패, 쿠키 → 저장소 → `authStore`, logout 흐름을 최신 서버 계약에 맞춘다. 과거 pending token과 제거된 등록 endpoint가 실제 사용자에게 다시 가입 절차로 제시되지 않게 한다. 실제 호출자와 서버 근거를 확인한 인증 alias만 정리한다.

서버의 OAuth redirect 설정, 이메일 가입·로그인 화면, 회원 탈퇴, 닉네임 변경 기능 추가, Board/Comment/File 구현과 디자인 변경은 범위 밖이다. 최신 `PATCH /api/user/me/nickname`은 기존 pending 등록의 대체 endpoint로 사용하지 않는다.

## Contract evidence and risk

최신 서버 worktree의 `tagnote-api/.../oauth/GoogleOauthOpenApi.java`, `OauthSuccessHandler.java`, `OauthFailureHandler.java`, `domain/user/UserApi.java`, `UserController.java`, `jwt/JwtCookieProvider.java`, `agents/server/decisions/ADR-015-google-immediate-account-and-canonical-auth.md`를 확인했다.

| 경로/응답 | 확인한 계약 | 현재 React 차이 |
| --- | --- | --- |
| `GET /oauth2/authorization/google`와 callback | 성공 시 신규·기존 모두 ACTIVE USER access/refresh cookie 후 redirect; 실패 시 `error_code` query redirect | Google 진입은 있음; callback은 오류 query를 처리하지 않음 |
| `POST /api/user/logout` | USER access 인증 필요; 서버 refresh token 삭제, 기존 access token은 만료까지 유효 | 로컬 clear는 수행하지만 쿠키 재동기화 위험과 실패 표시가 미특성화 |
| `POST /api/user/register` | ADR-015에서 제거, Controller에 없음; 서버 테스트는 404를 명시 | `authService.register/updateNickname`과 `NicknamePage`가 호출 |
| `GET /api/user/check-registration` | 현재 Controller/`UserApi`에 없음 | `authService.checkRegisterUser`에만 존재, 호출자는 없음 |
| `PATCH /api/user/me/nickname` | 인증된 USER의 별도 닉네임 변경, 1~20자·공백 불가 | 현재 pending 사용자 화면과 계약·의미가 다름 |

서버 `application-api.yml`은 OAuth redirect를 `/token.html`로 설정한다. React route는 `/token`이다. M2-2에서 서버 설정을 바꾸지 않으며 실제 브라우저 OAuth 여정의 검증 제약으로 기록한다.

과거 로드맵의 pending 사용자 → nickname 등록 수용 기준은 최신 서버 ADR-015 및 Controller와 충돌했다. 현재 로드맵에서는 이를 레거시 호출자 정리 대상으로 수정했다. 제거된 endpoint를 새 가입 경로처럼 구현하지 않는다.

## Existing behavior to characterize first

`TokenPage`는 `Pending` 쿠키가 있으면 `/nickname`으로 이동한다. `HomePage`도 저장된 pending token을 보고 `/nickname`으로 보낸다. `NicknamePage`는 7자 nickname으로 `/api/user/register`를 요청하고 성공하면 재로그인을 안내한다. `tokenStorage.syncFromCookies()`는 access/refresh 쿠키를 저장소로 옮기지만 소비 후 삭제하지 않는다. `AppLayout`은 logout 요청의 성공 여부와 관계없이 로컬 상태를 clear한다. 해당 동작을 먼저 특성화하고 변경 지점을 확인한다.

## Implementation steps and file responsibilities

1. `src/pages/TokenPage.test.jsx`, `src/utils/tokenStorage.test.js`, `src/components/layout/AppLayout.test.jsx` 등 필요한 테스트로 현재 callback, pending, 쿠키 동기화, logout 상태와 navigation을 고정한다. 서버 OAuth 응답은 cookie/query fixture로만 모사한다.
2. `src/utils/tokenStorage.js`와 `cookie.js`는 USER access/refresh 쿠키를 읽고 저장하는 규칙을 관리한다. legacy `Pending` 쿠키·저장소 값이 있어도 새 등록 흐름을 열지 않도록 정리한다. cookie 삭제 가능 범위는 실제 도메인/브라우저 검증 전 확정된 사실로 쓰지 않는다.
3. `src/store/authStore.js`는 M2-1의 동기화 진입점을 사용한다. `TokenPage`는 OAuth `error_code`가 있으면 임의의 token 성공 상태로 처리하지 않고 로그인 화면의 번역된 오류 안내로 이동한다. 성공 쿠키가 있으면 store를 갱신하고 홈으로 이동한다. 쿠키가 없는 직접 callback 진입의 이동 규칙을 테스트로 확정한다.
4. `src/services/authService.js`에서 `/api/user/register`, `/api/user/check-registration`과 중복 `updateNickname` alias의 호출자·계약 근거를 최종 확인한 뒤 제거한다. `reissueRefreshToken`은 서버에 실제 endpoint가 있으므로 미사용이라는 이유만으로 이 Plan에서 삭제하지 않는다.
5. `src/pages/NicknamePage.jsx`, `HomePage.jsx`, `src/app/router.jsx`와 `src/utils/routes.js`에서 오래된 pending/`/nickname` 진입을 안전한 로그인 안내로 처리한다. 북마크된 `/nickname`이 제거된 등록 API를 호출하지 않게 하되, 라우트 삭제 여부는 Human Review에서 확정한다. 닉네임 변경 UI는 만들지 않는다.
6. `src/components/layout/AppLayout.jsx`의 logout은 서버 호출 후 로컬 저장소/store 정리와 navigation을 보장한다. 실패 처리와 남은 cookie가 로컬 상태를 되살리는 문제를 테스트·브라우저 검증으로 다룬다. 서버가 쿠키를 만료시키지 않는 사실을 UI에서 은폐하지 않는다.
7. 안내·오류 문구와 접근성 라벨은 `src/i18n/locales/{ko,en}/auth.json` 등 번역 리소스에 둔다. API/user 콘텐츠는 번역하지 않는다.

## State and navigation rules

- 성공 Google callback의 기준은 최신 USER token cookie 두 개다. 오래된 pending token은 가입 자격으로 해석하지 않는다.
- OAuth 실패의 `error_code`는 서버가 나열한 값만 UI 안내에 매핑한다. 예상하지 못한 값은 일반 인증 실패 안내를 사용한다.
- logout 뒤에는 저장소와 store가 동시에 비인증 상태가 된다. 요청 실패에서 로컬 상태를 어떻게 처리할지는 현행 `finally` 동작을 특성화한 뒤 Review에서 확정한다.
- 로그인 완료 전 mutation 자동 재실행이나 닉네임 변경 API 호출은 하지 않는다.

## Acceptance criteria

- 신규·기존 Google 성공 callback이 access/refresh cookie를 저장소에 반영하고 `authStore`를 갱신한다.
- OAuth 실패 query와 cookie 없는 callback이 로그인 완료로 표시되지 않는다.
- 과거 pending 값과 `/nickname` 직접 진입이 제거된 `/api/user/register`를 호출하지 않는다.
- logout 후 Header와 저장소 상태가 일치하고, 재동기화 시 인증 상태 부활 여부가 브라우저에서 확인·기록된다.
- 제거된 등록 API의 실제 호출자를 새 OAuth 여정으로 전환한 뒤 해당 메서드를 정리한다. 호출자 없는 endpoint/alias도 계약 근거를 기록하고 정리한다. `PATCH /api/user/me/nickname`을 등록에 사용하지 않는다.

## User verification and review

구현 후 사용자가 `cd tag-note-react`에서 `npm run test`, `npm run lint`, `npm run build`를 실행하고 결과를 전달한다. 브라우저에서는 Google 기존·신규 사용자 callback, 실패 `error_code`, 오래된 pending 저장소, logout 후 새로고침을 확인한다. 서버 redirect가 여전히 `/token.html`이면 React callback 수동 검증 미완료로 명시한다. diff에서 삭제된 endpoint의 호출자, storage/cookie side effect, 기존 route 퇴보를 검토한다.

## Open decisions and rollback

- OAuth redirect 변경은 서버 범위이며 이 Plan에서 수행하지 않는다.
- `/nickname` 레거시 route 유지·redirect·제거 중 최종 처리와 logout 실패 시 로컬 clear 정책은 Human Review 항목이다.
- 문제가 생기면 OAuth callback/쿠키 처리와 제거된 등록 UI 변경을 각각 되돌린다. 특성화 테스트는 남겨 회귀를 비교한다. 후속 M2-3은 이 Plan의 수용 기준이 끝난 뒤 착수한다.
