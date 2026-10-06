package com.dbook.infrastructure.persistence.booking

import com.dbook.domain.booking.BookingStatus
import com.dbook.infrastructure.persistence.catalog.BookableJpaEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "booking")
class BookingJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne
    @JoinColumn(name = "bookable_id")
    var bookable: BookableJpaEntity,
    // plain ids, not relations: the seat belongs to flight and the payment to payment; the foreign keys are in the
    // database, and booking does not need their entities to be read or written
    var seatId: Long?,
    var customerId: Long = 0,
    @Enumerated(EnumType.STRING)
    var status: BookingStatus = BookingStatus.PENDING,
    var paymentId: Long? = null,
    @Version
    var version: Long = 0,
    var price: BigDecimal = BigDecimal.ZERO,
    var discount: BigDecimal = BigDecimal.ZERO,
    // a stay (a room for some nights) instead of a seat
    var roomTypeId: Long? = null,
    var checkIn: LocalDate? = null,
    var checkOut: LocalDate? = null,
    var guests: Int? = null,
    var nightlyRate: BigDecimal? = null,
)
