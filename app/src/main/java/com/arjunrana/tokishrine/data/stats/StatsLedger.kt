package com.arjunrana.tokishrine.data.stats

import com.arjunrana.tokishrine.config.AppConfig.MIN_VISITS
import com.arjunrana.tokishrine.config.AppConfig.MIN_VISIT_MS
import com.arjunrana.tokishrine.config.AppConfig.VISIT_PERCENTILE
import java.time.Instant
import java.time.ZoneId

/** Called only inside the owning challenge Room transaction. No OS reads here. */
class StatsLedger(private val dao: StatsDao) {
    suspend fun record(
        sessionId: String,
        target: String?,
        targetType: String?,
        hostPackage: String?,
        pass: Boolean,
        at: Long,
        zone: ZoneId,
    ) {
        val pkg = statsPackage(target, targetType, hostPackage)
        if (pkg == null || dao.outcome(sessionId) != null) return
        val counted = pass || shouldCountNope(dao.lastCounted(pkg), at)
        val state = dao.state()
        val baseline = if (!pass && counted) dao.baselines().firstOrNull { it.packageName == pkg } else null
        dao.outcome(StatsOutcome(
            sessionId, pkg, at, Instant.ofEpochMilli(at).atZone(zone).toLocalDate().toString(),
            if (pass) "pass" else "nope", counted,
            if (!pass && counted) baseline?.visitMs ?: FALLBACK_MS else 0L,
            state?.baselineId,
        ))
    }

    companion object {
        const val FALLBACK_MS = 600_000L
        const val DEDUP_MS = 120_000L // owner, 10 October: two minutes (was five)

        // Usual-visit constants (8 October §17 addendum, P7-F6) live in
        // AppConfig (P7-F11); the override bounds stay here.

        // Per-app override bounds in whole minutes (sheet stepper/presets).
        const val OVERRIDE_MINUTES_MIN = 1
        const val OVERRIDE_MINUTES_MAX = 120

        /**
         * Measured usual visit length: visits shorter than [MIN_VISIT_MS] are
         * ignored, then the [VISIT_PERCENTILE]th percentile of the rest by
         * linear interpolation between closest ranks (numpy/Excel
         * PERCENTILE.INC). Fewer than [MIN_VISITS] such visits → [FALLBACK_MS].
         * Returns the value and the number of qualifying visits.
         */
        fun usualVisit(durationsMs: List<Long>): Pair<Long, Int> {
            val valid = durationsMs.filter { it >= MIN_VISIT_MS }.sorted()
            if (valid.size < MIN_VISITS) return FALLBACK_MS to valid.size
            val rank = VISIT_PERCENTILE / 100.0 * (valid.size - 1)
            val low = rank.toInt()
            val high = minOf(low + 1, valid.lastIndex)
            val value = valid[low] + (valid[high] - valid[low]) * (rank - low)
            return Math.round(value) to valid.size
        }
        fun shouldCountNope(last: StatsOutcome?, at: Long): Boolean =
            last == null || last.kind == "pass" || at < last.at || at - last.at >= DEDUP_MS

        /**
         * The package a Stats outcome belongs to: the target of an app
         * interruption, the hosting browser of a site interruption (4 October
         * §17 addendum). A site outcome without a known browser is not recorded.
         */
        fun statsPackage(target: String?, targetType: String?, hostPackage: String?): String? = when (targetType) {
            "app" -> target
            "site" -> hostPackage
            else -> null
        }?.takeIf { it.isNotBlank() }
    }
}
