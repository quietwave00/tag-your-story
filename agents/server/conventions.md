# Server Convention

* Lombok 사용 가능 시 `@Getter`, `@RequiredArgsConstructor,... 등`을 우선 사용한다.
* 기존 Request / Response DTO 구조와 응답 포맷을 유지한다.
* 기존 `error_code`, `custom_error_code` 체계와 예외 처리 방식을 준수한다.
* 생성자 주입을 사용하며 필드 주입(`@Autowired`)은 사용하지 않는다.
* JPA 연관관계는 기본적으로 `LAZY`를 사용하고, 필요하지 않은 양방향 연관관계를 만들지 않는다.
* Entity를 API Response로 직접 반환하지 않고 기존 DTO 변환 방식을 따른다.
* Controller에는 비즈니스 로직을 작성하지 않는다.
* 트랜잭션은 Application Service의 필요한 쓰기 범위에만 적용하며 외부 API 호출을 트랜잭션 내부에서 수행하지 않는다.
* 기존 Naming Convention과 패키지 구조를 우선하며, 명확한 이유 없이 기존 클래스/메서드/DTO 이름을 변경하지 않는다.
* 기존 API endpoint와 Request / Response contract는 명세에서 변경을 요구하지 않는 한 유지한다.
* 프론트엔드에 노출되는 API를 추가·수정·삭제하면 같은 작업에서 Swagger `*Api.java`와 `tag-note-client/agents/client/api-contract.md`의 해당 계약·근거 날짜를 함께 갱신한다. 실행 검증이 없는 내용은 현재 코드의 정적 계약으로 표시한다.
* 중복 생성 방지가 필요한 데이터는 애플리케이션 검증뿐 아니라 DB `UNIQUE` 제약도 함께 고려한다.
* 새로운 enum/status/error code를 추가할 경우 기존 명명 방식과 관리 위치를 따른다.
* 공통화 목적만으로 불필요한 추상 클래스, 인터페이스, 유틸 클래스를 만들지 않는다.
* 기능 구현과 관계없는 formatting, rename, cleanup 리팩토링을 함께 수행하지 않는다.
* 새로 작성하거나 수정하는 테스트 메서드명은 검증하는 동작을 설명하는 한글 문장으로 작성한다. 기존 테스트 전체의 일괄 이름 변경은 별도 작업으로 분리한다.
