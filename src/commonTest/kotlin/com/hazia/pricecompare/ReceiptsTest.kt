package com.hazia.pricecompare

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReceiptsTest {

    private fun cents(values: List<Money>) = values.map { it.cents }

    // SPEC:R38 — the three rows of line totals.
    @Test
    fun lineTotals() {
        assertEquals(318L, Receipts.lineTotal(ReceiptLine("q600", 3, eur(159), Discount.ThreeForTwo)).cents)
        assertEquals(150L, Receipts.lineTotal(ReceiptLine("x", 1, eur(200), Discount.PercentOff(25))).cents)
        assertEquals(258L, Receipts.lineTotal(ReceiptLine("y", 2, eur(129))).cents)
    }

    // SPEC:R38 — a multibuy charges full price for packs outside a complete bundle.
    @Test
    fun multibuyRemainderPaysFullPrice() {
        assertEquals(477L, Receipts.lineTotal(ReceiptLine("q600", 4, eur(159), Discount.ThreeForTwo)).cents)
        assertEquals(318L, Receipts.lineTotal(ReceiptLine("q600", 3, eur(159), Discount.TwoForOne)).cents)
    }

    // SPEC:R39 — 5% off a 60,00 € receipt.
    @Test
    fun percentOffAWholeReceipt() {
        val lines = listOf(eur(1_000), eur(2_000), eur(3_000))
        assertEquals(listOf(950L, 1_900L, 2_850L), cents(Receipts.applyReceiptDiscount(lines, ReceiptDiscount.PercentOff(5))))
    }

    // SPEC:R39 — 10 € off a 60,00 € receipt.
    @Test
    fun amountOffAWholeReceipt() {
        val lines = listOf(eur(1_000), eur(2_000), eur(3_000))
        val after = Receipts.applyReceiptDiscount(lines, ReceiptDiscount.AmountOff(eur(1_000)))
        assertEquals(listOf(833L, 1_667L, 2_500L), cents(after))
        assertEquals(5_000L, after.sumOf { it.cents })
    }

    // SPEC:R39 — the leftover cent goes on the largest line, the first of equal ones.
    @Test
    fun remainderGoesOnTheFirstLargestLine() {
        val lines = listOf(eur(100), eur(100), eur(100))
        val after = Receipts.applyReceiptDiscount(lines, ReceiptDiscount.AmountOff(eur(100)))
        assertEquals(listOf(66L, 67L, 67L), cents(after))
    }

    // SPEC:R39 — whatever the discount, the lines add up to the total paid.
    @Test
    fun linesAlwaysAddUpToTheTotalPaid() {
        val lines = listOf(eur(333), eur(1), eur(97), eur(1_249), eur(5))
        val total = lines.sumOf { it.cents }
        for (percent in 1..99) {
            val after = Receipts.applyReceiptDiscount(lines, ReceiptDiscount.PercentOff(percent))
            assertEquals(total - Exact.divideHalfUp(total * percent, 100), after.sumOf { it.cents }, "at $percent%")
        }
        for (off in listOf(1L, 7L, 999L, total)) {
            val after = Receipts.applyReceiptDiscount(lines, ReceiptDiscount.AmountOff(eur(off)))
            assertEquals(total - off, after.sumOf { it.cents }, "at $off cents off")
        }
    }

    // SPEC:R39 — a discount larger than the receipt is refused.
    @Test
    fun discountAboveTheTotalIsRefused() {
        assertFailsWith<IllegalArgumentException> {
            Receipts.applyReceiptDiscount(listOf(eur(100)), ReceiptDiscount.AmountOff(eur(101)))
        }
    }
}
