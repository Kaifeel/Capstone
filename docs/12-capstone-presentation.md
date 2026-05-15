# 12단계. 캡스톤 발표용 핵심 설명 문서

## 1. 발표 제목

대규모 트래픽 대응 콘서트 티켓 예매 백엔드 시스템

## 2. 한 줄 소개

동시 접속자가 몰리는 콘서트 티켓팅 상황에서 좌석 중복 예매를 방지하고, Redis 대기열로 서버 부하를 제어하며, 부하 테스트와 모니터링으로 결과를 검증하는 백엔드 시스템이다.

## 3. 문제 정의

일반적인 CRUD 예매 서비스는 사용자가 적을 때는 정상적으로 동작한다.

하지만 실제 콘서트 티켓팅에서는 다음 문제가 발생한다.

- 예매 시작 직후 많은 사용자가 동시에 접속한다.
- 여러 사용자가 같은 좌석을 동시에 선택한다.
- 서버와 DB에 순간적으로 높은 부하가 걸린다.
- 결제 전 좌석을 임시로 점유해야 한다.
- 결제 실패 또는 시간 초과 시 좌석을 다시 풀어야 한다.

이 프로젝트는 위 문제를 백엔드 관점에서 해결하는 것을 목표로 한다.

## 4. 핵심 목표

```text
1. 같은 좌석 중복 예매 방지
2. 대기열을 통한 트래픽 진입 제어
3. 결제 전 HOLD와 만료 처리
4. 동시성 테스트로 문제 재현
5. 락 적용 후 해결 검증
6. 부하 테스트와 모니터링 구성
```

## 5. 전체 아키텍처

```text
Client
  |
  v
Spring Boot API Server
  |
  |-- PostgreSQL
  |     - User
  |     - Concert
  |     - Seat
  |     - Reservation
  |     - Payment
  |
  |-- Redis
  |     - Waiting Room
  |     - Entry Token
  |
  |-- Actuator
        |
        v
Prometheus -> Grafana
```

## 6. 주요 도메인

| 도메인 | 역할 |
| --- | --- |
| User | 회원, 권한 |
| Concert | 공연 정보, 예매 기간 |
| Seat | 좌석 정보, 좌석 상태 |
| Reservation | 예매 상태 |
| Payment | Mock 결제 상태 |
| Waiting Room | Redis 기반 대기열 |

## 7. 좌석 상태 모델

```text
AVAILABLE -> HOLD -> RESERVED
        \       \
         \       -> AVAILABLE
          -> CANCELLED
```

의미:

- `AVAILABLE`: 예매 가능
- `HOLD`: 결제 전 임시 점유
- `RESERVED`: 결제 완료
- `CANCELLED`: 비활성 또는 취소

## 8. 예매 상태 모델

```text
PENDING -> CONFIRMED
PENDING -> CANCELLED
PENDING -> EXPIRED
CONFIRMED -> CANCELLED
```

의미:

- `PENDING`: 결제 대기
- `CONFIRMED`: 결제 성공
- `CANCELLED`: 취소 또는 결제 실패
- `EXPIRED`: 결제 시간 초과

## 9. 동시성 문제 재현

단순 트랜잭션 방식에서는 다음 상황이 발생할 수 있다.

```text
User A: seat_id = 1 AVAILABLE 조회
User B: seat_id = 1 AVAILABLE 조회
User A: Reservation INSERT
User B: Reservation INSERT
```

결과:

```text
하나의 좌석에 활성 예매가 2개 생성될 수 있음
```

이 문제는 `ReservationConcurrencyReproductionTest`로 재현했다.

## 10. 동시성 해결

### 비관적 락

```text
SELECT ... FOR UPDATE
```

효과:

- 같은 좌석 row를 하나의 요청만 점유한다.
- 나머지 요청은 대기 후 이미 점유된 좌석임을 확인하고 실패한다.

### 낙관적 락

```text
@Version
```

효과:

- 동시에 읽을 수는 있지만, 먼저 커밋한 요청만 성공한다.
- 나머지는 version 충돌로 실패한다.

