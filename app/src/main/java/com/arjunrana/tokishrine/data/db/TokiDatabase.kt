package com.arjunrana.tokishrine.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.arjunrana.tokishrine.data.entity.AppMeta
import com.arjunrana.tokishrine.data.entity.BlockedApp
import com.arjunrana.tokishrine.data.entity.BlockedSite
import com.arjunrana.tokishrine.data.entity.Block
import com.arjunrana.tokishrine.data.entity.Event

@Database(
    entities = [Block::class, BlockedApp::class, BlockedSite::class, Event::class, AppMeta::class],
    // v3, wizard redesign (19 September 2026): blocks gains turnoff_seconds
    // and drops the obsolete show_typos column (typos are always shown). The
    // owner explicitly approved discarding all existing app data for this
    // redesign, so the destructive rebuild in TokiApplication is the
    // deliberate reset path — no legacy mapping or data-preserving
    // migration ships. Not a general production migration policy.
    version = 3,
    exportSchema = false,
)
abstract class TokiDatabase : RoomDatabase() {
    abstract fun blockDao(): BlockDao
    abstract fun eventDao(): EventDao
    abstract fun appMetaDao(): AppMetaDao

    companion object {
        const val NAME = "toki-shrine.db"
    }
}
