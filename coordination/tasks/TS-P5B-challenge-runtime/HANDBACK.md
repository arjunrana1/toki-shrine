# Senior Codex repair handback — TS-P5B-challenge-runtime

- **Repair submission:** `7a8fb00` on reviewed implementation `eb2f956`; review record is `74202ed`.
- **Implementer:** senior Codex, 21 September 2026.
- **Scope:** P5B-01 only. The original handback is preserved under `submissions/eb2f956/`.

## Repair

- Confirmed the finding: `completeTurnOff()` reused the pause completion writer, so it emitted `challenge_completed`, the event used by `EventRepository.getStats()` as the completed side of the walk-away-rate denominator.
- Split pause and turn-off completion persistence. Pause still emits `challenge_completed`; turn-off now emits only its atomic `turnoff_completed`, `block_turned_off`, and applicable `countdown_completed` terminal events alongside the OFF mutation.
- Updated the existing at-most-once assertion and added a focused Room regression that starts with one walk-away, proves the rate is `1.0`, completes a turn-off, then proves the rate remains `1.0`, `challenge_completed` remains absent, and `turnoff_completed` is present.
- Preserved the existing transaction, rollback/retry, idempotency, stored-config validation, and Phase 6 boundary.

## Verification

- `./build.sh assembleDebug` — **PASS**.
- `./build.sh testDebugUnitTest` — **PASS, 142/142** from fresh XML results.
- `./build.sh assembleDebugAndroidTest` — **PASS, compile-only**; seven P5B Room tests compile, including the new Stats-boundary regression. They were not executed.
- `git diff --check` — **PASS**.

No device, emulator, adb, installation, screenshot, database extraction, or instrumented-test execution occurred. This is implementer self-verification; fresh independent review remains required.

Next prompt: `Read AGENTS.md and independently re-review P5B-01 in TS-P5B-challenge-runtime. Review repair 7a8fb00 against eb2f956 with REVIEW.md and the focused regression, update CURRENT, and stop before owner testing or Phase 6.`
