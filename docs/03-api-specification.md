# 3단계. API 명세

## 1. 기본 정책

### Base URL

```text
/api/v1
```

### Content-Type

요청과 응답은 기본적으로 JSON을 사용한다.

```http
Content-Type: application/json
```

### 인증 방식

로그인 성공 시 JWT Access Token을 발급한다. 인증이 필요한 API는 `Authorization` 헤더에 Bearer Token을 전달한다.

```http
Authorization: Bearer {accessToken}
```

### 권한

| 권한 | 설명 |
| --- | --- |
| PUBLIC | 인증 없이 접근 가능 |
| USER | 로그인 사용자 접근 가능 |
| ADMIN | 관리자만 접근 가능 |

## 2. 공통 응답 형식

### 성공 응답

```json
{
  "success": true,
  "code": "OK",
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

### 실패 응답

```json
{
  "success": false,
  "code": "SEAT_ALREADY_OCCUPIED",
  "message": "이미 선택할 수 없는 좌석입니다.",
  "data": null
}
```

### 페이징 응답

```json
{
  "success": true,
  "code": "OK",
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "last": false
  }
}
```

## 3. 공통 에러 코드

| HTTP Status | Code | 설명 |
| --- | --- | --- |
| 400 | INVALID_REQUEST | 요청 값이 올바르지 않음 |
| 400 | INVALID_STATUS | 현재 상태에서 처리할 수 없음 |
| 401 | UNAUTHORIZED | 인증 필요 |
| 401 | INVALID_TOKEN | JWT가 없거나 유효하지 않음 |
| 403 | FORBIDDEN | 권한 없음 |
| 404 | USER_NOT_FOUND | 사용자를 찾을 수 없음 |
| 404 | CONCERT_NOT_FOUND | 공연을 찾을 수 없음 |
| 404 | SEAT_NOT_FOUND | 좌석을 찾을 수 없음 |
| 404 | RESERVATION_NOT_FOUND | 예매를 찾을 수 없음 |
| 409 | EMAIL_ALREADY_EXISTS | 이미 가입된 이메일 |
| 409 | SEAT_ALREADY_OCCUPIED | 이미 선택할 수 없는 좌석 |
| 409 | ACTIVE_RESERVATION_ALREADY_EXISTS | 활성 예매가 이미 존재함 |
| 409 | OPTIMISTIC_LOCK_CONFLICT | 낙관적 락 충돌 |
| 423 | WAITING_ROOM_REQUIRED | 대기열 통과 필요 |
| 423 | WAITING_TOKEN_NOT_ALLOWED | 아직 입장 순서가 아님 |
| 410 | WAITING_TOKEN_EXPIRED | 대기열 토큰 만료 |
| 410 | RESERVATION_EXPIRED | 예매 결제 시간 만료 |
| 500 | INTERNAL_SERVER_ERROR | 서버 내부 오류 |

## 4. API 목록

| 기능 | Method | Path | 권한 |
| --- | --- | --- | --- |
| 회원가입 | POST | `/api/v1/auth/signup` | PUBLIC |
| 로그인 | POST | `/api/v1/auth/login` | PUBLIC |
| 공연 등록 | POST | `/api/v1/concerts` | ADMIN |
| 공연 목록 조회 | GET | `/api/v1/concerts` | PUBLIC |
| 공연 상세 조회 | GET | `/api/v1/concerts/{concertId}` | PUBLIC |
| 공연 수정 | PATCH | `/api/v1/concerts/{concertId}` | ADMIN |
| 공연 삭제 | DELETE | `/api/v1/concerts/{concertId}` | ADMIN |
| 좌석 생성 | POST | `/api/v1/concerts/{concertId}/seats` | ADMIN |
| 좌석 목록 조회 | GET | `/api/v1/concerts/{concertId}/seats` | PUBLIC |
| 좌석 상세 조회 | GET | `/api/v1/seats/{seatId}` | PUBLIC |
| 대기열 진입 | POST | `/api/v1/waiting-room/enter` | USER |
| 대기열 상태 조회 | GET | `/api/v1/waiting-room/status` | USER |
| 입장 토큰 검증 | POST | `/api/v1/waiting-room/validate` | USER |
| 예매 생성 | POST | `/api/v1/reservations` | USER |
| 예매 상세 조회 | GET | `/api/v1/reservations/{reservationId}` | USER |
| 내 예매 목록 조회 | GET | `/api/v1/reservations/me` | USER |
| 예매 취소 | POST | `/api/v1/reservations/{reservationId}/cancel` | USER |
| Mock 결제 | POST | `/api/v1/payments/mock` | USER |
| 예매 지표 조회 | GET | `/api/v1/admin/metrics/reservations` | ADMIN |

## 5. 인증 API

### 5.1 회원가입

```http
POST /api/v1/auth/signup
```

권한: PUBLIC

#### Request

```json
{
  "email": "user@example.com",
  "password": "password1234",
  "name": "홍길동"
}
```

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "USER"
  }
}
```

