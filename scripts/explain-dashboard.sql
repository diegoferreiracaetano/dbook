-- The dashboard queries exactly as DashboardSql builds them, under EXPLAIN (ANALYZE), over the last 30 days and the
-- full 400. Run after scripts/seed-customers.sh:
--   docker compose exec -T postgres psql -U dbook -d dbook < scripts/explain-dashboard.sql
\echo '== 1. gross revenue, 30 days'
EXPLAIN (ANALYZE, COSTS OFF, TIMING OFF, BUFFERS OFF)
SELECT coalesce(sum(b.price), 0)
FROM booking_status_history h JOIN booking b ON b.id = h.booking_id
WHERE h.to_status = 'CONFIRMED' AND h.occurred_at >= now() - interval '30 days' AND h.occurred_at < now();

\echo '== 2. gross revenue, 400 days'
EXPLAIN (ANALYZE, COSTS OFF, TIMING OFF, BUFFERS OFF)
SELECT coalesce(sum(b.price), 0)
FROM booking_status_history h JOIN booking b ON b.id = h.booking_id
WHERE h.to_status = 'CONFIRMED' AND h.occurred_at >= now() - interval '400 days' AND h.occurred_at < now();

\echo '== 3. bookings by status created in 30 days'
EXPLAIN (ANALYZE, COSTS OFF, TIMING OFF, BUFFERS OFF)
SELECT b.status, count(*) FROM booking b
WHERE b.created_at >= now() - interval '30 days' AND b.created_at < now() GROUP BY b.status;

\echo '== 4. conversion: created in 30 days that were paid'
EXPLAIN (ANALYZE, COSTS OFF, TIMING OFF, BUFFERS OFF)
SELECT count(*) FROM booking b
WHERE b.created_at >= now() - interval '30 days' AND b.created_at < now()
  AND EXISTS (SELECT 1 FROM booking_status_history h WHERE h.booking_id = b.id AND h.to_status = 'CONFIRMED');

\echo '== 5. expiration: created in 400 days that the system cancelled'
EXPLAIN (ANALYZE, COSTS OFF, TIMING OFF, BUFFERS OFF)
SELECT count(*) FROM booking b
WHERE b.created_at >= now() - interval '400 days' AND b.created_at < now()
  AND EXISTS (SELECT 1 FROM booking_status_history h
              WHERE h.booking_id = b.id AND h.to_status = 'CANCELLED' AND h.actor_id IS NULL);

\echo '== 6. revenue per day, 400 days'
EXPLAIN (ANALYZE, COSTS OFF, TIMING OFF, BUFFERS OFF)
SELECT bucket, sum(amount) FROM (
    SELECT date_trunc('day', h.occurred_at AT TIME ZONE 'America/Sao_Paulo')::date AS bucket, b.price AS amount
    FROM booking_status_history h JOIN booking b ON b.id = h.booking_id
    WHERE h.to_status = 'CONFIRMED' AND h.occurred_at >= now() - interval '400 days' AND h.occurred_at < now()
    UNION ALL
    SELECT date_trunc('day', completed_at AT TIME ZONE 'America/Sao_Paulo')::date, -amount
    FROM refund WHERE status = 'COMPLETED' AND completed_at >= now() - interval '400 days' AND completed_at < now()
) movements GROUP BY bucket;

\echo '== 7. top routes, 400 days'
EXPLAIN (ANALYZE, COSTS OFF, TIMING OFF, BUFFERS OFF)
SELECT o.iata_code, d.iata_code, count(*), sum(b.price)
FROM booking_status_history h
JOIN booking b ON b.id = h.booking_id
JOIN flight f ON f.id = b.bookable_id
JOIN airport o ON o.id = f.origin_airport_id
JOIN airport d ON d.id = f.destination_airport_id
WHERE h.to_status = 'CONFIRMED' AND h.occurred_at >= now() - interval '400 days' AND h.occurred_at < now()
GROUP BY o.iata_code, d.iata_code ORDER BY count(*) DESC LIMIT 10;

\echo '== 8. occupancy of the flights leaving in the next 30 days'
EXPLAIN (ANALYZE, COSTS OFF, TIMING OFF, BUFFERS OFF)
SELECT coalesce(sum(r.reserved), 0), coalesce(sum(b.total_capacity), 0)
FROM flight f JOIN bookable b ON b.id = f.id
LEFT JOIN LATERAL (SELECT count(*) AS reserved FROM seat s WHERE s.bookable_id = f.id AND s.status = 'RESERVED') r ON TRUE
WHERE b.active AND f.departure_time >= now() AND f.departure_time < now() + interval '30 days';
