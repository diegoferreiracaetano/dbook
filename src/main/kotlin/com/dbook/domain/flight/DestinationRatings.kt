package com.dbook.domain.flight

/**
 * How travellers rate a destination, for the destination cards: implemented by whoever keeps the reviews (the `review`
 * concept). The flights do not know where the ratings come from, only that there may be none yet.
 */
interface DestinationRatings {
    /** The average rating (1-5) of the visible reviews of flights to the destination, or null when there is none. */
    fun averageRating(destinationIataCode: String): Double?
}
