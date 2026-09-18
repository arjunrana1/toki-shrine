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
}
