# Phase 0 re-verification — PASS WITH NOTES

Reviewed commit `221ae964329899afc30f65882b71658205f123ea` on 9 September 2026. This report supersedes the technical verdict in `../phase-0-verification/REPORT.md` for this commit only.

Both original code defects are fixed, and all five Phase 0 acceptance checks pass independently. The owner approved the error palette on 9 September 2026. Phase 0 is closed; GLM may proceed with Phase 1 (data layer only). Shadow visual comparison remains a non-blocking later UI checkpoint.

## Original findings

- **Closed: missing titleMedium.** `app/src/main/java/com/arjunrana/tokishrine/ui/theme/Type.kt:46` maps titleMedium to heading(16). Reviewed the full file and helper implementations: all 15 slots explicitly resolve through heading/body to the bundled Inter family. The h5 mapping uses weight 500, 17.92sp line height and -0.24sp tracking.
- **Closed: eight inherited Material color roles.** `app/src/main/java/com/arjunrana/tokishrine/ui/theme/Theme.kt:186` through line 199 explicitly maps all eight omitted roles. Inverse roles reuse canonical ramps. Scrim encodes the CSS neutral-900/50% value. The error roles are now explicit but introduce the design proposal below. surfaceTint still safely defaults to the supplied primary in resolved Material 3 1.2.1.

## Acceptance evidence

| Check | Result | Evidence |
|---|---|---|
| Debug build | PASS | `./build.sh assembleDebug` exited 0: BUILD SUCCESSFUL in 685ms; incremental build, 1 task executed, 34 up-to-date. See evidence/build.txt. |
| Install | PASS | adb install -r returned Success on the single USB-connected SM-S918B. See evidence/device.txt. |
| Launch / background | PASS | Cold launch Status ok, TotalTime 510ms; MainActivity had window focus. Screenshot visually inspected. All 913,920 pixels in x=[20,700), y=[100,1444) are RGB(22,24,38), exactly #161826. See evidence/phase0-screen.png and evidence/pixels.txt. |
| Prescribed hex grep | PASS | No matches outside ui/theme; grep exit 1 is the expected no-match result. See evidence/static-checks.txt. |
| Canonical tokens | PASS | All 51 remain represented. Rechecked 35 literal colors, divider, six spacing values and three radii against CSS; font definitions and documented shadow approximation unchanged. See evidence/static-checks.txt. |

## Owner decision — approved 9 September 2026

`DECISIONS.md:38`, `app/src/main/java/com/arjunrana/tokishrine/ui/theme/Color.kt:50–57`.

The owner approved error #d4716b, errorContainer #612421, onErrorContainer #ffebe8 and onError = background #161826 as an explicit Nocturne extension. No design approval remains pending.

## Non-blocking notes

- **Carry forward shadow visual check:** DECISIONS.md:39 correctly preserves the earlier caveat. Compare the first card/dialog consumers with their renders before accepting the approximation across screens.
- **Minor historical wording:** DECISIONS.md:34 says the old titleMedium tracking was 0.15sp. The resolved 1.2.1 library inspected in the original review used 0.2sp. This is only a historical documentation correction and does not affect the fixed style.
- **Audit files:** retaining the original report and supporting evidence in Git is acceptable and consistent with keeping reports in the project. No need to remove them. These are verification artifacts, not later-phase feature code.

## Scope and limits

Only Color.kt, Theme.kt and Type.kt changed in the implementation; those files were re-read in full. Build setup, dependencies, manifest, activity and remaining resources are unchanged from the prior full review. No later-phase implementation found. Existing shadow and toolchain caveats remain; no repeated tests beyond the Phase 0 checkpoint were necessary. No accessibility detection tests apply because Phase 0 has no service.

The working tree was clean at review start. This session adds this report/evidence and records the owner approval and phase closure in DECISIONS.md. No feature code was changed, and no commit or push was made. The local main branch was one commit ahead of its locally recorded origin/main; remote status was not refreshed.
