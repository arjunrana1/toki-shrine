# Owner checks — TS-P5-owner-corrections

Owner observations are from Arjun's 23 September 2026 testing of the Phase 5B APK installed from code commit `c5a8e9e` (with later records-only HEAD `59596a8`). Unmentioned behavior was reported working well; this record does not turn source/build checks into device evidence. Retest results for the correction submission remain open.

**Installed retest build:** on 23 September 2026 the review-approved correction build was installed on SM-S918B (`R5CW30ZBM2R`) via `adb install -r` (Success): `app-debug.apk` SHA-256 `1077fb1b32d660d7bd05038c10648371dbebeba460b43d97670ef1445040376b`, containing code `7fbb692` exactly (built from that tree, Gradle up-to-date against records-only `5eb27ed`). `com.arjunrana.tokishrine` 0.1.0 / versionCode 1, debug. App not launched by the installer; accessibility re-grant is owner setup.

**Superseding install, same day:** after Arjun's direct headline-placement correction, the rebuilt build from commit `9d411d4` (`Move gate headline cluster to top segment`, on top of the reviewed `7fbb692` code) was installed via `adb install -r` (Success, data preserved): APK SHA-256 `4e05967e5bbe23386d93e88418ec32d22b3208fe6477c60a34f4718e01e68d4b`, `pm path` verified, app not launched, accessibility re-grant still owner setup. **Second superseding install, same day:** after the pause-completion landing correction (`2543329`, on top of `9d411d4`), the rebuilt APK SHA-256 `9542463a8a645a8360d1bab1c214869006503461427973c3e28a2d1405314d66` was installed via `adb install -r` (Success, data preserved), `pm path` verified, app not launched. **Third superseding install, same day:** after review of the P5C-O14 repair, a fresh debug APK built from the tree at records-only HEAD `497fb5a` (app source identical to reviewed `68fdcb3`) — SHA-256 `34717bd89a2fd85bdd65e688d42ba438eeb8d7b42f2cc8ca78561865b47290a5` — was installed via `adb -s R5CW30ZBM2R install -r` (Success, streamed install, data preserved, no uninstall/clear). `pm path` confirms `com.arjunrana.tokishrine` 0.1.0; the `enabled_accessibility_services` setting retains `com.arjunrana.tokishrine/.TokiAccessibilityService` across the reinstall (actual service re-binding remains owner observation). The app was not launched and no device testing was performed. All P5C retest results below refer to this newest build once Arjun reports them; earlier installs remain historical.

| ID | Owner observation on installed build | Required correction / retest |
|---|---|---|
| P5C-O1 | Gate displayed raw package `com.ril.ajio` | Installed apps show user-visible label such as Ajio; sites show domain |
| P5C-O2 | Fixed “Open target?” copy was wrong for the intent | Seven approved target-aware headlines rotate independently of eleven approved subtexts |
| P5C-O3 | Gate headline alignment/type did not match reference | Centered regular Inter treatment; no Anton headline |
| P5C-O4 | Background subject was stretched/cropped out of view | Cropped full-screen layer plus centered aspect-preserving copy of the same selected image; rotate all eleven assets |
| P5C-O5 | Gate CTA sizes differed and reference emojis were absent | Equal CTA geometry; challenge CTA ends `🚩🤨` |
| P5C-O6 | Walk-away screen disappeared too quickly, emoji was absent, and dismissal returned to Toki | Count line ends `🎉`; tap or 10 seconds routes to Android home |
| P5C-O7 | Waiting challenge disappeared on app-switch/lock | Session remains active in process, resets to the full duration, and resumes without a terminal event |
| P5C-O8 | Waiting screen did not match supplied design | Timer ring, heading/reset explanation, no Counting pill, full-width outlined Never mind |
| P5C-O9 | Testing costs could not be lowered enough | Debug only: pause 20-char/20-sec minima; disable first rungs 20-char/20-sec; release values unchanged |
| P5C-O10 | Typing exposed typos live, lacked Submit, used wrong escape copy, and disappeared on switch | Never mind; passage/text retained in process; Submit-only validation, disabled until required length; typos revealed after failed Submit |
| P5C-O11 | Android Back needed intentionality distinction | Back backgrounds/preserves; only explicit Never mind terminates and records a walk-away |
| P5C-O12 | Completing a pause challenge did not grant access | Expected Phase 5 boundary; verify no premature Phase 6 claim, then test access lifecycle in Phase 6 |
| P5C-O13 | Completing a typing or waiting challenge landed on the Toki homescreen instead of the triggering app | App-triggered pause completions launch the triggering app's launcher intent and land there; turn-off completions stay in Toki. Site triggers carry a domain, not a browser package, and keep the previous finish behavior pending an owner decision on carrying the browser package through the detection pipeline |

Typing paste suppression and autocorrect/prediction safeguards were explicitly reported working well and must remain unchanged.

## Follow-up owner finding — installed code `2543329` (records HEAD `4ed621b`)

Arjun confirmed the top-positioned headline/subtext and completed-challenge return to the triggering app work well. A separate enforcement defect was then reproduced on the same installed build: after choosing **Not now** for App A, reopening App A immediately could bypass the interruption, while App B in the same block still triggered. Waiting before reopening, navigating through other apps, or locking/unlocking caused App A to trigger again. Android Back from a challenge could expose the same immediate-return window. Source diagnosis found that the per-target ten-second repeat debounce suppressed the immediate App A event, with no scheduled reevaluation after expiry.

| ID | What to test | Steps to follow | Fail conditions | Pass conditions |
|---|---|---|---|---|
| P5C-O14 | Immediate same-target enforcement after no-access outcomes | Open blocked App A → choose **Not now** → dismiss the walk-away moment → immediately reopen App A. Repeat by pressing Android Back during both the gate and an active challenge, then return immediately to App A. Also confirm App B still triggers independently. | App A is accessible without the interruption; the retained live challenge is lost/replaced; successful challenge completion no longer follows its separately specified route. | **Not now** and gate Back make App A trigger a fresh gate immediately. Back/background during a live challenge brings that retained challenge forward. App B remains independently enforced. Successful completion behavior is unchanged. |

P5C-O14 is fixed in code submission `68fdcb3`, which passed independent review on 23 September 2026 (PASS WITH NOTES, RV2-N1–N3; see [REVIEW](REVIEW.md)). The review-attributed build (SHA-256 `34717bd8…290a5`) is installed on SM-S918B; owner retest is open.
