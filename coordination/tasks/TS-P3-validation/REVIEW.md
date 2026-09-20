# Phase 3 review checkpoint

- **Verdict:** code PASS for `cb1ad45`; final reviewed range `4b0ca0f..cb1ad45`, original approved phase base `6c3b962`.
- **Reviewer/source:** Codex code-only review on 14 September 2026, imported without re-audit. [Original reviewer record](../../../docs/history/migration-2026-09-18/HANDOVER.before.txt).
- **Resolved findings:** route/request recreation and single-flight terminal transitions addressed by `4b0ca0f`; permission outcome durability/serialization and first-launch resolution race addressed by `cb1ad45`.
- **Checks:** earlier reviewer inspected source/tests and builder XML (44 JVM tests, zero failures/errors). It performed no builds, test execution or device operations. This migration adds no new runtime evidence.
- **Current blockers:** none in that reviewed code submission. Owner acceptance and P3-NV evidence remain pending; this is not full phase sign-off.
- **Next:** reconcile P3-01–P3-15. Inspect changed code only when new submission or concrete owner failure justifies it. New routine GLM corrections are reviewed as one accumulated scoped delta where appropriate; keep their owner evidence tied to tested builds.

Add stable finding IDs for new defects with reproduction, location, consequence, required correction and owner. Preserve prior submission/verdict before replacing this record; never apply this PASS to unreviewed changes.

## Owner validation review — 19 September 2026

- **Submission:** owner-tested build unconfirmed; repository code remains exactly `cb1ad45` with unrelated uncommitted documentation/design work.
- **Validation verdict:** **FAIL / changes requested.** The earlier static code PASS above remains preserved as its historical checkpoint, but it no longer clears Phase 3 because owner testing exposed a concrete manifest defect and requested bounded UI corrections.
- **Review activity:** reconciled the supplied DOCX against P3-01–P3-15, rendered and inspected all four pages, inspected implicated source/design files, and checked the overlay platform contract. No build, test, device or implementation action was performed.

### Findings

- **P3-F01 — Blocker — overlay permission cannot be granted.** Location: `app/src/main/AndroidManifest.xml` (permission declarations), with request/check callers in `PermissionChecklistScreen.kt:159–165` and `AppPermission.kt:47–50`. Scenario: tap Grant for Display over other apps on the reported build. Toki Shrine is absent from the system list because the manifest does not declare `android.permission.SYSTEM_ALERT_WINDOW`. Consequence: P3-08 fails and P3-02 cannot establish a real 4-of-4 state; the future Phase 6 bubble could not be shown. Correction: add the special permission declaration, preserve the user-mediated Settings route and real-state check, add a manifest assertion if proportionate, then owner-retest grant, return refresh, revoke and denial on the identified build. **Owner: GLM (small localized platform repair).**
- **P3-F02 — Required visual correction — permission checklist layout/copy.** Location: `PermissionChecklistScreen.kt:96–179` and `AppPermission.kt:18–43`. Scenario: open the checklist at normal scale. Consequence: heading sits too close to the status bar and current ungrouped order/copy does not match the newly supplied `design/screens/Accessibility screen design v2.png`. Correction: apply the new Essential permissions / For a better experience grouping, Accessibility/Battery/Overlay/Notifications order, supplied row copy and safe top spacing while retaining exactly four real-state rows, only Grant/Granted, accurate progress and a visible Continue CTA. **Owner: GLM (routine UI).**
- **P3-F03 — Required content correction — accessibility explainer.** Location: `AccessibilityExplainerScreen.kt:46–90`. Scenario: open the explainer. Consequence: current title and statements differ materially from the owner-supplied replacement. Correction: implement the exact content and hierarchy in `design/screens/Accessibility Explainer.png`, preserving explainer-before-Settings and return refresh behavior. **Owner: GLM (routine copy/UI).**
- **P3-F04 — Required tuning — ON haptic too short.** Location: `Haptics.kt:14–26`. Scenario: successfully turn a block on. Consequence: owner reports the heavy-click effect is too brief. Correction: make the ON feedback perceptibly about 30% longer without changing OFF feedback, ordering, single-flight behavior or failure/no-op semantics; because Android's predefined effect exposes no duration, acceptance is owner feel-testing on the target device. **Owner: GLM (bounded haptic/UI repair).**
- **P3-F05 — Required UI correction — battery completion CTA.** Location: `BatteryInstructionsScreen.kt:110–135`. Scenario: open Samsung battery guidance. Consequence: the extra `I've done this` CTA is no longer wanted. Correction: remove it while retaining Open battery settings, manufacturer picker, Back behavior and refresh-on-return. Do not add a “minimal battery impact” claim until later detection/service behavior has measured evidence. **Owner: GLM (routine UI).**
- **P3-F06 — Required helper copy — post-save activation guidance.** Location: `CreateFlowScreen.kt:962–968`. Scenario: reach the final step of new-block creation. Consequence: the owner wants the OFF-after-save path made explicit. Correction: add `Turn it on from the “Blocks” homescreen.` below Save block with reference-consistent padding, without changing save enablement or activation gating. **Owner: GLM (routine copy/UI).**

