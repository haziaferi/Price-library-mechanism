# Price-compare

A Kotlin Multiplatform library that ranks packs by unit price across stores, applies promotions, and does receipt arithmetic in whole cents. It has no storage and no screens: the host app passes what it stores. Its first host is Chatelaine's Shopping module.

`SPEC.md` holds the rules; `SPEC-history.md` records each change. Tests cite the rule they check as `SPEC:Rnn`.

## Build

```
./gradlew check assemble
```

JDK 21 and an Android SDK with platform 37 are needed. `check` runs the tests; `assemble` builds the JVM jar and the Android AAR.

## How changes land

Every change reaches `main` through a pull request that the owner reviews and merges (squash). The CI gate (`./gradlew check assemble`) and CodeQL must pass first; the repository's ruleset enforces all three: pull requests, the `Gate` check and CodeQL. Version tags start when Chatelaine moves to JitPack releases (SPEC rule R08).

## Licence

GPL-3.0, see `LICENSE`.
