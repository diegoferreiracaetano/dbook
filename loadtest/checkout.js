// The whole purchase, as a customer does it: register, sign in, book a seat, pay. Each iteration is a new customer
// on a seat of its own, so nothing contends and the numbers say what the flow costs.
// (Registering and signing in pay the BCrypt cost on purpose: that is what a real customer costs.)
//   k6 run loadtest/checkout.js          (RATE=5 DURATION=1m to change the load)
import { check } from 'k6';
import exec from 'k6/execution';
import { Trend } from 'k6/metrics';
import { adminToken, createFlight, newCustomer, post, seatsOf } from './lib.js';

const bookTime = new Trend('step_book', true);
const payTime = new Trend('step_pay', true);

export const options = {
  scenarios: {
    checkout: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 5),
      timeUnit: '1s',
      duration: __ENV.DURATION || '1m',
      preAllocatedVUs: 20,
      maxVUs: 100,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    step_book: ['p(95)<400'],
    step_pay: ['p(95)<600'],
  },
};

export function setup() {
  const token = adminToken();
  const seats = [];
  // 20 flights of 200 seats: enough seats for the whole run without two customers on one seat
  for (let i = 0; i < 20; i++) {
    const flight = createFlight(token, 200, 600 + Math.floor(Math.random() * 300));
    seatsOf(flight.id).forEach((seat) => seats.push({ flight: flight.id, seat }));
  }
  return seats;
}

export default function (seats) {
  const token = newCustomer('checkout');
  // the iteration number is unique across the whole run: no two customers pick the same seat
  const target = seats[exec.scenario.iterationInTest % seats.length];
  const booked = post('/v1/bookings', { bookableId: target.flight, seatId: target.seat }, token);
  bookTime.add(booked.timings.duration);
  if (!check(booked, { 'booked (201)': (r) => r.status === 201 })) return;

  const paid = post(
    '/v1/payments',
    { bookingIds: [booked.json('id')], cardLast4: '4242', cardholderName: 'Load Test' },
    token,
    { 'Idempotency-Key': `${__VU}-${__ITER}-${Date.now()}` },
  );
  payTime.add(paid.timings.duration);
  check(paid, { 'paid (201)': (r) => r.status === 201 });
}
