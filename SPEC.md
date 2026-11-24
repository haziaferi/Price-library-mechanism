# Price-compare spec

Scope: Price-compare, a Kotlin Multiplatform library that ranks packs by unit price, keeps the rules for prices, promotions and receipt totals, and is used by the owner's apps, first by the Chatelaine home inventory's Shopping module.

History: `SPEC-history.md`, one line per change. It is never a source of current rules.

Kinds: O owner rule · D decided · I inherited · A assumed · Q open.

## Rule index

| ID | Name | Section |
|---|---|---|
| D-R01 | Users and hosts | 1. Purpose and jobs |
| D-R02 | Jobs and how often | 1. Purpose and jobs |
| D-R03 | Library shape | 2. Shape and platform |
| D-R04 | Whole numbers | 2. Shape and platform |
| D-R05 | Build versions | 2. Shape and platform |
| D-R06 | Record ids | 2. Shape and platform |
| D-R07 | Licence and repository | 2. Shape and platform |
| D-R08 | How the host app uses it | 2. Shape and platform |
| D-R09 | Package name | 2. Shape and platform |
| D-R10 | Units | 3. Units and packs |
| D-R11 | One dimension per pack | 3. Units and packs |
| D-R12 | Multipacks | 3. Units and packs |
| D-R13 | Loose goods | 3. Units and packs |
| D-R14 | Units shared with the host | 3. Units and packs |
| D-R15 | Reference unit | 3. Units and packs |
| D-R16 | Prices and their history | 4. Prices and stores |
| D-R17 | Stores | 4. Prices and stores |
| D-R18 | Currency | 4. Prices and stores |
| D-R19 | Carried by a store | 4. Prices and stores |
| D-R20 | Deleting a product or a store | 4. Prices and stores |
| D-R21 | Discount kinds | 5. Promotions |
| D-R22 | Entering a percentage off | 5. Promotions |
| D-R23 | Promotion dates | 5. Promotions |
| D-R24 | Promotion and regular price | 5. Promotions |
| D-R25 | No stacking on one pack | 5. Promotions |
| D-R27 | Unit-price ranking | 6. Ranking |
| D-R28 | One row per pack | 6. Ranking |
| D-R29 | Ties | 6. Ranking |
| D-R30 | The store a row shows | 6. Ranking |
| D-R31 | Multibuys in the ranking | 6. Ranking |
| D-R32 | Ranking date | 6. Ranking |
| D-R33 | Stale prices | 6. Ranking |
| D-R34 | Displayed values | 6. Ranking |
| D-R35 | Hidden packs | 6. Ranking |
| D-R36 | Quick comparison of unsaved packs | 6. Ranking |
| D-R37 | One product across stores | 6. Ranking |
| D-R38 | Line totals | 7. Receipt arithmetic |
| D-R39 | Discount on a whole receipt | 7. Receipt arithmetic |
| D-R40 | Not in v1 | Out of scope |

## Glossary

| Term | Meaning |
|---|---|
| Host app | The app that uses the library and owns storage, screens, tags and record ids; Chatelaine first. |
| Product | Something to buy: an item or a wish in the host app. The library knows it only by its id and name. |
| Pack | One unit of a product with a given size, as sold, whatever amount it holds: a 500 g bag of cookies, a 6 × 330 ml multipack. |
| Dimension | What a pack's size measures: weight, volume, count or length. |
| Base unit | The whole-number unit a size is stored in: mg, ml, piece or mm. |
| Reference unit | The amount a unit price is given per: 1 kg, 1 L, 1 piece or 1 m unless a tag overrides it. |
| Unit price | A pack's price per reference unit. |
| Store | One branch of a shop, optionally with a chain name and a type. |
| Price | One observation of a pack's shelf price at a store on a date. |
| Promotion | A discount on one pack at one store, with a start date and an optional end date. |
| Current price | The price a pack ranks by at a store on a given date: an active promotion, or else the newest price. |
| Carried | A pack is carried by a store when it has a price there or the user marked it so, whichever is newer. |
| Receipt line | One pack bought, with its quantity and the total paid for it. |

## 1. Purpose and jobs

