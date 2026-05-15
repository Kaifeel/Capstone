# 9단계. Redis 대기열 구현

## 1. 목표

이번 단계의 목표는 예매 시작 직후 사용자가 몰리는 상황에서 모든 사용자가 바로 예매 API로 진입하지 않도록 Redis 기반 대기열을 구현하는 것이다.

구현한 기능:

- 대기열 진입
- 기존 대기열 토큰 재사용
- 대기 순번 조회
- 입장 가능 사용자 승격
- 입장 토큰 발급
- 예매 API 호출 시 entryToken 검증
- entryToken 1회 사용 처리

## 2. 구현 파일

### Controller

```text
src/main/java/com/example/ticketing/waitingroom/controller/WaitingRoomController.java
```

### Service

```text
src/main/java/com/example/ticketing/waitingroom/service/WaitingRoomService.java
```

### Redis Repository

```text
src/main/java/com/example/ticketing/waitingroom/redis/WaitingRoomRedisRepository.java
```

### DTO

```text
src/main/java/com/example/ticketing/waitingroom/dto/WaitingRoomEnterRequest.java
src/main/java/com/example/ticketing/waitingroom/dto/WaitingRoomEnterResponse.java
src/main/java/com/example/ticketing/waitingroom/dto/WaitingRoomStatusResponse.java
src/main/java/com/example/ticketing/waitingroom/dto/WaitingRoomValidateRequest.java
src/main/java/com/example/ticketing/waitingroom/dto/WaitingRoomValidateResponse.java
src/main/java/com/example/ticketing/waitingroom/dto/WaitingTokenStatus.java
```

## 3. API

### 대기열 진입

```http
POST /api/v1/waiting-room/enter
```

Request:

```json
{
  "concertId": 1
}
```

Response:

```json
{
  "success": true,
  "code": "OK",
  "message": "대기열에 진입했습니다.",
  "data": {
    "concertId": 1,
    "waitingToken": "1:uuid",
    "rank": 120,
    "estimatedWaitSeconds": 20,
    "status": "WAITING"
  }
}
```

### 대기열 상태 조회

```http
GET /api/v1/waiting-room/status?concertId=1&waitingToken=1:uuid
```

Response:

```json
{
  "success": true,
  "code": "OK",
  "message": "요청이 성공했습니다.",
  "data": {
    "concertId": 1,
    "waitingToken": "1:uuid",
    "rank": 10,
    "waitingCount": 2350,
    "allowed": false,
    "status": "WAITING",
    "estimatedWaitSeconds": 10,
    "expiresAt": null
  }
}
```

### 입장 토큰 검증

```http
POST /api/v1/waiting-room/validate
```

Request:

```json
{
  "concertId": 1,
  "waitingToken": "1:uuid"
}
```

Response:

```json
{
  "success": true,
  "code": "OK",
  "message": "입장 토큰이 검증되었습니다.",
  "data": {
    "concertId": 1,
    "entryToken": "entry-uuid",
    "expiresAt": "2026-05-07T16:58:00"
  }
}
```

### 입장 허용 처리

```http
POST /api/v1/waiting-room/admit?concertId=1&limit=100
```

권한:

```text
ADMIN
```

현재 구현에서는 운영 편의를 위해 관리자 API로 제공한다. 이후에는 스케줄러로 주기적으로 실행하거나, 운영 지표에 따라 동적으로 `limit`을 조절할 수 있다.

## 4. Redis Key 구조

### 공연별 대기열

```text
waiting:{concertId}:queue
```

Type:

```text
Sorted Set
```

Member:

```text
{userId}:{uuid}
```

Score:

```text
System.currentTimeMillis()
```

### 사용자 토큰 매핑

```text
waiting:{concertId}:user:{userId}
```

Type:

```text
String
```

TTL:

```text
6시간
```

역할:

- 같은 사용자가 같은 공연 대기열에 다시 진입하면 기존 토큰을 반환한다.

### 입장 허용 토큰

```text
waiting:{concertId}:allowed:{waitingToken}
```

Type:

```text
String
```

Value:

```text
userId
```

TTL:

```text
5분
```

