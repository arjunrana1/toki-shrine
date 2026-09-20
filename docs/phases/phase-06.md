# Phase 6 — Pause lifecycle

Planned only. Requires prior phase clearance and a scoped task before implementation.

Read only for this phase or an affected acceptance question. Scope/acceptance below was relocated from the original root instructions. PRD and applicable §17 amendments resolve stale copy/values; current WORKFLOW governs who performs checks. Historical shell/device commands describe evidence, not standing authorization; build through `./build.sh` and leave device operations to the authorized owner.

Senior owns monotonic timing, concurrent pauses and re-arm correctness. Keep data/event contracts intact; do not build this phase early.

## Scope and acceptance

Pause timer, monotonic timing, automatic re-arm, floating bubble, ongoing notification.

**Acceptance**

- Completing a challenge opens every app and site in that block, and no others.
- The bubble appears when overlay permission is granted, is draggable, and shows remaining time.
- With overlay permission **denied**, the pause still works and everything except the bubble functions.
- The notification shows a live countdown and cannot be swiped away for the pause duration.
- Setting the device clock forward during a pause does **not** shorten it (`adb shell date` to verify).
- When the timer expires the block re-arms immediately with no warning, and reopening the app shows the block screen again.
- Two blocks paused simultaneously both work; the bubble shows the soonest expiry.
