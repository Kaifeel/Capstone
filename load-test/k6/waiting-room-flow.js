import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    waiting_room: {
      executor: 'ramping-vus',
      stages: [
        { duration: '10s', target: Number(__ENV.VUS || 100) },
        { duration: '30s', target: Number(__ENV.VUS || 100) },
        { duration: '10s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.10'],
    http_req_duration: ['p(95)<1000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN;
const CONCERT_ID = Number(__ENV.CONCERT_ID || 1);

export default function () {
  const enterResponse = http.post(
    `${BASE_URL}/api/v1/waiting-room/enter`,
    JSON.stringify({ concertId: CONCERT_ID }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${TOKEN}`,
      },
    },
  );

  check(enterResponse, {
    'enter status is 200': (res) => res.status === 200,
  });

  const body = enterResponse.json();
  const waitingToken = body && body.data ? body.data.waitingToken : null;

  if (waitingToken) {
    const statusResponse = http.get(
      `${BASE_URL}/api/v1/waiting-room/status?concertId=${CONCERT_ID}&waitingToken=${encodeURIComponent(waitingToken)}`,
      {
        headers: {
          Authorization: `Bearer ${TOKEN}`,
        },
      },
    );

    check(statusResponse, {
      'status query is 200': (res) => res.status === 200,
    });
  }

  sleep(1);
}
