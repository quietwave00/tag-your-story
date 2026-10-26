# M2-1 — API 오류와 access token 재발급 정합성

## Status and dependency

- 상태: 2026-10-20 Human Review 승인, 2026-10-21 구현 및 정적 diff 검토 완료, 2026-10-26 사용자 검증 완료.
- 대상: `tag-note-react/`. 수용 기준, 테스트·lint·build 및 브라우저 확인을 완료했다.
- 착수 기준: `agents/client/api-contract.md`의 인증·오류 기준선을 읽고 이 Plan의 endpoint와 오류 형태를 최신 서버 worktree에 다시 매핑한다. 계약 충돌은 아래 위험으로 유지한다.
- 다음 Plan: `M2-2-oauth-and-auth-lifecycle.md` → `M2-3-protected-mutation-navigation.md` (Human Review 승인·active 등록, 선행 Plan의 사용자 검증 후 순차 착수).

## Goal and non-goals

공통 HTTP 호출의 성공·오류 해석과 access token 재발급을 하나의 정책으로 만든다. 만료된 동시 요청은 재발급 호출 하나를 공유하고, 성공 시 각 원 요청을 최대 한 번만 다시 보낸다. 저장소와 `authStore`는 재발급 성공·실패 후 같은 상태를 나타낸다.

OAuth callback 화면, pending 사용자 화면, logout UI, 개별 Board/Comment/File 기능과 디자인은 이 Plan에서 변경하지 않는다. 이메일 가입·로그인 UI와 refresh token 자동 갱신 기능도 추가하지 않는다.

## Contract evidence and risk

최신 서버 근거는 `D:\p\tag-your-story`의 `feature/migration-server` worktree에서 읽은 `tagnote-api/.../domain/user/UserApi.java`, `UserController.java`, `dto/request/ReissueAccessTokenRequest.java`, `dto/response/ReissueAccessTokenResponse.java`, `tagnote-api/.../filter/JwtAuthorizationFilter.java`, `tagnote-core/.../exception/CustomExceptionManager.java`, `agents/server/decisions/ADR-015-google-immediate-account-and-canonical-auth.md`이다. 사용자의 지시에 따라 실행 OpenAPI에는 접속하지 않는다. 실행 응답을 검증한 것으로 표현하지 않는다.

| 호출/응답 | 확인한 계약 | 현재 React 차이 |
| --- | --- | --- |
| `POST /api/user/reissue/accessToken` | 별도 Authorization 헤더 없이 JSON `{ "refreshToken": "JWT 원문" }`; 성공 `{ "success": true, "response": { "newAccessToken": "JWT 원문" } }` | refresh token을 헤더와 본문 양쪽에 보냄; `response.accessToken` 대체 필드도 허용 |
| 인증 필터 오류 | 실제 HTTP 오류와 bare `{ "exceptionCode", "message", "status" }` | bare 오류만 안정적으로 읽음 |
| Controller advice 오류 | `success=false`, `response.exceptionCode/message/status`; token 만료·위변조·invalid refresh는 HTTP 401, 나머지는 대체로 HTTP 200 | wrapped 오류의 안쪽 필드를 펼치지 않음 |

**문서 충돌:** `UserApi`는 재발급의 `GOOGLE_RELOGIN_REQUIRED`를 HTTP 401로 설명하지만 `CustomExceptionManager`는 이 코드에 HTTP 200과 `success=false`를 반환한다. 서버 측 계약 확인 전 어느 status가 정답인지 가정하지 않는다. M2-1은 두 wire 형태를 모두 `ApiError`로 정규화한다.

## Existing behavior to characterize first

`src/services/apiClient.js`는 현재 `TOKEN_HAS_EXPIRED`에만 재발급을 시도하며 동시 요청마다 독립 호출한다. 요청 재시도는 한 번이지만 재발급 실패 시 일부 경로에서 저장소만 지우고 store는 갱신하지 않는다. 재발급 token이 없을 때도 store를 정리하지 않는다. 먼저 이 관찰을 Vitest의 `fetch` mock과 저장소 fixture로 기록한다.

## Implementation steps and file responsibilities

1. `src/services/apiClient.test.js`와 필요한 store/storage 테스트에 현행 성공 envelope, bare 오류, wrapped HTTP 오류와 HTTP 200 오류, 만료 후 재시도, 동시 만료 요청을 특성화한다.
2. `src/services/apiClient.js`에서 오류 정보를 한 번만 추출한다. `ApiError.message`와 `exceptionCode`는 안쪽 오류 객체에서 읽고, 기존 `status`는 실제 HTTP status로 유지한다. 논리 status가 다르면 별도 필드로 보존하며 원본 payload를 남긴다. 성공 응답의 `response` 추출 규칙은 유지한다. 비 JSON·빈 응답과 네트워크 실패의 기존 호출자 영향도 확인한다.
3. 재발급 요청은 본문 `refreshToken`만 사용하고 자동 access header를 보내지 않는다. 성공 필드는 서버 DTO의 `newAccessToken`만 사용한다. `withAuth=false` 요청과 재발급 요청 자체는 자동 재발급 대상에서 제외한다.
4. 동일한 만료 시점의 요청들은 진행 중인 재발급 Promise를 공유한다. 대기 중 다른 요청이 이미 저장된 access token을 갱신했다면 새 재발급 없이 현재 token으로 원 요청을 한 번 재시도한다. 재발급 Promise는 성공·실패 모두 끝난 뒤 해제한다.
5. `src/store/authStore.js`에 저장소 반영·동시 clear 진입점을 두고 `apiClient`가 이를 사용한다. 기존 `src/utils/tokenStorage.js`는 실제 브라우저 저장소 접근만 담당한다. 재발급 실패 또는 refresh token 부재 시 로드맵의 정책대로 저장소와 store를 함께 clear하고 `AuthExpiredError` 계열로 전달한다. 재발급 실패를 자동 재시도하지 않는다. 이 정책은 네트워크 일시 장애에서도 로컬 로그아웃을 일으키므로 Human Review에서 명시적으로 확인한다.
6. 변경 후 기존 page/service 호출자가 `ApiError`를 어떻게 표시하는지 최소 범위에서 확인한다. 이 Plan에서는 화면별 오류 문구를 재설계하지 않는다.

