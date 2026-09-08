package com.arjunrana.tokishrine.data.repo

import com.arjunrana.tokishrine.data.db.AppMetaDao
import com.arjunrana.tokishrine.data.db.EventDao
import com.arjunrana.tokishrine.data.db.TargetCount
import com.arjunrana.tokishrine.data.entity.AppMeta
import com.arjunrana.tokishrine.data.entity.Event
import com.arjunrana.tokishrine.data.stats.StatsCalculator
import java.time.ZoneId
import org.json.JSONObject

data class StatsSnapshot(
    val totalWalkAways: Int,
    val daysActive: Int,
    val thisWeek: Int,
    val bestDay: Int,
    val walkAwayRate: Double,
    val mostWalkedAwayFrom: List<TargetCount>,
)

class EventRepository(
    private val eventDao: EventDao,
    private val metaDao: AppMetaDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend fun log(
        name: String,
        blockId: Long? = null,
        target: String? = null,
        params: Map<String, Any?> = emptyMap(),
    ) {
        ensureFirstLaunchRecorded()
        val paramsJson = if (params.isEmpty()) null else JSONObject(params).toString()
        eventDao.insert(
            Event(
                name = name,
                timestampUtc = clock(),
                blockId = blockId,
                target = target,
                paramsJson = paramsJson,
            ),
        )
    }

    // All six PRD §9 figures in one read. nowMs/zone are parameters so the
    // formulas are pinned down exactly in tests; production uses defaults.
    suspend fun getStats(nowMs: Long = clock(), zone: ZoneId = ZoneId.systemDefault()): StatsSnapshot {
        ensureFirstLaunchRecorded()
        val totalWalkAways = eventDao.countByName(EVENT_WALK_AWAY)
        val completed = eventDao.countByName(EVENT_CHALLENGE_COMPLETED)
        val firstLaunchAt = metaDao.get(AppMeta.KEY_FIRST_LAUNCH_AT)?.toLongOrNull()
        return StatsSnapshot(
            totalWalkAways = totalWalkAways,
            daysActive = firstLaunchAt?.let { StatsCalculator.daysSince(it, nowMs, zone) } ?: 0,
            thisWeek = eventDao.countByNameSince(
                EVENT_WALK_AWAY,
                StatsCalculator.calendarWeekStart(nowMs, zone),
            ),
            bestDay = eventDao.maxCountPerLocalDay(EVENT_WALK_AWAY) ?: 0,
            walkAwayRate = StatsCalculator.walkAwayRate(totalWalkAways, completed),
            mostWalkedAwayFrom = eventDao.countsByTarget(EVENT_WALK_AWAY),
        )
    }

    suspend fun countWalkAwaysSince(sinceUtc: Long): Int =
        eventDao.countByNameSince(EVENT_WALK_AWAY, sinceUtc)

    suspend fun countAllEvents(): Int = eventDao.countAll()

    private suspend fun ensureFirstLaunchRecorded() {
        metaDao.putIfAbsent(AppMeta(AppMeta.KEY_FIRST_LAUNCH_AT, clock().toString()))
    }

    companion object {
        // Canonical event names (PRD §10). Fixed here so the store, Stats and
        // later phases can't drift apart.
        const val EVENT_WALK_AWAY = "walk_away"
        const val EVENT_CHALLENGE_COMPLETED = "challenge_completed"
        const val EVENT_CHALLENGE_ABANDONED = "challenge_abandoned"
        const val EVENT_BLOCK_SCREEN_SHOWN = "block_screen_shown"
        const val EVENT_TURNOFF_COMPLETED = "turnoff_completed"
        const val EVENT_STATS_VIEWED = "stats_viewed"
    }
}
