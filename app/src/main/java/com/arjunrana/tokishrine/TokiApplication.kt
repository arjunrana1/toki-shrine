package com.arjunrana.tokishrine

import android.app.Application
import androidx.room.Room
import com.arjunrana.tokishrine.data.apps.InstalledAppsRepository
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.data.repo.ChallengeRepository
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.detection.AssetDetectionConfigLoader
import com.arjunrana.tokishrine.detection.DetectionConfigLoader
import com.arjunrana.tokishrine.detection.DetectionCoordinator
import com.arjunrana.tokishrine.pause.PauseCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TokiApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: TokiDatabase by lazy {
        Room.databaseBuilder(this, TokiDatabase::class.java, TokiDatabase.NAME)
            // Phase 7 migration posture: the dev-only destructive fallback is
            // retired. Schema v3 stands (exported under app/schemas), so an
            // existing v3 install — the only version in the field — opens
            // with its blocks and events preserved, and any future version
            // bump must ship an explicit migration. A pre-v3 install, of
            // which none exists, now fails loudly instead of silently
            // wiping data.
            .build()
    }

    val blockRepository: BlockRepository by lazy { BlockRepository(database) }
    val challengeRepository: ChallengeRepository by lazy { ChallengeRepository(database) }
    val eventRepository: EventRepository by lazy {
        EventRepository(database)
    }
    val installedAppsRepository: InstalledAppsRepository by lazy { InstalledAppsRepository(this) }
    val detectionCoordinator = DetectionCoordinator()

    // Process owner of the live pause set (Phase 6): pause access for
    // detection, monotonic expiry/re-arm, and the PauseService lifecycle.
    val pauseCoordinator: PauseCoordinator by lazy {
        PauseCoordinator(this, eventRepository, blockRepository, applicationScope)
    }

    // Bundled detection JSON (browser map + OEM battery text) behind the
    // replaceable loader seam (PRD §13, Phase 4).
    val detectionConfigLoader: DetectionConfigLoader by lazy { AssetDetectionConfigLoader(this) }
}
