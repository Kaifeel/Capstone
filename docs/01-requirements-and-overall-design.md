# 1단계. 요구사항 정리 및 전체 설계

## 1. 프로젝트 개요

### 프로젝트명

대규모 트래픽 대응 콘서트 티켓 예매 백엔드 시스템

### 목표

본 프로젝트는 단순 CRUD 기반 예매 서비스가 아니라, 실제 콘서트 티켓팅 상황처럼 짧은 시간에 많은 사용자가 동시에 접속하고 예매를 시도하는 환경을 가정한다.

핵심 목표는 다음과 같다.

- 동일 좌석에 대한 중복 예매를 방지한다.
- 예매 시작 직후 폭증하는 요청을 대기열로 제어한다.
- 사용자 입장 순서를 Redis 기반 대기열로 공정하게 관리한다.
- 결제 전 좌석을 일정 시간 HOLD 상태로 유지하고, 결제 실패 또는 시간 초과 시 자동으로 복구한다.
- 동시성 제어 방식별 차이를 테스트와 문서로 비교한다.
- 부하 테스트와 모니터링을 통해 시스템의 안정성과 성능을 확인한다.

## 2. 기술 스택

### 필수 기술

| 영역 | 기술 |
| --- | --- |
| Language | Java 17 (9월까진가 11월인가 서비스 종료한다는 소리가 있어서 이건 찾아봐야 java 25) | temurin 25버전 추천 (무료) 
| Framework | Spring Boot 3.x |
| Persistence | Spring Data JPA |
| Database | PostgreSQL |
| Cache / Queue | Redis |
| Build | Gradle |
| Test | JUnit5 |
| Infra | Docker Compose |
| Auth | JWT |

### 확장 기술

| 영역 | 후보 기술 | 적용 목적 |
| --- | --- | --- |
| Message Broker | Kafka 또는 RabbitMQ | 예매/결제/만료 이벤트 비동기 처리 |
| Monitoring | Spring Actuator, Prometheus, Grafana | API 응답 시간, 에러율, DB/Redis 상태, 대기열 길이 관측 |
| Load Test | k6 또는 JMeter | 동시 접속 및 예매 부하 테스트 |

### 권장 선택

캡스톤 프로젝트 구현 난이도와 설명 가능성을 고려하면 다음 순서가 적절하다.

1. Spring Boot, PostgreSQL, Redis, Docker Compose를 먼저 안정화한다.
2. 동시성 제어는 단순 트랜잭션, 비관적 락, 낙관적 락, Redis 분산락 순서로 확장한다.
3. 이벤트 처리는 초기에 Spring ApplicationEvent 또는 스케줄러로 구현하고, 이후 RabbitMQ 또는 Kafka로 분리한다.
4. 모니터링은 Actuator, Prometheus, Grafana 순서로 붙인다.

## 3. 사용자 역할

### 비회원

- 회원가입을 할 수 있다.
- 로그인을 할 수 있다.
- 공개 공연 목록을 조회할 수 있다.

### 일반 사용자

- JWT 인증 후 서비스를 이용한다.
- 공연 목록과 상세 정보를 조회한다.
- 공연별 좌석 상태를 조회한다.
- 예매 대기열에 진입한다.
- 대기열 통과 후 좌석 예매를 요청한다.
- 본인의 예매 내역을 조회한다.
- 본인의 예매를 취소할 수 있다.
- Mock 결제를 요청할 수 있다.

### 관리자

- 공연을 등록, 수정, 삭제할 수 있다.
- 공연별 좌석을 생성할 수 있다.
- 예매 현황과 운영 지표를 조회할 수 있다.

## 4. 핵심 도메인

### User

회원 정보를 관리한다.

주요 속성:

- id
- email
- password
- name
- role
- createdAt
- updatedAt

주요 정책:

- email은 중복될 수 없다.
- password는 해시된 값으로 저장한다.
- role은 USER, ADMIN으로 구분한다.

### Concert

공연 정보를 관리한다.

주요 속성:

- id
- title
- venue
- concertDateTime
- reservationStartAt
- reservationEndAt
- totalSeatCount
- createdAt
- updatedAt

주요 정책:

- 예매 가능 시간은 reservationStartAt부터 reservationEndAt까지다.
- 공연 삭제 시 이미 예매된 좌석이 있으면 삭제를 제한하거나 소프트 삭제를 고려한다.

### Seat

공연별 좌석 정보를 관리한다.

주요 속성:

