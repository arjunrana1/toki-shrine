# TS-P5B-challenge-runtime — Challenge lifecycle, outcomes and events

- **State:** `planned` — do not implement until TS-P5A has an independently reviewed code PASS and CURRENT routes work here.
- **Goal:** integrate the Phase 5 interruption surfaces into `BlockActivity` and implement the correctness-critical typing/waiting challenge runtime, explicit walk-away accounting, disable completion and a clean Phase 6 pause-outcome seam.
- **Implementation owner:** senior model (Codex session using Arjun's chosen capable model). Cancellation, foreground/unlocked visibility, monotonic time, terminal races, duplicate callbacks and atomic persistence/event ordering are explicitly senior-owned. Do not delegate the core to GLM after P5A.
- **Base:** the independently approved P5A submission. Record the exact commit before editing and preserve unrelated work.
- **Authority:** [Phase 5](../../../docs/phases/phase-05.md), PRD §§7, 8, 10, 11, 13 and applicable §17 amendments, [persistence/events](../../../docs/components/persistence-events.md), [theme/UI](../../../docs/components/theme-ui.md), and [build/validation](../../../docs/components/build-validation.md).

## Runtime architecture and invariants

- Keep one pure, deterministic challenge state machine behind a thin Android/Compose adapter. Inject a monotonic clock and passage/image/humour selectors. Android callbacks translate lifecycle/screen state into state-machine inputs; they do not independently decide outcomes.
- A challenge session has a stable identity and exactly one terminal outcome. Cancellation/escape/completion invalidate pending ticks and late callbacks. Recreation may restore a live, still-visible session without duplicating started/shown events; leaving or locking cancels and resets progress, so returning starts from zero.
- Delay progress accrues only while the waiting Activity is presented and the device is unlocked. Use monotonic elapsed time, not wall time. Flat/still placement has no effect. Apply keep-screen-awake only while a challenge is live; explicit locking still cancels.
- Distinguish explicit escape from abandonment. Block-screen decline and pause-challenge escape record one `walk_away`; app switch/system leave records `challenge_abandoned`; screen-off records the same with reason `screen_off`. Disable escape records `turnoff_abandoned` and leaves the block ON. No abandonment path may also count as a walk-away.
- A typing challenge creates a fresh passage for each new challenge session. A mismatch keeps that passage and the typed text, records `typing_mismatch`, increments attempts and permits unlimited correction/resubmission. Exact completion alone may terminate.
- Disable uses the persisted method and fixed stored choice (typing 220/350/700 characters; waiting 180/360/720 seconds). Its successful terminal operation must atomically leave the block OFF and record the required turn-off/block transition and challenge completion events without split-brain state. Retry safely after write failure; never report success before commit.
- A pause-challenge completion records its Phase 5 challenge/countdown outcome and emits one explicit `PauseRequested(blockId, pauseMinutes, ...)` integration result. Phase 5 must not implement the Phase 6 pause timer, access grant, automatic re-arm, bubble or notification, and must not claim the blocked target opens until Phase 6.
- Preserve Phase 4 `block_screen_shown` presentation timing and one-shot behavior. Resolve the launched block/target defensively; stale/deleted/disabled/mismatched inputs terminate without presenting a challenge or fabricating events.
- Walk-away insertion and the displayed local-day global count must have a repository operation with unambiguous ordering. Event payload names/params follow PRD §10 exactly; add missing canonical constants, including `turnoff_abandoned`, without reviving retired events.

## Required coverage and checks

- Pure JVM tests: exact/mismatch typing, unlimited retries, monotonic countdown, pause/resume visibility, screen-off/app-switch/escape, cancellation-vs-completion race in both orders, stale ticks/generations, recreation, duplicate callbacks and write-retry state.
- Repository/instrumented coverage: walk-away write plus daily count, disable mutation plus terminal events, rollback on forced event failure, retry, at-most-once success and unchanged/invalid block behavior.
- Integration-focused coverage must preserve `block_screen_shown` and verify invalid launch data does not emit challenge events.
- Run `./build.sh assembleDebug`, `./build.sh testDebugUnitTest`, and `./build.sh assembleDebugAndroidTest` (compile-only). Any execution of Android tests or device behavior remains separately owner-authorized.
- Write HANDBACK and set CURRENT=`ready_for_review`; independent review must PASS before Arjun begins the Phase 5 owner checklist.

## Exclusions and stop

- No Phase 6 pause lifecycle/access grant/re-arm, floating bubble or ongoing notification; no Stats/settings/feedback; no rename; no unrelated migration/toolchain/dependency change; no device operations.
- Stop at the independently reviewable Phase 5 runtime submission. Explicitly list all Phase 6-dependent acceptance that remains unclaimable.
