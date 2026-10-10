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
 * Pure display mapping for the redesigned Stats screen (PRD §9 + §17 8 October
 * addendum: S1 v2 layout, visit chips, visit-length sheet). No Android
 * dependencies, so the presentation rules the §10 stats_viewed/state, the
 * chip/sheet copy and the mock's figures depend on are pinned on the JVM;
 * metric formulas stay in the Stats data layer and the screen only renders
 * what these helpers return.
 */

// One blocked-app row: label resolved by the caller (InstalledAppsRepository),
// visit-chip text from the effective (override-or-measured) visit length, and
// the sheet's measured/user inputs carried alongside (P7-F6).
data class StatsAppRow(
    val packageName: String,
    val label: String,
    val nopes: Int,
    val savedMs: Long,
    /** "~6m/visit" for every app — user-set values are not marked (9 October §17 addendum). */
    val visitChip: String,
    val usesFallback: Boolean,
    val measuredVisitMs: Long,
    val userVisitMs: Long?,
)

// Equal saved durations sort by display label (stats contract) — the data
// layer's package-name tiebreak is replaced here, per §9's display-name rule.
fun statsAppRows(apps: List<StatsApp>, labelFor: (String) -> String): List<StatsAppRow> =
    apps.map { app ->
        StatsAppRow(
            packageName = app.packageName,
            label = labelFor(app.packageName),
            nopes = app.nopes,
            savedMs = app.savedMs,
            visitChip = visitChipLabel(app.usualVisitMs),
            usesFallback = app.usesFallback,
            measuredVisitMs = app.measuredVisitMs,
            userVisitMs = app.userVisitMs,
        )
    }.sortedWith(compareByDescending<StatsAppRow> { it.savedMs }.thenBy { it.label })

// PRD §17 (9 October): rows read "N nopes this week ·" then the chip.
fun appRowSubtitle(row: StatsAppRow): String = "${row.nopes} nopes this week ·"

// Every chip reads "~Xm/visit" with the pencil — a user-set value is not
// marked (9 October §17 addendum; the S7 "· yours" distinction is gone).
// [effectiveVisitMs] is the data layer's usualVisitMs: the override when
// set, else the measured baseline.
fun visitChipLabel(effectiveVisitMs: Long): String =
    "~${StatsDurationFormat.visitMinutes(effectiveVisitMs)}m/visit"

// The sheet opens at the effective value: the user's override when set, else
// the measured baseline (visitMinutes' 1-minute floor keeps a display value).
fun initialSheetMinutes(row: StatsAppRow): Int =
    StatsDurationFormat.visitMinutes(row.userVisitMs ?: row.measuredVisitMs).toInt()

// "We measured 7m." — or the honest assumption when the baseline is the
// fallback (P7-F6: fewer than three usable visits → 10 m).
fun visitSheetMeasuredLabel(usesFallback: Boolean, measuredVisitMs: Long): String =
    if (usesFallback) {
        "We assume ${StatsDurationFormat.visitMinutes(measuredVisitMs)}m."
    } else {
        "We measured ${StatsDurationFormat.visitMinutes(measuredVisitMs)}m."
    }

// The S8 live line: "8 nopes × 12m" on the left, "1h 36m saved this week" on
// the right, re-valued through StatsApp.savedWith as the stepper moves.
fun visitSheetNopesLine(nopes: Int, minutes: Int): String =
    "$nopes ${if (nopes == 1) "nope" else "nopes"} × ${minutes}m"

fun visitSheetSavedLine(savedMs: Long): String = "${StatsDurationFormat.duration(savedMs)} saved this week"

// THIS WEEK card right-hand value: "N%" only — the 9 October §17 addendum
// drops "N of M" — with an em dash when the week has no resolved attempts
// (§9: never a fabricated percentage).
fun weekNopeRateLine(nopeRatePercent: Int, attempts: Int): String =
    if (attempts == 0) "—" else "$nopeRatePercent%"

// The "Nope rate today ⓘ" sheet explains the tile it opens from, so it reads
// today's counts, not the week's (owner, 8 October delta-10 review P7-N12).
// Push-throughs today = today's attempts − today's counted nopes.
data class NopeRateSheetFigures(val nopes: Int, val passes: Int, val attempts: Int, val ratePercent: Int)

fun todayNopeRateSheetFigures(dashboard: StatsDashboard): NopeRateSheetFigures =
    NopeRateSheetFigures(
        nopes = dashboard.todayNopes,
        passes = dashboard.todayAttempts - dashboard.todayNopes,
        attempts = dashboard.todayAttempts,
        ratePercent = dashboard.todayNopeRatePercent,
    )

// Hero "Screen time X today" line: hidden entirely when today's screen time
// is unknown (P7-F13; "vs your usual" is gone). Null-check + format.
fun todayScreenTimeLine(todayScreenMs: Long?): String? =
    todayScreenMs?.let { "Screen time ${StatsDurationFormat.duration(it)} today" }

// Rolling weekday letters under the charts, Today last (PRD §9). Narrow
// style gives the mock's single letters (M, T, W …). Fixed English locale:
// every other label on the screen is hardcoded English copy, so device
// locale must not mix languages in the charts.
fun weekdayLabels(days: List<LocalDate>, locale: Locale = Locale.ENGLISH): List<String> =
    days.mapIndexed { index, date ->
        if (index == days.lastIndex) "Today"
        else date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
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
