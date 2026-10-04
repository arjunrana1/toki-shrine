package com.arjunrana.tokishrine.data.stats

import java.time.Instant
import java.time.ZoneId

/** Platform-neutral event types; never count deprecated foreground aliases twice. */
object UsageKind {
    const val RESUME = 1
    const val PAUSE = 2
    const val STOP = 3 // lock, screen off, shutdown/startup: never bridge a visit
}

data class UsageSegment(val start: Long, val end: Long, val zoneId: String)
data class UsageVisit(val packageName: String, val start: Long, val end: Long, val segments: List<UsageSegment>) {
    val id: String get() = "$packageName:$start"
    val durationMs: Long get() = segments.sumOf { it.end - it.start }
}

object UsageVisits {
    /**
     * Single foreground owner prevents double-counting multi-window time. Same-package
     * activity transitions merge only within one second; same-activity reopen is a new
     * visit. Unclosed foreground tails are omitted, never extrapolated across silence.
     */
    fun parse(events: List<StatsUsageEvent>, until: Long? = null): List<UsageVisit> {
        val visits = mutableListOf<UsageVisit>()
        var active: StatsUsageEvent? = null
        var lastActivity: String? = null
        var canMerge = false
        fun close(at: Long) {
            val start = active ?: return
            if (at >= start.at) {
                val segment = UsageSegment(start.at, at, start.zoneId)
                val previous = visits.lastOrNull()
                if (canMerge && previous != null && previous.packageName == start.packageName &&
                    start.at - previous.end in 0..1_000 && lastActivity != start.activity
                ) {
                    visits[visits.lastIndex] = previous.copy(end = at, segments = previous.segments + segment)
                } else {
                    visits += UsageVisit(start.packageName, start.at, at, listOf(segment))
                }
                lastActivity = start.activity
                canMerge = true
            }
            active = null
        }
        for (e in events) {
            when (e.kind) {
                UsageKind.RESUME -> {
                    if (active?.packageName == e.packageName && active?.activity == e.activity) continue
                    close(e.at)
                    if (visits.lastOrNull()?.packageName != e.packageName) canMerge = false
                    active = e
                }
                UsageKind.PAUSE -> if (active?.packageName == e.packageName && active?.activity == e.activity) close(e.at)
                UsageKind.STOP -> { close(e.at); canMerge = false }
            }
        }
        if (until != null) close(until)
        else {
            // An activity transition into a still-open tail is not a completed visit.
            val tail = active
            val previous = visits.lastOrNull()
            if (tail != null && previous != null && canMerge && tail.packageName == previous.packageName &&
                tail.at - previous.end in 0..1_000 && tail.activity != lastActivity) visits.removeAt(visits.lastIndex)
        }
        return visits
    }

    fun covered(start: Long, end: Long, coverage: List<StatsCoverage>): Boolean =
        end >= start && coverage.any { it.start <= start && it.end >= end }

    fun mergeCoverage(values: List<StatsCoverage>): List<StatsCoverage> {
        val result = mutableListOf<StatsCoverage>()
        values.sortedBy { it.start }.forEach { interval ->
            val last = result.lastOrNull()
            if (last != null && interval.start <= last.end) result[result.lastIndex] = last.copy(end = maxOf(last.end, interval.end))
            else result += interval
        }
        return result
    }

    fun dailyMs(visits: List<UsageVisit>): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        for (visit in visits) for (segment in visit.segments) {
            var cursor = segment.start
            val zone = ZoneId.of(segment.zoneId)
            while (cursor < segment.end) {
                val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
                val end = minOf(segment.end, date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
                result[date.toString()] = (result[date.toString()] ?: 0L) + end - cursor
                cursor = end
            }
        }
        return result
    }

    /** A late/unrelated open is not proof of the challenge's return. Fail closed. */
    fun firstPostChallenge(outcome: StatsOutcome, visits: List<UsageVisit>, ownPackage: String): UsageVisit? {
        val first = visits.firstOrNull { it.start >= outcome.at && it.packageName != ownPackage } ?: return null
        return first.takeIf { it.packageName == outcome.packageName && it.start - outcome.at <= 30_000L }
    }
}
