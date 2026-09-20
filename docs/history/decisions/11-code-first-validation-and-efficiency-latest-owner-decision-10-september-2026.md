# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 11.

## Code-first validation and efficiency — latest owner decision, 10 September 2026

- Supersedes the earlier split retaining GLM hardware checks: GLM now compiles and runs targeted non-device regressions only; Codex reviews code only. Arjun owns all device/experience testing after code blockers are resolved. Neither agent runs device operations or connected/instrumented tests without explicit authorization. Device-only nonvisual evidence remains explicitly pending, not waived or treated as visually verified.
- Repair/re-review cycles address the requested findings, new diff against the last reviewed submission and directly affected dependencies. No repeated whole-project audits, unrelated rewrites or reopening accepted phases without a concrete regression risk. Preserve work in progress.
- Both agents report progress and delays promptly, avoid redundant checks, and finish with commit/base, files/reasons, actual results and remaining evidence. Record known time sinks without invented timings. Existing handoffs/reports remain the durable context; no new documents.
- Updated AGENTS.md §§3a–3b, HANDOVER.md and GLM_HANDOVER.md. The last completed code review is FAIL on a0df32c (two P2 findings in the existing report); subsequent builder work requires diff re-review. This documentation update neither reviews ongoing feature changes nor clears Phase 2/3.
