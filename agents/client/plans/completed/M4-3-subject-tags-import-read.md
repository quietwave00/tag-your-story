# M4-3 — Track·Album 상세 태그 import 후 조회

## 완료 처리 기록

- 2026-11-16 사용자 지시로 이 Plan의 관리 상태를 완료로 변경한다. 아래의 검증 대기 기록은 완료 처리 이전에 작성된 이력이다.
- `npm run test`, `npm run lint`, `npm run build` 및 실행 서버·브라우저 태그 검증 결과는 전달받지 않았다. 검증 통과로 기록하지 않는다.

## 범위와 근거

- 2026-11-02 사용자 요청: Track·Album 상세 진입 시 `POST /api/catalog/import` 완료 후 각 subject의 저장된 태그 GET을 호출한다.
- 접근 가능한 서버의 `CatalogImportApi` → Controller → DTO와 `CatalogSubjectTagsApi` → Controller → DTO, `CatalogSubjectTagsReadService`를 정적으로 대조했다. 지정된 `migration-server` worktree와 실행 응답은 확인하지 못했다.
- Artist import는 지원하지 않는다. 기존 메타데이터·게시글·목록 조회는 유지한다.

## 수용 기준

1. Track은 `{subjectType: "TRACK", spotifyId: trackId}`, Album은 `{subjectType: "ALBUM", spotifyId: spotifyAlbumId}`로 import한다.
2. import 성공 후에만 `/api/catalog/tracks/{spotifyTrackId}/tags` 또는 `/api/catalog/albums/{spotifyAlbumId}/tags`를 호출한다. 두 요청은 `src/services`를 거친다.
3. `CONFIRMED` 태그, `PREVIEW` 태그, `EMPTY` 상태를 구분해 표시한다. 태그 이름은 원문을 유지한다.
4. import 또는 GET 실패 시 태그 영역에 독립적인 오류를 표시하고 메타데이터·기존 목록은 유지한다. 대상 전환·언마운트 시 오래된 응답을 표시하지 않는다.
5. import→GET 순서와 실패 시 GET 중단, Track·Album 표시, Artist 제외를 회귀 테스트에 기록한다.

## 검증 상태

- 사용자 실행 필요: `cd tag-note-react && npm run test && npm run lint && npm run build`.
- 실행 서버 계약과 실제 브라우저 화면은 미검증. 사용자 결과 확인 전에는 `completed/`로 이동하지 않는다.

## Human Review 기록

- 2026-11-02 사용자가 두 요청의 순서·body·endpoint와 서버 read service를 직접 지정하고 이식을 요청했다. 이 범위를 현재 작은 마일스톤의 승인으로 기록한다.