### Evidence gaps and next gate

- P3-01 and the behavioral matrix in P3-14 were not actually reported; obtain exact results after repair rather than inferring them from copy notes.
- P3-NV-02 through P3-NV-04 remain open exactly as recorded in OWNER-CHECKS. Visible P3-07/P3-12 passes do not prove durable or atomic database/event outcomes.
- Repair submission must identify its base/commit and actual checks. Re-review only P3-F01–P3-F06 and affected dependencies, then owner-retest P3-02, P3-03, P3-06, P3-08, P3-09, P3-14 and P3-15. Phase 4 remains uncleared.

## Repair re-review — 19 September 2026

- **Submission:** `7d779a6` on `cb1ad45`; reviewed range `cb1ad45..7d779a6`, exactly the nine files listed in HANDBACK.
- **Verdict:** **PASS WITH NOTES.** No blocking code finding remains in P3-F01–P3-F06. This approves the repair delta only; owner acceptance and the recorded nonvisual evidence gaps remain separate.
- **Reviewer activity:** inspected the complete delta, implicated callers/contracts and supplied visual references; confirmed HEAD/parent; confirmed the built merged and packaged debug manifests contain `android.permission.SYSTEM_ALERT_WINDOW`; inspected GLM's test XML recording 45 JVM tests, zero failures/errors; and ran `git diff --check` on the range. Codex did not rerun builds/tests or perform device work.

### Finding resolution

- **P3-F01 — Resolved in code; owner retest required.** The source manifest declares `SYSTEM_ALERT_WINDOW`; the existing user-mediated Settings route and `Settings.canDrawOverlays()` state check are unchanged. The new JVM guard passed and the declaration is present in the built debug manifests. P3-08 still needs grant/return/revoke/denial confirmation on an identified installed build.
- **P3-F02 — Resolved in code; visual retest required.** The checklist now has the requested two groups and Accessibility/Battery/Overlay/Notifications display order, exactly four real-state rows, only Grant/Granted badges, unchanged event identities, live n-of-4 progress and the retained Continue action. The onboarding heading gains its own top gap. P3-02/P3-15 must confirm fit, hierarchy and bottom-CTA visibility on device.
- **P3-F03 — Resolved in code; visual retest required.** Title, body and both lists match the supplied replacement reference; explainer-before-Settings and return refresh wiring are unchanged. P3-03 remains the device/visual gate.
- **P3-F04 — Resolved for code review; feel retest required.** Successful ON now emits a heavy click plus a delayed settling click; OFF and all success-only callers are unchanged. Static review cannot prove the two segments feel like one longer pulse, so P3-06 remains owner acceptance.
- **P3-F05 — Resolved in code; owner retest required.** The `I've done this` action and parameter were removed without altering Back, Open battery settings, manufacturer selection or return settlement. No battery-impact claim was introduced. Retest P3-09.
- **P3-F06 — Resolved in code; owner retest required.** The requested helper appears below the pinned Save action; save enablement and save-OFF behavior are unchanged. Full P3-14 behavior remains unreported and must be exercised, not inferred from this copy change.

### Notes and remaining gate

- The new owner-facing overlay row says it shows the pause screen and timer bubble, matching the supplied reference. The technical contract remains unchanged: accessibility launches the block/pause screen; overlay absence removes only the future bubble. This review approves the requested copy but does not introduce overlay activation gating or Phase 6 behavior. Reconcile the older PRD stated-purpose wording when Phase 3 records close.
- GLM's checks are implementer evidence, not independent execution: `assembleDebug`; `testDebugUnitTest` with 45/0/0; `assembleDebugAndroidTest` compile-only; legacy Kotlin hex check clean. Instrumented execution remains absent.
- Next: Arjun identifies the installed `7d779a6` build/device, retests P3-02, P3-03, P3-06, P3-08, P3-09, P3-14 and P3-15, and supplies the precise P3-01 result. P3-NV-02–P3-NV-04 remain open. Phase 4 is not cleared.

