# 10단계. Mock 결제 및 예매 만료 처리

## 1. 범위

이번 단계의 결제 기능은 실제 PG사, 카드사, 계좌이체, 간편결제와 연동하지 않는다.

이 프로젝트에서 결제는 다음 목적만 가진다.

- 예매 생성 후 `PENDING` 상태를 표현한다.
- Mock 결제 성공 시 예매와 좌석 상태를 확정 상태로 바꾼다.
- Mock 결제 실패 시 예매를 취소하고 좌석을 다시 예매 가능 상태로 복구한다.
- 결제 제한 시간이 지나면 예매를 만료시키고 좌석을 복구한다.

즉, 실제 돈이 이동하는 결제 시스템이 아니라 티켓팅 백엔드의 상태 전이 검증용 Mock Payment다.

## 2. 안전 정책

결제 기능은 위험도가 높은 도메인이므로 캡스톤 범위에서는 다음을 지킨다.

- 외부 결제 API를 호출하지 않는다.
- 카드번호, 계좌번호, 생년월일, CVC 같은 민감 정보를 받지 않는다.
- 결제 승인 번호나 영수증을 실제처럼 생성하지 않는다.
- 결제 성공/실패는 요청 값으로만 시뮬레이션한다.
- 결제 금액은 좌석 가격을 기준으로 내부 상태 검증에만 사용한다.
- 실제 운영 가능한 결제 기능이라고 설명하지 않는다.

## 3. 구현한 파일

### Payment Domain

```text
src/main/java/com/example/ticketing/payment/domain/Payment.java
src/main/java/com/example/ticketing/payment/domain/PaymentStatus.java
```

### Payment Repository

```text
src/main/java/com/example/ticketing/payment/repository/PaymentRepository.java
```

### Payment DTO

```text
src/main/java/com/example/ticketing/payment/dto/MockPaymentRequest.java
src/main/java/com/example/ticketing/payment/dto/MockPaymentResponse.java
src/main/java/com/example/ticketing/payment/dto/MockPaymentResult.java
```

### Payment Service / Controller

```text
src/main/java/com/example/ticketing/payment/service/PaymentService.java
src/main/java/com/example/ticketing/payment/controller/PaymentController.java
```

### Reservation Expiration

```text
src/main/java/com/example/ticketing/reservation/service/ReservationExpirationService.java
src/main/java/com/example/ticketing/reservation/scheduler/ReservationExpirationScheduler.java
```

## 4. Payment 상태

```text
PENDING
SUCCESS
FAILED
TIMEOUT
```

의미:

| 상태 | 의미 |
| --- | --- |
| PENDING | 결제 요청 생성 |
| SUCCESS | Mock 결제 성공 |
| FAILED | Mock 결제 실패 |
| TIMEOUT | 결제 제한 시간 초과 |

## 5. 예매/좌석/결제 상태 전이

### 예매 생성 직후

```text
Seat: AVAILABLE -> HOLD
Reservation: PENDING
Payment: 아직 없거나 PENDING
```

### Mock 결제 성공

```text
Payment: PENDING -> SUCCESS
Reservation: PENDING -> CONFIRMED
Seat: HOLD -> RESERVED
```

### Mock 결제 실패

```text
Payment: PENDING -> FAILED
Reservation: PENDING -> CANCELLED
Seat: HOLD -> AVAILABLE
```

### 결제 시간 초과

```text
Payment: PENDING -> TIMEOUT
Reservation: PENDING -> EXPIRED
Seat: HOLD -> AVAILABLE
```

## 6. API

### Mock 결제

```http
POST /api/v1/payments/mock
```

권한:

```text
USER
```

Request:

```json
{
  "reservationId": 1,
  "result": "SUCCESS"
}
```

`result` 값:

```text
SUCCESS
FAILED
```

Success Response:

```json
{
  "success": true,
  "code": "OK",
  "message": "결제가 완료되었습니다.",
  "data": {
    "paymentId": 1,
    "reservationId": 1,
    "paymentStatus": "SUCCESS",
    "reservationStatus": "CONFIRMED",
    "seatStatus": "RESERVED",
    "approvedAt": "2026-05-07T17:00:00",
    "failedAt": null
  }
}
```

Failed Response:

```json
{
  "success": true,
  "code": "OK",
  "message": "결제가 실패했습니다.",
  "data": {
    "paymentId": 1,
    "reservationId": 1,
    "paymentStatus": "FAILED",
    "reservationStatus": "CANCELLED",
    "seatStatus": "AVAILABLE",
    "approvedAt": null,
    "failedAt": "2026-05-07T17:00:00"
  }
}
```

## 7. Mock 결제 처리 규칙

Mock 결제 요청 시 다음을 검증한다.

1. 예매가 존재해야 한다.
2. 요청 사용자가 예매 소유자여야 한다.
3. 예매 상태가 `PENDING`이어야 한다.
4. 예매의 `expiresAt`이 현재 시간보다 이후여야 한다.
5. 결제 결과는 `SUCCESS` 또는 `FAILED`여야 한다.

시간이 이미 만료된 예매에 결제를 요청하면:

```text
Reservation -> EXPIRED
Seat -> AVAILABLE
RESERVATION_EXPIRED 예외
```

## 8. 예매 만료 처리

만료 처리 서비스:

```text
ReservationExpirationService
```

스케줄러:

```text
ReservationExpirationScheduler
```

기본 실행 주기:

```text
30초
```

설정 키:

```yaml
reservation:
  expiration-scheduler:
    fixed-delay-ms: 30000
```

처리 대상:

```text
status = PENDING
expiresAt < now
```

처리 결과:

```text
Reservation -> EXPIRED
Seat -> AVAILABLE
Payment -> TIMEOUT
```

## 9. 테스트

결제/만료 테스트 파일은 추가했다.

```text
src/test/java/com/example/ticketing/payment/PaymentServiceTest.java
```

테스트 의도:

- Mock 결제 성공 시 예매 확정 및 좌석 예약
- Mock 결제 실패 시 예매 취소 및 좌석 복구
- 다른 사용자의 결제 요청 차단
- 만료된 예매 처리 시 좌석 복구 및 Payment TIMEOUT

다만 사용자가 요청한 대로 이번 단계에서는 전체 테스트 실행을 진행하지 않았다.

## 10. 발표에서 설명할 포인트

결제 부분은 다음처럼 설명하는 것이 안전하다.

```text
실제 결제 연동은 하지 않았고, 예매 시스템에서 필요한 결제 전후 상태 전이를 Mock Payment로 시뮬레이션했습니다.
민감 결제 정보는 받지 않으며, 성공/실패 결과만 입력받아 좌석과 예매 상태의 정합성을 검증했습니다.
```

핵심은 결제 자체가 아니라 다음 정합성이다.

- 결제 성공 후 좌석이 다시 예매되지 않아야 한다.
- 결제 실패 후 좌석이 다시 예매 가능해야 한다.
- 결제 시간 초과 후 HOLD 좌석이 계속 묶여 있으면 안 된다.
- 예매 상태와 좌석 상태가 서로 어긋나면 안 된다.

## 11. 다음 단계

11단계에서는 부하 테스트와 모니터링을 구성한다.

목표:

- k6 또는 JMeter 시나리오 작성
- 같은 좌석 동시 예매 테스트
- 여러 좌석 분산 예매 테스트
- 대기열 적용 전후 비교
- Actuator 메트릭 확인
- Prometheus/Grafana 구성
