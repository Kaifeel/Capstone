# 문서 인덱스

이 문서는 캡스톤 프로젝트 산출물 위치를 정리한다.


markdown 문서 .md



## 1. 전체 문서

| 순서 | 문서 | 설명 |
| --- | --- | --- |
| 00 | `00-docs-index.md` | 문서 인덱스 |
| 01 | `01-requirements-and-overall-design.md` | 요구사항 정리와 전체 설계 |
| 02 | `02-erd-design.md` | ERD, 테이블, 인덱스, Redis key 설계 |
| 03 | `03-api-specification.md` | API 명세 |
| 04 | `04-spring-boot-project-structure.md` | Spring Boot 프로젝트 구조 |
| 05 | `05-basic-crud-implementation.md` | 기본 CRUD 구현 정리 |
| 06 | `06-reservation-transaction.md` | 좌석 예매 트랜잭션 |
| 07 | `07-concurrency-reproduction-test.md` | 동시성 문제 재현 테스트 |
| 08 | `08-lock-based-duplicate-prevention.md` | 락 기반 중복 예매 방지 |
| 09 | `09-redis-waiting-room.md` | Redis 대기열 |
| 10 | `10-mock-payment-and-expiration.md` | Mock 결제와 예매 만료 |
| 11 | `11-load-test-and-monitoring.md` | 부하 테스트와 모니터링 |
| 12 | `12-capstone-presentation.md` | 발표용 핵심 설명 |
| 13 | `13-manual-verification-guide.md` | 수동 API 검증 가이드 |
| 14 | `14-verification-status.md` | 검증 상태 정리 |

## 2. 코드 산출물

### Application

```text
src/main/java/com/example/ticketing/TicketingApplication.java
```

### Auth/User

```text
src/main/java/com/example/ticketing/auth
src/main/java/com/example/ticketing/user
```

### Concert/Seat

```text
src/main/java/com/example/ticketing/concert
src/main/java/com/example/ticketing/seat
```

### Reservation

```text
src/main/java/com/example/ticketing/reservation
```

### Payment

```text
src/main/java/com/example/ticketing/payment
```

### Waiting Room

```text
src/main/java/com/example/ticketing/waitingroom
```

### Monitoring

```text
src/main/java/com/example/ticketing/monitoring
monitoring/
```

### Load Test

```text
load-test/k6
scripts/http/manual-flow.sh
```

## 3. 실행 관련 파일

```text
README.md
build.gradle
settings.gradle
gradlew
docker-compose.yml
src/main/resources/application.yml
src/main/resources/application-local.yml
src/main/resources/application-test.yml
```

## 4. 테스트 산출물

```text
src/test/java/com/example/ticketing/reservation/ReservationServiceTest.java
src/test/java/com/example/ticketing/reservation/ReservationConcurrencyReproductionTest.java
src/test/java/com/example/ticketing/reservation/ReservationLockConcurrencyTest.java
src/test/java/com/example/ticketing/payment/PaymentServiceTest.java
```

## 5. 발표 시 추천 순서

1. `README.md`로 프로젝트 개요와 실행 방법 설명
2. `01-requirements-and-overall-design.md`로 문제 정의 설명
3. `02-erd-design.md`로 데이터 모델 설명
4. `07-concurrency-reproduction-test.md`로 동시성 문제 설명
5. `08-lock-based-duplicate-prevention.md`로 해결 방식 설명
6. `09-redis-waiting-room.md`로 대기열 설명
7. `10-mock-payment-and-expiration.md`로 결제 범위와 상태 전이 설명
8. `11-load-test-and-monitoring.md`로 실험과 관측 설명
9. `12-capstone-presentation.md`로 발표 흐름 정리
10. `13-manual-verification-guide.md`와 `14-verification-status.md`로 제출 전 검증 범위 설명
