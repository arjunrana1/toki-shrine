# Phase 3 — Onboarding and permissions

Phase 3 is closed. The accumulated correction range through `f92661d` has reviewer PASS; all P3-01–P3-15 owner checks pass; the final JVM and Android suites passed 60/60 and 51/51 respectively. Exact attribution and the earlier checkpoints remain preserved in [TS-P3-validation](../../coordination/tasks/TS-P3-validation/TASK.md).

Read only for this phase or an affected acceptance question. Scope/acceptance below was relocated from the original root instructions. PRD and applicable §17 amendments resolve stale copy/values; current WORKFLOW governs who performs checks. Historical shell/device commands describe evidence, not standing authorization; build through `./build.sh` and leave device operations to the authorized owner.

## Scope and acceptance

Screens 1–4 and 24. Welcome, the four-row checklist, accessibility explainer, manufacturer-neutral battery instructions with automatic device-specific steps, settings.

**Acceptance**

- The checklist shows exactly four rows, each in one of two states only: pending or granted.
- Granting a permission and returning to the app flips that row to granted without a manual refresh.
- The progress indicator reads `n of 4` and matches the number of granted rows.
- The accessibility explainer appears **before** the system permission screen is opened.
- The battery screen uses manufacturer-neutral framing while automatically showing Samsung or generic Android steps; there is no manufacturer picker.
- With accessibility not granted, tapping a block's ON toggle does **not** enable the block and routes to the checklist.
- With permissions absent, the app still opens, navigates and creates blocks — no gate before the ON toggle.
