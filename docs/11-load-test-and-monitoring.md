# 11단계. 부하 테스트 및 모니터링 구성

## 1. 목표

이번 단계의 목표는 대규모 티켓팅 상황을 측정 가능한 형태로 만드는 것이다.

구성한 범위:

- Spring Actuator Prometheus 메트릭 노출
- Prometheus 수집 설정
- Grafana datasource/provisioning/dashboard 설정
- Redis exporter 설정
- 관리자 예매 지표 API
- k6 부하 테스트 시나리오

## 2. 추가한 파일

### Monitoring

```text
src/main/java/com/example/ticketing/monitoring/controller/AdminMetricsController.java
src/main/java/com/example/ticketing/monitoring/service/ReservationMetricsService.java
src/main/java/com/example/ticketing/monitoring/dto/ReservationMetricsResponse.java
```

### Prometheus / Grafana

```text
monitoring/prometheus/prometheus.yml
monitoring/grafana/provisioning/datasources/prometheus.yml
monitoring/grafana/provisioning/dashboards/dashboards.yml
monitoring/grafana/dashboards/ticketing-overview.json
```

### Load Test

```text
load-test/k6/same-seat-reservation.js
load-test/k6/multiple-seats-reservation.js
load-test/k6/waiting-room-flow.js
load-test/k6/README.md
```

### Docker Compose

```text
docker-compose.yml
```

추가 서비스:

- redis-exporter
- prometheus
- grafana

## 3. 모니터링 구성

### Spring Actuator

이미 다음 endpoint를 노출한다.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

확인 URL:

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/prometheus
```

### Prometheus

실행 URL:

```text
http://localhost:9090
```

수집 대상:

```text
ticketing-api: host.docker.internal:8080/actuator/prometheus
redis: redis-exporter:9121
prometheus: prometheus:9090
```

### Grafana

실행 URL:

```text
http://localhost:3000
```

기본 계정:

```text
admin / admin
```

자동 등록:

- Prometheus datasource
- Ticketing Overview dashboard

## 4. Docker Compose 실행

인프라 실행:

```bash
docker compose up -d
```

서비스 확인:

```bash
docker compose ps
```

Spring Boot 실행:

```bash
./gradlew bootRun
```

Prometheus는 Docker 컨테이너 안에서 호스트의 Spring Boot 앱을 `host.docker.internal:8080`으로 scrape한다.

## 5. 관리자 지표 API

API:

```http
GET /api/v1/admin/metrics/reservations?concertId=1
```

권한:

```text
ADMIN
```

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "요청이 성공했습니다.",
  "data": {
    "concertId": 1,
    "totalReservationCount": 1000,
    "pendingCount": 100,
    "confirmedCount": 780,
    "cancelledCount": 50,
    "expiredCount": 70,
    "availableSeatCount": 9120,
    "holdSeatCount": 100,
    "reservedSeatCount": 780,
    "waitingRoomLength": 2350
  }
}
```

`waitingRoomLength`는 Redis 연결이 실패하면 `-1`로 반환한다.

## 6. Grafana Dashboard 지표

기본 dashboard에는 다음 panel을 구성했다.

- HTTP request rate
- HTTP p95 latency
- HTTP 5xx error rate
- Redis connected clients
- Hikari active connections
- JVM memory used

추가하면 좋은 지표:

- 예약 성공/실패 custom counter
- 대기열 길이 custom gauge
- 좌석 상태별 gauge
- 결제 성공/실패/만료 counter

## 7. k6 시나리오

### 같은 좌석 집중 예매

목적:

```text
여러 사용자가 하나의 좌석에 동시에 예매 요청을 보낼 때 중복 예매가 발생하지 않는지 확인한다.
```

실행:

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TOKEN={jwt} \
  -e CONCERT_ID=1 \
  -e SEAT_ID=1 \
  -e VUS=100 \
  load-test/k6/same-seat-reservation.js
```

확인:

- 성공 요청은 최대 1개여야 한다.
- 나머지는 `SEAT_ALREADY_OCCUPIED` 또는 충돌 응답이어야 한다.
- DB에서 활성 예매 수가 1개인지 확인한다.

검증 SQL:

```sql
SELECT seat_id, COUNT(*) AS active_count
FROM reservations
WHERE status IN ('PENDING', 'CONFIRMED')
GROUP BY seat_id
HAVING COUNT(*) > 1;
```

결과가 없어야 한다.

### 여러 좌석 분산 예매

목적:

```text
같은 공연 안의 여러 좌석으로 요청이 분산될 때 처리량과 응답 시간을 확인한다.
```

실행:

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

### 대기열 진입/순번 조회

목적:

```text
Redis 대기열이 높은 진입 요청을 처리하고 순번 조회를 안정적으로 응답하는지 확인한다.
```

실행:

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TOKEN={jwt} \
  -e CONCERT_ID=1 \
  -e VUS=1000 \
  load-test/k6/waiting-room-flow.js
```

## 8. 권장 실험 조건

각 시나리오는 다음 동시 사용자 수로 반복한다.

```text
100명
500명
1000명
```

기록할 값:

- 총 요청 수
- 성공률
- 실패율
- 평균 응답 시간
- p95 응답 시간
- TPS 또는 requests/sec
- DB active connection 수
- Redis connected clients
- 대기열 길이
- 중복 예매 발생 여부

## 9. 대기열 적용 전후 비교

### 대기열 미적용

```text
사용자 -> 예매 API 직접 호출
```

관찰 포인트:

- DB row lock 경합
- 409 응답 증가
- 응답 시간 증가
- API 서버 thread 사용량 증가

### 대기열 적용

```text
사용자 -> 대기열 진입 -> 입장 허용 -> 예매 API 호출
```

관찰 포인트:

- 예매 API 직접 진입 요청 수 감소
- Redis 대기열 길이 증가
- DB 부하 완화
- 사용자에게 순번 제공 가능

## 10. 컴파일 검증

이번 단계에서는 사용자의 이전 요청을 고려해 전체 테스트 대신 애플리케이션 코드 컴파일만 확인했다.

실행:

```bash
./gradlew compileJava
```

결과:

```text
BUILD SUCCESSFUL
```

## 11. 다음 단계

12단계에서는 README와 발표 자료를 정리한다.

포함할 내용:

- 프로젝트 소개
- 실행 방법
- ERD
- API 요약
- 동시성 문제 재현 결과
- 락 적용 결과
- Redis 대기열 구조
- Mock 결제 범위와 안전 정책
- 부하 테스트 시나리오
- 모니터링 화면 설명
- 캡스톤 발표용 핵심 메시지
