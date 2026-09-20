# Phase 5 — The interruption

Phase 4 is closed. Delivery is split into the active bounded GLM visual task
[TS-P5A-interruption-ui](../../coordination/tasks/TS-P5A-interruption-ui/TASK.md),
followed after independent review by the senior-owned
[TS-P5B-challenge-runtime](../../coordination/tasks/TS-P5B-challenge-runtime/TASK.md).
Phase 6 pause lifecycle remains separate.

Read only for this phase or an affected acceptance question. Scope/acceptance below was relocated from the original root instructions. PRD and applicable §17 amendments resolve stale copy/values; current WORKFLOW governs who performs checks. Historical shell/device commands describe evidence, not standing authorization; build through `./build.sh` and leave device operations to the authorized owner.

PRD §17 supersedes the original show-typos control: typos must always be shown without a preference. Senior owns challenge/event/state correctness; GLM may handle bounded visual work.

## Scope and acceptance

Screens 15–19 and 22. Real block screen with humour assets, walk-away moment, typing challenge and mistyped state, visible-and-unlocked delay countdown, turn-off typing or waiting inherited from the block method.

**Acceptance**

- Block screen: the walk-away is the filled prominent button; the way in is the outlined secondary. Verify against `design/screens/15-block-screen.png`.
- The humour line is drawn from the ten strings in `PRD.md` §11 and changes between showings.
- The background image is one of `sys_block_1–6.jpg`, dimmed such that foreground text remains legible.
- Long-pressing the typing input offers **no Paste option**.
- The typing field reports no autocorrect or predictive suggestions while typing random words.
- Typing a wrong character marks the position and keeps the typed text.
- Completing the passage exactly advances; a mismatch does not, and can be retried without limit.
- Tapping *I'll walk away* or *Never mind* records a `walk_away` event; switching apps mid-challenge records `challenge_abandoned` and **no** `walk_away`.
- Switching away mid-countdown and returning restarts the countdown at zero.
- Placing the phone flat and still does not stall the countdown. It advances only while the Toki Shrine waiting screen is visible and the device unlocked; no motion sensor requirement. Leaving, locking or escape cancels and resets progress for both pause and disable waits.
- Disable uses the stored fixed ladder choice: typing 220/350/700 characters or waiting 3/6/12 minutes. Completion turns OFF until manually re-enabled; it does not begin a temporary pause. Escape leaves the block ON.
- Cover lock/unlock, leave/return, screen visibility, cancellation/completion races and duplicate callbacks with senior-owned lifecycle logic; no completion after cancellation. Turn-off events remain separate from walk-away Stats.
- The screen does not sleep during a challenge — leave it untouched for twice the device display timeout and the challenge is still live.
- Walk-away screen auto-dismisses after 2 seconds and shows the correct daily count.
