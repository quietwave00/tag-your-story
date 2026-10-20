# TagNote React

Vite 기반 React 마이그레이션 앱입니다. 현재 단계는 기능 이식 전 기반 구조만 포함합니다.

## 구조

- `src/app`: 앱 조립, 라우터
- `src/components`: 공통 UI와 레이아웃
- `src/features`: 도메인별 기능 모듈 진입점
- `src/pages`: 라우트 단위 페이지
- `src/services`: 백엔드 API, OAuth, SSE 통신
- `src/store`: 클라이언트 상태 저장소
- `src/styles`: 전역 스타일과 디자인 토큰
- `src/utils`: 경로, 쿠키, 토큰 등 순수 유틸

## 환경변수

`.env.example`을 기준으로 `.env`를 생성합니다.

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_CLIENT_BASE_URL=http://localhost:5173
VITE_OAUTH_REDIRECT_PATH=/token
VITE_AUTH_HEADER_NAME=Authorization
VITE_SSE_AUTH_MODE=query
```
