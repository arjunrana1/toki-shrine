package com.arjunrana.tokishrine.data.stats

import kotlin.math.roundToInt

// Pure display formatting for the §6 screen 23 figures (mirrors PauseFormat's
// role for the pause surfaces): no Android dependencies, so the boundaries are
// pinned on the JVM. The formulas themselves live in StatsCalculator.
object StatsFormat {

    // The mock shows "63%" for 0.63: whole-percent display of the §9 rate.
    // roundToInt is floor(x + 0.5): half rounds up, so a 1/3 rate reads as
    // 33% and 2/3 as 67%.
    fun walkAwayRatePercent(rate: Double): String =
        "${(rate * 100).roundToInt()}%"

    // "since you started, 21 days ago" under the hero figure. Launch day
    // itself has no "ago" (PRD §9: days active starts at zero on launch day).
    fun sinceStartedLine(daysActive: Int): String = when {
        daysActive <= 0 -> "since you started, today"
        daysActive == 1 -> "since you started, 1 day ago"
        else -> "since you started, $daysActive days ago"
    }
}
