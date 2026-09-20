# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 12.

## Bounded runs and model guidance — owner update, 10 September 2026

- Added AGENTS.md §3c and pointers in both handoffs: phase-start model/scope/check/stop recommendations; GLM 5.3 for state, lifecycle and persistence work, Flash for bounded routine work, GLM 5.2 optional for mechanical edits. These are project allocation recommendations using owner-provided names, not independently verified model rankings. Codex remains code reviewer; Arjun chooses models.
- Stop editing after scoped work and authorized checks pass. After two unsuccessful attempts at one check, report and diagnose before another bounded attempt; no speculative loops or redundant green runs. Test dependencies still require owner approval under rule 8.
- Separate owner-authorized installation/preparation of an approved build from owner-run device testing. Preserve data, report setup, then stop; no phone-availability loops.
- Interrupted builder handoff reports test-harness corrections and device availability delays. Existing XML inspected read-only confirms 25 passing instrumented tests (12 repository, 6 event, 7 UI), correcting the handoff's six-UI-test count. Repair remains uncommitted; code correctness has not been re-reviewed here. Backup file exists at /tmp/toki-fixture-backup/toki-shrine.db; contents/restorability and current phone state were not verified. No feature edits or device operations performed in this workflow update.

- **Model guidance clarified after interrupted-run handoff (10 September 2026).** At every phase/repair start, state model, specific scope/base, checks and stop condition. Per Arjun, Flash reads images and is suited to bounded visual/reference work; GLM 5.3 is preferred for complex state/lifecycle/persistence. No mandatory two-model cycle. Existing retry/stop rules remain. An interrupted/read-only handoff uses local records and does not authorize phone queries; code status, evidence gaps and phone setup are reported separately. Handoff attributes delays to test-side corrections and device availability; it does not establish total elapsed time or prove product correctness. No feature changes or new tests run in this update.
