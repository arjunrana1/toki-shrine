# Build and verification reference

Read when building, changing test infrastructure or performing explicitly requested phone setup. No device operations are authorized merely by this reference. See [WORKFLOW](../../coordination/WORKFLOW.md) for ownership and stopping rules.

## Non-device toolchain and checks

Use `./build.sh` (sets JDK/SDK environment), not a regenerated project. Existing setup: Gradle 8.7, AGP 8.4.1, Kotlin 1.9.24, Compose compiler 1.5.14, KSP 1.9.24-1.0.20, Room 2.6.1; inspect pinned build files before any change. compile/target SDK 36 uses the existing unsupported-compile-SDK suppression. Android Studio is not required. No toolchain or new dependency changes without owner approval, including test dependencies.

Typical implementation checks, narrowed to task risk:

```sh
./build.sh assembleDebug
./build.sh testDebugUnitTest
./build.sh assembleDebugAndroidTest
```

The third command only compiles instrumented sources. Existing approved test dependencies include Compose ui-test-junit4 and ui-test-manifest. Use current test APIs/primitives; do not introduce large frameworks to imitate unavailable hardware. Read actual result XML under `app/build/test-results/testDebugUnitTest/` for counts when reporting a new run. Attribute old artifacts with their provenance; never relabel them as fresh evidence. Preserve intentional test assertions.

The legacy Kotlin literal-color check searches `app/src/main/java` for hex colors outside ui/theme; keep its known scope and resource exceptions. Use it when visuals warrant it, not as proof of rendered compliance.

## Human device ownership

Arjun uses a Samsung Galaxy S23 Ultra / Android 16. Neither model runs adb, installations, emulator controls, screenshots, device DB reads or connected/instrumented tests without explicit authorization. Human visual results cannot prove transaction rollback or event payloads; record specific missing evidence separately.

## Only if owner requests installation/setup

Identify approved code/APK and preserve existing data. Install without uninstall/clear; launch and configure only agreed permissions/fixtures. Stop after setup; no exploratory tests. Do not restore over newer data or rely on old /tmp backups (the old September 10 fixture backup was purged). Source `tools/env.sh` only for authorized SDK operations. If the phone is absent or multiple devices appear, report once; no polling or guessing device serials.

Reinstall may disable accessibility; explicit setup should account for service grant/binding, which is asynchronous. Sideload restricted settings may need human unlocking. Another blocker service can interfere with detection. These are diagnostic references, not permission to inspect the phone. Instrumented harness execution historically uninstalled/wiped fixture data; plan data preservation before any separately authorized run.
