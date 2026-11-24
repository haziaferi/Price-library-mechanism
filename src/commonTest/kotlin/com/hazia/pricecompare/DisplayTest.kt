package com.hazia.pricecompare

import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayTest {

    // SPEC:R34 — 2,6499 €/kg displays as 2,65 in an Italian format and 2.65 in an English one.
    @Test
    fun unitPriceRoundsHalfUpAndFollowsTheHostFormat() {
        val sack = pack("sack", "Flour", Quantity.of(100, MeasureUnit.KILOGRAM))
        val book = PriceBook(listOf(sack), listOf(price("sack", 26_499)))
        val cents = Ranking.rank(book, TODAY).single().rows.single().unitPriceCents

        assertEquals(265L, cents)
        assertEquals("2,65", Display.amount(cents, ','))
        assertEquals("2.65", Display.amount(cents, '.'))
    }

    // SPEC:R34 — small and whole amounts keep two decimals.
    @Test
    fun amountsKeepTwoDecimals() {
        assertEquals("0,05", Display.amount(5))
        assertEquals("30,00", Display.amount(3_000))
    }

    // SPEC:R27 — gaps read "+13%", and the first row has none.
    @Test
    fun gaps() {
        assertEquals("+13%", Display.gap(13))
        assertEquals("", Display.gap(null))
    }
}
