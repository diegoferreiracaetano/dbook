package com.dbook.domain.promo

import java.math.BigDecimal
import java.math.RoundingMode

private const val CENTS = 2
private val ONE_CENT = BigDecimal("0.01")

/** The most that can come off [amount] when it is paid for [bookingCount] bookings: one cent is left on each. */
fun maxDiscount(
    amount: BigDecimal,
    bookingCount: Int,
): BigDecimal = (amount - ONE_CENT * BigDecimal(bookingCount)).max(BigDecimal.ZERO)

/**
 * Splits a [discount] over bookings of the given [prices], in proportion to each price, to the cent: the shares add up
 * to exactly the discount and none takes a booking below one cent (so each booking still has something to refund). The
 * cents lost by rounding down go, one at a time, to the bookings that still have room.
 */
fun allocateDiscount(
    discount: BigDecimal,
    prices: List<BigDecimal>,
): List<BigDecimal> {
    val total = prices.fold(BigDecimal.ZERO, BigDecimal::add)
    require(discount >= BigDecimal.ZERO && discount <= maxDiscount(total, prices.size)) {
        "the discount cannot be more than the total minus one cent per booking"
    }
    val shares = prices.map { (discount * it).divide(total, CENTS, RoundingMode.DOWN) }.toMutableList()
    var left = discount - shares.fold(BigDecimal.ZERO, BigDecimal::add)
    while (left > BigDecimal.ZERO) {
        val room = prices.indices.firstOrNull { prices[it] - shares[it] - ONE_CENT >= ONE_CENT }
        checkNotNull(room) { "no room left to allocate the discount" }
        shares[room] += ONE_CENT
        left -= ONE_CENT
    }
    return shares
}