- id
- concertId
- section
- row
- number
- price
- status
- version
- holdExpiresAt
- createdAt
- updatedAt

주요 정책:

- 같은 공연 안에서 section, row, number 조합은 중복될 수 없다.
- 좌석 상태는 AVAILABLE, HOLD, RESERVED, CANCELLED로 관리한다.
- HOLD 상태는 결제 대기 상태이며, 제한 시간이 지나면 AVAILABLE로 복구된다.
- 동시성 제어 비교를 위해 version 필드를 둔다.

### Reservation

예매 정보를 관리한다.

주요 속성:

- id
- userId
- concertId
- seatId
- status
- expiresAt
- reservedAt
- cancelledAt
- createdAt
- updatedAt

주요 정책:

- 같은 좌석은 동시에 하나의 활성 예매만 가질 수 있다.
- 예매 생성 직후 상태는 PENDING 또는 HOLD로 둔다.
- 결제 성공 시 CONFIRMED로 변경한다.
- 결제 실패, 시간 초과, 취소 시 CANCELLED 또는 EXPIRED로 변경한다.

### Payment

Mock 결제 정보를 관리한다.

주요 속성:

- id
- reservationId
- amount
- status
- requestedAt
- approvedAt
- failedAt
- createdAt
- updatedAt

주요 정책:

- 실제 외부 결제 연동은 하지 않는다.
- 결제 상태는 PENDING, SUCCESS, FAILED, TIMEOUT으로 관리한다.
- 결제 성공 시 좌석은 RESERVED, 예매는 CONFIRMED가 된다.
- 결제 실패 또는 시간 초과 시 좌석은 AVAILABLE로 복구된다.

### WaitingToken

Redis 기반 대기열 토큰이다. DB 엔티티가 아니라 Redis 자료구조 중심으로 관리한다.

주요 속성:

- token
- userId
- concertId
- issuedAt
- rank
- status
- expiresAt

주요 정책:

- Redis Sorted Set의 score는 진입 시간 또는 증가 시퀀스로 사용한다.
- 사용자는 자신의 대기 순번을 조회할 수 있다.
- 서버는 일정 인원만 입장 가능 상태로 전환한다.
- 입장 토큰에는 TTL을 적용한다.
- 입장 검증을 통과한 사용자만 실제 예매 API를 호출할 수 있다.

## 5. 주요 기능 요구사항

### 회원 및 인증

- 사용자는 이메일, 비밀번호, 이름으로 회원가입한다.
- 사용자는 이메일과 비밀번호로 로그인한다.
- 로그인 성공 시 JWT Access Token을 발급한다.
- 인증이 필요한 API는 JWT를 검증한다.
- 관리자 API는 ADMIN 권한만 접근할 수 있다.

### 공연 관리

- 관리자는 공연을 등록할 수 있다.
- 관리자는 공연 정보를 수정할 수 있다.
- 관리자는 공연을 삭제할 수 있다.
- 사용자는 공연 목록을 조회할 수 있다.
- 사용자는 공연 상세 정보를 조회할 수 있다.

### 좌석 관리

- 관리자는 공연별 좌석을 생성할 수 있다.
- 좌석은 구역, 열, 번호, 가격, 상태를 가진다.
- 사용자는 공연별 좌석 목록을 조회할 수 있다.
- 사용자는 좌석별 현재 상태를 확인할 수 있다.

### 예매

- 사용자는 대기열 통과 후 좌석 예매를 요청한다.
- 예매 가능한 시간 안에서만 예매할 수 있다.
- AVAILABLE 상태의 좌석만 예매할 수 있다.
- 예매 요청 성공 시 좌석은 HOLD 상태가 된다.
- 예매 요청 성공 시 Reservation이 생성된다.
- HOLD 제한 시간 안에 결제가 완료되지 않으면 예매는 만료된다.
- 예매 만료 시 좌석은 AVAILABLE 상태로 복구된다.
- 사용자는 본인의 예매 내역을 조회할 수 있다.
- 사용자는 본인의 예매를 취소할 수 있다.

### 결제

- 결제는 Mock API로 처리한다.
- 결제 요청 시 Payment가 PENDING 상태로 생성된다.
- 결제 성공 시 Payment는 SUCCESS, Reservation은 CONFIRMED, Seat는 RESERVED가 된다.
- 결제 실패 시 Payment는 FAILED, Reservation은 CANCELLED, Seat는 AVAILABLE이 된다.
- 결제 제한 시간 초과 시 Reservation은 EXPIRED, Seat는 AVAILABLE이 된다.

