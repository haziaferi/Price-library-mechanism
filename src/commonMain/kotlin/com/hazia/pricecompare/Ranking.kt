package com.hazia.pricecompare

/** A pack's price at one store, ready to rank. */
data class StoreOffer(
    val storeId: String,
    val offer: Offer,
)

/** One ranked pack (SPEC:R28): the store it shows and the other stores' offers. */
data class RankedRow(
    val pack: Pack,
    val shown: StoreOffer,
    val others: List<StoreOffer>,
    /** Unit price in cents per the group's reference unit, rounded half up (SPEC:R34). */
    val unitPriceCents: Long,
    /** Whole-percent gap from the first row; null on the first row (SPEC:R27). */
    val gapPercent: Int?,
)

/** Packs of one dimension, ranked cheapest first (SPEC:R11, R27). */
data class RankedGroup(
    val dimension: Dimension,
    val reference: Quantity,
    val rows: List<RankedRow>,
)

/** The inputs of a ranking; the host passes what it stores (SPEC:R03). */
data class PriceBook(
    val packs: List<Pack>,
    val prices: List<PriceObservation>,
    val promotions: List<Promotion> = emptyList(),
)

object Ranking {

    /**
     * Ranks the book's packs by unit price on [on], one group per dimension
     * (SPEC:R11, R27, R32).
     *
     * With [store] set, each pack shows that store's offer and packs without one
     * are left out; otherwise a pack shows its cheapest offer, the later-seen one
     * on a tie (SPEC:R30). [hidden] pack ids are left out (SPEC:R35), and
     * [references] overrides a dimension's reference unit (SPEC:R15).
     */
    fun rank(
        book: PriceBook,
        on: Day,
        store: String? = null,
        hidden: Set<String> = emptySet(),
        references: Map<Dimension, Quantity> = emptyMap(),
    ): List<RankedGroup> {
        val candidates = book.packs
            .filter { it.id !in hidden }
            .mapNotNull { pack ->
                val stores = store?.let { listOf(it) } ?: Pricing.storesOf(pack.id, book).sorted()
                val offers = stores.mapNotNull { s -> Pricing.currentOffer(pack.id, s, book, on)?.let { StoreOffer(s, it) } }
                if (offers.isEmpty()) null else pack to offers
            }

        val currencies = candidates.flatMap { (_, offers) -> offers.map { it.offer.total.currency } }.toSet()
        require(currencies.size <= 1) { "Cannot rank prices in different currencies: $currencies" } // SPEC:R18

        return candidates
            .groupBy { (pack, _) -> pack.dimension }
            .entries.sortedBy { it.key.ordinal }
            .map { (dimension, group) ->
                val reference = references[dimension] ?: Quantity.defaultReference(dimension)
                require(reference.dimension == dimension) { "Reference for $dimension must be a $dimension" }
                RankedGroup(dimension, reference, rankGroup(group, reference))
            }
    }

    /**
     * One row per pack (SPEC:R28), sorted by unit price to the displayed cent,
     * then the larger pack, then the product name (SPEC:R29).
     */
    private fun rankGroup(group: List<Pair<Pack, List<StoreOffer>>>, reference: Quantity): List<RankedRow> {
        val unsorted = group.map { (pack, offers) ->
            val sorted = offers.sortedWith(byUnitPrice(pack).thenByDescending { it.offer.seenOn })
            val shown = sorted.first()
            Triple(pack, shown, sorted.drop(1))
        }
        val keyed = unsorted.map { (pack, shown, others) ->
            val cents = Exact.unitPriceCents(shown.offer.total.cents, offerSize(pack, shown.offer), reference.base)
            Row(pack, shown, others, cents)
        }.sortedWith(
            compareBy<Row> { it.unitPriceCents }
                .thenByDescending { it.pack.size.base }
                .thenBy { it.pack.productName }
                .thenBy { it.pack.id },
        )

        val best = keyed.firstOrNull() ?: return emptyList()
        return keyed.mapIndexed { index, row ->
            RankedRow(
                pack = row.pack,
                shown = row.shown,
                others = row.others,
                unitPriceCents = row.unitPriceCents,
                gapPercent = if (index == 0) null else Exact.gapPercent(
                    row.shown.offer.total.cents, offerSize(row.pack, row.shown.offer),
                    best.shown.offer.total.cents, offerSize(best.pack, best.shown.offer),
                ),
            )
        }
    }

    /** One pack's offer at one store, in a cross-store comparison (SPEC:R37). */
    data class StoreRow(val pack: Pack, val storeId: String, val offer: Offer, val unitPriceCents: Long)

    /**
     * Lists every pack of [productId] at every store with a current offer on
     * [on], cheapest unit price first (SPEC:R37). Ties sort as R29 does.
     */
    fun acrossStores(
        productId: String,
        book: PriceBook,
        on: Day,
        reference: Quantity? = null,
    ): List<StoreRow> {
        val packs = book.packs.filter { it.productId == productId }
        if (packs.isEmpty()) return emptyList()
        val dimension = packs.first().dimension
        require(packs.all { it.dimension == dimension }) { "A product's packs share one dimension (SPEC:R11)" }
        val ref = reference ?: Quantity.defaultReference(dimension)

        val rows = packs.flatMap { pack ->
            Pricing.storesOf(pack.id, book).mapNotNull { store ->
                Pricing.currentOffer(pack.id, store, book, on)?.let { offer ->
                    StoreRow(pack, store, offer, Exact.unitPriceCents(offer.total.cents, offerSize(pack, offer), ref.base))
                }
            }
        }
        require(rows.map { it.offer.total.currency }.toSet().size <= 1) { "Cannot compare different currencies" }
        return rows.sortedWith(
            compareBy<StoreRow> { it.unitPriceCents }
                .thenByDescending { it.pack.size.base }
                .thenByDescending { it.offer.seenOn }
                .thenBy { it.storeId },
        )
    }

    private class Row(val pack: Pack, val shown: StoreOffer, val others: List<StoreOffer>, val unitPriceCents: Long)

    /** The base units an offer's total buys: a bundle buys several packs (SPEC:R31). */
    private fun offerSize(pack: Pack, offer: Offer): Long = pack.size.base * offer.packCount

    private fun byUnitPrice(pack: Pack): Comparator<StoreOffer> = Comparator { a, b ->
        Exact.compareUnitPrices(
            a.offer.total.cents, offerSize(pack, a.offer),
            b.offer.total.cents, offerSize(pack, b.offer),
        )
    }
}
