# 4단계. Spring Boot 프로젝트 구조 설계

## 1. 설계 목표

이 단계의 목표는 실제 구현에 들어가기 전에 Spring Boot 프로젝트의 기본 구조와 책임을 명확히 정하는 것이다.

핵심 원칙은 다음과 같다.

- 도메인 중심 패키지 구조를 사용한다.
- Controller, Service, Repository, Domain, DTO 책임을 분리한다.
- 공통 응답과 예외 처리를 일관되게 사용한다.
- JWT 인증과 권한 검사를 Security 계층에서 처리한다.
- 동시성 제어, Redis 대기열, 결제 만료 처리를 확장 가능한 구조로 분리한다.
- 테스트 코드는 기능별, 계층별로 작성할 수 있게 구조화한다.

## 2. 프로젝트 기본 정보

| 항목 | 값 |
| --- | --- |
| Group | `com.example` |
| Artifact | `ticketing` |
| Package | `com.example.ticketing` |
| Java | 17 |
| Spring Boot | 3.x |
| Build Tool | Gradle |
| Database | PostgreSQL |
| Cache | Redis |
| Test | JUnit5 |

## 3. Gradle 의존성 설계

### 필수 의존성

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

    runtimeOnly 'org.postgresql:postgresql'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:testcontainers'
}
```

### 확장 의존성

RabbitMQ를 선택하는 경우:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-amqp'
```

Kafka를 선택하는 경우:

```gradle
implementation 'org.springframework.kafka:spring-kafka'
testImplementation 'org.springframework.kafka:spring-kafka-test'
```

Prometheus 연동:

```gradle
runtimeOnly 'io.micrometer:micrometer-registry-prometheus'
```

### 선택 기준

- 초기 구현에서는 Kafka/RabbitMQ 없이 동기 Service와 Scheduler로 시작한다.
- 이벤트 구조를 먼저 인터페이스로 열어두고, 10단계 이후 메시지 브로커로 확장한다.
- 모니터링은 Actuator를 먼저 붙이고, 11단계에서 Prometheus/Grafana를 추가한다.

## 4. 전체 디렉터리 구조

```text
ticketing/
  build.gradle
  settings.gradle
  docker-compose.yml
  README.md
  docs/
    01-requirements-and-overall-design.md
    02-erd-design.md
    03-api-specification.md
    04-spring-boot-project-structure.md
  src/
    main/
      java/
        com/example/ticketing/
          TicketingApplication.java
          auth/
          user/
          concert/
          seat/
          reservation/
          payment/
          waitingroom/
          common/
          config/
          security/
          event/
          monitoring/
      resources/
        application.yml
        application-local.yml
        application-test.yml
    test/
      java/
        com/example/ticketing/
        다 있는 것은 아님
          auth/
          concert/
          seat/
          reservation/
          payment/
          waitingroom/
          support/
```

## 5. 패키지 구조

도메인 중심 패키지 구조를 사용한다. 기능별 응집도가 높고, 캡스톤 발표에서 구조 설명이 쉽다.

```text
com.example.ticketing
  auth
    controller
    dto
    service
  user
    domain
    repository
    service
    dto
  concert
    controller
    domain
    repository
    service
    dto
  seat
    controller
    domain
    repository
    service
    dto
  reservation
    controller
    domain
    repository
    service
    dto
    lock
    scheduler
  payment
    controller
    domain
    repository
    service
    dto
  waitingroom
    controller
    service
    dto
    redis
  common
    response
    exception
    entity
    util
  config
  security
    jwt
    filter
    handler
  event
    reservation
    payment
  monitoring
```

## 6. 계층별 책임

### Controller

역할:

- HTTP 요청과 응답을 담당한다.
- Request DTO validation을 수행한다.
- 인증 사용자 정보를 Service에 전달한다.
- 비즈니스 로직을 직접 처리하지 않는다.

예시:

```java
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {
}
```