### 동시성 제어

동시성 제어는 단계별로 구현한다.

1. 단순 트랜잭션
   - 기본 JPA 조회 후 상태 변경
   - 동시 요청에서 중복 예매 문제가 발생할 수 있음을 테스트로 재현

2. 비관적 락
   - `SELECT FOR UPDATE` 기반으로 좌석 행을 잠근다.
   - 동일 좌석 요청은 직렬화된다.
   - 정확성은 높지만 대기 시간이 증가할 수 있다.

3. 낙관적 락
   - Seat의 `version` 필드를 사용한다.
   - 충돌 발생 시 OptimisticLockException을 처리한다.
   - 읽기 비중이 높고 충돌이 낮은 환경에 적합하다.

4. Redis 분산락
   - 좌석 단위 lock key를 사용한다.
   - 여러 애플리케이션 인스턴스 환경에서도 락을 공유할 수 있다.
   - TTL, 락 해제 안정성, 장애 상황을 함께 고려해야 한다.

### 대기열

- 사용자가 예매 페이지 진입 시 Redis Sorted Set에 등록한다.
- 토큰은 UUID 또는 서명된 문자열로 발급한다.
- 사용자는 자신의 현재 순번을 조회할 수 있다.
- 서버는 N명씩 입장 가능 상태로 이동시킨다.
- 입장 가능 토큰은 Redis에 별도 key로 저장하고 TTL을 부여한다.
- 예매 API는 대기열 검증을 통과한 요청만 처리한다.

### 비동기 이벤트

async 꼭 공부하는 것을 추천

초기 구현에서는 트랜잭션과 스케줄러 중심으로 단순화하고, 확장 단계에서 메시지 브로커를 적용한다.

후보 이벤트:

- ReservationCreatedEvent
- PaymentRequestedEvent
- PaymentSucceededEvent
- PaymentFailedEvent
- ReservationExpiredEvent
- ReservationCancelledEvent

이벤트 분리 목적:

- API 응답 경로를 짧게 유지한다.
- 결제, 만료, 알림 같은 후속 처리를 분리한다.
- 장애 발생 시 재처리 구조를 만들 수 있다.

## 6. 비기능 요구사항

### 정합성

- 동일 좌석은 동시에 하나의 활성 예매만 가져야 한다.
- 결제 성공 후 좌석 상태와 예매 상태는 불일치하면 안 된다.
- 예매 만료와 결제 성공이 경합하는 경우 최종 상태가 일관되어야 한다.

### 성능

- 대기열 적용 전후의 응답 시간과 실패율을 비교할 수 있어야 한다.
- 100명, 500명, 1000명 동시 요청 시나리오를 준비한다.
- 같은 좌석 집중 요청과 여러 좌석 분산 요청을 모두 테스트한다.

### 확장성

- 애플리케이션 서버를 여러 대로 늘려도 Redis 대기열과 분산락이 동작해야 한다.
- DB는 좌석 예매 정합성의 최종 기준이 된다.
- Redis는 대기열, 토큰, 임시 락, 캐시 용도로 사용한다.

### 관측 가능성

- API 응답 시간, 에러율, TPS를 확인할 수 있어야 한다.
- DB 커넥션 상태를 확인할 수 있어야 한다.
- Redis 연결 상태와 대기열 길이를 확인할 수 있어야 한다.
- 예매 성공/실패/만료 건수를 확인할 수 있어야 한다.

### 보안

- 비밀번호는 평문 저장하지 않는다.
- JWT 서명 키는 환경 변수로 관리한다.
- 관리자 API는 권한 검사를 적용한다.
- 사용자는 본인 예매만 조회하거나 취소할 수 있다.

## 7. 전체 아키텍처 방향

### 기본 구조

```text
Client
  |
  v
Spring Boot API Server
  |
  |-- PostgreSQL: 회원, 공연, 좌석, 예매, 결제 영속 데이터
  |
  |-- Redis: 대기열, 입장 토큰, 분산락, 임시 상태
  |
  |-- Message Broker: 예매/결제/만료 이벤트 처리
  |
  |-- Actuator: 애플리케이션 메트릭 노출
       |
       v
Prometheus -> Grafana
```

### 계층 구조

```text
controller
service
repository
domain
dto
config
exception
security
event
infra
```

### 패키지 설계 방향

```text
com.example.ticketing
  auth
  user
  concert
  seat
  reservation
  payment
  waitingroom
  common
  config
  security
  event
  monitoring
```

