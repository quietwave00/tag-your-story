# ADR-024 — Artist Album 유형별 인덱스

## Decision

- `releaseType` 없는 Artist Album 목록 요청은 ADR-022의 Spotify 원본 페이지·전체 건수 계약을 유지한다. `ALL`, `ALBUM`, `SINGLE_EP`, `COMPILATION`을 명시한 요청만 분류 후 0-based 페이지를 사용한다.
- 아티스트별 Spotify 원본 순서의 항목 한 벌과 유형별 원본 위치를 하나의 불변 Redis 스냅샷으로 저장한다. 키에는 분류 정책·포맷 버전을 포함한다. 완전한 스냅샷 TTL은 15분, 분류가 불완전하면 1분이다.
- 같은 서버 프로세스의 동시 cache miss는 한 빌드로 합치며, 경계가 있는 실행기에서 전체 Spotify 목록을 수집한 뒤 Redis 한 키에 게시한다. 구축 중에는 HTTP 202, `Retry-After: 1`, `response.status=BUILDING`을 반환한다.
- 목록 수집 실패는 스냅샷을 게시하지 않고 기존 `SPOTIFY_EXCEPTION` 계약을 따른다. 개별 앨범의 분류 실패는 `releaseType=null`로 보존한다. `ALL`에는 포함하고 유형별 목록에서는 제외하며, `classificationComplete=false`와 `unclassifiedCount`를 반환한다.

## Reason

Spotify 원본 페이지의 10건은 유형별 10건과 다르다. 원본 순서를 유지한 단일 스냅샷에서 유형별 위치를 잘라야 정확한 페이지와 건수를 제공하고 탭 전환 시 Spotify 전체 목록을 반복 수집하지 않는다.

## Consequence

- 명시적 유형 요청의 `totalCount`는 선택 유형에서 분류가 확정된 항목 수다. 불완전한 경우 전체 유형 수로 해석하지 않는다.
- Redis TTL 후 첫 요청은 다시 BUILDING이 될 수 있다. 프로세스 간 빌드 합치기는 이 결정의 범위가 아니다.
- DB 읽기·쓰기 및 DB 트랜잭션은 추가하지 않는다. ADR-021/022의 분류 규칙은 유지한다.
