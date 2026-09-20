# Theme, UI and branding

Read for visual implementation or shared controls. PRD §§6/15 and §17 addenda override stale mock copy/layout. Canonical token files: [styles.css](../../toki-shrine-ui-mockups/project/_ds/nocturne-82412343-c17a-4e25-85de-f680ed74498c/styles.css), [design rules](../../toki-shrine-ui-mockups/project/_ds/nocturne-82412343-c17a-4e25-85de-f680ed74498c/readme.md). Read only relevant screen markup/render in `toki-shrine-ui-mockups/project/TimeShrine Mocks.dc.html` and `design/screens/`.

Source: `app/src/main/java/com/arjunrana/tokishrine/ui/theme/`, `ui/components/NocturneUi.kt`, `TargetList.kt`, `ui/icons/Phosphor.kt`, resources under `app/src/main/res/`.

## Active design decisions

- Nocturne dark theme; use existing tokens/theme, no invented hardcoded token values. Outlined secondary buttons remain outlined; no pure black/white except explicit design exceptions. Phosphor glyph font, Inter regular/medium; Anton is for interruption assets. CSS px maps 1:1 to dp/sp in the established implementation; do not apply a second density scale to translated tokens.
- Preserve all Material typography and palette slots, including inverse/error/scrim. Owner-approved error family: #d4716b / #612421 / #ffebe8, onError = theme background. These approved values are the documented exception where original tokens lacked an error family.
- Shadows approximate CSS edge/ambient/elevation; do not claim exact CSS offsets. First relevant consuming surfaces still need owner visual comparison.
- Window/icon resource colors mirror canonical tokens because Kotlin theme is unavailable at startup. A hex grep alone cannot prove all visual conformance.
- Current launcher uses the selected raster torii/hourglass, integrated before Phase 3. [Icon pack](../../design/icon-final/README.md) describes assets. Vector/themed monochrome redraw remains polish; do not replace installed assets during workflow work.
- Screen roots respect both status- and navigation-bar insets. Shared top navigation uses the owner-approved app-bar gap, the Blocks home header has its explicit top separation, and bottom actions remain inside the navigation safe area. The owner accepted these corrections on `f92661d`; preserve the shared treatment when adding or changing screens.

## Naming and open obligations

Owner recorded a future rename to **Time Shrine**. Current app/spec/package remain Toki Shrine / com.arjunrana.tokishrine; perform rename only as a separately assigned change. Historical root “use Toki everywhere” referred to current implementation, not cancellation of the recorded rename.

Glyph-only shared controls still need semantic labels/roles as touched, with release accessibility review. Haptic tuning is tracked in [navigation/permissions](navigation-permissions.md). Arjun owns visual/device acceptance; source/compilation is not a pixel or feel pass. Routine owner-reported visual corrections may use the direct GLM path in WORKFLOW.