Shoppers compare packs of different sizes and prices, and the cheapest-looking pack is often not the cheapest per kilo. Shelf labels are inconsistent and promotions change the answer. The library does that arithmetic exactly, once, for every app of the owner's that needs it, and keeps it free of storage and screens so each host app can present it its own way.

### D-R01 · Users and hosts

**Rule:** The user is one person keeping their own prices; a household shares data through the host app's sync, not through the library. The first host app is Chatelaine's Shopping module, and the savings app the owner will port can reuse the receipt arithmetic (R38, R39).

**Acceptance:** none: who the library serves, not a behaviour.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R02 · Jobs and how often

**Rule:** The library is shaped around the jobs below, most frequent first; tests are written in that order.

**Values:**

| Job | Rules | How often |
|---|---|---|
| Rank packs under a tag or across stores | R27, R37 | every time the user shops |
| Record a price as the user ticks a pack as bought | R16, R22 | every time the user shops |
| Work out receipt totals | R38, R39 | every time the user shops |
| Quick-compare packs not yet saved | R36 | sometimes |
| Record a promotion with its end date | R23 | weekly |

**Acceptance:** Each job has a test that runs it end to end through the library's public interface.

**Since:** 2026-11-24 · owner · agreed rewording

## 2. Shape and platform

### D-R03 · Library shape

**Rule:** The library is Kotlin Multiplatform common code built for Android and the JVM desktop. It holds no storage, no screens and no tags: the host app passes in packs, prices, promotions and stores, and gets rankings and totals back.

**Why:** Each host app keeps its own database, sync and look; the library stays the one tested place for the arithmetic.

**Acceptance:** The library's code has no dependency on Android, a database or a UI toolkit, and its tests run on the JVM.

