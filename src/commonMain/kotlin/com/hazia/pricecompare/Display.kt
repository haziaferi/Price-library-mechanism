package com.hazia.pricecompare

/** Turns cents into text in the number format the host passes (SPEC:R34). */
object Display {

    /** [cents] with two decimals and the host's [decimalSeparator]: 265 → "2,65" or "2.65". */
    fun amount(cents: Long, decimalSeparator: Char = ','): String {
        require(cents >= 0) { "Amounts are never negative" }
        val fraction = (cents % 100).toString().padStart(2, '0')
        return "${cents / 100}$decimalSeparator$fraction"
    }

    /** A gap from the cheapest row as "+13%"; empty for the first row (SPEC:R27). */
    fun gap(percent: Int?): String = percent?.let { "+$it%" } ?: ""
}
