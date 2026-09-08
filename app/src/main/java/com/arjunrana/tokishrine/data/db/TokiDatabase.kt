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
    version = 1,
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