도메인 단위 패키지를 우선 사용하고, 각 패키지 안에 controller, service, repository, dto를 배치하는 구조를 권장한다. 기능이 커질수록 도메인별 응집도가 높아지고, 캡스톤 발표에서도 구조 설명이 쉽다.

## 8. 핵심 상태 모델

### SeatStatus

| 상태 | 의미 |
| --- | --- |
| AVAILABLE | 예매 가능 |
| HOLD | 결제 대기 중 임시 점유 |
| RESERVED | 결제 완료 후 최종 예매 |
| CANCELLED | 취소된 좌석 상태 또는 관리상 비활성 상태 |

### ReservationStatus

| 상태 | 의미 |
| --- | --- |
| PENDING | 예매 생성 후 결제 대기 |
| CONFIRMED | 결제 성공으로 예매 확정 |
| CANCELLED | 사용자 취소 또는 결제 실패로 취소 |
| EXPIRED | 결제 제한 시간 초과 |

### PaymentStatus

| 상태 | 의미 |
| --- | --- |
| PENDING | 결제 요청 생성 |
| SUCCESS | 결제 성공 |
| FAILED | 결제 실패 |
| TIMEOUT | 결제 시간 초과 |

### WaitingTokenStatus

| 상태 | 의미 |
| --- | --- |
| WAITING | 대기 중 |
| ALLOWED | 입장 가능 |
| EXPIRED | 토큰 만료 |
| USED | 예매 API 검증에 사용됨 |

## 9. 주요 유스케이스

### 유스케이스 1. 사용자가 좌석 예매에 성공한다

1. 사용자가 로그인한다.
2. 공연 상세와 좌석 목록을 조회한다.
3. 사용자가 대기열에 진입한다.
4. Redis가 대기열 토큰을 발급한다.
5. 사용자가 대기 순번을 조회한다.
6. 서버가 일정 수의 사용자를 입장 가능 상태로 전환한다.
7. 사용자가 입장 토큰 검증을 통과한다.
8. 사용자가 좌석 예매 API를 호출한다.
9. 서버가 좌석 상태와 예매 가능 시간을 검증한다.
10. 서버가 좌석을 HOLD 상태로 변경한다.
11. 서버가 Reservation을 PENDING 상태로 생성한다.
12. 사용자가 Mock 결제를 요청한다.
13. 결제 성공 시 좌석은 RESERVED, 예매는 CONFIRMED가 된다.

### 유스케이스 2. 여러 사용자가 같은 좌석을 동시에 예매한다

1. 여러 사용자가 같은 seatId로 동시에 예매 요청을 보낸다.
2. 단순 트랜잭션 버전에서는 동시성 문제가 발생할 수 있음을 테스트로 확인한다.
3. 비관적 락 또는 낙관적 락 버전에서는 하나의 요청만 성공한다.
4. 나머지 요청은 이미 점유된 좌석이라는 오류를 받는다.
5. 테스트는 reservation 테이블에 해당 seatId의 활성 예매가 하나만 존재하는지 검증한다.

### 유스케이스 3. 결제 시간이 만료된다

1. 사용자가 좌석 예매에 성공해 좌석이 HOLD 상태가 된다.
2. Reservation은 PENDING 상태로 생성된다.
3. 사용자가 제한 시간 안에 결제하지 않는다.
4. 만료 처리 작업이 expiresAt이 지난 Reservation을 찾는다.
5. Reservation은 EXPIRED로 변경된다.
6. Seat는 AVAILABLE 상태로 복구된다.

### 유스케이스 4. 대기열로 서버 부하를 제어한다

1. 예매 시작 시 많은 사용자가 대기열 진입 API를 호출한다.
2. Redis Sorted Set에 사용자 토큰이 순서대로 저장된다.
3. 서버는 일정 주기 또는 관리자 설정에 따라 상위 N명을 입장 가능 상태로 이동시킨다.
4. 입장 가능 사용자는 예매 API를 호출할 수 있다.
5. 아직 대기 중인 사용자는 순번만 조회할 수 있다.
6. 부하 테스트에서 대기열 적용 전후 API 실패율과 응답 시간을 비교한다.

## 10. 데이터 정합성 기준

### 좌석 중복 예매 방지 기준

최종적으로 다음 조건을 만족해야 한다.

