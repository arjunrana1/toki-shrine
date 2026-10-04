# PostHog analytics — proposed follow-up

- **Status:** planning proposal, 27 September 2026. Arjun wants PostHog integration after the documentation cleanup and Phase 7. No SDK, consent choice, remote payload or privacy amendment is approved by this document.
- **Sequence:** finish [Phase 7](../../coordination/tasks/TS-P7-stats-feedback-final/TASK.md) owner acceptance and the pending bounded Codex confirmation; approve the choices below; open one scoped implementation task. [CURRENT](../../coordination/CURRENT.md) remains the live assignment.
- **Current authority:** PRD §§3/10/13 and [persistence/events](../components/persistence-events.md) still require local-only analytics. The new task must explicitly amend the product/privacy text and authorize the SDK/network changes it needs. Accounts/login and the Time Shrine rename are separate work.

## Decisions needed from Arjun

| Choice | Proposal for discussion | Status |
|---|---|---|
| Purpose and success measures | Measure activation, return usage, challenge/walk-away outcomes and feature engagement; select the questions the dashboards should answer first | Owner priorities pending |
| Consent and user control | Explicit opt-in with a Settings control; decide what withdrawal does to queued data and installation identity | Owner choice pending; no default approved |
| Identity | Random installation-scoped identifier; no account or email needed for this scope | Proposed |
| Remote data | Allowlisted event names and coarse properties, with no apps/sites/block names or user text | Allowlist/property schema pending approval |
| PostHog project | Choose the project/region, retention expectations and beta/production separation | Owner/project details pending; verify current vendor options when preparing implementation |

## Proposed implementation boundaries

- Keep local Room writes and Stats independent of remote availability, consent or upload success. Analytics failure must not delay the interruption, challenge, pause or re-arm paths.
- Define a separate sanitized remote schema. Do not upload the local event table wholesale: it contains app/site identity and other local-only details.
- Exclude block names, package names, domains/URLs, feedback/challenge text, raw accessibility content, exact timer ticks and individual accessibility callbacks. Use only the reviewed event/property allowlist.
- Leave automatic capture, session replay and unrelated profiling outside the initial scope. No historical event backfill without a separately approved purpose and consent rule.
- Cover every approved committed-event path. `BlockRepository`, `ChallengeRepository` and `EventRepository` write events, including inside Room transactions; adding an upload hook only to `EventRepository.log()` would miss outcomes. Select a delivery approach after examining these boundaries and current SDK behavior.
- Specify offline retry, duplicate handling, bounded storage, withdrawal, process restart and environment isolation before implementation. Export only committed outcomes; no remote success claim may stand in for local persistence.

## Handoff prerequisites and checks

The implementation TASK should record the approved PRD/privacy changes, project configuration, SDK/version and manifest/dependency additions, allowed events/properties, consent/identity lifecycle, delivery guarantees and release disclosures. Use a senior implementation owner for consent/persistence/delivery reasoning, with a separate scoped review; Arjun selects the model/session under WORKFLOW.

Verify vendor guidance and supported SDK behavior against current official documentation when creating that task. Required checks should target consent-off/no-upload, sanitized payloads, all committed-event paths, failed local writes, offline/retry/duplicate behavior, withdrawal and unchanged local Stats/challenge/pause behavior. Prepare owner checks for the opt-in/out experience and test-project event inspection. This proposal adds no implementation or device permission.
