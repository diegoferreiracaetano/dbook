package com.dbook.infrastructure.persistence

import com.dbook.domain.SeatStatus
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

@Entity
@Table(name = "seat")
class SeatJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne
    @JoinColumn(name = "bookable_id")
    var bookable: BookableJpaEntity,
    var label: String = "",
    @Enumerated(EnumType.STRING)
    var status: SeatStatus = SeatStatus.AVAILABLE,
    @Version
    var version: Long = 0,
)
