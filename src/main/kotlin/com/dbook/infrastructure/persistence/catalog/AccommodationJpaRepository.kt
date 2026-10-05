package com.dbook.infrastructure.persistence.catalog

import org.springframework.data.jpa.repository.JpaRepository

interface AccommodationJpaRepository : JpaRepository<AccommodationJpaEntity, Long>
