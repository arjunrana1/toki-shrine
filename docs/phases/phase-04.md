# Phase 4 — Detection engine

Implemented and closed in [TS-P4-detection-engine](../../coordination/tasks/TS-P4-detection-engine/TASK.md): reviewed repair `9b3da43`, owner results and executed parser evidence are recorded there. P4-06 is explicitly waived; P4-09 Samsung overnight survival is an owner-accepted deferred risk, not a pass.

Read only for this phase or an affected acceptance question. Scope/acceptance below was relocated from the original root instructions. PRD and applicable §17 amendments resolve stale copy/values; current WORKFLOW governs who performs checks. Historical shell/device commands describe evidence, not standing authorization; build through `./build.sh` and leave device operations to the authorized owner.

Senior model should own changes to service/window/URL timing and state correctness; GLM may implement bounded loader/UI work against agreed contracts. Read PRD §§10/13/14 and affected persistence/permissions contracts. Prototype platform observations are not validation of this app. Overnight Samsung survival remains unverified and the Phase 6 pause service inherits that environmental risk.

Current sources under `app/src/main/java/com/arjunrana/tokishrine/`: `TokiAccessibilityService.kt`, `detection/{DetectionEngine,DetectionCoordinator,DetectionConfigLoader,ActiveBlocks,BlockShownEvent,DomainMatcher}.kt` and bundled assets under `app/src/main/assets/`. JVM detection tests live under `app/src/test/.../detection/`; `AssetDetectionConfigLoaderTest` is under `app/src/androidTest/.../detection/`. The placeholder described below was the Phase 4 acceptance host; Phase 5 replaced it with the real interruption/challenge UI.

## Scope and acceptance

`AccessibilityService`, app detection, browser address-bar reading, and the bundled JSON loader for the browser map and OEM text. Block screen is a **placeholder** in this phase — plain text naming the trigger.

Implement the four behaviours in `PRD.md` §13: never act while the address bar is focused, ignore values containing spaces, apply a settle delay before blocking a site, and cache the last known URL per window.

**Acceptance**

- `adb shell dumpsys accessibility | grep "label=Toki Shrine"` shows the service under `Bound services`.
- With a block containing Instagram switched ON, launching Instagram shows the placeholder. `adb shell dumpsys window | grep mCurrentFocus` reports the app's block activity **within 1 second**.
- With `reddit.com` blocked, opening it in Chrome shows the placeholder within 3 seconds.
- Typing into Chrome's address bar without navigating does **not** trigger the placeholder.
- Entering a search query containing spaces does not trigger it.
- `old.reddit.com` triggers a block on `reddit.com`; `notreddit.com` does not.
- Opening a blocked site in a browser absent from the map does not trigger it.
- The browser map is loaded from a JSON asset, not hard-coded in Kotlin. Behind an interface with one implementation, so a remote loader can replace it later.
