package com.dbook.infrastructure.persistence.catalog

import org.springframework.data.jpa.repository.JpaRepository

interface BookableJpaRepository : JpaRepository<BookableJpaEntity, Long>
