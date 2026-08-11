# #tagnote 디자인 스펙

## 1. 디자인 역할

감각적인 독립 디자인 스튜디오의 디자인 리드로서, 헤비 리스너와 음악 디거를 위한 음악 검색/태그 아카이브 웹 서비스를 설계한다.
목표는 오래된 커뮤니티 게시판이나 개발자 포트폴리오가 아니라, 실제 상용 브랜드 서비스로 보일 만큼 정제된 흑백 기반 인터페이스를 만드는 것이다. 차갑고 현대적인 인상은 유지하되, 과도한 선, 점, 터미널식 문구, 오래된 HTML 게시판 감성은 배제한다.

## 2. 사이트 목적과 사용자

브랜드 이름: `#tagnote`

주요 역할:
- 음악의 기본 정보와 장르 데이터를 구조화하여 조회한다.
- 사용자가 직접 커스텀 태그를 부여해 음악을 아카이빙한다.
- 검색한 트랙, 앨범, 아티스트를 태그 노트와 연결한다.
- 헤비 리스너, 음악 힙스터, 서브컬처 디거가 음악을 깊게 탐색하도록 돕는다.

## 3. 브랜드 태도

전달해야 하는 인상:
- 현대적
- 유려함
- 차가움
- 쿨함
- 상용 브랜드 서비스 수준의 완성도
- 음악 아카이브 스튜디오 같은 집중감

금지 인상:
- 귀여움
- 과한 친근함
- 유머 중심 표현
- 오래된 HTML 게시판
- 터미널/해커 콘솔 감성
- 과도한 dot/noise 패턴
- 과도한 solid line 기반 브루탈리즘

## 4. 핵심 디자인 콘셉트

핵심 키워드:
- modern monochrome
- paper texture
- soft dust
- blurred motion
- shoegaze haze
- editorial archive
- commercial-grade minimalism

디자인은 흑백 신문과 음악 아카이브의 차가운 정밀함을 출발점으로 삼되, 최종 표현은 더 유려하고 상용 서비스답게 정리한다. 점이나 선으로 화면을 채우지 않는다. 질감은 매우 흐린 종이 표면, 뿌연 먼지, 서서히 퍼지는 blur로 표현한다.

## 5. 색상 시스템

주요 색상은 제한된 무채색만 사용한다.

Color Tokens:
- Primary Dark: `#0A0A0A`
- Primary Off-White: `#F2F2F2`
- Paper Gray: `#E9E9E9`
- Muted Text: `#737373`
- Soft Line: `#D1D1D1`
- Hover Invert: `#0A0A0A` background + `#F2F2F2` text

규칙:
- 다채로운 컬러 팔레트는 사용하지 않는다.
- 파스텔, 보라/파랑 그라데이션, SaaS식 컬러 포인트는 사용하지 않는다.
- disabled 상태는 취소선이 아니라 light gray 배경/텍스트로 구분한다.
- 상태 구분은 색, 굵기, blur, opacity를 우선하고 선은 최소화한다.

## 6. 타이포그래피

상용 브랜드 서비스처럼 보이는 네오 그로테스크 계열을 사용한다.

Font Family:
- Display: `Inter Tight`, `Avenir Next`, `Helvetica Neue`, `Helvetica`, `Arial`, `Pretendard`, sans-serif
- Body: `Inter`, `Pretendard`, `Helvetica Neue`, `Helvetica`, `Arial`, sans-serif
- Label/Metadata: 기본적으로 Body와 같은 현대적 산세리프를 사용한다. Monospace는 정말 필요한 숫자/ID/개발자성 메타데이터에만 제한적으로 사용한다.

Typography Rules:
- `Arial Black`처럼 오래된 웹 포스터 느낌이 강한 폰트는 사용하지 않는다.
- 전역 `letter-spacing`은 `0`을 기본값으로 한다. 음수 자간은 사용하지 않는다.
- 큰 타이틀도 과도하게 거대하게 만들지 않는다. 일반 페이지 제목은 대략 `34px ~ 86px` 범위 안에서 제어한다.
- 폰트 굵기는 `760` 이하를 기본 상한으로 보고, 무조건 `900`에 의존하지 않는다.
- 텍스트와 아이템 간 간격은 타이트하게 유지하되, 답답하지 않도록 충분한 섹션 여백을 둔다.

## 7. 레이아웃

기본 레이아웃:
- 모든 주요 콘텐츠는 중앙 정렬한다.
- 페이지 폭은 `1120px` 내외로 제한한다.
- 검색, 리스트, 상세, Contact 모두 중앙에 안정적으로 배치한다.
- 섹션 간 여백은 충분히 둔다.
- 아이템 내부 간격은 조밀하게 유지한다.

금지:
- 전역 12-column grid 배경
- 과도한 1px solid line 반복
- 데이터베이스 테이블처럼 보이는 촘촘한 경계선
- 오래된 게시판식 리스트
- 카드 남발

Line Rules:
- 선은 입력 하단선, 버튼/textarea/알림 박스처럼 기능적 경계가 필요한 곳에만 사용한다.
- 카테고리 active 상태에 underline을 사용하지 않는다. 굵기와 색으로만 구분한다.

## 8. 질감과 이미지

자산:
- `tag-note-react/assets/img_1.png`: 구겨진 종이 질감. 홈 히어로와 전역 질감의 주 자산으로 사용한다.
- `tag-note-react/assets/img.png`: 명시적 dot/grain 인상이 강하므로 기본 디자인에서는 사용하지 않는다.

