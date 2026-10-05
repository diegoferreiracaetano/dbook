package com.dbook.application.review.editreviewusecase

import com.dbook.application.review.EditReviewCommand
import com.dbook.application.review.EditReviewUseCase
import com.dbook.application.review.ReviewUseCaseFixture
import com.dbook.domain.review.NotReviewOwnerException
import com.dbook.domain.review.ReviewNotFoundException
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class TheAuthorEditsOnlyWhatTheyGiveTest : ReviewUseCaseFixture() {
    private val useCase = EditReviewUseCase(reviews, clock)

    @Test
    fun `given the author when only the rating is sent then the comment stays and the review is marked edited`() {
        val edited = useCase.execute(EditReviewCommand(1, author, rating = 2, comment = null))

        assertEquals(2, edited.rating)
        assertEquals("Good trip", edited.comment)
        assertEquals(LocalDateTime.of(2026, 10, 4, 12, 0), edited.updatedAt)
        assertNotNull(reviews.findById(1)?.updatedAt)
    }

    @Test
    fun `given someone else when editing then it is refused and nothing changes`() {
        assertFailsWith<NotReviewOwnerException> { useCase.execute(EditReviewCommand(1, stranger, 1, "hacked")) }

        assertEquals(4, reviews.findById(1)?.rating)
    }

    @Test
    fun `given no review when editing then it is not found, and an empty edit or a bad rating is refused`() {
        assertFailsWith<ReviewNotFoundException> { useCase.execute(EditReviewCommand(99, author, 3, null)) }
        assertFailsWith<IllegalArgumentException> { useCase.execute(EditReviewCommand(1, author, null, null)) }
        assertFailsWith<IllegalArgumentException> { useCase.execute(EditReviewCommand(1, author, 6, null)) }
        assertFailsWith<IllegalArgumentException> { useCase.execute(EditReviewCommand(1, author, null, "   ")) }
    }

    @Test
    fun `given a hidden review when its author edits it then it stays hidden`() {
        val edited = useCase.execute(EditReviewCommand(2, 8, 3, "Calmer now"))

        assertEquals(false, edited.isVisible)
    }
}
