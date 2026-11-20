# M4-1 — Catalog 타입별 검색 화면

## 완료 처리 기록

- 2026-11-16 사용자 지시로 이 Plan의 관리 상태를 완료로 변경한다. 아래의 검증 대기 기록은 완료 처리 이전에 작성된 이력이다.
- `npm run test`, `npm run lint`, `npm run build` 및 실행 서버·브라우저 검색 검증 결과는 전달받지 않았다. 검증 통과로 기록하지 않는다.

## 범위와 근거

- 사용자 요청에 따라 기존 `TracksPage`를 Artist·Track·Album 세 영역이 한 화면에 세로로 배치되는 검색으로 변경한다. Artist는 이미지와 이름만 가로로, Track·Album은 세로 목록으로 표시한다. 기존 Track 상세 이동을 유지한다.
- 사용자 제공 목표 계약은 `GET /api/catalog/search?keyword=&type=TRACK|ARTIST|ALBUM&page=`이며 page는 0-based, `response.items`와 타입별 `totalCount`를 반환한다.
- 2026-10-27 참조 서버의 `CatalogSearchApi` → `CatalogSearchController` → DTO를 다시 확인했다. 현재 서버는 선택적 `type=TRACK|ARTIST|ALBUM`을 받고 선택 타입의 `items`와 `totalCount`를 반환한다. 홈 기본 검색은 `/tracks?keyword=`로 이동한 뒤 세 타입을 각각 `/api/catalog/search`로 조회한다. 백엔드 코드는 이 작업에서 변경하지 않는다.
- `GET /api/tracks/{trackId}`는 Spotify ID를 받는다. 검색 결과의 `spotifyId`로 기존 Track 상세 경로를 유지한다. 이 Plan 착수 당시 Artist·Album 상세 API가 확정되지 않아 경로와 안내 화면만 제공했다. 2026-10-28 상세 API 연결은 별도 `M4-2-catalog-subject-detail.md`에서 관리한다.
- 2026-10-28 사용자 지시로 검색 화면 조회를 `GET /api/catalog/search/screen?keyword=&trackPage=&albumPage=&artistPage=` 한 요청으로 변경한다. 세 page는 독립적인 0-based이고 응답의 `tracks`, `albums`, `artists`가 각각 `items`, `totalCount`, `hasNext`를 갖는다. 지정 `migration-server` worktree는 접근할 수 없었고 접근 가능한 서버의 `CatalogScreenSearchApi` → Controller → DTO를 정적으로 확인했다.

## 구현 및 수용 기준

1. Catalog 검색 호출을 전용 service에 두고 화면 page를 API page로 변환한다.
2. 검색어마다 screen API를 한 번 조회한다. URL에는 Track `page`, Artist `artistPage`, Album `albumPage`를 보존하고 검색어 변경 시 모두 첫 page로 돌아간다. 한 영역의 페이지 변경에도 세 page 값을 한 요청에 전달하되, 화면에서는 그 영역만 로딩·갱신한다.
3. 세 영역을 Artist → Track → Album 순서와 대략 1:3:2 시각 비중으로 배치한다. 타입 제목은 일반 본문 수준 크기로 둔다. 검색어의 첫 조회에서는 결과 영역을 숨기고 공통 로딩 UI 하나를 표시한다. Artist는 한 줄 목록과 좌우 화살표로 카드 묶음 및 서버 페이지를 이동한다. Track·Album은 현재 페이지와 이전/다음 버튼만 둔다. 다음 버튼은 `hasNext`로 제어하고 `totalCount`는 건수 표시용으로만 사용한다. 다음 page가 비면 이전 page와 항목을 유지하고 다음 버튼을 닫는다. 이전 요청은 취소하고 늦은 응답을 무시한다.
4. Track은 기존 상세, Artist는 `/artists/{spotifyArtistId}`, Album은 `/albums/{spotifyAlbumId}`로 이동한다.
5. 한국어·영어 번역과 기존 시각 스타일을 유지한다. 세 영역의 표시, 검색어 경쟁, 독립 페이지, 이동의 프론트 테스트를 추가한다.

## 검증 상태

- 변경 대상의 정적 diff와 `git diff --check`를 확인했다. 테스트·lint·build는 실행하지 않았다.
- 사용자 실행 필요: `cd tag-note-react && npm run test && npm run lint && npm run build`.
- 서버 코드의 `type` 지원을 정적으로 확인했다. 실행 서버 배포 상태와 실제 검색·페이지네이션은 브라우저에서 검증해야 완료 처리할 수 있다.

## Human Review 기록

- 2026-10-26 사용자가 목표 API 계약, 탭·상태·라우팅·다국어·테스트 범위와 백엔드 미지원 상태를 상세히 지정했다. 이 지시를 해당 범위의 Human Review로 기록한다.
- 이후 사용자가 탭 선택을 없애고 Artist 가로 목록, Track·Album 세로 목록을 한 화면에 배치하도록 범위를 변경했다. 세 영역을 동시에 보여주려면 각 타입을 조회해야 하므로 앞선 선택 타입 한 건 요청 규칙을 이 레이아웃에서는 세 타입 요청으로 바꿨다.
- 이어서 사용자가 타입 제목을 본문 텍스트 크기로 줄이고, 세 결과 영역이 준비되기 전에는 영역을 숨긴 채 로딩 UI 하나만 보이도록 요청했다.
- 2026-10-27 사용자가 새로 개발된 `/api/catalog/search`에 기본 검색을 연결하도록 요청했다. 현재 홈→검색 화면과 세 타입 조회가 이미 그 endpoint를 사용함을 확인하고, 사용되지 않는 `/api/tracks` 검색 메서드를 제거했다. 서버 `type` 계약과 문서·테스트 설명을 현재 상태로 갱신했다.
- 이후 사용자가 Artist의 숫자 페이지네이션 대신 좌우 화살표를 요청했고, 다시 한 줄 배치와 간결한 화살표 및 페이지 변경 시 해당 영역만 갱신하도록 지정했다. 이 지시를 현재 검색 화면의 디자인·로딩 동작 변경 승인으로 기록한다.
- 사용자가 Artist 페이지 로딩 문구를 카드의 순차적인 파도형 로딩 효과로 바꾸고 캐러셀 전환 애니메이션을 요청했다. 이어서 스크롤 이동에는 카드 전환 효과가 보이지 않는다고 지적하며, 화면에 보이는 카드 묶음을 교체하고 최초 검색의 공통 로더에도 파도 효과를 적용하도록 명시했다. 접근성 안내 문구는 화면에서만 숨기고 유지한다.
- 2026-10-28 사용자가 `screen` API 한 요청, 타입별 독립 0-based page, `hasNext` 기반 이전/다음, 빈 다음 page 복귀, 검색어 변경 시 요청 취소를 상세히 지시했다. 이 지시를 기존 타입별 요청 및 전체 숫자 페이지네이션을 대체하는 범위 변경 Human Review로 기록한다.
