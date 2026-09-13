package com.arjunrana.tokishrine.data.repo

import androidx.room.withTransaction
import com.arjunrana.tokishrine.data.db.AppMetaDao
import com.arjunrana.tokishrine.data.db.EventDao
import com.arjunrana.tokishrine.data.db.TargetCount
import com.arjunrana.tokishrine.data.db.TokiDatabase
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

// open (and log below) only so instrumented UI tests can gate event writes;
// production code always uses this class directly.
open class EventRepository(
    db: TokiDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    // The whole event store shares one database; the handle stays reachable
    // for the transactional completion below.
    private val db: TokiDatabase = db
    private val eventDao: EventDao = db.eventDao()
    private val metaDao: AppMetaDao = db.appMetaDao()

    open suspend fun log(
        name: String,
        blockId: Long? = null,
        target: String? = null,
        targetType: String? = null,
        params: Map<String, Any?> = emptyMap(),
    ) {
        // A target without a kind would silently vanish from the per-app
        // leaderboard, so the contract is enforced loudly at the call site.
        require(target == null || targetType != null) {
            "target_type is required whenever target is set (event: $name, target: $target)"
        }
        ensureFirstLaunchRecorded()
        val paramsJson = if (params.isEmpty()) null else JSONObject(params).toString()
        eventDao.insert(
            Event(
                name = name,
                timestampUtc = clock(),
                blockId = blockId,
                target = target,
                targetType = targetType,
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
            mostWalkedAwayFrom = eventDao.countsByTarget(EVENT_WALK_AWAY, TARGET_TYPE_APP),
        )
    }

    suspend fun countWalkAwaysSince(sinceUtc: Long): Int =
        eventDao.countByNameSince(EVENT_WALK_AWAY, sinceUtc)

    suspend fun countAllEvents(): Int = eventDao.countAll()

    // Onboarding completion flag (PRD §6 screen 2: persistent and resumable;
    // the welcome screen returns until onboarding is completed).
    suspend fun isOnboardingCompleted(): Boolean =
        metaDao.get(AppMeta.KEY_ONBOARDING_COMPLETED_AT) != null

    // Onboarding completion is terminal: the §10 event and the one-way
    // app_meta marker commit in one transaction and at most once, so a
    // repeated or recreated Continue can produce neither a second
    // onboarding_completed event nor an event/marker disagreement (review
    // blocker 2). Returns whether this call recorded the completion.
    open suspend fun completeOnboarding(grantedCount: Int): Boolean = db.withTransaction {
        ensureFirstLaunchRecorded()
        if (metaDao.get(AppMeta.KEY_ONBOARDING_COMPLETED_AT) != null) {
            false
        } else {
            eventDao.insert(
                Event(
                    name = EVENT_ONBOARDING_COMPLETED,
                    timestampUtc = clock(),
                    paramsJson = JSONObject(mapOf("granted_count" to grantedCount)).toString(),
                ),
            )
            metaDao.putIfAbsent(AppMeta(AppMeta.KEY_ONBOARDING_COMPLETED_AT, clock().toString()))
            true
        }
    }

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

        // target_type values, aligned with §10's trigger_type (app | site).
        const val TARGET_TYPE_APP = "app"
        const val TARGET_TYPE_SITE = "site"

        // Block-management events (§10) fired by the list/create/edit flows.
        const val EVENT_BLOCK_CREATE_STARTED = "block_create_started"
        const val EVENT_BLOCK_CREATE_STEP_COMPLETED = "block_create_step_completed"
        const val EVENT_BLOCK_CREATE_ABANDONED = "block_create_abandoned"
        const val EVENT_BLOCK_CREATED = "block_created"
        const val EVENT_BLOCK_EDITED = "block_edited"
        const val EVENT_BLOCK_DELETED = "block_deleted"
        const val EVENT_BLOCK_TURNED_ON = "block_turned_on"
        const val EVENT_BLOCK_TURNED_OFF = "block_turned_off"

        // Onboarding and settings events (§10) fired by the permission flow
        // (Phase 3) and the Settings screen.
        const val EVENT_ONBOARDING_STARTED = "onboarding_started"
        const val EVENT_PERMISSION_REQUESTED = "permission_requested"
        const val EVENT_PERMISSION_GRANTED = "permission_granted"
        const val EVENT_PERMISSION_DENIED = "permission_denied"
        const val EVENT_ONBOARDING_COMPLETED = "onboarding_completed"
        const val EVENT_SETTINGS_VIEWED = "settings_viewed"
    }
}
