-- The CRM queries exactly as CustomerSql / CustomerProfileAdapter build them, under EXPLAIN (ANALYZE), to check
-- that the indexes of V31/V32 are used. Run after scripts/seed-customers.sh:
--   docker compose exec -T postgres psql -U dbook -d dbook < scripts/explain-customers.sql

\echo '== 1. default listing (no filter): newest first, page 1'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF, TIMING OFF)
SELECT p.id, p.name, p.email, p.status, p.created_at, p.last_login_at,
       (SELECT count(*) FROM booking b WHERE b.customer_id = p.id) AS booking_count
FROM (SELECT u.id, u.name, u.email, u.status, u.created_at, u.last_login_at
      FROM app_user u WHERE u.role = 'CLIENT' ORDER BY u.created_at DESC, u.id DESC LIMIT 20 OFFSET 0) p
ORDER BY p.created_at DESC, p.id DESC;

\echo '== 2. text search (a surname shared by ~2% of customers), page 1'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF, TIMING OFF)
SELECT p.id, p.name, p.email, p.status, p.created_at, p.last_login_at,
       (SELECT count(*) FROM booking b WHERE b.customer_id = p.id) AS booking_count
FROM (SELECT u.id, u.name, u.email, u.status, u.created_at, u.last_login_at
      FROM app_user u WHERE u.role = 'CLIENT'
        AND (immutable_unaccent(lower(u.name)) LIKE immutable_unaccent(lower('%barbosa%')) ESCAPE '\'
             OR immutable_unaccent(lower(u.email)) LIKE immutable_unaccent(lower('%barbosa%')) ESCAPE '\') ORDER BY u.created_at DESC, u.id DESC LIMIT 20 OFFSET 0) p
ORDER BY p.created_at DESC, p.id DESC;

\echo '== 3. text search: its count'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF, TIMING OFF)
SELECT count(*) FROM app_user u
WHERE u.role = 'CLIENT'
  AND (immutable_unaccent(lower(u.name)) LIKE immutable_unaccent(lower('%barbosa%')) ESCAPE '\'
       OR immutable_unaccent(lower(u.email)) LIKE immutable_unaccent(lower('%barbosa%')) ESCAPE '\');

\echo '== 4. a selective search (one e-mail), accent and case folded'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF, TIMING OFF)
SELECT u.id FROM app_user u
WHERE u.role = 'CLIENT'
  AND (immutable_unaccent(lower(u.name)) LIKE immutable_unaccent(lower('%CUSTOMER49999@%')) ESCAPE '\'
       OR immutable_unaccent(lower(u.email)) LIKE immutable_unaccent(lower('%CUSTOMER49999@%')) ESCAPE '\')
ORDER BY u.created_at DESC, u.id DESC LIMIT 20;

\echo '== 5. customers with bookings, blocked, page 1'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF, TIMING OFF)
SELECT p.id, p.name, p.email, p.status, p.created_at, p.last_login_at,
       (SELECT count(*) FROM booking b WHERE b.customer_id = p.id) AS booking_count
FROM (SELECT u.id, u.name, u.email, u.status, u.created_at, u.last_login_at
      FROM app_user u WHERE u.role = 'CLIENT' AND u.status = 'BLOCKED'
        AND EXISTS (SELECT 1 FROM booking b WHERE b.customer_id = u.id) ORDER BY u.created_at DESC, u.id DESC LIMIT 20 OFFSET 0) p
ORDER BY p.created_at DESC, p.id DESC;

\echo '== 6. a deep page (offset 40 000): the price of paging by offset'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF, TIMING OFF)
SELECT p.id, p.name, p.email, p.status, p.created_at, p.last_login_at,
       (SELECT count(*) FROM booking b WHERE b.customer_id = p.id) AS booking_count
FROM (SELECT u.id, u.name, u.email, u.status, u.created_at, u.last_login_at
      FROM app_user u WHERE u.role = 'CLIENT' ORDER BY u.created_at DESC, u.id DESC LIMIT 20 OFFSET 40000) p
ORDER BY p.created_at DESC, p.id DESC;

\echo '== 7. the 360 view of one customer'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF, TIMING OFF)
SELECT u.id, b.pending, b.confirmed, b.cancelled, p.total, p.paid, r.total, r.average
FROM app_user u
CROSS JOIN LATERAL (
    SELECT count(*) FILTER (WHERE status = 'PENDING') AS pending,
           count(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed,
           count(*) FILTER (WHERE status = 'CANCELLED') AS cancelled
    FROM booking WHERE customer_id = u.id) b
CROSS JOIN LATERAL (SELECT count(*) AS total, coalesce(sum(amount), 0) AS paid FROM payment WHERE customer_id = u.id) p
CROSS JOIN LATERAL (SELECT count(*) AS total, round(avg(rating), 1) AS average FROM review WHERE customer_id = u.id) r
WHERE u.id = (SELECT customer_id FROM booking GROUP BY customer_id HAVING count(*) = 3 LIMIT 1) AND u.role = 'CLIENT';
