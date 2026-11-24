package com.hazia.pricecompare

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PricingTest {

    // SPEC:R10 — each unit converts to its base unit exactly.
    @Test
    fun unitsConvertExactly() {
        assertEquals(1_000_000L, Quantity.of(1, MeasureUnit.KILOGRAM).base)
        assertEquals(100L, Quantity.of(1, MeasureUnit.DECILITRE).base)
        assertEquals(1_000L, Quantity.of(1, MeasureUnit.METRE).base)
        assertEquals(Dimension.LENGTH, MeasureUnit.CENTIMETRE.dimension)
    }

    // SPEC:R12 — multipacks are one level: 6 × 330 ml and 12 × 1.5 L.
    @Test
    fun multipackSizes() {
        assertEquals(1_980L, pack("cans", "Cola", ml(330), count = 6).size.base)
        assertEquals(18_000L, pack("water", "Water", ml(1_500), count = 12).size.base)
    }

    // SPEC:R21 — "2 x 1": two packs at 1,59 € cost 1,59 €.
    @Test
    fun twoForOne() {
        val offer = Pricing.offerOf(Promotion("p", "A", Discount.TwoForOne, eur(159), start = TODAY))
        assertEquals(159L, offer.total.cents)
        assertEquals(2, offer.packCount)
    }

    // SPEC:R21 — "3 x 2": three packs at 1,59 € cost 3,18 €.
    @Test
    fun threeForTwo() {
        val offer = Pricing.offerOf(Promotion("p", "A", Discount.ThreeForTwo, eur(159), start = TODAY))
        assertEquals(318L, offer.total.cents)
        assertEquals(3, offer.packCount)
    }

    // SPEC:R21, R22 — 2,00 € at 25% off is 1,50 €; 1,99 € at 30% off rounds half up to 1,39 €.
    @Test
    fun percentOffRoundsHalfUp() {
        assertEquals(150L, Pricing.discountedPackPrice(eur(200), Discount.PercentOff(25)).cents)
        assertEquals(139L, Pricing.discountedPackPrice(eur(199), Discount.PercentOff(30)).cents)
    }

    // SPEC:R22 — the shelf label's price overwrites the worked-out one.
    @Test
    fun labelPriceOverwritesTheWorkedOutPrice() {
        val promotion = Promotion("p", "A", Discount.PercentOff(30), eur(199), start = TODAY, labelPrice = eur(140))
        assertEquals(140L, Pricing.offerOf(promotion).total.cents)
    }

    // SPEC:R23 — no end date: active on any later day; ending 30 November: inactive on 1 December.
    @Test
    fun promotionDates() {
        val open = Promotion("p", "A", Discount.TwoForOne, eur(100), start = TODAY)
        val ending = open.copy(end = TODAY + 10)

        assertTrue(open.isActiveOn(TODAY + 400))
        assertTrue(ending.isActiveOn(TODAY + 10))
        assertFalse(ending.isActiveOn(TODAY + 11))
        assertFalse(open.isActiveOn(TODAY - 1))
    }

    // SPEC:R24 — while active the promotion is the current price; once it ends, the regular price.
    @Test
    fun promotionThenRegularPrice() {
        val book = PriceBook(
            packs = listOf(cookieQ600),
            prices = listOf(price("q600", 159, seenOn = TODAY - 5)),
            promotions = listOf(Promotion("q600", "A", Discount.ThreeForTwo, eur(159), TODAY, end = TODAY + 6)),
        )
        assertEquals(318L, Pricing.currentOffer("q600", "A", book, TODAY + 6)!!.total.cents)
        val after = Pricing.currentOffer("q600", "A", book, TODAY + 7)!!
        assertEquals(159L, after.total.cents)
        assertFalse(after.isPromotion)
    }

    // SPEC:R24 — a promotion that ended with no regular price keeps its price, flagged.
    @Test
    fun endedPromotionWithoutRegularPriceIsFlagged() {
        val book = PriceBook(
            packs = listOf(cookieQ600),
            prices = emptyList(),
            promotions = listOf(Promotion("q600", "A", Discount.PercentOff(20), eur(200), TODAY - 10, end = TODAY - 1)),
        )
        val offer = Pricing.currentOffer("q600", "A", book, TODAY)!!
        assertEquals(160L, offer.total.cents)
        assertTrue(offer.noRegularPrice)
        assertEquals(1, Ranking.rank(book, TODAY).single().rows.size)
    }

    // SPEC:R25 — a second promotion on the same pack and store replaces the first.
    @Test
    fun aNewerPromotionReplacesTheOlder() {
        val book = PriceBook(
            packs = listOf(cookieQ600),
            prices = listOf(price("q600", 159)),
            promotions = listOf(
                Promotion("q600", "A", Discount.ThreeForTwo, eur(159), TODAY - 3),
                Promotion("q600", "A", Discount.PercentOff(10), eur(159), TODAY - 1),
            ),
        )
        assertEquals(143L, Pricing.currentOffer("q600", "A", book, TODAY)!!.total.cents)
    }

    // SPEC:R16 — the newest observation is the regular price; older ones stay.
    @Test
    fun newestObservationIsTheRegularPrice() {
        val book = PriceBook(
            packs = listOf(cookieQ600),
            prices = listOf(price("q600", 159, seenOn = TODAY - 3), price("q600", 129, seenOn = TODAY)),
        )
        assertEquals(129L, Pricing.currentOffer("q600", "A", book, TODAY)!!.total.cents)
        assertEquals(159L, Pricing.currentOffer("q600", "A", book, TODAY - 1)!!.total.cents)
        assertNull(Pricing.currentOffer("q600", "B", book, TODAY))
    }

    // SPEC:R19 — between a "not carried" mark and a price at that store, the newer wins.
    @Test
    fun carriedFollowsTheNewerOfMarkAndPrice() {
        val markedOff = listOf(CarriedMark("q600", "A", carried = false, on = TODAY - 5))
        val priceLater = listOf(price("q600", 159, seenOn = TODAY))
        val priceEarlier = listOf(price("q600", 159, seenOn = TODAY - 9))

        assertTrue(Pricing.isCarried("q600", "A", priceLater, markedOff))
        assertFalse(Pricing.isCarried("q600", "A", priceEarlier, markedOff))
        assertTrue(Pricing.isCarried("q600", "A", priceEarlier, emptyList()))
        assertFalse(Pricing.isCarried("q600", "A", emptyList(), emptyList()))
        assertTrue(Pricing.isCarried("q600", "A", emptyList(), listOf(CarriedMark("q600", "A", true, TODAY))))
    }
}
