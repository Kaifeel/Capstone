# 대규모 트래픽 대응 콘서트 티켓 예매 백엔드 시스템

## 1. 프로젝트 개요

실제 콘서트 티켓팅처럼 예매 시작 직후 많은 사용자가 동시에 접속하는 상황을 가정한 백엔드 시스템이다.

단순 CRUD 예매 서비스가 아니라 다음 문제를 다룬다.

- 같은 좌석 중복 예매 방지
- 좌석 HOLD 및 결제 시간 초과 처리
- 비관적 락/낙관적 락 기반 동시성 제어
- Redis Sorted Set 기반 대기열
- 부하 테스트 시나리오
- Actuator, Prometheus, Grafana 기반 모니터링

## 2. 기술 스택

| 영역 | 기술 |
| --- | --- |
| Language | Java 17, Temurin JDK |
| Framework | Spring Boot 3.x |
| Persistence | Spring Data JPA |
| Database | PostgreSQL |
| Cache / Queue | Redis |
| Auth | Spring Security, JWT |
| Build | Gradle Wrapper |
| Test | JUnit5 |
| Infra | Docker Compose |
| Monitoring | Spring Actuator, Prometheus, Grafana |
| Load Test | k6 |

## 3. 주요 기능

### 회원/인증

- 회원가입
- 로그인
- JWT 발급
- USER / ADMIN 권한 분리

### 공연/좌석

- 공연 등록, 수정, 삭제, 조회
- 공연별 좌석 다건 생성
- 좌석 상태 조회
- 좌석 상태: `AVAILABLE`, `HOLD`, `RESERVED`, `CANCELLED`

### 예매

- 좌석 예매 생성
- 예매 생성 시 좌석 `HOLD`
- 예매 취소 시 좌석 `AVAILABLE` 복구
- 내 예매 목록 조회
- 예매 상세 조회

### 동시성 제어

- 단순 트랜잭션 동시성 문제 재현 테스트
- 비관적 락 기반 중복 예매 방지
- 낙관적 락 기반 중복 예매 방지
- 같은 좌석 동시 요청 시 활성 예매 1건만 생성되도록 검증

### Redis 대기열

- Redis Sorted Set 기반 대기열 진입
- 대기 순번 조회
- 입장 가능 사용자 승격
- entryToken 발급
- 예매 API에서 entryToken 검증

### Mock 결제

실제 결제 연동은 하지 않는다.

- 민감 결제 정보 수집 없음
- 외부 PG API 호출 없음
- 성공/실패 결과만 시뮬레이션
- 결제 성공 시 예매 확정 및 좌석 예약
- 결제 실패/시간 초과 시 좌석 복구

## 4. 프로젝트 구조

```text
src/main/java/com/example/ticketing
  auth
  user
  concert
  seat
  reservation
  payment
  waitingroom
  monitoring
  common
  config
  security
```

문서:

```text
docs/
  01-requirements-and-overall-design.md
  02-erd-design.md
  03-api-specification.md
  04-spring-boot-project-structure.md
  05-basic-crud-implementation.md
  06-reservation-transaction.md
  07-concurrency-reproduction-test.md
  08-lock-based-duplicate-prevention.md
  09-redis-waiting-room.md
  10-mock-payment-and-expiration.md
  11-load-test-and-monitoring.md
  12-capstone-presentation.md
  13-manual-verification-guide.md
  14-verification-status.md
```

## 5. 실행 방법

### 5.1 인프라 실행

```bash
docker compose up -d
```

포함 서비스:

- PostgreSQL
- Redis
- Redis exporter
- Prometheus
- Grafana

### 5.2 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 URL:

```text
http://localhost:8080
```

### 5.3 컴파일 확인

```bash
./gradlew compileJava
```

### 5.4 테스트 실행

```bash
./gradlew test
```

결제 관련 테스트 실행이 부담스럽다면 검증 상태 문서를 먼저 확인한다.

```text
docs/14-verification-status.md
```

## 6. 수동 검증

수동 검증 가이드:

```text
docs/13-manual-verification-guide.md
```

샘플 스크립트:

```bash
scripts/http/manual-flow.sh
```

## 7. 주요 API

### Auth

```text
POST /api/v1/auth/signup
POST /api/v1/auth/login
```

### Concert

```text
POST   /api/v1/concerts
GET    /api/v1/concerts
GET    /api/v1/concerts/{concertId}
PATCH  /api/v1/concerts/{concertId}
DELETE /api/v1/concerts/{concertId}
```

### Seat

```text
POST /api/v1/concerts/{concertId}/seats
GET  /api/v1/concerts/{concertId}/seats
GET  /api/v1/seats/{seatId}
```

### Waiting Room

```text
POST /api/v1/waiting-room/enter
GET  /api/v1/waiting-room/status
POST /api/v1/waiting-room/validate
POST /api/v1/waiting-room/admit
```

### Reservation

```text
POST /api/v1/reservations
GET  /api/v1/reservations/{reservationId}
GET  /api/v1/reservations/me
POST /api/v1/reservations/{reservationId}/cancel
```

### Payment

```text
POST /api/v1/payments/mock
```

### Admin Metrics

```text
GET /api/v1/admin/metrics/reservations?concertId=1
```

## 8. 모니터링

### Actuator

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/prometheus
```

### Prometheus

```text
http://localhost:9090
```

### Grafana

```text
http://localhost:3000
```

기본 계정:

```text
admin / admin
```

## 9. 부하 테스트

k6 시나리오 위치:

```text
load-test/k6/
```

같은 좌석 집중 예매:

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TOKEN={jwt} \
  -e CONCERT_ID=1 \
  -e SEAT_ID=1 \
  -e VUS=100 \
  load-test/k6/same-seat-reservation.js
```

여러 좌석 분산 예매:

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TOKEN={jwt} \
  -e CONCERT_ID=1 \
  -e START_SEAT_ID=1 \
  -e SEAT_COUNT=100 \
  -e VUS=500 \
  load-test/k6/multiple-seats-reservation.js
```

대기열 테스트:

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TOKEN={jwt} \
  -e CONCERT_ID=1 \
  -e VUS=1000 \
  load-test/k6/waiting-room-flow.js
```

## 10. 동시성 검증

문제 재현:

```text
ReservationConcurrencyReproductionTest
```

락 기반 해결:

```text
ReservationLockConcurrencyTest
```

검증 기준:

```text
같은 좌석에 대해 동시에 여러 요청이 들어와도 활성 예매는 1개만 존재해야 한다.
```

검증 SQL:

```sql
SELECT seat_id, COUNT(*) AS active_count
FROM reservations
WHERE status IN ('PENDING', 'CONFIRMED')
GROUP BY seat_id
HAVING COUNT(*) > 1;
```

결과가 없어야 한다.

## 11. 캡스톤 발표 핵심

이 프로젝트의 핵심은 CRUD가 아니라 다음 세 가지다.

1. 중복 예매 방지
2. 대기열 기반 트래픽 진입 제어
3. 테스트와 모니터링으로 검증 가능한 백엔드 설계

발표에서는 다음 흐름으로 설명한다.

```text
문제 정의
-> 단순 트랜잭션의 한계
-> 락 기반 중복 예매 방지
-> Redis 대기열로 트래픽 제어
-> Mock 결제와 만료 처리로 좌석 상태 복구
-> k6와 Grafana로 결과 관측
```
