# Codex review — TS-P5B-challenge-runtime

- **Reviewed submission:** `eb2f956` (`Implement Phase 5 challenge runtime`)
- **Base:** `9e3a373`
- **Reviewer:** independent Codex review, 21 September 2026
- **Verdict:** **FAIL — changes requested**

## Review boundary

Reviewed the submitted diff against its declared base, the Phase 5 task and
acceptance scope, the Phase 5 contract, persistence-event semantics, the
challenge runtime/repository/activity integration, its direct callers, and the
new focused tests. This is source and data-flow review only; no device,
emulator, adb, installation, or instrumented-test execution was performed.

## Finding

### P5B-01 — Turn-off completion is counted as a walk-away Stats completion

**Priority:** blocker
**Location:** `ChallengeRepository.kt`, `completeTurnOff()` and
`insertCompletionEvents()`; `EventRepository.kt`, `getStats()`

`completeTurnOff()` calls `insertCompletionEvents()`, which unconditionally
persists `challenge_completed`. `EventRepository.getStats()` uses every
`challenge_completed` event as the completed side of the walk-away-rate
denominator. Consequently, successfully turning off an enabled block changes
the walk-away rate even though the Phase 5 contract requires turn-off events to
remain separate from walk-away Stats; the persistence-event contract likewise
excludes `turnoff_completed` from that calculation.

The new repository test currently asserts this generic completion event on the
turn-off path, so it codifies the incorrect accounting rather than protecting
the required Stats boundary.

**Required correction:** split pause and turn-off completion persistence. A
turn-off must atomically retain its `turnoff_completed`, `block_turned_off`, and
applicable waiting `countdown_completed` events with the OFF write, but it must
not create the generic event consumed by walk-away Stats. Add focused regression
coverage proving a completed turn-off does not alter walk-away-rate completion
accounting, while preserving atomic retry/rollback behavior.

## Evidence and limits

- The declared base is the direct parent of `eb2f956`; the submission diff is
  structurally clean (`git diff --check 9e3a373..eb2f956`: PASS).
- The pure runtime, lifecycle gating, defensive launch validation, retry-aware
  persistence paths, and pause boundary were inspected. No Phase 6 behavior was
  found in the reviewed submission.
- The implementation handback attributes `./build.sh assembleDebug`, JVM tests,
  and Android test assembly to the builder. Those results are retained as
  builder evidence, not as independent runtime or device evidence.

## Next

Senior Codex should repair **P5B-01 only**, add the focused regression coverage,
write a replacement handback for the repair submission, and route the task for
a fresh independent code review. Owner testing and Phase 6 remain out of scope.

## Re-review — P5B-01 repair

- **Repair submission:** `7a8fb00` (`Fix turn-off stats accounting`)
- **Repair comparison:** `eb2f956`; original task base remains `9e3a373`
- **Verdict:** **PASS.** No blocking or required code finding remains in the
  P5B boundary.

`7a8fb00` separates the writers so `completePause()` alone persists the generic
`challenge_completed` event consumed by `EventRepository.getStats()`. The
turn-off transaction now keeps the OFF mutation, `turnoff_completed`,
`block_turned_off`, and applicable `countdown_completed` in its existing Room
transaction without inserting that Stats-counted event. The focused Room
regression establishes a walk-away rate of `1.0`, completes a turn-off, and
proves both that the rate remains `1.0` and that no generic completion event was
written. Existing at-most-once, retry/rollback, and waiting-countdown coverage
remains applicable to the unchanged transaction boundary.

The repair delta is structurally clean (`git diff --check eb2f956..7a8fb00`:
PASS). This re-review is source and data-flow evidence only. The repair
handback's assembly, JVM, and compile-only Android-test results remain
implementer-attributed; no device, emulator, adb, installation, or instrumented
test execution was performed by the reviewer.

**P5B-01 resolution:** resolved and independently verified in `7a8fb00`.

P5B code review is complete. Phase 5 owner acceptance remains separate; no
Phase 6 behavior is authorized.
