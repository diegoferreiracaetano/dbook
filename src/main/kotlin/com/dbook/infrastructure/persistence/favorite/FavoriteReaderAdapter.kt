package com.dbook.infrastructure.persistence.favorite

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.favorite.FavoriteDestination
import com.dbook.domain.favorite.FavoriteFlight
import com.dbook.domain.favorite.FavoriteReader
import com.dbook.domain.favorite.FavoriteType
import com.dbook.domain.favorite.FavoriteView
import com.dbook.infrastructure.persistence.common.queryPage
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

@Repository
class FavoriteReaderAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : FavoriteReader {
    @Transactional(readOnly = true)
    override fun list(
        userId: Long,
        type: FavoriteType?,
        page: PageQuery,
    ): PageResult<FavoriteView> {
        val params = MapSqlParameterSource("user", userId)
        val where =
            if (type == null) {
                "f.user_id = :user"
            } else {
                params.addValue("type", type.name)
                "f.user_id = :user AND f.target_type = :type"
            }
        return jdbc.queryPage(
            countSql = "SELECT count(*) FROM favorite f WHERE $where",
            pageSql = pageSql(where),
            params = params,
            page = page,
            mapper = ::viewOf,
        )
    }

    // The page is cut first, on the favorite's own columns, and only those rows are joined to what they point at.
    // The CASE keeps the cast of a flight id from ever running on a destination's code.
    private fun pageSql(where: String) =
        """
        SELECT p.target_type, p.target_id, p.created_at,
               ap.iata_code, ap.name AS airport_name, ap.city, ap.country, ap.photo_url,
               fl.id AS flight_id, fl.flight_number, fl.departure_time, o.iata_code AS origin_code,
               d.iata_code AS destination_code, b.price, b.active
        FROM (SELECT f.* FROM favorite f WHERE $where ORDER BY f.created_at DESC, f.id DESC LIMIT :limit OFFSET :offset) p
        LEFT JOIN airport ap ON ap.iata_code = CASE WHEN p.target_type = 'DESTINATION' THEN p.target_id END
        LEFT JOIN flight fl ON fl.id = CASE WHEN p.target_type = 'FLIGHT' THEN CAST(p.target_id AS BIGINT) END
        LEFT JOIN bookable b ON b.id = fl.id
        LEFT JOIN airport o ON o.id = fl.origin_airport_id
        LEFT JOIN airport d ON d.id = fl.destination_airport_id
        ORDER BY p.created_at DESC, p.id DESC
        """

    private fun viewOf(rs: ResultSet): FavoriteView {
        val type = FavoriteType.valueOf(rs.getString("target_type"))
        return FavoriteView(
            type = type,
            targetId = rs.getString("target_id"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            destination =
                if (type == FavoriteType.DESTINATION) {
                    rs.getString("iata_code")?.let {
                        FavoriteDestination(
                            it,
                            rs.getString("airport_name"),
                            rs.getString("city"),
                            rs.getString("country"),
                            rs.getString("photo_url"),
                        )
                    }
                } else {
                    null
                },
            flight =
                if (type == FavoriteType.FLIGHT) {
                    rs.getString("flight_number")?.let {
                        FavoriteFlight(
                            rs.getLong("flight_id"),
                            it,
                            rs.getString("origin_code"),
                            rs.getString("destination_code"),
                            rs.getTimestamp("departure_time").toLocalDateTime(),
                            rs.getBigDecimal("price"),
                            rs.getBoolean("active"),
                        )
                    }
                } else {
                    null
                },
        )
    }
}
