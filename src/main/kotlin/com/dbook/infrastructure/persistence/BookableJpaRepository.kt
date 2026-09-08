package com.dbook.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface BookableJpaRepository : JpaRepository<BookableJpaEntity, Long>
