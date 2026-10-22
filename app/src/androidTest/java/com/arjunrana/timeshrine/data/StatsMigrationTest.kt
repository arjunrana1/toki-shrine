package com.arjunrana.timeshrine.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.util.TableInfo
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.arjunrana.timeshrine.data.db.TimeShrineDatabase
import com.arjunrana.timeshrine.data.entity.FrictionType
import com.arjunrana.timeshrine.data.repo.BlockDraft
import com.arjunrana.timeshrine.data.repo.BlockRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Uses exported schema SQL. Isolated DB files only.
 *
 * Schema v6 (11 October §17 unlock-minutes addendum) resets upgrades from
 * v3, v4 and v5 (owner-accepted, `TimeShrineDatabase.build`), so the historical
 * v3 → v4 → v5 chain no longer runs inside Room. It is still checked here:
 * the kept MIGRATION_3_4 / MIGRATION_4_5 run on a raw helper and every table
 * is compared with Room's own [TableInfo] check against the exported 5.json.
 */
class StatsMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun exported(version: Int): JSONObject =
        InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.arjunrana.timeshrine.data.db.TimeShrineDatabase/$version.json").bufferedReader().use { JSONObject(it.readText()) }
            .getJSONObject("database")

    /** Builds an isolated database from exported [schemaVersion] SQL, seeds it, and stamps [version]. */
    private fun createAt(name: String, version: Int, schemaVersion: Int = version, seed: (SQLiteDatabase) -> Unit) {
        context.deleteDatabase(name)
        val json = exported(schemaVersion)
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

    /** A raw (non-Room) open at [version]; [upgrade] runs for an older file. */
    private fun rawOpen(
        name: String,
        version: Int,
        upgrade: (SupportSQLiteDatabase, Int) -> Unit = { _, _ -> },
    ): SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) = error("expected an existing database file")
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = upgrade(db, oldVersion)
            })
            .build(),
    )

    /** The historical chain, exactly as Room ran it before v6. */
    private fun openMigratedTo5(name: String) = rawOpen(name, 5) { db, oldVersion ->
        if (oldVersion < 4) TimeShrineDatabase.MIGRATION_3_4.migrate(db)
        TimeShrineDatabase.MIGRATION_4_5.migrate(db)
    }

    /** Room's schema validation, table by table, against a database built from 5.json. */
    private fun assertMatchesExportedV5(migrated: SupportSQLiteDatabase) {
        val referenceName = "stats-migration-v5-reference.db"
        createAt(referenceName, 5) {}
        val reference = rawOpen(referenceName, 5)
        try {
            val entities = exported(5).getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val table = entities.getJSONObject(i).getString("tableName")
                assertEquals(table, TableInfo.read(reference.writableDatabase, table), TableInfo.read(migrated, table))
            }
        } finally {
            reference.close()
            context.deleteDatabase(referenceName)
        }
    }

    private fun SupportSQLiteDatabase.strings(sql: String): List<String> = query(sql).use { cursor ->
        buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
    }

    @Test fun v3UpgradePreservesBlocksTargetsEventsAndMarkersThenReopens() {
        val name = "stats-migration-test.db"
        try {
            createAt(name, 3) { sqlite ->
                sqlite.execSQL("INSERT INTO blocks VALUES (41, 'Preserved', 'TYPING', 15, 150, 350, 60, 360, 1)")
                sqlite.execSQL("INSERT INTO blocked_apps (block_id, package_name) VALUES (41, 'fixture.app')")
                sqlite.execSQL("INSERT INTO event (name, timestamp_utc, block_id, target, target_type, params_json) VALUES ('challenge_abandoned', 10, 41, 'fixture.app', 'app', '{}')")
                sqlite.execSQL("INSERT INTO app_meta (`key`, value) VALUES ('first_launch_at', '123')")
            }
            repeat(2) { round ->
                val helper = openMigratedTo5(name)
                try {
                    val db = helper.writableDatabase
                    if (round == 0) assertMatchesExportedV5(db)
                    assertEquals(listOf("Preserved"), db.strings("SELECT name FROM blocks WHERE id = 41"))
                    assertEquals(listOf("fixture.app"), db.strings("SELECT package_name FROM blocked_apps"))
                    assertEquals(listOf("challenge_abandoned"), db.strings("SELECT name FROM event"))
                    assertEquals(listOf("123"), db.strings("SELECT value FROM app_meta WHERE `key` = 'first_launch_at'"))
                    assertTrue(db.strings("SELECT baselineId FROM stats_state").isEmpty())
                    assertTrue(db.strings("SELECT sessionId FROM stats_outcome").isEmpty())
                    assertTrue(db.strings("SELECT packageName FROM stats_visit_override").isEmpty())
                } finally { helper.close() }
            }
        } finally { context.deleteDatabase(name) }
    }

    /** P7-F6: v4 → v5 only adds the override table; stored baselines keep their old value (no p75 recompute). */
    @Test fun v4UpgradeKeepsStatsBaselinesOutcomesAndBlocksAndAddsAnEmptyOverrideTable() {
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
                val helper = openMigratedTo5(name)
                try {
                    val db = helper.writableDatabase
                    if (round == 0) assertMatchesExportedV5(db)
                    assertEquals(listOf("Preserved"), db.strings("SELECT name FROM blocks WHERE id = 41"))
                    assertEquals(listOf("fixture.app"), db.strings("SELECT package_name FROM blocked_apps"))
                    assertEquals(listOf("123"), db.strings("SELECT value FROM app_meta WHERE `key` = 'first_launch_at'"))
                    assertEquals(listOf("b1"), db.strings("SELECT baselineId FROM stats_state"))
                    assertEquals(listOf("fixture.app|12345|7"), db.strings("SELECT packageName || '|' || visitMs || '|' || samples FROM stats_baseline"))
                    assertEquals(listOf("12345"), db.strings("SELECT savedMs FROM stats_outcome WHERE sessionId = 's1'"))
                    if (round == 0) {
                        assertTrue(db.strings("SELECT packageName FROM stats_visit_override").isEmpty())
                        db.execSQL("INSERT INTO stats_visit_override (packageName, visitMs, setAt) VALUES ('fixture.app', 300000, 400)")
                    } else {
                        assertEquals(listOf("fixture.app|300000|400"), db.strings("SELECT packageName || '|' || visitMs || '|' || setAt FROM stats_visit_override"))
                    }
                } finally { helper.close() }
            }
        } finally { context.deleteDatabase(name) }
    }

    /**
     * UM-1: a v3, v4 or v5 file opens as v6 through the shipped builder
     * without crashing, empty (the owner-accepted reset), and the reset
     * happens once — rows written at v6 survive a reopen.
     */
    @Test fun v3V4AndV5FilesOpenAsAnEmptyV6DatabaseOnceAndThenKeepTheirRows() = runBlocking {
        listOf(3, 4, 5).forEach { version ->
            val name = "unlock-minutes-v$version-to-v6.db"
            try {
                createAt(name, version) { sqlite ->
                    sqlite.execSQL("INSERT INTO blocks VALUES (41, 'Old', 'TYPING', 15, 150, 350, 60, 360, 1)")
                    sqlite.execSQL("INSERT INTO blocked_apps (block_id, package_name) VALUES (41, 'fixture.app')")
                    sqlite.execSQL("INSERT INTO event (name, timestamp_utc, block_id, target, target_type, params_json) VALUES ('walk_away', 10, 41, 'fixture.app', 'app', '{}')")
                    sqlite.execSQL("INSERT INTO app_meta (`key`, value) VALUES ('first_launch_at', '123')")
                }
                val first = TimeShrineDatabase.build(context, name)
                val id = try {
                    assertTrue(first.blockDao().getBlocksWithContents().isEmpty())
                    assertTrue(first.eventDao().getAll().isEmpty())
                    assertNull(first.appMetaDao().get("first_launch_at"))
                    assertNull(first.statsDao().state())
                    BlockRepository(first).createBlock(
                        BlockDraft("New", listOf("fixture.app"), emptyList(), FrictionType.TYPING, 40, 200, 60, 120),
                    )
                } finally { first.close() }

                val second = TimeShrineDatabase.build(context, name)
                try {
                    assertEquals(6, second.openHelper.readableDatabase.version)
                    val reopened = second.blockDao().getBlocksWithContents().single()
                    assertEquals(id, reopened.block.id)
                    assertEquals("New", reopened.block.name)
                    assertEquals(listOf("fixture.app"), reopened.apps.map { it.packageName })
                } finally { second.close() }
            } finally { context.deleteDatabase(name) }
        }
    }

    /** UM-1: the reset is scoped to 3, 4 and 5; any other old version still needs a migration. */
    @Test fun destructiveFallbackIsScopedToVersionsThreeToFive() {
        val name = "unlock-minutes-v2-file.db"
        try {
            createAt(name, version = 2, schemaVersion = 3) {}
            val db = TimeShrineDatabase.build(context, name)
            try {
                assertThrows(IllegalStateException::class.java) { db.openHelper.writableDatabase }
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }
}
