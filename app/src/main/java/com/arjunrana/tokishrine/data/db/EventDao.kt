package com.arjunrana.tokishrine.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arjunrana.tokishrine.data.entity.AppMeta
import com.arjunrana.tokishrine.data.entity.Event

@Dao
interface EventDao {

    @Insert
    suspend fun insert(event: Event): Long

    @Query("SELECT COUNT(*) FROM event WHERE name = :name")
    suspend fun countByName(name: String): Int

    @Query("SELECT COUNT(*) FROM event WHERE name = :name AND timestamp_utc >= :sinceUtc")
    suspend fun countByNameSince(name: String, sinceUtc: Long): Int

    // Best day groups by calendar day in the device's timezone. Seeded tests
    // use local-noon timestamps so every event lands in exactly one bucket.
    @Query(
        "SELECT MAX(c) FROM (" +
            "SELECT COUNT(*) AS c FROM event WHERE name = :name " +
            "GROUP BY strftime('%Y-%m-%d', timestamp_utc / 1000, 'unixepoch', 'localtime')" +
            ")",
    )
    suspend fun maxCountPerLocalDay(name: String): Int?

    // Per-app leaderboard only (PRD §9: "Most walked away from — per-app").
    // Website walk-aways carry target_type 'site' and are excluded here while
    // global counts (countByName / countByNameSince) stay inclusive.
    @Query(
        "SELECT target AS target, COUNT(*) AS count FROM event " +
            "WHERE name = :name AND target IS NOT NULL AND target_type = :targetType " +
            "GROUP BY target ORDER BY count DESC",
    )
    suspend fun countsByTarget(name: String, targetType: String): List<TargetCount>

    @Query("SELECT COUNT(*) FROM event")
    suspend fun countAll(): Int

    @Query("SELECT * FROM event ORDER BY id")
    suspend fun getAll(): List<Event>
}

data class TargetCount(val target: String, val count: Int)

@Dao
interface AppMetaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun putIfAbsent(meta: AppMeta)

    @Query("SELECT value FROM app_meta WHERE `key` = :key")
    suspend fun get(key: String): String?
}
