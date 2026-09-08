package com.arjunrana.tokishrine.data.apps

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

// Launchable apps for the picker. Package visibility is scoped by the
// manifest <queries> block (MAIN/LAUNCHER), so QUERY_ALL_PACKAGES is not
// needed. Toki Shrine excludes itself: blocking the only screen that can
// undo a block is not allowed (PRD §4).
class InstalledAppsRepository(private val context: Context) {

    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { it.packageName != context.packageName }
            .map { info ->
                AppEntry(
                    packageName = info.packageName,
                    label = info.loadLabel(pm).toString(),
                    icon = runCatching { pm.getApplicationIcon(info.packageName) }.getOrNull(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    // Label for a package that may or may not still be installed; falls back
    // to the package name so stored blocks stay readable after uninstalls.
    fun labelFor(packageName: String): String {
        val pm = context.packageManager
        return runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }
}
