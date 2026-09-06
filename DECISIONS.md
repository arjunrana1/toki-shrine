# Decisions log

Running record of decisions made during the build. Read this at the start of every session; append to it whenever you resolve something.

Format: newest at the bottom of each phase. Record what was decided, why, and anything it constrains later.

---

## Pre-build

Decisions settled before implementation began are recorded in `PRD.md` §13 with their rationale — stack, minSdk, no third-party dependencies, local-only event store, bundled JSON over Remote Config, feedback by email intent, dark theme only.

Platform findings verified on hardware are in `PRD.md` §14.

---

## Phase 0 — Project skeleton and Nocturne theme

_(append below)_

## Phase 0 — Project skeleton and Nocturne theme

- **Toolchain versions (Phase 0).** AGP 8.4.1, Kotlin 1.9.24, Compose compiler 1.5.14, Compose BOM 2024.06.00, activity-compose 1.9.0, core-ktx 1.13.1, lifecycle-runtime-ktx 2.7.0, Room 2.6.1 (runtime + ktx only — no KSP/room-compiler until Phase 1 needs it). Constrained by the committed Gradle 8.7 wrapper and JDK 17; HANDOVER.md warns AGP newer than ~8.4 won't run on Gradle 8.7. AGP 8.4.1 and Kotlin 1.9.24 were also already in the local Gradle cache. **Constrains later phases:** do not bump any of these without re-checking the Gradle 8.7 ceiling.
- **compileSdk 36 on AGP 8.4.1 (Phase 0).** AGP 8.4.1 is only tested up to compileSdk 34, so `android.suppressUnsupportedCompileSdk=36` was added to `gradle.properties` (the property AGP itself suggests) to silence the warning. targetSdk stays 36 per the stack spec. Compiling and packaging work; if a future phase hits an API that behaves differently under the older compiler, revisit this pairing rather than Gradle.
- **CSS px → dp/sp 1:1 (Phase 0).** The design renders at a 400×800 CSS px viewport (`design/regenerate-screens.py`), so one CSS px maps to one dp, and text px to sp. No scaling factor.
- **Inter bundled as a font resource (Phase 0).** The Nocturne type token requires Inter; Android doesn't ship it, so `inter_regular.ttf` (400) and `inter_medium.ttf` (500) live in `res/font/`. The CSS @import also loads 600/700 but no token or component rule uses them — not bundled. Anton stays a Phase 5 asset.
- **Shadow tokens translated as edge + ambient (Phase 0).** Nocturne shadows are a 1px hairline edge plus an offset blur; Compose can't offset blurs, so the translation is: edge colour applied as a border, blur as `Modifier.shadow` with elevation ≈ blur ÷ 2 (md 18px → 9dp, lg 40px → 20dp), and the `rgba(0,0,0,α)` layer carried as `shadowColor` alpha (0.55 / 0.65). Pure black here is the design readme's stated shade exception.
- **Hex in `res/values/colors.xml` (Phase 0).** The launch-window background (`android:windowBackground`) and launcher icon must resolve before any Kotlin runs, so `#161826` and `#9184d9` are duplicated there, commented as mirroring the tokens. The hex-grep acceptance only covers `app/src/main/java`, and these two values are taken from tokens, not invented.
- **Launcher icon (Phase 0).** Minimal adaptive-icon XML only (minSdk 33 > 26, so no bitmaps needed): a torii-gate mark stroked in the accent colour on the bg colour. Placeholder-quality by design; replace when a real mark exists.
- **Material 3 mapping (Phase 0).** Nocturne has no M3 colour scheme, so one was mapped: primary = accent, onPrimary = bg (matches the CSS radio checked state), primaryContainer = accent-800 / onPrimaryContainer = accent-100 (the .tag-accent pair), outline = neutral-700, outlineVariant = divider, onSurfaceVariant = neutral-400, and all `surfaceContainer*` = surface so M3 dialogs read like the CSS `.dialog`. Typography maps h1–h6/body plus the component-layer sizes (14/13/12/11/10) onto M3 slots; shapes map sm/md/lg onto the five M3 slots. Ramps are exposed as `NocturneTheme.colors.*` alongside the M3 scheme.
- **--color-divider alpha (Phase 0).** `color-mix(in srgb, #e9e9ed 16%, transparent)` → `0x29E9E9ED` (16% of 255 ≈ 41 = 0x29).
- **Phase 0 UI scope (Phase 0).** The empty themed screen is literally empty — a full-bleed bg-colour box under `NocturneTheme` with edge-to-edge system bars. Screen 5 (block list) belongs to Phase 2; nothing beyond the theme was built.

---

## Open questions

_Anything blocking. Write the question here, report it, and wait for an answer rather than guessing._
