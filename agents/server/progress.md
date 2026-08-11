# Progress

- 2026-08-11: `board`, `search`, `user`, `spotify`, `track` 현재 구현 baseline 분석 완료.
  - `agents/server/current_state.md`에 package 구조, 호출 흐름, Entity 관계, public API, spec 충돌, 보호 동작, characterization test 제안 정리.
  - production code 변경 없음.
- 2026-08-11: characterization test baseline 구현 시작.
  - 추가 테스트:
    - `tagnote-api/src/test/java/com/tagnote/api/domain/board/BoardControllerTest.java`
    - `tagnote-api/src/test/java/com/tagnote/api/domain/tracks/TrackControllerTest.java`
    - `tagnote-api/src/test/java/com/tagnote/api/domain/user/UserControllerTest.java`
    - `tagnote-api/src/test/java/com/tagnote/api/support/WebMvcMethodSecurityTestConfig.java`
    - `tagnote-core/src/test/java/com/tagnote/domain/board/service/BoardFacadeTest.java`
    - `tagnote-core/src/test/java/com/tagnote/domain/board/service/BoardServiceCharacterizationTest.java`
    - `tagnote-core/src/test/java/com/tagnote/domain/tracks/service/TrackServiceTest.java`
    - `tagnote-core/src/test/java/com/tagnote/domain/user/service/UserServiceTest.java`
    - `tagnote-core/src/test/java/com/tagnote/domain/tracks/util/SearchKeywordTrackerTest.java`
  - production code 변경 없음.
  - `./gradlew test` 실행을 시도했으나 현재 실행 환경에 `JAVA_HOME`과 `java`가 없어 검증이 차단됨.
