# GLM repair handback — TS-P3-validation, P3-F01–P3-F06

Builder submission 19 September 2026. The prior imported builder handback for `cb1ad45` is preserved verbatim at [imported-cb1ad45/HANDBACK.md](../imported-cb1ad45/HANDBACK.md).

- **Submission:** `7d779a6` on `cb1ad45` (Phase 3 base `6c3b962`). Exactly nine files changed; every unrelated migration/PRD/branding/design edit in the working tree is preserved untouched.
- **Implementer:** GLM 5.3 under the direct owner-correction path in [TASK](../../TASK.md); findings and owners from [REVIEW](../../REVIEW.md) (19 September section).
- **Scope:** exactly P3-F01–P3-F06. No other behavior, dependency, test-assertion or tooling change.

## Changes per finding

- **P3-F01 — overlay permission.** `app/src/main/AndroidManifest.xml`: added the `android.permission.SYSTEM_ALERT_WINDOW` declaration (comment records why its absence hid the app from the system list). The user-mediated Settings route, `Settings.canDrawOverlays` real-state check and request/event flow are unchanged. Proportionate manifest assertion added: new `app/src/test/java/com/arjunrana/tokishrine/AndroidManifestTest.kt` reads the source manifest (Gradle module working dir) and fails if the declaration is removed.
- **P3-F02 — checklist layout/copy.** `AppPermission.kt`: declaration order is now the display order (Accessibility, Battery, Overlay, Notifications) with a new `essential` flag; overlay row description is now "Shows the pause screen and the timer bubble" per the owner-supplied `design/screens/Accessibility screen design v2.png`. `eventValue` strings unchanged. `PermissionChecklistScreen.kt`: new `PermissionGroup` band renders "Essential permissions" / "For a better experience" section headers (11 sp caps, 0.88 letter-spacing) over the grouped rows; subtitle replaced with "Two are essential. The rest just make Toki Shrine nicer to live with."; onboarding-mode heading top padding raised to 56 dp (appbar modes keep their previous spacing). Exactly four real-state rows, Grant/Granted badges only, live n-of-4 progress and the Continue CTA are retained unchanged.
- **P3-F03 — accessibility explainer content.** `AccessibilityExplainerScreen.kt`: exact owner-supplied replacement from `design/screens/Accessibility Explainer.png` — heading "About the accessibility screen"; body naming "full control of your device" and "view everything you do."; WHAT WE DO: "See which app or site is open", "Show the pause screen over it"; WHAT WE NEVER DO: "Read the contents of any page", "Tap, type, or act for you", "Log, store, or send your activity anywhere". Structure, styling, "Continue to settings" CTA, explainer-before-Settings ordering and return-refresh behavior unchanged.
- **P3-F04 — ON haptic length.** `Haptics.kt`: `turnedOn` now plays `EFFECT_HEAVY_CLICK` followed 30 ms later by a settling `EFFECT_CLICK` (main-looper `postDelayed` via `androidx.core.os.postDelayed`; constant `ON_SETTLE_DELAY_MILLIS`). `turnedOff` is untouched. Callers, ordering, single-flight and failure/no-op semantics unchanged; every segment stays a device-tuned predefined effect. Per REVIEW, acceptance is owner feel-testing, not a measured percentage.
- **P3-F05 — battery completion CTA.** `BatteryInstructionsScreen.kt`: removed the "I've done this" ghost button and the `onDone` parameter; `MainActivity.kt` wiring removed with it (unused `ButtonVariant` import dropped). Open battery settings, manufacturer picker, Back behavior and refresh-on-return unchanged. No battery-impact claim added anywhere.
- **P3-F06 — post-save guidance.** `CreateFlowScreen.kt` step 4: centered helper `Turn it on from the "Blocks" homescreen.` below Save block (11.5 sp, neutral step500, 10 dp top padding — the same below-CTA helper pattern as the battery screen's "Not a Samsung?" line). Save enablement, save-OFF behavior and activation gating untouched.

## Checks (GLM, non-device, this machine, 19 September 2026)

- `./build.sh assembleDebug` — BUILD SUCCESSFUL. (One transient compile failure during the session: `Handler.postDelayed` argument order; fixed with the `androidx.core.os.postDelayed` extension, then clean.)
- `./build.sh testDebugUnitTest` — BUILD SUCCESSFUL; **45 tests, 0 failures, 0 errors** read from `app/build/test-results/testDebugUnitTest/` XML (44 prior classes/tests + 1 new `AndroidManifestTest.overlayPermissionIsDeclared`).
- `./build.sh assembleDebugAndroidTest` — BUILD SUCCESSFUL (instrumented compilation only; no execution, per the no-device boundary).
- Legacy Kotlin hex-color grep over `app/src/main/java` excluding `ui/theme/` — no matches.

## Limits and readiness

- Self-verification by the implementer only. Stopping for Codex re-review of the `cb1ad45..7d779a6` repair delta and affected dependencies.
- No device operations performed or requested. Owner retest needed on an installed, identified build: P3-02, P3-03, P3-06 (feel), P3-08 (grant/revoke after manifest fix), P3-09, P3-14 (including the still-unreported behavioral matrix) and P3-15; a precise P3-01 result is still missing. P3-NV-02–P3-NV-04 remain open exactly as recorded in [OWNER-CHECKS](../../OWNER-CHECKS.md). Phase 4 not cleared.
- Coordination records remain uncommitted alongside the owner's pending documentation migration; only the code repair was committed (`7d779a6`).

## Next-role prompt (paste-ready)

> Read AGENTS.md and resume [TS-P3-validation](../../TASK.md) as reviewer (Codex). Re-review the repair submission `7d779a6` (range `cb1ad45..7d779a6`) against findings P3-F01–P3-F06 in REVIEW.md and affected dependencies only; HANDBACK.md records the exact changes and checks. Then owner retests P3-02/03/06/08/09/14/15 and reports P3-01.
