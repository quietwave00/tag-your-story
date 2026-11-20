# AUTH-EMAIL-001 — 이메일 로그인과 자체 회원가입

## 완료 처리 기록

- 2026-11-16 사용자 지시로 이 Plan의 관리 상태를 완료로 변경한다. 아래의 검증 대기 기록은 완료 처리 이전에 작성된 이력이다.
- `npm run test`, `npm run lint`, `npm run build` 실행 결과와 실제 이메일·쿠키 브라우저 검증 결과는 전달받지 않았다. 검증 통과로 기록하지 않는다.

## 범위와 선행 조건

- 로그인 화면에서 이메일·비밀번호 로그인과 자체 회원가입 진입을 제공한다.
- 이메일·비밀번호·비밀번호 확인으로 가입을 시작하고, 이메일로 받은 6자리 코드를 확인해 가입을 마친다. 코드 재발송과 서버 오류 안내를 포함한다.
- 완료 시 서버가 발급한 access/refresh cookie를 기존 `tokenStorage`와 `authStore` 경로로 동기화한다.
- Google 로그인과 기존 인증 필요 화면의 동작은 유지한다.
- 회원가입에서 닉네임은 입력받지 않는다. 서버가 자동 생성하며 프로필 변경은 이 마일스톤에 포함하지 않는다.
- 비밀번호 찾기, 로그인 유지, 아이디 찾기는 서버 계약이 없어 제공하지 않는다.

## 계약 근거와 착수 위험

- `agents/client/api-contract.md`의 정적 기록과 2026-10-26 사용자 제공 명세에는 공개 `POST /api/auth/login`, `/api/auth/signup/email/start`, `/resend`, `/confirm`이 있다. 로그인은 `email`, `password`, start는 `email`, `password`, `passwordConfirm`, resend는 `challengeId`, confirm은 `challengeId`와 6자리 `code`를 사용한다. start는 `challengeId`, login/confirm은 USER token cookie와 `userId`·`nickname`을 반환한다.
- 기존 React에는 이메일 가입 service, 화면, route가 없다. Google OAuth와 cookie 동기화만 구현되어 있다.
- 지침에서 지정한 `D:\p\tag-your-story\migration-server` worktree는 현재 환경의 `/mnt/d/p/tag-your-story/migration-server`에서 찾을 수 없다. 사용자 제공 계약을 구현 근거로 삼고, `/mnt/d/p/tag-your-story`의 `EmailLoginApi`/`EmailSignupApi` → Controller/DTO → 서비스·예외 처리를 대조했다. 이 서버 코드를 지정 worktree의 최신 상태라고 주장하지 않는다. 실행 Swagger 접속은 시도하지 않는다.
- 화면에서 추측한 필드나 인증 동작을 추가하지 않는다. 계약이 다르면 이 초안을 수정해 다시 검토받는다.

## 구현 순서

1. 기존 로그인·OAuth·쿠키 동기화 동작을 특성화 테스트로 고정한다.
2. `src/services/authService.js`에 이메일 로그인, 가입 시작, 재발송, 코드 확인 호출을 추가하고 공개 요청임을 명시한다. 새 API 호출을 page에 두지 않는다.
3. `src/pages/SignupPage.jsx`와 필요한 `src/features/auth/` 상태 처리를 만든다. 입력 검증은 서버 계약의 비밀번호 조건과 6자리 코드 조건만 따른다. 비밀번호를 브라우저 저장소에 기록하지 않는다.
4. `src/utils/routes.js`와 `src/app/router.jsx`에 가입 route를 연결하고 로그인 화면에서 번역된 링크를 제공한다. 로그인 화면에 이메일·비밀번호 입력, 표시/숨김, 오류 안내, Google 로그인 구분을 추가한다. 이미 인증된 사용자의 가입 화면 진입은 홈으로 돌린다.
5. confirm 성공 후 기존 cookie 동기화 유틸리티와 `authStore`를 갱신한다. cookie가 누락되면 가입 완료를 로그인 완료로 표시하지 않고 안내한다. 로그아웃 표식이 가입 성공을 막지 않도록 기존 수명주기를 확인한다.
6. `auth.json`의 한국어·영어 UI 문구와 `auth.css`의 디자인을 추가한다. `design_spec.md`의 무채색, 산세리프, 낮은 대비의 blur와 여백을 사용한다. 점무늬 noise, 과한 선과 그림자는 사용하지 않는다.
7. 가입 시작·확인·재발송·오류·키보드 접근·인증 상태 전환 테스트를 추가하고 diff를 검토한다.

