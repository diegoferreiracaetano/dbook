-- Loads a CRM-sized dataset straight into the local database: customers (some blocked, some never logged in),
-- bookings in every status, payments and reviews. Used to measure the customer search (docs/crm.md).
--
-- Local dev only, and straight SQL on purpose: 50 000 accounts through the API would spend their time in BCrypt.
-- The password hash is not a real one, so none of these customers can log in. Safe to run once per database.
--
-- Usage: ./scripts/seed-customers.sh [customer_count]
\if :{?customers}
\else
\set customers 50000
\endif

BEGIN;

-- 100 flights of 999 seats each, to book from
INSERT INTO bookable (title, price, total_capacity)
SELECT 'Seed flight ' || g, 200 + (g * 37 % 800), 999 FROM generate_series(1, 100) g;

INSERT INTO flight (id, flight_number, origin_airport_id, destination_airport_id, departure_time, arrival_time,
                    seat_class, airline_id, aircraft_type)
SELECT b.id,
       'SD' || lpad(b.id::text, 5, '0'),
       (SELECT id FROM airport ORDER BY id LIMIT 1 OFFSET (b.id % 9)),
       (SELECT id FROM airport ORDER BY id LIMIT 1 OFFSET ((b.id + 3) % 9)),
       now() + (b.id || ' days')::interval,
       now() + (b.id || ' days')::interval + interval '3 hours',
       'ECONOMY',
       (SELECT id FROM airline ORDER BY id LIMIT 1 OFFSET (b.id % 6)),
       'Airbus A320'
FROM bookable b WHERE b.title LIKE 'Seed flight %';

INSERT INTO seat (bookable_id, label, status)
SELECT b.id, 'S' || lpad(s::text, 3, '0'), 'AVAILABLE'
FROM bookable b CROSS JOIN generate_series(1, 999) s
WHERE b.title LIKE 'Seed flight %'
ORDER BY b.id, s;

CREATE TEMP TABLE seed_flight ON COMMIT DROP AS
SELECT row_number() OVER (ORDER BY b.id) - 1 AS idx, b.id, b.price,
       (SELECT min(s.id) FROM seat s WHERE s.bookable_id = b.id) AS first_seat
FROM bookable b WHERE b.title LIKE 'Seed flight %';

-- customers: names with a few rare surnames (selective searches), 5% blocked, 20% never logged in
INSERT INTO app_user (email, password_hash, name, role, status, blocked_reason, blocked_at, last_login_at, created_at)
SELECT 'customer' || g || '@seed.example.com',
       '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseedse',
       (ARRAY['João','Maria','José','Ana','Pedro','Beatriz','Lucas','Camila','Gabriel','Fernanda'])[1 + g % 10] || ' ' ||
       (ARRAY['Silva','Santos','Oliveira','Souza','Lima','Pereira','Costa','Ferreira','Rodrigues','Almeida'])[1 + (g / 10) % 10] ||
       CASE WHEN g % 7 = 0
            THEN ' ' || (ARRAY['Barbosa','Cardoso','Ribeiro','Carvalho','Gomes','Martins','Araújo','Melo'])[1 + g % 8]
            ELSE '' END,
       'CLIENT',
       CASE WHEN g % 20 = 0 THEN 'BLOCKED' ELSE 'ACTIVE' END,
       CASE WHEN g % 20 = 0 THEN 'Seed: blocked for testing' END,
       CASE WHEN g % 20 = 0 THEN now() - interval '10 days' END,
       CASE WHEN g % 5 = 0 THEN NULL ELSE now() - (random() * 60 || ' days')::interval END,
       now() - (random() * 730 || ' days')::interval
FROM generate_series(1, :customers) g;

-- 60% of the customers have 1 to 3 bookings, in every status
INSERT INTO booking (bookable_id, customer_id, status, seat_id, price)
SELECT f.id, u.id,
       CASE (u.id + k) % 3 WHEN 0 THEN 'PENDING' WHEN 1 THEN 'CONFIRMED' ELSE 'CANCELLED' END,
       f.first_seat + ((u.id * (k + 1)) % 999),
       f.price
FROM app_user u
CROSS JOIN generate_series(0, 2) k
JOIN seed_flight f ON f.idx = (u.id * (k + 1)) % 100
WHERE u.email LIKE '%@seed.example.com' AND u.id % 10 < 6 AND k <= u.id % 3;

-- one payment per customer with confirmed bookings, covering all of them
INSERT INTO payment (customer_id, amount, card_last4, cardholder_name)
SELECT b.customer_id, sum(b.price), '4242', max(u.name)
FROM booking b JOIN app_user u ON u.id = b.customer_id
WHERE b.status = 'CONFIRMED' AND u.email LIKE '%@seed.example.com'
GROUP BY b.customer_id;

UPDATE booking SET payment_id = p.id
FROM payment p
WHERE booking.customer_id = p.customer_id AND booking.status = 'CONFIRMED' AND booking.payment_id IS NULL;

-- spread the bookings over the last 400 days and give each the history the app would have written: created as PENDING,
-- then the move to its current status a few minutes later (cancellations by the system have no actor)
UPDATE booking SET created_at = now() - (random() * 400 || ' days')::interval
WHERE customer_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.example.com');

INSERT INTO booking_status_history (booking_id, from_status, to_status, actor_id, occurred_at)
SELECT b.id, NULL, 'PENDING', b.customer_id, b.created_at
FROM booking b JOIN app_user u ON u.id = b.customer_id WHERE u.email LIKE '%@seed.example.com';

INSERT INTO booking_status_history (booking_id, from_status, to_status, actor_id, occurred_at)
SELECT b.id, 'PENDING', b.status, CASE WHEN b.status = 'CANCELLED' THEN NULL ELSE b.customer_id END,
       b.created_at + interval '10 minutes'
FROM booking b JOIN app_user u ON u.id = b.customer_id
WHERE u.email LIKE '%@seed.example.com' AND b.status IN ('CONFIRMED', 'CANCELLED');

INSERT INTO review (booking_id, customer_id, rating, comment)
SELECT id, customer_id, 1 + id % 5, 'Seed review'
FROM booking WHERE status = 'CONFIRMED' AND id % 5 < 2;

COMMIT;

ANALYZE app_user;
ANALYZE booking;
ANALYZE payment;
ANALYZE review;
ANALYZE booking_status_history;
