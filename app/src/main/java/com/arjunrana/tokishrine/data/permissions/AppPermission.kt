package com.arjunrana.tokishrine.data.permissions

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.arjunrana.tokishrine.TokiAccessibilityService

/*
 * The four onboarding permissions (PRD §12). Declaration order is the
 * checklist display order, and row copy plus the Essential/Better
 * experience grouping follow the owner-supplied `Accessibility screen
 * design v2.png` (P3-F02, 19 September) — that reference supersedes the
 * older §12 stated-purpose wording, which the overlay row no longer
 * matches: it now names the pause screen as well as the timer bubble.
 * eventValue strings are §10 event identity and must never change.
 */
enum class AppPermission(
    val eventValue: String,
    val essential: Boolean,
    val rowTitle: String,
    val rowDescription: String,
) {
    ACCESSIBILITY(
        eventValue = "accessibility",
        essential = true,
        rowTitle = "Accessibility",
        rowDescription = "Tells us which app or site is open",
    ),
    BATTERY(
        eventValue = "battery",
        essential = true,
        rowTitle = "Battery exemption",
        rowDescription = "Stops the phone shutting us down in the background",
    ),
    OVERLAY(
        eventValue = "overlay",
        essential = false,
        rowTitle = "Display over other apps",
        rowDescription = "Shows the pause screen and the timer bubble",
    ),
    NOTIFICATIONS(
        eventValue = "notifications",
        essential = false,
        rowTitle = "Notifications",
        rowDescription = "Shows the pause countdown",
    ),
}

object Permissions {

    fun isGranted(context: Context, permission: AppPermission): Boolean = when (permission) {
        AppPermission.ACCESSIBILITY -> isAccessibilityServiceEnabled(context)
        AppPermission.OVERLAY -> Settings.canDrawOverlays(context)
        AppPermission.BATTERY -> isBatteryExempt(context)
        AppPermission.NOTIFICATIONS -> isNotificationsGranted(context)
    }

    fun snapshot(context: Context): Map<AppPermission, Boolean> =
        AppPermission.values().associateWith { isGranted(context, it) }

    fun grantedCount(context: Context): Int =
        AppPermission.values().count { isGranted(context, it) }

    // Compares our flattened ComponentName against the colon-separated
    // enabled_accessibility_services list — the same source the system
    // settings screen writes.
    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, TokiAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        if (enabled.isEmpty()) return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        for (entry in splitter) {
            val component = ComponentName.unflattenFromString(entry)
            if (component != null && component == expected) return true
        }
        return false
    }

    private fun isBatteryExempt(context: Context): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun isNotificationsGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
}
