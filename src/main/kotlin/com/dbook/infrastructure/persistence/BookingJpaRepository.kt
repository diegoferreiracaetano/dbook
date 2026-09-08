package com.dbook.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface BookingJpaRepository : JpaRepository<BookingJpaEntity, Long>