- 하나의 seatId에 대해 CONFIRMED 또는 PENDING 상태의 활성 Reservation은 최대 하나다.
- Seat가 RESERVED이면 해당 좌석에 CONFIRMED Reservation이 존재해야 한다.
- Seat가 HOLD이면 해당 좌석에 PENDING Reservation이 존재해야 한다.
- Reservation이 EXPIRED 또는 CANCELLED이면 Seat는 AVAILABLE 또는 CANCELLED로 복구되어야 한다.

### DB 제약 조건 후보

- users.email unique
- seats.concert_id, section, row, number unique
- reservations.seat_id active unique 제약은 PostgreSQL partial unique index로 고려
- payments.reservation_id unique

PostgreSQL partial unique index 예시:

```sql
CREATE UNIQUE INDEX uq_active_reservation_seat
ON reservations (seat_id)
WHERE status IN ('PENDING', 'CONFIRMED');
```

이 제약은 애플리케이션 락 구현과 별개로 DB 레벨의 마지막 방어선 역할을 한다.

## 11. API 범위

### 인증

- POST `/api/v1/auth/signup`
- POST `/api/v1/auth/login`

### 공연

- POST `/api/v1/concerts`
- GET `/api/v1/concerts`
- GET `/api/v1/concerts/{concertId}`
- PATCH `/api/v1/concerts/{concertId}`
- DELETE `/api/v1/concerts/{concertId}`

### 좌석

- POST `/api/v1/concerts/{concertId}/seats`
- GET `/api/v1/concerts/{concertId}/seats`
- GET `/api/v1/seats/{seatId}`

### 대기열

- POST `/api/v1/waiting-room/enter`
- GET `/api/v1/waiting-room/status`
- POST `/api/v1/waiting-room/validate`

### 예매

- POST `/api/v1/reservations`
- GET `/api/v1/reservations/{reservationId}`
- GET `/api/v1/reservations/me`
- POST `/api/v1/reservations/{reservationId}/cancel`

### 결제

- POST `/api/v1/payments/mock`

### 관리자 지표

- GET `/api/v1/admin/metrics/reservations`

## 12. 구현 단계별 범위

### 1단계. 요구사항 정리

- 프로젝트 목표 정리
- 기능 요구사항 정리
- 비기능 요구사항 정리
- 핵심 도메인과 상태 모델 정의
- 전체 아키텍처 방향 수립

### 2단계. ERD 설계

- users, concerts, seats, reservations, payments 테이블 설계
- Redis key 설계
- 인덱스와 unique 제약 정의

### 3단계. API 명세 작성

- 요청/응답 DTO 정의
- 인증 필요 여부 정의
- 권한 정의
- 에러 응답 정의

### 4단계. Spring Boot 프로젝트 구조 설계

- Gradle 프로젝트 생성
- 패키지 구조 생성
- 공통 응답, 예외, 보안 기본 구조 설계

### 5단계. 기본 공연/좌석/예매 CRUD 구현

- 공연 CRUD
- 좌석 생성 및 조회
- 예매 생성 기본 흐름
- 본인 예매 조회

### 6단계. 좌석 예매 트랜잭션 구현

- 예매 가능 시간 검증
- 좌석 상태 검증
- 좌석 HOLD 처리
- Reservation PENDING 생성

### 7단계. 동시성 문제 재현 테스트 작성

- 같은 좌석에 대해 다중 스레드 예매 요청 테스트
- 중복 예매 발생 여부 확인
- 실패 케이스를 의도적으로 기록

### 8단계. 락을 이용한 중복 예매 방지 구현

- 비관적 락 버전 구현
- 낙관적 락 버전 구현
- Redis 분산락 버전 구현
- 방식별 테스트와 비교 문서 작성

### 9단계. Redis 대기열 구현

- Sorted Set 기반 대기열 진입
- 순번 조회
- 입장 가능 토큰 발급
- 예매 API 앞단 검증
- TTL 처리

### 10단계. Mock 결제 및 예매 만료 처리 구현

- Payment PENDING 생성
- Mock 결제 성공/실패 처리
- 예약 만료 스케줄러 또는 이벤트 처리
- 좌석 상태 복구

### 11단계. 부하 테스트 및 모니터링 구성

- k6 또는 JMeter 시나리오 작성
- Actuator 적용
- Prometheus 설정
- Grafana 대시보드 구성
- 대기열 적용 전후 결과 비교

### 12단계. README와 발표 자료 정리

- 실행 방법
- 아키텍처 설명
- ERD
- API 명세
- 동시성 제어 비교
- 부하 테스트 결과
- 모니터링 화면
- 캡스톤 발표용 핵심 시나리오

