# Phase 4 — Detection engine

Phase 3 is cleared. Implementation is authorized through [TS-P4-detection-engine](../../coordination/tasks/TS-P4-detection-engine/TASK.md).

Read only for this phase or an affected acceptance question. Scope/acceptance below was relocated from the original root instructions. PRD and applicable §17 amendments resolve stale copy/values; current WORKFLOW governs who performs checks. Historical shell/device commands describe evidence, not standing authorization; build through `./build.sh` and leave device operations to the authorized owner.

Senior model should own service/window/URL timing and state correctness; GLM may implement bounded loader/UI work against agreed contracts. The Phase 3 accessibility service is only a grantable prerequisite, not the implemented detection engine. Read PRD §§10/13/14 and affected persistence/permissions contracts. Prototype platform observations are not validation of this app. Overnight Samsung survival remains unverified.

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
