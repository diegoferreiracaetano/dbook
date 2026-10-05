package com.dbook.application.accommodation

import com.dbook.application.audit.FakeAuditLog
import com.dbook.application.common.RecordingOutboxWriter
import com.dbook.domain.accommodation.Accommodation
import com.dbook.domain.accommodation.RoomType
import com.dbook.domain.catalog.Airport
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Hotel 1 (GIG, on sale) has room type 10 (Double: 2 guests, 300.00, one room only) and 11 (Suite: 4 guests, 800.00,
// 3 rooms, off sale); hotel 2 is off sale. The clock is 2026-10-04 12:00.
abstract class StayFixture {
    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    protected val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    protected val gig =
        Airport(2, "GIG", "Galeão", "Rio de Janeiro", "Brasil", "https://example.com/p.jpg", "América do Sul", true)

    protected val double = RoomType(10, "Double", 2, BigDecimal("300.00"), 1)
    protected val suite = RoomType(11, "Suite", 4, BigDecimal("800.00"), 3, active = false)
    protected val hotel =
        Accommodation(1, "Copacabana", gig, "Av. Atlântica", 4, roomTypes = listOf(double, suite))
    protected val offSale =
        Accommodation(2, "Closed", gig, "Rua 1", 3, roomTypes = listOf(double.copy(id = 12)), active = false)

    protected val accommodations = InMemoryAccommodations(hotel, offSale)
    protected val inventory = RecordingRoomInventory(quantities = mapOf(10L to 1, 11L to 3, 12L to 1))
    protected val bookings = AssigningBookings()
    protected val outbox = RecordingOutboxWriter()
    protected val audit = FakeAuditLog()

    protected fun day(date: String): LocalDate = LocalDate.parse(date)
}
