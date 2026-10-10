package com.arjunrana.tokishrine.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.stats.StatsBaseline
import com.arjunrana.tokishrine.data.stats.StatsVisitOverride
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Uses exported schema SQL, then Room's real migrations + schema validation. Isolated DB only. */
class StatsMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** Builds an isolated database at exported [version], seeds it with [seed], then closes it. */
    private fun createAt(name: String, version: Int, seed: (SQLiteDatabase) -> Unit) {
        context.deleteDatabase(name)
        val json = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.arjunrana.tokishrine.data.db.TokiDatabase/$version.json").bufferedReader().use { JSONObject(it.readText()) }
            .getJSONObject("database")
        context.getDatabasePath(name).parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { sqlite ->
            val entities = json.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                sqlite.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) sqlite.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = json.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) sqlite.execSQL(setup.getString(i))
            seed(sqlite)
            sqlite.version = version
        }
    }

    private fun open(name: String) = Room.databaseBuilder(context, TokiDatabase::class.java, name)
        .addMigrations(TokiDatabase.MIGRATION_3_4, TokiDatabase.MIGRATION_4_5).build()

    @Test fun v3UpgradePreservesBlocksTargetsEventsAndMarkersThenReopens() = runBlocking {
        val name = "stats-migration-test.db"
        try {
            createAt(name, 3) { sqlite ->
                sqlite.execSQL("INSERT INTO blocks VALUES (41, 'Preserved', 'TYPING', 15, 150, 350, 60, 360, 1)")
                sqlite.execSQL("INSERT INTO blocked_apps (block_id, package_name) VALUES (41, 'fixture.app')")
                sqlite.execSQL("INSERT INTO event (name, timestamp_utc, block_id, target, target_type, params_json) VALUES ('challenge_abandoned', 10, 41, 'fixture.app', 'app', '{}')")
                sqlite.execSQL("INSERT INTO app_meta (`key`, value) VALUES ('first_launch_at', '123')")
            }
            repeat(2) {
                val db = open(name)
                try {
                    assertEquals("Preserved", db.blockDao().getBlock(41)!!.name)
                    assertEquals("fixture.app", db.blockDao().getAllApps().single().packageName)
                    assertEquals("challenge_abandoned", db.eventDao().getAll().single().name)
                    assertEquals("123", db.appMetaDao().get("first_launch_at"))
                    assertNull(db.statsDao().state())
                    assertTrue(db.statsDao().outcomes("0000-01-01").isEmpty())
                    assertTrue(db.statsDao().overrides().isEmpty())
                } finally { db.close() }
            }
        } finally { context.deleteDatabase(name) }
    }

    /** P7-F6: v4 → v5 only adds the override table; stored baselines keep their old value (no p75 recompute). */
    @Test fun v4UpgradeKeepsStatsBaselinesOutcomesAndBlocksAndAddsAnEmptyOverrideTable() = runBlocking {
        val name = "stats-migration-v4-test.db"
        try {
            createAt(name, 4) { sqlite ->
                sqlite.execSQL("INSERT INTO blocks VALUES (41, 'Preserved', 'TYPING', 15, 150, 350, 60, 360, 1)")
                sqlite.execSQL("INSERT INTO blocked_apps (block_id, package_name) VALUES (41, 'fixture.app')")
                sqlite.execSQL("INSERT INTO stats_state (id, baselineId, capturedAt, screenDailyMs, lastReadAt) VALUES (1, 'b1', 100, 3600000, 200)")
                sqlite.execSQL("INSERT INTO stats_baseline (packageName, visitMs, samples) VALUES ('fixture.app', 12345, 7)")
                sqlite.execSQL("INSERT INTO stats_outcome (sessionId, packageName, at, localDate, kind, counted, savedMs, baselineId, spentMs, visitId) VALUES ('s1', 'fixture.app', 150, '2026-10-04', 'nope', 1, 12345, 'b1', NULL, NULL)")
                sqlite.execSQL("INSERT INTO app_meta (`key`, value) VALUES ('first_launch_at', '123')")
            }
            repeat(2) { round ->
                val db = open(name)
                try {
                    assertEquals("Preserved", db.blockDao().getBlock(41)!!.name)
                    assertEquals("fixture.app", db.blockDao().getAllApps().single().packageName)
                    assertEquals("123", db.appMetaDao().get("first_launch_at"))
                    assertEquals("b1", db.statsDao().state()!!.baselineId)
                    assertEquals(StatsBaseline("fixture.app", 12345, 7), db.statsDao().baselines().single())
                    assertEquals(12345L, db.statsDao().outcome("s1")!!.savedMs)
                    if (round == 0) {
                        assertTrue(db.statsDao().overrides().isEmpty())
                        db.statsDao().override(StatsVisitOverride("fixture.app", 300_000, 400))
                    } else {
                        assertEquals(StatsVisitOverride("fixture.app", 300_000, 400), db.statsDao().overrides().single())
                    }
                } finally { db.close() }
            }
        } finally { context.deleteDatabase(name) }
    }
}