## State and error rules

- 재발급은 유효한 refresh token이 있고, 인증이 필요한 요청의 `TOKEN_HAS_EXPIRED`가 확인된 경우에만 시작한다. `NO_PERMISSION`, 다른 401, 일반 HTTP 200 오류는 재발급 트리거가 아니다.
- 성공 시 새 access token을 저장한 뒤 store를 갱신하고 원 요청을 각 1회만 재시도한다. 두 번째 만료 응답은 그대로 오류로 전달한다.
- 재발급 실패 시 저장소/store를 함께 clear한다. 재발급 요청 자체, 원 요청의 mutation, 다른 API를 무조건 재호출하지 않는다.
- `AuthExpiredError`는 `ApiError`의 한 형태로 유지할 수 있지만 최종 필드 정책은 동일해야 한다.

## Acceptance criteria

- wrapped/bare 오류와 HTTP 200 `success=false`가 같은 `ApiError` 필드를 채워 호출자에게 전달된다.
- 동시에 만료된 요청은 재발급 호출 하나를 공유한다. 원 요청은 성공 후 한 번만 재시도된다.
- 재발급 요청에 refresh token Authorization 헤더가 없고 본문 필드와 성공 응답 필드가 최신 DTO와 일치한다.
- 성공·실패·refresh token 부재 후 저장소와 `authStore` 인증 상태가 일치한다.
- 공개 요청과 재발급 요청은 자동 재발급을 순환 호출하지 않는다.

## User verification and review

구현 후 사용자가 `cd tag-note-react`에서 `npm run test`, `npm run lint`, `npm run build`를 실행하고 결과를 전달한다. 브라우저에서는 만료 access token의 단일·동시 요청, 재발급 실패 후 Header 인증 표시를 확인한다. 코드 diff에서 기존 성공 응답, 중복 요청, 무한 재시도, 인증 상태 경합을 검토한다. 사용자 결과를 받기 전 완료로 옮기지 않는다.

2026-10-21 정적 검토: 최신 서버 `UserApi` → `UserController` → 재발급 DTO → JWT 필터·예외 처리를 다시 확인했다. `GOOGLE_RELOGIN_REQUIRED`의 문서와 advice HTTP status 충돌은 유지된다. 성공 envelope, bare/wrapped 오류, HTTP 200 논리 오류, 단일·동시 만료, 중복 재발급 방지, 재발급 실패·refresh token 부재, 공개 요청과 재발급 요청 제외를 `apiClient.test.js`에 기록했다. diff 및 CRLF 허용 정적 검사는 확인했으며 실행 검증 결과는 아직 없다.

사용자 첫 `npm run test` 결과: Vitest 시작 전에 Rolldown의 `node:util.styleText` import에서 `SyntaxError`가 발생했다. 테스트 케이스는 실행되지 않았다. 현재 lockfile의 엔진 조건은 Vite/Rolldown `^20.19.0 || >=22.12.0`, Vitest `^22.12.0 || ^24.0.0 || >=26.0.0`, jsdom `^20.19.0 || ^22.13.0 || >=24.0.0`이다. 사용자의 PowerShell Node 버전 확인 및 지원 버전 전환 후 재실행이 필요하다. `npm run lint`와 `npm run build` 결과도 아직 받지 않았다.

사용자 재실행 결과: 테스트가 시작됐으며 `apiClient.test.js`의 공개 요청/재발급 요청 제외 테스트 한 건이 실패했다. 원인은 mock이 동일한 `Response` 인스턴스를 두 번 반환해 두 번째 body 읽기가 실패한 것이었다. 호출마다 새 `Response`를 생성하도록 fixture를 수정했다. 재실행 결과는 아직 받지 않았다.

2026-10-26 사용자 최종 확인: 수정 후 `npm run test`, `npm run lint`, `npm run build` 및 요청한 브라우저 동작을 모두 확인했다고 전달받았다. 최종 실행 로그와 개별 테스트 수치는 전달받지 않았으므로 수치를 기록하지 않는다. 수용 기준 및 diff 검토를 완료하고 이 Plan을 `completed/`로 이동한다.

## Open decisions and rollback

- `GOOGLE_RELOGIN_REQUIRED`의 문서/Controller advice status 충돌은 서버 담당 확인이 필요하다. 구현은 상태 코드 하나에 의존하지 않는다.
- 일시적 네트워크 실패에도 즉시 clear하는 정책은 승인된 Plan대로 구현하고 사용자 검증을 완료했다.
- 문제가 생기면 `apiClient.js`와 `authStore.js`의 변경을 독립적으로 되돌리고 추가한 특성화 테스트를 회귀 근거로 유지한다. 후속 M2-2는 M2-1의 수용 기준이 끝난 뒤 착수한다.
