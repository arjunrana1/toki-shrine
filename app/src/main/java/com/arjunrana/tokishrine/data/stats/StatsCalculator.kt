package com.arjunrana.tokishrine.data.stats

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Pure implementations of the Stats definitions in PRD §9. No Android
// dependencies, so the formulas are unit-testable on the JVM; the DAOs only
// supply raw counts.
object StatsCalculator {

    // walk_aways ÷ (walk_aways + completed_challenges); 0.0 with no data yet
    // rather than NaN. The retired challenge_abandoned rows (historical only)
    // and turnoff_completed deliberately stay out of the denominator (PRD §8:
    // abandonment is not a walk-away).
    fun walkAwayRate(walkAways: Int, completedChallenges: Int): Double {
        val denominator = walkAways + completedChallenges
        if (denominator == 0) return 0.0
        return walkAways.toDouble() / denominator
    }

    // "This week — walk-aways in the last 7 calendar days": the window opens
    // at the start of the day six days ago, local time, so today plus the six
    // before it are the seven calendar days.
    fun calendarWeekStart(nowMs: Long, zone: ZoneId): Long =
        localDate(nowMs, zone).minusDays(6)
            .atStartOfDay(zone).toInstant().toEpochMilli()

    // "Days active — days since first launch": whole calendar days between the
    // first-launch date and today, local time. Launch day itself is day 0.
    fun daysSince(firstLaunchMs: Long, nowMs: Long, zone: ZoneId): Int =
        ChronoUnit.DAYS.between(localDate(firstLaunchMs, zone), localDate(nowMs, zone))
            .coerceAtLeast(0)
            .toInt()

    // LocalDate.ofInstant is API 34+; minSdk is 33, so go via ZonedDateTime.
    private fun localDate(ms: Long, zone: ZoneId) =
        Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
}
