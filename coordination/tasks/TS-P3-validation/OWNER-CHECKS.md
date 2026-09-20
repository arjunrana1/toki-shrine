# Phase 3 owner checklist and evidence

Original P3-01–P3-15 IDs and checklist text preserved from Codex's Phase 3 review task, 14 September 2026. Cleared for owner testing by the later `cb1ad45` code PASS. Arjun supplied a completed checklist document on 19 September 2026; the reconciled results below distinguish reported device observations from still-missing evidence.

## Setup and reporting

Record installed build/commit if known, device, date and permission state with results. Repository HEAD does not prove what is installed. Report `P3-xx: pass/fail/blocked`, observations and screenshots if useful. Keep existing IDs even when wording is clarified.

P3-01's fresh-data scenario is conditional: do not clear/reinstall or erase your current fixtures merely because this document exists. If a fresh-state check cannot be performed safely during the current pass, mark it pending and coordinate setup separately. Sideloaded accessibility may require Android restricted-settings unlock (PRD §12), a tester setup instruction rather than an in-app step.

Visible behavior establishes only visible outcomes. Original mentions of duplicate events/durable outcomes in P3-07/P3-12 are covered separately by P3-NV evidence; do not infer database correctness from haptics or screens. The previous checklist remains intact below so already-started testing maps exactly.

## Original checklist

| ID | Test title | Setup/actions | Expected pass | Fail conditions |
|---|---|---|---|---|
| P3-01 | Welcome and no entry gate | Use fresh app data with all permissions absent. Launch; inspect Welcome; press Back; create a block. | One *Get started* CTA, no *How it works*. Back reaches Blocks. Creation works without permissions. | Permission wall before ON, extra CTA, or creation unavailable. |
| P3-02 | Four rows and progress | Open onboarding/checklist with a mixture of granted permissions. | Exactly four named rows; every row says only Grant or Granted; `n of 4` and bar match system state. | Extra/missing row, third state, or wrong count/bar. |
| P3-03 | Accessibility explanation | Tap Accessibility Grant, inspect the next screen, continue, grant in Android, return. | Explainer appears before Android Settings; returning shows Granted and increments progress automatically. | System Settings opens first, manual refresh needed, or wrong row changes. |
| P3-04 | Revocation refresh | Revoke accessibility while Toki Shrine is backgrounded, then return. | Accessibility becomes pending and progress decrements without relaunching. | Stale Granted state or incorrect count. |
| P3-05 | All activation gates | Without accessibility, try ON from home and detail. Then grant it, open final confirmation, revoke it externally, return and tap *Turn it on*. | Every denied path opens the checklist; block remains OFF; no success haptic. | Confirmation bypasses the gate, block becomes ON, or success haptic fires. |
| P3-06 | Successful haptics | Grant accessibility. Turn a block ON, then OFF from home; repeat and turn OFF from detail. | ON changes once with one stronger haptic; each OFF changes once with one lighter haptic. | Haptic precedes state change, fires on failure, repeats, or state does not persist. |
| P3-07 | Rapid and competing actions | Rapidly tap *Turn it on* twice; repeat while pressing *Not yet* or system Back. Rapidly tap an OFF switch. | One terminal result, one haptic, no crash, no extra navigation/pop. | Duplicate feedback/events, wrong screen, partial state, or crash. |
| P3-08 | Overlay flow | Deny, grant, and later revoke Display over other apps, returning after each action. | Row and progress reflect real state after each return; denial keeps it pending. | Stale state, wrong row, or manual refresh required. |
| P3-09 | Samsung battery flow | On the S23 Ultra, open Battery exemption; inspect steps, open Settings, choose Unrestricted, return. Open the manufacturer picker and select the generic option. | Samsung copy/three steps are correct; picker opens and changes guidance; battery state refreshes correctly. | Wrong OEM guidance, picker unusable, wrong Settings destination, or stale state. |
| P3-10 | Notifications | Deny, allow, revoke, and retry after repeated denial. | State refreshes correctly. Grant always presents a usable route to granting, including any permanent-denial state. | False Granted state or Grant becomes a dead action. |
| P3-11 | Settings | Open the gear and Permission health. Return after changing a permission. | Health count matches; checklist is reused; Theme is Dark; real version shown; no Countdown messages row; no crash from inert future rows. | Wrong count, stale state, removed row returns, or incorrect version. |
| P3-12 | Recreation/resumption | Rotate on onboarding, gate checklist, accessibility explainer and battery screen; also rotate/background while Android Settings is open, then return. | Same logical flow and request resume; refreshed result appears once; no reset to Welcome/Blocks. | Lost route, lost request result, duplicate outcome, or unexpected onboarding restart. |
| P3-13 | Onboarding completion | Continue with a partial permission set, close/relaunch, then inspect Settings. | Welcome stays completed; Blocks opens; stored permission count remains accurate. | Welcome unexpectedly returns or completion appears only partially saved. |
| P3-14 | Phase 2 preservation | With permissions absent: create/edit a nonempty block; test a 31-character/multiline name, empty targets, a domain owned elsewhere, and `reddit.com↵abdes`. | Name is single-line/capped at 30; empty progression/save blocked; owned target remains unavailable without transfer; multiline domain is rejected without merging; new block saves OFF. | Any ownership transfer, empty save, overlong name, merged domain, or permission gate before ON. |
| P3-15 | Visual comparison | Compare screens 1–4 and 24 with their supplied renders at normal font/display scale. | No clipping/overlap; hierarchy, button variants and dark styling match; four checklist rows and bottom CTA remain visible. | Clipping, filled outlined controls, missing CTA, or unreadable hierarchy. |

