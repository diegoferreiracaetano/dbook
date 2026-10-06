package com.dbook.domain.review

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import java.time.LocalDateTime

enum class ReviewSort { RECENT, RATING }

/** What anyone may read about a review: never the customer's id or e-mail, never the booking. */
data class PublicReview(
    val id: Long,
    val rating: Int,
    val comment: String,
    val author: String,
    val createdAt: LocalDateTime,
    val edited: Boolean,
)

/** How a destination is rated by its visible reviews; [distribution] has an entry for each rating from 1 to 5. */
data class RatingSummary(
    val average: Double?,
    val total: Long,
    val distribution: Map<Int, Long>,
)

data class DestinationReviews(
    val summary: RatingSummary,
    val reviews: PageResult<PublicReview>,
)

interface PublicReviewReader {
    fun ofAccommodation(
        accommodationId: Long,
        sort: ReviewSort,
        page: PageQuery,
    ): DestinationReviews

    fun ofDestination(
        iataCode: String,
        sort: ReviewSort,
        page: PageQuery,
    ): DestinationReviews
}

/** "Maria Silva" is shown as "Maria S."; one name stays as it is; an anonymized customer has no name to show. */
fun publicAuthorName(
    fullName: String,
    anonymized: Boolean,
): String {
    if (anonymized) return ANONYMOUS_AUTHOR
    val words = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> ANONYMOUS_AUTHOR
        words.size == 1 -> words[0]
        else -> "${words.first()} ${words.last().first().uppercaseChar()}."
    }
}

private const val ANONYMOUS_AUTHOR = "Cliente anônimo"
