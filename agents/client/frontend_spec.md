# #tagnote 프론트엔드 스펙

## 1. 역할

`tag-note-react`는 기존 `tag-note-front`의 바닐라 JavaScript 화면을 React 기반 SPA로 전환한 프론트엔드 애플리케이션이다.

프론트엔드의 역할은 다음과 같다.

- 음악 검색, 트랙 상세, 사용자 커스텀 태그 게시글, 댓글, 좋아요, 알림, 인증 흐름을 React 화면으로 제공한다.
- 서버 API와 직접 통신하는 진입점은 `services` 계층으로 모은다.
- 페이지 컴포넌트는 라우팅 단위의 화면 조립을 담당하고, 복잡한 상태/비즈니스 흐름은 feature hook 또는 feature 내부 모듈로 분리한다.
- 전역 인증 상태, 알림 상태처럼 여러 화면에서 공유되는 값은 `store` 계층으로 관리한다.
- 브라우저 저장소, 쿠키, 라우트 생성처럼 앱 전반에서 재사용되는 순수 유틸리티는 `utils` 계층으로 분리한다.
- 기존 바닐라 JS의 DOM 직접 조작 방식은 React state, props, effect, component composition으로 대체한다.

프론트엔드 구현 원칙은 다음과 같다.

- `pages`는 가능한 얇게 유지한다. 데이터 로딩, mutation, 복잡한 폼 상태, API 응답 정규화가 커지면 hook 또는 feature 모듈로 이동한다.
- `features`는 단순 re-export 폴더가 아니라 도메인별 UI, hook, model, helper를 모으는 단위로 사용한다.
- `services`는 API endpoint 호출만 담당한다. 화면 상태, navigation, alert/confirm 같은 UI 관심사는 포함하지 않는다.
- 같은 API 동작에 대해 여러 alias 메서드를 장기 유지하지 않는다. 전환 중 임시 alias가 필요하면 정리 대상임을 명확히 한다.
- 인증 상태는 `tokenStorage` 직접 조회보다 `authStore`/`useAuthStore` 기반 접근을 우선한다.
- `window`, `document`, `localStorage`, `sessionStorage` 사용은 `utils`, custom hook, layout-level component에 최대한 격리한다.

## 2. 시스템 아키텍처

프론트엔드는 Vite + React + React Router 기반 SPA로 구성한다.

### 2.1 App 계층

위치: `tag-note-react/src/app`

역할:
- React 앱의 최상위 provider와 router를 정의한다.
- route table을 한 곳에서 관리한다.
- route path string은 직접 하드코딩하지 않고 `utils/routes.js`의 `routes`, `buildRoute`를 사용한다.

구성 기준:
- `App.jsx`: 앱 provider 조립만 담당한다.
- `router.jsx`: route tree, layout, errorElement, page 연결을 담당한다.

주의:
- 페이지별 데이터 로딩 로직을 `router.jsx`에 직접 넣지 않는다.
- route path와 URL 생성 규칙을 여러 파일에 흩뜨리지 않는다.

### 2.2 Pages 계층

위치: `tag-note-react/src/pages`

역할:
- 라우트에 직접 매핑되는 화면 단위 컴포넌트를 둔다.
- layout, feature component, common component를 조립한다.
- URL param, search param, navigation처럼 라우팅에 가까운 책임을 가진다.

구성 기준:
- `HomePage.jsx`: 홈, 검색 진입, 최근 태그/랭킹 노출
- `TracksPage.jsx`: 트랙 검색 결과
- `TrackDetailPage.jsx`: 트랙 상세와 해당 트랙의 게시글 목록
- `BoardDetailPage.jsx`: 게시글 상세, 좋아요, 댓글 조립
- `BoardEditPage.jsx`: 게시글 작성/수정 화면
- `LoginPage.jsx`, `TokenPage.jsx`, `NicknamePage.jsx`: 인증/가입 흐름
- `ExceptionPage.jsx`: 라우팅 및 오류 화면

주의:
- API 호출과 화면 상태가 150~200라인 이상으로 커지는 page는 feature hook으로 분리한다.
- form validation, file preview, pagination 계산, API response normalization을 page 안에 계속 누적하지 않는다.
- page 내부 helper는 해당 화면에서만 쓰는 작은 함수로 제한한다.

### 2.3 Features 계층

위치: `tag-note-react/src/features`

역할:
- 도메인별 사용자 기능을 응집도 있게 모은다.
- 여러 page에서 공유되거나 자체 상태가 큰 UI 컴포넌트는 feature 하위에 둔다.
- feature 내부에서 필요한 hook, model mapper, local helper를 함께 관리한다.

권장 하위 구조:

```text
features/
  boards/
    components/
    hooks/
    model/
    index.js
  comments/
    components/
    hooks/
    model/
    index.js
  notifications/
    components/
    hooks/
    model/
    index.js
  tracks/
    components/
    hooks/
    model/
    index.js
  auth/
    hooks/
    model/
    index.js
```

