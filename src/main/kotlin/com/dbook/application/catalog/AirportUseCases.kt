package com.dbook.application.catalog

import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.AirportRepository
import com.dbook.domain.catalog.CatalogEntryInUseException
import com.dbook.domain.catalog.CatalogEntryNotFoundException
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class AirportCommand(
    val actor: Actor,
    val iataCode: String,
    val name: String,
    val city: String,
    val country: String,
    val photoUrl: String,
    val region: String,
    val isPopular: Boolean,
) {
    fun toAirport(id: Long? = null) =
        Airport(
            id,
            iataCode.trim().uppercase(),
            name.trim(),
            city.trim(),
            country.trim(),
            photoUrl.trim(),
            region.trim(),
            isPopular,
        )
}

private fun Airport.snapshot() =
    mapOf(
        "id" to id,
        "iataCode" to iataCode,
        "name" to name,
        "city" to city,
        "country" to country,
        "isPopular" to isPopular,
    )

@Observed(name = "dbook.usecase")
@Service
class ListAirportsUseCase(
    private val airportRepository: AirportRepository,
) {
    fun execute(): List<Airport> = airportRepository.findAll()
}

@Observed(name = "dbook.usecase")
@Service
class CreateAirportUseCase(
    private val airportRepository: AirportRepository,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(command: AirportCommand): Airport {
        val saved = airportRepository.save(command.toAirport())
        auditLog.record(
            AuditEvent(
                command.actor,
                AuditAction.AIRPORT_CREATED,
                requireNotNull(saved.id).toString(),
                after = saved.snapshot(),
            ),
        )
        return saved
    }
}

@Observed(name = "dbook.usecase")
@Service
class UpdateAirportUseCase(
    private val airportRepository: AirportRepository,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(
        id: Long,
        command: AirportCommand,
    ): Airport {
        val before = airportRepository.findById(id) ?: throw CatalogEntryNotFoundException("Airport", id)
        val saved = airportRepository.save(command.toAirport(id))
        auditLog.record(
            AuditEvent(
                command.actor,
                AuditAction.AIRPORT_UPDATED,
                id.toString(),
                before = before.snapshot(),
                after = saved.snapshot(),
            ),
        )
        return saved
    }
}

// only an airport no flight leaves from or arrives at can go
@Observed(name = "dbook.usecase")
@Service
class DeleteAirportUseCase(
    private val airportRepository: AirportRepository,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(
        actor: Actor,
        id: Long,
    ) {
        val airport = airportRepository.findById(id) ?: throw CatalogEntryNotFoundException("Airport", id)
        val flights = airportRepository.flightCount(id)
        if (flights > 0) {
            throw CatalogEntryInUseException("Airport ${airport.iataCode}", flights)
        }
        airportRepository.delete(id)
        auditLog.record(AuditEvent(actor, AuditAction.AIRPORT_DELETED, id.toString(), before = airport.snapshot()))
    }
}
