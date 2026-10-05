package com.dbook.application.review.deletereviewusecase

import com.dbook.application.review.DeleteReviewUseCase
import com.dbook.application.review.ReviewUseCaseFixture
import com.dbook.domain.review.NotReviewOwnerException
import com.dbook.domain.review.ReviewNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OnlyTheAuthorDeletesAReviewTest : ReviewUseCaseFixture() {
    private val useCase = DeleteReviewUseCase(reviews)

    @Test
    fun `given the author when deleting then the review is gone`() {
        useCase.execute(1, author)

        assertNull(reviews.findById(1))
    }

    @Test
    fun `given someone else when deleting then it is refused and the review stays`() {
        assertFailsWith<NotReviewOwnerException> { useCase.execute(1, stranger) }

        assertNotNull(reviews.findById(1))
    }

    @Test
    fun `given no review when deleting then it is not found`() {
        assertFailsWith<ReviewNotFoundException> { useCase.execute(99, author) }
    }
}
