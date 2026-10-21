# TagNote React Frontend Roadmap

## 문서 역할과 현재 상태

- 이 문서는 클라이언트 마일스톤의 **순서와 의존성**을 관리한다. 아키텍처·시각 언어는 `agents/client/frontend_spec.md`, `design_spec.md`, 세션 규칙은 `agents/client/AGENTS.md`, 도메인별 서버 계약은 `agents/client/api-contract.md`를 따른다.
- 갱신일: 2026-10-21. 대상: `tag-note-react/`. 기존 React 구현은 유지하며 작은 마일스톤으로 진행한다.
- 완료 기반: Vitest 테스트 명령, `I18N-001` 한국어/영어 전역 UI 다국어화, Header 메뉴 재구성.
- `M2-1`, `M2-2`, `M2-3`은 Human Review가 끝나 `active/`에 등록됐다. 현재 다음 구현 대상은 **M2-1**이다. 세 Plan은 M2-1 → M2-2 → M2-3 순서로 실행하며, 후속 Plan은 선행 Plan의 수용 기준과 사용자 검증이 끝난 뒤 착수한다. `active/`에 여러 승인 Plan이 있어도 한 세션에서는 하나만 구현한다.
- API 계약 목록은 M1 마일스톤 대신 `agents/client/api-contract.md`에 유지한다. 각 Plan 착수 시 해당 API를 최신 서버 worktree에서 다시 확인하고 문서·구현 충돌을 보고한다.

## 마일스톤 순서

1. **M2-1 — API 오류와 access token 재발급 정합성**
   - Plan: [active/M2-1-api-error-and-reissue.md](active/M2-1-api-error-and-reissue.md)
   - 선행: 없음. 다음 구현 대상.
2. **M2-2 — OAuth 쿠키와 인증 수명주기 정합성**
   - Plan: [active/M2-2-oauth-and-auth-lifecycle.md](active/M2-2-oauth-and-auth-lifecycle.md)
   - 선행: M2-1 수용 기준 및 사용자 검증 완료.
3. **M2-3 — 인증 필요 mutation의 로그인 유도**
   - Plan: [active/M2-3-protected-mutation-navigation.md](active/M2-3-protected-mutation-navigation.md)
   - 선행: M2-1, M2-2 수용 기준 및 사용자 검증 완료.
4. **M3 — App shell과 디자인 토큰 정렬**
   - 선행: 없음. M2와 독립적으로 계획할 수 있다.
5. **M4 — 홈과 Catalog 통합 검색**
   - 선행: M3.
6. **M5 — Track 상세와 공개 Board 읽기 흐름**
   - 선행: M4.
7. **M6 — 보호된 Board 작성·수정·삭제와 파일**
   - 선행: M2, M5.
8. **M7 — 댓글·답글·좋아요**
   - 선행: M2, M5.
9. **M8 — 알림 SSE와 전역 상태**
   - 선행: M2, M7.
10. **M9 — User Tag 탐색, Contact, 접근성 및 출시 검증**
    - 선행: M3~M8.

M3 이후의 상세 범위와 수용 기준은 실제 착수할 작은 단위의 Plan에서 정의하고 Human Review를 받는다. 구현·테스트·빌드 검증과 `completed/` 이동은 각 Plan 및 루트 `AGENTS.md`의 사용자 검증 규칙을 따른다.