## Results received 19 September 2026

Source: `Toki Shrine _ Owner device checklist _ Phase 3.docx`, supplied by Arjun and reviewed as owner evidence rather than task instructions. All four rendered pages and the document text were inspected. The report does not state its execution date, installed commit/build or device. P3-09's prescribed setup names the S23 Ultra, but that wording alone does not prove the device used for every check. Installed build therefore remains **unconfirmed**.

| ID | Reconciled owner result | Evidence and disposition |
|---|---|---|
| P3-01 | **Incomplete** | Report says only “Content corrections suggested to GLM”. It does not say whether the fresh-data, Back-navigation and no-entry-gate behaviors passed, nor identify the requested correction. Exact observation and post-repair retest remain required. |
| P3-02 | **Partial; repair and retest** | Grant/Granted labels and progress movement were reported as passing. Full four-permission coverage is blocked because Toki Shrine is absent from Display over other apps. Owner also supplied `design/screens/Accessibility screen design v2.png` and requested its Essential/Better experience grouping, order, copy and spacing. |
| P3-03 | **Functional pass; repair and visual retest** | Explainer-before-Settings and return refresh were reported as passing. Owner requested the exact replacement title/body/list content and supplied `design/screens/Accessibility Explainer.png` as the replacement reference. |
| P3-04 | **Pass** | Accessibility revocation refresh reported passing. |
| P3-05 | **Pass** | All tested activation gates reported passing. |
| P3-06 | **Fail — feel/tuning** | Owner requires the ON haptic to be approximately 30% longer. The state-change behavior was not reported as failing. Retest feel after tuning. |
| P3-07 | **Pass — visible behavior only** | Rapid/competing actions reported passing. This adds no database/durability evidence beyond the separately attributed P3-NV results. |
| P3-08 | **Fail — blocker** | Toki Shrine is absent from the system Display over other apps list, so grant/revoke/refresh could not be exercised. Source inspection confirms `android.permission.SYSTEM_ALERT_WINDOW` is absent from the manifest even though the app launches overlay Settings and calls `Settings.canDrawOverlays()`. |
| P3-09 | **Functional pass; repair and visual retest** | Samsung guidance, picker and battery-state behavior reported passing. Owner requires removal of the `I've done this` CTA and asked for a defensible battery-impact explanation; do not add a minimal-impact claim without measured Phase 4/6 evidence. |
| P3-10 | **Pass** | Deny/allow/revoke/retry reported passing. |
| P3-11 | **Pass** | Required Settings behavior reported passing. Other future/inert rows doing nothing is consistent with this phase so long as they do not crash. |
| P3-12 | **Pass — visible behavior only** | Rotation/background/resumption reported passing. This does not establish durable callback/database outcome evidence in P3-NV-03. |
| P3-13 | **Pass** | Partial-permission onboarding completion and relaunch reported passing. |
| P3-14 | **Incomplete; repair and retest** | Report requests helper text below `Save block`: `Turn it on from the “Blocks” homescreen.` It does not report the required name, empty-target, ownership, multiline-domain and new-block-OFF scenarios, so those remain open. |
| P3-15 | **Fail — visual** | Owner reports the permission header is too close to the status/notification bar; the rest was reported acceptable. Retest the revised checklist against the new reference at normal font/display scale. |