## Direct correction re-review — 19 September 2026

- **Submission:** `f92661d` on `6f31817`; reviewed as the accumulated direct-correction range `7d779a6..f92661d` (12 files).
- **Verdict:** **PASS.** No blocking or required code finding remains in the two owner-directed correction rounds. This verdict covers only that delta; it does not clear the remaining Phase 3 acceptance or nonvisual evidence.
- **Reviewer activity:** inspected the complete accumulated diff and affected UI/state dependencies, confirmed all nine screen roots with status-bar handling also apply navigation-bar handling, inspected the latest test XML and packaged manifest, and ran `git diff --check` on the range. Codex did not rerun builds/tests or perform device work.
- **Implementer evidence:** `assembleDebug`; `testDebugUnitTest` with 45 tests and zero failures/errors; `assembleDebugAndroidTest` compile-only; legacy Kotlin hex check clean. The packaged debug manifest retains `android.permission.SYSTEM_ALERT_WINDOW`.

### Reviewed outcomes

- Welcome copy matches the four owner-supplied lines exactly. Shared app-bar spacing and the Blocks header top gap implement the requested safe top spacing.
- The battery screen is manufacturer-neutral in presentation: the picker and Samsung-specific CTA are removed, the introduction says Android is aggressive, and automatic Samsung/generic step selection remains intact. This supersedes the earlier P3-09 picker expectation.
- The final create/review helper has the requested lower separation without changing save enablement or the save-OFF contract.
- Every screen root now respects navigation-bar insets, providing a consistent lower safe area for bottom actions. The app list displays `loading...` directly below search until loading completes and does not expose the interim zero-result count.
- The Android/One UI **Installed apps** row is system-owned. Android's accessibility Settings action exposes no input for highlighting or decorating that row, so omitting an attempted system-row badge is correct. A preceding in-app guidance sentence remains the available product-level mitigation if the owner later requests it.

### Owner evidence and remaining gate

- Arjun reports that the requested corrections pass on the installed `f92661d` build on the SM-S918B / Android 16. This closes the targeted P3-03 content/path retest, the revised P3-09 presentation, and the P3-15 spacing corrections; it also accepts the targeted P3-01 welcome presentation and P3-14 loading/helper presentation.
- Still open: P3-01's fresh-data/Back/create-without-permissions behavior; P3-02; P3-06 haptic feel; P3-08 overlay deny/grant/return/revoke flow; the full P3-14 behavior matrix; and P3-NV-02–P3-NV-04. Phase 4 remains uncleared.

## Phase closure — PASS, 19 September 2026

- **Verdict:** **PASS / Phase 3 cleared.** Arjun reports every remaining owner scenario passing, including P3-01, P3-02, P3-06 and P3-08. P3-NV-02–P3-NV-04 now have executed evidence recorded in OWNER-CHECKS.
- **Automated evidence:** `./build.sh testDebugUnitTest` passed 60/60. On SM-S918B / Android 16, final `./build.sh connectedDebugAndroidTest` passed **51/51**, comprising BlockRepositoryTest 23, EventRepositoryTest 9 and CreateFlowScreenTest 19.
- **Critical pairings:** forced SQLite event-insert failures prove enabled-state/event and onboarding-marker/event transactions roll back together; successful repetitions prove at-most-once behavior. A restored pending overlay outcome remains pending after a forced Room write failure and, after recreation/retry, persists exactly one granted event.
- **Test repair boundary:** no production behavior changed for closure. Android-test corrections replaced invalid double `setContent` recreation, aligned expected formatted text and traversed-step count with production, removed a read that necessarily blocked behind the deliberately open transaction, and installed a non-exported debug-only lifecycle host. Five focused Room/permission cases were added.
- **Disposition:** no Phase 3 blocker or acceptance gap remains. Phase 4 is authorized. The Gradle harness removed the app/test packages and accessibility grant after execution; reinstall and re-grant only when later device work needs them.
