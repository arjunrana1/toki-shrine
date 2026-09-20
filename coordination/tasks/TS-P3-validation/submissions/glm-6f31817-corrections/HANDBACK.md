# GLM handback — TS-P3-validation

Current submission: direct owner corrections, 19 September (round 2). The prior P3-F01–P3-F06 repair handback is preserved verbatim at [submissions/glm-7d779a6-repair/HANDBACK.md](submissions/glm-7d779a6-repair/HANDBACK.md); the pre-migration builder record remains at [submissions/imported-cb1ad45/HANDBACK.md](submissions/imported-cb1ad45/HANDBACK.md).

- **Submission:** `6f31817` on `7d779a6` (Phase 3 base `6c3b962`). Six files changed; all unrelated migration/PRD/branding/design edits preserved untouched.
- **Implementer:** GLM 5.3 under the direct owner→GLM correction path (WORKFLOW); owner message of 19 September 11:40 with one screenshot (Samsung One UI Accessibility list).
- **Scope:** exactly the five actionable requests; one request (highlighting the system "Installed apps" row) was assessed as not implementable and is recorded below without a code change.

## Owner corrections implemented

- **Welcome copy** (`WelcomeScreen.kt`; affects P3-01, P3-15): headline replaced with "Your time, your rules" (28 sp) and the three owner lines — "Doomscrolling? Time-blindness? Yeah, we got you." (15 sp step300), "We don't block you! We just do a vibe check before you fall in." and "You choose what gets paused, for how long, and how you get back." (14 sp step500). Supersedes the PRD §6 screen-1 wording; single Get started CTA and no-gating behavior unchanged.
- **Appbar top spacing** (`NocturneUi.kt`; affects P3-15 and all appbar screens): NocturneAppbar top padding 4 → 14 dp, so back controls no longer sit close to the status-bar edge. One shared component change; screen content below shifts down uniformly.
- **Battery guidance manufacturer-agnostic** (`BatteryInstructionsScreen.kt`, `OemBattery.kt`, `OemBatteryTest.kt`; affects P3-09): removed the "Not a Samsung? Pick your phone" CTA and override dialog entirely (the saveable `selected` state is gone — detection is derived from `Build.MANUFACTURER` and nothing user-set survives recreation); Samsung intro now reads "Android is aggressive about closing background apps. …"; automatic Samsung-steps/generic-dialog split and both Settings destinations unchanged; `BatteryOem.pickerLabel` removed with the picker and the pinned test updated accordingly (instructions/detection assertions unchanged).
- **Review-step helper padding** (`CreateFlowScreen.kt`; affects P3-14/P3-15): the "Turn it on from the "Blocks" homescreen." helper now also carries 10 dp bottom padding (10 dp top retained).

## Assessed, not implemented

- **Highlight the system "Installed apps" row in Android's accessibility settings:** not possible for a normal app. That list is OS/One UI-owned UI an app cannot badge or annotate; the workarounds fail at exactly that moment (our accessibility service is not yet granted — that is what the screen is for — and an overlay on system Settings would need the overlay permission that is granted later in this same flow). Feasible mitigation awaiting owner approval because it edits the P3-F03-locked explainer copy: add one guidance line to our explainer, e.g. "On the next screen: tap Installed apps, then Toki Shrine." No change made.

## Checks (GLM, non-device, this machine, 19 September 2026)

- `./build.sh assembleDebug` — BUILD SUCCESSFUL.
- `./build.sh testDebugUnitTest` — BUILD SUCCESSFUL; **45 tests, 0 failures, 0 errors** from `app/build/test-results/testDebugUnitTest/` XML (picker-label assertion replaced by an instructions-only check; count unchanged).
- `./build.sh assembleDebugAndroidTest` — BUILD SUCCESSFUL (compile only).
- Legacy hex-color grep outside `ui/theme/` — no matches.

## Deployment record

- Debug APK from `6f31817` keep-data reinstalled on Arjun's SM-S918B (`R5CW30ZBM2R`, Android 16) at owner request, 19 September 2026; install verified via package lastUpdateTime. App not launched by the agent; no exploratory testing.

## Limits and readiness

- Self-verification by the implementer only; these corrections ride the direct owner→GLM path and are pending Codex review as an accumulated delta at the next checkpoint (per WORKFLOW, new changes do not inherit the `7d779a6` PASS).
- Owner retest guidance: welcome visuals (P3-01/P3-15), battery flow without picker incl. intro copy (P3-09 — its original "picker opens and changes guidance" expectation is superseded by this owner decision), review-step helper spacing (P3-14), plus the still-open P3-02/03/06/08/14/15 set and precise P3-01 behavior results. P3-NV-02–P3-NV-04 unchanged. Phase 4 not cleared.

## Next-role prompt (paste-ready)

> Read AGENTS.md and resume [TS-P3-validation](../TASK.md) as reviewer (Codex). Re-review the direct owner-correction delta `7d779a6..6f31817` (welcome copy, appbar spacing, battery picker removal, review-step padding; details in HANDBACK.md), then Arjun continues owner retests on installed build `6f31817`.
