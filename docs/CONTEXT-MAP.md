# Context map — select by requested behavior AND touched files

Do not read every linked document. TASK gives starting references; use this map when a change reaches another boundary. Within PRD, search headings and read relevant sections plus applicable §17 amendments. Current contracts summarize constraints; code/tests establish implementation, not owner intent. Report unresolved contradictions.

| Trigger / source area | Read | Specification / verification |
|---|---|---|
| Room, transactions, event writes, Stats, migration: `data/db`, `data/entity`, `data/repo`, `data/stats`, `TokiApplication.kt` | [Persistence/events](components/persistence-events.md) | PRD §§4/8–10/17; repository instrumented tests, Stats JVM tests |
| Create/edit, ownership, domain input, target lists: `CreateFlowScreen.kt`, `BlockDetailScreen.kt`, `TargetList.kt`, `Domain.kt`, `Estimate.kt` | [Blocks/targets](components/blocks-targets.md) | PRD §§4/6/7 and §17 including later addenda; Domain JVM, CreateFlow instrumented tests |
| Activation, navigation, permissions, onboarding: `MainActivity.kt`, `data/permissions`, `ui/navigation`, `TerminalAction.kt` and permission screens | [Navigation/permissions](components/navigation-permissions.md) | PRD §§5/6/10/12 + §17 haptics; Phase 3 task, JVM orchestration tests |
| Visual values, shared controls, icons/branding: `ui/theme`, `ui/components`, `ui/icons`, resources/design | [Theme/UI](components/theme-ui.md) | PRD §§6/15/17; relevant Nocturne tokens/markup/render only |
| Build, dependencies, non-device checks, explicitly authorized phone setup | [Build/validation](components/build-validation.md) | Existing build.sh, tools/env.sh, build files; no unsolicited environment rebuild |
| Detection service/URL/window logic, browser/OEM JSON: `TokiAccessibilityService.kt`, `detection` | [Phase 4](phases/phase-04.md) + affected contracts above | PRD §§10/13/14; implemented and closed, with recorded waivers/deferred risk |
| Challenge/countdown: `BlockActivity.kt`, `challenge`, `ChallengeRepository.kt`; pause lifecycle: `pause` | [Phase 5](phases/phase-05.md), [Phase 6](phases/phase-06.md) as applicable | PRD §§7/8/10/17; both implemented and closed; waiting is visible-and-unlocked with no hold detection |
| Stats UI, feedback, distribution polish: `StatsScreen.kt`, `FeedbackScreen.kt`, feedback utilities | [Phase 7](phases/phase-07.md) + persistence/theme/navigation contracts | PRD §§9/10/13 + [Stats data/UI contract](components/stats.md); Codex data layer prepared, GLM redesign UI next |
| Future PostHog integration, remote event export, identity or consent | [PostHog plan](plans/posthog-analytics.md) + persistence/build contracts | Proposed follow-up after Phase 7 closure; PRD §10 remains local-only until amended |
| Why a decision was made or superseded | [History index](history/INDEX.md) | Named entry only; old role policies are not operative |

## Open obligations by owner document

- Five-screen editor redesign: implemented and closed in [its task](../coordination/tasks/TS-block-wizard-redesign/TASK.md), with reviewer, owner and executed nonvisual PASS. PRD §7 and the latest §17 wizard amendment remain authoritative; Phase 7 restored production bounds in every variant.

- Phase 5 delivery: closed ([owner-correction closure](../coordination/tasks/TS-P5-owner-corrections/OWNER-CHECKS.md)). RV2-N1's unused `Abandoned` handler was removed in Phase 7; do not revive it. RV2-N2/RV2-N3 describe the historical coverage limits, not new blockers or executed coverage.
- Phase 6 pause/access/re-arm, bubble and notification: closed in [TS-P6-pause-lifecycle](../coordination/tasks/TS-P6-pause-lifecycle/TASK.md). Preserve P6C-N1's accepted theoretical edge and P6C-O4/P6-O13's explicit do-not-fix decisions, including platform-permitted notification dismissal/re-post.
- Phase 7: [TS-P7-stats-feedback-final](../coordination/tasks/TS-P7-stats-feedback-final/TASK.md) is reopened for the approved Stats redesign; GLM UI implementation follows Codex data work, then scoped review and owner acceptance. See CURRENT for the next actor.

- Phase 3 owner results and executed nonvisual evidence are closed in the [Phase 3 checklist](../coordination/tasks/TS-P3-validation/OWNER-CHECKS.md).
- Create-flow draft restoration/shared accessibility semantics: blocks/targets and theme/UI contracts; do not mislabel the repaired Phase 3 route stack as still broken.
- Migration posture is implemented: destructive fallback removed, v3/v4 exported and Stats-only MIGRATION_3_4 added, `show_typos` already absent. Read persistence/events before future schema changes; Phase 7 upgrade/data-preservation acceptance remains pending.
- Separate Time Shrine rename and vector/themed icon polish, shadow approximation: theme/UI contract.
- Samsung overnight service survival and supported browser limits: Phase 4/PRD §14.

## Phase index

[0](phases/phase-00.md) · [1](phases/phase-01.md) · [2](phases/phase-02.md) · [3](phases/phase-03.md) · [4](phases/phase-04.md) · [5](phases/phase-05.md) · [6](phases/phase-06.md) · [7](phases/phase-07.md). Read only the applicable phase. No blanket historical report or complete-codebase startup scan.
