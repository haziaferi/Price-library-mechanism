package com.hazia.pricecompare

/**
 * One unit of a product with a given size, as sold (SPEC:R12, R13). Ids come
 * from the host app (SPEC:R06).
 *
 * A multipack is [count] × [unitSize], one level only. A loose good is entered
 * as a price per [unitSize] (1 kg or 1 piece) with [loose] set.
 */
data class Pack(
    val id: String,
    val productId: String,
    val productName: String,
    val unitSize: Quantity,
    val count: Int = 1,
    val loose: Boolean = false,
) {
    init {
        require(count >= 1) { "A pack holds at least one unit" }
    }

    val size: Quantity get() = unitSize * count
    val dimension: Dimension get() = unitSize.dimension
}

/** One observation of a pack's shelf price at a store on a day (SPEC:R16). */
data class PriceObservation(
    val packId: String,
    val storeId: String,
    val price: Money,
    val seenOn: Day,
)

/** The v1 discount kinds on one pack at one store (SPEC:R21). */
sealed interface Discount {
    /** The shelf price less [percent] (SPEC:R22). */
    data class PercentOff(val percent: Int) : Discount {
        init {
            require(percent in 1..99) { "A percentage off is 1..99, was $percent" }
        }
    }

    /** Take [take] packs, pay for [pay]: "2 x 1" and "3 x 2". */
    sealed class Multibuy(val take: Int, val pay: Int) : Discount

    data object TwoForOne : Multibuy(2, 1)
    data object ThreeForTwo : Multibuy(3, 2)
}

/**
 * A discount on one pack at one store (SPEC:R23, R24). [shelfPrice] is the
 * per-pack price the discount applies to; [labelPrice] overwrites the worked-out
 * price of a percentage off with the one on the shelf label (SPEC:R22).
 */
data class Promotion(
    val packId: String,
    val storeId: String,
    val discount: Discount,
    val shelfPrice: Money,
    val start: Day,
    val end: Day? = null,
    val labelPrice: Money? = null,
) {
    fun isActiveOn(day: Day): Boolean = day >= start && (end == null || day <= end)
}

/** The user's mark that a store does or does not carry a pack (SPEC:R19). */
data class CarriedMark(
    val packId: String,
    val storeId: String,
    val carried: Boolean,
    val on: Day,
)
