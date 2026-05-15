# 14. 검증 상태

## 1. 목적

이 문서는 현재 프로젝트에서 수행한 검증과 아직 남은 검증을 명확히 구분한다.

## 2. 수행한 검증

### 컴파일 검증

명령:

```bash
./gradlew compileJava
```

상태:

```text
성공
```

의미:

- main Java 코드가 컴파일된다.
- Spring Boot 애플리케이션 코드의 타입 오류는 현재 확인된 범위에서 없다.

### 이전 단계 테스트 검증

8단계까지 다음 테스트는 통과를 확인했다.

```text
ReservationServiceTest
ReservationConcurrencyReproductionTest
ReservationLockConcurrencyTest
```

검증 내용:

- 예매 생성 트랜잭션
- 예매 취소 시 좌석 복구
- 단순 트랜잭션 동시성 문제 재현
- 비관적 락/낙관적 락 기반 중복 예매 방지

## 3. 실행하지 않은 검증

사용자 요청에 따라 10단계 이후 전체 테스트 실행은 중단했다.

실행하지 않은 항목:

```text
./gradlew test 전체 실행
PaymentServiceTest 실행 결과 확인
실제 Docker Compose 기반 수동 API 검증
k6 부하 테스트 실제 실행
Grafana dashboard 화면 확인
```

이유:

- Mock Payment도 결제라는 이름을 가지므로 실제 결제 기능으로 오해되지 않도록 범위를 조심스럽게 제한했다.
- 10단계 이후에는 전체 테스트 대신 컴파일 중심으로 확인했다.

## 4. 제출 전 권장 검증

제출 전 다음 순서로 검증하는 것을 권장한다.

### 1. 인프라 실행

```bash
docker compose up -d
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. Health Check

```bash
curl http://localhost:8080/actuator/health
```

### 4. 수동 API 검증

문서:

```text
docs/13-manual-verification-guide.md
```

스크립트:

```bash
scripts/http/manual-flow.sh
```

### 5. 결제 제외 테스트

결제 테스트를 제외하고 나머지만 확인하려면 다음 명령을 사용할 수 있다.

```bash
./gradlew test \
  --tests com.example.ticketing.reservation.ReservationServiceTest \
  --tests com.example.ticketing.reservation.ReservationConcurrencyReproductionTest \
  --tests com.example.ticketing.reservation.ReservationLockConcurrencyTest
```

### 6. Mock Payment 테스트

Mock 결제 상태 전이를 확인하려면 다음을 별도로 실행한다.

```bash
./gradlew test --tests com.example.ticketing.payment.PaymentServiceTest
```

주의:

```text
이 테스트는 실제 결제가 아니라 상태 전이 시뮬레이션만 검증한다.
```

### 7. 부하 테스트

```bash
k6 run load-test/k6/same-seat-reservation.js
k6 run load-test/k6/multiple-seats-reservation.js
k6 run load-test/k6/waiting-room-flow.js
```

실행 전 JWT, 공연 ID, 좌석 ID 환경 변수를 지정해야 한다.

## 5. 현재 상태 요약

| 항목 | 상태 |
| --- | --- |
| main 코드 컴파일 | 완료 |
| 예매 트랜잭션 테스트 | 완료 |
| 동시성 문제 재현 테스트 | 완료 |
| 락 기반 중복 방지 테스트 | 완료 |
| PaymentServiceTest | 작성됨, 최종 실행 보류 |
| Docker Compose 수동 검증 | 문서/스크립트 작성됨, 실행 필요 |
| k6 부하 테스트 | 시나리오 작성됨, 실행 필요 |
| Grafana 확인 | 설정 작성됨, 실행 필요 |

## 6. 발표 시 표현

발표에서는 검증 상태를 다음처럼 표현하는 것이 정확하다.

```text
동시성 문제 재현과 락 기반 해결은 테스트로 검증했습니다.
Mock 결제는 실제 결제 연동이 아니라 상태 전이 시뮬레이션이며, 민감 결제 정보는 다루지 않습니다.
부하 테스트와 모니터링은 실행 가능한 시나리오와 설정을 구성했으며, 실제 측정값은 실행 환경에서 수집합니다.
```
