# TS-launch-gate-tests — owner checks

## LG-7 re-run — pending authorization

Not run yet. It needs Arjun's fresh authorization for the isolated emulator (AVD `toki-p7d-isolated`) only. When authorized, the run will record the environment, source commit, both APK hashes, the full `am instrument -w -r` command, raw output under `checks/`, pass/fail per class, and limits, in the same form as the [23 October run](../TS-unlock-minutes/OWNER-CHECKS.md#launch-gate-android-test-run--23-october-2026-isolated-emulator-agent-executed).

Expected identity: debug APK `9d505bd8…` (unchanged app) and androidTest APK `def129a8…` (D1), unless later repairs change the tests.
