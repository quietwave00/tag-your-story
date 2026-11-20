# Client UI 재사용 규칙

`tag-note-react/`에서 화면을 추가하거나 고칠 때 적용한다. 구조와 책임은 `frontend_spec.md`, 시각 언어는 `design_spec.md`가 기준이다. 이 문서는 **기존 UI를 어디서 찾고 어떻게 재사용할지** 정한다.

## 작업 시작 순서

1. 같은 역할의 컴포넌트·훅·스타일과 실제 호출자를 검색한다. 예: `rg -n 'BackButton|useDetailScrollTop|detail-back-button' tag-note-react/src`.
2. 기존 공통 코드가 있으면 그 진입점으로 연결한다. 다른 화면의 마크업·effect·CSS를 복사해 새 변형을 만들지 않는다.
3. 기존 코드로 표현할 수 없는 차이가 있으면 먼저 데이터·이벤트·접근성 라벨을 prop으로 전달할 수 있는지 본다. 화면별 API 호출·라우트 결정은 page/feature에 남긴다.
4. 예외가 필요하면 해당 active Plan에 **기존 코드, 재사용할 수 없는 이유, 검증할 화면**을 짧게 기록한다.

## 현재 공통 진입점

| 역할 | 재사용할 코드 | 페이지가 맡는 일 |
| --- | --- | --- |
| 상세 뒤로가기 버튼의 모양·아이콘 | `src/components/common/BackButton.jsx`, `src/styles/globals.css`의 `.detail-back-button` | 클릭 시 이동 대상과 필요 시 접근성 라벨 |
| 상세 진입 시 맨 위로 이동 | `src/hooks/useDetailScrollTop.js` | 조회 대상 ID를 훅에 전달 |
| 클라이언트 경로 생성 | `src/utils/routes.js`의 `routes`, `buildRoute` | 어떤 화면으로 갈지 결정하고 필요한 navigation state 전달 |
| 앱 공통 헤더·본문 폭·푸터 | `src/components/layout/AppLayout.jsx` | 페이지 안의 콘텐츠 구성 |
| 상세 화면의 최대 폭·뒤로가기 시작점 | `src/styles/globals.css`의 `.detail-page-frame`, `.detail-back-button` | 상세 페이지 최상위에 공통 class를 붙이고 이동 대상 결정 |
| 작성자 기본 아바타 | `src/components/common/UserAvatar.jsx`, `src/styles/globals.css`의 `.user-avatar` | 닉네임을 전달하고, 실제 닉네임 텍스트를 함께 표시 |
| 데이터 조회 스켈레톤 블록 | `src/styles/globals.css`의 `.skeleton-block` | 단색 블록으로 실제 이미지·행·카드 배치를 닮은 구조와 번역된 숨김 `role="status"` 구성 |
| 도메인별 댓글·알림 UI | `src/features/comments/CommentSection.jsx`, `src/features/notifications/NotificationCenter.jsx` | 해당 feature가 제공하는 props·이벤트 연결 |

표에 없는 코드가 자동으로 공통 컴포넌트가 되는 것은 아니다. 같은 **의미와 상호작용**이 두 곳 이상에서 반복될 때 추출한다. 화면 하나에만 필요한 표현은 그 화면에 둔다. 공통 컴포넌트에 endpoint 호출이나 특정 페이지의 뒤로가기 목적지를 넣지 않는다.

## 상세 화면 필수 계약

- Track·Artist·Album·Board 상세는 `BackButton`과 `useDetailScrollTop`을 사용한다. 화면별 뒤로가기 버튼·아이콘·진입 스크롤 effect를 새로 만들지 않는다.
- 네 상세 화면의 최상위 래퍼는 `.detail-page-frame`을 사용해 `980px` 최대 폭과 동일한 뒤로가기 시작점을 공유한다. 페이지 CSS에서 별도의 최대 폭으로 재정의하지 않는다.
- 훅에는 해당 상세의 ID를 전달한다. 대상 ID가 달라지거나 상세 화면에 새로 진입하면 첫 페인트 전에 `scrollTo(0, 0)`이 실행되어 뒤로가기 버튼이 보여야 한다.
- 뒤로가기의 **목적지**는 페이지가 결정한다. 검색어·페이지·이전 상세 화면 등 기존 navigation state를 보존한다. 공통 버튼은 이동 정책을 갖지 않는다.
- 목록 내부 페이징의 스크롤·focus는 상세 진입 스크롤과 별개로 해당 목록에서 처리한다.

## 공통화할 때의 경계

- 공통 UI의 시각 변경은 공통 컴포넌트 또는 공통 스타일에서 수정하고, 모든 호출 화면을 확인한다. 페이지 CSS로 공통 컨트롤을 다시 그리지 않는다.
- 문구와 접근성 라벨은 기존 i18n key를 사용한다. 공통 UI에 API 콘텐츠를 번역 key로 넣지 않는다.
- 공통화가 기존 동작을 바꾸면 먼저 호출자의 현 동작을 테스트로 고정한 뒤 교체한다. 한 화면의 특수 요구를 위해 공통 컴포넌트에 여러 화면별 분기를 쌓지 않는다.
- 재사용 목록이 바뀌면 이 표와 실제 호출자를 함께 갱신한다. 존재하지 않는 공통 컴포넌트를 문서에 적지 않는다.

## 변경 후 확인

- 직접 URL 진입과 스크롤된 화면에서의 내부 이동 모두에서 상세가 맨 위로 열리고 뒤로가기 버튼이 보이는지 확인한다.
- 공통 컨트롤의 시각·키보드 focus·접근성 라벨·클릭 결과를 사용하는 화면에서 확인한다.
- 원래 화면으로 돌아갈 때 검색어·페이지·상세 이동 상태가 유지되는지 확인한다.
- 관련 회귀 테스트 코드를 갱신한다. 실행 검증 명령과 완료 판정은 `AGENTS.md` 및 루트 `AGENTS.md`의 사용자 실행 규칙을 따른다.
