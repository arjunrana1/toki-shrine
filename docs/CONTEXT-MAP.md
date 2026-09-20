# Context map — select by requested behavior AND touched files

Do not read every linked document. TASK gives starting references; use this map when a change reaches another boundary. Within PRD, search headings and read relevant sections plus applicable §17 amendments. Current contracts summarize constraints; code/tests establish implementation, not owner intent. Report unresolved contradictions.

| Trigger / source area | Read | Specification / verification |
|---|---|---|
| Room, transactions, event writes, Stats, migration: `data/db`, `data/entity`, `data/repo`, `data/stats`, `TokiApplication.kt` | [Persistence/events](components/persistence-events.md) | PRD §§4/8–10/17; repository instrumented tests, Stats JVM tests |
| Create/edit, ownership, domain input, target lists: `CreateFlowScreen.kt`, `BlockDetailScreen.kt`, `TargetList.kt`, `Domain.kt`, `Estimate.kt` | [Blocks/targets](components/blocks-targets.md) | PRD §§4/6/7 and §17 including later addenda; Domain JVM, CreateFlow instrumented tests |
| Activation, navigation, permissions, onboarding: `MainActivity.kt`, `data/permissions`, `ui/navigation`, `TerminalAction.kt` and permission screens | [Navigation/permissions](components/navigation-permissions.md) | PRD §§5/6/10/12 + §17 haptics; Phase 3 task, JVM orchestration tests |
| Visual values, shared controls, icons/branding: `ui/theme`, `ui/components`, `ui/icons`, resources/design | [Theme/UI](components/theme-ui.md) | PRD §§6/15/17; relevant Nocturne tokens/markup/render only |
| Build, dependencies, non-device checks, explicitly authorized phone setup | [Build/validation](components/build-validation.md) | Existing build.sh, tools/env.sh, build files; no unsolicited environment rebuild |
| Detection service/URL/window logic, browser/OEM JSON | [Phase 4](phases/phase-04.md) + affected contracts above | PRD §§10/13/14; service currently declaration/prerequisite only |
| Challenge/hold/countdown, pause lifecycle | [Phase 5](phases/phase-05.md), [Phase 6](phases/phase-06.md) as applicable | PRD §§7/8/10/17; planned, no implementation clearance |
| Stats UI, feedback, distribution polish | [Phase 7](phases/phase-07.md) + persistence/theme contracts | PRD §§9/10/13; planned |
| Why a decision was made or superseded | [History index](history/INDEX.md) | Named entry only; old role policies are not operative |

## Open obligations by owner document

- Five-screen editor redesign: implemented and closed in [its task](../coordination/tasks/TS-block-wizard-redesign/TASK.md), with reviewer, owner and executed nonvisual PASS. PRD §7 and the latest §17 wizard amendment remain authoritative. Waiting is visible-and-unlocked, with no hold detection; challenge runtime remains Phase 5.

- Phase 3 owner results and executed nonvisual evidence are closed in the [Phase 3 checklist](../coordination/tasks/TS-P3-validation/OWNER-CHECKS.md).
- Create-flow draft restoration/shared accessibility semantics: blocks/targets and theme/UI contracts; do not mislabel the repaired Phase 3 route stack as still broken.
- Destructive migration policy, schema export and retained show_typos column: persistence/events contract before schema/distribution work.
- Separate Time Shrine rename and vector/themed icon polish, shadow approximation: theme/UI contract.
- Samsung overnight service survival and supported browser limits: Phase 4/PRD §14.

## Phase index

[0](phases/phase-00.md) · [1](phases/phase-01.md) · [2](phases/phase-02.md) · [3](phases/phase-03.md) · [4](phases/phase-04.md) · [5](phases/phase-05.md) · [6](phases/phase-06.md) · [7](phases/phase-07.md). Read only the applicable phase. No blanket historical report or complete-codebase startup scan.
