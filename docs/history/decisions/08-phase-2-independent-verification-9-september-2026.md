# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 8.

## Phase 2 independent verification — 9 September 2026

- **8f1ad1e: FAIL; Phase 3 not cleared.** Fresh 7 JVM / 16 device tests and the seven happy-path acceptance scenarios pass, but device checks reproduce target removal from an ON source through conflict moves, taps passing through the conflict backdrop, and Android Back discarding the flow without abandonment telemetry. See `Verification Feedback/phase-2-verification/REPORT.md`. No production code patched by verifier; original phone data restored.
- **Correction to the Phase 2 dialog record.** The reviewed commit uses an in-place Box in ConflictSheet, not the separate Dialog window claimed above; backdrop taps reach underlying controls. The builder must correct the implementation and record the tested result before this becomes guidance for future phases.
- **Fresh builder session.** `GLM_HANDOVER.md` captures the repair boundary and durable context; `HANDOVER.md` remains the verifier-role document. Keep unrelated owner branding work separate from Phase 2 repair commits.
