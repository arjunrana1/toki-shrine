# TS-P4-detection-engine — Accessibility detection and browser URL matching

- **State:** `changes_requested` — Codex review of `0ca1424` found P4-F01–P4-F04; see [REVIEW](REVIEW.md). Repair is required before owner/device acceptance.
- **Goal:** detect blocked foreground apps and supported-browser whole-domain visits, then open the Phase 4 plain trigger placeholder within the required timing without firing from address-bar editing or stale window state.
- **Implementation owner:** GLM 5.3 Pro / senior model. Service/window/URL timing, cancellation, stale events and per-window caching are stateful correctness work; Flash is not the primary owner. Arjun dispatches manually; one writer.
- **Base:** the committed Phase 3/wizard closure plus repository-hygiene documentation/assets at repository HEAD when handed to the implementer. Verify and record `git rev-parse HEAD` before editing. The worktree is expected to be clean; if it is not, reconcile unexpected changes with Arjun before writing. Do not rewrite the accepted prior submission, and record the exact implementation base in HANDBACK.
- **Authority:** [Phase 4](../../../docs/phases/phase-04.md), PRD §§10, 13 and 14, [navigation/permissions](../../../docs/components/navigation-permissions.md), [persistence/events](../../../docs/components/persistence-events.md), and [build/validation](../../../docs/components/build-validation.md). Prototype observations are product evidence, not this app's runtime proof.

## Scope and invariants

- Implement `AccessibilityService` foreground-app detection and supported-browser address-bar reading. Keep detection/service ownership out of `MainActivity`.
- Load the nine-browser package/view-ID map and OEM text from bundled JSON behind one replaceable loader interface; do not hard-code the map in Kotlin and add no remote/backend dependency.
- Filter relevant packages at runtime to enabled blocked apps plus supported browsers. A blocked app trigger opens the plain Phase 4 placeholder naming the trigger.
- For websites: never act while the address bar is focused; ignore values containing spaces; apply one tunable settle delay (prototype reference 2000 ms); cache the last known URL per window; cancel stale delayed work when window/package/address state changes; debounce repeated triggers.
- Match whole domains including subdomains (`old.reddit.com` matches `reddit.com`) without lookalikes (`notreddit.com` does not). Unsupported browsers and in-app webviews do not trigger.
- Preserve Phase 3 permission/onboarding/activation behavior, Room ownership and event contracts. Add applicable §10 detection events without fabricating outcomes. Overlay remains unnecessary for launching the placeholder.

## Checks

- Add focused deterministic tests for app detection, supported/unsupported browsers, focus exclusion, space-containing searches, exact/subdomain/lookalike matching, per-window URL caching, settle cancellation/stale events, repeat debounce, JSON loading/failure and service recreation.
- Run `./build.sh assembleDebug`, `./build.sh testDebugUnitTest`, and `./build.sh assembleDebugAndroidTest` (compile-only). Device/service/browser execution remains Arjun-authorized separately; do not run adb or install during implementation.
- Write HANDBACK with exact base/submission, state-machine invariants, files, commands/results and remaining device evidence. Update CURRENT to `ready_for_review`; independent Codex review precedes owner/device acceptance.

## Exclusions and stop

- No Phase 5 typing/waiting challenge, pause/re-arm timer, hold detection, full block-screen UX, floating bubble, notification, stats UI, feedback, rename, dependency/toolchain change or remote loader.
- Stop after scoped implementation, non-device checks, HANDBACK and CURRENT. Do not claim service binding, latency, browser behavior, battery survival or device acceptance from compilation/tests.
