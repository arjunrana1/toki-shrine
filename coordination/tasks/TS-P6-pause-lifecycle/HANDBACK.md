# Handback — TS-P6-pause-lifecycle P6C-R1 repair + P6C-O7

- **Submission:** `f7bb1f2` (repair of review finding P6C-R1 plus direct owner correction P6C-O7, 23 September 2026).
- **Base:** `7fb322e` (last owner-corrections submission) via records-only `69cd2be` (the FAIL review). App delta: exactly the four files listed below.
- **Implementer:** GLM. **State:** `ready_for_review` — route the repair delta `7fb322e..f7bb1f2` for fresh independent review.
- Prior corrections handback preserved at [submissions/7fb322e/HANDBACK.md](submissions/7fb322e/HANDBACK.md); senior-repair handback at [submissions/062c71e/HANDBACK.md](submissions/062c71e/HANDBACK.md). Owner results and finding rationale: [OWNER-CHECKS](OWNER-CHECKS.md).

## P6C-R1 — dismissal decision snapshotted before the exit animation

Files: `BubbleDismissalPolicy.kt`, `PauseBubbleView.kt`, `PauseService.kt`, `BubbleDismissalPolicyTest.kt`.

- New pure `BubbleDismissalSnapshot(blockId, activePauseKeys)` — the displayed block and live pause-instance set captured at the release decision, defensively copied so later pause state cannot enter it. `BubbleDismissalPolicy.recordDismissal` now consumes the snapshot instead of a caller-read collection.
- `PauseBubbleView.Host` gains `onBubbleDismissalDecided(displayedBlockId): BubbleDismissalSnapshot`, called **synchronously at ACTION_UP over the discard zone, before `animateOutThen` starts the 140 ms animation**. The animation's end action applies that immutable snapshot via the unchanged-signature-in-spirit `onBubbleDismissed(snapshot)`; the animation stays cosmetic.
- `PauseService.onBubbleDismissalDecided` builds the snapshot on the main thread (`displayedBlockId` + `activePauseKeys()` read together at release time; `latestPauses` is only written on the main thread, so no torn read). `onBubbleDismissed` records the snapshot in the policy and logs `bubble_dismissed` with `snapshot.blockId` — the pill the user discarded, not a post-release displayed block.
- Effect on the failing scenario: a pause starting between release and animation completion is absent from the snapshot, so `shouldShow` returns true on the next render and the bubble re-shows with a fresh `bubble_shown`.
- Untouched: pause timing/enforcement (`PauseRegistry`/`PauseCoordinator`), notification behavior, P6C-O4/P6-O13 (owner do-not-fix), drag gesture, discard zone, hidden-until-new-instance memory semantics.

## P6C-O7 — Dismiss label visibility (direct owner correction, same day)

Owner report: the discard-zone **Dismiss** label is often not very visible. `PauseBubbleView` hint window: width `MATCH_PARENT` → `WRAP_CONTENT` and the label gains a faded, semi-opaque dark chip — `NocturneBg` at alpha 200 (≈78%), 18 dp radius, 14/6 dp padding — with text brightened `NocturneNeutral300` → `NocturneNeutral100` (accent tint over the zone retained). Existing tokens only. Fade in/out, non-touchability and discard semantics unchanged.

## Focused coverage

`BubbleDismissalPolicyTest` now 7 JVM cases — the 5 existing (converted to the snapshot API, semantics unchanged) plus:

- `a pause arriving between dismissal decision and animation completion shows the bubble` — snapshot captured with `[A]`, applied when `[A, B]` is live ⇒ `shouldShow` true.
- `the snapshot ignores pause state that changes after the decision` — defensive copy: post-decision mutations of the source collection cannot enter the snapshot; `blockId` attribution stays with the captured block.

Total JVM suite: **177 tests**. The Android callback ordering (decision before animation) remains source-reviewed/compile-tested, not device-executed — same evidence boundary the reviewer recorded for the gesture/animation wiring.

## Checks run on `f7bb1f2`

- `./build.sh assembleDebug` — **PASS**.
- `./build.sh testDebugUnitTest` — **PASS, 177/177** (fresh XML: 177 tests, 0 failures, 0 errors, 0 skipped); `--tests …BubbleDismissalPolicyTest` rerun — **PASS** (7/7).
- `git diff --check` — clean.
- `assembleDebugAndroidTest` not rerun: no test infrastructure or instrumented sources touched (proportionate checks; same precedent as the corrections round).

## Not done (boundaries)

No device testing by the implementer. After the review **PASS WITH NOTES**, at Arjun's 23 September 2026 request the reviewed repair build was installed on `R5CW30ZBM2R`: fresh debug APK from records-only HEAD `138360f` (app source exactly `f7bb1f2`), SHA-256 `f39dca91…7e08`, `install -r` **Success**, data preserved, app not launched — attribution in [CURRENT](../../../coordination/CURRENT.md) device state. On-device behavior of this repair is owner retest material. No Phase 7 work.

## Stop

Independent reviewer verifies the repair delta `7fb322e..f7bb1f2` (P6C-R1 + P6C-O7). After PASS, owner retests: dismissal still hides only the visible instances (incl. starting a new block's pause right after a dismissal), `bubble_dismissed` attribution, and the Dismiss label readability. No Phase 7 before acceptance.
