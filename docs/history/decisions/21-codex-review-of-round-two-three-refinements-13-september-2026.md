# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 21.

## Codex review of round-two/three refinements — 13 September 2026

Reviewed `512dc36..084b5eb`, including affected input/save/event/haptic paths. Source unchanged by reviewer. Required: `CreateFlowScreenTest.kt:470` expects `none` despite its `showTypos=false` fixture becoming true on save (`CreateFlowScreen.kt:133,334`). Use an always-on fixture for the unchanged-edit scenario and retain explicit normalization coverage if testing legacy false values. At test line 505, wait for enabled semantics rather than a click action before Add. Compilation cannot establish these runtime assertions. No broad test rerun or feature rewrite requested.

Notes: newline stripping merges `reddit.com\nabdes` into syntactically valid `reddit.comabdes`; raw validator tests do not demonstrate rejection of that UI input. Owner should choose rejection of multiline paste if silent merging is unwanted; do not assume registered-TLD validation. Current haptics fire before the persistence coroutine (TurnOnScreen.kt:174; BlockListScreen.kt:117; BlockDetailScreen.kt:88). Phase 3 activation gating must emit success feedback only after permission checks and successful activation. Owner finds haptics weak and floated +30% duration; no exact tuning change approved. Current preset effects contain no app-defined duration. Disable helper uses “the block” where owner requested “this block”; match the requested copy in the bounded correction.

Phase 3 remains onboarding/permissions only, using GLM 5.3 for lifecycle/permission logic. Start after corrective diff approval; retain owner-led device validation of the newer changes as a separate gate. No device checks were executed by Codex.
