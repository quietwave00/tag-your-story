# ADR-031 — Catalog 우선 반환과 영속 외부 보강 작업

> 2026-11-09 ADR-032에 의해 대체됨. 아래 설계는 구현되지 않았으며 현재 목표 계약이 아니다.

## Decision

`POST /api/catalog/import`는 Spotify metadata로 Catalog Track/Album 및 필요한 관계를 저장하거나 재사용하고, 해당 subject의 외부 보강 작업을 영속 등록한 뒤 Catalog 결과를 반환한다. MusicBrainz·Discogs·Last.fm HTTP와 태그 계산 완료를 이 요청의 반환 조건으로 두지 않는다. 같은 Spotify ID의 반복 요청은 Catalog와 진행 중인 작업을 재사용하며 중복 작업을 만들지 않는다. 작업 등록은 Catalog 저장과 정합성이 보장되는 짧은 DB transaction에서 수행한다. 요청 종료 후 작업 실행은 영속 상태를 스캔·획득하는 worker가 맡으며, 프로세스 메모리에만 남는 실행 예약을 완료 보장 수단으로 사용하지 않는다.

작업 단위는 provider와 근거 범위(`TRACK_DIRECT`, `ALBUM_DIRECT`)를 구분한다. Album/provider 근거는 같은 Album의 Track과 Album 선택에서 공유한다. 등록 시 활성 provider 집합을 기록해 진행 상태가 설정 변경 때문에 끝나지 않는 일을 막는다. 기존 ADR-029의 lease, fencing token, 원본 근거 재사용 및 provider별 부분 성공 원칙은 유지하고, 실행 대기·재시도까지 포함하는 단일 영속 상태 모델로 정리한다. 외부 HTTP 동안 DB transaction을 열지 않는다. 작업 소유자가 종료되면 lease 만료 후 재획득한다. 성공한 provider 결과는 다른 provider의 실패와 독립적으로 Observation·Assertion·projection에 반영한다. 완료된 작업은 같은 POST로 다시 시작하지 않으며, 실패한 작업의 재개는 제한된 재처리 정책으로 수행한다.

작업 상태는 내부적으로 `PENDING`, `RUNNING`, `SUCCESS`, `EMPTY`, `NOT_FOUND`, `RETRY_WAIT`, `FAILED`를 구분한다. `SUCCESS`/`EMPTY`/`NOT_FOUND`는 해당 provider branch의 정상 종료이고, 일시적 HTTP 오류·503·timeout은 제한된 backoff 후 `RETRY_WAIT`다. 재시도 한도나 영구적 오류를 넘기면 `FAILED`로 남기며 운영 재처리 경로를 둔다. 한 branch의 정상 빈 응답이나 실패로 subject 전체를 완료 처리하지 않는다. 새 태그 어휘·계산 정책은 저장된 원본 Observation을 다시 해석할 수 있어야 하며 provider 재조회 상태와 별도 버전으로 관리한다.

태그 GET은 기존처럼 DB 읽기 전용이다. `GET /api/catalog/tracks/{spotifyTrackId}/tags`와 Album 대응 경로는 현재 저장된 태그 내용과 별도의 `enrichmentStatus`를 제공한다. 집계 상태는 `PENDING`(등록 후 미실행), `RUNNING`(실행 중), `PARTIAL`(하나 이상의 branch가 정상 종료했지만 다른 branch가 미종결 또는 실패), `COMPLETE`(활성 branch가 모두 `SUCCESS`/`EMPTY`/`NOT_FOUND`로 종료), `RETRY_WAIT`(정상 종료 branch 없이 재시도 대기), `FAILED`(정상 종료 branch 없이 재시도 종료), `UNAVAILABLE`(활성 provider가 없음)로 표현한다. `tagDisplayStatus`의 `CONFIRMED`/`PREVIEW`/`EMPTY`는 현재 저장된 내용의 상태로 유지한다. `EMPTY`와 보강 완료를 동일시하지 않는다. GET은 외부 호출이나 작업 등록·재시도를 실행하지 않는다.

