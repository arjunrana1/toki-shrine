# Phase 1 verification — FAIL

Reviewed `8ea7a33d42ee8390aef2b506b22b98887534b34d` on 9 September 2026 against Phase 0 closure `ff89edd`. Read the phase diff, every changed implementation/test file in full, PRD requirements and DECISIONS.md. Scope is data layer only; no premature UI or service work found.

The submitted acceptance suite passes, but three additional device regression probes reproduce correctness defects. Fix these before Phase 2. No production code was changed by the verifier.

## Blocking findings for GLM

### P1 — Make block creation one transaction

`app/src/main/java/com/arjunrana/tokishrine/data/repo/BlockRepository.kt:36–54`.

createBlock commits the parent row, then inserts each app and site independently. Reproduction: create Existing holding com.instagram.android; attempt Failed with [com.reddit.frontpage, com.instagram.android]. The second target raises SQLiteConstraintException, yet Failed and its first app remain saved. The device probe expected one block and found two. A rejected save can therefore leave an incomplete block and reserve targets the user did not successfully save. Cancellation between writes has the same structural risk.

Wrap parent and all child inserts in a single Room transaction, with rollback on any failure or cancellation. Keep the uniqueness constraints. Add a regression test asserting both the failed parent and previously inserted children are absent after rejection, while the original block remains intact. Prechecking conflicts in a later editor is not a substitute for atomic persistence.

### P2 — Enforce uniqueness for domain identity, not raw spelling

`app/src/main/java/com/arjunrana/tokishrine/data/repo/BlockRepository.kt:79–86`; `app/src/main/java/com/arjunrana/tokishrine/data/entity/BlockedTarget.kt:36`.

The repository and UNIQUE index compare raw domain strings case-sensitively. Reproduction: add reddit.com to A, then REDDIT.COM to B. The second call returns Added instead of Conflict(A). These are the same domain, so the database does not actually guarantee PRD section 4's single-block ownership rule for websites.

Define a consistent canonical domain representation at the repository/storage boundary, or reject noncanonical input consistently, and use it for create/add/find/move/remove. The database constraint must protect the same identity. At minimum, case-only variants must not enter separate blocks. Preserve whole-domain/subdomain semantics from PRD section 13; do not invent path-level blocking. Add case-variant tests for creation and repository mutations.

### P2 — Keep website events out of the per-app leaderboard

`app/src/main/java/com/arjunrana/tokishrine/data/db/EventDao.kt:32–37`, consumed by `data/repo/EventRepository.kt` getStats.

countsByTarget groups every non-null walk-away target, including domains. Reproduction: one Instagram walk-away and two reddit.com walk-aways. getStats returns [reddit.com, com.instagram.android] for mostWalkedAwayFrom. PRD section 9 specifies a per-app leaderboard, while global totals correctly include both app and website events.

Persist/use a reliable target-kind distinction and filter only the leaderboard to app events. Keep global walk-away totals inclusive. Do not infer target type solely from dots: both package names and domains contain them. Keep the PRD event schema and any added metadata consistent. Add a mixed app/site fixture verifying global total 3 and a leaderboard containing only Instagram with count 1.

## Non-blocking test note

`app/src/androidTest/java/com/arjunrana/tokishrine/data/EventRepositoryTest.kt:144` uses Kotlin assert for the JSON-content check. JVM assertions are conditional, so use JUnit assertions and parse the JSON to verify actual key/value round-trip. The other acceptance assertions are substantive; this does not invalidate the entire suite.

## Independently run evidence

- `./build.sh testDebugUnitTest connectedDebugAndroidTest`: exit 0, BUILD SUCCESSFUL in 13s, 10 device tests passed on the single USB SM-S918B / Android 16. JVM results were initially up-to-date.
- `./build.sh testDebugUnitTest --rerun-tasks`: exit 0, BUILD SUCCESSFUL in 7s, all 7 JVM tests passed freshly.
- The four specified fixture checks are present and pass: block field round-trip, cross-block app rejection, 100-event query, and seeded Stats formulas including rate and descending app counts.
- Three verifier-only instrumented probes: all failed at the specific assertions above, not during setup/build. See evidence/probes.txt and evidence/probes-device.xml.
- Probe source is retained at evidence/Phase1VerificationProbe.kt for GLM to reproduce/adapt. It was temporarily placed in androidTest, then removed after execution. Production and submitted test sources are unchanged. Only review artifacts and the decision-log review outcome remain as working-tree changes.

## Decisions and scope assessment

Normalized target tables, foreign keys with cascading target deletion, event retention after block deletion, and the corrected walk-away rate are sound choices. First-use metadata and future UI integration are explicitly documented. The createBlock decision accepts a loud uniqueness error but overlooks partial persistence; finding 1 corrects that omission. Test dependencies and the Room processor support the requested phase and do not add runtime networking. No schema-export or future UI work is requested as part of these fixes.

Phase 1 remains at its review boundary. GLM should commit these fixes separately, then return for re-verification. The original implementation commit remains a useful checkpoint even though this review fails.
