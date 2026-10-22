# Theme, UI and branding

Read for visual implementation or shared controls. PRD §§6/15 and §17 addenda override stale mock copy/layout. Canonical token files: [styles.css](../../toki-shrine-ui-mockups/project/_ds/nocturne-82412343-c17a-4e25-85de-f680ed74498c/styles.css), [design rules](../../toki-shrine-ui-mockups/project/_ds/nocturne-82412343-c17a-4e25-85de-f680ed74498c/readme.md). Read only relevant screen markup/render in `toki-shrine-ui-mockups/project/TimeShrine Mocks.dc.html` and `design/screens/`.

Source: `app/src/main/java/com/arjunrana/tokishrine/ui/theme/`, `ui/components/NocturneUi.kt`, `TargetList.kt`, `ui/icons/Phosphor.kt`, resources under `app/src/main/res/`.

## Active design decisions

- Nocturne dark theme; use existing tokens/theme, no invented hardcoded token values. Outlined secondary buttons remain outlined; no pure black/white except explicit design exceptions. Phosphor glyph font and Inter regular/medium. The carried Anton asset is not used by the owner-corrected interruption headline, which is centered regular Inter. CSS px maps 1:1 to dp/sp in the established implementation; do not apply a second density scale to translated tokens.
- Preserve all Material typography and palette slots, including inverse/error/scrim. Owner-approved error family: #d4716b / #612421 / #ffebe8, onError = theme background. These approved values are the documented exception where original tokens lacked an error family.
- Shadows approximate CSS edge/ambient/elevation; do not claim exact CSS offsets. Closed-phase visual evidence remains attributed to those builds; new/touched surfaces still need the applicable owner visual comparison.
- Window/icon resource colors mirror canonical tokens because Kotlin theme is unavailable at startup. A hex grep alone cannot prove all visual conformance.
- Current launcher uses the selected raster torii/hourglass, integrated before Phase 3. [Icon pack](../../design/icon-final/README.md) describes assets. Vector/themed monochrome redraw remains polish; do not replace installed assets during workflow work.
- Screen roots respect both status- and navigation-bar insets. Shared top navigation uses the owner-approved app-bar gap, the Blocks home header has its explicit top separation, and bottom actions remain inside the navigation safe area. The owner accepted these corrections on `f92661d`; preserve the shared treatment when adding or changing screens.

## Naming and open obligations

Owner recorded a future rename to **Time Shrine**. Current app/spec/package remain Toki Shrine / com.arjunrana.tokishrine; perform rename only as a separately assigned change. Historical root “use Toki everywhere” referred to current implementation, not cancellation of the recorded rename.

Phase 7 added labels/roles and 44 dp targets to the shared Back and home Stats/Settings controls. Its TalkBack, large-text and final visual acceptance remain pending in [P7-O20–O22](../../coordination/tasks/TS-P7-stats-feedback-final/OWNER-CHECKS.md); this is not proof that every surface has passed accessibility testing. Haptic acceptance is recorded in [navigation/permissions](navigation-permissions.md). Arjun owns visual/device acceptance; source/compilation is not a pixel or feel pass. Routine owner-reported visual corrections may use the direct GLM path in WORKFLOW.

## Phase 7 Stats redesign

Use [S1–S6 PNGs](../../design/screens/stats-redesign/README.md) and the bundled HTML/CSS for layout. PRD §9 and [Stats contract](stats.md) override old mockup copy/metrics. Screen 23 retains Nocturne; the original 23-stats.png is historical. GLM owns presentation, accessible info-button targets, charts, sheets, nested list, dynamic dates, unknown-data display and five-row permission UI. No new fonts, dependencies, palette or app rename.

## Unlock-minutes screens

Gate 14 v2 ([`BlockGateScreen`](../../app/src/main/java/com/arjunrana/tokishrine/ui/interruption/BlockGateScreen.kt)) and step 3 / 09 v3 ([`CreateFlowScreen`](../../app/src/main/java/com/arjunrana/tokishrine/ui/screens/CreateFlowScreen.kt), `StepFriction` and `MethodCard`) follow [design/screens/unlock-minutes](../../design/screens/unlock-minutes/README.md) as amended by PRD §17 (11 October). Motion is spelled out in code constants beside each screen; CSS `ease` is `CubicBezierEasing(.25,.1,.25,1)`. The gate's translucent black/white values are the documented photo-overlay literals (also used by the way-in button); the step-3 cards use Nocturne tokens only. The gate's "Minutes to unlock" row is always composed at a constant height, so opening it never moves the buttons; closed, it is hidden from touch and accessibility. Owner visual/motion acceptance passed in portrait (UM-O1, UM-O4, UM-O5, 22 October 2026). Open: the gate is not fitted for short landscape windows (the 100 dp bottom spacing plus the headline stack can exceed ~360 dp height) and is not rotation-locked; fix or lock before launch if landscape matters.