## 13. 구현 우선순위

### MVP

캡스톤 중간 점검까지 반드시 완성해야 하는 범위다.

- 회원가입, 로그인, JWT 인증
- 공연 등록 및 조회
- 좌석 생성 및 조회
- 기본 예매 생성
- 좌석 HOLD 처리
- 예매 내역 조회
- Mock 결제 성공 처리
- Docker Compose로 PostgreSQL, Redis 실행

### 핵심 차별화 기능

프로젝트의 주제를 드러내는 핵심 범위다.

- 동시성 문제 재현 테스트
- 비관적 락 또는 낙관적 락 적용
- Redis 대기열
- 결제 만료 처리
- 부하 테스트 시나리오

### 고도화 기능

시간이 허용될 때 추가한다.

- Redis 분산락
- Kafka 또는 RabbitMQ 이벤트 처리
- Prometheus/Grafana 모니터링
- Grafana 대시보드 JSON
- 운영 지표 API

## 14. 테스트 전략

### 단위 테스트

- 도메인 상태 변경 로직
- 예매 가능 시간 검증
- 좌석 상태 전이 검증
- 결제 성공/실패 처리

### 통합 테스트

- 회원가입/로그인 흐름
- 공연/좌석/예매 API 흐름
- 예매 생성 후 결제 성공 흐름
- 예매 만료 후 좌석 복구 흐름

### 동시성 테스트

- 동일 좌석에 대한 100개 동시 요청
- 성공 예매 수가 1개인지 검증
- 실패 요청이 명확한 예외로 반환되는지 검증
- 락 방식별 평균 수행 시간 비교

### 부하 테스트

- 100명 동시 요청
- 500명 동시 요청
- 1000명 동시 요청
- 같은 좌석 집중 예매
- 같은 공연의 여러 좌석 분산 예매
- 대기열 적용 전후 비교

## 15. 성공 기준

### 기능 성공 기준

- 사용자는 회원가입, 로그인, 공연 조회, 좌석 조회, 예매, 결제, 취소를 수행할 수 있다.
- 관리자는 공연과 좌석을 등록할 수 있다.
- 사용자는 본인의 예매만 조회할 수 있다.
- 대기열을 통과하지 않은 사용자는 예매 API를 사용할 수 없다.

### 정합성 성공 기준

- 같은 좌석에 대해 동시에 여러 요청이 들어와도 활성 예매는 하나만 생성된다.
- 결제 실패 또는 만료 시 좌석은 AVAILABLE로 복구된다.
- 결제 성공 후 좌석은 RESERVED 상태로 유지된다.

### 성능 및 관측 성공 기준

- 부하 테스트 결과에서 성공률, 실패율, 평균 응답 시간, TPS를 확인할 수 있다.
- 대기열 적용 전후 차이를 수치로 설명할 수 있다.
- Prometheus/Grafana 또는 Actuator 지표로 시스템 상태를 확인할 수 있다.

## 16. 캡스톤 발표 핵심 포인트

발표에서는 단순 기능 나열보다 다음 흐름을 중심으로 설명한다.

1. 실제 티켓팅 문제 정의
   - 짧은 시간에 많은 사용자가 몰림
   - 동일 좌석 중복 예매 위험
   - 서버 과부하 위험
   - 결제 전 임시 점유와 만료 처리 필요

2. 해결 전략
   - DB 트랜잭션과 락으로 좌석 정합성 보장
   - Redis 대기열로 트래픽 진입 제어
   - HOLD/RESERVED 상태 모델로 결제 흐름 분리
   - 테스트와 부하 측정으로 결과 검증

3. 실험 결과
   - 단순 트랜잭션에서 발생 가능한 동시성 문제
   - 락 적용 후 중복 예매 방지 결과
   - 대기열 적용 전후 응답 시간과 실패율 비교
   - 모니터링 지표를 통한 병목 확인

4. 확장 가능성
   - 서버 다중 인스턴스
   - Redis 분산락
   - Kafka/RabbitMQ 기반 이벤트 처리
   - Prometheus/Grafana 기반 운영 관측

## 17. 다음 단계

2단계에서는 ERD를 설계한다.

다음 내용을 구체화한다.

- 테이블 목록
- 컬럼 정의
- 연관관계
- 상태 enum
- 인덱스
- unique 제약
- PostgreSQL partial unique index
- Redis key 구조
- Mermaid ERD
