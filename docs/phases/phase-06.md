# Phase 6 — Pause lifecycle

Implemented and closed in [TS-P6-pause-lifecycle](../../coordination/tasks/TS-P6-pause-lifecycle/TASK.md). Final reviewed repair `f7bb1f2` is PASS WITH NOTES and owner acceptance is complete. The task's [OWNER-CHECKS](../../coordination/tasks/TS-P6-pause-lifecycle/OWNER-CHECKS.md) retains conditional/not-applicable results and P6C-N1's accepted theoretical edge; do not relabel them as executed passes.

Read only for this phase or an affected acceptance question. Scope/acceptance below was relocated from the original root instructions. PRD and applicable §17 amendments resolve stale copy/values; current WORKFLOW governs who performs checks. Historical shell/device commands describe evidence, not standing authorization; build through `./build.sh` and leave device operations to the authorized owner.

Senior ownership remains the default for changes to monotonic timing, concurrent pauses and re-arm correctness; the closed task records Arjun's GLM implementation assignment and the Codex repairs. Keep data/event contracts intact. Process-local pauses are deliberate: process death/reboot drops them and re-arms enforcement. P6C-O4 unfinished-challenge precedence and P6-O13 notification dismissal/re-post are owner-accepted do-not-fix cases. Samsung overnight survival remains the Phase 4 deferred risk.

Sources under `app/src/main/java/com/arjunrana/tokishrine/`: `pause/{PauseRegistry,PauseCoordinator,PauseService,PauseBubbleView,BubbleDismissalPolicy}.kt`, `detection/DetectionCoordinator.kt` and the `BlockActivity` completion seam. JVM tests live under `app/src/test/.../pause/` and the affected detection/challenge suites.

## Scope and acceptance

Pause timer, monotonic timing, automatic re-arm, floating bubble, ongoing notification.

**Acceptance**

- Completing a challenge opens every app and site in that block, and no others.
- The bubble appears when overlay permission is granted, is draggable, and shows remaining time.
- Owner-approved bubble dismissal: drag to the bottom edge and release; the bubble hides for the pause instances visible at release and a later/new pause shows it again. Timing, enforcement and notifications are unaffected.
- With overlay permission **denied**, the pause still works and everything except the bubble functions.
- The notification shows a live countdown and is posted as ongoing. The original non-swipeable expectation is superseded by PRD §17/P6-O13: Android may allow dismissal, and periodic renders re-post it; the owner explicitly accepted that behavior.
- Setting the device clock forward during a pause does **not** shorten it (`adb shell date` to verify).
- When the timer expires the block re-arms immediately with no warning, and reopening the app shows the block screen again.
- Two blocks paused simultaneously both work; the bubble shows the soonest expiry.
