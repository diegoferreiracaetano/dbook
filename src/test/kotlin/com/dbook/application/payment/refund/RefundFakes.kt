package com.dbook.application.payment.refund

import com.dbook.domain.booking.AvailabilityBroadcaster
import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.catalog.Airport
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.flight.Airline
import com.dbook.domain.flight.Flight
import com.dbook.domain.flight.SeatClass
import com.dbook.domain.payment.PaymentGateway
import com.dbook.domain.payment.PaymentGatewayException
import com.dbook.domain.payment.Refund
import com.dbook.domain.payment.RefundAlreadyRequestedException
import com.dbook.domain.payment.RefundRepository
import com.dbook.domain.payment.RefundStatus
import com.dbook.domain.seating.Seat
import com.dbook.domain.seating.SeatRepository
import java.math.BigDecimal
import java.time.LocalDateTime

class InMemoryBookings(vararg initial: Booking) : BookingRepository {
    val store = initial.associateBy { requireNotNull(it.id) }.toMutableMap()

    override fun findById(id: Long): Booking? = store[id]

    override fun findByCustomerId(customerId: Long): List<Booking> = store.values.filter { it.customerId == customerId }

    override fun save(booking: Booking): Booking {
        store[requireNotNull(booking.id)] = booking
        return booking
    }

    override fun countPending(): Long = store.values.count { it.status == BookingStatus.PENDING }.toLong()
}

// Behaves like the table: one live (not FAILED) refund per booking, and the same (requester, key) only once.
class InMemoryRefunds : RefundRepository {
    private val refunds = mutableListOf<Refund>()
    private var lastId = 0L
    var hideKeyLookupOnce = false

    fun all(): List<Refund> = refunds.toList()

    override fun save(refund: Refund): Refund {
        val clash =
            refunds.any { it.bookingId == refund.bookingId && it.id != refund.id && it.status != RefundStatus.FAILED }
        if (refund.status != RefundStatus.FAILED && clash) {
            throw RefundAlreadyRequestedException()
        }
        val stored =
            if (refund.id == null) {
                with(refund) {
                    Refund(
                        ++lastId, paymentId, bookingId, amount, reason, note, status, idempotencyKey,
                        requestFingerprint, requestedBy, failureReason, createdAt, completedAt,
                    )
                }
            } else {
                refund
            }
        refunds.removeAll { it.id == stored.id }
        refunds += stored
        return stored
    }

    override fun findById(id: Long): Refund? = refunds.find { it.id == id }

    override fun findInProgressByBookingId(bookingId: Long): Refund? =
        refunds.find { it.bookingId == bookingId && it.status == RefundStatus.REQUESTED }

    override fun findByRequestedByAndIdempotencyKey(
        requestedBy: Long,
        idempotencyKey: String,
    ): Refund? {
        if (hideKeyLookupOnce) {
            hideKeyLookupOnce = false
            return null
        }
        return refunds.find { it.requestedBy == requestedBy && it.idempotencyKey == idempotencyKey }
    }

    override fun search(
        status: RefundStatus?,
        page: PageQuery,
    ): PageResult<Refund> {
        val found = refunds.filter { status == null || it.status == status }.sortedByDescending { it.id }
        return PageResult(found.drop(page.offset.toInt()).take(page.size), page, found.size.toLong())
    }
}

class RecordingSeats : SeatRepository {
    val released = mutableListOf<Long>()

    override fun release(seatId: Long): Seat {
        released += seatId
        return Seat(id = seatId, bookableId = 1, label = "1A")
    }

    override fun countAvailable(bookableId: Long): Int = released.size

    override fun findById(id: Long): Seat? = error("not needed for this test")

    override fun findByBookableId(bookableId: Long): List<Seat> = error("not needed for this test")

    override fun saveAll(seats: List<Seat>): List<Seat> = error("not needed for this test")

    override fun reserve(seatId: Long): Seat = error("not needed for this test")

    override fun deleteAll(seatIds: List<Long>) = error("not needed for this test")
}

class RecordingBroadcaster : AvailabilityBroadcaster {
    val broadcasts = mutableListOf<Pair<Long, Int>>()

    override fun broadcast(
        bookableId: Long,
        availableSeats: Int,
    ) {
        broadcasts += bookableId to availableSeats
    }
}

class ScriptedGateway : PaymentGateway {
    val calls = mutableListOf<Triple<Long, BigDecimal, String>>()
    var failure: String? = null

    override fun refund(
        paymentId: Long,
        amount: BigDecimal,
        idempotencyKey: String,
    ) {
        calls += Triple(paymentId, amount, idempotencyKey)
        failure?.let { throw PaymentGatewayException(it) }
    }
}

fun aFlight(
    id: Long,
    price: BigDecimal,
    departure: LocalDateTime,
) = Flight(
    id = id,
    title = "GRU-GIG",
    price = price,
    totalCapacity = 1,
    availableCapacity = 0,
    flightNumber = "DB1234",
    airline = Airline(id = 1, iataCode = "LA", name = "LATAM Airlines"),
    origin = anAirport(1, "GRU"),
    destination = anAirport(2, "GIG"),
    departureTime = departure,
    arrivalTime = departure.plusHours(1),
    seatClass = SeatClass.ECONOMY,
    aircraftType = "Airbus A320",
)

private fun anAirport(
    id: Long,
    code: String,
) = Airport(
    id = id,
    iataCode = code,
    name = code,
    city = code,
    country = "Brasil",
    photoUrl = "https://example.com/photo.jpg",
    region = "América do Sul",
    isPopular = false,
)
