import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    same_seat: {
      executor: 'ramping-vus',
      stages: [
        { duration: '10s', target: Number(__ENV.VUS || 100) },
        { duration: '30s', target: Number(__ENV.VUS || 100) },
        { duration: '10s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.50'],
    http_req_duration: ['p(95)<2000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN;
const CONCERT_ID = Number(__ENV.CONCERT_ID || 1);
const SEAT_ID = Number(__ENV.SEAT_ID || 1);
const ENTRY_TOKEN = __ENV.ENTRY_TOKEN || null;

export default function () {
  const payload = {
    concertId: CONCERT_ID,
    seatId: SEAT_ID,
    entryToken: ENTRY_TOKEN,
  };

  const response = http.post(`${BASE_URL}/api/v1/reservations`, JSON.stringify(payload), {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${TOKEN}`,
    },
  });

  check(response, {
    'status is handled': (res) => [200, 400, 401, 403, 409, 410, 423, 500].includes(res.status),
    'no duplicate success shape error': (res) => res.status !== 201,
  });

  sleep(1);
}
