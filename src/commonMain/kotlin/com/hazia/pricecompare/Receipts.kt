package com.hazia.pricecompare

/** One pack bought: [quantity] packs at [pricePerPack], with the line's own discount (SPEC:R38). */
data class ReceiptLine(
    val packId: String,
    val quantity: Int,
    val pricePerPack: Money,
    val discount: Discount? = null,
) {
    init {
        require(quantity >= 1) { "A receipt line buys at least one pack" }
    }
}

/** A discount on a whole receipt's total (SPEC:R39). */
sealed interface ReceiptDiscount {
    data class PercentOff(val percent: Int) : ReceiptDiscount {
        init {
            require(percent in 1..100) { "A percentage off is 1..100, was $percent" }
        }
    }

    data class AmountOff(val amount: Money) : ReceiptDiscount
}

object Receipts {

    /**
     * The line's total (SPEC:R38): price per pack times quantity with the
     * line's discount. A percentage off is rounded per pack (SPEC:R22); a
     * multibuy charges full price for packs outside a complete bundle.
     */
    fun lineTotal(line: ReceiptLine): Money {
        val price = line.pricePerPack
        val cents = when (val d = line.discount) {
            null -> price.cents * line.quantity
            is Discount.PercentOff -> Pricing.discountedPackPrice(price, d).cents * line.quantity
            is Discount.Multibuy -> price.cents * ((line.quantity / d.take) * d.pay + line.quantity % d.take)
        }
        return Money(cents, price.currency)
    }

    /**
     * Spreads a discount on the whole receipt over its lines in proportion to
     * their totals, rounded half up to the cent, with any remainder on the
     * largest line, the first of equal ones, so the lines add up to the total
     * paid (SPEC:R39).
     */
    fun applyReceiptDiscount(lineTotals: List<Money>, discount: ReceiptDiscount): List<Money> {
        if (lineTotals.isEmpty()) return lineTotals
        val currency = lineTotals.first().currency
        require(lineTotals.all { it.currency == currency }) { "A receipt has one currency" }

        val total = lineTotals.sumOf { it.cents }
        val off = when (discount) {
            is ReceiptDiscount.PercentOff -> Exact.divideHalfUp(total * discount.percent, 100)
            is ReceiptDiscount.AmountOff -> {
                require(discount.amount.currency == currency) { "The discount must be in $currency" }
                discount.amount.cents
            }
        }
        require(off <= total) { "The discount ($off) is larger than the receipt ($total)" }
        if (total == 0L) return lineTotals

        val paid = total - off
        val shares = lineTotals.map { Exact.divideHalfUp(it.cents * paid, total) }.toMutableList()
        val largest = lineTotals.indices.maxWith(compareBy<Int> { lineTotals[it].cents }.thenByDescending { it })
        shares[largest] += paid - shares.sum()
        check(shares.all { it >= 0 }) { "A line went below zero: $shares" }
        return shares.map { Money(it, currency) }
    }
}
