# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 17.

## Phase 2 post-validation fixes — 13 September 2026

- **Owner device feedback on fe836dc (six fixes, one pass).** Recorded as PRD §17 "Post-validation addenda" and implemented:
  - **Step 1 scroll + pinned CTA (the blocker).** With many selected apps/sites the selected-list area now scrolls on its own (`weight(1f)` + `verticalScroll`) and the "N selected" + Next row is pinned at the bottom with 14 dp above and the root's 16 dp below — the same structure as the friction step. Previously a long selection pushed Next below the fold with no scrolling anywhere.
  - **Shared bounded target list (`ui/components/TargetList.kt`).** The create review's R6 list was extracted into `BoundedTargetList` (46 dp rows, hairline dividers, four-row cap, surface card) and reused by the block detail screen, whose APPS & SITES section previously still showed the old chips. A 3 dp scrollbar thumb on the right edge appears only while content overflows the cap (`scroll.maxValue > 0`), sized by viewport/content ratio (24 dp minimum) and tracking the scroll offset. Detail rows resolve app icons via a new `InstalledAppsRepository.iconFor(packageName)` (null → glyph fallback, mirroring `labelFor`); rows are built once per load with `remember(loaded)`.
  - **Name cap 20 → 30.** `NAME_MAX_CHARS` 30; PRD §17 R7 updated in place (supersedes 20, which superseded 40). Test renamed `nameInputStripsLineBreaksAndTruncatesAtThirtyCharacters` with boundary/over-limit/strip cases rebased on 30. No migration (early build, owner-confirmed).
  - **Launcher icon from `design/icon-final`.** Integrated per the pack's README: the five `ic_launcher`/`ic_launcher_round` rasters copied into `res/mipmap-*dpi`, adaptive XMLs rewritten to a full-bleed foreground (the 1024 master resized to 108 dp densities: 108/162/216/324/432) over `@color/ic_launcher_background` (`#161826`, already present), and the template `drawable/ic_launcher_foreground.xml` vector removed. The README's monochrome layer was dropped (the raster artwork cannot provide a clean themed icon; vector redraw remains future polish). Manifest references unchanged.
  - **Home actions.** Left button is now **Feedback** — label only, the chat glyph removed (owner: "Share feedback" looked uneven). Feedback + New Block row gained 14 dp padding above (16 dp below already existed via root padding).
- **Checks:** assembleDebug, testDebugUnitTest (7 JVM green), assembleDebugAndroidTest compile — all passing. Instrumented execution still pending; device/visual verification (icon shape under One UI masks, scrollbar feel, step-1 scrolling, detail list) belongs to the owner test pass that follows the requested reinstall.
