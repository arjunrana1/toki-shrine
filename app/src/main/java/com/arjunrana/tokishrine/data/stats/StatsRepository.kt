package com.arjunrana.tokishrine.data.stats

import androidx.room.withTransaction
import com.arjunrana.tokishrine.data.db.BlockWithContents
import com.arjunrana.tokishrine.data.entity.Event
import com.arjunrana.tokishrine.data.repo.EventRepository
import org.json.JSONObject
import com.arjunrana.tokishrine.data.db.TokiDatabase
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** The sole UI data API. Android history reads never run in a challenge transaction. */
class StatsRepository(
    private val db: TokiDatabase,
    private val usage: UsageSource,
    private val ownPackage: String,
    private val installedPackages: suspend () -> Set<String>,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    /** Browsers that host site blocks; their rows join the leaderboard while a site block is enabled. */
    private val siteHostPackages: suspend () -> Set<String> = { emptySet() },
) {
    private val dao = db.statsDao()
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow<StatsScreenState>(StatsScreenState.Loading)
    val state: StateFlow<StatsScreenState> = mutableState.asStateFlow()

    suspend fun refresh() { update(recalibrate = false) }
    /** Caller must show confirmation first. False means no baseline was replaced. */
    suspend fun recalibrate(): Boolean = update(recalibrate = true)

    private suspend fun update(recalibrate: Boolean): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (!usage.hasAccess()) {
                    mutableState.value = StatsScreenState.NoUsageAccess
                    return@withLock false
                }
                val now = clock()
                val today = Instant.ofEpochMilli(now).atZone(zone()).toLocalDate()
                val old = dao.state()
                val earliest = now - RETENTION_MS
                // Short overlap catches late Android events. Longer absence queries all
                // available history; its actual first event establishes coverage again.
                val incremental = !recalibrate && old != null && now - old.lastReadAt in 0..DAY_MS
                val readStart = if (incremental) maxOf(earliest, old!!.lastReadAt - 60_000) else now - 7 * DAY_MS
                val fresh = usage.read(readStart, now)
                // Permission can change while queryEvents is in flight.
                if (!usage.hasAccess()) {
                    mutableState.value = StatsScreenState.NoUsageAccess
                    return@withLock false
                }
                if (recalibrate && fresh == null) return@withLock false
                val installed = installedPackages()
                val siteHosts = siteHostPackages()
                val screenState = db.withTransaction {
                    if (fresh != null) {
                        dao.usage(fresh)
                        dao.pruneUsage(earliest)
                        val coverageStart = if (incremental) readStart else fresh.minOfOrNull { it.at }
                        val coverage = UsageVisits.mergeCoverage(dao.coverage().filter { it.end >= earliest }.map { it.copy(start = maxOf(it.start, earliest)) } +
                            listOfNotNull(coverageStart?.let { StatsCoverage(it, now) }))
                        dao.clearCoverage()
                        dao.coverage(coverage)
                        // Process each uninterrupted range independently: never bridge lost history.
                        val raw = dao.usage().filter { it.at <= now }
                        val visits = coverage.flatMap { span -> UsageVisits.parse(raw.filter { it.at in span.start..span.end }) }
                            .filter { it.end <= now - 2_000 }
                        if (old == null || recalibrate) {
                            val recent = visits.filter { it.start >= now - 7 * DAY_MS }
                            val baselines = recent.groupBy { it.packageName }.filterKeys { it != ownPackage }.map { (pkg, rows) ->
                                val valid = rows.filter { it.durationMs > 0 }
                                StatsBaseline(pkg, if (valid.size >= 3) valid.sumOf { it.durationMs } / valid.size else StatsLedger.FALLBACK_MS, valid.size)
                            }
                            val daily = UsageVisits.dailyMs(coverage.flatMap { span -> UsageVisits.parse(raw.filter { it.at in span.start..span.end }, minOf(span.end, now)) })
                            val completeDays = (1L..7L).map(today::minusDays).filter { date ->
                                date.atStartOfDay(zone()).toInstant().toEpochMilli() >= now - 7 * DAY_MS &&
                                UsageVisits.covered(date.atStartOfDay(zone()).toInstant().toEpochMilli(),
                                    date.plusDays(1).atStartOfDay(zone()).toInstant().toEpochMilli(), coverage)
                            }
                            val screenBaseline = completeDays.takeIf { it.isNotEmpty() }?.let { days ->
                                days.sumOf { daily[it.toString()] ?: 0L } / days.size
                            }
                            dao.clearBaselines()
                            dao.baselines(baselines)
                            dao.state(StatsState(baselineId = UUID.randomUUID().toString(), capturedAt = now,
                                screenDailyMs = screenBaseline, lastReadAt = now))
                            if (recalibrate) db.eventDao().insert(Event(
                                name = EventRepository.EVENT_BASELINE_RECALIBRATED, timestampUtc = now,
                                paramsJson = JSONObject().put("old_screen_daily_ms", old?.screenDailyMs ?: JSONObject.NULL)
                                    .put("new_screen_daily_ms", screenBaseline ?: JSONObject.NULL).toString(),
                            ))
                        } else dao.state(old.copy(lastReadAt = now))
                        for (outcome in dao.pending(earliest)) {
                            val visit = UsageVisits.firstPostChallenge(outcome, visits, ownPackage) ?: continue
                            if (!UsageVisits.covered(outcome.at, visit.end, coverage) || dao.claimed(visit.id) > 0) continue
                            dao.measure(outcome.sessionId, visit.durationMs, visit.id)
                        }
                    }
                    val blocks = db.blockDao().getBlocksWithContents().filter { it.block.enabled }
                    if (blocks.isEmpty()) {
                        StatsScreenState.NoBlocks
                    } else {
                        val coverage = dao.coverage()
                        val raw = dao.usage().filter { it.at <= now }
                        val visits = coverage.flatMap { span -> UsageVisits.parse(raw.filter { it.at in span.start..span.end }, minOf(span.end, now)) }
                        val daily = UsageVisits.dailyMs(visits)
                        val weekStart = today.minusDays(6).atStartOfDay(zone()).toInstant().toEpochMilli()
                        val screenDaily = if (fresh != null && UsageVisits.covered(weekStart, now, coverage))
                            (0L..6L).sumOf { daily[today.minusDays(it).toString()] ?: 0L } / 7 else null
                        StatsScreenState.Ready(StatsDashboardCalculator.calculate(
                            today, dao.outcomes(today.minusDays(6).toString()),
                            leaderboardPackages(blocks, siteHosts).intersect(installed),
                            dao.baselines(), dao.state(), screenDaily, fresh != null,
                        ))
                    }
                }
                mutableState.value = screenState
                fresh != null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = StatsScreenState.Error("Stats couldn't be loaded. Try again.")
                false
            }
        }
    }

    companion object {
        const val DAY_MS = 86_400_000L
        const val RETENTION_MS = 8 * DAY_MS

        /**
         * App targets of enabled blocks, plus every supported browser while an
         * enabled block has a site: site outcomes attribute to the hosting
         * browser (4 October §17 addendum), so it appears with zero rows too.
         */
        fun leaderboardPackages(enabledBlocks: List<BlockWithContents>, siteHosts: Set<String>): Set<String> =
            enabledBlocks.flatMap { it.apps }.map { it.packageName }.toSet() +
                if (enabledBlocks.any { it.sites.isNotEmpty() }) siteHosts else emptySet()
    }
}
