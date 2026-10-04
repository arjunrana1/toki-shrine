# Historical records — load only to answer a specific question

Current contracts are indexed in [CONTEXT-MAP](../CONTEXT-MAP.md). Current work is in [CURRENT](../../coordination/CURRENT.md). The entries below retain superseded claims, old role policies and old test counts; none is a new sign-off.

## Decisions by original topic

- [Pre-build](decisions/01-pre-build.md)
- [Phase 0 — Project skeleton and Nocturne theme](decisions/02-phase-0-project-skeleton-and-nocturne-theme.md)
- [Phase 0 — Project skeleton and Nocturne theme](decisions/03-phase-0-project-skeleton-and-nocturne-theme.md)
- [Phase 1 — Data layer](decisions/04-phase-1-data-layer.md)
- [Phase 2 — Block list and create flow](decisions/05-phase-2-block-list-and-create-flow.md)
- [Open questions](decisions/06-open-questions.md)
- [Branding](decisions/07-branding.md)
- [Phase 2 independent verification — 9 September 2026](decisions/08-phase-2-independent-verification-9-september-2026.md)
- [Phase 2 app ownership UX — owner decision, 9 September 2026](decisions/09-phase-2-app-ownership-ux-owner-decision-9-september-2026.md)
- [Review responsibility split — owner decision, 10 September 2026](decisions/10-review-responsibility-split-owner-decision-10-september-2026.md)
- [Code-first validation and efficiency — latest owner decision, 10 September 2026](decisions/11-code-first-validation-and-efficiency-latest-owner-decision-10-september-2026.md)
- [Bounded runs and model guidance — owner update, 10 September 2026](decisions/12-bounded-runs-and-model-guidance-owner-update-10-september-2026.md)
- [Phase 2 repair finalized — 12 September 2026](decisions/13-phase-2-repair-finalized-12-september-2026.md)
- [Phase 2 owner experience feedback — 12 September 2026](decisions/14-phase-2-owner-experience-feedback-12-september-2026.md)
- [Phase 2 owner refinements implemented — 13 September 2026](decisions/15-phase-2-owner-refinements-implemented-13-september-2026.md)
- [R7 name-input correction — 13 September 2026](decisions/16-r7-name-input-correction-13-september-2026.md)
- [Phase 2 post-validation fixes — 13 September 2026](decisions/17-phase-2-post-validation-fixes-13-september-2026.md)
- [Phase 2 checkpoint and Phase 3 readiness — 13 September 2026](decisions/18-phase-2-checkpoint-and-phase-3-readiness-13-september-2026.md)
- [Phase 2 post-validation fixes, round two — 13 September 2026](decisions/19-phase-2-post-validation-fixes-round-two-13-september-2026.md)
- [Phase 2 round-three fixes — 13 September 2026](decisions/20-phase-2-round-three-fixes-13-september-2026.md)
- [Codex review of round-two/three refinements — 13 September 2026](decisions/21-codex-review-of-round-two-three-refinements-13-september-2026.md)
- [Codex corrective pass on 512dc36..084b5eb — 13 September 2026](decisions/22-codex-corrective-pass-on-512dc36-084b5eb-13-september-2026.md)
- [Multiline website paste: reject, never merge — owner clarification, 13 September 2026](decisions/23-multiline-website-paste-reject-never-merge-owner-clarification-13-september-2026.md)
- [Corrective diff closure — 13 September 2026](decisions/24-corrective-diff-closure-13-september-2026.md)
- [Phase 3 implementation — onboarding and permissions — 14 September 2026](decisions/25-phase-3-implementation-onboarding-and-permissions-14-september-2026.md)
- [Phase 3 independent code review — 14 September 2026](decisions/26-phase-3-independent-code-review-14-september-2026.md)
- [Phase 3 repair — recreation and terminal-action blockers — 14 September 2026](decisions/27-phase-3-repair-recreation-and-terminal-action-blockers-14-september-2026.md)
- [Phase 3 repair re-review — 14 September 2026](decisions/28-phase-3-repair-re-review-14-september-2026.md)
- [Phase 3 repair, round two — outcome durability and launch race — 14 September 2026](decisions/29-phase-3-repair-round-two-outcome-durability-and-launch-race-14-september-2026.md)
- [Phase 3 final code re-review — 14 September 2026](decisions/30-phase-3-final-code-re-review-14-september-2026.md)

## Exact pre-migration snapshots

The six files in `migration-2026-09-18/` preserve all original bytes, including uncommitted owner edits. They are recovery/provenance material, never startup instructions.

- [Original root instructions](migration-2026-09-18/AGENTS.before.txt)
- [Original reviewer handoff](migration-2026-09-18/HANDOVER.before.txt)
- [Original builder handoff](migration-2026-09-18/GLM_HANDOVER.before.txt)
- [Original decisions log](migration-2026-09-18/DECISIONS.before.txt)
- [Original README](migration-2026-09-18/README.before.txt)
- [Original product specification](migration-2026-09-18/PRD.before.txt)

Existing Verification Feedback reports remain in place. Earlier PRD-phase-0.md and DESIGN-BRIEF-phase-1.md are historical design inputs, not the current specification.

## Document freshness maintenance — 27 September 2026

[TS-docs-freshness](../../coordination/tasks/TS-docs-freshness/TASK.md) records the review, cleanup plan and checks at base `89ef9e3`. Its immutable snapshots preserve the former [CURRENT installation/status history](../../coordination/tasks/TS-docs-freshness/submissions/89ef9e3/CURRENT.before.txt), [Phase 7 HANDBACK](../../coordination/tasks/TS-docs-freshness/submissions/89ef9e3/P7-HANDBACK.before.txt) and [Phase 7 REVIEW](../../coordination/tasks/TS-docs-freshness/submissions/89ef9e3/P7-REVIEW.before.txt). Relative links inside those plain-text snapshots retain their original source-file meaning. They are historical evidence, not current status or a new code review.

## Archived submission link bases

Four existing Markdown links were copied verbatim when handbacks/reviews moved into immutable submission folders. Resolve them from their original owning-task directory using this table; the archived bytes remain unchanged.

| Archived source | Original relative link | Resolved destination |
|---|---|---|
| `TS-P5-owner-corrections/submissions/7fbb692/REVIEW.md` | `OWNER-CHECKS.md` | [P5 owner checks](../../coordination/tasks/TS-P5-owner-corrections/OWNER-CHECKS.md) |
| `TS-P6-pause-lifecycle/submissions/062c71e/HANDBACK.md` | `REVIEW.md` | [P6 review](../../coordination/tasks/TS-P6-pause-lifecycle/REVIEW.md) |
| `TS-P6-pause-lifecycle/submissions/7fb322e/HANDBACK.md` | `submissions/062c71e/HANDBACK.md` | [P6 prior handback](../../coordination/tasks/TS-P6-pause-lifecycle/submissions/062c71e/HANDBACK.md) |
| `TS-P6-pause-lifecycle/submissions/7fb322e/HANDBACK.md` | `OWNER-CHECKS.md` | [P6 owner checks](../../coordination/tasks/TS-P6-pause-lifecycle/OWNER-CHECKS.md) |
