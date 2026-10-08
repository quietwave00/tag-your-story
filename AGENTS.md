# TagNote 엔지니어링 가이드

## Project
'#tagnote'는 헤비 리스너를 위한 음악 정보/커뮤니티 서비스이다.
서버는 계층형 아키텍처(Layered Architecture)를 사용한다.

주요 계층:
Presentation
Application
Domain
Infrastructure

## Source of Truth
아키텍처 변경을 수행하기 전에 관련 문서를 읽어라.

Server 명세:
- agents/server/server_spec.md

System Tag 아키텍처:
- agents/server/server_tag_feature_architecture.md

현재 레거시 구현 상태:
- agents/server/current_state.md

엔지니어링 컨벤션:
-agents/server/conventions.md

아키텍처 결정 사항:
-agents/server/decisions/
(파일 포맷은 현재 생성된 md 파일처럼 ADR-nnn-subject.md 형식으로 생성 nnn은 incrementing number, subject는 구현 내용 요약 입력. 최초 시작 시 ADR-000-subject.md는 삭제하고 시작.)
(## Decision ## Reason ## Consequence 형식)

현재 구현 진행 상황:
-agents/server/progress.md

명시적으로 문서화된 아키텍처 결정 사항을 충돌 보고 없이 임의로 재해석하지 마라.

## Plan 관리

구현 및 리팩토링 작업은 `agents/server/plans/`에서 관리한다.

```text
agents/server/plans/
├─ active/
└─ completed/
```

* 모든 향후 Plan을 미리 작성하지 않는다.
* `progress.md`에는 전체 Roadmap과 현재 진행 상태만 관리한다.
* 실제 착수할 마일스톤만 Plan Mode에서 상세 계획을 작성한다.
* Human Review로 승인된 Plan만 `plans/active/`에 저장하고 구현을 시작한다.
* 하나의 Plan은 가능한 한 하나의 작은 마일스톤만 다룬다.
* 구현 완료 후 Acceptance Criteria, 테스트, 검증, 리뷰가 모두 통과하면 해당 Plan을 `plans/completed/`로 이동한다.
* 완료 시 `progress.md`도 함께 업데이트한다.
* 구현 중 Plan 변경이 필요하면 이유를 먼저 보고하고, Scope 또는 설계 결정에 영향이 있으면 임의로 변경하지 않는다.

역할 구분:

```text
progress.md
= 전체 Roadmap / 현재 진행 상태

plans/active/
= 현재 승인되어 구현 중인 상세 Plan

plans/completed/
= 완료된 마일스톤의 실행 기록
```

## ADR 사용 규칙

서버 Plan 작성, 구현, 리팩토링, 리뷰를 시작할 때:

1. `agents/server/decisions/ADR-*.md` 목록을 확인한다.
2. 변경 대상 도메인과 관련된 ADR을 구현 전에 전부 읽는다.
3. ADR과 명세 또는 active plan이 충돌하면 임의로 선택하지 말고 보고한다.
4. ADR을 뒤집거나 범위를 변경해야 하면 새로운 ADR을 작성한다.
5. ADR은 현재 진행 상태가 아니므로 구현 여부는 `progress.md`와 실제 코드를 함께 확인한다.

## API 명세 / Swagger

프론트엔드에 노출되는 API는 Swagger(OpenAPI) 명세를 유지한다.

* Swagger 명세는 Controller 로직과 분리하기 위해 API 명세용 `interface`에 작성하고 Controller가 이를 구현하도록 한다.
* Controller에는 Swagger 문서화를 위한 어노테이션을 가능한 작성하지 않는다.
* 기존 Controller의 동작 및 Request / Response 구조를 Swagger 적용을 이유로 변경하지 않는다.
* API 추가/변경 시 Swagger 명세도 함께 추가/수정한다.
* 각 API 명세에는 Endpoint, HTTP Method, Request, Response, Path/Query Parameter, 인증 여부를 명확하게 작성한다.
* 성공 응답뿐 아니라 실제 발생 가능한 주요 Error Response와 기존 `error_code`, `custom_error_code` 체계를 문서화한다.
* Request / Response DTO의 필드 의미, 필수 여부 및 주요 제약조건이 프론트엔드에서 이해 가능하도록 작성한다.
* Swagger 명세는 실제 Controller / DTO 구현을 기준으로 하며 구현과 불일치해서는 안 된다.
* API Contract 변경이 없는 리팩토링에서는 기존 Swagger Contract도 유지한다.


## 작업 규칙
사소하지 않은 변경 사항의 경우:
1. 기존 구현을 먼저 점검
2. 요청을 충족할 수 있는 가장 작은 마일스톤을 식별
관련 문서와 실제 코드를 조사한 뒤 Plan Mode에서 계획 수립 및 Human Review
4. 명세가 명시적으로 변경하지 않는 한 기존 동작 유지
5. 테스트를 추가하거나 업데이트
6. 사용자에게 검증 명령을 안내하고 사용자가 전달한 실행 결과 확인
7. diff 검토
8. 마일스톤 완료 시 progress.md 업데이트 및 Plan을 completed로 이동

관련 없는 리팩토링 수행 금지한다.
기능 구현 시 이를 가능하게 하는 최소한의 리팩토링은 포함될 수 있으나, 관련 없는 리팩토링 작업은 분리되어야 한다.

## 기존 코드
Board, Search 및 일부 기능이 이미 존재한다.
이러한 기능들을 처음부터 다시 작성하지 마라.
동작을 유지하는 리팩토링 전에는 특성화 테스트를 사용하라.
빅뱅 방식의 전면 교체보다는 점진적 마이그레이션을 선호하라.

## 아키텍처 규칙
- Controller는 비즈니스 로직을 포함해서는 안 된다.
- Controller는 Repository에 직접 접근해서는 안 된다.
- Application Service는 유스케이스를 조율한다.
- Domain은 비즈니스 규칙을 포함한다.
- Infrastructure는 영속성 및 외부 API 구현을 포함한다.
- 불필요한 양방향 JPA 관계를 피한다.
- LAZY(지연 로딩) 관계를 선호하라.
- 불필요한 SELECT 쿼리 및 N+1 쿼리를 피하라.

## 외부 APIs
Spotify는 탐색/검색 소스이다.
MusicBrains와 Discogs는 데이터 보강 제공자이다.
외부 HTTP 요청을 기다리는 동안 DB 트랜잭션을 열어두지 마라.
부분적인 해결이 가능한 경우, 외부 제공자의 실패로 인해 성공한 다른 제공자의 결과를 폐기해서는 안 된다.

## Tag 규칙
System Tag와 User Tag는 분리된 개념이다.

자세한 태그 규칙:
agents/server/server_tag_feature_architecture.md

resolver 정책을 관련 없는 코드에 중복하여 작성하지 마라.

## 검증

컴파일, 빌드, 테스트 및 자동 검증 명령은 사용자가 직접 수행한다.

- Codex는 필요한 테스트 코드를 작성하거나 갱신하고, 사용자가 실행할 명령만 안내한다.
- Codex는 사용자가 현재 요청에서 명시적으로 실행을 지시하지 않는 한 `./gradlew`로 시작하는 명령을 직접 실행하지 않는다.
  금지 대상에는 `compileJava`, `compileTestJava`, `classes`, `assemble`, `build`, `test`, `check` 및 개별 테스트 태스크가 모두 포함된다.
- Codex는 컴파일 확인, 정적 검증 또는 테스트 준비라는 이유로도 위 Gradle 명령을 실행하지 않는다.
- Codex는 `./scripts/verify.sh`를 직접 실행하지 않는다. 필요한 경우 사용자 실행 명령으로만 안내한다.
- Codex가 직접 수행할 수 있는 검증은 코드 읽기, diff 검토, `git diff --check`처럼 빌드나 테스트를 수행하지 않는 정적 검사로 제한한다.
- 실행하지 않은 검증은 완료한 것처럼 표현하지 않고, 최종 보고에 미실행 상태와 사용자 실행 명령을 명시한다.
- 사용자가 전달한 실행 결과를 기준으로만 검증 상태와 Plan 완료 여부를 갱신한다.

사용자가 작업 완료를 검증할 명령:

```bash
./gradlew test
./gradlew check
./scripts/verify.sh
```

## 리뷰 규칙

다음 사항에 대해 리뷰:

- 명세 불일치
- 기존 동작의 퇴보
- 트랜잭션 경계 실수
- JPA N+1 또는 불필요한 쿼리 발생
- 누락된 unique/FK 제약 조건
- 멱등성 문제
- 동시성 문제
- 중복된 도메인 규칙
- 아키텍처 계층 위반

## Scope 제어
현재의 마일스톤만 구현하라.
현재 마일스톤을 컴파일하거나 수용 기준을 통과시키는 데에 필요한 경우가 아니라면, 이후 단계를 먼저 구현하지 마라. 
