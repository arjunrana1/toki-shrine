# Toki Shrine — agent entry point

Read this file first. Arjun manually orchestrates architect, implementer and reviewer sessions. One agent writes at a time; no automatic dispatch or parallel writers.

## Start here

1. Read [CURRENT](coordination/CURRENT.md): active task, state, next actor.
2. Read the short [WORKFLOW](coordination/WORKFLOW.md) policy, then the referenced task. State role, scope/base, planned checks and stop condition briefly.
3. Confirm Git state and the exact submission/base. Preserve unrelated edits. If assignment/state conflicts, reconcile before editing; waiting for owner evidence does not authorize new work.
4. Load only records needed for this activity: handback/review for code review; assigned findings for repair; owner checklist/results for validation; phase scope for planning.
5. Follow task links and [CONTEXT-MAP](docs/CONTEXT-MAP.md) to relevant contracts, PRD sections, source and tests. Expand into dependencies for concrete correctness questions. History is not startup reading.

## Authority and boundaries

- [PRD](PRD.md) defines behavior. Its §17 and post-validation addenda supersede older values/copy/mockups for affected features. Read the applicable amendments with the relevant section. Ask Arjun about genuinely unresolved product behavior; decide routine implementation details within scope.
- Nocturne tokens define visual values; use the [theme/UI contract](docs/components/theme-ui.md) before visual work. The recorded Time Shrine rename is a separate pending change; preserve current Toki Shrine naming/package until assigned.
- Kotlin/Compose/Room; minSdk 33, targetSdk 36, applicationId `com.arjunrana.tokishrine`. No new dependencies or toolchain changes without owner approval. Use `./build.sh`; see [build/validation](docs/components/build-validation.md) when implementing or verifying.
- Codex may implement critical logic and efficient localized repairs under WORKFLOW; it is no longer restricted to review-only. GLM handles suitable routine work. After one unsuccessful GLM repair submission, reassess ownership before another assignment.
- Arjun owns all device and experience testing. No adb, device/emulator access, installations, screenshots, database extraction or instrumented execution unless explicitly authorized. An installation request permits the agreed setup, not exploratory testing.
- Preserve scope and acceptance tests; never weaken tests to obtain a pass. Code approval is separate from owner acceptance. Do not start the next phase without clearance.
- At handoff update the owning task records and CURRENT, preserving previous evidence before replacement. Keep reports concise and supply a short paste-ready next-role prompt. No growing global handoff/decision log. Document lifecycle is in [RECORDS](coordination/RECORDS.md), read when creating, closing or restructuring records.

The owner-approved workflow migration of 18 September 2026 replaces older role/startup policies in historical records. Live task records identify current code evidence; archived completion claims do not.
