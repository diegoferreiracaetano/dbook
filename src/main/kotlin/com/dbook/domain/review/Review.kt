package com.dbook.domain.review

import java.time.LocalDateTime

private const val MAX_RATING = 5
private const val MAX_COMMENT_LENGTH = 2000
private const val MIN_REASON_LENGTH = 10
private const val MAX_REASON_LENGTH = 500

enum class ReviewStatus { VISIBLE, HIDDEN }

/**
 * What a customer wrote about a trip. The author can edit it; the team can hide it (it then stops showing and
 * counting, but is kept) and restore it. The average of a destination is always derived from the visible reviews,
 * never stored, so no edit, delete or hide can leave it out of sync.
 */
data class Review(
    val id: Long? = null,
    val bookingId: Long,
    val customerId: Long,
    val rating: Int,
    val comment: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val status: ReviewStatus = ReviewStatus.VISIBLE,
    val updatedAt: LocalDateTime? = null,
    val hiddenReason: String? = null,
    val hiddenBy: Long? = null,
    val hiddenAt: LocalDateTime? = null,
) {
    init {
        require(rating in 1..MAX_RATING) { "rating must be between 1 and $MAX_RATING" }
        require(comment.isNotBlank()) { "comment must not be blank" }
        require(comment.length <= MAX_COMMENT_LENGTH) { "comment must have at most $MAX_COMMENT_LENGTH characters" }
    }

    val isVisible: Boolean get() = status == ReviewStatus.VISIBLE

    /** Only what is given changes. Editing a hidden review does not make it visible again: only the team does that. */
    fun edit(
        rating: Int?,
        comment: String?,
        now: LocalDateTime,
    ): Review {
        require(rating != null || comment != null) { "nothing to change: send a rating, a comment or both" }
        return copy(rating = rating ?: this.rating, comment = comment?.trim() ?: this.comment, updatedAt = now)
    }

    fun hide(
        reason: String,
        staffId: Long,
        now: LocalDateTime,
    ): Review {
        check(isVisible) { "Review $id is already hidden" }
        val clean = reason.trim()
        require(clean.length in MIN_REASON_LENGTH..MAX_REASON_LENGTH) {
            "reason must have between $MIN_REASON_LENGTH and $MAX_REASON_LENGTH characters"
        }
        return copy(status = ReviewStatus.HIDDEN, hiddenReason = clean, hiddenBy = staffId, hiddenAt = now)
    }

    fun restore(): Review {
        check(!isVisible) { "Review $id is not hidden" }
        return copy(status = ReviewStatus.VISIBLE, hiddenReason = null, hiddenBy = null, hiddenAt = null)
    }
}
