# 6단계. 좌석 예매 트랜잭션 구현

## 1. 구현 목표

이번 단계의 목표는 좌석 예매 요청이 하나의 트랜잭션 안에서 일관되게 처리되도록 만드는 것이다.

예매 생성 시 반드시 함께 처리되어야 하는 변경:

- 좌석 상태를 `AVAILABLE`에서 `HOLD`로 변경
- 좌석의 `holdExpiresAt` 설정
- 예매를 `PENDING` 상태로 생성
- 예매의 `reservedAt`, `expiresAt` 설정

이 중 하나라도 실패하면 전체 트랜잭션이 롤백되어야 한다.

## 2. 현재 구현 위치

```text
src/main/java/com/example/ticketing/reservation/service/ReservationService.java
```

핵심 메서드:

```java
@Transactional
public ReservationCreateResponse create(Long userId, ReservationCreateRequest request)
```

시간 의존 로직은 테스트 가능성을 위해 `Clock`을 주입받는다.

```text
src/main/java/com/example/ticketing/config/TimeConfig.java
```

## 3. 예매 생성 트랜잭션 흐름

현재 예매 생성 흐름은 다음 순서로 동작한다.

1. 현재 시간 조회
2. 사용자 조회
3. 공연 조회
4. 공연 예매 가능 시간 검증
5. 좌석 조회
6. 요청한 공연과 좌석의 공연이 같은지 검증
7. 해당 좌석에 활성 예매가 이미 있는지 검증
8. 같은 사용자가 같은 좌석에 활성 예매를 가지고 있는지 검증
9. 결제 만료 시간 계산
10. 좌석을 `HOLD` 상태로 변경
11. `PENDING` 예매 생성
12. Reservation 저장

현재 활성 예매 기준:

```text
PENDING
CONFIRMED
```

## 4. 좌석 상태 전이

예매 생성 시:

```text
AVAILABLE -> HOLD
```

예매 취소 시:

```text
HOLD -> AVAILABLE
```

현재 단계에서는 결제 성공 흐름이 아직 없으므로 다음 전이는 10단계에서 구현한다.

```text
HOLD -> RESERVED
```

## 5. 트랜잭션 정합성 기준

예매 생성 성공 후 다음 조건이 성립해야 한다.

- Reservation 상태는 `PENDING`이다.
- Seat 상태는 `HOLD`이다.
- Seat의 `holdExpiresAt`과 Reservation의 `expiresAt`은 같다.
- Reservation은 요청 사용자, 공연, 좌석을 참조한다.
- `expiresAt`은 `reservedAt`보다 이후 시간이다.

예매 생성 실패 후 다음 조건이 성립해야 한다.

- Reservation이 생성되지 않는다.
- Seat 상태는 변경되지 않는다.

예매 취소 성공 후 다음 조건이 성립해야 한다.

- Reservation 상태는 `CANCELLED`이다.
- Reservation의 `cancelledAt`이 기록된다.
- Seat 상태는 `AVAILABLE`이다.
- Seat의 `holdExpiresAt`은 null이다.

## 6. 현재 방식의 한계

현재 구현은 단순 트랜잭션 방식이다.

```text
조회 -> 검증 -> 상태 변경 -> 저장
```

이 방식은 단일 요청 흐름에서는 정합성을 보장하지만, 동시에 여러 요청이 같은 좌석을 예매하면 다음 문제가 생길 수 있다.

```text
사용자 A: 좌석 AVAILABLE 조회
사용자 B: 좌석 AVAILABLE 조회
사용자 A: Reservation 생성
사용자 B: Reservation 생성
```

즉, 높은 동시성 상황에서는 중복 예매가 발생할 수 있다. 이 문제는 7단계에서 동시성 테스트로 재현하고, 8단계에서 비관적 락/낙관적 락으로 해결한다.

## 7. 테스트

추가한 테스트 파일:

```text
src/test/java/com/example/ticketing/reservation/ReservationServiceTest.java
```

테스트 케이스:

- 예매 생성 시 좌석이 HOLD되고 PENDING 예매가 생성된다.
- 이미 활성 예매가 있는 좌석은 다시 예매할 수 없다.
- 예매 취소 시 Reservation이 CANCELLED가 되고 Seat가 AVAILABLE로 복구된다.
- 요청 공연과 좌석의 공연이 다르면 예매 생성에 실패하고 좌석 상태가 바뀌지 않는다.

실행 결과:

```text
./gradlew test

BUILD SUCCESSFUL
```

## 8. 다음 단계

7단계에서는 동시성 문제 재현 테스트를 작성한다.

목표:

- 같은 좌석에 여러 사용자가 동시에 예매 요청을 보내는 테스트 작성
- 단순 트랜잭션 방식에서 발생할 수 있는 경쟁 조건 확인
- 성공 예매 수와 실패 예매 수 측정
- 8단계 락 적용 전후 비교 기준 마련
