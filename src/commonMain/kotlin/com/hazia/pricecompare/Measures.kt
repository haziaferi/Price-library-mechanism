package com.hazia.pricecompare

import kotlin.jvm.JvmInline

/** What a pack's size measures (SPEC:R10). */
enum class Dimension { WEIGHT, VOLUME, COUNT, LENGTH }

/**
 * The units sizes are entered in, each with its size in its dimension's base unit:
 * mg, ml, piece or mm (SPEC:R10). Metric only in v1.
 */
enum class MeasureUnit(val dimension: Dimension, val baseFactor: Long, val symbol: String) {
    MILLIGRAM(Dimension.WEIGHT, 1, "mg"),
    GRAM(Dimension.WEIGHT, 1_000, "g"),
    KILOGRAM(Dimension.WEIGHT, 1_000_000, "kg"),
    MILLILITRE(Dimension.VOLUME, 1, "ml"),
    CENTILITRE(Dimension.VOLUME, 10, "cl"),
    DECILITRE(Dimension.VOLUME, 100, "dl"),
    LITRE(Dimension.VOLUME, 1_000, "L"),
    PIECE(Dimension.COUNT, 1, "pc"),
    CENTIMETRE(Dimension.LENGTH, 10, "cm"),
    METRE(Dimension.LENGTH, 1_000, "m"),
}

/**
 * An amount in whole base units (SPEC:R04). Shared with the host app's stock
 * quantities (SPEC:R14).
 *
 * The upper bound keeps every product the library forms (price × size × 100)
 * inside a Long, so comparisons stay exact without big-number types.
 */
data class Quantity(val base: Long, val dimension: Dimension) : Comparable<Quantity> {
    init {
        require(base in 1..MAX_BASE) { "Quantity must be 1..$MAX_BASE base units, was $base" }
    }

    operator fun times(count: Int): Quantity = Quantity(base * count, dimension)

    override fun compareTo(other: Quantity): Int {
        require(dimension == other.dimension) { "Cannot compare $dimension with ${other.dimension}" }
        return base.compareTo(other.base)
    }

    companion object {
        /** One tonne in mg, 1 000 m³ in ml. */
        const val MAX_BASE: Long = 1_000_000_000

        fun of(amount: Long, unit: MeasureUnit): Quantity = Quantity(amount * unit.baseFactor, unit.dimension)

        /** The reference unit a unit price is given per, unless a tag overrides it (SPEC:R15). */
        fun defaultReference(dimension: Dimension): Quantity = when (dimension) {
            Dimension.WEIGHT -> of(1, MeasureUnit.KILOGRAM)
            Dimension.VOLUME -> of(1, MeasureUnit.LITRE)
            Dimension.COUNT -> of(1, MeasureUnit.PIECE)
            Dimension.LENGTH -> of(1, MeasureUnit.METRE)
        }
    }
}

/** An amount of money in whole cents with its ISO 4217 code (SPEC:R04, R18). */
data class Money(val cents: Long, val currency: String = EURO) {
    init {
        require(cents in 0..MAX_CENTS) { "Money must be 0..$MAX_CENTS cents, was $cents" }
    }

    companion object {
        const val EURO = "EUR"

        /** 100 000 €, far above any one purchase; keeps products inside a Long. */
        const val MAX_CENTS: Long = 10_000_000
    }
}

/** A calendar day as days since 1970-01-01; the host converts from its own date type. */
@JvmInline
value class Day(val epochDay: Int) : Comparable<Day> {
    override fun compareTo(other: Day): Int = epochDay.compareTo(other.epochDay)
    operator fun minus(days: Int): Day = Day(epochDay - days)
    operator fun plus(days: Int): Day = Day(epochDay + days)
}