사용자 명세의 구체 조건: 이메일은 앞뒤 공백 제거·소문자화·255자 이하이며 Gmail 점/+ 주소는 합치지 않는다. 비밀번호는 8자 이상, 영문자와 숫자를 각각 포함, UTF-8 72바이트 이하이고 특수문자는 선택이다. 인증 입력은 실제로 하나의 6자리 숫자 입력이며 `inputMode=numeric`, 붙여넣기, `autocomplete=one-time-code`를 지원한다. 코드 10분, 재발송 60초, 최초 발송 포함 5회 제한은 화면 카운트다운으로 안내하되 서버 응답을 최종 기준으로 한다. 닉네임·내부 식별자·역할·상태는 가입 입력에 넣지 않는다.

## 수용 기준과 사용자 검증

- 로그인 화면에서 이메일 로그인·Google 로그인·회원가입 진입을 사용할 수 있고, 가입 화면에서 로그인으로 돌아올 수 있다.
- 필수 입력과 비밀번호 조건을 확인하고, start 성공 후 코드 확인 단계로 이동한다.
- 유효한 코드의 confirm 성공 시 인증 상태가 갱신된다. 실패와 재발송 제한은 서버 오류를 번역된 문구로 안내하며 입력한 이메일을 보존한다.
- Google 로그인 흐름과 기존 인증 상태가 퇴보하지 않는다.
- 이메일 로그인은 잘못된 이메일·비밀번호를 같은 문구로 안내하고, Google 계정·비활성 계정·시도 제한·서비스 장애를 구분한다. 비밀번호 찾기·로그인 유지·아이디 찾기는 표시하지 않는다.
- 사용자가 `cd tag-note-react && npm run test`, `npm run lint`, `npm run build`를 실행하고 결과를 전달한다. 브라우저에서 실제 이메일 수신, 코드 확인, cookie 반영과 새로고침 후 인증 상태를 검증한다.

## Human Review 기록

- 2026-10-26 사용자가 이메일 로그인, 가입 1단계, 6자리 인증 2단계, 오류 상태와 디자인·접근성 요구를 구체적으로 전달했다. 그 범위를 승인된 작업 지시로 삼는다.
- 지정 worktree가 부재한 계약 위험은 남는다. 사용자 명세와 현재 접근 가능한 서버 코드가 충돌하면 구현을 멈추고 보고한다.

## 구현 및 검증 상태

- 로그인 폼, Google 버튼, 회원가입 진입 링크, 가입 시작·코드 확인·재발송·성공 화면과 한국어/영어 문구를 구현했다. API 호출은 `authService`, 쿠키와 공유 인증은 기존 유틸리티·store를 경유한다.
- 이메일 로그인 오류, 쿠키 동기화, 쿠키 누락, 가입 시작·확인, 입력 검증, 재발송 대기시간의 테스트를 추가했다. 화면의 10분·60초 카운트다운은 안내이며 서버 오류가 최종 기준이다.
- 정적 diff 검토와 변경 파일의 `git diff --check`만 수행했다. `npm run test`, `npm run lint`, `npm run build` 및 실제 이메일·쿠키 브라우저 흐름은 사용자의 실행 결과를 기다린다. 그 전에는 Plan을 `completed/`로 옮기지 않는다.
