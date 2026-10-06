package com.dbook.infrastructure.persistence.pricing

import com.dbook.domain.pricing.PriceHistory
import com.dbook.domain.pricing.PricePoint
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant

@Repository
class PriceHistoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : PriceHistory {
    // it joins the transaction that sets the price: a history row without the price change cannot exist
    @Transactional(propagation = Propagation.MANDATORY)
    override fun record(
        flightId: Long,
        price: BigDecimal,
        at: Instant,
    ) {
        jdbc.update(
            "INSERT INTO flight_price_history (flight_id, price, changed_at) VALUES (:flight, :price, :at)",
            mapOf("flight" to flightId, "price" to price, "at" to Timestamp.from(at)),
        )
    }

    @Transactional(readOnly = true)
    override fun of(
        flightId: Long,
        limit: Int,
    ): List<PricePoint> =
        jdbc.query(
            "SELECT price, changed_at FROM (SELECT price, changed_at, id FROM flight_price_history " +
                "WHERE flight_id = :flight ORDER BY changed_at DESC, id DESC LIMIT :limit) latest " +
                "ORDER BY changed_at, id",
            mapOf("flight" to flightId, "limit" to limit),
        ) { rs, _ -> PricePoint(rs.getBigDecimal("price"), rs.getTimestamp("changed_at").toInstant()) }
}
