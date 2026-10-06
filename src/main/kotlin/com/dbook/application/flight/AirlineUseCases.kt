package com.dbook.application.flight

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.catalog.CatalogEntryInUseException
import com.dbook.domain.catalog.CatalogEntryNotFoundException
import com.dbook.domain.flight.Airline
import com.dbook.domain.flight.AirlineRepository
import com.dbook.domain.identity.Actor
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class AirlineCommand(
    val actor: Actor,
    val iataCode: String,
    val name: String,
)

private fun Airline.snapshot() = mapOf("id" to id, "iataCode" to iataCode, "name" to name)

@Observed(name = "dbook.usecase")
@Service
class ListAirlinesUseCase(
    private val airlineRepository: AirlineRepository,
) {
    fun execute(): List<Airline> = airlineRepository.findAll()
}

@Observed(name = "dbook.usecase")
@Service
class CreateAirlineUseCase(
    private val airlineRepository: AirlineRepository,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(command: AirlineCommand): Airline {
        val saved =
            airlineRepository.save(
                Airline(iataCode = command.iataCode.trim().uppercase(), name = command.name.trim()),
            )
        auditLog.record(
            AuditEvent(
                command.actor,
                AuditAction.AIRLINE_CREATED,
                requireNotNull(saved.id).toString(),
                after = saved.snapshot(),
            ),
        )
        return saved
    }
}

@Observed(name = "dbook.usecase")
@Service
class UpdateAirlineUseCase(
    private val airlineRepository: AirlineRepository,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(
        id: Long,
        command: AirlineCommand,
    ): Airline {
        val before = airlineRepository.findById(id) ?: throw CatalogEntryNotFoundException("Airline", id)
        val saved =
            airlineRepository.save(Airline(id, command.iataCode.trim().uppercase(), command.name.trim()))
        auditLog.record(
            AuditEvent(
                command.actor,
                AuditAction.AIRLINE_UPDATED,
                id.toString(),
                before = before.snapshot(),
                after = saved.snapshot(),
            ),
        )
        return saved
    }
}

// only an airline no flight refers to can go: removing one in use would break the flights' history
@Observed(name = "dbook.usecase")
@Service
class DeleteAirlineUseCase(
    private val airlineRepository: AirlineRepository,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(
        actor: Actor,
        id: Long,
    ) {
        val airline = airlineRepository.findById(id) ?: throw CatalogEntryNotFoundException("Airline", id)
        val flights = airlineRepository.flightCount(id)
        if (flights > 0) {
            throw CatalogEntryInUseException("Airline ${airline.iataCode}", flights)
        }
        airlineRepository.delete(id)
        auditLog.record(AuditEvent(actor, AuditAction.AIRLINE_DELETED, id.toString(), before = airline.snapshot()))
    }
}
