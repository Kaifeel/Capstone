# 7단계. 동시성 문제 재현 테스트

## 1. 목표

이번 단계의 목표는 단순 트랜잭션 방식이 왜 대규모 티켓팅 상황에서 위험한지 테스트로 보여주는 것이다.

핵심 문제:

```text
여러 사용자가 같은 좌석을 거의 동시에 조회하면 모두 AVAILABLE로 판단할 수 있다.
그 뒤 각 요청이 예매를 생성하면 하나의 좌석에 여러 활성 예매가 생길 수 있다.
```

## 2. 추가한 테스트

테스트 파일:

```text
src/test/java/com/example/ticketing/reservation/ReservationConcurrencyReproductionTest.java
```

테스트 이름:

```text
naiveTransaction_canCreateDuplicatedReservationsWhenRequestsReadAvailableSeatAtSameTime
```

## 3. 테스트 설계

현재 운영 코드의 `Seat` 엔티티에는 `@Version`이 있다. 이 필드는 이미 낙관적 락 역할을 하기 때문에, 운영 `ReservationService`를 그대로 사용하면 단순 트랜잭션의 문제를 순수하게 재현하기 어렵다.

그래서 이번 테스트에서는 의도적으로 다음을 우회했다.

- JPA 엔티티의 `@Version` 체크
- 비관적 락
- Redis 분산락
- DB partial unique index

대신 `JdbcTemplate`과 `TransactionTemplate`으로 naive 예매 흐름을 직접 구성했다.

## 4. naive 예매 흐름

테스트 안의 naive 예매 흐름은 다음과 같다.

1. 트랜잭션 시작
2. 좌석 상태 조회
3. 활성 예매 수 조회
4. 모든 스레드가 조회를 끝낼 때까지 대기
5. 조회 당시 좌석이 `AVAILABLE`이고 활성 예매가 0개면 진행
6. 좌석을 `HOLD`로 변경
7. `PENDING` 예매 INSERT
8. 트랜잭션 커밋

의도적으로 4번 대기 지점을 넣어 두 요청이 모두 같은 초기 상태를 읽도록 만들었다.

```text
Thread A: Seat = AVAILABLE, Active Reservation = 0 조회
Thread B: Seat = AVAILABLE, Active Reservation = 0 조회
Thread A: Seat HOLD 변경, Reservation INSERT
Thread B: Seat HOLD 변경, Reservation INSERT
```

## 5. 테스트 검증 기준

테스트는 다음을 검증한다.

```text
두 요청 모두 성공한다.
하나의 seat_id에 대해 활성 예매가 2개 생성된다.
```

검증 SQL:

```sql
select count(*)
from reservations
where seat_id = ?
  and status in ('PENDING', 'CONFIRMED');
```

기대 결과:

```text
activeReservationCount = 2
```

이 결과는 성공적인 기능 동작이 아니라, 의도적으로 재현한 장애 상황이다.

## 6. 테스트 결과

실행 명령:

```bash
./gradlew test --tests com.example.ticketing.reservation.ReservationConcurrencyReproductionTest
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

## 7. 의미

이 테스트는 다음 사실을 보여준다.

- `@Transactional`만으로는 동시성 정합성을 보장할 수 없다.
- 조회 시점의 상태가 커밋 시점에도 유효하다고 가정하면 안 된다.
- 좌석 예매처럼 경쟁이 집중되는 도메인은 별도의 동시성 제어가 필요하다.
- 최종 방어선으로 DB unique 제약도 함께 고려해야 한다.

## 8. 다음 단계와 비교 기준

8단계에서는 락을 이용해 중복 예매를 방지한다.

비교할 방식:

- 비관적 락
- 낙관적 락
- Redis 분산락

8단계 테스트의 성공 기준:

```text
동시에 여러 사용자가 같은 좌석을 예매해도 활성 예매는 1개만 생성된다.
나머지 요청은 명확한 실패 응답을 받는다.
```

비교 지표:

- 성공 요청 수
- 실패 요청 수
- 활성 예매 수
- 평균 수행 시간
- 예외 유형
- 구현 복잡도
