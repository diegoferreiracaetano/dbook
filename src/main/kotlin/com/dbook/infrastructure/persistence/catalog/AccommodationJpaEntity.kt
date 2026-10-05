package com.dbook.infrastructure.persistence.catalog

import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import java.math.BigDecimal

// A Bookable like the flight (JOINED inheritance: its own columns live in `accommodation`). The room types are not
// mapped here: they are kept by their own adapter, because the inventory is counted night by night in SQL.
@Entity
@Table(name = "accommodation")
@PrimaryKeyJoinColumn(name = "id")
class AccommodationJpaEntity(
    id: Long? = null,
    title: String = "",
    price: BigDecimal = BigDecimal.ZERO,
    totalCapacity: Int = 0,
    active: Boolean = true,
    @ManyToOne
    @JoinColumn(name = "destination_airport_id")
    var destination: AirportJpaEntity,
    var address: String = "",
    var stars: Int = 1,
    var description: String? = null,
    var photoUrl: String? = null,
    var amenities: String = "",
) : BookableJpaEntity(id, title, price, totalCapacity, active)