### Service

역할:

- 비즈니스 유스케이스를 처리한다.
- 트랜잭션 경계를 관리한다.
- 도메인 상태 변경을 조율한다.
- Repository, Redis, Event Publisher를 조합한다.

예시:

```java
@Service
@RequiredArgsConstructor
public class ReservationService {
}
```

### Repository

역할:

- JPA 기반 DB 접근을 담당한다.
- 비관적 락, 조회 최적화, 상태별 조회 쿼리를 제공한다.

예시:

```java
public interface SeatRepository extends JpaRepository<Seat, Long> {
}
```

### Domain

역할:

- 엔티티 상태와 상태 전이 메서드를 가진다.
- 상태 변경 규칙을 엔티티 내부에 모은다.
- Controller 또는 DTO에 의존하지 않는다.

예시:

```java
public class Seat {
    public void hold(LocalDateTime expiresAt) {
    }
}
```

### DTO

역할:

- API 요청/응답 모델을 표현한다.
- Entity를 외부에 직접 노출하지 않는다.
- validation annotation을 가진다.

### Config

역할:

- Security, Redis, JPA, Jackson, Monitoring 설정을 관리한다.

### Common

역할:

- 공통 응답 형식
- 공통 예외
- 에러 코드
- BaseEntity
- 테스트와 운영에서 공통으로 쓰는 유틸리티

## 7. 공통 응답 구조

### ApiResponse

위치:

```text
common/response/ApiResponse.java
```

책임:

- 모든 API의 성공/실패 응답 형식을 통일한다.

형태:

```java
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", "요청이 성공했습니다.", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "OK", message, data);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
```

### PageResponse

위치:

```text
common/response/PageResponse.java
```

책임:

- Spring Page를 API 응답용 페이징 DTO로 변환한다.

## 8. 공통 예외 구조

### ErrorCode

위치:

```text
common/exception/ErrorCode.java
```

책임:

- 비즈니스 에러 코드와 HTTP 상태를 한 곳에서 관리한다.

예시:

```java
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값이 올바르지 않습니다."),
    CONCERT_NOT_FOUND(HttpStatus.NOT_FOUND, "CONCERT_NOT_FOUND", "공연을 찾을 수 없습니다."),
    SEAT_ALREADY_OCCUPIED(HttpStatus.CONFLICT, "SEAT_ALREADY_OCCUPIED", "이미 선택할 수 없는 좌석입니다.");
}
```

### BusinessException

위치:

```text
common/exception/BusinessException.java
```

책임:

- 비즈니스 예외의 기본 타입이다.
- ErrorCode를 포함한다.

### GlobalExceptionHandler

위치:

```text
common/exception/GlobalExceptionHandler.java
```

책임:

- validation 예외, 비즈니스 예외, 인증 예외, 알 수 없는 예외를 공통 응답으로 변환한다.

처리 대상:

- `BusinessException`
- `MethodArgumentNotValidException`
- `ConstraintViolationException`
- `AuthenticationException`
- `AccessDeniedException`
- `Exception`

## 9. 공통 Entity 구조

### BaseEntity

위치:

```text
common/entity/BaseEntity.java
```

책임:

- 생성 시간과 수정 시간을 공통 관리한다.

예시:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### JPA Auditing

위치:

```text
config/JpaAuditingConfig.java
```

책임:

- `@EnableJpaAuditing`을 활성화한다.

## 10. Security 구조

### SecurityConfig

위치:

```text
config/SecurityConfig.java
```

책임:

- 인증/인가 정책을 설정한다.
- JWT 필터를 등록한다.
- 세션을 사용하지 않는 stateless 구조로 설정한다.

정책:

