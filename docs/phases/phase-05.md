# Phase 5 — The interruption

Implemented and closed: [P5A UI](../../coordination/tasks/TS-P5A-interruption-ui/TASK.md), [P5B runtime](../../coordination/tasks/TS-P5B-challenge-runtime/TASK.md) and [owner-correction closure](../../coordination/tasks/TS-P5-owner-corrections/TASK.md). Repair `68fdcb3` passed review and Arjun completed the owner acceptance on 23 September 2026. Phase 6 pause lifecycle is also closed; Phase 7 restored production values in every build variant.

Read only for this phase or an affected acceptance question. Scope/acceptance below was relocated from the original root instructions. PRD and applicable §17 amendments resolve stale copy/values; current WORKFLOW governs who performs checks. Historical shell/device commands describe evidence, not standing authorization; build through `./build.sh` and leave device operations to the authorized owner.

PRD §17 supersedes the original show-typos control: typos must always be shown without a preference. Senior owns challenge/event/state correctness; GLM may handle bounded visual work.

Sources under `app/src/main/java/com/arjunrana/tokishrine/`: `BlockActivity.kt`, `challenge/`, `data/repo/ChallengeRepository.kt` and interruption/challenge screens under `ui/screens/`. JVM coverage lives under `app/src/test/.../challenge/` and `detection/`. Phase 5 review notes RV2-N2/RV2-N3 retain their historical coverage limits; Phase 7 removed the unused `Abandoned` instrumentation referenced by RV2-N1. No new coverage is implied by this documentation refresh.

## Scope and acceptance

Screens 15–19 and 22. Real block screen with humour assets, walk-away moment, typing challenge and mistyped state, visible-and-unlocked delay countdown, turn-off typing or waiting inherited from the block method.

**Acceptance**

- Block screen: the walk-away is the filled prominent button; the way in is the outlined secondary. Verify against `design/screens/15-block-screen.png`.
- The target-aware headline rotates across the seven approved templates; the humour line independently rotates across the eleven strings in `PRD.md` §11. Installed apps display their user-visible label.
- The background image is one of `sys_block_1–11.jpg`, rendered as a cropped full-screen ground plus a centered aspect-preserving copy and dimmed for legibility.
- Long-pressing the typing input offers **no Paste option**.
- The typing field reports no autocorrect or predictive suggestions while typing random words.
- Typing does not reveal mistakes live. Submit is the only validation path, remains disabled until the required length, and a failed Submit then marks wrong positions while keeping the typed text.
- Completing the passage exactly advances; a mismatch does not, and can be retried without limit.
- Tapping *Never mind* records a pause-challenge `walk_away`; switching apps, locking, or Android Back records no terminal event and preserves the in-process challenge.
- Switching away mid-countdown and returning restarts the countdown at zero. Typing retains the same passage and entered text. Process-death/reboot restoration is not required.
- Placing the phone flat and still does not stall the countdown. It advances only while the Toki Shrine waiting screen is visible and the device unlocked; no motion sensor requirement. Leaving or locking resets progress without ending the challenge; explicit escape ends it.
- All variants now use disable typing 220/350/700 characters or waiting 3/6/12 minutes. The Phase 5 temporary debug ladders are historical; Phase 7 removed them. Completion turns OFF until manually re-enabled; it does not begin a temporary pause. Escape leaves the block ON.
- Cover lock/unlock, leave/return, screen visibility, cancellation/completion races and duplicate callbacks with senior-owned lifecycle logic; no completion after cancellation. Turn-off events remain separate from walk-away Stats.
- The screen does not sleep during a challenge — leave it untouched for twice the device display timeout and the challenge is still live.
- Walk-away screen shows the correct daily count plus 🎉 and goes to Android home on tap or after 10 seconds.
