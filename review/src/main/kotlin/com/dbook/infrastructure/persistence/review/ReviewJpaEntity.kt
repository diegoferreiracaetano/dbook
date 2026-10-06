package com.dbook.infrastructure.persistence.review

import com.dbook.domain.review.ReviewStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "review")
class ReviewJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var bookingId: Long = 0,
    var customerId: Long = 0,
    var rating: Int = 0,
    var comment: String = "",
    var createdAt: LocalDateTime,
    @Enumerated(EnumType.STRING)
    var status: ReviewStatus = ReviewStatus.VISIBLE,
    var updatedAt: LocalDateTime? = null,
    var hiddenReason: String? = null,
    var hiddenBy: Long? = null,
    var hiddenAt: LocalDateTime? = null,
)
