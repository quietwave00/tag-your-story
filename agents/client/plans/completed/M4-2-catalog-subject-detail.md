# M4-2 — Artist·Album 상세 API 연결

## 완료 처리 기록

- 2026-11-16 사용자 지시로 이 Plan의 관리 상태를 완료로 변경한다. 아래의 검증 대기 기록은 완료 처리 이전에 작성된 이력이다.
- `npm run test`, `npm run lint`, `npm run build` 및 실행 서버·브라우저 상세 검증 결과는 전달받지 않았다. 검증 통과로 기록하지 않는다.

## 범위와 근거

- 사용자가 새로 만든 Artist·Album 조회 API를 기존 상세 경로에 연결하도록 요청했다. 검색 화면의 `/artists/{spotifyArtistId}`, `/albums/{spotifyAlbumId}` 이동 경로를 유지한다.
- 현재 접근 가능한 `/mnt/d/p/tag-your-story/tagnote-api`의 `CatalogDetailApi` → `CatalogDetailController` → 상세·목록 응답 DTO를 정적으로 확인했다. 지정된 `migration-server` worktree와 실행 서버 응답은 확인하지 못했다.
- 이 마일스톤은 기존 안내 화면을 상세·관련 목록 조회 화면으로 바꾼다. 검색·Track 상세·Board 기능은 범위에 포함하지 않는다.

## 수용 기준

1. Artist 상세의 이름·이미지·장르와 해당 Artist의 Album 목록을 보여준다.
2. Album 상세의 제목·이미지·발매일·참여 Artist·Track 수와 해당 Album의 Track 목록을 보여준다.
3. Artist의 Album 목록은 `releaseType`별 서버 분류 페이지(10건)를 제공하고 선택 타입의 페이지를 각각 기억한다. null 유형은 전체에만 포함한다. UI page 1-based와 API page 0-based 변환은 service에 둔다. Album의 Track 목록은 서버 페이지를 모두 읽고 페이징 없이 표시한다.
4. 상세와 목록의 로딩·실패 상태를 독립적으로 처리하며 오래된 응답은 표시하지 않는다. 검색으로 돌아가기와 Artist→Album, Album→Artist 이동을 유지한다.
5. 한국어·영어 UI 문구와 특성화 테스트를 추가한다.
6. 2026-10-28 후속 사용자 결정에 따라 Track·Album·Artist의 클라이언트 상세 URL을 `ID/이름-slug`로 만든다. 기존 ID 전용 URL은 받아서 상세 조회 후 canonical 주소로 `replace`한다. 서버 상세 API는 ID 기준으로 유지한다.
7. 2026-11-02 사용자 요청에 따라 Artist Album 목록과 Album 상세의 nullable `releaseType`을 서버 응답 그대로 표시한다. 분류 불가(null)는 유형 문구를 표시하지 않고, UI에서 별도 분류 계산을 하지 않는다.

## 검증 상태

- 코드 정적 검토와 diff 검사 후 사용자 실행 검증 필요: `cd tag-note-react && npm run test && npm run lint && npm run build`.
- 실행 서버 계약과 실제 브라우저 화면은 사용자 검증 전까지 미확인이다.

## Human Review 기록

- 2026-10-28 사용자가 새 Artist·Album 조회 API를 `/artist`, `/album` 페이지에 연결하도록 직접 요청했다. 기존 실제 경로가 복수형 `/artists/:spotifyArtistId`, `/albums/:spotifyAlbumId`인 점을 확인하고 해당 경로의 안내 화면에 연결한다.
- 같은 날 사용자가 상세 URL의 이름 slug를 요청했고, 동명 처리 때문에 최종적으로 `ID/slug` 구조를 선택했다. 서버 코드 수정은 금지하고 서버 모델 전달 지침서를 별도로 작성하도록 지정했다. 이 지시를 상세 페이지 이동 경로 확장의 Human Review로 기록한다.
- 2026-11-02 사용자가 `api-contract.md`의 Artist Album 목록 및 Album 상세 `releaseType` 계약 확인과 그에 따른 구현을 요청했다. 서버 `CatalogDetailApi` → Controller → DTO에서 두 필드를 확인하고, 과거의 클라이언트 전체 목록 스캔·캐시 설명을 현재 서버 필터 계약에 맞춰 수정했다.
