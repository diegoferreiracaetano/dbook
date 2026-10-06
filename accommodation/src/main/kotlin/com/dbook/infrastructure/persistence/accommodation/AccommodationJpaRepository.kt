package com.dbook.infrastructure.persistence.accommodation

import org.springframework.data.jpa.repository.JpaRepository

interface AccommodationJpaRepository : JpaRepository<AccommodationJpaEntity, Long>
