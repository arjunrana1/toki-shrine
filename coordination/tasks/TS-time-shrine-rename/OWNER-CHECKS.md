# TS-time-shrine-rename — owner checks

Run on the installed Time Shrine build (a new app, so set up from scratch). Record each result with the date and the **installed APK hash**. Not installed yet; no results. Also run [TS-config-values CV-O1–CV-O4](../TS-config-values/OWNER-CHECKS.md) on the same build.

Before installing: uninstall the old Toki Shrine yourself, so two apps don't both use Accessibility.

| ID | What to test | Fail | Pass |
|---|---|---|---|
| TR-O1 | Launcher and app info | Old name, missing icon, or Toki Shrine still installed alongside | "Time Shrine" with the shrine icon; only one copy installed |
| TR-O2 | Onboarding and permissions | Any "Toki" text; the Accessibility list shows the old name or description | Copy says Time Shrine; Accessibility lists "Time Shrine" with its description; the battery steps name Time Shrine |
| TR-O3 | Create a block and hit it | Crash; anything other than the name differs from before | Block, gate, challenge, pause bubble and notification all work as before |
| TR-O4 | Settings and Stats | "About Toki Shrine"; "before Toki" in the time-saved sheet | "About Time Shrine"; "…your 7 days before Time Shrine…" |
