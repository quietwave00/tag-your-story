# TagNote React 세션 가이드

## 시작할 때 읽을 문서

1. 저장소 루트 `AGENTS.md` — 공통 작업·검증 규칙
2. `agents/client/frontend_spec.md` — 클라이언트 구조와 기능 책임
3. `agents/client/design_spec.md` — 시각 언어와 금지사항
4. `agents/client/plans/FRONTEND-ROADMAP.md` — 마일스톤 순서와 의존성
5. 이번 작업의 `agents/client/plans/active/` Plan — 승인된 변경 범위와 수용 기준
6. `agents/client/api-contract.md` — 도메인·API별 서버 계약 기준선

`tag-note-react/`에서 작업할 때도 위 문서를 읽는다. 여러 Plan이 `active/`에 있어도 한 세션에서는 하나의 Plan만 구현한다. 현재 M2 구현 순서는 M2-1 → M2-2 → M2-3이며, 후속 Plan은 선행 Plan의 사용자 검증이 끝난 뒤 착수한다.

## 계약 확인과 문서 우선순위

- `api-contract.md`는 기준 서버 커밋을 명시한 **정적 조사 기록**이다. 새 세션에서 해당 API를 사용하기 전에 최신 서버 worktree의 공개 `*Api.java` → Controller → Request/Response DTO → 인증 필터·예외 처리·관련 ADR을 대조한다. 이 클라이언트 worktree에 포함된 서버 코드를 최신으로 가정하지 않는다.
- 이 환경에서는 Swagger/OpenAPI 접속을 시도하지 않는다. 사용자가 전달한 curl 응답만 실행 서버 계약의 근거로 추가한다. 정적 문서, Controller/DTO, 실행 응답이 다르면 충돌을 보고하고 임의로 선택하지 않는다.
- 클라이언트 구현 범위는 Human Review를 거친 해당 active Plan을 따른다. Plan과 최신 서버 계약이 충돌하면 구현 전에 보고한다. 확인되지 않은 endpoint, 응답 필드, 인증 동작은 추측하지 않는다.
- API 변경이 확인되면 `api-contract.md`의 해당 항목과 기준 근거를 갱신한다. 마일스톤 순서 변경은 `FRONTEND-ROADMAP.md`, 실행 범위 변경은 해당 Plan에 기록한다. 같은 계약 목록을 여러 문서에 복제하지 않는다.

## 구현·검증 규칙

- 기존 React 구현을 보존하며, 변경 전에 실제 service와 page/feature 호출자를 찾아 특성화한다. 제거할 endpoint나 alias는 서버 근거와 호출자 확인을 모두 거친다.
- API 호출은 `src/services/`, 공유 인증 상태는 `src/store/authStore.js`, 브라우저 저장소는 `src/utils/`가 담당한다. page나 공통 UI에 endpoint 호출 또는 token 직접 판독을 새로 넣지 않는다.
- API와 화면의 page 번호 변환은 service 또는 feature mapper 한 곳에서 처리한다. 확인되지 않은 page 기준을 가정하지 않는다.
- 테스트·lint·build는 사용자가 실행한다. 실행하지 않은 검증을 통과로 기록하지 않고, 전달받은 결과와 diff를 검토한 뒤 Plan 완료 여부를 갱신한다.
