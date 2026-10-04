package com.arjunrana.tokishrine.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.arjunrana.tokishrine.data.db.TokiDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Uses exported v3 SQL, then Room's real v4 migration + schema validation. Isolated DB only. */
class StatsMigrationTest {
    @Test fun v3UpgradePreservesBlocksTargetsEventsAndMarkersThenReopens() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "stats-migration-test.db"
        context.deleteDatabase(name)
        try {
            val json = InstrumentationRegistry.getInstrumentation().context.assets
                .open("com.arjunrana.tokishrine.data.db.TokiDatabase/3.json").bufferedReader().use { JSONObject(it.readText()) }
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
                sqlite.execSQL("INSERT INTO blocks VALUES (41, 'Preserved', 'TYPING', 15, 150, 350, 60, 360, 1)")
                sqlite.execSQL("INSERT INTO blocked_apps (block_id, package_name) VALUES (41, 'fixture.app')")
                sqlite.execSQL("INSERT INTO event (name, timestamp_utc, block_id, target, target_type, params_json) VALUES ('challenge_abandoned', 10, 41, 'fixture.app', 'app', '{}')")
                sqlite.execSQL("INSERT INTO app_meta (`key`, value) VALUES ('first_launch_at', '123')")
                sqlite.version = 3
            }
            repeat(2) {
                val db = Room.databaseBuilder(context, TokiDatabase::class.java, name).addMigrations(TokiDatabase.MIGRATION_3_4).build()
                try {
                    assertEquals("Preserved", db.blockDao().getBlock(41)!!.name)
                    assertEquals("fixture.app", db.blockDao().getAllApps().single().packageName)
                    assertEquals("challenge_abandoned", db.eventDao().getAll().single().name)
                    assertEquals("123", db.appMetaDao().get("first_launch_at"))
                    assertNull(db.statsDao().state())
                    assertTrue(db.statsDao().outcomes("0000-01-01").isEmpty())
                } finally { db.close() }
            }
        } finally { context.deleteDatabase(name) }
    }
}