검증:

```text
동시 사용자 20명
같은 좌석 요청
성공 1건
실패 19건
활성 예매 1건
```

## 11. Redis 대기열

대기열은 Redis Sorted Set을 사용한다.

```text
waiting:{concertId}:queue
```

흐름:

```text
1. 사용자가 대기열 진입
2. Redis Sorted Set에 토큰 저장
3. 사용자는 순번 조회
4. 서버가 상위 N명을 입장 허용
5. 입장 가능 사용자는 entryToken 발급
6. entryToken이 있는 사용자만 예매 API 호출
```

대기열을 사용하는 이유:

- 예매 API 직접 진입 요청 수를 줄인다.
- DB row lock 경합을 줄인다.
- 사용자에게 공정한 순번 정보를 제공한다.
- 서버가 입장 가능한 사용자 수를 조절할 수 있다.

## 12. Mock 결제

실제 결제 연동은 하지 않는다.

안전 범위:

- 카드번호, 계좌번호, CVC 등 민감 정보 없음
- PG사 API 호출 없음
- 결제 성공/실패 상태만 시뮬레이션

상태 전이:

```text
성공:
Payment SUCCESS
Reservation CONFIRMED
Seat RESERVED

실패:
Payment FAILED
Reservation CANCELLED
Seat AVAILABLE

시간 초과:
Payment TIMEOUT
Reservation EXPIRED
Seat AVAILABLE
```

## 13. 부하 테스트

k6 시나리오:

```text
same-seat-reservation.js
multiple-seats-reservation.js
waiting-room-flow.js
```

실험 조건:

```text
100명
500명
1000명
```

측정 지표:

- 성공률
- 실패율
- 평균 응답 시간
- p95 응답 시간
- TPS
- 중복 예매 여부
- 대기열 길이

## 14. 모니터링

구성:

```text
Spring Actuator
Prometheus
Grafana
Redis exporter
```

관측 지표:

- HTTP 요청 수
- HTTP 응답 시간
- HTTP 에러율
- DB 커넥션
- Redis 연결 수
- JVM 메모리
- 예매 상태별 수
- 좌석 상태별 수
- 대기열 길이

## 15. 구현 결과 요약

| 항목 | 구현 여부 |
| --- | --- |
| 회원가입/로그인/JWT | 완료 |
| 공연/좌석 CRUD | 완료 |
| 예매 생성/조회/취소 | 완료 |
| 단순 트랜잭션 예매 | 완료 |
| 동시성 문제 재현 테스트 | 완료 |
| 비관적 락 | 완료 |
| 낙관적 락 | 완료 |
| Redis 대기열 | 완료 |
| Mock 결제 | 완료 |
| 예매 만료 처리 | 완료 |
| k6 부하 테스트 시나리오 | 완료 |
| Prometheus/Grafana 설정 | 완료 |

## 16. 발표 결론

이 프로젝트는 단순 예매 CRUD가 아니라 실제 티켓팅 백엔드에서 중요한 세 가지 문제를 다룬다.

```text
정합성: 같은 좌석은 한 번만 예매되어야 한다.
트래픽 제어: 모든 사용자가 한 번에 예매 API로 진입하면 안 된다.
관측 가능성: 부하와 장애 상황을 수치로 확인할 수 있어야 한다.
```

최종적으로 다음을 구현했다.

- 동시성 문제를 테스트로 재현
- 락으로 중복 예매 방지
- Redis 대기열로 진입 제어
- Mock 결제와 만료 처리로 좌석 상태 복구
- k6와 Grafana로 실험 가능한 구조 마련

## 17. 향후 개선

- Redis 분산락 적용
- Kafka 또는 RabbitMQ 이벤트 처리
- 대기열 입장 허용 자동 조절
- 결제/예매 이벤트 outbox 패턴
- 실서비스 수준의 장애 복구 정책
- Grafana custom dashboard 고도화
- Testcontainers 기반 PostgreSQL/Redis 통합 테스트 강화
