# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 10.

## Review responsibility split — owner decision, 10 September 2026

- Codex reviews code only: high-level architecture, data/state flow, persistence, validation, formatting and requirement compliance. No device operations, builds or test execution unless the owner explicitly changes scope. GLM retains builder testing duties; Arjun performs device experience checks from a numbered checklist supplied by Codex. Nonvisual checks are assigned explicitly to the builder.
- Report code verdict separately from owner device results; code approval alone does not clear a phase. Keep reviews focused and documentation minimal. Reuse existing instructions/reports; provide a pasteable prompt instead of creating a session-handoff document.
- Updated AGENTS.md §3a, HANDOVER.md and GLM_HANDOVER.md. Phase 2 repair a0df32c is ready for review in a NEW session; this session changed workflow instructions only and did not review the repair or issue sign-off.
