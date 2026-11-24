package com.hazia.pricecompare

/**
 * What a pack costs at one store on one day: [total] for [packCount] packs.
 * A multibuy offers a bundle (SPEC:R31); everything else offers one pack.
 */
data class Offer(
    val total: Money,
    val packCount: Int,
    val seenOn: Day,
    val isPromotion: Boolean = false,
    /** A promotion that ended with no regular price to fall back on (SPEC:R24). */
    val noRegularPrice: Boolean = false,
    /** A regular price older than [STALE_AFTER_DAYS] on the ranking day (SPEC:R33). */
    val stale: Boolean = false,
) {
    /** The bundle a multibuy row is labelled with ("when buying 3"), or null. */
    val bundleOf: Int? get() = packCount.takeIf { it > 1 }

    companion object {
        const val STALE_AFTER_DAYS = 60
    }
}

object Pricing {

    /** The price one pack costs under [discount] alone, rounded half up (SPEC:R21, R22). */
    fun discountedPackPrice(shelfPrice: Money, discount: Discount.PercentOff, labelPrice: Money? = null): Money =
        labelPrice ?: Money(Exact.percentOff(shelfPrice.cents, discount.percent), shelfPrice.currency)

    /** A promotion as an offer: one discounted pack, or a whole multibuy bundle (SPEC:R31). */
    fun offerOf(promotion: Promotion): Offer = when (val d = promotion.discount) {
        is Discount.PercentOff -> Offer(
            total = discountedPackPrice(promotion.shelfPrice, d, promotion.labelPrice),
            packCount = 1,
            seenOn = promotion.start,
            isPromotion = true,
        )
        is Discount.Multibuy -> Offer(
            total = Money(promotion.shelfPrice.cents * d.pay, promotion.shelfPrice.currency),
            packCount = d.take,
            seenOn = promotion.start,
            isPromotion = true,
        )
    }

    /**
     * The pack's current offer at [storeId] on [on] (SPEC:R16, R24, R25, R33):
     * an active promotion (the latest started, if two overlap); else the newest
     * regular price seen by that day; else the last ended promotion, flagged as
     * having no regular price. Null when the store has nothing for the pack.
     */
    fun currentOffer(packId: String, storeId: String, book: PriceBook, on: Day): Offer? {
        val promotions = book.promotions.filter { it.packId == packId && it.storeId == storeId && it.start <= on }
        promotions.filter { it.isActiveOn(on) }.maxByOrNull { it.start }?.let { return offerOf(it) }

        val regular = book.prices
            .filter { it.packId == packId && it.storeId == storeId && it.seenOn <= on }
            .maxByOrNull { it.seenOn }
        if (regular != null) {
            return Offer(
                total = regular.price,
                packCount = 1,
                seenOn = regular.seenOn,
                stale = on.epochDay - regular.seenOn.epochDay > Offer.STALE_AFTER_DAYS,
            )
        }

        return promotions.maxByOrNull { it.start }?.let { offerOf(it).copy(noRegularPrice = true) }
    }

    /** The stores with any price or promotion for [packId]. */
    internal fun storesOf(packId: String, book: PriceBook): Set<String> =
        (book.prices.filter { it.packId == packId }.map { it.storeId } +
            book.promotions.filter { it.packId == packId }.map { it.storeId }).toSet()

    /**
     * Whether [storeId] carries [packId] (SPEC:R19): it has a price there or the
     * user marked it carried; between a mark and a price, the newer wins.
     */
    fun isCarried(packId: String, storeId: String, prices: List<PriceObservation>, marks: List<CarriedMark>): Boolean {
        val lastPrice = prices.filter { it.packId == packId && it.storeId == storeId }.maxOfOrNull { it.seenOn.epochDay }
        val lastMark = marks.filter { it.packId == packId && it.storeId == storeId }.maxByOrNull { it.on }
        return when {
            lastMark == null -> lastPrice != null
            lastPrice == null -> lastMark.carried
            lastPrice > lastMark.on.epochDay -> true
            else -> lastMark.carried
        }
    }
}
