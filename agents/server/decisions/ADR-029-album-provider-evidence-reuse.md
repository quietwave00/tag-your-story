# ADR-029 — Album provider 근거의 저장과 재사용 경계

## Decision

Album 근거의 재사용 단위는 `(album_id, provider)`다. 각 branch의 원본 `ExternalTagInput`(이름, external reference, evidence type, confidence), 수집 결과 `SUCCESS`/`EMPTY`/`NOT_FOUND`, 수집 시각을 별도 영속 저장한다. `FAILED`/`TIMEOUT`은 재사용 가능한 완료 상태가 아니다. 원본 근거 저장과 해당 Album Observation/Assertion/Resolved 갱신이 모두 성공한 뒤에만 branch를 완료 상태로 전환한다. `SUCCESS`라도 유효한 영속 입력이 모두 필터에서 제외되면, 필터된 실제 입력과 상태를 함께 저장한다.

동시 최초 수집은 `(album_id, provider)`의 DB unique 제약과 만료 시간이 있는 소유권 lease로 조율한다. lease 획득·갱신은 짧은 DB transaction에서 조건부 update로 수행한다. 소유자는 transaction 밖에서 provider HTTP를 호출한다. lease가 만료되면 다른 인스턴스가 소유권을 획득할 수 있으며, 저장 시 소유권 token을 다시 검사해 늦게 끝난 이전 소유자의 결과를 버린다. 다른 요청은 제한된 deadline 안에서 완료 결과를 기다리고, 획득 실패 또는 deadline 만료를 `TIMEOUT`으로 처리한다. 같은 Album의 서로 다른 provider branch는 독립적으로 완료·재시도한다.

Track 선택은 매번 Track 직접 provider 근거를 조회한다. 첫 Track의 Album branch는 해당 Track metadata를 이용하는 기존 탐색 정책을 유지하며, Album 완료 branch가 있으면 그 저장 결과를 재사용한다. Album 직접 선택도 같은 branch를 공유한다. Album 근거의 변경은 Album projection과 그 Album을 참조하는 기존 Track의 상속 projection을 짧은 쓰기 transaction에서 재계산한다. `PROVIDER_ONLY` 진단은 이 저장·lease·projection 경로를 사용하지 않는다.

## Reason

`external_tag_observation`만으로는 provider가 빈 응답을 끝낸 경우와 아직 시도하지 않은 경우를 구별할 수 없다. 현재 `ProviderEnrichmentResult`도 Track/Album 근거를 한 결과로 합쳐 상태 하나만 반환하므로, 부분 실패한 Album branch의 재시도 여부를 판단할 수 없다. DB lease와 fencing token은 같은 Album을 여러 인스턴스가 동시에 선택했을 때 중복 HTTP와 늦은 결과의 덮어쓰기를 제한한다.

## Consequence

- branch 상태 테이블과 Oracle/H2 schema migration, provider의 Track/Album 수집 경계 분리, Album projection 갱신 후 기존 Track 재계산 경로가 필요하다.
- lease 소유자는 장애로 종료될 수 있으므로 만료 후 재시도 가능해야 한다. 외부 요청이 lease보다 오래 걸리면 늦은 결과는 저장되지 않는다.
- 기존 `EMPTY_COMPLETED` Track/Album의 일괄 재처리는 하지 않는다. 새 branch 상태는 실제 수집이 실행될 때부터 기록한다.
- 공개 API 계약, Last.fm Track 직접 호출, Resolver 점수 및 우선순위는 유지한다.
