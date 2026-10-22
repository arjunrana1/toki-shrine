package com.arjunrana.timeshrine.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arjunrana.timeshrine.data.stats.*
import androidx.room.Database
import androidx.room.RoomDatabase
import com.arjunrana.timeshrine.data.entity.AppMeta
import com.arjunrana.timeshrine.data.entity.BlockedApp
import com.arjunrana.timeshrine.data.entity.BlockedSite
import com.arjunrana.timeshrine.data.entity.Block
import com.arjunrana.timeshrine.data.entity.Event

@Database(
    entities = [Block::class, BlockedApp::class, BlockedSite::class, Event::class, AppMeta::class,
        StatsBaseline::class, StatsState::class, StatsOutcome::class, StatsUsageEvent::class, StatsCoverage::class, StatsVisitOverride::class],
    // v4 adds Stats storage only. v3 blocks, events and markers are untouched.
    // v5 adds the per-app visit-length override table only (P7-F6).
    // v6 drops blocks.pause_minutes: the pause length is picked at the gate
    // (11 October §17 unlock-minutes addendum). See [build] for the upgrade.
    version = 6,
    exportSchema = true,
)
abstract class TimeShrineDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao
    abstract fun blockDao(): BlockDao
    abstract fun eventDao(): EventDao
    abstract fun appMetaDao(): AppMetaDao

    companion object {
        /**
         * The shipped database configuration. Upgrading from v3, v4 or v5 to
         * v6 is an owner-accepted data reset (11 October §17 addendum: no
         * users yet), scoped to exactly those start versions. Room rejects
         * migrations that start or end at a destructive start version, so
         * [MIGRATION_3_4] and [MIGRATION_4_5] are no longer registered; they
         * stay below, with schemas 3–5, as history. There is deliberately no
         * global fallbackToDestructiveMigration(): a same-version reopen
         * keeps every row, and any later version needs an explicit migration.
         */
        fun build(context: Context, name: String = NAME): TimeShrineDatabase =
            Room.databaseBuilder(context, TimeShrineDatabase::class.java, name)
                .fallbackToDestructiveMigrationFrom(*DESTRUCTIVE_FROM)
                .build()

        /** Start versions whose upgrade to v6 resets the database. */
        val DESTRUCTIVE_FROM = intArrayOf(3, 4, 5)

        // History: the v3 → v4 → v5 chain, no longer registered (see [build]).
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
        const val NAME = "time-shrine.db"
    }
}