#### Error

| Status | Code | 설명 |
| --- | --- | --- |
| 400 | INVALID_REQUEST | 이메일, 비밀번호, 이름 형식 오류 |
| 409 | EMAIL_ALREADY_EXISTS | 이미 가입된 이메일 |

### 5.2 로그인

```http
POST /api/v1/auth/login
```

권한: PUBLIC

#### Request

```json
{
  "email": "user@example.com",
  "password": "password1234"
}
```

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "로그인이 완료되었습니다.",
  "data": {
    "accessToken": "jwt-access-token",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "userId": 1,
      "email": "user@example.com",
      "name": "홍길동",
      "role": "USER"
    }
  }
}
```

#### Error

| Status | Code | 설명 |
| --- | --- | --- |
| 400 | INVALID_REQUEST | 요청 형식 오류 |
| 401 | UNAUTHORIZED | 이메일 또는 비밀번호 불일치 |

## 6. 공연 API

### 6.1 공연 등록

```http
POST /api/v1/concerts
```

권한: ADMIN

#### Request

```json
{
  "title": "2026 Spring Live Concert",
  "venue": "KSPO Dome",
  "concertDateTime": "2026-06-20T19:00:00",
  "reservationStartAt": "2026-06-01T20:00:00",
  "reservationEndAt": "2026-06-20T18:00:00",
  "totalSeatCount": 10000
}
```

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "공연이 등록되었습니다.",
  "data": {
    "concertId": 1,
    "title": "2026 Spring Live Concert",
    "venue": "KSPO Dome",
    "concertDateTime": "2026-06-20T19:00:00",
    "reservationStartAt": "2026-06-01T20:00:00",
    "reservationEndAt": "2026-06-20T18:00:00",
    "totalSeatCount": 10000
  }
}
```

### 6.2 공연 목록 조회

```http
GET /api/v1/concerts?page=0&size=20
```

권한: PUBLIC

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "concertId": 1,
        "title": "2026 Spring Live Concert",
        "venue": "KSPO Dome",
        "concertDateTime": "2026-06-20T19:00:00",
        "reservationStartAt": "2026-06-01T20:00:00",
        "reservationEndAt": "2026-06-20T18:00:00",
        "totalSeatCount": 10000
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

### 6.3 공연 상세 조회

```http
GET /api/v1/concerts/{concertId}
```

