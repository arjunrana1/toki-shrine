package com.arjunrana.tokishrine.data.stats

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

sealed interface StatsScreenState {
    data object Loading : StatsScreenState
    data object NoUsageAccess : StatsScreenState
    data object NoBlocks : StatsScreenState
    data class Ready(val dashboard: StatsDashboard) : StatsScreenState
    data class Error(val message: String) : StatsScreenState
}

data class StatsDay(val date: LocalDate, val savedMs: Long, val spentMs: Long?, val unmeasuredPasses: Int)
data class StatsApp(val packageName: String, val nopes: Int, val savedMs: Long, val usualVisitMs: Long, val usesFallback: Boolean)
data class StatsDashboard(
    val days: List<StatsDay>,
    val savedMs: Long,
    val savedPerDayMs: Long,
    val attempts: Int,
    val attemptsPerDay: Double,
    val nopes: Int,
    val passes: Int,
    val nopeRatePercent: Int,
    val apps: List<StatsApp>,
    val spentMs: Long?,
    val unmeasuredPasses: Int,
    val screenDailyMs: Long?,
    val screenChangePercent: Int?,
    val baselineCapturedAt: Long?,
    val usageAvailable: Boolean,
)

object StatsDashboardCalculator {
    fun calculate(today: LocalDate, outcomes: List<StatsOutcome>, apps: Set<String>, baselines: List<StatsBaseline>,
                  state: StatsState?, screenDailyMs: Long?, usageAvailable: Boolean): StatsDashboard {
        val dates = (6L downTo 0L).map(today::minusDays)
        val week = outcomes.filter { it.localDate in dates.map(LocalDate::toString) && it.counted }
        val nopes = week.filter { it.kind == "nope" }
        val passes = week.filter { it.kind == "pass" }
        val saved = nopes.sumOf { it.savedMs }
        val missing = passes.count { it.spentMs == null }
        val days = dates.map { date ->
            val rows = week.filter { it.localDate == date.toString() }
            val dayPasses = rows.filter { it.kind == "pass" }
            val unknown = dayPasses.count { it.spentMs == null }
            StatsDay(date, rows.sumOf { it.savedMs }, if (unknown > 0) null else dayPasses.sumOf { it.spentMs ?: 0 }, unknown)
        }
        return StatsDashboard(
            days, saved, saved / 7, week.size, week.size / 7.0, nopes.size, passes.size,
            if (week.isEmpty()) 0 else (nopes.size * 100.0 / week.size).roundToInt(),
            apps.map { pkg ->
                val rows = nopes.filter { it.packageName == pkg }
                val baseline = baselines.firstOrNull { it.packageName == pkg }
                StatsApp(pkg, rows.size, rows.sumOf { it.savedMs }, baseline?.visitMs ?: StatsLedger.FALLBACK_MS,
                    baseline == null || baseline.samples < 3)
            }.sortedWith(compareByDescending<StatsApp> { it.savedMs }.thenBy { it.packageName }),
            if (missing > 0) null else passes.sumOf { it.spentMs ?: 0 }, missing, screenDailyMs,
            state?.screenDailyMs?.takeIf { it > 0 }?.let { usual ->
                screenDailyMs?.let { val delta = (it - usual) * 100.0 / usual; if (abs(delta) < 1.0) 0 else delta.roundToInt() }
            }, state?.capturedAt, usageAvailable,
        )
    }
}

/** Round only after summing; baseline display flooring never changes saved arithmetic. */
object StatsDurationFormat {
    fun duration(ms: Long): String {
        val minutes = (ms.coerceAtLeast(0) / 60_000.0).roundToLong()
        if (minutes < 100) return "${minutes}m"
        val hours = minutes / 60
        val remainder = minutes % 60
        return if (remainder == 0L) "${hours}h" else "${hours}h ${remainder}m"
    }
    fun visitMinutes(ms: Long): Long = (ms / 60_000.0).roundToLong().coerceAtLeast(1)
}
