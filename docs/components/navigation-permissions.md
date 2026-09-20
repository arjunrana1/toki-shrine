# Navigation, permissions and terminal actions

Read for onboarding, activation, state restoration, permission outcomes or lifecycle work. PRD §§5/6/10/12 and §17 haptic addenda govern behavior. Phase 3 is closed with reviewer, owner and executed nonvisual PASS; the final Android suite ran 51/51 on SM-S918B / Android 16.

## Source and test pointers

`app/src/main/java/com/arjunrana/tokishrine/MainActivity.kt`; `ui/navigation/Routes.kt`; `ui/util/TerminalAction.kt`, `Haptics.kt`; `data/permissions/{AppPermission,PermissionEventLogic,OemBattery}.kt`; permission/onboarding/Settings screens; `TokiAccessibilityService.kt`, AndroidManifest and accessibility XML.
JVM tests under `app/src/test/java/com/arjunrana/tokishrine/`: `data/permissions/{PermissionEventLogicTest,OemBatteryTest}.kt`, `ui/navigation/RouteCodecTest.kt`, `ui/util/TerminalActionTest.kt`. Atomic repository pairings: [persistence/events](persistence-events.md).

## Invariants and boundaries

- Four permission rows only, displayed in owner-approved groups/order: Essential (accessibility, battery), then Better experience (overlay, notifications). Each remains pending/granted with n-of-4 real-system-state progress, refreshed on return. `SYSTEM_ALERT_WINDOW` is declared so overlay is grantable. App entry, browsing and block creation are never permission-gated.
- Home/detail ON use shared accessibility gating, then confirmation; final enable rechecks accessibility. Denial writes/enables nothing and emits no success haptic. Overlay absence removes the future bubble only, not the block screen. The newer owner-facing row mentions the pause screen and timer bubble; that copy does not change this technical gating boundary.
- Checklist modes ONBOARDING/GATE/SETTINGS distinguish onboarding events from revisits. Completion event/marker is atomic and at most once. Back from Welcome can reach Blocks. Preserve exact PRD events/permission values.
- Save minimum route/mode/block identifiers and pending request identities; never save Context/repositories. First-launch resolution is marked only after suspended completion lookup and route decision. Restored non-root stacks resolve without re-pushing Welcome.
- Request ordering: synchronously mark pending → durably record requested → launch system UI; failed record/launch removes the pending mark. A recorded request may remain when later launch fails; do not fabricate an outcome.
- Callback and resume settlement are serialized. Write each outcome before removing that pending identity; failure/cancellation retains failed/unprocessed work for retry. Preserve tested guarantees; saveable state and a mutex are not proof of unlimited crash-proof exactly-once semantics across every OS kill boundary.
- Terminal actions are single-flight synchronously, with accepted commits outside destination-composable scope. Pending input/Back cannot cause a second close. Haptic/route effects follow successful commit; unchanged state skips success haptic, failure allows retry. This does not guarantee work survives activity destruction indefinitely; preserve lifecycle and persistence boundaries honestly.
- Accessibility explainer precedes system Settings. Minimal service declaration currently observes no detection events; actual detection and browser/OEM JSON belong to Phase 4.
- The Android/One UI Accessibility **Installed apps** row is system-owned. `ACTION_ACCESSIBILITY_SETTINGS` provides no app-controlled highlight/badge input; if more direction is needed, add it to Toki Shrine's preceding explainer rather than pretending to modify Settings UI.
- Battery guidance uses Android-neutral framing and automatic Samsung/generic step selection. There is no owner-facing manufacturer picker or completion CTA; return refresh remains unchanged.
- Settings reuses permission health; static Dark, real version, no Countdown messages row. Feedback is Phase 7; supported-browser display follows PRD §13.

## Owner validation and future work

See the closed [P3-01–P3-15 and nonvisual evidence](../../coordination/tasks/TS-P3-validation/OWNER-CHECKS.md). Manufacturer-neutral battery presentation, notification retry, actual permission lifecycle behavior, recreation and haptic feel are owner-accepted; Room retry/durability evidence executed on Android. OEM text migrates to a bundled loader in Phase 4. Do not accumulate detection/challenge/pause logic in MainActivity; Phase 4 uses dedicated state/service ownership.