Unaffected passes retain this report's unconfirmed-build attribution. Repairs affect P3-02, P3-03, P3-06, P3-08, P3-09, P3-14 and P3-15. P3-01 requires a precise owner result before it can be classified. Do not relabel any of these as a fresh pass until the repaired build is identified and retested.

### Repair checkpoint awaiting owner retest

Codex reviewed repair commit `7d779a6` on `cb1ad45` and recorded **PASS WITH NOTES** for the code delta on 19 September 2026. Two later direct correction rounds culminated in `f92661d`; their accumulated range `7d779a6..f92661d` now has reviewer **PASS**. Preserve the original observations above as provenance and apply the newer identified-build results below.

### Owner retest — direct corrections on `f92661d`

- **Build/device/date:** debug build from `f92661d`, installed keep-data on Arjun's SM-S918B (Android 16) on 19 September 2026. Arjun reports that the two supplied correction sets pass.
- **Scope of report:** exact welcome copy; top and bottom safe spacing, including the Blocks title; accessibility handoff as far as the app can control it; manufacturer-neutral battery presentation without picker; final review helper spacing; and app-search `loading...` behavior. This report is not evidence for unmentioned behavioral or database scenarios.

| ID | Updated owner result | Evidence and disposition |
|---|---|---|
| P3-01 | **Targeted presentation pass; behavior pending** | The replacement welcome copy and revised top/bottom spacing pass. Fresh-data entry, Back-to-Blocks and create-without-permissions were not explicitly reported and remain open. |
| P3-03 | **Pass** | The earlier functional path passed; the replacement explainer/direct accessibility route is included in the now-passing correction set. Android's system-owned Installed apps row cannot be decorated by Toki Shrine, and the owner's request was conditional on feasibility. |
| P3-09 | **Pass under superseding owner requirement** | The manufacturer-neutral screen, Android wording and removal of the picker pass. The original picker expectation is retired; automatic Samsung/generic step selection remains an implementation detail. |
| P3-14 | **Targeted presentation pass; matrix pending** | The review helper spacing and app-search loading state pass. The name, empty-target, ownership, multiline-domain and new-block-OFF matrix still requires an explicit result. |
| P3-15 | **Pass for requested visual corrections** | Owner confirms consistent top/bottom spacing, including the Blocks title and bottom actions, after the earlier report said the remaining visual treatment was acceptable. |

P3-02, P3-06 and P3-08 remain open because this report did not state their required state/feel/deny-grant-revoke outcomes. P3-01 and P3-14 remain partially open as described above. Do not infer any P3-NV result from this device presentation pass.

### Wizard-preservation retest — owner report, 19 September 2026

Source: Arjun's direct report that BW-01–BW-10 passed. The report did not restate an installed build identifier; it applies only to the named visible paths and does not replace the separate nonvisual evidence requirements.

| ID | Updated result | Evidence and disposition |
|---|---|---|
| P3-01 | **Partially passed** | The create-without-permissions path passed through BW-09. Fresh-data Welcome behavior and Back-to-Blocks were not stated and remain open. |
| P3-14 | **Pass** | BW-09 reports the full Phase 2 preservation matrix passed: name, nonempty targets, ownership, multiline domain and OFF-save behavior. |

## Nonvisual evidence — separate from screen testing