권한: PUBLIC

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "요청이 성공했습니다.",
  "data": {
    "concertId": 1,
    "title": "2026 Spring Live Concert",
    "venue": "KSPO Dome",
    "concertDateTime": "2026-06-20T19:00:00",
    "reservationStartAt": "2026-06-01T20:00:00",
    "reservationEndAt": "2026-06-20T18:00:00",
    "totalSeatCount": 10000,
    "availableSeatCount": 9120,
    "holdSeatCount": 100,
    "reservedSeatCount": 780
  }
}
```

### 6.4 공연 수정

```http
PATCH /api/v1/concerts/{concertId}
```

권한: ADMIN

#### Request

```json
{
  "title": "2026 Spring Live Concert - Seoul",
  "venue": "KSPO Dome",
  "concertDateTime": "2026-06-20T19:30:00",
  "reservationStartAt": "2026-06-01T20:00:00",
  "reservationEndAt": "2026-06-20T18:00:00"
}
```

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "공연이 수정되었습니다.",
  "data": {
    "concertId": 1
  }
}
```

### 6.5 공연 삭제

```http
DELETE /api/v1/concerts/{concertId}
```

권한: ADMIN

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "공연이 삭제되었습니다.",
  "data": {
    "concertId": 1
  }
}
```

## 7. 좌석 API

### 7.1 좌석 생성

```http
POST /api/v1/concerts/{concertId}/seats
```

권한: ADMIN

단건 또는 다건 생성을 지원한다. 초기 구현에서는 다건 생성 API 하나로 처리한다.

#### Request

```json
{
  "seats": [
    {
      "section": "A",
      "row": "1",
      "number": 1,
      "price": 150000
    },
    {
      "section": "A",
      "row": "1",
      "number": 2,
      "price": 150000
    }
  ]
}
```

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "좌석이 생성되었습니다.",
  "data": {
    "concertId": 1,
    "createdCount": 2
  }
}
```

#### Error

| Status | Code | 설명 |
| --- | --- | --- |
| 404 | CONCERT_NOT_FOUND | 공연 없음 |
| 409 | INVALID_REQUEST | 중복 좌석 포함 |

### 7.2 좌석 목록 조회

```http
GET /api/v1/concerts/{concertId}/seats?status=AVAILABLE
```

권한: PUBLIC

`status`는 선택 값이다.

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "요청이 성공했습니다.",
  "data": {
    "concertId": 1,
    "seats": [
      {
        "seatId": 1,
        "section": "A",
        "row": "1",
        "number": 1,
        "price": 150000,
        "status": "AVAILABLE"
      },
      {
        "seatId": 2,
        "section": "A",
        "row": "1",
        "number": 2,
        "price": 150000,
        "status": "HOLD",
        "holdExpiresAt": "2026-06-01T20:05:00"
      }
    ]
  }
}
```

### 7.3 좌석 상세 조회

```http
GET /api/v1/seats/{seatId}
```

권한: PUBLIC

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "요청이 성공했습니다.",
  "data": {
    "seatId": 1,
    "concertId": 1,
    "section": "A",
    "row": "1",
    "number": 1,
    "price": 150000,
    "status": "AVAILABLE",
    "holdExpiresAt": null
  }
}
```

## 8. 대기열 API

### 8.1 대기열 진입

```http
POST /api/v1/waiting-room/enter
```

권한: USER

#### Request

```json
{
  "concertId": 1
}
```

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "대기열에 진입했습니다.",
  "data": {
    "concertId": 1,
    "waitingToken": "waiting-token",
    "rank": 120,
    "estimatedWaitSeconds": 240,
    "status": "WAITING"
  }
}
```

#### 정책

- 같은 사용자가 같은 공연 대기열에 다시 진입하면 기존 토큰을 반환한다.
- Redis Sorted Set의 score는 진입 시각 또는 증가 시퀀스를 사용한다.
- 예매 종료 시간이 지나면 대기열 진입을 거부한다.

### 8.2 대기열 상태 조회

```http
GET /api/v1/waiting-room/status?concertId=1&waitingToken=waiting-token
```

권한: USER

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "요청이 성공했습니다.",
  "data": {
    "concertId": 1,
    "waitingToken": "waiting-token",
    "rank": 10,
    "waitingCount": 2350,
    "allowed": false,
    "status": "WAITING",
    "estimatedWaitSeconds": 20
  }
}
```

