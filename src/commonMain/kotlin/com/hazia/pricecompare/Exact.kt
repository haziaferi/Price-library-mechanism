package com.hazia.pricecompare

/**
 * Whole-number arithmetic for prices and sizes (SPEC:R04). Nothing is divided
 * until a value is displayed, and division rounds half up. The bounds on
 * [Money] and [Quantity] keep every product here inside a Long.
 */
internal object Exact {

    /** Compares price A per size A with price B per size B by cross-multiplying. */
    fun compareUnitPrices(priceA: Long, sizeA: Long, priceB: Long, sizeB: Long): Int =
        (priceA * sizeB).compareTo(priceB * sizeA)

    /** numerator / denominator rounded half up, for non-negative numerators. */
    fun divideHalfUp(numerator: Long, denominator: Long): Long {
        require(denominator > 0 && numerator >= 0)
        return (2 * numerator + denominator) / (2 * denominator)
    }

    /** Cents per [reference] for [price] cents per [size], rounded half up (SPEC:R34). */
    fun unitPriceCents(price: Long, size: Long, reference: Long): Long =
        divideHalfUp(price * reference, size)

    /** Whole-percent gap of A over the cheapest B, rounded half up (SPEC:R27). */
    fun gapPercent(priceA: Long, sizeA: Long, priceBest: Long, sizeBest: Long): Int =
        (divideHalfUp(priceA * sizeBest * 100, priceBest * sizeA) - 100).toInt()

    /** [cents] less [percent] percent, rounded half up to the cent (SPEC:R22). */
    fun percentOff(cents: Long, percent: Int): Long = divideHalfUp(cents * (100 - percent), 100)
}
