package com.hazia.pricecompare

/** Shared builders so each test reads like its SPEC example. */
internal val TODAY = Day(20_000)

internal fun grams(g: Long) = Quantity.of(g, MeasureUnit.GRAM)
internal fun ml(v: Long) = Quantity.of(v, MeasureUnit.MILLILITRE)
internal fun pieces(n: Long) = Quantity.of(n, MeasureUnit.PIECE)
internal fun eur(cents: Long) = Money(cents)

internal fun pack(id: String, name: String, size: Quantity, count: Int = 1, loose: Boolean = false) =
    Pack(id = id, productId = name, productName = name, unitSize = size, count = count, loose = loose)

internal fun price(packId: String, cents: Long, store: String = "A", seenOn: Day = TODAY) =
    PriceObservation(packId, store, eur(cents), seenOn)

/** The three cookie packs of SPEC R27 at store A. */
internal val cookieQ600 = pack("q600", "Cookies Q", grams(600))
internal val cookieS1000 = pack("s1000", "Cookies S", grams(1_000))
internal val cookieS500 = pack("s500", "Cookies S", grams(500))

internal val cookieBook = PriceBook(
    packs = listOf(cookieS1000, cookieS500, cookieQ600),
    prices = listOf(price("s1000", 299), price("s500", 199), price("q600", 159)),
)
