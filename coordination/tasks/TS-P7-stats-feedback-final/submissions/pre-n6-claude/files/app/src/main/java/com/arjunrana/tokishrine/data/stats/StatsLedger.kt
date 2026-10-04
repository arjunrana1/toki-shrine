package com.arjunrana.tokishrine.data.stats

import java.time.Instant
import java.time.ZoneId

/** Called only inside the owning challenge Room transaction. No OS reads here. */
class StatsLedger(private val dao: StatsDao) {
    suspend fun record(sessionId: String, pkg: String?, targetType: String?, pass: Boolean, at: Long, zone: ZoneId) {
        if (targetType != "app" || pkg.isNullOrBlank() || dao.outcome(sessionId) != null) return
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
        const val DEDUP_MS = 300_000L
        fun shouldCountNope(last: StatsOutcome?, at: Long): Boolean =
            last == null || last.kind == "pass" || at < last.at || at - last.at >= DEDUP_MS
    }
}
