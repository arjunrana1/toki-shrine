package com.arjunrana.tokishrine.data.stats

import androidx.room.*

@Entity(tableName = "stats_baseline")
data class StatsBaseline(@PrimaryKey val packageName: String, val visitMs: Long, val samples: Int)

@Entity(tableName = "stats_state")
data class StatsState(
    @PrimaryKey val id: Int = 1,
    val baselineId: String,
    val capturedAt: Long,
    val screenDailyMs: Long?,
    val lastReadAt: Long,
)

/** No FK to blocks: deleting a block must not delete earned history. */
@Entity(tableName = "stats_outcome", indices = [Index(value = ["packageName", "at"])])
data class StatsOutcome(
    @PrimaryKey val sessionId: String,
    val packageName: String,
    val at: Long,
    val localDate: String,
    val kind: String,
    val counted: Boolean,
    val savedMs: Long,
    val baselineId: String?,
    val spentMs: Long? = null,
    val visitId: String? = null,
)

@Entity(tableName = "stats_usage_event", indices = [Index("at")])
data class StatsUsageEvent(
    @PrimaryKey val key: String,
    val at: Long,
    val packageName: String,
    val activity: String,
    val kind: Int,
    val zoneId: String,
)

/**
 * The user's own usual visit length for one app (v5, P7-F6). It always wins
 * over the measured baseline and survives Recalibrate; clearing it restores
 * the measured value and each outcome's frozen contribution.
 */
@Entity(tableName = "stats_visit_override")
data class StatsVisitOverride(@PrimaryKey val packageName: String, val visitMs: Long, val setAt: Long)

@Entity(tableName = "stats_coverage")
data class StatsCoverage(@PrimaryKey val start: Long, val end: Long)

@Dao
interface StatsDao {
    @Query("SELECT * FROM stats_state WHERE id = 1") suspend fun state(): StatsState?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun state(value: StatsState)
    @Query("SELECT * FROM stats_baseline") suspend fun baselines(): List<StatsBaseline>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun baselines(values: List<StatsBaseline>)
    @Query("DELETE FROM stats_baseline") suspend fun clearBaselines()
    @Query("SELECT * FROM stats_outcome WHERE sessionId = :id") suspend fun outcome(id: String): StatsOutcome?
    @Query("SELECT * FROM stats_outcome WHERE packageName = :pkg AND counted = 1 ORDER BY rowid DESC LIMIT 1")
    suspend fun lastCounted(pkg: String): StatsOutcome?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun outcome(value: StatsOutcome)
    @Query("SELECT * FROM stats_outcome WHERE localDate >= :since ORDER BY at, sessionId")
    suspend fun outcomes(since: String): List<StatsOutcome>
    @Query("SELECT * FROM stats_outcome WHERE kind = 'pass' AND spentMs IS NULL AND at >= :since ORDER BY at, sessionId")
    suspend fun pending(since: Long): List<StatsOutcome>
    @Query("SELECT COUNT(*) FROM stats_outcome WHERE visitId = :visitId") suspend fun claimed(visitId: String): Int
    @Query("UPDATE stats_outcome SET spentMs = :ms, visitId = :visitId WHERE sessionId = :id AND spentMs IS NULL")
    suspend fun measure(id: String, ms: Long, visitId: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun usage(values: List<StatsUsageEvent>)
    @Query("SELECT * FROM stats_usage_event ORDER BY at, rowid") suspend fun usage(): List<StatsUsageEvent>
    @Query("DELETE FROM stats_usage_event WHERE at < :before") suspend fun pruneUsage(before: Long)
    @Query("SELECT * FROM stats_visit_override") suspend fun overrides(): List<StatsVisitOverride>
    @Query("SELECT * FROM stats_visit_override WHERE packageName = :pkg") suspend fun overrideFor(pkg: String): StatsVisitOverride?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun override(value: StatsVisitOverride)
    @Query("DELETE FROM stats_visit_override WHERE packageName = :pkg") suspend fun clearOverride(pkg: String)
    @Query("SELECT * FROM stats_coverage ORDER BY start") suspend fun coverage(): List<StatsCoverage>
    @Query("DELETE FROM stats_coverage") suspend fun clearCoverage()
    @Insert suspend fun coverage(values: List<StatsCoverage>)
}
