package com.arjunrana.tokishrine

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// P3-F01 (owner validation, 19 September): without the special-permission
// declaration the app is simply absent from the system "Display over other
// apps" list, so the overlay checklist row can never reach a real granted
// state. Guards the source manifest; Gradle runs JVM tests with the module
// directory as the working directory.
class AndroidManifestTest {

    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun overlayPermissionIsDeclared() {
        assertTrue(
            "AndroidManifest.xml must declare android.permission.SYSTEM_ALERT_WINDOW",
            manifest.contains("android:name=\"android.permission.SYSTEM_ALERT_WINDOW\""),
        )
    }

    // Phase 6: the pause countdown runs in a specialUse foreground service.
    // Without the permissions the service cannot start on API 34+, and an
    // exported pause service would let any app drive it.
    @Test
    fun foregroundServicePermissionsAreDeclared() {
        assertTrue(
            "AndroidManifest.xml must declare android.permission.FOREGROUND_SERVICE",
            manifest.contains("android:name=\"android.permission.FOREGROUND_SERVICE\""),
        )
        assertTrue(
            "AndroidManifest.xml must declare android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
            manifest.contains("android:name=\"android.permission.FOREGROUND_SERVICE_SPECIAL_USE\""),
        )
    }

    @Test
    fun pauseServiceIsUnexportedSpecialUse() {
        val service = manifest.substringAfter("<service\n            android:name=\".pause.PauseService\"")
        assertTrue(
            "PauseService must be declared",
            service != manifest,
        )
        assertTrue(
            "PauseService must not be exported",
            service.contains("android:exported=\"false\""),
        )
        assertTrue(
            "PauseService must declare the specialUse foreground type",
            service.contains("android:foregroundServiceType=\"specialUse\""),
        )
        assertTrue(
            "PauseService must state its special-use subtype",
            manifest.contains("android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"),
        )
    }
}
