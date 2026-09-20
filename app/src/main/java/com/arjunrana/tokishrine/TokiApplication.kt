package com.arjunrana.tokishrine

import android.app.Application
import androidx.room.Room
import com.arjunrana.tokishrine.data.apps.InstalledAppsRepository
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.detection.AssetDetectionConfigLoader
import com.arjunrana.tokishrine.detection.DetectionConfigLoader

class TokiApplication : Application() {

    val database: TokiDatabase by lazy {
        Room.databaseBuilder(this, TokiDatabase::class.java, TokiDatabase.NAME)
            // Dev-phase decision (DECISIONS.md, Phase 2): no builds are
            // distributed yet, so a schema bump during development rebuilds
            // instead of crashing on a stale install. Revisit before any
            // build leaves this device.
            .fallbackToDestructiveMigration()
            .build()
    }

    val blockRepository: BlockRepository by lazy { BlockRepository(database) }
    val eventRepository: EventRepository by lazy {
        EventRepository(database)
    }
    val installedAppsRepository: InstalledAppsRepository by lazy { InstalledAppsRepository(this) }

    // Bundled detection JSON (browser map + OEM battery text) behind the
    // replaceable loader seam (PRD §13, Phase 4).
    val detectionConfigLoader: DetectionConfigLoader by lazy { AssetDetectionConfigLoader(this) }
}
