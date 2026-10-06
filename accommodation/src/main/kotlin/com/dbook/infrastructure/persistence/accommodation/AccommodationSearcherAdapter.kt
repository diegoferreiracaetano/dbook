package com.dbook.infrastructure.persistence.accommodation

import com.dbook.domain.accommodation.AccommodationResult
import com.dbook.domain.accommodation.AccommodationSearch
import com.dbook.domain.accommodation.AccommodationSearcher
import com.dbook.domain.accommodation.AvailableRoom
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.Date

@Repository
class AccommodationSearcherAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : AccommodationSearcher {
    @Transactional(readOnly = true)
    override fun search(
        search: AccommodationSearch,
        page: PageQuery,
    ): PageResult<AccommodationResult> {
        val params =
            MapSqlParameterSource()
                .addValue("destination", search.destinationIataCode)
                .addValue("from", Date.valueOf(search.checkIn))
                .addValue("last", Date.valueOf(search.checkOut.minusDays(1)))
                .addValue("nights", search.nights)
                .addValue("guests", search.guests)
        val total =
            jdbc.queryForObject(
                "$AVAILABLE SELECT count(DISTINCT accommodation_id) FROM available",
                params,
                Long::class.javaObjectType,
            ) ?: 0L
        if (page.offset >= total) {
            return PageResult(emptyList(), page, total)
        }
        params.addValue("limit", page.size).addValue("offset", page.offset)
        val rows = jdbc.query(PAGE_SQL, params) { rs, _ -> rowOf(rs) }
        val hotels =
            rows.groupBy { it.hotel.id }.values.map { group ->
                group.first().hotel.copy(rooms = group.map { it.room })
            }
        return PageResult(hotels, page, total)
    }

    private data class Row(
        val hotel: AccommodationResult,
        val room: AvailableRoom,
    )

    private fun rowOf(rs: java.sql.ResultSet): Row {
        val room =
            AvailableRoom(
                roomTypeId = rs.getLong("room_id"),
                name = rs.getString("room_name"),
                capacity = rs.getInt("capacity"),
                nightlyRate = rs.getBigDecimal("nightly_rate"),
                totalPrice = rs.getBigDecimal("total"),
            )
        val hotel =
            AccommodationResult(
                id = rs.getLong("accommodation_id"),
                name = rs.getString("title"),
                destinationIataCode = rs.getString("iata_code"),
                city = rs.getString("city"),
                address = rs.getString("address"),
                stars = rs.getInt("stars"),
                photoUrl = rs.getString("photo_url"),
                amenities = rs.getString("amenities").split(',').filter { it.isNotBlank() }.toSet(),
                averageRating = rs.getBigDecimal("rating")?.toDouble(),
                reviewCount = rs.getLong("reviews"),
                fromPrice = rs.getBigDecimal("from_price") ?: BigDecimal.ZERO,
                rooms = emptyList(),
            )
        return Row(hotel, room)
    }

    private companion object {
        // The room types that are free on every night of the stay and take the guests. A night without a row has no
        // room taken, so the join finds nothing and the count is zero.
        const val AVAILABLE =
            """
            WITH available AS (
                SELECT rt.accommodation_id, rt.id AS room_id, rt.name AS room_name, rt.capacity, rt.nightly_rate,
                       rt.nightly_rate * :nights AS total
                FROM room_type rt
                JOIN accommodation ac ON ac.id = rt.accommodation_id
                JOIN bookable b ON b.id = ac.id AND b.active
                JOIN airport a ON a.id = ac.destination_airport_id AND a.iata_code = :destination
                WHERE rt.active AND rt.capacity >= :guests
                  AND NOT EXISTS (
                      SELECT 1 FROM generate_series(:from::date, :last::date, interval '1 day') AS n(night)
                      LEFT JOIN room_night rn ON rn.room_type_id = rt.id AND rn.night = n.night::date
                      WHERE COALESCE(rn.booked, 0) >= rt.quantity)
            )
            """

        // The page is cut first, on the hotels, and only then are their rooms and their rating read.
        const val PAGE_SQL =
            AVAILABLE +
                """
                , page AS (
                    SELECT accommodation_id, min(total) AS from_price FROM available
                    GROUP BY accommodation_id ORDER BY from_price, accommodation_id LIMIT :limit OFFSET :offset
                )
                SELECT p.from_price, v.accommodation_id, v.room_id, v.room_name, v.capacity, v.nightly_rate, v.total,
                       b.title, ap.iata_code, ap.city, ac.address, ac.stars, ac.photo_url, ac.amenities,
                       (SELECT avg(r.rating) FROM review r JOIN booking bk ON bk.id = r.booking_id
                        WHERE bk.bookable_id = p.accommodation_id AND r.status = 'VISIBLE') AS rating,
                       (SELECT count(*) FROM review r JOIN booking bk ON bk.id = r.booking_id
                        WHERE bk.bookable_id = p.accommodation_id AND r.status = 'VISIBLE') AS reviews
                FROM page p
                JOIN available v ON v.accommodation_id = p.accommodation_id
                JOIN bookable b ON b.id = p.accommodation_id
                JOIN accommodation ac ON ac.id = p.accommodation_id
                JOIN airport ap ON ap.id = ac.destination_airport_id
                ORDER BY p.from_price, p.accommodation_id, v.total, v.room_id
                """
    }
}
