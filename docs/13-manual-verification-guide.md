# 13. 수동 검증 가이드

## 1. 목적

이 문서는 발표 또는 제출 전 실제 API를 순서대로 호출해 핵심 기능이 동작하는지 확인하기 위한 체크리스트다.

검증 범위:

- 회원가입/로그인
- 공연 등록
- 좌석 생성
- 좌석 조회
- 대기열 진입/입장 허용/entryToken 발급
- 예매 생성
- Mock 결제 성공 또는 실패
- 관리자 지표 조회
- 모니터링 endpoint 확인

## 2. 사전 준비

### 인프라 실행

```bash
docker compose up -d
```

### 애플리케이션 실행

```bash
./gradlew bootRun
```

### 기본 URL

```text
http://localhost:8080
```

## 3. 확인 순서

## 3.1 Health Check

```bash
curl http://localhost:8080/actuator/health
```

기대:

```text
status = UP
```

## 3.2 회원가입

```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password1234",
    "name": "일반사용자"
  }'
```

기대:

```text
success = true
role = USER
```

## 3.3 로그인

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password1234"
  }'
```

기대:

```text
accessToken 발급
```

토큰 저장:

```bash
export USER_TOKEN={accessToken}
```

## 3.4 관리자 계정 준비

현재 회원가입 API는 기본적으로 `USER` 권한을 만든다. 관리자 API 검증을 위해서는 DB에서 role을 `ADMIN`으로 변경한다.

예시:

```bash
docker exec -it ticketing-postgres psql -U ticketing -d ticketing
```

```sql
UPDATE users
SET role = 'ADMIN'
WHERE email = 'user@example.com';
```

다시 로그인해 관리자 토큰을 발급받는다.

```bash
export ADMIN_TOKEN={adminAccessToken}
```

## 3.5 공연 등록

```bash
curl -X POST http://localhost:8080/api/v1/concerts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "title": "Capstone Live Concert",
    "venue": "Campus Hall",
    "concertDateTime": "2026-12-20T19:00:00",
    "reservationStartAt": "2026-01-01T00:00:00",
    "reservationEndAt": "2026-12-20T18:00:00",
    "totalSeatCount": 100
  }'
```

기대:

```text
concertId 반환
```

저장:

```bash
export CONCERT_ID={concertId}
```

## 3.6 좌석 생성

```bash
curl -X POST http://localhost:8080/api/v1/concerts/$CONCERT_ID/seats \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "seats": [
      {"section": "A", "row": "1", "number": 1, "price": 150000},
      {"section": "A", "row": "1", "number": 2, "price": 150000},
      {"section": "A", "row": "1", "number": 3, "price": 150000}
    ]
  }'
```

기대:

```text
createdCount = 3
```

## 3.7 좌석 조회

```bash
curl http://localhost:8080/api/v1/concerts/$CONCERT_ID/seats
```

기대:

```text
status = AVAILABLE
seatId 확인
```

저장:

```bash
export SEAT_ID={seatId}
```

## 3.8 대기열 진입

```bash
curl -X POST http://localhost:8080/api/v1/waiting-room/enter \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d "{
    \"concertId\": $CONCERT_ID
  }"
```

기대:

```text
waitingToken 반환
```

저장:

```bash
export WAITING_TOKEN={waitingToken}
```

## 3.9 입장 허용

관리자 API로 상위 N명을 입장 가능 상태로 전환한다.

```bash
curl -X POST "http://localhost:8080/api/v1/waiting-room/admit?concertId=$CONCERT_ID&limit=100" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 3.10 Entry Token 발급

```bash
curl -X POST http://localhost:8080/api/v1/waiting-room/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d "{
    \"concertId\": $CONCERT_ID,
    \"waitingToken\": \"$WAITING_TOKEN\"
  }"
```

기대:

```text
entryToken 반환
```

저장:

```bash
export ENTRY_TOKEN={entryToken}
```

## 3.11 예매 생성

```bash
curl -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d "{
    \"concertId\": $CONCERT_ID,
    \"seatId\": $SEAT_ID,
    \"entryToken\": \"$ENTRY_TOKEN\"
  }"
```

기대:

```text
Reservation status = PENDING
Seat status = HOLD
```

저장:

```bash
export RESERVATION_ID={reservationId}
```

## 3.12 Mock 결제 성공

실제 결제 연동이 아니라 상태 전이 시뮬레이션이다.

```bash
curl -X POST http://localhost:8080/api/v1/payments/mock \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d "{
    \"reservationId\": $RESERVATION_ID,
    \"result\": \"SUCCESS\"
  }"
```

기대:

```text
Payment status = SUCCESS
Reservation status = CONFIRMED
Seat status = RESERVED
```

## 3.13 관리자 지표 조회

```bash
curl "http://localhost:8080/api/v1/admin/metrics/reservations?concertId=$CONCERT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

기대:

```text
confirmedCount 증가
reservedSeatCount 증가
```

## 4. 모니터링 확인

Prometheus:

```text
http://localhost:9090
```

Grafana:

```text
http://localhost:3000
admin / admin
```

Actuator Prometheus:

```text
http://localhost:8080/actuator/prometheus
```

## 5. 검증 체크리스트

| 항목 | 기대 결과 | 확인 |
| --- | --- | --- |
| 회원가입 | USER 생성 |  |
| 로그인 | JWT 발급 |  |
| 관리자 권한 변경 | ADMIN API 접근 가능 |  |
| 공연 등록 | concertId 반환 |  |
| 좌석 생성 | createdCount 반환 |  |
| 대기열 진입 | waitingToken 반환 |  |
| 입장 허용 | allowed token 생성 |  |
| entryToken 발급 | entryToken 반환 |  |
| 예매 생성 | PENDING/HOLD |  |
| Mock 결제 성공 | CONFIRMED/RESERVED |  |
| 관리자 지표 | 예약/좌석 카운트 반영 |  |
| Grafana | dashboard 표시 |  |