| ID | Required evidence | Recorded status |
|---|---|---|
| P3-NV-01 | Pure route codec, single-flight orchestration and permission failure/cancellation/partial-retry coverage | Historical builder XML attributed by Codex: 44 JVM tests pass at cb1ad45; not rerun by migration |
| P3-NV-02 | Room block enabled-state/event and onboarding marker/event atomicity, at-most-once and rollback | **Pass.** Executed on SM-S918B: transition state/event commit once and forced event failure rolls state back; onboarding marker/event commits once and forced event failure rolls marker/first-launch writes back |
| P3-NV-03 | Real permission request/outcome persistence across Android recreation and callback/resume paths | **Pass by combined evidence.** Owner P3-08/P3-12 exercise the actual callback/resume/recreation paths; executed Android Room test restores a pending overlay request, proves failed outcome write stays pending, then proves recreation/retry persists exactly one outcome. JVM logic suite supplies the wider serialized callback/resume matrix |
| P3-NV-04 | Revised Phase 2 ownership/save/UI regression assertions carried into the Phase 3 preservation check | **Pass.** Complete revised instrumented suite executed on SM-S918B: 51 tests, 0 failures/errors/skips (BlockRepository 23, EventRepository 9, CreateFlowScreen 19) |

## Final owner report — 19 September 2026

Arjun reports that the four remaining owner scenarios pass on the latest installed direct-correction build recorded in CURRENT. This closes the owner/device portion of Phase 3, but it does not by itself establish the separate database and instrumented-test evidence below.

| ID | Final owner result | Evidence and disposition |
|---|---|---|
| P3-01 | **Pass** | Fresh-data Welcome/Back behavior and create-without-permissions now pass. |
| P3-02 | **Pass** | All four permission rows, labels, count and progress behavior pass. |
| P3-06 | **Pass** | ON/OFF state changes and the revised stronger/lighter haptic feel pass. |
| P3-08 | **Pass** | Overlay deny, grant, return-refresh and later revocation behavior pass. |

### Nonvisual execution — 19 September 2026

- Codex reran `./build.sh testDebugUnitTest`: **60 tests, 0 failures, 0 errors, 0 skipped**, including all 18 `PermissionEventLogicTest` cases.
- With Arjun's explicit authorization, Codex ran the Android suite on SM-S918B (`R5CW30ZBM2R`, Android 16). The first run exposed that the screen had turned off; after holding the display awake over USB, the suite exposed compile-only test defects rather than product failures. Codex made bounded test-only repairs: lifecycle-correct debug host, correct formatter/event expectations, and a rapid-save stimulus that does not wait through its own gate.
- Codex added five focused Android/Room cases: enabled transition commit/at-most-once and rollback; onboarding completion commit/at-most-once and rollback; restored permission outcome failure retention plus recreation/retry exactly once.
- Final `./build.sh connectedDebugAndroidTest`: **51 tests, 0 failures, 0 errors, 0 skipped** — BlockRepositoryTest 23, EventRepositoryTest 9, CreateFlowScreenTest 19.
- Gradle removed both app and test packages after execution; `pm path` returned no package and `enabled_accessibility_services` was `null`. The temporary USB stay-awake setting was restored. Reinstall/re-grant is required before further owner device use, but no Phase 3 result needs repeating.

Codex should identify the smallest exact missing evidence when results arrive. Device-only evidence requires owner-led validation or explicit execution authorization; do not silently waive it or build a large substitute framework.

## Historical testing risks — resolved at closure

The items below are retained as provenance. P3-10, P3-06 and P3-14 are resolved by the final owner report and nonvisual execution above; none remains an open Phase 3 gate.

- Notification retry after repeated denial; record a dead Grant action as an issue, not a platform-based automatic pass.
- Haptic feel: stronger ON/lighter OFF remains required; the repaired two-segment ON effect still needs owner feel confirmation.
- P3-14 carries forward the latest Phase 2 input/paste behavior and other affected regressions.

The 19 September repair represents the requested longer ON feedback as a heavy click followed by a delayed settling click because Android's predefined effect has no exposed duration. Treat owner feel-testing as the acceptance measurement rather than claiming a mathematically exact 30% transformation.