역할:

- 대기열에서 입장 가능 상태가 된 사용자를 표시한다.

### Entry Token

```text
waiting:{concertId}:entry:{entryToken}
```

Type:

```text
String
```

Value:

```text
userId
```

TTL:

```text
5분
```

역할:

- 실제 예매 API 호출 시 검증한다.
- 검증 성공 후 삭제되므로 1회성 토큰으로 동작한다.

### 사용 완료 Entry Token

```text
waiting:{concertId}:used:{entryToken}
```

Type:

```text
String
```

TTL:

```text
10분
```

역할:

- 이미 사용한 entryToken 재사용을 차단한다.

## 5. 대기열 처리 흐름

### 1. 대기열 진입

```text
사용자 -> POST /waiting-room/enter
서버 -> Redis Sorted Set에 waitingToken 저장
서버 -> 사용자별 token key 저장
서버 -> rank 반환
```

### 2. 순번 조회

```text
사용자 -> GET /waiting-room/status
서버 -> ZRANK로 순번 조회
서버 -> allowed key 확인
서버 -> WAITING 또는 ALLOWED 반환
```

### 3. 입장 허용

```text
관리자 또는 스케줄러 -> POST /waiting-room/admit
서버 -> ZRANGE로 상위 N명 조회
서버 -> ZREM으로 대기열에서 제거
서버 -> allowed key 저장
```

### 4. 입장 토큰 발급

```text
사용자 -> POST /waiting-room/validate
서버 -> allowed key 확인
서버 -> entryToken 발급
서버 -> entry key 저장
```

### 5. 예매 API 검증

```text
사용자 -> POST /reservations with entryToken
서버 -> entry key 확인
서버 -> userId 일치 확인
서버 -> entry key 삭제
서버 -> used key 저장
서버 -> 좌석 예매 트랜잭션 진행
```

## 6. 예매 API 연결

변경 파일:

```text
src/main/java/com/example/ticketing/reservation/service/ReservationService.java
```

예매 생성 트랜잭션 내부에서 다음 검증을 수행한다.

```java
waitingRoomService.validateEntryTokenIfPresent(userId, request.concertId(), request.entryToken());
```

현재는 이전 단계 테스트와 기본 API 사용성을 유지하기 위해 `entryToken`이 없으면 검증을 생략한다. 실제 운영 모드에서는 `entryToken`을 필수로 바꾸면 된다.

운영 모드에서 바꿀 정책:

```text
entryToken 없음 -> WAITING_ROOM_REQUIRED
entryToken 있음 but Redis 검증 실패 -> WAITING_TOKEN_NOT_ALLOWED
entryToken 만료 또는 재사용 -> WAITING_TOKEN_EXPIRED
```

## 7. 현재 구현의 한계

이번 단계에서는 Redis 대기열의 핵심 흐름을 구현했다. 다만 다음 항목은 11단계 모니터링/부하 테스트 단계에서 보강하는 것이 좋다.

- 입장 허용 자동 스케줄러
- 공연별 동적 admission limit
- Redis 장애 시 fallback 정책
- Lua Script를 이용한 allowed token 원자적 소비
- 대기열 길이 Micrometer Gauge 등록
- 부하 테스트에서 대기열 적용 전후 비교

## 8. 테스트 결과

현재 단계에서는 Redis 서버가 없어도 기존 트랜잭션/락 테스트가 통과하도록 설계했다.

실행 명령:

```bash
./gradlew test
```

결과:

```text
BUILD SUCCESSFUL
```

Redis 대기열 API를 수동 테스트하려면 먼저 Docker Compose로 Redis를 실행한다.

```bash
docker compose up -d redis
```

## 9. 다음 단계

10단계에서는 Mock 결제 및 예매 만료 처리를 구현한다.

목표:

- Payment 엔티티와 Repository 구현
- Mock 결제 성공/실패 API 구현
- 결제 성공 시 좌석 `RESERVED`, 예매 `CONFIRMED`
- 결제 실패 시 좌석 `AVAILABLE`, 예매 `CANCELLED`
- 예매 만료 스케줄러 구현
- 만료 시 좌석 복구