입장 가능 상태:

```json
{
  "success": true,
  "code": "OK",
  "message": "입장 가능합니다.",
  "data": {
    "concertId": 1,
    "waitingToken": "waiting-token",
    "rank": 0,
    "waitingCount": 2300,
    "allowed": true,
    "status": "ALLOWED",
    "expiresAt": "2026-06-01T20:05:00"
  }
}
```

### 8.3 입장 토큰 검증

```http
POST /api/v1/waiting-room/validate
```

권한: USER

#### Request

```json
{
  "concertId": 1,
  "waitingToken": "waiting-token"
}
```

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "입장 토큰이 검증되었습니다.",
  "data": {
    "concertId": 1,
    "entryToken": "entry-token",
    "expiresAt": "2026-06-01T20:05:00"
  }
}
```

#### 정책

- 예매 API는 `entryToken`을 함께 받아 검증한다.
- `entryToken`은 짧은 TTL을 가진다.
- 검증에 성공한 토큰은 재사용 방지를 위해 used key로 이동할 수 있다.

## 9. 예매 API

### 9.1 예매 생성

```http
POST /api/v1/reservations
```

권한: USER

#### Request

```json
{
  "concertId": 1,
  "seatId": 1,
  "entryToken": "entry-token"
}
```

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "예매가 생성되었습니다. 제한 시간 안에 결제를 완료해주세요.",
  "data": {
    "reservationId": 1,
    "concertId": 1,
    "seatId": 1,
    "status": "PENDING",
    "seatStatus": "HOLD",
    "expiresAt": "2026-06-01T20:05:00",
    "amount": 150000
  }
}
```

#### 처리 순서

1. JWT 인증 사용자 확인
2. entryToken 검증
3. 공연 예매 가능 시간 검증
4. 좌석 존재 여부와 공연 일치 여부 검증
5. 좌석 상태 AVAILABLE 검증
6. 락 전략에 따라 좌석 점유 처리
7. Seat를 HOLD로 변경
8. Reservation을 PENDING으로 생성
9. Payment PENDING 생성 또는 결제 요청 전까지 지연 생성

#### Error

| Status | Code | 설명 |
| --- | --- | --- |
| 404 | CONCERT_NOT_FOUND | 공연 없음 |
| 404 | SEAT_NOT_FOUND | 좌석 없음 |
| 409 | SEAT_ALREADY_OCCUPIED | 이미 HOLD 또는 RESERVED 상태 |
| 409 | ACTIVE_RESERVATION_ALREADY_EXISTS | 활성 예매가 이미 존재 |
| 423 | WAITING_ROOM_REQUIRED | 대기열 검증 필요 |
| 423 | WAITING_TOKEN_NOT_ALLOWED | 입장 불가 |
| 410 | WAITING_TOKEN_EXPIRED | 입장 토큰 만료 |

### 9.2 예매 상세 조회

```http
GET /api/v1/reservations/{reservationId}
```

권한: USER

