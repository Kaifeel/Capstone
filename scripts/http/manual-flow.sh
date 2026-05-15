#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
USER_EMAIL="${USER_EMAIL:-user@example.com}"
USER_PASSWORD="${USER_PASSWORD:-password1234}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing command: $1" >&2
    exit 1
  fi
}

extract_json_value() {
  local key="$1"
  python3 -c "import json,sys; data=json.load(sys.stdin); cur=data; [cur := cur[p] for p in '$key'.split('.')]; print(cur)"
}

post_json() {
  local path="$1"
  local token="${2:-}"
  local body="$3"
  if [ -n "$token" ]; then
    curl -sS -X POST "$BASE_URL$path" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $token" \
      -d "$body"
  else
    curl -sS -X POST "$BASE_URL$path" \
      -H "Content-Type: application/json" \
      -d "$body"
  fi
}

get_with_token() {
  local path="$1"
  local token="${2:-}"
  if [ -n "$token" ]; then
    curl -sS "$BASE_URL$path" -H "Authorization: Bearer $token"
  else
    curl -sS "$BASE_URL$path"
  fi
}

require_command curl
require_command python3

echo "1. Health check"
get_with_token "/actuator/health"
echo

echo "2. Signup user. Duplicate signup may fail if the user already exists."
post_json "/api/v1/auth/signup" "" "{
  \"email\": \"$USER_EMAIL\",
  \"password\": \"$USER_PASSWORD\",
  \"name\": \"일반사용자\"
}" || true
echo

echo "3. Login user"
LOGIN_RESPONSE="$(post_json "/api/v1/auth/login" "" "{
  \"email\": \"$USER_EMAIL\",
  \"password\": \"$USER_PASSWORD\"
}")"
USER_TOKEN="$(printf '%s' "$LOGIN_RESPONSE" | extract_json_value "data.accessToken")"
echo "USER_TOKEN issued"

cat <<GUIDE

4. 관리자 API를 사용하려면 DB에서 이 사용자의 role을 ADMIN으로 변경한 뒤 다시 로그인하세요.

docker exec -it ticketing-postgres psql -U ticketing -d ticketing

UPDATE users
SET role = 'ADMIN'
WHERE email = '$USER_EMAIL';

그 후 아래 명령으로 스크립트를 다시 실행하거나, ADMIN_TOKEN 환경 변수를 직접 지정하세요.

ADMIN_TOKEN=\${ADMIN_TOKEN:-}
GUIDE

if [ -z "${ADMIN_TOKEN:-}" ]; then
  echo "ADMIN_TOKEN is not set. Stop before admin-only API calls."
  exit 0
fi

echo "5. Create concert"
CONCERT_RESPONSE="$(post_json "/api/v1/concerts" "$ADMIN_TOKEN" '{
  "title": "Capstone Live Concert",
  "venue": "Campus Hall",
  "concertDateTime": "2026-12-20T19:00:00",
  "reservationStartAt": "2026-01-01T00:00:00",
  "reservationEndAt": "2026-12-20T18:00:00",
  "totalSeatCount": 100
}')"
CONCERT_ID="$(printf '%s' "$CONCERT_RESPONSE" | extract_json_value "data.concertId")"
echo "CONCERT_ID=$CONCERT_ID"

echo "6. Create seats"
post_json "/api/v1/concerts/$CONCERT_ID/seats" "$ADMIN_TOKEN" '{
  "seats": [
    {"section": "A", "row": "1", "number": 1, "price": 150000},
    {"section": "A", "row": "1", "number": 2, "price": 150000},
    {"section": "A", "row": "1", "number": 3, "price": 150000}
  ]
}'
echo

echo "7. Get seats"
SEATS_RESPONSE="$(get_with_token "/api/v1/concerts/$CONCERT_ID/seats")"
SEAT_ID="$(printf '%s' "$SEATS_RESPONSE" | extract_json_value "data.seats.0.seatId")"
echo "SEAT_ID=$SEAT_ID"

echo "8. Enter waiting room"
WAITING_RESPONSE="$(post_json "/api/v1/waiting-room/enter" "$USER_TOKEN" "{
  \"concertId\": $CONCERT_ID
}")"
WAITING_TOKEN="$(printf '%s' "$WAITING_RESPONSE" | extract_json_value "data.waitingToken")"
echo "WAITING_TOKEN=$WAITING_TOKEN"

echo "9. Admit waiting users"
post_json "/api/v1/waiting-room/admit?concertId=$CONCERT_ID&limit=100" "$ADMIN_TOKEN" '{}'
echo

echo "10. Validate waiting token"
ENTRY_RESPONSE="$(post_json "/api/v1/waiting-room/validate" "$USER_TOKEN" "{
  \"concertId\": $CONCERT_ID,
  \"waitingToken\": \"$WAITING_TOKEN\"
}")"
ENTRY_TOKEN="$(printf '%s' "$ENTRY_RESPONSE" | extract_json_value "data.entryToken")"
echo "ENTRY_TOKEN=$ENTRY_TOKEN"

echo "11. Create reservation"
RESERVATION_RESPONSE="$(post_json "/api/v1/reservations" "$USER_TOKEN" "{
  \"concertId\": $CONCERT_ID,
  \"seatId\": $SEAT_ID,
  \"entryToken\": \"$ENTRY_TOKEN\"
}")"
RESERVATION_ID="$(printf '%s' "$RESERVATION_RESPONSE" | extract_json_value "data.reservationId")"
echo "RESERVATION_ID=$RESERVATION_ID"

echo "12. Mock payment success. This is not a real payment."
post_json "/api/v1/payments/mock" "$USER_TOKEN" "{
  \"reservationId\": $RESERVATION_ID,
  \"result\": \"SUCCESS\"
}"
echo

echo "13. Admin metrics"
get_with_token "/api/v1/admin/metrics/reservations?concertId=$CONCERT_ID" "$ADMIN_TOKEN"
echo
