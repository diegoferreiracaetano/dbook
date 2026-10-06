package com.dbook.application.payment.refund

import com.dbook.application.accommodation.RecordingRoomInventory
import com.dbook.application.accommodation.StayInventoryReleaser
import com.dbook.application.audit.FakeAuditLog
import com.dbook.application.booking.BookingInventoryReleaser
import com.dbook.application.common.RecordingOutboxWriter
import com.dbook.application.flight.SeatInventoryReleaser
import com.dbook.application.payment.RefundBookingUseCase
import com.dbook.application.payment.RefundCommand
import com.dbook.application.payment.RefundProcessor
import com.dbook.application.payment.RefundRegistrar
import com.dbook.application.payment.RefundSettler
import com.dbook.application.payment.RetryRefundUseCase
import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import com.dbook.domain.payment.RefundReason
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

// Shared "given": the clock is 2026-10-04 12:00. Booking 100 is CONFIRMED on a flight 16 days away (a $500 seat,
// payment 7), 101 is CONFIRMED on a flight leaving in 6 hours, 102 is still PENDING.
abstract class RefundUseCaseFixture {
    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    protected val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    protected val support = Actor(2, Role.SUPPORT)
    protected val superAdmin = Actor(1, Role.SUPER_ADMIN)

    private fun confirmed(
        id: Long,
        departure: LocalDateTime,
    ) = Booking(id, aFlight(id, BigDecimal("500.00"), departure), seatId = id * 10, customerId = 3)
        .confirm(paymentId = 7)

    protected val bookings =
        InMemoryBookings(
            confirmed(100, LocalDateTime.of(2026, 10, 20, 8, 0)),
            confirmed(101, LocalDateTime.of(2026, 10, 4, 18, 0)),
            Booking(102, aFlight(102, BigDecimal("500.00"), LocalDateTime.of(2026, 10, 20, 8, 0)), 1020, 3),
            Booking(103, aFlight(103, BigDecimal("500.00"), LocalDateTime.of(2026, 10, 20, 8, 0)), 1030, 3)
                .confirm(paymentId = 8, discount = BigDecimal("40.00")),
        )
    protected val refunds = InMemoryRefunds()
    protected val seats = RecordingSeats()
    protected val broadcaster = RecordingBroadcaster()
    protected val gateway = ScriptedGateway()
    protected val audit = FakeAuditLog()
    protected val outbox = RecordingOutboxWriter()
    protected val rooms = RecordingRoomInventory()
    protected val meters = SimpleMeterRegistry()

    private val registrar = RefundRegistrar(bookings, refunds, audit, clock)
    protected val settler =
        RefundSettler(
            refunds,
            bookings,
            BookingInventoryReleaser(listOf(SeatInventoryReleaser(seats, broadcaster), StayInventoryReleaser(rooms))),
            audit,
            outbox,
            meters,
            clock,
        )
    private val processor = RefundProcessor(gateway, settler)
    protected val refundBooking = RefundBookingUseCase(refunds, registrar, processor, meters)
    protected val retryRefund = RetryRefundUseCase(registrar, processor)

    protected fun command(
        bookingId: Long = 100,
        key: String = "key-1",
        actor: Actor = support,
        note: String? = null,
        override: Boolean = false,
    ) = RefundCommand(actor, bookingId, RefundReason.CUSTOMER_REQUEST, note, override, key)

    /** Runs [action] and then the after-commit callbacks, the way a successful commit would. */
    protected fun <T> committed(action: () -> T): T {
        TransactionSynchronizationManager.initSynchronization()
        try {
            val result = action()
            TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
            return result
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    protected fun counted(outcome: String): Double =
        meters.find("dbook.refund").tag("outcome", outcome).counter()?.count() ?: 0.0

    protected fun statusOf(bookingId: Long): BookingStatus = requireNotNull(bookings.findById(bookingId)).status
}
