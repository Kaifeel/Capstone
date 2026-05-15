# k6 Load Test Scenarios

## Prerequisites

- Spring Boot API is running on `http://localhost:8080`
- PostgreSQL and Redis are running with Docker Compose
- Test users, concert, and seats are already prepared
- JWT token is issued with `/api/v1/auth/login`

## Same Seat

Many users try to reserve the same seat.

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TOKEN={jwt} \
  -e CONCERT_ID=1 \
  -e SEAT_ID=1 \
  -e VUS=100 \
  load-test/k6/same-seat-reservation.js
```

## Multiple Seats

Users reserve seats distributed across the same concert.

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TOKEN={jwt} \
  -e CONCERT_ID=1 \
  -e START_SEAT_ID=1 \
  -e SEAT_COUNT=100 \
  -e VUS=500 \
  load-test/k6/multiple-seats-reservation.js
```

## Waiting Room

Users enter the Redis waiting room and query their rank.

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TOKEN={jwt} \
  -e CONCERT_ID=1 \
  -e VUS=1000 \
  load-test/k6/waiting-room-flow.js
```

## Recommended Runs

Run each scenario with:

```text
VUS=100
VUS=500
VUS=1000
```

Record:

- success count
- failure count
- average response time
- p95 response time
- requests per second
- duplicated active reservations
- waiting room length
