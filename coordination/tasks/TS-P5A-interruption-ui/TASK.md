# TS-P5A-interruption-ui — Interruption visual surfaces

- **State:** `awaiting_owner` — independent Codex review passed submission `3ba8037` on base `8523006`; see [REVIEW](REVIEW.md). Owner visual/input acceptance remains separate. P5B is eligible to be scheduled but has not started.
- **Goal:** build the reusable, stateless Compose surfaces for the real block gate, walk-away moment, typing challenge/mismatch state and delay countdown, ready for the senior-owned runtime task to integrate.
- **Implementation owner:** GLM 5.3 Pro. This task is bounded visual/mechanical Compose work against explicit models and callbacks. Do not use Flash for the primary submission and do not absorb the senior-owned lifecycle/event state machine.
- **Base:** the committed task-opening HEAD. Before editing, verify a clean worktree and record `git rev-parse HEAD` in `HANDBACK.md`; stop and reconcile with Arjun if the task routing or worktree differs.
- **Authority:** [Phase 5](../../../docs/phases/phase-05.md), PRD §§7, 8, 10, 11 and the applicable §17 amendments, [theme/UI](../../../docs/components/theme-ui.md), and [build/validation](../../../docs/components/build-validation.md). The PNGs are visual references; current PRD copy and values win.
- **Next task:** [TS-P5B-challenge-runtime](../TS-P5B-challenge-runtime/TASK.md) owns Activity integration, time, lifecycle, persistence and events after this submission receives independent review.

## Scope and interface boundary

- Add the six supplied `design/humor-assets/sys_block_1–6.jpg` files as Android drawable resources without recompressing or redesigning them. Add the supplied Anton font as an Android font resource. Keep current Toki Shrine naming and existing launcher assets.
- Implement reusable Compose surfaces for screens 15–19 and 22:
  - block gate: background image plus legible dim treatment, context, one supplied humour line, prominent filled walk-away action and outlined way-in action;
  - walk-away moment: supplied daily count and dismissal callback; no internally owned delay;
  - typing challenge: supplied passage/current text, unconditional per-character mismatch styling, retained typed text, submit and explicit walk-away/never-mind callbacks;
  - delay challenge: supplied total/remaining time and explicit escape callback; render only, with no timer or lifecycle ownership;
  - the typing/waiting surfaces must support both pause and disable copy through an explicit mode/model rather than duplicated screens.
- Keep the UI state hoisted. Composables may format supplied values but must not read Room/repositories, select random passages or images, launch coroutines/timers, observe Activity lifecycle, infer app-switch/screen-off reasons, write events, enable/disable a block or grant a pause.
- Expose explicit callbacks for every user action. A submit callback reports the current text; it does not decide completion. The runtime task decides all terminal outcomes and ignores duplicate callbacks.
- Typing UI always shows typo positions; never read or expose `show_typos`. Suppress paste/context-menu insertion and configure the IME for no autocorrect or predictive suggestions. Do not clear typed text on mismatch.
- Use existing Nocturne tokens/shared controls, Inter for ordinary UI and Anton only where the interruption reference calls for it. Preserve status/navigation safe areas, semantic labels, readable contrast and the established 1 CSS px to 1 dp/sp translation.
- Prefer a small pure presentation model and focused pure formatter/mismatch helpers that can be JVM-tested. Keep resource selection injectable; deterministic tests must not depend on randomness.

## Allowed files

- New interruption UI/model/helper files under `app/src/main/java/com/arjunrana/tokishrine/ui/`.
- New copied Phase 5 resources under `app/src/main/res/drawable-nodpi/` and `app/src/main/res/font/`, plus narrowly required `strings.xml` additions.
- Focused JVM tests under the matching `app/src/test/.../ui/` paths.
- This task's `HANDBACK.md` and `coordination/CURRENT.md` at handoff.

Do not modify `BlockActivity.kt`, `TokiAccessibilityService.kt`, detection code, Room/entities/DAOs/repositories, navigation/permissions, manifest, Gradle/toolchain or prior task records. If a necessary visual implementation cannot fit this boundary, record the dependency for P5B instead of crossing it.

## Checks and evidence

- Add focused JVM coverage for mismatch spans/positions and any pure duration/progress formatting. Compilation is not visual or device proof.
- Run `./build.sh assembleDebug`, `./build.sh testDebugUnitTest`, `./build.sh assembleDebugAndroidTest` (compile-only), and `git diff --check`.
- In `HANDBACK.md`, record exact base/submission, files, UI model/callback contract, resource handling, commands/results, and unexecuted owner visual/input checks. Update CURRENT to `ready_for_review`.
- No adb, emulator/device, installation, screenshots, database extraction or instrumented execution. Arjun owns visual/device acceptance after independent code review.

## Exclusions and stop

- No Activity integration; block lookup; passage/background/humour randomization; timers; keep-screen-awake flags; lifecycle/lock detection; event writes; walk-away count query; challenge completion decision; pause/re-arm; block disable; floating bubble; notification; Stats/settings/feedback; rename; new dependency or toolchain change.
- Stop after the bounded submission, authorized non-device checks, `HANDBACK.md`, and CURRENT=`ready_for_review`. Do not begin P5B or Phase 6.
