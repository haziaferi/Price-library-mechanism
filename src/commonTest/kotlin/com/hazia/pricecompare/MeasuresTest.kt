package com.hazia.pricecompare

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MeasuresTest {

    // SPEC:R14 — a host's stock quantity, built from the library's units, compares with a pack's size.
    @Test
    fun hostStockComparesWithAPackSize() {
        val stock = Quantity.of(1, MeasureUnit.KILOGRAM)
        val packSize = pack("s500", "Cookies S", grams(500)).size

        assertTrue(stock > packSize)
        assertEquals(Quantity.of(500, MeasureUnit.GRAM), packSize)
        assertFailsWith<IllegalArgumentException> { stock.compareTo(ml(500)) }
    }
}
