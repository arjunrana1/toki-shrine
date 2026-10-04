package com.arjunrana.tokishrine

import android.app.Application
import android.content.Intent
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
import com.arjunrana.tokishrine.data.stats.AndroidUsageSource
import com.arjunrana.tokishrine.data.stats.StatsRepository

class TokiApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: TokiDatabase by lazy {
        Room.databaseBuilder(this, TokiDatabase::class.java, TokiDatabase.NAME)
            .addMigrations(TokiDatabase.MIGRATION_3_4)
            .build()
    }

    val blockRepository: BlockRepository by lazy { BlockRepository(database) }
    val challengeRepository: ChallengeRepository by lazy { ChallengeRepository(database) }
    val eventRepository: EventRepository by lazy {
        EventRepository(database)
    }
    val installedAppsRepository: InstalledAppsRepository by lazy { InstalledAppsRepository(this) }
    val statsRepository: StatsRepository by lazy {
        StatsRepository(database, AndroidUsageSource(this), packageName,
            installedPackages = {
                packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                    .mapNotNull { it.activityInfo?.packageName }.toSet()
            })
    }
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
