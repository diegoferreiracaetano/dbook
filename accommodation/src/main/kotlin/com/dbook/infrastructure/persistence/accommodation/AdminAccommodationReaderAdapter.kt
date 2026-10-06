package com.dbook.infrastructure.persistence.accommodation

import com.dbook.domain.accommodation.Accommodation
import com.dbook.domain.accommodation.AccommodationRepository
import com.dbook.domain.accommodation.AdminAccommodationReader
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.infrastructure.persistence.common.queryPage
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class AdminAccommodationReaderAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val accommodations: AccommodationRepository,
) : AdminAccommodationReader {
    @Transactional(readOnly = true)
    override fun search(
        destinationIataCode: String?,
        page: PageQuery,
    ): PageResult<Accommodation> {
        val params = MapSqlParameterSource()
        val where =
            if (destinationIataCode == null) {
                "TRUE"
            } else {
                params.addValue("destination", destinationIataCode)
                "a.iata_code = :destination"
            }
        val ids =
            jdbc.queryPage(
                countSql =
                    "SELECT count(*) FROM accommodation ac JOIN airport a ON a.id = ac.destination_airport_id " +
                        "WHERE $where",
                pageSql =
                    "SELECT ac.id FROM accommodation ac JOIN airport a ON a.id = ac.destination_airport_id " +
                        "WHERE $where ORDER BY ac.id DESC LIMIT :limit OFFSET :offset",
                params = params,
                page = page,
            ) { it.getLong("id") }
        return PageResult(ids.items.mapNotNull { accommodations.findById(it) }, page, ids.totalElements)
    }
}
