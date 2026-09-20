# Records and selective context — read when maintaining the protocol

Adaptation: human-led architect–implementer–reviewer playbook **1.0**, owner-approved 18 September 2026. Upstream: [playbook 1.0 at adopted commit 4bdc9f0](https://github.com/arjunrana1/human-in-the-loop-architect-implementer-reviewer-workflow/blob/4bdc9f01e7da97e32f99c4d27915df2897084617/PLAYBOOK.md). Local instructions work offline; never fetch the upstream guide at routine startup or silently adopt updates.

## Ownership of facts

- CURRENT: one active task, state, next actor; links to evidence rather than duplicating it.
- TASK: task ID, goal, implementation owner/reason, base and allowed working-tree state, scope/exclusions, relevant contract/spec/source links, invariants, checks and stop condition.
- HANDBACK: task/submission ID, exact code/base, change summary, commands/results/evidence locations, limitations and readiness. Implementer maintains it.
- REVIEW: exact reviewed submission/base, verdict/limits, stable finding IDs and resolution, repair owner/reason. Reviewer maintains it.
- OWNER-CHECKS: stable IDs, setup/actions/expected results, actual owner reports with installed build/date and affected retest status. Agent records human reports without inventing results.
- Components: current interfaces/invariants, direct dependencies, source/test pointers and open obligations. PRD remains product authority, including applicable amendments.
- Phases: scope, acceptance and prerequisites; no global narrative. History/ADRs: rationale and past evidence, not live workflow instructions.

## Creation, preservation and closure

Create a task for a coherent independent assignment, a contract for a distinct component, and an ADR only for substantial lasting rationale. Reuse current sections for trivial owner corrections; do not create one file per session or command. Every new file must have an incoming task/map/phase link.

Update current records in place. Before replacing a handback/review, preserve the prior record in a scoped documentation commit or a small immutable `submissions/<submission-id>/` snapshot inside that task. Record the code commit separately from the later documentation commit. Never assume uncommitted edits are recoverable. Retain finding IDs across revisions. Full command logs live outside routine context; handbacks point to the evidence needed to reproduce a result.

At closure retain the task folder as history, promote lasting invariants to component contracts and open obligations to their component/phase, then move CURRENT. Do not load closed tasks by default. A phase transition creates its next task, updates affected contracts and changes the pointer; it does not restructure the repository or replay all prior phases. Read history only for a named gap.

Use `ready`, `implementing`, `ready_for_review`, `changes_requested`, `awaiting_owner`, `blocked`, `closed` with evidence-backed meanings. No queued/waiting state authorizes invented work. Check code/base against Git; reconcile mismatch before approving or editing. No silent task replacement when the human reports results during work.

## Keeping startup lean

Entry: AGENTS + CURRENT + short WORKFLOW + active TASK. Role inputs: HANDBACK/REVIEW for review, assigned findings for repair, OWNER-CHECKS for human results, phase scope for new planning. TASK supplies initial component links; CONTEXT-MAP maps actual touched files/behavior to additional contracts. Follow dependencies only when affected. Read PRD sections and applicable §17 amendments, not the whole document for every repair.

Keep stable instructions separate from current status. Replace stale rules; do not stack contradictory dated updates. Split independently useful topics; avoid arbitrary tiny-file proliferation. Contracts describe boundaries, not every implementation line. Verify links, states, submission references and evidence attribution at handoff. For existing entry-file redirects, follow the new entry point rather than archived prompts.

## Fresh-session prompts

- Reviewer: “Read AGENTS.md and resume the current task as reviewer. Review the recorded submission and unresolved findings.”
- Builder: “Read AGENTS.md and implement the current task assigned to GLM. Save the handback when finished.”
- Owner results: “Read AGENTS.md and resume the current owner-validation task. Here are my results by checklist ID: …”
- Direct correction: “Read AGENTS.md. Fix this bounded testing issue under the direct owner-to-GLM path: [build, steps, expected, observed, check ID]. Escalate if it crosses that boundary.”

Verify each harness actually loads the entry instructions in fresh sessions. An explicit root-file prompt is the portable fallback; this migration does not claim to have run a new GLM session.