```text
PUBLIC:
  POST /api/v1/auth/signup
  POST /api/v1/auth/login
  GET  /api/v1/concerts/**
  GET  /api/v1/seats/**

ADMIN:
  POST   /api/v1/concerts
  PATCH  /api/v1/concerts/**
  DELETE /api/v1/concerts/**
  POST   /api/v1/concerts/*/seats
  GET    /api/v1/admin/**

USER:
  /api/v1/waiting-room/**
  /api/v1/reservations/**
  /api/v1/payments/**
```

### JWT 구성 요소

```text
security/jwt/JwtTokenProvider.java
security/jwt/JwtAuthenticationToken.java
security/filter/JwtAuthenticationFilter.java
security/handler/JwtAccessDeniedHandler.java
security/handler/JwtAuthenticationEntryPoint.java
```

역할:

- `JwtTokenProvider`: 토큰 생성, 검증, subject 추출
- `JwtAuthenticationFilter`: 요청 헤더에서 JWT 추출 후 SecurityContext 설정
- `JwtAuthenticationEntryPoint`: 인증 실패 응답
- `JwtAccessDeniedHandler`: 권한 실패 응답

### 인증 사용자 객체

위치:

```text
security/CustomUserDetails.java
security/CustomUserDetailsService.java
```

책임:

- Spring Security에서 현재 로그인 사용자 ID, email, role을 사용할 수 있게 한다.

## 11. 도메인별 클래스 설계

### Auth

```text
auth/
  controller/AuthController.java
  service/AuthService.java
  dto/SignupRequest.java
  dto/SignupResponse.java
  dto/LoginRequest.java
  dto/LoginResponse.java
```

책임:

- 회원가입
- 로그인
- JWT 발급

### User

```text
user/
  domain/User.java
  domain/UserRole.java
  repository/UserRepository.java
  service/UserService.java
  dto/UserResponse.java
```

책임:

- 사용자 영속화
- 이메일 중복 검증
- 사용자 조회

### Concert

```text
concert/
  controller/ConcertController.java
  domain/Concert.java
  repository/ConcertRepository.java
  service/ConcertService.java
  dto/ConcertCreateRequest.java
  dto/ConcertUpdateRequest.java
  dto/ConcertResponse.java
  dto/ConcertDetailResponse.java
```

책임:

- 공연 등록, 수정, 삭제
- 공연 목록 조회
- 공연 상세 조회
- 예매 가능 기간 검증

### Seat

```text
seat/
  controller/SeatController.java
  domain/Seat.java
  domain/SeatStatus.java
  repository/SeatRepository.java
  service/SeatService.java
  dto/SeatBulkCreateRequest.java
  dto/SeatCreateRequest.java
  dto/SeatResponse.java
  dto/SeatListResponse.java
```

책임:

- 공연별 좌석 생성
- 좌석 상태 조회
- 좌석 상태 전이
- 비관적 락 조회 쿼리 제공

### Reservation

```text
reservation/
  controller/ReservationController.java
  domain/Reservation.java
  domain/ReservationStatus.java
  repository/ReservationRepository.java
  service/ReservationService.java
  service/ReservationExpirationService.java
  lock/SeatLockManager.java
  lock/NoLockSeatLockManager.java
  lock/PessimisticSeatLockManager.java
  lock/OptimisticReservationService.java
  lock/RedisSeatLockManager.java
  scheduler/ReservationExpirationScheduler.java
  dto/ReservationCreateRequest.java
  dto/ReservationCreateResponse.java
  dto/ReservationDetailResponse.java
  dto/ReservationSummaryResponse.java
  dto/ReservationCancelRequest.java
  dto/ReservationCancelResponse.java
```

책임:

- 예매 생성
- 예매 조회
- 예매 취소
- 예매 만료 처리
- 좌석 점유 동시성 제어

### Payment

```text
payment/
  controller/PaymentController.java
  domain/Payment.java
  domain/PaymentStatus.java
  repository/PaymentRepository.java
  service/PaymentService.java
  dto/MockPaymentRequest.java
  dto/MockPaymentResponse.java
```

책임:

