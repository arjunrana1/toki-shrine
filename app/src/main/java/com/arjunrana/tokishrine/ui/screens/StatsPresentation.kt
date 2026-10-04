package com.arjunrana.tokishrine.ui.screens

import com.arjunrana.tokishrine.data.stats.StatsApp
import com.arjunrana.tokishrine.data.stats.StatsDashboard
import com.arjunrana.tokishrine.data.stats.StatsDurationFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/*
 * Pure display mapping for the redesigned Stats screen (PRD §9). No Android
 * dependencies, so the presentation rules the §10 stats_viewed/state and the
 * mock's figures depend on are pinned on the JVM; metric formulas stay in the
 * Stats data layer and the screen only renders what these helpers return.
 */

// One blocked-app row: label resolved by the caller (InstalledAppsRepository),
// per-visit label from the frozen baseline (minimum 1m display rule).
data class StatsAppRow(
    val packageName: String,
    val label: String,
    val nopes: Int,
    val visitLabel: String,
    val savedMs: Long,
)

// Equal saved durations sort by display label (stats contract) — the data
// layer's package-name tiebreak is replaced here, per §9's display-name rule.
fun statsAppRows(apps: List<StatsApp>, labelFor: (String) -> String): List<StatsAppRow> =
    apps.map { app ->
        StatsAppRow(
            packageName = app.packageName,
            label = labelFor(app.packageName),
            nopes = app.nopes,
            visitLabel = "~${StatsDurationFormat.visitMinutes(app.usualVisitMs)}m per visit",
            savedMs = app.savedMs,
        )
    }.sortedWith(compareByDescending<StatsAppRow> { it.savedMs }.thenBy { it.label })

fun appRowSubtitle(row: StatsAppRow): String = "${row.nopes} nopes this week · ${row.visitLabel}"

// Rolling weekday letters under the charts, Today last (PRD §9). Narrow
// style gives the mock's single letters (M, T, W …). Fixed English locale:
// every other label on the screen is hardcoded English copy, so device
// locale must not mix languages in the charts.
fun weekdayLabels(days: List<LocalDate>, locale: Locale = Locale.ENGLISH): List<String> =
    days.mapIndexed { index, date ->
        if (index == days.lastIndex) "Today"
        else date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
    }

// Hero screen-time line: hidden entirely without an available average, and
// the comparison is hidden for an unavailable/zero-baseline delta or a
// change below 1% (the calculator reports that as 0). Neutral tone both ways.
fun screenTimeLine(screenDailyMs: Long?, screenChangePercent: Int?): String? {
    val average = screenDailyMs?.let { StatsDurationFormat.duration(it) } ?: return null
    val comparison = when {
        screenChangePercent == null || screenChangePercent == 0 -> ""
        screenChangePercent > 0 -> " · ↑ $screenChangePercent% vs your usual"
        else -> " · ↓ ${-screenChangePercent}% vs your usual"
    }
    return "Screen time $average/day$comparison"
}

// stats_viewed `state` param (PRD §10): "partial" while the seven-day window
// still includes days before measurement began (the S4 case), "default" once
// the whole week is covered. A missing baseline means the window cannot be
// covered, so it also reads partial.
fun statsViewedState(dashboard: StatsDashboard, weekStartUtcMs: Long): String =
    if (dashboard.baselineCapturedAt == null || dashboard.baselineCapturedAt >= weekStartUtcMs) {
        "partial"
    } else {
        "default"
    }

// One chart bar: value label is always whole minutes ("140m", never the §9
// "2h 12m" form, which clips in the ~35dp bar column — 4 October §17
// addendum) or an em dash for unknown spent; height in dp is a 3dp stub
// for zero/unknown, otherwise proportional up to max with a 4dp floor
// (mock rules); `filled` marks the drawn bar.
data class StatsBarSpec(
    val valueLabel: String,
    val heightDp: Int,
    val filled: Boolean,
)

fun statsBarSpecs(
    valuesMs: List<Long?>,
    labelUnknown: String = "—",
    maxBarHeightDp: Int = 74,
): List<StatsBarSpec> {
    val max = valuesMs.filterNotNull().maxOrNull() ?: 0L
    return valuesMs.map { value ->
        when {
            value == null -> StatsBarSpec(labelUnknown, 3, filled = false)
            value == 0L || max == 0L -> StatsBarSpec(barValueLabel(value), 3, filled = false)
            else -> StatsBarSpec(
                valueLabel = barValueLabel(value),
                heightDp = ((value.toDouble() / max) * maxBarHeightDp).roundToInt()
                    .coerceIn(4, maxBarHeightDp),
                filled = true,
            )
        }
    }
}

// Same nearest-minute rounding as StatsDurationFormat.duration, but the
// label never switches to hours+minutes: the bar column cannot fit it.
private fun barValueLabel(ms: Long): String = "${(ms.coerceAtLeast(0) / 60_000.0).roundToLong()}m"
