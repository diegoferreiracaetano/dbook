package com.dbook.infrastructure.persistence.review

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.review.DestinationReviews
import com.dbook.domain.review.PublicReview
import com.dbook.domain.review.PublicReviewReader
import com.dbook.domain.review.RatingSummary
import com.dbook.domain.review.ReviewSort
import com.dbook.domain.review.publicAuthorName
import com.dbook.infrastructure.persistence.common.queryPage
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class PublicReviewReaderAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : PublicReviewReader {
    // One transaction, so the summary and the page come from the same moment
    @Transactional(readOnly = true)
    override fun ofDestination(
        iataCode: String,
        sort: ReviewSort,
        page: PageQuery,
    ): DestinationReviews {
        val params = MapSqlParameterSource("iata", iataCode)
        return DestinationReviews(summary(DESTINATION, params), reviews(DESTINATION, params, sort, page))
    }

    // The reviews of one hotel
    @Transactional(readOnly = true)
    override fun ofAccommodation(
        accommodationId: Long,
        sort: ReviewSort,
        page: PageQuery,
    ): DestinationReviews {
        val params = MapSqlParameterSource("hotel", accommodationId)
        return DestinationReviews(summary(HOTEL, params), reviews(HOTEL, params, sort, page))
    }

    private fun summary(
        filter: String,
        params: MapSqlParameterSource,
    ): RatingSummary {
        val counts =
            jdbc.query(
                "SELECT r.rating, count(*) AS total FROM $FROM WHERE $filter AND $VISIBLE GROUP BY r.rating",
                params,
            ) { rs, _ -> rs.getInt("rating") to rs.getLong("total") }.toMap()
        val total = counts.values.sum()
        val average = if (total == 0L) null else counts.entries.sumOf { it.key * it.value }.toDouble() / total
        return RatingSummary(average, total, (1..MAX_RATING).associateWith { counts[it] ?: 0L })
    }

    private fun reviews(
        filter: String,
        params: MapSqlParameterSource,
        sort: ReviewSort,
        page: PageQuery,
    ): PageResult<PublicReview> {
        // the id closes every order: two reviews at the same instant never swap places between pages
        val order = if (sort == ReviewSort.RATING) "r.rating DESC, $RECENT_FIRST" else RECENT_FIRST
        return jdbc.queryPage(
            countSql = "SELECT count(*) FROM $FROM WHERE $filter AND $VISIBLE",
            pageSql =
                "SELECT r.id, r.rating, r.comment, r.created_at, r.updated_at, u.name, u.anonymized_at " +
                    "FROM $FROM WHERE $filter AND $VISIBLE ORDER BY $order LIMIT :limit OFFSET :offset",
            params = params,
            page = page,
        ) { rs ->
            PublicReview(
                id = rs.getLong("id"),
                rating = rs.getInt("rating"),
                comment = rs.getString("comment"),
                author = publicAuthorName(rs.getString("name"), rs.getTimestamp("anonymized_at") != null),
                createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
                edited = rs.getTimestamp("updated_at") != null,
            )
        }
    }

    private companion object {
        const val MAX_RATING = 5
        const val RECENT_FIRST = "r.created_at DESC, r.id DESC"

        // a review is of a booking, and a booking is of a flight or of a hotel: the destination is where either goes
        const val FROM =
            "review r JOIN booking b ON b.id = r.booking_id " +
                "LEFT JOIN flight f ON f.id = b.bookable_id LEFT JOIN accommodation ac ON ac.id = b.bookable_id " +
                "JOIN airport a ON a.id = COALESCE(f.destination_airport_id, ac.destination_airport_id) " +
                "JOIN app_user u ON u.id = r.customer_id"
        const val VISIBLE = "r.status = 'VISIBLE'"
        const val DESTINATION = "a.iata_code = :iata"
        const val HOTEL = "b.bookable_id = :hotel"
    }
}
