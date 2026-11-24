package com.hazia.pricecompare

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RankingTest {

    private fun ids(groups: List<RankedGroup>) = groups.single().rows.map { it.pack.id }

    // SPEC:R27 — the library's first test: the cookie example.
    @Test
    fun cookiesRankByPricePerKilo() {
        val groups = Ranking.rank(cookieBook, on = TODAY)

        assertEquals(1, groups.size)
        val rows = groups.single().rows
        assertEquals(listOf("q600", "s1000", "s500"), rows.map { it.pack.id })
        assertEquals(listOf(265L, 299L, 398L), rows.map { it.unitPriceCents })
        assertEquals(listOf(null, 13, 50), rows.map { it.gapPercent })
    }

    // SPEC:R28 — a pack priced at three stores is one row carrying three prices.
    @Test
    fun onePackAtThreeStoresIsOneRow() {
        val book = PriceBook(
            packs = listOf(cookieQ600),
            prices = listOf(price("q600", 159, "A"), price("q600", 165, "B"), price("q600", 149, "C")),
        )
        val row = Ranking.rank(book, TODAY).single().rows.single()

        assertEquals("C", row.shown.storeId)
        assertEquals(listOf("A", "B"), row.others.map { it.storeId })
    }

    // SPEC:R29 — tied to the displayed cent, the larger pack comes first,
    // even when the smaller pack is cheaper at full precision.
    @Test
    fun tiesToTheDisplayedCentPutTheLargerPackFirst() {
        val big = pack("b1100", "Cookies B", grams(1_100)) // 2,92 € → 2,6545 €/kg
        val small = pack("q600", "Cookies Q", grams(600)) // 1,59 € → 2,6500 €/kg
        val book = PriceBook(listOf(small, big), listOf(price("q600", 159), price("b1100", 292)))

        val rows = Ranking.rank(book, TODAY).single().rows
        assertEquals(listOf("b1100", "q600"), rows.map { it.pack.id })
        assertEquals(listOf(265L, 265L), rows.map { it.unitPriceCents })
    }

    // SPEC:R29 — same displayed price and size: by product name.
    @Test
    fun tiesOfEqualSizeSortByName() {
        val zeta = pack("z", "Zeta", grams(500))
        val alpha = pack("a", "Alpha", grams(500))
        val book = PriceBook(listOf(zeta, alpha), listOf(price("z", 199), price("a", 199)))

        assertEquals(listOf("a", "z"), ids(Ranking.rank(book, TODAY)))
    }

    // SPEC:R30 — two stores at the same price: the one seen later.
    @Test
    fun equalPricesShowTheStoreSeenLater() {
        val book = PriceBook(
            packs = listOf(cookieS1000),
            prices = listOf(
                price("s1000", 299, "A", seenOn = TODAY - 9),
                price("s1000", 299, "B", seenOn = TODAY),
            ),
        )
        assertEquals("B", Ranking.rank(book, TODAY).single().rows.single().shown.storeId)
    }

    // SPEC:R30 — a chosen store shows that store's price, and packs it has no price for are left out.
    @Test
    fun aChosenStoreShowsItsOwnPrice() {
        val book = PriceBook(
            packs = listOf(cookieQ600, cookieS500),
            prices = listOf(price("q600", 149, "A"), price("q600", 159, "B"), price("s500", 199, "A")),
        )
        val rows = Ranking.rank(book, TODAY, store = "B").single().rows

        assertEquals(listOf("q600"), rows.map { it.pack.id })
        assertEquals("B", rows.single().shown.storeId)
        assertEquals(159L, rows.single().shown.offer.total.cents)
    }

    // SPEC:R31 — a "3 x 2" ranks by the bundle's unit price and says how many to buy.
    @Test
    fun multibuyRanksByTheBundle() {
        val book = cookieBook.copy(
            promotions = listOf(Promotion("q600", "A", Discount.ThreeForTwo, eur(159), start = TODAY)),
        )
        val first = Ranking.rank(book, TODAY).single().rows.first()

        assertEquals("q600", first.pack.id)
        assertEquals(177L, first.unitPriceCents)
        assertEquals(3, first.shown.offer.bundleOf)
    }

    // SPEC:R11 — packs of different dimensions rank in separate groups.
    @Test
    fun dimensionsRankInSeparateGroups() {
        val eggs = pack("eggs", "Eggs", pieces(10))
        val book = PriceBook(cookieBook.packs + eggs, cookieBook.prices + price("eggs", 289))
        val groups = Ranking.rank(book, TODAY)

        assertEquals(listOf(Dimension.WEIGHT, Dimension.COUNT), groups.map { it.dimension })
        assertEquals(listOf("eggs"), groups[1].rows.map { it.pack.id })
        assertEquals(29L, groups[1].rows.single().unitPriceCents)
    }

    // SPEC:R18 — a ranking that mixes currencies is refused.
    @Test
    fun mixedCurrenciesAreRefused() {
        val book = cookieBook.copy(
            prices = cookieBook.prices + PriceObservation("s500", "B", Money(210, "CHF"), TODAY),
        )
        assertFailsWith<IllegalArgumentException> { Ranking.rank(book, TODAY) }
    }

    // SPEC:R35 — a hidden pack leaves the ranking and comes back unchanged.
    @Test
    fun hiddenPacksLeaveTheRanking() {
        assertEquals(listOf("s1000", "s500"), ids(Ranking.rank(cookieBook, TODAY, hidden = setOf("q600"))))
        assertEquals(listOf("q600", "s1000", "s500"), ids(Ranking.rank(cookieBook, TODAY)))
    }

    // SPEC:R15 — a spice at 1,20 € for 40 g: 30,00 €/kg, or 3,00 €/100 g with the override.
    @Test
    fun referenceUnitCanBeOverridden() {
        val book = PriceBook(listOf(pack("pepper", "Pepper", grams(40))), listOf(price("pepper", 120)))

        assertEquals(3_000L, Ranking.rank(book, TODAY).single().rows.single().unitPriceCents)
        val per100g = Ranking.rank(book, TODAY, references = mapOf(Dimension.WEIGHT to grams(100)))
        assertEquals(300L, per100g.single().rows.single().unitPriceCents)
    }

    // SPEC:R13 — loose apples at 2,40 €/kg rank between packed apples at 2,20 and 2,60 €/kg.
    @Test
    fun looseGoodsRankWithPackedOnes() {
        val loose = pack("loose", "Apples, loose", grams(1_000), loose = true)
        val cheap = pack("cheap", "Apples A", grams(1_000))
        val dear = pack("dear", "Apples B", grams(1_000))
        val book = PriceBook(
            listOf(loose, cheap, dear),
            listOf(price("loose", 240), price("cheap", 220), price("dear", 260)),
        )
        assertEquals(listOf("cheap", "loose", "dear"), ids(Ranking.rank(book, TODAY)))
    }

    // SPEC:R32 — the day before a promotion ranks the regular price, its first day the promotion.
    @Test
    fun rankingUsesTheDayPassedIn() {
        val book = cookieBook.copy(
            promotions = listOf(Promotion("s500", "A", Discount.PercentOff(50), eur(199), start = TODAY + 1)),
        )
        val before = Ranking.rank(book, TODAY).single().rows.first { it.pack.id == "s500" }
        val during = Ranking.rank(book, TODAY + 1).single().rows.first { it.pack.id == "s500" }

        assertEquals(199L, before.shown.offer.total.cents)
        assertEquals(100L, during.shown.offer.total.cents)
        assertTrue(during.shown.offer.isPromotion)
    }

    // SPEC:R33 — a price seen 61 days before the ranking day is flagged and keeps its place.
    @Test
    fun stalePricesAreFlaggedButRanked() {
        val book = cookieBook.copy(
            prices = listOf(price("s1000", 299), price("s500", 199), price("q600", 159, seenOn = TODAY - 61)),
        )
        val rows = Ranking.rank(book, TODAY).single().rows

        assertEquals("q600", rows.first().pack.id)
        assertTrue(rows.first().shown.offer.stale)
        assertEquals(listOf(false, false), rows.drop(1).map { it.shown.offer.stale })
    }

    // SPEC:R37 — one product priced at three stores gives three rows, cheapest per kg first.
    @Test
    fun oneProductAcrossStores() {
        val book = PriceBook(
            packs = listOf(cookieQ600, cookieS1000),
            prices = listOf(price("q600", 159, "A"), price("q600", 149, "B"), price("q600", 169, "C"), price("s1000", 299, "A")),
        )
        val rows = Ranking.acrossStores("Cookies Q", book, TODAY)

        assertEquals(listOf("B", "A", "C"), rows.map { it.storeId })
        assertEquals(listOf(248L, 265L, 282L), rows.map { it.unitPriceCents })
    }

    // SPEC:R17 — two branches of one chain keep separate prices, each ranked on its own.
    @Test
    fun branchesOfOneChainRankSeparately() {
        val book = PriceBook(
            packs = listOf(cookieQ600),
            prices = listOf(price("q600", 159, "chain-north"), price("q600", 171, "chain-south")),
        )
        val rows = Ranking.acrossStores("Cookies Q", book, TODAY)

        assertEquals(listOf("chain-north", "chain-south"), rows.map { it.storeId })
        assertEquals(listOf(159L, 171L), rows.map { it.offer.total.cents })
    }

    // SPEC:R36 — packs typed in the aisle, never saved, rank as R27 ranks saved ones.
    @Test
    fun unsavedPacksRankTheSameWay() {
        val typed = PriceBook(
            packs = listOf(pack("t1", "Brand X", grams(750)), pack("t2", "Brand Y", grams(400))),
            prices = listOf(price("t1", 225, "here"), price("t2", 112, "here")),
        )
        val rows = Ranking.rank(typed, TODAY).single().rows

        assertEquals(listOf("t2", "t1"), rows.map { it.pack.id })
        assertEquals(listOf(280L, 300L), rows.map { it.unitPriceCents })
        assertEquals(7, rows[1].gapPercent)
    }

    // SPEC:R33 — exactly 60 days is not stale yet.
    @Test
    fun sixtyDaysIsNotStale() {
        val book = cookieBook.copy(prices = listOf(price("q600", 159, seenOn = TODAY - 60)))
        assertEquals(false, Ranking.rank(book, TODAY).single().rows.single().shown.offer.stale)
    }
}
