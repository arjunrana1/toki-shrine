# Phase 0 verification — FAIL

Verified 9 September 2026 on Samsung Galaxy S23 Ultra SM-S918B, Android 16, one USB connection. Owner confirmed GLM idle.

Reviewed implementation commit `d5349ec1f980c47f8fec32e7e5cd6d1926fa3fde` at repository HEAD `fa3e31ac7dcc53b2cccc4373dd43868150bb7c74`. Working tree was clean before and after verification. No feature code, configuration, or acceptance criteria changed. No commit or push made.

The five explicit acceptance checks pass, but the phase fails the broader theme-conformance review: two omitted mappings leak Material defaults into the shared theme. Fix these before Phase 1. The empty screen cannot expose either defect.

## Blocking findings for GLM

### P2 — Define titleMedium using the Nocturne type system

File: `app/src/main/java/com/arjunrana/tokishrine/ui/theme/Type.kt`, lines 35–45 (insertion near line 42).

`NocturneTypography` supplies 14 of Material 3's 15 typography slots and omits `titleMedium`. The resolved Material 3 1.2.1 constructor therefore uses `TypographyTokens.TitleMedium`, whose font is the generic family, with 16sp size, 24sp line height, and 0.2sp tracking. It does not inherit the bundled Inter family or the Nocturne heading metrics from neighboring slots. Any consumer of `MaterialTheme.typography.titleMedium` gets an inconsistent style despite using the theme correctly. Explicitly map the slot to an appropriate Nocturne-derived TextStyle and document the mapping. Confirm all 15 slots use Inter and intentionally chosen design metrics.

Evidence: local resolved AAR inspected with javap; `typography-bytecode.txt` constructor default and `type-scale.txt` TitleMedium initialization are included with this report.

### P2 — Complete the Material colour-role mapping

File: `app/src/main/java/com/arjunrana/tokishrine/ui/theme/Theme.kt`, lines 155–183.

`darkColorScheme` leaves `inversePrimary`, `inverseSurface`, `inverseOnSurface`, `error`, `onError`, `errorContainer`, `onErrorContainer`, and `scrim` unspecified. In the resolved Material 3 1.2.1 implementation these come from ColorDarkTokens, not from the supplied Nocturne colors. That introduces Material's error palette and inverse colors, plus a black scrim, into the supposedly canonical theme. Components using these roles can render outside the approved palette even though they reference MaterialTheme correctly; the source hex grep cannot detect this. Explicitly map these roles to Nocturne tokens and document the semantic mapping. If a new semantic color is desired, raise it as a design decision rather than silently retaining a library default. `surfaceTint` defaults to the supplied primary in this version and is not part of this finding.

Evidence: `colorscheme-bytecode.txt` darkColorScheme default implementation and `color-tokens.txt` token initialization, extracted from the actual resolved AAR.

## Acceptance evidence

| Requirement | Result | Evidence |
|---|---|---|
| Debug build exits 0 | PASS | `./build.sh assembleDebug`: exit 0, BUILD SUCCESSFUL in 5s; 35 actionable tasks, 1 executed and 34 up-to-date. Wrapper supplies required toolchain. This was an incremental build, not a clean rebuild. |
| Install succeeds | PASS | `adb install -r app/build/outputs/apk/debug/app-debug.apk`: Performing Streamed Install / Success. |
| Empty screen background #161826 | PASS | `adb shell am start -W -n com.arjunrana.tokishrine/.MainActivity`: Status ok, COLD, TotalTime 552ms. Window focus confirmed MainActivity. Screenshot 720×1544; every pixel in rectangle x=[20,700), y=[100,1444) is RGB(22,24,38), exactly #161826. System bars and Samsung edge handle excluded. Screenshot visually inspected. |
| Prescribed hex grep returns no matches outside theme | PASS | Exact AGENTS.md pipeline produced no output. Additional rg for Kotlin Color literals and 0x literals outside ui/theme also produced no matches. |
| All CSS root tokens defined | PASS, with shadow caveat below | 51 root tokens represented: 35 literal colors matched by name and value, divider alpha 0x29, six spacing values, three radii, three font tokens, and three shadow structures. Font metadata confirms Inter weights 400/500. Shadow edges and alpha match, with documented elevation approximations. |

The first sandbox build/adb attempts failed because cache writes and adb server binding were restricted. Approved retries outside the sandbox succeeded. These are environment restrictions, not app failures.

## Non-blocking note

File: `app/src/main/java/com/arjunrana/tokishrine/ui/theme/Theme.kt`, lines 129–148.

The shadow translation is explicitly approximate: CSS blur radii 18/40 become elevation 9/20dp. The token structure does not preserve CSS Y offsets 6/16 or the 1px edge width. The blank Phase 0 screen exercises none of the shadows, so this review verifies their representation, not visual equivalence. Preserve this limitation in the decision log and compare the first consuming card/dialog against its reference before relying on the approximation across screens. No shadow rendering implementation is requested in Phase 0.

## Other review conclusions

- Read all Phase 0 text implementation/configuration files in full, supporting build scripts and wrappers; inspected bundled font metadata and launcher XML resources. Reviewed instructions, PRD, decisions, canonical CSS, and design-language rules.
- Scope remains project skeleton, theme, and empty launch screen. Room is declared without premature entities, repositories, or services. No later-phase feature stubs found.
- applicationId, minSdk 33, targetSdk 36, Kotlin/Compose and dark-only configuration match the specification. Gradle remains 8.7. The compileSdk warning suppression is documented in DECISIONS.md; build success does not establish future API compatibility.
- App manifest declares no permissions, including no INTERNET permission. Direct app dependencies are AndroidX/Compose/Room only.
- Accessibility detection, permissions, data persistence and later-phase UI were not tested. Phase 0 has no accessibility service to re-enable after installation.
- No implementation patch was made. GLM should fix the two blocking findings and return Phase 0 for re-verification.
