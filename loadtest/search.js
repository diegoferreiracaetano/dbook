// The public flight search under a steady load: the most repeated read of the API.
//   k6 run loadtest/search.js            (RATE=100 DURATION=1m to change the load)
import { check } from 'k6';
import { adminToken, createFlight, get } from './lib.js';

export const options = {
  scenarios: {
    search: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 100),
      timeUnit: '1s',
      duration: __ENV.DURATION || '1m',
      preAllocatedVUs: 50,
      maxVUs: 200,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<300', 'p(99)<800'],
  },
};

export function setup() {
  const token = adminToken();
  // a handful of days with flights on GRU-GIG: the searches spread over them like real customers do
  return Array.from({ length: 5 }, (_, i) => createFlight(token, 30, 400 + i).date);
}

export default function (days) {
  const date = days[Math.floor(Math.random() * days.length)];
  const res = get(`/v1/flights/search?origin=GRU&destination=GIG&date=${date}`);
  check(res, { 'search answered 200': (r) => r.status === 200 });
}
