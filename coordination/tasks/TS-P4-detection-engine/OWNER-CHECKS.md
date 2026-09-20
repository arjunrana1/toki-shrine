# Phase 4 owner checklist and evidence

Code review PASS applies only to repair submission `9b3da43` (on implementation base `0ca1424`). It does not establish service binding, browser behavior, presentation timing, battery survival, or executed instrumented tests. Record the installed commit/build, device, date, accessibility state and each result as `pass`, `fail`, `blocked`, or `not run`; do not infer one check from another.

## Setup and preservation

- Reinstall only with owner-approved, keep-data setup. Accessibility can be disabled after installation; re-grant it and seed an enabled block containing Instagram plus a separate enabled block containing `reddit.com` before testing.
- The prior Android harness removed its app/test packages after Phase 3. Do not run `connectedDebugAndroidTest` unless Arjun explicitly authorizes it and chooses a fixture/data-preservation plan.
- Visible results do not prove Room/event persistence. The asset-loader suite is an instrumented evidence gate, not a substitute for the browser checks below.

| ID | Test title | Setup/actions | Expected pass | Fail conditions |
|---|---|---|---|---|
| P4-01 | Service bound | Grant accessibility, then verify Toki Shrine is present under Android Accessibility’s bound services (the phase acceptance command may be used if desired). | The service is bound and remains available for the following checks. | Service absent, disabled unexpectedly, or cannot be granted. |
| P4-02 | Blocked app trigger | With the Instagram block ON, launch Instagram. | The plain placeholder names the app/block and appears within 1 second. | No placeholder, wrong trigger, late presentation, crash, or repeat loop before the 10-second debounce expires. |
| P4-03 | Chrome blocked-site trigger | With `reddit.com` ON, navigate to `reddit.com` in Chrome and wait without editing. | Placeholder names the site/block within 3 seconds. | No placeholder, wrong target, or delayed presentation. |
| P4-04 | Typing and search exclusion | In Chrome, focus the address bar and type without navigating; separately enter a search query containing spaces. Wait longer than the settle delay after each. | Neither action opens the placeholder. | Any placeholder while editing or from the spaced query. |
| P4-05 | Domain boundary matching | Navigate to `old.reddit.com`, then `notreddit.com` and `reddit.com.evil.com`, waiting after each. | The subdomain triggers; both lookalikes remain silent. | Subdomain misses or either lookalike triggers. |
| P4-06 | Unsupported-browser limit | Open the same blocked site in a browser absent from the bundled nine-browser map. | No placeholder opens. | Unsupported browser triggers or interferes with supported-browser behavior. |
| P4-07 | Foreground/settle cancellation | Start navigation to blocked Reddit in Chrome, then immediately switch to Toki Shrine, launcher, or another unfiltered app before the settle delay. Separately, turn the Reddit block OFF during a pending settle. | No placeholder opens after either cancellation path. | A stale placeholder opens over another app or after the block is OFF. |
| P4-08 | Battery JSON rendering | Open the battery instructions route on Samsung hardware; inspect the neutral intro and three authored bold steps, then confirm the settings action remains usable. | Text renders correctly from the bundled configuration and the action works. | Missing/blank/corrupt text, wrong steps, or unusable action. |
| P4-NV-01 | Executed asset-loader suite | Only after explicit authorization and a fixture-preservation plan, execute the instrumented suite containing `AssetDetectionConfigLoaderTest`. | Real asset parsing and malformed/missing/duplicate parser paths pass on Android. | Any test failure, inability to run safely, or unrecorded execution. |
| P4-09 | Overnight survival | With accessibility granted and a browser/block fixture ready, leave the phone under ordinary Samsung battery management overnight, then repeat P4-02 or P4-03. | Service remains bound and detection still works. | Service disabled/killed or detection no longer fires. |

## Results

Source: `Toki Shrine _ Owner device checklist _ Phase 4.docx`, supplied by Arjun on 20 September 2026. The document records owner observations, but does not identify the installed code commit, device, test date, or permission/fixture state. Its statements are preserved as evidence, not used to waive product requirements by themselves.

| ID | Owner result | Evidence and disposition |
|---|---|---|
| P4-01 | **Pass reported** | Service bound was marked Pass. Installed-build/device attribution remains unconfirmed. |
| P4-02 | **Pass reported** | Instagram interception was marked Pass. The document does not independently establish the 1-second measurement method. |
| P4-03 | **Pass reported** | Chrome `reddit.com` interception was marked Pass. The document does not independently establish the 3-second measurement method. |
| P4-04 | **Pass reported** | Focused typing and spaced-search exclusion were marked Pass. |
| P4-05 | **Pass reported** | `old.reddit.com` matching and both stated lookalike exclusions were marked Pass. |
| P4-06 | **Waived by owner** | Arjun explicitly waived this non-essential unsupported-browser scenario in the active conversation on 20 September 2026. It is not runtime evidence and need not be retried for Phase 4 closure. |
| P4-07 | **Pass reported** | Both foreground departure and turning the block OFF during settle were marked Pass. |
| P4-08 | **Pass reported** | Samsung battery JSON text and usable action were marked Pass. |
| P4-NV-01 | **Pass** | With Arjun’s explicit authorization, Codex ran only `AssetDetectionConfigLoaderTest` through `./build.sh connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.arjunrana.tokishrine.detection.AssetDetectionConfigLoaderTest` on SM-S918B (`R5CW30ZBM2R`, Android 16): **5 tests, 0 failures, 0 errors, 0 skipped**. This executes real asset parsing plus malformed/missing/duplicate parser paths. |
| P4-09 | **Deferred risk accepted by owner** | Arjun explicitly deferred overnight survival in the active conversation on 20 September 2026. Samsung battery survival remains unverified and must not be represented as a pass, but it does not block Phase 4 closure. |