POST 응답은 기존 Catalog 필드를 유지하고 보강 진행 상태를 추가하는 방향으로 설계한다. 기존 HTTP 200 성공 경로를 유지하며 `202`를 작업 ID 응답으로 새로 도입하지 않는다. 진행 중인 결과의 재조회 간격은 응답의 nullable `nextPollAfterSeconds`로 제공한다. `COMPLETE`/최종 `FAILED`와 추가 시도가 예약되지 않은 최종 `PARTIAL`에서는 이 값을 비워 클라이언트가 반복 조회를 멈출 수 있게 한다. 구체적인 DTO·Swagger 및 클라이언트 계약은 구현 마일스톤에서 실제 코드와 함께 변경한다.

첫 요청의 12초 전체 보강 예산을 요청 성공 조건으로 사용하지 않는다. worker는 각 외부 호출에 유한 timeout과 작업별 재시도 한도를 둔다. MusicBrainz 요청 간격은 같은 외부 IP를 공유하는 모든 worker에서 지키고, HTTP 503에 대해 즉시 연쇄 재시도하지 않으며 `Retry-After`가 있으면 존중한다. User-Agent가 설정되고 실제 전송되는지 검증한다. 구체적인 간격·backoff·한도는 표본 및 운영 환경에서 교정한다.

이 결정은 ADR-029의 “다른 요청이 Album branch 완료를 제한된 deadline 안에서 기다린다”, “Track 선택 때마다 Track 직접 provider를 다시 조회한다”는 요청 동기 실행 부분을 대체한다. ADR-029의 Album 근거 공유와 소유권·정합성 원칙은 유지한다. ADR-030의 장르 어휘·Track 직접 장르 우선 표시는 계산 정책으로 적용한다. 기존 공개 API의 동기 태그 계산 설명과 `EMPTY_COMPLETED` 조기 반환 정책은 전환 시점에 새 계약으로 교체한다.

## Reason

현재 Track/Album 선택 서비스는 Catalog 저장과 세 provider 조회·태그 계산을 한 요청에서 조율한다. 12초 예산 안에 MusicBrainz의 순차 요청과 요청 간격 대기를 모두 끝내지 못하면 일부 조회가 건너뛰어진다. HTTP 503 및 다른 provider의 부분 실패도 첫 화면의 지연과 계산 결과에 직접 반영된다. 이미 resolved 결과나 빈 결과 완료 표식이 있으면 다음 import에서 재시도하지 않는 경로도 있다.

Catalog identity는 Spotify metadata로 먼저 확정할 수 있다. 외부 보강을 영속 작업으로 분리하면 첫 응답의 시간 경계를 Catalog 저장에 맞추면서, 부분 성공한 근거를 즉시 보존하고 일시적 실패를 다음 시도에서 회복할 수 있다. 진행 상태와 태그 내용 상태를 따로 제공해야 사용자가 “아직 계산 중”과 “정상적으로 장르가 없음”을 구별할 수 있다.

## Consequence

- provider별·범위별 영속 작업 상태, lease/재시도 시각과 정책 버전, 작업 획득 worker, 트랜잭션 경계, 중복 요청·프로세스 재시작·부분 실패 테스트가 필요하다. 기존 `album_provider_evidence`와 중복된 상태 저장을 만들지 않도록 스키마 전환을 설계한다.
- POST import의 응답 시점과 태그 GET의 필드가 바뀌므로 API 명세 interface, DTO, Server Spec, 클라이언트 계약 및 화면의 진행 상태 처리가 함께 필요하다. 기존 메타데이터와 error envelope의 호환 범위를 명시한다.
- 기존 `subject_tag_resolved`와 완료 marker는 새 작업 상태의 근거가 아니다. 전환 중에는 구 projection과 새 정책 결과를 구분하고, 기존 Observation의 재계산·완료 marker 무효화·되돌리기 경로를 마이그레이션 Plan에 기록한다.
- 외부 보강의 완료 시간은 첫 POST 응답 시간과 분리된다. 클라이언트는 조회 간격을 지키고 진행 상태가 종료될 때까지 현재 저장된 태그를 갱신해 표시한다. 기존 태그가 있으면 재보강 중에도 이를 표시할지 정책 버전별로 결정해야 한다.
- 이 ADR은 목표 아키텍처 결정이며 아직 구현되지 않았다. 현재 Swagger와 공개 DTO는 기존 동기 계약을 표현한다.
