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
    // Schema history:
    // v3, wizard redesign (19 September 2026): blocks gains turnoff_seconds
    // and drops the obsolete show_typos column (typos are always shown). The
    // owner explicitly approved discarding all existing app data for that
    // redesign, so the v2→v3 transition used the then-present destructive
    // fallback as the deliberate reset path.
    //
    // Phase 7 (23 September 2026) closes the dev-only posture: schema export
    // is enabled (app/schemas) so future version bumps ship reviewed JSON
    // schemas and explicit migrations, and the destructive fallback is gone
    // (see TokiApplication). v3 stands unchanged — no Phase 7 entity changed —
    // so existing v3 blocks/events are preserved; no pre-v3 install exists in
    // the field, and an unexpected one now fails loudly instead of wiping.
    version = 3,
    exportSchema = true,
)
abstract class TokiDatabase : RoomDatabase() {
    abstract fun blockDao(): BlockDao
    abstract fun eventDao(): EventDao
    abstract fun appMetaDao(): AppMetaDao

    companion object {
        const val NAME = "toki-shrine.db"
    }
}
