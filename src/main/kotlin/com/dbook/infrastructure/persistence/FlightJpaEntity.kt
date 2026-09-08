package com.dbook.infrastructure.persistence

import com.dbook.domain.SeatClass
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "flight")
@PrimaryKeyJoinColumn(name = "id")
class FlightJpaEntity(
    id: Long? = null,
    title: String = "",
    price: BigDecimal = BigDecimal.ZERO,
    totalCapacity: Int = 0,
    availableCapacity: Int = 0,
    active: Boolean = true,
    var flightNumber: String = "",
    @ManyToOne
    @JoinColumn(name = "origin_airport_id")
    var origin: AirportJpaEntity,
    @ManyToOne
    @JoinColumn(name = "destination_airport_id")
    var destination: AirportJpaEntity,
    var departureTime: LocalDateTime = LocalDateTime.now(),
    var arrivalTime: LocalDateTime = LocalDateTime.now(),
    @Enumerated(EnumType.STRING)
    var seatClass: SeatClass = SeatClass.ECONOMY,
) : BookableJpaEntity(id, title, price, totalCapacity, availableCapacity, active)