- Mock 결제 생성
- 결제 성공 처리
- 결제 실패 처리
- 예매/좌석 상태 동기화

### Waiting Room

```text
waitingroom/
  controller/WaitingRoomController.java
  service/WaitingRoomService.java
  service/WaitingRoomAdmissionService.java
  redis/WaitingRoomRedisRepository.java
  dto/WaitingRoomEnterRequest.java
  dto/WaitingRoomEnterResponse.java
  dto/WaitingRoomStatusResponse.java
  dto/WaitingRoomValidateRequest.java
  dto/WaitingRoomValidateResponse.java
```

책임:

- Redis Sorted Set 기반 대기열 진입
- 순번 조회
- 입장 허용 처리
- entryToken 검증
- TTL 관리

### Event

```text
event/
  reservation/ReservationCreatedEvent.java
  reservation/ReservationExpiredEvent.java
  reservation/ReservationCancelledEvent.java
  payment/PaymentRequestedEvent.java
  payment/PaymentSucceededEvent.java
  payment/PaymentFailedEvent.java
  EventPublisher.java
```

책임:

- 초기에는 Spring ApplicationEvent 기반으로 처리한다.
- 이후 RabbitMQ 또는 Kafka로 구현체를 교체할 수 있게 한다.

### Monitoring

```text
monitoring/
  AdminMetricsController.java
  ReservationMetricsService.java
  WaitingRoomMetricsBinder.java
```

책임:

- 예매 상태별 집계
- 좌석 상태별 집계
- Redis 대기열 길이 집계
- Actuator/Micrometer custom metric 등록

## 12. 동시성 제어 구조

동시성 제어는 전략별로 비교할 수 있도록 코드 구조를 분리한다.

### 전략 1. 단순 트랜잭션

```text
ReservationService
```

특징:

- 일반 조회 후 상태 변경
- 동시성 문제 재현용 기준 구현
- 7단계 테스트에서 중복 예매 가능성을 확인한다.

### 전략 2. 비관적 락

```text
SeatRepository.findByIdForUpdate()
PessimisticReservationService
```

특징:

- DB row lock 사용
- 하나의 좌석에 대한 요청을 직렬화한다.
- 정확성은 높지만 대기 시간이 늘 수 있다.

### 전략 3. 낙관적 락

```text
Seat.version
OptimisticReservationService
```

특징:

- `@Version` 사용
- 충돌 발생 시 예외 처리
- 충돌이 적은 환경에 적합하다.

### 전략 4. Redis 분산락

```text
RedisSeatLockManager
RedisTemplate
```

특징:

- `lock:seat:{seatId}` key 사용
- 다중 서버 인스턴스 환경에서 락 공유 가능
- 락 TTL과 소유자 검증이 필요하다.

## 13. 설정 파일 구조

### application.yml

```yaml
spring:
  profiles:
    active: local

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

### application-local.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ticketing
    username: ticketing
    password: ticketing
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
    open-in-view: false
  data:
    redis:
      host: localhost
      port: 6379

jwt:
  secret: local-development-secret-key-must-be-long-enough
  access-token-expiration-seconds: 3600
```

### application-test.yml

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
    open-in-view: false
  data:
    redis:
      host: localhost
      port: 6379

jwt:
  secret: test-secret-key-must-be-long-enough
  access-token-expiration-seconds: 3600
