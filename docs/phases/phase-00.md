# Phase 0 — Project skeleton and Nocturne theme

Historical closure recorded at 221ae96; see history. Do not reopen without a concrete regression.

Read only for this phase or an affected acceptance question. Scope/acceptance below was relocated from the original root instructions. PRD and applicable §17 amendments resolve stale copy/values; current WORKFLOW governs who performs checks. Historical shell/device commands describe evidence, not standing authorization; build through `./build.sh` and leave device operations to the authorized owner.

## Scope and acceptance

Gradle project, Compose, Room dependency, and the Nocturne tokens translated into a Compose theme (colours, type scale, spacing, shapes, elevation). App launches to an empty themed screen.

**Acceptance**

- `./gradlew assembleDebug` exits 0.
- `adb install -r app/build/outputs/apk/debug/app-debug.apk` prints `Success`.
- Launching the app shows a screen whose background is `#161826` (verify by screenshot).
- `grep -rE "#[0-9a-fA-F]{6}" app/src/main/java --include=*.kt | grep -v ui/theme/` returns **no matches**.
- The theme defines every token in `styles.css` `:root`: `--color-bg`, `--color-surface`, `--color-text`, `--color-accent`, all nine steps of `--color-neutral-*` and `--color-accent-*`, six `--space-*`, three `--radius-*`, three `--shadow-*`.