본인 예매만 조회할 수 있다. ADMIN은 모든 예매를 조회할 수 있도록 확장 가능하다.

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "요청이 성공했습니다.",
  "data": {
    "reservationId": 1,
    "status": "PENDING",
    "expiresAt": "2026-06-01T20:05:00",
    "reservedAt": "2026-06-01T20:00:00",
    "confirmedAt": null,
    "cancelledAt": null,
    "concert": {
      "concertId": 1,
      "title": "2026 Spring Live Concert",
      "venue": "KSPO Dome",
      "concertDateTime": "2026-06-20T19:00:00"
    },
    "seat": {
      "seatId": 1,
      "section": "A",
      "row": "1",
      "number": 1,
      "price": 150000,
      "status": "HOLD"
    },
    "payment": {
      "paymentId": 1,
      "status": "PENDING",
      "amount": 150000
    }
  }
}
```

### 9.3 내 예매 목록 조회

```http
GET /api/v1/reservations/me?page=0&size=20&status=CONFIRMED
```

권한: USER

`status`는 선택 값이다.

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "reservationId": 1,
        "status": "CONFIRMED",
        "concertTitle": "2026 Spring Live Concert",
        "concertDateTime": "2026-06-20T19:00:00",
        "seatLabel": "A구역 1열 1번",
        "amount": 150000,
        "reservedAt": "2026-06-01T20:00:00",
        "confirmedAt": "2026-06-01T20:01:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

### 9.4 예매 취소

```http
POST /api/v1/reservations/{reservationId}/cancel
```

권한: USER

#### Request

```json
{
  "reason": "사용자 요청"
}
```

#### Response

```json
{
  "success": true,
  "code": "OK",
  "message": "예매가 취소되었습니다.",
  "data": {
    "reservationId": 1,
    "reservationStatus": "CANCELLED",
    "seatId": 1,
    "seatStatus": "AVAILABLE",
    "cancelledAt": "2026-06-01T20:10:00"
  }
}
```

#### 정책

- PENDING 예매 취소 시 좌석은 AVAILABLE로 복구한다.
- CONFIRMED 예매 취소도 초기 구현에서는 좌석을 AVAILABLE로 복구한다.
- EXPIRED 또는 CANCELLED 예매는 다시 취소할 수 없다.

## 10. 결제 API

### 10.1 Mock 결제

```http
POST /api/v1/payments/mock
```

권한: USER

#### Request

```json
{
  "reservationId": 1,
  "result": "SUCCESS"
}
```

`result`는 `SUCCESS`, `FAILED` 중 하나다. 부하 테스트나 시나리오 테스트를 위해 클라이언트가 결과를 지정할 수 있게 한다.

#### Success Response

```json
{
  "success": true,
  "code": "OK",
  "message": "결제가 완료되었습니다.",
  "data": {
    "paymentId": 1,
    "reservationId": 1,
    "paymentStatus": "SUCCESS",
    "reservationStatus": "CONFIRMED",
    "seatStatus": "RESERVED",
    "approvedAt": "2026-06-01T20:01:00"
  }
}
```

#### Failed Response

```json
{
  "success": true,
  "code": "OK",
  "message": "결제가 실패했습니다.",
  "data": {
    "paymentId": 1,
    "reservationId": 1,
    "paymentStatus": "FAILED",
    "reservationStatus": "CANCELLED",
    "seatStatus": "AVAILABLE",
    "failedAt": "2026-06-01T20:01:00"
  }
}
```

#### Error

| Status | Code | 설명 |
| --- | --- | --- |
| 404 | RESERVATION_NOT_FOUND | 예매 없음 |
| 410 | RESERVATION_EXPIRED | 이미 만료된 예매 |
| 409 | INVALID_STATUS | 결제 가능한 예매 상태가 아님 |

## 11. 관리자 지표 API

### 11.1 예매 지표 조회

```http
GET /api/v1/admin/metrics/reservations?concertId=1
```

권한: ADMIN

#### Response

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

## 12. DTO 목록
data transfer object

a b c
a_b_c

ab cd
abCd

AbCd



### Auth

| DTO | 용도 |
| --- | --- |
| SignupRequest | 회원가입 요청 |
| SignupResponse | 회원가입 응답 |
| LoginRequest | 로그인 요청 |
| LoginResponse | 로그인 응답 |
| AuthUserResponse | 인증 사용자 정보 |

### Concert

| DTO | 용도 |
| --- | --- |
| ConcertCreateRequest | 공연 등록 요청 |
| ConcertUpdateRequest | 공연 수정 요청 |
| ConcertResponse | 공연 기본 응답 |
| ConcertDetailResponse | 공연 상세 응답 |

### Seat

| DTO | 용도 |
| --- | --- |
| SeatBulkCreateRequest | 좌석 다건 생성 요청 |
| SeatCreateRequest | 좌석 단건 요청 |
| SeatResponse | 좌석 응답 |
| SeatListResponse | 공연별 좌석 목록 응답 |

### Waiting Room

| DTO | 용도 |
| --- | --- |
| WaitingRoomEnterRequest | 대기열 진입 요청 |
| WaitingRoomEnterResponse | 대기열 진입 응답 |
| WaitingRoomStatusResponse | 대기열 상태 응답 |
| WaitingRoomValidateRequest | 입장 검증 요청 |
| WaitingRoomValidateResponse | 입장 검증 응답 |

### Reservation

| DTO | 용도 |
| --- | --- |
| ReservationCreateRequest | 예매 생성 요청 |
| ReservationCreateResponse | 예매 생성 응답 |
| ReservationDetailResponse | 예매 상세 응답 |
| ReservationSummaryResponse | 내 예매 목록 응답 |
| ReservationCancelRequest | 예매 취소 요청 |
| ReservationCancelResponse | 예매 취소 응답 |

### Payment

| DTO | 용도 |
| --- | --- |
| MockPaymentRequest | Mock 결제 요청 |
| MockPaymentResponse | Mock 결제 응답 |

### Admin

| DTO | 용도 |
| --- | --- |
| ReservationMetricsResponse | 예매 운영 지표 응답 |

## 13. API별 검증 규칙 (프론트가 진짜 중요)

### 회원가입

- email은 이메일 형식이어야 한다.
- password는 최소 8자 이상으로 한다.
- name은 비어 있을 수 없다.

### 공연 등록

- title, venue는 비어 있을 수 없다.
- reservationStartAt은 reservationEndAt보다 이전이어야 한다.
- totalSeatCount는 1 이상이어야 한다.

### 좌석 생성

- section, row는 비어 있을 수 없다.
- number는 1 이상이어야 한다.
- price는 0 이상이어야 한다.
- 같은 공연 안에서 section, row, number 조합은 중복될 수 없다.

### 예매 생성

- concertId, seatId는 필수다.
- entryToken은 대기열 적용 후 필수다.
- 공연 예매 기간 안에서만 예매 가능하다.
- 좌석은 AVAILABLE 상태여야 한다.
- 좌석의 concertId와 요청 concertId가 일치해야 한다.

### 결제

- reservationId는 필수다.
- result는 SUCCESS 또는 FAILED만 허용한다.
- Reservation 상태가 PENDING이어야 한다.
- 결제 만료 시간이 지나지 않아야 한다.

## 14. 인증과 권한 적용 기준

| API 그룹 | 인증 필요 | 관리자 권한 |
| --- | --- | --- |
| Auth | 아니오 | 아니오 |
| Concert 조회 | 아니오 | 아니오 |
| Concert 등록/수정/삭제 | 예 | 예 |
| Seat 조회 | 아니오 | 아니오 |
| Seat 생성 | 예 | 예 |
| Waiting Room | 예 | 아니오 |
| Reservation | 예 | 아니오 |
| Payment | 예 | 아니오 |
| Admin Metrics | 예 | 예 |

## 15. 구현 순서

API 구현은 다음 순서로 진행한다.

1. 공통 응답 DTO와 에러 응답 구현
2. GlobalExceptionHandler 구현
3. Auth API 구현
4. Concert API 구현
5. Seat API 구현
6. Reservation API 기본 흐름 구현
7. Payment API 구현
8. Waiting Room API 구현
9. Admin Metrics API 구현

## 16. 다음 단계

4단계에서는 Spring Boot 프로젝트 구조를 설계한다.

다음 항목을 작성한다.

- Gradle 설정
- 패키지 구조
- 계층별 책임
- 공통 응답/예외 구조
- Security 구조
- 도메인별 클래스 목록
- 테스트 패키지 구조
