# 5단계. 기본 공연/좌석/예매 CRUD 구현

## 1. 구현 범위

이번 단계에서는 Spring Boot 프로젝트 골격과 기본 CRUD 흐름을 구현했다.

구현한 범위:

- Gradle 기반 Spring Boot 프로젝트 생성
- PostgreSQL, Redis Docker Compose 설정
- 공통 API 응답 구조
- 공통 예외 처리 구조
- JWT 인증 기반 구조
- 회원가입/로그인 API
- 공연 등록, 조회, 수정, 삭제 API
- 좌석 생성, 조회 API
- 기본 예매 생성, 조회, 내 예매 목록, 취소 API

아직 구현하지 않은 범위:

- 결제 API
- 예매 만료 스케줄러
- 동시성 문제 재현 테스트
- 비관적 락/낙관적 락 적용 버전
- Redis 대기열
- Redis 분산락
- Prometheus/Grafana 모니터링

## 2. 생성된 주요 파일

### 프로젝트 설정

```text
settings.gradle
build.gradle
docker-compose.yml
src/main/resources/application.yml
src/main/resources/application-local.yml
src/main/resources/application-test.yml
```

### 공통 구조

```text
common/response/ApiResponse.java
common/response/PageResponse.java
common/exception/ErrorCode.java
common/exception/BusinessException.java
common/exception/GlobalExceptionHandler.java
common/entity/BaseEntity.java
```

### Security

```text
config/SecurityConfig.java
security/CustomUserDetails.java
security/CustomUserDetailsService.java
security/jwt/JwtProperties.java
security/jwt/JwtTokenProvider.java
security/filter/JwtAuthenticationFilter.java
security/handler/JwtAuthenticationEntryPoint.java
security/handler/JwtAccessDeniedHandler.java
```

### Auth/User

```text
auth/controller/AuthController.java
auth/service/AuthService.java
auth/dto/*
user/domain/User.java
user/domain/UserRole.java
user/repository/UserRepository.java
user/service/UserService.java
```

### Concert

```text
concert/controller/ConcertController.java
concert/domain/Concert.java
concert/repository/ConcertRepository.java
concert/service/ConcertService.java
concert/dto/*
```

### Seat

```text
seat/controller/SeatController.java
seat/domain/Seat.java
seat/domain/SeatStatus.java
seat/repository/SeatRepository.java
seat/service/SeatService.java
seat/dto/*
```

### Reservation

```text
reservation/controller/ReservationController.java
reservation/domain/Reservation.java
reservation/domain/ReservationStatus.java
reservation/repository/ReservationRepository.java
reservation/service/ReservationService.java
reservation/dto/*
```

## 3. 현재 예매 처리 흐름

현재 예매 생성은 단순 트랜잭션 방식이다.

처리 순서:

1. JWT 인증 사용자 확인
2. 공연 조회
3. 공연 예매 가능 시간 검증
4. 좌석 조회
5. 요청 공연과 좌석의 공연이 같은지 검증
6. 같은 좌석의 활성 예매 존재 여부 확인
7. 같은 사용자의 같은 좌석 활성 예매 존재 여부 확인
8. 좌석을 `HOLD` 상태로 변경
9. `PENDING` 예매 생성
10. 결제 만료 시간 `expiresAt` 저장

현재 방식은 동시 요청에서 경쟁 조건이 발생할 수 있다. 이 문제는 7단계에서 테스트로 재현하고, 8단계에서 락을 적용해 해결한다.

## 4. 현재 API

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

### Reservation

```text
POST /api/v1/reservations
GET  /api/v1/reservations/{reservationId}
GET  /api/v1/reservations/me
POST /api/v1/reservations/{reservationId}/cancel
```

## 5. 실행 준비

PostgreSQL과 Redis는 Docker Compose로 실행한다.

```bash
docker compose up -d
```

애플리케이션 실행:

```bash
./gradlew bootRun
```

현재 작업 환경에는 Java와 Gradle 명령이 설치되어 있지 않아 컴파일 검증은 아직 수행하지 못했다. JDK 17과 Gradle Wrapper 구성이 완료되면 `./gradlew test`로 검증한다.

## 6. 다음 단계

6단계에서는 좌석 예매 트랜잭션 구현을 더 명확히 다듬는다.

구체적으로 다음을 보강한다.

- 예매 생성 트랜잭션 경계 문서화
- 좌석 상태 전이 테스트
- 예매 생성 성공/실패 테스트
- DB unique 제약 또는 Flyway 마이그레이션 검토
- 결제 전 HOLD 만료 시간 정책 정리
