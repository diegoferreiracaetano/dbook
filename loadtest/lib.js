// What the three scenarios share: where the API is, how to sign in as the seed admin and create a flight to book.
// The seed admin exists after `./scripts/seed-flights.sh` (local only).
import http from 'k6/http';

export const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const JSON_HEADERS = { 'Content-Type': 'application/json' };

export function post(path, body, token, extraHeaders) {
  const headers = Object.assign({}, JSON_HEADERS, extraHeaders || {});
  if (token) headers.Authorization = `Bearer ${token}`;
  return http.post(`${BASE}${path}`, JSON.stringify(body), { headers });
}

export function get(path, token, params) {
  const headers = token ? { Authorization: `Bearer ${token}` } : {};
  return http.get(`${BASE}${path}`, Object.assign({ headers }, params || {}));
}

export function adminToken() {
  const res = post('/v1/auth/login', { email: 'seed-admin@example.com', password: 'seed-password-123' });
  if (res.status !== 200) throw new Error(`seed admin cannot sign in (${res.status} ${res.body}): run ./scripts/seed-flights.sh first`);
  return res.json('accessToken');
}

// a flight with `capacity` seats on a day far enough ahead for nobody else to use
export function createFlight(token, capacity, daysAhead) {
  const departure = new Date(Date.now() + daysAhead * 86400000);
  departure.setUTCHours(10, 0, 0, 0);
  const arrival = new Date(departure.getTime() + 3600000);
  const iso = (d) => d.toISOString().slice(0, 19);
  const res = post('/v1/admin/flights', {
    flightNumber: `LT${Math.floor(Math.random() * 90000) + 10000}`,
    airlineIataCode: 'LA',
    originIataCode: 'GRU',
    destinationIataCode: 'GIG',
    departureTime: iso(departure),
    arrivalTime: iso(arrival),
    seatClass: 'ECONOMY',
    price: 100.0,
    totalCapacity: capacity,
    aircraftType: 'Airbus A320',
  }, token);
  if (res.status !== 201) throw new Error(`could not create the flight (${res.status}): ${res.body}`);
  return { id: res.json('id'), date: iso(departure).slice(0, 10) };
}

export function seatsOf(flightId) {
  const res = get(`/v1/bookables/${flightId}/seats`);
  if (res.status !== 200) throw new Error(`no seat map for flight ${flightId} (${res.status}): ${res.body}`);
  return res.json().map((seat) => seat.id);
}

export function newCustomer(label) {
  const email = `${label}-${__VU}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@loadtest.example.com`;
  const password = 'load-test-password-1';
  post('/v1/auth/register', { email, password, name: 'Load Test' });
  const login = post('/v1/auth/login', { email, password });
  return login.json('accessToken');
}
