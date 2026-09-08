package com.dbook.infrastructure.persistence

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal

@Entity
@Table(name = "bookable")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class BookableJpaEntity(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,
	var title: String = "",
	var price: BigDecimal = BigDecimal.ZERO,
	var totalCapacity: Int = 0,
	var availableCapacity: Int = 0,
	var active: Boolean = true,
	@Version
	var version: Long = 0,
)
