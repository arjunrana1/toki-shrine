package com.arjunrana.tokishrine.data.stats

import com.arjunrana.tokishrine.config.AppConfig
import java.time.LocalDate
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

/**
 * One leaderboard row. [usualVisitMs] is the effective value (the user's
 * override when set, else the measured baseline); [measuredVisitMs] and
 * [usesFallback] describe the measured baseline only ("Use measured (Xm)").
 * With an override, [savedMs] = [nopes] × override for the week shown.
 */
data class StatsApp(
    val packageName: String,
    val nopes: Int,
    val savedMs: Long,
    val usualVisitMs: Long,
    val usesFallback: Boolean,
    val measuredVisitMs: Long = usualVisitMs,
    val userVisitMs: Long? = null,
) {
    /** The sheet's live "N nopes × Xm = saved this week" line; equals [savedMs] after saving [visitMs]. */
    fun savedWith(visitMs: Long): Long = nopes * visitMs
}

/**
 * Week = the seven local dates ending today; today = since local midnight.
 * Nope rate as "N of M" is [nopes] of [attempts]. Rates are 0 when there are
 * no attempts (render "—").
 */
data class StatsDashboard(
    val days: List<StatsDay>,
    val savedMs: Long,
    /** This week's average time saved per day: 7-day saved ÷ 7. */
    val savedPerDayMs: Long,
    val attempts: Int,
    val nopes: Int,
    val passes: Int,
    val nopeRatePercent: Int,
    val todaySavedMs: Long,
    /** Counted nopes + push-throughs today: one counted nope adds exactly 1. */
    val todayAttempts: Int,
    val todayNopes: Int,
    val todayNopeRatePercent: Int,
    /** Observed screen time since local midnight; null when not fully covered or unavailable. */
    val todayScreenMs: Long?,
    val apps: List<StatsApp>,
    val spentMs: Long?,
    val unmeasuredPasses: Int,
    val baselineCapturedAt: Long?,
    val usageAvailable: Boolean,
    /** False shows the "No blocks are on" note over a dashboard kept for this week's outcomes (P7-F10). */
    val blocksOn: Boolean = true,
)

object StatsDashboardCalculator {
    /**
     * [overrides] re-value every counted nope of their package in the window
     * (8 October §17 addendum); other outcomes keep their frozen contribution.
     */
    fun calculate(today: LocalDate, outcomes: List<StatsOutcome>, apps: Set<String>, baselines: List<StatsBaseline>,
                  state: StatsState?, usageAvailable: Boolean,
                  overrides: List<StatsVisitOverride> = emptyList(), todayScreenMs: Long? = null,
                  blocksOn: Boolean = true): StatsDashboard {
        val dates = (6L downTo 0L).map(today::minusDays)
        val userVisit = overrides.associate { it.packageName to it.visitMs }
        fun saved(outcome: StatsOutcome): Long =
            if (outcome.kind == "nope" && outcome.counted) userVisit[outcome.packageName] ?: outcome.savedMs else outcome.savedMs
        val week = outcomes.filter { it.localDate in dates.map(LocalDate::toString) && it.counted }
        val nopes = week.filter { it.kind == "nope" }
        val passes = week.filter { it.kind == "pass" }
        val saved = nopes.sumOf(::saved)
        val missing = passes.count { it.spentMs == null }
        val days = dates.map { date ->
            val rows = week.filter { it.localDate == date.toString() }
            val dayPasses = rows.filter { it.kind == "pass" }
            val unknown = dayPasses.count { it.spentMs == null }
            StatsDay(date, rows.sumOf(::saved), if (unknown > 0) null else dayPasses.sumOf { it.spentMs ?: 0 }, unknown)
        }
        val todayRows = week.filter { it.localDate == today.toString() }
        val todayNopes = todayRows.filter { it.kind == "nope" }
        return StatsDashboard(
            days, saved, saved / 7, week.size, nopes.size, passes.size, rate(nopes.size, week.size),
            todayNopes.sumOf(::saved), todayRows.size, todayNopes.size, rate(todayNopes.size, todayRows.size), todayScreenMs,
            apps.map { pkg ->
                val rows = nopes.filter { it.packageName == pkg }
                val baseline = baselines.firstOrNull { it.packageName == pkg }
                val measured = baseline?.visitMs ?: StatsLedger.FALLBACK_MS
                StatsApp(pkg, rows.size, rows.sumOf(::saved), userVisit[pkg] ?: measured,
                    baseline == null || baseline.samples < AppConfig.MIN_VISITS, measured, userVisit[pkg])
            }.sortedWith(compareByDescending<StatsApp> { it.savedMs }.thenBy { it.packageName }),
            if (missing > 0) null else passes.sumOf { it.spentMs ?: 0 }, missing,
            state?.capturedAt, usageAvailable, blocksOn,
        )
    }

    private fun rate(nopes: Int, attempts: Int): Int = if (attempts == 0) 0 else (nopes * 100.0 / attempts).roundToInt()
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