```

## 14. Docker Compose 설계

초기 구성:

```text
PostgreSQL
Redis
```

확장 구성:

```text
PostgreSQL
Redis
RabbitMQ or Kafka
Prometheus
Grafana
```

초기 `docker-compose.yml` 목표:

- 로컬 개발용 PostgreSQL 실행
- 로컬 개발용 Redis 실행
- 데이터 볼륨 유지
- 포트는 표준 포트 사용

예상 포트:

| 서비스 | 포트 |
| --- | --- |
| Spring Boot | 8080 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Prometheus | 9090 |
| Grafana | 3000 |
| RabbitMQ Management | 15672 |
| Kafka | 9092 |

## 15. 테스트 구조

```text
src/test/java/com/example/ticketing/
  auth/
    AuthServiceTest.java
    AuthControllerTest.java
  concert/
    ConcertServiceTest.java
    ConcertControllerTest.java
  seat/
    SeatServiceTest.java
  reservation/
    ReservationServiceTest.java
    ReservationConcurrencyTest.java
    PessimisticReservationConcurrencyTest.java
    OptimisticReservationConcurrencyTest.java
    RedisLockReservationConcurrencyTest.java
    ReservationExpirationServiceTest.java
  payment/
    PaymentServiceTest.java
  waitingroom/
    WaitingRoomServiceTest.java
    WaitingRoomRedisRepositoryTest.java
  support/
    IntegrationTestSupport.java
    TestDataFactory.java
```

### 테스트 분류

| 테스트 | 목적 |
| --- | --- |
| 단위 테스트 | 도메인 상태 전이, Service 검증 |
| WebMvc 테스트 | Controller 요청/응답 검증 |
| 통합 테스트 | DB, Redis 연동 검증 |
| 동시성 테스트 | 좌석 중복 예매 방지 검증 |
| 부하 테스트 | k6/JMeter 기반 외부 시나리오 |

### Testcontainers 적용

PostgreSQL은 Testcontainers 사용을 권장한다.

Redis는 다음 중 하나를 선택한다.

- Docker Compose 기반 로컬 Redis 사용
- Testcontainers Redis 호환 이미지 사용
- embedded redis는 유지보수 이슈가 있어 우선순위를 낮춘다.

## 16. 구현 우선순위

### 1차 구현

```text
common
user
auth
concert
seat
reservation 기본 구현
payment 기본 구현
```

목표:

- 회원가입/로그인
- 공연 등록/조회
- 좌석 생성/조회
- 기본 예매 생성
- Mock 결제 성공/실패

### 2차 구현

```text
reservation 동시성 테스트
reservation lock 전략
reservation 만료 처리
```

목표:

- 동시성 문제 재현
- 비관적 락 적용
- 낙관적 락 적용
- 만료 예매 복구

### 3차 구현

```text
waitingroom
redis lock
monitoring
load-test
```

목표:

- Redis 대기열
- Redis 분산락
- Actuator/Prometheus/Grafana
- k6/JMeter 시나리오

## 17. 코드 작성 규칙

### Entity

- 기본 생성자는 protected로 둔다.
- 상태 변경은 의미 있는 메서드로 표현한다.
- Setter는 최소화한다.
- 연관관계는 우선 단방향으로 둔다.

### DTO

- Request DTO에는 validation annotation을 둔다.
- Response DTO는 정적 팩토리 메서드로 Entity를 변환한다.
- Entity를 API 응답으로 직접 반환하지 않는다.

### Service

- public 메서드는 하나의 유스케이스를 표현한다.
- 트랜잭션은 Service 계층에 둔다.
- 조회 전용 메서드는 `@Transactional(readOnly = true)`를 사용한다.

### Repository

- 복잡한 조회는 명시적 `@Query`를 사용한다.
- 락이 필요한 쿼리는 메서드명과 annotation으로 의도를 드러낸다.

### Exception

- 비즈니스 실패는 `BusinessException`과 `ErrorCode`로 표현한다.
- Controller에서 try-catch를 반복하지 않는다.

## 18. 다음 단계

5단계에서는 기본 공연/좌석/예매 CRUD 구현을 시작한다.

구현 전에 먼저 실제 Spring Boot 프로젝트를 생성하고 다음 파일을 만든다.

- `build.gradle`
- `settings.gradle`
- `docker-compose.yml`
- `application.yml`
- `application-local.yml`
- `application-test.yml`
- 공통 응답/예외 클래스
- User/Auth 기본 클래스
- Concert/Seat/Reservation 기본 클래스