현재 구조에서 우선 분리할 후보:
- `BoardEditPage`의 게시글 폼 상태, 유저 태그 검증, 파일 미리보기, 저장 흐름
- `BoardDetailPage`의 게시글 상세 조회, 작성자 확인, 좋아요 상태
- `TrackDetailPage`의 트랙 상세 조회와 게시글 목록 pagination
- `TracksPage`의 검색 결과 조회와 page query 동기화
- `CommentSection`의 댓글 조회/작성/수정/삭제/답글 상태
- `NotificationCenter`의 SSE 구독, toast, dropdown, 읽음 처리

주의:
- `features/*/index.js`는 단순히 `services`를 다시 export하는 용도로만 두지 않는다.
- feature에서 page route path를 직접 조합해야 하면 `utils/routes.js`의 `buildRoute`를 사용한다.
- feature component는 가능하면 props로 필요한 값과 handler를 받고, navigation 직접 수행은 page 또는 hook으로 격리한다.

### 2.4 Components 계층

위치: `tag-note-react/src/components`

역할:
- 특정 도메인에 강하게 묶이지 않는 재사용 UI를 둔다.
- 앱 레이아웃, 공통 header, empty state, page header 등 범용 UI를 관리한다.

구성 기준:
- `layout/`: 앱 shell, header, outlet 배치
- `common/`: 여러 화면에서 공유 가능한 작은 UI 컴포넌트

주의:
- board, comment, notification처럼 도메인 의미가 강한 컴포넌트는 `components/common`이 아니라 `features` 하위에 둔다.
- 공통 컴포넌트에 API 호출을 넣지 않는다.

### 2.5 Services 계층

위치: `tag-note-react/src/services`

역할:
- 서버 API 호출을 담당한다.
- `apiClient`를 통해 base URL, credentials, auth header, JSON parsing, token reissue, API error 처리를 공통화한다.

구성 기준:
- `apiClient.js`: fetch wrapper, token reissue, API error class
- `authService.js`: 로그인/가입/토큰/로그아웃 API
- `trackService.js`: 트랙 검색, 상세, 랭킹 API
- `boardService.js`: 게시글 목록/상세/작성/수정/삭제 API
- `commentService.js`: 댓글/답글 API
- `fileService.js`: 파일 업로드/조회/삭제 API
- `likeService.js`: 좋아요 API
- `notificationService.js`: 알림 목록, 읽음 처리, SSE 구독 API
- `userTagService.js`: 유저 태그 API

주의:
- 서비스 함수명은 하나의 기준으로 통일한다.
- page index는 UI 기준 1-base, API page는 0-base일 수 있으므로 변환 위치를 명확히 한다.
- API response shape 보정은 service 또는 feature model mapper 중 한 곳으로 통일한다.
- 서비스는 `navigate`, `window.confirm`, toast state 같은 UI 동작을 알지 않는다.

### 2.6 Store 계층

위치: `tag-note-react/src/store`

역할:
- 앱 전역에서 공유되는 최소 상태를 관리한다.
- React Context 없이 `useSyncExternalStore` 기반의 작은 store를 사용한다.

구성 기준:
- `createStore.js`: store factory
- `authStore.js`: access token, pending token, 인증 여부
- `notificationStore.js`: unread count, 최신 알림, SSE 구독 여부

주의:
- 서버 캐시성 데이터 전체를 전역 store에 넣지 않는다.
- 인증 상태는 `tokenStorage`와 store가 어긋나지 않도록 `refreshFromStorage`, `clear` 같은 동기화 진입점을 사용한다.
- 여러 화면에서 인증 여부가 필요하면 `tokenStorage.getAccessToken()` 직접 호출보다 `useAuthStore`를 우선한다.

### 2.7 Utils 계층

위치: `tag-note-react/src/utils`

역할:
- 도메인 UI와 무관한 작은 재사용 함수를 둔다.
- 브라우저 저장소, 쿠키, route builder를 격리한다.

구성 기준:
- `routes.js`: route path 상수와 URL builder
- `tokenStorage.js`: access/refresh/pending token 저장소
- `trackSearchStorage.js`: 검색 keyword/page와 선택 track session storage
- `cookie.js`: cookie read/delete helper

주의:
- storage key는 한 파일에서만 정의한다.
- URL 생성은 `buildRoute`를 사용해 중복 문자열을 줄인다.
- 브라우저 API를 쓰는 util은 SSR/test 환경을 고려해 `typeof window`, `typeof localStorage` 같은 guard를 둔다.

### 2.8 Styles 계층

위치: `tag-note-react/src/styles`

역할:
- 전역 스타일과 페이지/feature 단위 CSS를 관리한다.
- 디자인 스펙은 `agents/design_spec.md`를 따른다.

구성 기준:
- `globals.css`: reset, 전역 token, layout base
- page CSS: `home.css`, `tracks.css`, `trackDetail.css`, `boardDetail.css`, `boardEdit.css`, `auth.css`
- feature CSS: `commentSection.css`, `notificationCenter.css`

