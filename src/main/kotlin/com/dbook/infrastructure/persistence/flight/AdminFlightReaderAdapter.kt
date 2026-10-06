package com.dbook.infrastructure.persistence.flight

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.flight.AdminFlightDetail
import com.dbook.domain.flight.AdminFlightFilter
import com.dbook.domain.flight.AdminFlightReader
import com.dbook.domain.flight.AdminFlightSummary
import com.dbook.domain.flight.FlightStatus
import com.dbook.domain.flight.SeatClass
import com.dbook.infrastructure.persistence.common.queryPage
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp

@Repository
class AdminFlightReaderAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : AdminFlightReader {
    @Transactional(readOnly = true)
    override fun search(
        filter: AdminFlightFilter,
        page: PageQuery,
    ): PageResult<AdminFlightSummary> {
        val params = MapSqlParameterSource()
        val where = whereClause(filter, params)
        return jdbc.queryPage(
            countSql = "SELECT count(*) FROM flight f JOIN bookable b ON b.id = f.id " + JOINS + " WHERE $where",
            pageSql = pageSql(where),
            params = params,
            page = page,
            mapper = ::summaryOf,
        )
    }

    @Transactional(readOnly = true)
    override fun find(id: Long): AdminFlightDetail? {
        val flight =
            jdbc.query("$SELECT_ONE WHERE f.id = :id", mapOf("id" to id)) { rs, _ -> summaryOf(rs) }.firstOrNull()
                ?: return null
        val active =
            jdbc.queryForObject(
                "SELECT count(*) FROM booking WHERE bookable_id = :id AND status IN ('PENDING', 'CONFIRMED')",
                mapOf("id" to id),
                Long::class.javaObjectType,
            ) ?: 0L
        return AdminFlightDetail(flight, active)
    }

    @Transactional(readOnly = true)
    override fun exists(
        flightNumber: String,
        departureTime: java.time.LocalDateTime,
    ): Boolean =
        jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM flight WHERE flight_number = :number AND departure_time = :departure)",
            mapOf("number" to flightNumber, "departure" to Timestamp.valueOf(departureTime)),
            Boolean::class.javaObjectType,
        ) == true

    private fun whereClause(
        filter: AdminFlightFilter,
        params: MapSqlParameterSource,
    ): String {
        val conditions = mutableListOf("TRUE")
        filter.originIataCode?.let {
            params.addValue("origin", it)
            conditions += "o.iata_code = :origin"
        }
        filter.destinationIataCode?.let {
            params.addValue("destination", it)
            conditions += "d.iata_code = :destination"
        }
        filter.airlineIataCode?.let {
            params.addValue("airline", it)
            conditions += "al.iata_code = :airline"
        }
        filter.departureFrom?.let {
            params.addValue("from", Timestamp.valueOf(it))
            conditions += "f.departure_time >= :from"
        }
        filter.departureTo?.let {
            params.addValue("to", Timestamp.valueOf(it))
            conditions += "f.departure_time <= :to"
        }
        filter.status?.let { conditions += if (it == FlightStatus.SCHEDULED) "b.active" else "NOT b.active" }
        return conditions.joinToString(" AND ")
    }

    // The seat counts are subqueries per row, so the LIMIT goes inside, on the plain columns (see CustomerSql.pageSql)
    private fun pageSql(where: String) =
        """
        SELECT p.*,
               (SELECT count(*) FROM seat s WHERE s.bookable_id = p.id AND s.status = 'AVAILABLE') AS available_seats,
               (SELECT count(*) FROM seat s WHERE s.bookable_id = p.id AND s.status = 'RESERVED') AS reserved_seats
        FROM (SELECT $COLUMNS FROM flight f JOIN bookable b ON b.id = f.id $JOINS WHERE $where
              ORDER BY f.departure_time, f.id LIMIT :limit OFFSET :offset) p
        ORDER BY p.departure_time, p.id
        """

    private fun summaryOf(rs: ResultSet) =
        AdminFlightSummary(
            id = rs.getLong("id"),
            flightNumber = rs.getString("flight_number"),
            airlineIataCode = rs.getString("airline_code"),
            airlineName = rs.getString("airline_name"),
            originIataCode = rs.getString("origin_code"),
            destinationIataCode = rs.getString("destination_code"),
            departureTime = rs.getTimestamp("departure_time").toLocalDateTime(),
            arrivalTime = rs.getTimestamp("arrival_time").toLocalDateTime(),
            seatClass = SeatClass.valueOf(rs.getString("seat_class")),
            price = rs.getBigDecimal("price"),
            totalCapacity = rs.getInt("total_capacity"),
            availableSeats = rs.getInt("available_seats"),
            reservedSeats = rs.getInt("reserved_seats"),
            aircraftType = rs.getString("aircraft_type"),
            status = if (rs.getBoolean("active")) FlightStatus.SCHEDULED else FlightStatus.CANCELLED,
            version = rs.getLong("version"),
        )

    private companion object {
        const val COLUMNS =
            "f.id, f.flight_number, al.iata_code AS airline_code, al.name AS airline_name, " +
                "o.iata_code AS origin_code, d.iata_code AS destination_code, f.departure_time, f.arrival_time, " +
                "f.seat_class, b.price, b.total_capacity, f.aircraft_type, b.active, b.version"

        const val JOINS =
            "JOIN airline al ON al.id = f.airline_id JOIN airport o ON o.id = f.origin_airport_id " +
                "JOIN airport d ON d.id = f.destination_airport_id"

        const val SELECT_ONE =
            """
            SELECT $COLUMNS,
                   (SELECT count(*) FROM seat s
                    WHERE s.bookable_id = f.id AND s.status = 'AVAILABLE') AS available_seats,
                   (SELECT count(*) FROM seat s
                    WHERE s.bookable_id = f.id AND s.status = 'RESERVED') AS reserved_seats
            FROM flight f JOIN bookable b ON b.id = f.id $JOINS
            """
    }
}
