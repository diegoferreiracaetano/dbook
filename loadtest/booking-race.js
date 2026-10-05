// Many customers book THE SAME seat at the same instant: exactly one may win, everyone else must get a 409, and
// nobody may get a 5xx. This is the promise of the whole booking flow, so the threshold is on the count, not on time.
//   k6 run loadtest/booking-race.js      (CUSTOMERS=50 to change how many race)
import { check } from 'k6';
import http from 'k6/http';
import { Counter } from 'k6/metrics';
import { adminToken, createFlight, newCustomer, post, seatsOf } from './lib.js';

const won = new Counter('booking_won');
const refused = new Counter('booking_refused_409');
const broken = new Counter('booking_unexpected_status');
// a refusal (409) is the right answer for every loser, not a failed request
http.setResponseCallback(http.expectedStatuses(201, 409));

const CUSTOMERS = Number(__ENV.CUSTOMERS || 50);

export const options = {
  scenarios: {
    race: { executor: 'shared-iterations', vus: CUSTOMERS, iterations: CUSTOMERS, maxDuration: '2m' },
  },
  thresholds: {
    booking_won: ['count==1'],
    booking_unexpected_status: ['count==0'],
    booking_refused_409: [`count==${CUSTOMERS - 1}`],
  },
};

export function setup() {
  const flight = createFlight(adminToken(), 1, 500 + Math.floor(Math.random() * 400));
  const customers = Array.from({ length: CUSTOMERS }, (_, i) => newCustomer(`race${i}`));
  return { flight, seat: seatsOf(flight.id)[0], customers };
}

export default function (data) {
  const token = data.customers[(__ITER) % data.customers.length];
  const res = post('/v1/bookings', { bookableId: data.flight.id, seatId: data.seat }, token);
  if (res.status === 201) won.add(1);
  else if (res.status === 409) refused.add(1);
  else broken.add(1);
  check(res, { 'never a server error': (r) => r.status < 500 });
}
