package com.dbook.infrastructure.persistence

import com.dbook.domain.BookingStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "booking")
class BookingJpaEntity(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,
	@ManyToOne
	@JoinColumn(name = "bookable_id")
	var bookable: BookableJpaEntity,
	var customerId: Long = 0,
	@Enumerated(EnumType.STRING)
	var status: BookingStatus = BookingStatus.PENDING,
)
