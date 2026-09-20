# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 28.

## Phase 3 repair re-review — 14 September 2026

Codex reviewed committed repair `9a88b41..4b0ca0f` code-only: **CHANGES REQUIRED**. The saveable route stack, single-flight terminal actions and transactional state/event writes resolve the earlier navigation/duplication blocker. One lifecycle/event correction remains. `PermissionEventLogic.resolve/settle` currently removes saved pending permission identities before the granted/denied event insert returns; cancellation or an insert failure therefore loses the outcome permanently instead of retrying on resume. Emit first and remove each identity only after success, preserving failed/unprocessed requests, and add failure/retry coverage. `MainActivity` also sets its saved `launchResolved` flag before the suspended onboarding-completion read; recreation during that read can restore a resolved root and skip first-run Welcome. Mark resolution only after the read and route decision, while allowing restored non-root stacks to resolve without re-querying. Existing builder result XML records 39 JVM tests, zero failures/errors; other reported checks remain builder-attributed. No builds, test execution or device work by Codex. Device testing waits for the narrow repair and diff re-review.
