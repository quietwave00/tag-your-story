# ADR-032 — 상세 메타데이터와 동기 태그 import의 독립 호출

## Decision

Track/Album 상세 화면은 기존처럼 메타데이터 조회와 태그 계산 호출을 독립적으로 시작한다. 메타데이터 GET은 Spotify 상세 정보를 반환하고, 태그 경로는 `POST /api/catalog/import`가 외부 근거 수집·계산을 마친 뒤 저장된 결과를 `GET /api/catalog/tracks/{spotifyTrackId}/tags` 또는 Album 대응 경로로 읽는다. 두 경로의 화면 로딩과 오류는 서로 독립적으로 처리한다. 태그 POST가 느려도 이미 응답한 메타데이터를 표시한다.

태그 import를 요청 밖의 영속 worker로 옮기지 않는다. `POST /api/catalog/import`의 동기 수집·계산 계약과 현재 `systemTags`/`previewTags`/`tagDisplayStatus` 응답을 유지한다. 공개 `enrichmentStatus`, `nextPollAfterSeconds`, 태그 GET polling, 신규 작업 큐를 도입하지 않는다. 화면의 태그 영역은 POST와 이어지는 GET을 기다리는 동안 클라이언트의 요청 로딩 상태로 표현한다. 현재 React의 상세 조회 effect와 `useSubjectTags`는 이미 독립적으로 시작하며, `catalogSubjectTagsService.load`는 POST 완료 뒤 GET을 호출한다.

제공자 실패와 재시도는 서버 내부에서 별도로 다룬다. ADR-029의 Album/provider 원본 근거 재사용·lease·부분 성공 정책을 유지한다. Track 직접 근거 및 기존 `ALREADY_RESOLVED`/`EMPTY_COMPLETED` 경로가 실패한 provider의 후속 재시도를 막는지는 특성화하고 필요한 최소 정책을 별도 구현 마일스톤에서 정한다. 일부 provider가 실패하면 성공한 근거는 보존하고, 클라이언트 태그 요청은 현재 계약대로 결과를 반환한다. MusicBrainz 503·요청 간격·12초 예산 문제는 UI 상태 API 없이 수집 정책과 예산을 조정해 다룬다.

이 결정은 아직 구현되지 않은 ADR-031의 Catalog 우선 POST 반환, 영속 비동기 worker, 공개 진행 상태 및 polling 결정을 대체한다. ADR-030의 승인된 장르 어휘, 가중치, Track 직접 장르 우선·Album 참고값 정책은 유지한다. 장르 표시를 위한 향후 API 계약 변경은 별도 Human Review에서 결정한다.

## Reason

사용자가 원하는 것은 상세 화면에서 Catalog 메타데이터를 먼저 보고 태그 결과가 오면 추가로 표시하는 경험이다. 현행 클라이언트는 메타데이터 요청과 태그 요청을 독립적으로 시작하므로, 동기 태그 import가 끝나기 전에도 메타데이터를 표시할 수 있다. 이를 위해 서버의 동기 POST를 비동기로 바꾸거나 새 작업 진행 상태를 계산할 필요가 없다.

동기 태그 POST에는 외부 API 대기와 제한 시간, 실패 시 후속 재시도 문제가 남는다. 이는 화면의 독립 로딩과 별개의 서버 수집 문제이므로 provider별 결과와 기존 저장 경계를 기준으로 해결한다.

## Consequence

- 이번 UI 흐름을 위해 신규 DB 작업 상태 테이블, worker, 진행 상태 DTO, Swagger 변경 또는 클라이언트 polling을 추가하지 않는다.
- 메타데이터 GET이 성공해도 태그 POST가 오래 걸리거나 실패할 수 있다. 화면은 메타데이터를 유지하고 태그 영역의 요청 로딩·오류를 별도로 표시한다.
- 기존 POST의 12초 전체 예산, MusicBrainz 503/timeout, provider별 재시도와 완료 marker의 조기 반환은 후속 수집 정책 설계·특성화 대상으로 남는다. 새 장르 정책의 데이터 재계산도 별도 마일스톤에서 다룬다.
- ADR-031은 설계 변경 이력으로 보존하고 구현 근거로 사용하지 않는다. 현행 Server Spec·Swagger·클라이언트 계약은 코드와 일치하는 동기 계약을 유지한다.
