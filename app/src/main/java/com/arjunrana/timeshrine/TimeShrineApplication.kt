package com.arjunrana.timeshrine

import android.app.Application
import android.content.Intent
import com.arjunrana.timeshrine.data.apps.InstalledAppsRepository
import com.arjunrana.timeshrine.data.db.TimeShrineDatabase
import com.arjunrana.timeshrine.data.repo.BlockRepository
import com.arjunrana.timeshrine.data.repo.ChallengeRepository
import com.arjunrana.timeshrine.data.repo.EventRepository
import com.arjunrana.timeshrine.detection.AssetDetectionConfigLoader
import com.arjunrana.timeshrine.detection.DetectionConfigLoader
import com.arjunrana.timeshrine.detection.DetectionCoordinator
import com.arjunrana.timeshrine.pause.PauseCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.arjunrana.timeshrine.data.stats.AndroidUsageSource
import com.arjunrana.timeshrine.data.stats.StatsRepository

class TimeShrineApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: TimeShrineDatabase by lazy {
        TimeShrineDatabase.build(this)
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
            },
            siteHostPackages = {
                // A broken bundled config already disables site detection; Stats keeps app rows.
                try {
                    detectionConfigLoader.load().browsers.keys
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    emptySet()
                }
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
