# Manual architect–implementer–reviewer workflow

Owner-approved 18 September 2026. Project adaptation of the reusable playbook, version 1.0; provenance is in [RECORDS](RECORDS.md). Arjun starts each session and selects models. No automatic agent dispatch.

## Shared rules

Read AGENTS → CURRENT → active TASK plus role records. Verify Git state/base; preserve unrelated work. Say role/model recommendation, scope/base, checks and stop condition briefly. One writer at a time. Do not restart approved work to produce a report. Stop if interrupted; report from local evidence without launching extra checks/device queries. Give useful progress updates and explain actual delays without inventing timings.

## Assign by remaining reasoning

| Work | Default owner |
|---|---|
| Routine screens, copy, visual corrections, established patterns | GLM; Flash for bounded visual/mechanical work where suitable |
| Designing/repairing cancellation, concurrency, durable event ordering or nontrivial state machines | Senior model (Codex session using the owner's chosen capable model) |
| Routine wiring around an established critical component | GLM if the boundary is clear |
| Small localized repair already diagnosed during review | Codex may implement directly when cheaper than a handoff |
| Ambiguous product behavior/new experience tradeoff | Arjun decides |

These are task-based defaults, not model benchmark claims. Recommend critical ownership before delegation. Do not split trivial work merely to involve both models. Senior implementation within an authorized task does not require another permission round; announce the role change and record authorship. Do not silently switch models or launch agents.

## Implementation and repairs

Use the task's invariants, files, exclusions and planned checks. For stateful work specify relevant failure/cancellation/recreation/duplicate/stale-state cases; add meaningful focused coverage, not a large substitute-device framework. Implementer, including Codex when it writes code, runs proportionate authorized non-device build/checks. After checks pass, stop; broaden only for a new change, failure or concrete unresolved risk. Keep one scoped submission and stage only intended files; no `git add -A` with unrelated work present.

After **one unsuccessful GLM repair submission of an assigned finding**, reassess ownership before another GLM repair. Prefer senior implementation for unresolved correctness reasoning. Count by defect across sessions/task names, not transient compiler errors within a run. For repeated infrastructure failures, stop that path after two attempts, report diagnosis and next bounded action; this is not a second product-repair allowance.

## Direct owner → GLM corrections

Arjun can report small, clear testing issues directly to GLM without waiting for Codex or finishing the checklist. GLM records reproduction/expected/observed behavior and affected check IDs in the active task, makes the bounded correction, verifies proportionately and updates HANDBACK/CURRENT. A new task folder is unnecessary for every typo.

Escalate before editing if diagnosis involves critical state/data logic, an ambiguous requirement or scope expansion. One unsuccessful submitted repair triggers reassessment. A previous PASS still applies only to its reviewed submission; new changes are pending review. Arjun may continue testing, recording the installed build; Codex reviews the accumulated diff at the next checkpoint. Retest affected checks and retain unaffected results with their original build attribution.

## Review and acceptance

Review the submitted diff, prior findings and enough relevant callers/contracts/tests to substantiate correctness. Re-review only the repair delta and affected dependencies; explain broader inspection by concrete risk. Consolidate actionable findings with ID, location, failing scenario, consequence and correction; distinguish blockers from notes. No cosmetic vetoes, speculative redesign or reopening accepted phases without evidence.

When reviewing only, inspect and attribute existing builder evidence; execute additional non-device checks only where a concrete unresolved correctness/evidence question justifies them. When implementing, perform the required checks and label own verification honestly. Self-verification is not independent review; consequential changes may warrant a separate scoped review, not an automatic whole-project audit.

Record PASS / PASS WITH NOTES / FAIL for an exact submission. Owner acceptance remains separate; compilation is not instrumented execution and screen observations cannot prove database/event assertions. List evidence gaps precisely; never waive them silently. Phase closure needs no code blockers and completed required acceptance evidence. Code PASS ends speculative review; wait for owner feedback. No next-phase implementation without clearance.

## Manual handoff

Implementer writes HANDBACK; reviewer writes REVIEW/next TASK; results go to OWNER-CHECKS with source/build attribution. Update CURRENT last. Give Arjun a short prompt naming the task and requested role, with any essential unresolved choice. Refer to repository instructions instead of pasting the complete history. See [RECORDS](RECORDS.md) for schemas, preservation and closure.
