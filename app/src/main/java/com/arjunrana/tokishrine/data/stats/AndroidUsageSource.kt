package com.arjunrana.tokishrine.data.stats

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.os.UserManager
import java.time.ZoneId

interface UsageSource {
    fun hasAccess(): Boolean
    /** Null is unavailable (including locked device), never a measured zero. */
    fun read(start: Long, end: Long): List<StatsUsageEvent>?
}

class AndroidUsageSource(private val context: Context) : UsageSource {
    override fun hasAccess(): Boolean = context.getSystemService(AppOpsManager::class.java)
        .unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED

    override fun read(start: Long, end: Long): List<StatsUsageEvent>? {
        if (!hasAccess() || !context.getSystemService(UserManager::class.java).isUserUnlocked) return null
        val stream = context.getSystemService(UsageStatsManager::class.java).queryEvents(start, end) ?: return null
        val zone = ZoneId.systemDefault().id
        val result = mutableListOf<StatsUsageEvent>()
        val event = UsageEvents.Event()
        while (stream.hasNextEvent()) {
            stream.getNextEvent(event)
            val kind = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageKind.RESUME
                UsageEvents.Event.ACTIVITY_PAUSED -> UsageKind.PAUSE
                UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.KEYGUARD_SHOWN,
                UsageEvents.Event.DEVICE_SHUTDOWN, UsageEvents.Event.DEVICE_STARTUP -> UsageKind.STOP
                else -> continue
            }
            val pkg = event.packageName.orEmpty()
            val activity = event.className.orEmpty()
            // Include original type: two different STOP events may share a timestamp.
            val key = "${event.timeStamp}:${event.eventType}:$pkg:$activity"
            result += StatsUsageEvent(key, event.timeStamp, pkg, activity, kind, zone)
        }
        return result
    }
}
