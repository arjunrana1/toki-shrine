package com.arjunrana.tokishrine.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arjunrana.tokishrine.data.stats.*
import androidx.room.Database
import androidx.room.RoomDatabase
import com.arjunrana.tokishrine.data.entity.AppMeta
import com.arjunrana.tokishrine.data.entity.BlockedApp
import com.arjunrana.tokishrine.data.entity.BlockedSite
import com.arjunrana.tokishrine.data.entity.Block
import com.arjunrana.tokishrine.data.entity.Event

@Database(
    entities = [Block::class, BlockedApp::class, BlockedSite::class, Event::class, AppMeta::class,
        StatsBaseline::class, StatsState::class, StatsOutcome::class, StatsUsageEvent::class, StatsCoverage::class, StatsVisitOverride::class],
    // v4 adds Stats storage only. v3 blocks, events and markers are untouched.
    // v5 adds the per-app visit-length override table only (P7-F6).
    version = 5,
    exportSchema = true,
)
abstract class TokiDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao
    abstract fun blockDao(): BlockDao
    abstract fun eventDao(): EventDao
    abstract fun appMetaDao(): AppMetaDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS stats_baseline (packageName TEXT NOT NULL PRIMARY KEY, visitMs INTEGER NOT NULL, samples INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS stats_state (id INTEGER NOT NULL PRIMARY KEY, baselineId TEXT NOT NULL, capturedAt INTEGER NOT NULL, screenDailyMs INTEGER, lastReadAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS stats_outcome (sessionId TEXT NOT NULL PRIMARY KEY, packageName TEXT NOT NULL, at INTEGER NOT NULL, localDate TEXT NOT NULL, kind TEXT NOT NULL, counted INTEGER NOT NULL, savedMs INTEGER NOT NULL, baselineId TEXT, spentMs INTEGER, visitId TEXT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stats_outcome_packageName_at ON stats_outcome (packageName, at)")
                db.execSQL("CREATE TABLE IF NOT EXISTS stats_usage_event (`key` TEXT NOT NULL PRIMARY KEY, at INTEGER NOT NULL, packageName TEXT NOT NULL, activity TEXT NOT NULL, kind INTEGER NOT NULL, zoneId TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stats_usage_event_at ON stats_usage_event (at)")
                db.execSQL("CREATE TABLE IF NOT EXISTS stats_coverage (start INTEGER NOT NULL PRIMARY KEY, end INTEGER NOT NULL)")
            }
        }
        // Additive only: no existing table or row changes. Stored baselines keep
        // their old mean value until the next Recalibrate (Arjun, 8 October:
        // no one-time p75 recompute).
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS stats_visit_override (packageName TEXT NOT NULL PRIMARY KEY, visitMs INTEGER NOT NULL, setAt INTEGER NOT NULL)")
            }
        }
        const val NAME = "toki-shrine.db"
    }
}
