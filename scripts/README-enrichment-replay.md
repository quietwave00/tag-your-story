# 태그 계산 import 전후 비교

`replay-enrichment-diagnostics.py`는 원본 JSONL의 `IMPORT_PIPELINE` 실행을 `(subjectType, Spotify ID)`로 중복 제거한다. 기본 기준선은 2026-11-05 이전 실행이다. 같은 subject에서 완료된 계산이 있으면 `import_skipped` 실행보다 우선한다.

현재 원본 전체에는 고유 subject 68개가 있지만, 2026-11-05 이전 기준선이 있는 대상은 63개다. 나머지 5개는 11월 6일 이후에만 기록되어 기본 비교에서 제외한다. 전체 대상을 보려면 `--before-cutoff`에 더 늦은 날짜를 지정할 수 있으나, 그 결과를 개선 전 기준선으로 해석해서는 안 된다.
63개 중 계산 완료 로그가 있는 대상은 60개이고, 나머지 3개는 기존 결과 재사용 기록만 있어 최종 태그 차이를 확정할 수 없다.

각 subject에 대해 원래 title, Track의 album title, artist 이름을 Catalog 검색 API에 각각 보낸다. 검색 결과의 Spotify ID 일치 여부를 기록하지만, import는 원본 로그의 Spotify ID를 사용한다. `ARTIST`는 검색만 하고 import하지 않는다. 검색의 첫 3개 page에 대상이 없더라도 import를 계속한다. 검색 순위와 Spotify 응답은 시간에 따라 달라질 수 있다.

## 준비

1. API가 접근 가능한 주소에서 실행 중인지 확인한다. 이 작업 환경의 `127.0.0.1:8080`은 현재 연결되지 않는다.
2. `application-tag.yml`에 진단 활성화와 `D:/p/tag-your-story/logs/enrichment-diagnostics-tobe.jsonl` 출력 경로를 직접 설정했다. IDE에서 API를 재시작하면 적용되며 환경변수는 필요 없다. 다른 PC·경로에서 실행할 때는 이 경로를 먼저 바꿔야 한다.
3. 실제 태그 계산을 비교하려면 대상이 아직 계산되지 않은 DB가 필요하다. 이미 계산된 DB에서는 정상적인 import 재호출이 `ALREADY_RESOLVED` 또는 `EMPTY_COMPLETED`로 끝나고, 이 결과는 `REUSED_EXISTING`으로 표시된다. `PROVIDER_ONLY` 진단은 제공자 비교에는 쓸 수 있지만 최종 System Tag 비교에는 쓸 수 없다.

표본 목록 확인은 API 호출 없이 가능하다.

```powershell
python scripts/replay-enrichment-diagnostics.py
```

처음에는 한 건으로 환경을 확인한다.

```powershell
python scripts/replay-enrichment-diagnostics.py --execute --limit 1 --base-url http://127.0.0.1:8080
```

`logs/enrichment-replay-results.jsonl`의 첫 행에서 `after.runId`가 있고 `comparison.status`가 `COMPARED`인지 확인한다. `REUSED_EXISTING`이면 DB에 이미 계산 결과가 있으므로 동일 DB에서 재실행해도 계산 전후 비교가 되지 않는다. `DIAGNOSTIC_MISSING`이면 API 프로세스의 진단 파일 경로를 확인한다.

나머지 표본을 이어서 실행한다. 완료된 subject는 다시 호출하지 않는다.

```powershell
python scripts/replay-enrichment-diagnostics.py --execute --base-url http://127.0.0.1:8080
```

API 주소가 다르면 `--base-url`을 바꾼다. 기본 파일은 다음과 같다.

| 파일 | 내용 |
| --- | --- |
| `logs/enrichment-diagnostics.jsonl` | 개선 전 원본, 읽기 전용 |
| `logs/enrichment-replay-manifest.jsonl` | 기준선 runId와 검색 입력이 담긴 63개 표본 |
| `logs/enrichment-diagnostics-tobe.jsonl` | API 프로세스가 기록하는 개선 후 사건 |
| `logs/enrichment-replay-results.jsonl` | 검색·import 응답·전후 실행 요약과 재개 상태 |
| `logs/enrichment-comparison.jsonl` | subject별 제공자 상태·원본 태그·resolved 태그 차이 |
| `logs/enrichment-comparison.md` | 비교 상태와 태그 수 표 |

`resolvedAdded`/`resolvedRemoved`는 태그 이름 기준이다. provider 원본 태그·상태 변화와 bootstrap 생성 수도 함께 기록하지만, 외부 API 응답 변동이 있으므로 이를 코드 변경의 효과로 단정하지 않는다. 재실행 중 요청이 끊기면 결과 파일에 오류가 남고 다음 실행에서 해당 subject를 재시도한다. 중단 시점에 POST가 이미 처리됐을 수 있으므로 이 경우 `REUSED_EXISTING`이 나올 수 있다.

진단 로그의 bounded 배열이 잘린 표본은 `COMPARED_PARTIAL`이다. 이 표본의 `resolvedAdded`/`resolvedRemoved`는 기록된 항목만 비교한 값이다. 원본 로그에는 resolved 배열이 잘린 실행이 2건 있다.

테스트와 프로젝트 검증은 사용자가 실행한다.

```powershell
python -m unittest discover -s scripts -p test_replay_enrichment_diagnostics.py
./gradlew test
./gradlew check
./scripts/verify.sh
```
