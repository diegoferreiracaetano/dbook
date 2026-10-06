package com.dbook.domain.review

class ReviewNotFoundException(id: Long) : RuntimeException("Review not found: $id")

class NotReviewOwnerException(id: Long) : RuntimeException("Review $id does not belong to the authenticated user")
