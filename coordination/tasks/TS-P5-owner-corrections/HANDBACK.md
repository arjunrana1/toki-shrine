# Senior Codex handback — TS-P5-owner-corrections

- **Submission:** `7fbb692` (`Apply Phase 5 owner testing corrections`).
- **Base:** `59596a8`.
- **Implementer:** senior Codex, 23 September 2026.
- **Owner evidence:** [OWNER-CHECKS](OWNER-CHECKS.md) records the installed-build observations that define this correction set.

## Delivered

- Gate presentation resolves installed app labels while retaining raw package/domain values for validation and events; independently rotates the seven approved headlines, eleven subtexts, and eleven owner-supplied images. Each image uses cropped-ground plus centered-fit layers. Headline typography is centered regular Inter; CTAs have equal geometry and the challenge CTA restores `🚩🤨`.
- Walk-away shows `🎉`, remains for tap or ten seconds, and both paths launch Android home.
- `BlockActivity` is retained in its task and single-top. Background/lock/Back is nonterminal: waiting resets to the full duration with a new generation that rejects stale ticks; typing keeps its passage/text. A detection relaunch brings an unfinished challenge forward rather than replacing it. Explicit escape/completion semantics and repository writers are unchanged.
- Typing uses `Never mind`, a visible Submit-only path, a runtime length guard, and mismatch styling revealed only by a failed explicit submission. Editing hides the marks until the next Submit. Existing paste/autocorrect safeguards remain.
- The waiting surface matches the owner reference structure: timer, “Stay on this screen”, reset explanation, no Counting pill, and a full-width outlined escape action.
- Debug source-set limits allow pause typing/waiting down to 20 and use disable ladders 20/350/700 characters and 20/360/720 seconds. Release source-set limits remain 100/60 and 220/350/700 plus 180/360/720. Defaults, maxima and 5-minute pause minimum are unchanged. PRD/components/phase records carry the Phase 7 removal obligation.

## Verification

- `./build.sh testDebugUnitTest` — **PASS, 146/146** from fresh XML results. New coverage includes nonterminal typing preservation, waiting reset/stale-tick rejection, mismatch reveal state, submit-length gating, copy/asset pools, retained Activity contract, and debug values.
- `./build.sh assembleDebug` — **PASS**.
- `./build.sh assembleDebugAndroidTest` — **PASS, compile-only**. No Android test was executed.
- `./build.sh assembleRelease` — **PASS**, including the production-limit release source set and release lint-vital tasks.
- `git diff --check` — **PASS**.
- Owner assets `sys_block_7–11.jpg` match their copied drawable resources by SHA-256.

## Direct correction addendum — 23 September 2026

Arjun's direct follow-up after install: move the gate headline and humour line into the top segment of the screen — generous 20–25px padding above the heading, centered, same type treatment; everything else unchanged. Implemented under the direct owner-correction path as commit `9d411d4` (`Move gate headline cluster to top segment`): one file, `BlockGateScreen.kt`, reanchors the headline cluster directly under the block name with a 22.dp top pad; the CTAs, background layers, scrim, and all runtime behavior are untouched. Verification was proportionate to the layout-only change: `./build.sh assembleDebug` — **PASS**. No JVM or Android test exercises this composable, so no test run was performed for it. The rebuilt APK was installed on SM-S918B the same day; attribution is in [OWNER-CHECKS](OWNER-CHECKS.md).

## Limits and review focus

- No device, emulator, adb, install, screenshot, database extraction, or instrumented execution occurred. UI appearance, app-label behavior, Android-home routing, recents/relaunch behavior, screen-lock handling, and IME behavior remain owner-device evidence after independent review.
- Process-death/reboot continuation is not required. Android saved-state may preserve some state during framework recreation, but this submission makes no process-death/reboot guarantee.
- Phase 6 pause/access/re-arm, bubble, and notification remain untouched. Successful Phase 5 challenge completion still ends at the existing `PauseRequested` seam.
- Reviewer should focus on lifecycle ordering (`onStop`/screen-off/relaunch), stale waiting ticks, exactly-once explicit outcomes, raw-target versus display-label separation, release/debug bounds, and submission-gated mismatch state.

Next prompt: `Read AGENTS.md and independently review TS-P5-owner-corrections submission 7fbb692 against base 59596a8. Record the verdict and stop before installation, owner retesting, or Phase 6.`