Texture Rules:
- dot 패턴처럼 보이는 noise는 사용하지 않는다.
- dust는 작은 점이 아니라 흐린 종이 안개처럼 보여야 한다.
- 전역 질감은 `img_1.png`를 blur, low opacity, slow drift로 처리한다.
- 질감은 콘텐츠를 방해하지 않아야 한다.

## 9. 메인 페이지

Header:
- 좌측 상단 브랜드는 `#tagnote`.
- 네비게이션은 `LOG / DIG / CONTACT / MY TAGS` 같은 짧은 라벨을 사용한다.
- active underline은 금지한다.

Hero:
- 홈 내부에 별도의 `#tagnote` 텍스트 타이틀을 반복하지 않는다.
- `home-visual-type`처럼 히어로 이미지 위에 `dig/tag/note` 텍스트를 올리는 레이어는 사용하지 않는다.
- 히어로에는 `img_1.png` 기반의 구겨진 종이 질감을 크게 배치한다.
- 검색창은 종이 질감 위에 살짝 떠 있는 translucent blur panel처럼 배치한다.

Search:
- input focus는 solid outline이 아니라 font-weight/bold와 미세한 blur 변화로 표현한다.
- search 버튼 disabled는 light gray, enabled는 dark gray로 구분한다.
- input underline과 버튼 사이에는 충분한 간격을 둔다.

Keyword Cloud:
- 다채로운 pill UI를 쓰지 않는다.
- 흑백 텍스트 중심으로 구성하되, 너무 빽빽한 Wall of Text가 되지 않게 한다.
- hover 시 반전과 blur를 사용한다.

Recent Tags:
- 오래된 게시판처럼 보이지 않도록 선과 번호의 존재감을 최소화한다.
- 중앙 폭 안에서 조밀하지만 정제된 리스트로 구성한다.

## 10. 검색 결과 화면

표현:
- `DATABASE INDEX / 0000 ROWS` 같은 프롬프트성 문장은 금지한다.
- 자연어 UI 문장을 사용한다.
- 중앙 정렬을 유지한다.
- 앨범 커버는 작고 정돈된 1:1 이미지로 사용한다.
- 리스트 간격은 타이트하게 유지하되, 테이블 경계선은 사용하지 않는다.

Hover:
- 커버 이미지는 흑백 대비가 살짝 강해진다.
- 텍스트는 미세 blur/반전으로 반응한다.

## 11. 트랙 상세 화면

구성:
- 앨범 커버와 트랙 정보를 중앙 폭 안에서 균형 있게 배치한다.
- 제목은 크지만 과도하게 화면을 압도하지 않는다.
- Artist, Album, Track ID 같은 메타데이터는 산세리프 라벨로 정돈한다.
- 커스텀 태그 노트 리스트는 카드나 테이블이 아닌 가벼운 텍스트 그룹으로 보이게 한다.

## 12. 글 작성/상세/댓글

글 작성:
- textarea 정도에만 기능적 경계선을 허용한다.
- 태그 chip은 `2px` radius까지 허용한다.
- 파일 프리뷰는 이미지 자체가 중심이 되게 하고 선을 남발하지 않는다.

글 상세:
- 콘텐츠를 중앙에 크게 배치하되 지나치게 포스터처럼 만들지 않는다.
- 좋아요/수정/삭제는 조용한 텍스트 버튼으로 유지한다.

댓글:
- 오래된 게시판형 row line을 쓰지 않는다.
- 작성자, 본문, 액션 간 간격은 조밀하지만 깨끗하게 둔다.

## 13. Contact

Contact 페이지는 반드시 제공한다.

요소:
- GITHUB
- SPOTIFY
- INSTAGRAM
- EMAIL

문의 폼은 만들지 않는다. 링크 중심으로 구성한다. 아이콘은 16x16 단선 형태 또는 간단한 사각 심볼만 허용한다.

## 14. 인터랙션

기본 원칙:
- UI는 너무 딱딱하지 않아야 한다.
- hover와 page enter에서 blur가 서서히 풀리거나 퍼지는 움직임을 사용한다.
- 움직임은 유려하지만 장식적 parallax처럼 과하지 않아야 한다.

Motion:
- page enter: `opacity 0 -> 1`, `blur(18px) -> blur(0)` 수준의 bloom.
- hero paper: 아주 느린 scale/translate drift.
- dust texture: 매우 낮은 opacity로 느리게 움직인다.
- hover: blur, inversion, slight translate를 조합한다.

Timing:
- `linear` 또는 절제된 ease를 사용한다.
- 700ms ~ 1400ms의 서서히 드러나는 모션을 허용한다.
- 클릭 피드백은 과장하지 않는다.

## 15. 금지사항 요약

- 이전 브랜드명 사용 금지. 브랜드는 `#tagnote`.
- `home-visual-type` 텍스트 레이어 사용 금지.
- dot/noise 패턴을 주 시각 요소로 사용 금지.
- `img.png`를 기본 dust로 사용 금지.
- 과도한 solid line grid 금지.
- active nav underline 금지.
- disabled 취소선 금지.
- 프롬프트성 문구 금지: `DATABASE INDEX`, `0000 ROWS`, `SCRAPED METADATA`, `STREAM`, `GRID`, `NOISE 03` 등.
- 오래된 HTML 게시판처럼 보이는 테이블/경계선/monospace 남발 금지.
- 과도한 `font-weight: 900` 의존 금지.