주의:
- 도메인 feature CSS는 해당 feature component와 이름을 맞춘다.
- 오래된 바닐라 JS CSS 파일명을 무조건 유지하지 않는다. React 컴포넌트 경계에 맞춰 이름을 조정한다.
- 전역 class 충돌을 줄이기 위해 page/feature prefix를 일관되게 사용한다.

## 3. Repo 구조

전체 repository는 Spring/Gradle backend와 기존 vanilla frontend, React frontend가 함께 있는 monorepo 형태다.

```text
tag-note/
  agents/
    design_spec.md
    frontend_spec.md

  tag-note-front/
    index.html
    tracks.html
    detail.html
    board.html
    edit.html
    login.html
    token.html
    nickname.html
    exception.html
    css/
    js/
    image/

  tag-note-react/
    .env.example
    .gitignore
    eslint.config.js
    index.html
    package.json
    package-lock.json
    vite.config.js
    src/
      main.jsx
      app/
        App.jsx
        router.jsx
      components/
        common/
          EmptyState.jsx
          PageHeader.jsx
        layout/
          AppLayout.jsx
      features/
        auth/
          index.js
        boards/
          index.js
        comments/
          CommentSection.jsx
          index.js
        files/
          index.js
        userTags/
          index.js
        notifications/
          NotificationCenter.jsx
          index.js
        tracks/
          index.js
      pages/
        HomePage.jsx
        TracksPage.jsx
        TrackDetailPage.jsx
        BoardDetailPage.jsx
        BoardEditPage.jsx
        LoginPage.jsx
        TokenPage.jsx
        NicknamePage.jsx
        ExceptionPage.jsx
      services/
        apiClient.js
        authService.js
        boardService.js
        commentService.js
        fileService.js
        userTagService.js
        likeService.js
        notificationService.js
        trackService.js
      store/
        createStore.js
        authStore.js
        notificationStore.js
      styles/
        globals.css
        home.css
        tracks.css
        trackDetail.css
        boardDetail.css
        boardEdit.css
        auth.css
        commentSection.css
        notificationCenter.css
      utils/
        cookie.js
        routes.js
        tokenStorage.js
        trackSearchStorage.js

  tagnote-api/
  tagnote-core/
  tagnote-batch/
  build.gradle
  settings.gradle
  docker-compose.yml
```

### 3.1 전환 기준

`tag-note-front`는 기존 구현의 동작 참조로 취급한다. 신규 기능과 구조 개선은 `tag-note-react`를 기준으로 한다.

전환 시 비교 기준:
- HTML page 하나는 React `pages/*Page.jsx` 하나로 대응한다.
- `js/*Api.js` 계열은 `src/services/*Service.js`로 대응한다.
- DOM 조작/이벤트 바인딩 코드는 React component state와 event handler로 대응한다.
- 공통 header, notification, comment 같은 재사용 화면 조각은 `features` 또는 `components`로 이동한다.
- cookie/localStorage/sessionStorage 접근은 `utils`로 이동한다.

### 3.2 목표 구조

현재 구조에서 장기적으로 지향할 `tag-note-react/src` 구조는 다음과 같다.

```text
src/
  app/
  components/
    common/
    layout/
  features/
    auth/
      hooks/
      model/
    boards/
      components/
      hooks/
      model/
    comments/
      components/
      hooks/
      model/
    notifications/
      components/
      hooks/
      model/
    tracks/
      components/
      hooks/
      model/
  pages/
  services/
  store/
  styles/
  utils/
```

이 구조에서 page는 routing shell, feature는 도메인 기능, service는 API, store는 전역 상태, utils는 순수 보조 기능을 담당한다.

### 3.3 정리 우선순위

1. `features/*/index.js`가 단순 service re-export만 하는 상태를 줄인다.
2. `BoardEditPage`, `BoardDetailPage`, `CommentSection`, `NotificationCenter`의 상태/side effect를 custom hook으로 분리한다.
3. `boardService`, `trackService`의 중복 alias 메서드를 하나의 naming convention으로 통일한다.
4. 인증 여부 조회를 `useAuthStore` 중심으로 통일한다.
5. API response normalization 위치를 service 또는 feature model 중 한 곳으로 정한다.
6. `HomePage`의 중복 id, 잘못된 `aria-labelledby` 같은 전환 중 품질 이슈를 정리한다.

### 3.4 검증 기준

구조 변경 또는 기능 추가 후 최소 검증:

```bash
npm run lint
npm run build
```

추가 권장 검증:
- 주요 route 수동 확인: `/`, `/tracks`, `/tracks/:trackId`, `/boards/:boardId`, `/boards/new`, `/login`, `/nickname`
- 인증 상태 변경 후 header와 notification 표시 확인
- token reissue 실패 시 로그아웃/인증 만료 흐름 확인
- 댓글 작성/수정/삭제 후 pagination과 작성 가능 댓글 목록 갱신 확인
- 파일 업로드/삭제 후 object URL revoke와 preview 표시 확인
