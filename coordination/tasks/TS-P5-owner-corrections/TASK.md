# TS-P5-owner-corrections — Owner-tested interruption corrections

- **State:** `closed` — repair submission `68fdcb3` passed independent review (PASS WITH NOTES, RV2-N1–N3; see [REVIEW](REVIEW.md)), and on 23 September 2026 Arjun reported Phase 5 owner testing complete and passing on the installed review-attributed build, closing the P5C-O14 retest (see [OWNER-CHECKS](OWNER-CHECKS.md)). Review notes RV2-N1–N3 are recorded obligations for later phases, not blockers. Phase 6 is now scoped in [TS-P6-pause-lifecycle](../TS-P6-pause-lifecycle/TASK.md).
- **Goal:** apply Arjun's Phase 5 device-testing feedback to the interruption gate, walk-away moment, typing challenge, waiting challenge, and temporary debug-only configuration bounds without beginning Phase 6.
- **Implementation owner:** senior Codex. The visual/copy changes are routine, but preserving typing state and resetting waiting state across app-switch/lock without creating a terminal outcome changes the lifecycle state machine and is senior-owned under WORKFLOW.
- **Base:** `59596a8`. The owner-supplied untracked `design/humor-assets/sys_block_7–11.jpg` files are explicitly part of this task; preserve unrelated work.
- **Authority:** Arjun's 23 September 2026 owner feedback, [Phase 5](../../../docs/phases/phase-05.md), PRD §§6–8/11/17, [theme/UI](../../../docs/components/theme-ui.md), [blocks/targets](../../../docs/components/blocks-targets.md), and [build/validation](../../../docs/components/build-validation.md).

## Accepted owner corrections

- Resolve installed app package names to their user-visible labels; sites continue to display their domain. Rotate seven approved target-aware headlines independently of the eleven approved humour subtexts.
- Match the centered regular Inter reference, layer each selected image as a full-screen cropped ground plus a centered aspect-preserving copy, rotate `sys_block_1–11`, equalize the two gate CTAs, and restore `🚩🤨` on the challenge CTA.
- Show `🎉` on the walk-away count, dismiss on tap or after ten seconds, and route both outcomes to the Android home screen.
- Waiting app-switch/lock is nonterminal: retain the challenge, reset progress to zero, and restart on return. Typing app-switch/lock is nonterminal: retain the passage and entered text. Ordinary in-process background/foreground survival is required; process death and reboot survival are not. Android Back backgrounds and preserves the live challenge. Only the explicit challenge escape is a walk-away/turn-off abandonment.
- Waiting follows the supplied owner reference: timer ring, “Stay on this screen”, reset explanation, no Counting pill, and a full-width outlined escape CTA.
- Typing uses `Never mind` for pause escape. A visible Submit CTA is the only submit path, is disabled until the required character count is reached, and mismatch styling appears only after an explicit failed submission rather than while typing.
- Debug/testing builds temporarily allow pause typing down to 20 characters and pause waiting down to 20 seconds; their defaults and maxima stay unchanged. Debug disable ladders are 20/350/700 characters and 20/360/720 seconds. Release builds retain production values 100–200, 60–300, 220/350/700, and 180/360/720. Pause duration remains 5–100 minutes. Phase 7 must remove the temporary debug overrides before final approval.

## Required correctness

- Backgrounding/locking cannot emit `walk_away`, `challenge_abandoned`, or `turnoff_abandoned`, cannot complete later from a stale waiting tick, and cannot lose typing text while the process remains alive.
- Waiting always returns at the full configured duration. Typing returns with the same passage and text. Explicit escape and completion remain exactly-once terminal outcomes.
- The raw package/domain remains the detection validation and event target; the resolved label is presentation-only with a safe package-name fallback.
- Existing Phase 5 persistence/event atomicity and the Phase 6 `PauseRequested` seam remain unchanged.
- A declined interruption or backgrounded live challenge grants no access: reopening the same triggering app/site must be eligible for a new detection immediately. Repeat suppression remains scoped to duplicate events for the exact target/block while its interruption is active, and successful completion does not use this release path.

## Checks and stop

- Add/update focused JVM coverage for background/reset/preservation/stale ticks, submit-gated mismatch state, selector pools, and debug configuration values. Keep Android/Room coverage compiling.
- Run `./build.sh assembleDebug`, `./build.sh testDebugUnitTest`, `./build.sh assembleDebugAndroidTest` (compile-only), and `git diff --check`.
- Save a replacement handback, update CURRENT to `ready_for_review`, and stop for fresh independent review. No adb, emulator, installation, screenshots, instrumented execution, Phase 6 access/pause/re-arm, bubble, notification, dependency, toolchain, or rename work.