**Not:** Storage interfaces with an in-memory version: the host already owns storage (Chatelaine's spec, rules 48 and 49).

**Since:** 2026-11-24 · owner · agreed rewording

### D-R04 · Whole numbers

**Rule:** Money is stored in whole cents and sizes in whole base units, and two unit prices are compared by cross-multiplying (price A × size B against price B × size A), so nothing is divided or rounded until a value is displayed (R34).

**Why:** Multiplatform common code has no `BigDecimal`, and division would make equal prices compare unequal.

**Acceptance:** The cookie ranking of R27 comes out exact with no floating-point type anywhere in the comparison path.

**Not:** `BigDecimal`: not available in common code.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R05 · Build versions

**Rule:** The library builds with the same Kotlin version and JVM toolchain as its first host app: Kotlin 2.4.10 and JVM 21.

**Acceptance:** The build declares Kotlin 2.4.10 and a JVM 21 toolchain, and Chatelaine builds against it without a version override.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R06 · Record ids

**Rule:** Every record the library handles carries an id the host app assigns; the library never creates ids.

**Why:** The host already has an id and sync scheme (in Chatelaine, ULIDs with sync timestamps).

**Acceptance:** No public function of the library generates an id.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R07 · Licence and repository

**Rule:** The library is GPL-3.0 and lives in a public GitHub repository of its own. Its spec names the owner's other projects generically, except its public host apps.

**Acceptance:** The repository has a GPL-3.0 `LICENSE`, and the spec names no private project.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R08 · How the host app uses it

**Rule:** While both change together, Chatelaine links the library from its local folder; once Chatelaine's CI builds it, Chatelaine uses tagged JitPack releases.

**Acceptance:** Chatelaine's build resolves the library from the local folder before the switch, and from a tagged JitPack release after it.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R09 · Package name

**Rule:** The library's package and artifact group is `com.hazia.pricecompare`, following the owner's `com.hazia.<app>` names.

**Acceptance:** Every source file sits under `com.hazia.pricecompare`.

**Since:** 2026-11-24 · builder · agreed rewording

## 3. Units and packs

### D-R10 · Units

**Rule:** Sizes are entered in the units below and stored in their dimension's base unit. Only metric units are in v1.

**Values:**

| Dimension | Units | Base unit |
|---|---|---|
| weight | mg, g, kg | mg |
| volume | ml, cl, dl, L | ml |
| count | pieces | piece |
| length | cm, m | mm |

**Acceptance:** Each unit converts to its base unit exactly: 1 kg = 1 000 000 mg, 1 dl = 100 ml, 1 m = 1 000 mm.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R11 · One dimension per pack

**Rule:** A pack has one dimension in v1, and packs of different dimensions never share a ranking: they show as separate groups.

**Acceptance:** A tag holding packs sold by weight and packs sold by count ranks them in two groups, never in one list.

**Not:** Converting between dimensions (10 eggs ≈ 600 g): out of scope (R40).

**Since:** 2026-11-24 · owner · agreed rewording

### D-R12 · Multipacks

**Rule:** A multipack is one level, a count times a size ("6 × 330 ml"); a nested pack is entered flat, so "2 × (6 × 1.5 L)" becomes 12 × 1.5 L.

**Acceptance:** 6 × 330 ml has a size of 1 980 ml, and 12 × 1.5 L one of 18 000 ml.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R13 · Loose goods

**Rule:** Goods sold loose (deli counter, fruit by the kilo) are entered as a price per 1 kg or per 1 piece and marked loose; they rank with packed goods of the same dimension.

**Acceptance:** Loose apples at 2,40 €/kg rank between packed apples at 2,20 €/kg and 2,60 €/kg.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R14 · Units shared with the host

**Rule:** The units of R10 are part of the library's public interface, and the host app's stock quantities (Chatelaine's Pantry) use them, so stock and prices compare without conversion.

**Acceptance:** The host app can build a quantity from the library's unit type and compare it with a pack's size.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R15 · Reference unit

**Rule:** Unit prices are given per 1 kg, 1 L, 1 piece or 1 m, and the host app can pass a different reference unit for a tag, such as 100 g for spices.

**Acceptance:** A spice at 1,20 € for 40 g shows 30,00 €/kg by default and 3,00 €/100 g with the override.

**Since:** 2026-11-24 · owner · agreed rewording

## 4. Prices and stores

### D-R16 · Prices and their history

**Rule:** Every price is kept as its own observation, and the newest one at a store is the pack's regular price there. A wrong price is corrected in place; only a new observation adds history, and an untick in the host app removes the price its tick recorded.

**Acceptance:** Recording 1,59 € and then 1,29 € keeps both, with 1,29 € regular; correcting 1,29 € to 1,25 € leaves two observations, not three.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R17 · Stores

**Rule:** A store is one branch, with an optional chain name and type (supermarket, electronics), and two branches of one chain keep separate prices.

**Acceptance:** The same pack priced at two branches of one chain ranks each branch's price on its own.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R18 · Currency

**Rule:** Every price carries an ISO 4217 currency code; v1 handles euro only, and packs priced in another currency are not ranked with euro packs.

**Acceptance:** Every price record has a currency field, and a ranking mixing currencies is refused.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R19 · Carried by a store

**Rule:** A pack is carried by a store when it has a price there or the user marked it as carried; a mark that it is not carried and a price at that store cancel each other, and the newer one wins.

**Evidence:** Chatelaine's spec, rule 129, offers only carried packs when the user makes a store list.

**Acceptance:** A "not carried" mark followed by a newer price makes the pack carried again, and a newer mark after a price makes it not carried.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R20 · Deleting a product or a store

**Rule:** Deleting a product removes its packs, prices and promotions; deleting a store removes its prices, promotions and carried marks. Receipts already made keep their lines.

**Evidence:** Chatelaine's spec, rule 141, decides the same for stores.

**Acceptance:** After a store is deleted, no ranking shows a price from it, and an earlier receipt's lines and total are unchanged.

**Since:** 2026-11-24 · owner · agreed rewording

## 5. Promotions

### D-R21 · Discount kinds

**Rule:** v1 has three discount kinds on one pack at one store, listed below.

**Values:**

| Kind | Meaning | Example |
|---|---|---|
| "% off" | the shelf price less a percentage | 2,00 € at 25% off is 1,50 € |
| "2 x 1" | take 2, pay for 1 | 2 packs at 1,59 € cost 1,59 € |
| "3 x 2" | take 3, pay for 2 | 3 packs at 1,59 € cost 3,18 € |

**Acceptance:** Each row's example comes out as stated.

**Since:** 2026-11-24 · owner · own words

### D-R22 · Entering a percentage off

**Rule:** For "% off" the user enters the shelf price and the percentage, and the library works out the final price, rounded half up to the cent; the user can overwrite it with the price on the shelf label.

**Acceptance:** 1,99 € at 30% off gives 1,39 €, and overwriting it with 1,40 € keeps 1,40 €.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R23 · Promotion dates

**Rule:** A promotion starts today unless another date is given, and its end date is optional; one with no end date stays active until it is replaced or removed.

**Acceptance:** A promotion with no end date is active on any later date until removed; one ending on 30 November is inactive on 1 December.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R24 · Promotion and regular price

**Rule:** While a promotion is active it is the pack's current price at that store; when it ends, the regular price (R16) applies again. A pack has at most one promotion per store at a time, and a pack whose promotion ended with no regular price keeps the promotion's price, flagged as having no regular price.

**Acceptance:** A pack at 1,59 € regular with a "3 x 2" active ranks by the bundle price (R31) and by 1,59 € once it ends; a pack with only an ended promotion stays in the ranking, flagged.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R25 · No stacking on one pack

**Rule:** Two discounts never apply to one pack at once in v1. A discount on a whole receipt (R39) still applies on top of the lines' own discounts.

**Acceptance:** Adding a second promotion to a pack at a store with one active replaces the first.

**Since:** 2026-11-24 · owner · agreed rewording

## 6. Ranking

### D-R27 · Unit-price ranking

**Rule:** Given a set of packs of one dimension, the library ranks them by unit price, cheapest first, as in the example below. The gap from the cheapest is shown as a whole percentage from the second row on.

**Values:**

| Rank | Pack | Price | €/kg | Gap |
|---|---|---|---|---|
| 1 | Cookies Q 600 g | 1,59 € | 2,65 | — |
| 2 | Cookies S 1 kg | 2,99 € | 2,99 | +13% |
| 3 | Cookies S 500 g | 1,99 € | 3,98 | +50% |

**Acceptance:** The three packs rank in that order with those unit prices and gaps; this is the library's first test.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R28 · One row per pack

**Rule:** A pack sold at several stores is one row, showing one store's price, with the other stores' prices inside the row.

**Acceptance:** A pack priced at three stores gives one row carrying three prices.

**Since:** 2026-11-24 · owner · own words

### D-R29 · Ties

**Rule:** Packs whose unit prices match to the displayed cent are tied. Tied packs put the larger pack first, then sort by product name.

**Acceptance:** Two packs both shown at 2,65 €/kg put the 1 kg pack before the 600 g pack, whatever their exact unit prices.

**Not:** Exact ties only: two rows showing the same price could appear with the smaller pack first.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R30 · The store a row shows

**Rule:** When no store is chosen, a row shows its cheapest current price across stores; between two stores at that price, the one whose price was seen later.

**Acceptance:** A pack at 2,99 € at store A seen on 1 November and 2,99 € at store B seen on 10 November shows store B.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R31 · Multibuys in the ranking

**Rule:** A pack under "2 x 1" or "3 x 2" ranks by its unit price when the whole bundle is bought, and its row says so ("when buying 3").

**Acceptance:** Cookies Q 600 g at 1,59 € under "3 x 2" rank at 1,77 €/kg, labelled "when buying 3".

**Since:** 2026-11-24 · owner · agreed rewording

### D-R32 · Ranking date

**Rule:** Every ranking is worked out for a date the host passes; host screens pass today.

**Acceptance:** Ranking for the day before a promotion starts uses the regular price, and for its first day the promotion.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R33 · Stale prices

**Rule:** A price older than 60 days on the ranking date is flagged as stale and still ranked.

**Acceptance:** A price seen 61 days before the ranking date is flagged and keeps its place.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R34 · Displayed values

**Rule:** Unit prices are displayed with 2 decimals, rounded half up, and gaps as whole percentages, in the number format the host passes; ordering never uses the rounded values except for ties (R29).

**Acceptance:** 2,6499 €/kg displays as 2,65 in an Italian format and 2.65 in an English one.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R35 · Hidden packs

**Rule:** The host can pass a set of packs to hide, such as those whose products carry a hidden season tag; hidden packs leave rankings and quick comparisons but stay in the data.

**Acceptance:** Hiding a pack removes it from the ranking and from quick comparisons, and unhiding restores it with its prices.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R36 · Quick comparison of unsaved packs

**Rule:** Packs not yet saved can be compared with the same ranking, and any of them can then be saved to the host's price book.

**Acceptance:** Two unsaved packs typed in the aisle rank as R27 would rank them, and saving one passes it to the host as a new price.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R37 · One product across stores

**Rule:** For one product, the library lists its packs' current prices at every store with a price, cheapest unit price first, as in R27.

**Evidence:** Chatelaine's spec, rule 132, shows this from a store-list entry.

**Acceptance:** Cookies Q 600 g priced at three stores give three rows, cheapest per kg first.

**Since:** 2026-11-24 · owner · agreed rewording

## 7. Receipt arithmetic

### D-R38 · Line totals

**Rule:** A receipt line's total is the price per pack times the quantity, with the line's discount applied (R21); a discount marked at the tick is a promotion and leaves the regular price unchanged.

**Values:**

| Purchase | Line total |
|---|---|
| 3 packs at 1,59 €, "3 x 2" | 3,18 € |
| 1 pack at 2,00 €, 25% off | 1,50 € |
| 2 packs at 1,29 €, no discount | 2,58 € |

**Evidence:** Chatelaine's spec, rules 116 and 135.

**Acceptance:** Each row gives its line total, and after the "3 x 2" line the pack's regular price is still 1,59 €.

**Since:** 2026-11-24 · owner · agreed rewording

### D-R39 · Discount on a whole receipt

**Rule:** A discount on a whole receipt, as a percentage or an amount off, is spread over the lines in proportion to their totals, rounded to the cent, with any remainder on the largest line (the first of equal ones), so the lines add up to the total paid.

**Values:**

| Lines before | Discount | Lines after | Total paid |
|---|---|---|---|
| 10,00 € · 20,00 € · 30,00 € | 5% off | 9,50 € · 19,00 € · 28,50 € | 57,00 € |
| 10,00 € · 20,00 € · 30,00 € | 10 € off | 8,33 € · 16,67 € · 25,00 € | 50,00 € |
| 1,00 € · 1,00 € · 1,00 € | 1 € off | 0,66 € · 0,67 € · 0,67 € | 2,00 € |

**Evidence:** Chatelaine's spec, rule 136.

**Acceptance:** Each row gives its lines and total, and the lines always add up to the total paid.

**Since:** 2026-11-24 · owner · agreed rewording

## Acceptance list

- R02 · Jobs and how often: Each job has a test that runs it end to end through the library's public interface.
- R03 · Library shape: The library's code has no dependency on Android, a database or a UI toolkit, and its tests run on the JVM.
- R04 · Whole numbers: The cookie ranking of R27 comes out exact with no floating-point type anywhere in the comparison path.
- R05 · Build versions: The build declares Kotlin 2.4.10 and a JVM 21 toolchain, and Chatelaine builds against it without a version override.
- R06 · Record ids: No public function of the library generates an id.
- R07 · Licence and repository: The repository has a GPL-3.0 `LICENSE`, and the spec names no private project.
- R08 · How the host app uses it: Chatelaine's build resolves the library from the local folder before the switch, and from a tagged JitPack release after it.
- R09 · Package name: Every source file sits under `com.hazia.pricecompare`.
- R10 · Units: Each unit converts to its base unit exactly: 1 kg = 1 000 000 mg, 1 dl = 100 ml, 1 m = 1 000 mm.
- R11 · One dimension per pack: A tag holding packs sold by weight and packs sold by count ranks them in two groups, never in one list.
- R12 · Multipacks: 6 × 330 ml has a size of 1 980 ml, and 12 × 1.5 L one of 18 000 ml.
- R13 · Loose goods: Loose apples at 2,40 €/kg rank between packed apples at 2,20 €/kg and 2,60 €/kg.
- R14 · Units shared with the host: The host app can build a quantity from the library's unit type and compare it with a pack's size.
- R15 · Reference unit: A spice at 1,20 € for 40 g shows 30,00 €/kg by default and 3,00 €/100 g with the override.
- R16 · Prices and their history: Recording 1,59 € and then 1,29 € keeps both, with 1,29 € regular; correcting 1,29 € to 1,25 € leaves two observations, not three.
- R17 · Stores: The same pack priced at two branches of one chain ranks each branch's price on its own.
- R18 · Currency: Every price record has a currency field, and a ranking mixing currencies is refused.
- R19 · Carried by a store: A "not carried" mark followed by a newer price makes the pack carried again, and a newer mark after a price makes it not carried.
- R20 · Deleting a product or a store: After a store is deleted, no ranking shows a price from it, and an earlier receipt's lines and total are unchanged.
- R21 · Discount kinds: Each row's example comes out as stated.
- R22 · Entering a percentage off: 1,99 € at 30% off gives 1,39 €, and overwriting it with 1,40 € keeps 1,40 €.
- R23 · Promotion dates: A promotion with no end date is active on any later date until removed; one ending on 30 November is inactive on 1 December.
- R24 · Promotion and regular price: A pack at 1,59 € regular with a "3 x 2" active ranks by the bundle price (R31) and by 1,59 € once it ends; a pack with only an ended promotion stays in the ranking, flagged.
- R25 · No stacking on one pack: Adding a second promotion to a pack at a store with one active replaces the first.
- R27 · Unit-price ranking: The three packs rank in that order with those unit prices and gaps; this is the library's first test.
- R28 · One row per pack: A pack priced at three stores gives one row carrying three prices.
- R29 · Ties: Two packs both shown at 2,65 €/kg put the 1 kg pack before the 600 g pack, whatever their exact unit prices.
- R30 · The store a row shows: A pack at 2,99 € at store A seen on 1 November and 2,99 € at store B seen on 10 November shows store B.
- R31 · Multibuys in the ranking: Cookies Q 600 g at 1,59 € under "3 x 2" rank at 1,77 €/kg, labelled "when buying 3".
- R32 · Ranking date: Ranking for the day before a promotion starts uses the regular price, and for its first day the promotion.
- R33 · Stale prices: A price seen 61 days before the ranking date is flagged and keeps its place.
- R34 · Displayed values: 2,6499 €/kg displays as 2,65 in an Italian format and 2.65 in an English one.
- R35 · Hidden packs: Hiding a pack removes it from the ranking and from quick comparisons, and unhiding restores it with its prices.
- R36 · Quick comparison of unsaved packs: Two unsaved packs typed in the aisle rank as R27 would rank them, and saving one passes it to the host as a new price.
- R37 · One product across stores: Cookies Q 600 g priced at three stores give three rows, cheapest per kg first.
- R38 · Line totals: Each row gives its line total, and after the "3 x 2" line the pack's regular price is still 1,59 €.
- R39 · Discount on a whole receipt: Each row gives its lines and total, and the lines always add up to the total paid.

## Out of scope

### D-R40 · Not in v1

**Rule:** The features below are not in v1, each for its reason.

**Values:**

| Feature | Why not in v1 |
|---|---|
| imperial units | the owner shops in metric |
| converting between dimensions (10 eggs ≈ 600 g) | needs a conversion per product, which the user would have to maintain |
| one multibuy across a range of packs | a promotion applies to one pack, which keeps the discount model simple |
| importing online catalogues | shops publish incomplete catalogues, often only their promotions |
| barcode scanning and photos of labels | the host app plans no barcode lookup |
| Open Prices integration | the library never fetches prices online (Chatelaine's spec, rule 119) |
| loyalty-card prices and coupons | they depend on the shopper, not the shelf |
| the EU 30-day prior-price check | the full price history is kept (R16), so it can be added later |
| weighing a bigger pack against using it up before it expires | needs usage rates and expiry from the host |

**Acceptance:** none: a scope boundary.

**Since:** 2026-11-24 · owner · agreed rewording
