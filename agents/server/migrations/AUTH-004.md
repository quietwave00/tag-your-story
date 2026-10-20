# AUTH-004 운영 설정과 장애 대응

## 설정

- `SIGNUP_CODE_SECRET`: 최소 32 UTF-8 byte의 무작위 서버 비밀값. 모든 인스턴스에서 동일해야 한다. 변경하면 진행 중인 가입 코드는 무효화된다. JWT/SMTP 비밀값과 별도로 관리한다.
- `SMTP_HOST`, `SMTP_PORT`(기본 587), `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`: SMTP 접속과 발신 주소. SMTP 인증과 STARTTLS가 필수다. 비밀값은 환경 변수 또는 secret manager에서 주입한다. 설정이 없으면 가입 코드 발송은 `SIGNUP_MAIL_UNAVAILABLE`(503)이다.
- Redis는 기존 `application-redis.yml` 설정을 사용한다. 가입 challenge에는 `signup:challenge:<UUID>` hash, 발송 제한에는 HMAC으로 익명화한 `signup:rate:email:*`, `signup:rate:ip:*` key를 쓴다.
- 후속 공개 API의 client IP는 신뢰할 수 있는 reverse proxy 설정을 거쳐 추출해야 한다. 클라이언트가 보낸 임의의 `X-Forwarded-For` 값을 그대로 사용하면 IP 제한을 우회할 수 있다.

## 저장·만료 정책

- 예약 상태는 최대 30초, SMTP 수락 후 활성 상태는 10분이다. 활성화 전 코드는 검증할 수 없다.
- SMTP 실패는 예약을 삭제하고 503을 돌린다. 삭제 실패 시 예약 TTL이 정리한다. SMTP는 성공했으나 Redis 활성화가 실패하면 503을 돌리고 예약을 삭제한다. 이 경우 사용자가 받은 코드는 사용할 수 없으므로 가입을 다시 시작해야 한다.
- 재발송은 기존 코드를 예약 시점에 무효화한다. 재발송 실패 시 해당 challenge를 폐기하고 새 가입 시작을 요구한다. 이전 코드로 되돌리지 않는다.
- challenge당 총 발송 5회, 재발송 간격 60초, 이메일당 5회/시간, IP당 20회/시간이다. 실패한 발송 예약도 제한 횟수에 포함되어 SMTP 장애 시 재시도 폭주를 막는다. 5회 코드 오입력 시 challenge를 삭제한다.
- 원문 비밀번호와 코드는 저장하지 않는다. `password_hash`에는 호출자가 만든 비밀번호 해시만 전달한다. 코드는 HMAC-SHA256 검증값으로 저장한다. Redis key에 이메일/IP 원문을 쓰지 않는다.

## 비운영 확인·장애 진단

- 비운영 환경에서는 SMTP 테스트 서버 또는 fake `SignupMailSender`를 사용한다. 실제 사용자 주소로 테스트 코드를 보내지 않는다.
- `./gradlew test`, `./gradlew check`, `./scripts/verify.sh`를 사용자가 실행한다. Redis 원자성 테스트는 Docker를 사용할 수 있을 때 Redis 7.2 컨테이너에서 수행된다.
- 503 `SIGNUP_MAIL_UNAVAILABLE`: SMTP host/from/credential, STARTTLS, 네트워크, 제공자 응답을 확인한다. 로그에 코드와 접속 secret을 넣지 않는다.
- 503 `SIGNUP_CHALLENGE_UNAVAILABLE`: Redis 연결과 Lua script 실행을 확인한다. SMTP 수락 후 발생했으면 메일 수신자에게 새 가입을 안내한다.
- 429 `SIGNUP_RATE_LIMITED`: 60초 cooldown, challenge 5회, 이메일/IP 시간당 제한을 확인한다. 제한 key는 1시간 후 만료된다.
