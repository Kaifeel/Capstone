# 8단계. 락을 이용한 중복 예매 방지 구현

## 1. 목표

7단계에서는 단순 트랜잭션 방식에서 같은 좌석에 여러 활성 예매가 생길 수 있음을 재현했다.

8단계의 목표는 같은 좌석에 여러 사용자가 동시에 예매 요청을 보내도 활성 예매가 1개만 생성되도록 만드는 것이다.

이번 단계에서 구현한 방식:

- 비관적 락
- 낙관적 락

Redis 분산락은 Redis 기반 대기열을 구현한 뒤, Redis 운영 구조와 함께 확장하는 편이 자연스럽다.

## 2. 구현 파일

### Service

```text
src/main/java/com/example/ticketing/reservation/service/ReservationService.java
```

추가한 메서드:

```java
@Transactional
public ReservationCreateResponse createWithPessimisticLock(Long userId, ReservationCreateRequest request)
```

```java
@Transactional
public ReservationCreateResponse createWithOptimisticLock(Long userId, ReservationCreateRequest request)
```

기존 단순 트랜잭션 메서드:

```java
@Transactional
public ReservationCreateResponse create(Long userId, ReservationCreateRequest request)
```

### Repository

```text
src/main/java/com/example/ticketing/seat/repository/SeatRepository.java
```

비관적 락 조회:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select s from Seat s where s.id = :seatId")
Optional<Seat> findByIdForUpdate(@Param("seatId") Long seatId);
```

### Entity

```text
src/main/java/com/example/ticketing/seat/domain/Seat.java
```

낙관적 락 필드:

```java
@Version
private Long version;
```

### Controller

```text
src/main/java/com/example/ticketing/reservation/controller/ReservationController.java
```

실제 예매 API는 8단계 이후 비관적 락 기반 메서드를 호출한다.

```java
reservationService.createWithPessimisticLock(userDetails.getUserId(), request)
```

## 3. 공통 예매 로직 분리

좌석 조회 전략만 바꾸고 나머지 검증과 상태 변경은 동일하게 사용한다.

공통 흐름:

1. 현재 시간 조회
2. 사용자 조회
3. 공연 조회
4. 예매 가능 시간 검증
5. 좌석 조회
6. 좌석과 공연 일치 검증
7. 활성 예매 존재 여부 검증
8. 같은 사용자 중복 예매 검증
9. 좌석 `HOLD` 처리
10. `PENDING` 예매 생성
11. `saveAndFlush`
12. `seatRepository.flush`

`flush`를 명시적으로 호출하는 이유는 낙관적 락 충돌을 트랜잭션 종료 시점이 아니라 서비스 메서드 안에서 확인하기 위해서다.

## 4. 비관적 락 방식

### 동작 방식

비관적 락은 좌석 row를 조회할 때 DB row lock을 건다.

```text
Thread A: seat_id = 1 row lock 획득
Thread B: seat_id = 1 row lock 대기
Thread A: 좌석 HOLD, 예매 생성, commit
Thread B: lock 획득 후 이미 활성 예매가 있음을 확인하고 실패
```

### 장점

- 중복 예매 방지 로직이 직관적이다.
- 충돌이 매우 높은 좌석 예매 상황에서 정합성을 강하게 보장한다.
- 실패 요청은 이미 점유된 좌석으로 명확히 처리할 수 있다.

### 단점

- 같은 좌석 요청이 직렬화되므로 대기 시간이 늘 수 있다.
- 트래픽이 특정 좌석에 집중되면 DB row lock 경합이 커진다.
- 락 대기 시간과 타임아웃 정책을 운영 환경에서 조정해야 한다.

## 5. 낙관적 락 방식

### 동작 방식

낙관적 락은 `Seat.version`을 이용한다.

```text
Thread A: Seat version = 0 조회
Thread B: Seat version = 0 조회
Thread A: Seat HOLD, version = 1 update 성공
Thread B: version = 0 기준 update 시도, 충돌 발생
```

충돌이 발생하면 `OPTIMISTIC_LOCK_CONFLICT`로 변환한다.

### 장점

- 조회 시 DB row lock을 오래 잡지 않는다.
- 충돌이 적은 환경에서는 처리량이 좋을 수 있다.
- 락 대기보다 충돌 감지 방식이라 구조가 가볍다.

### 단점

- 인기 좌석처럼 충돌이 높은 상황에서는 실패와 재시도가 많아질 수 있다.
- 충돌 예외 처리와 재시도 정책을 별도로 설계해야 한다.
- 사용자 경험 측면에서 "방금 다른 사용자가 선택했다"는 실패 응답이 많아질 수 있다.

## 6. 테스트

추가한 테스트 파일:

```text
src/test/java/com/example/ticketing/reservation/ReservationLockConcurrencyTest.java
```

테스트 케이스:

```text
pessimisticLock_allowsOnlyOneReservationForSameSeat
optimisticLock_allowsOnlyOneReservationForSameSeat
```

테스트 조건:

- 사용자 20명 생성
- 같은 공연의 같은 좌석 1개 생성
- 20개 스레드가 동시에 같은 좌석 예매 요청
- 비관적 락 방식과 낙관적 락 방식을 각각 검증

검증 기준:

```text
성공 요청 수 = 1
실패 요청 수 = 19
활성 예매 수 = 1
좌석 상태 = HOLD
```

활성 예매 검증 SQL:

```sql
select count(*)
from reservations
where seat_id = ?
  and status in ('PENDING', 'CONFIRMED');
```

## 7. 테스트 결과

락 테스트:

```bash
./gradlew test --tests com.example.ticketing.reservation.ReservationLockConcurrencyTest
```

결과:

```text
BUILD SUCCESSFUL
```

전체 테스트:

```bash
./gradlew test
```

결과:

```text
BUILD SUCCESSFUL
```

## 8. 7단계와 8단계 비교

| 항목 | 7단계 naive 테스트 | 8단계 락 테스트 |
| --- | --- | --- |
| 동시 요청 수 | 2명 | 20명 |
| 대상 좌석 | 같은 좌석 | 같은 좌석 |
| 성공 예매 수 | 2개 | 1개 |
| 활성 예매 수 | 2개 | 1개 |
| 결과 | 중복 예매 재현 | 중복 예매 방지 |

## 9. 현재 선택

실제 예매 API는 비관적 락 방식을 사용하도록 연결했다.

선택 이유:

- 티켓팅은 인기 좌석에 요청이 집중된다.
- 같은 좌석에 대한 충돌 가능성이 높다.
- 캡스톤 발표에서 동작 원리를 설명하기 쉽다.
- 실패 요청을 이미 점유된 좌석으로 명확히 처리할 수 있다.

낙관적 락 방식은 비교 실험용 메서드와 테스트로 유지한다.

## 10. 다음 단계

9단계에서는 Redis 대기열을 구현한다.

목표:

- Redis Sorted Set 기반 대기열 진입
- 대기 순번 조회
- 입장 허용 토큰 발급
- 예매 API 호출 전 입장 토큰 검증
- 대기열 적용 전후 부하 제어 비교 기준 마련
