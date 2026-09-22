package com.arjunrana.tokishrine.pause

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.ServiceCompat
import com.arjunrana.tokishrine.MainActivity
import com.arjunrana.tokishrine.R
import com.arjunrana.tokishrine.TokiApplication
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.ui.theme.NocturneAccent
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground host for the Phase 6 pause surfaces (PRD §§6/12): one ongoing
 * countdown notification per active pause (the soonest doubles as the
 * foreground notification) and the draggable overlay bubble when the
 * overlay permission is granted. All pause rules live in
 * [PauseCoordinator]/[PauseRegistry]; this service only renders and reports
 * the §10 bubble events.
 *
 * Lifetime follows the pause set: the coordinator starts it with the first
 * pause and it stops itself when the last pause ends. Everything it shows
 * is re-derived from the monotonic deadlines on every tick, so a device
 * clock change cannot shorten or extend a countdown.
 */
class PauseService : Service(), PauseBubbleView.Host {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var app: TokiApplication
    private lateinit var notifications: NotificationManager

    private var bubble: PauseBubbleView? = null
    private var bubbleShownLoggedBlockId: Long = -1
    private var activeNotifIds = mutableSetOf<Int>()
    private var foregroundMet = false

    // Last rendered inputs, so the tickers can re-derive fresh output.
    private var latestPauses: Map<Long, PauseState> = emptyMap()
    private var latestNames: Map<Long, String> = emptyMap()

    private val minuteTick = object : Runnable {
        override fun run() {
            app.pauseCoordinator.evaluate()
            renderNotificationsFromCache()
            mainHandler.postDelayed(this, MINUTE_MS)
        }
    }

    private val bubbleTick = object : Runnable {
        override fun run() {
            app.pauseCoordinator.evaluate()
            updateBubbleText()
            mainHandler.postDelayed(this, SECOND_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = application as TokiApplication
        notifications = getSystemService(NotificationManager::class.java)
        createChannel()
        serviceScope.launch {
            combine(
                app.pauseCoordinator.pauses,
                app.blockRepository.observeBlocksWithContents()
                    .distinctUntilChanged(),
            ) { pauses, blocks ->
                pauses to blocks.associate { it.block.id to it.block.name }
            }.collect { (pauses, names) ->
                withContext(Dispatchers.Main) { render(pauses, names) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The foreground obligation is met immediately, before the first
        // flow emission has names loaded; the next render replaces this.
        val soonest = app.pauseCoordinator.pauses.value.values.minByOrNull { it.deadlineMs }
        if (soonest == null) {
            // Nothing left to host (the last pause ended before this start
            // arrived). Still satisfy the startForegroundService contract,
            // then leave.
            stopAfterMeetingForegroundObligation()
        } else {
            promote(soonest, name = null)
            scheduleTicks()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(minuteTick)
        mainHandler.removeCallbacks(bubbleTick)
        bubble?.detach()
        bubble = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // — rendering —

    private fun render(pauses: Map<Long, PauseState>, names: Map<Long, String>) {
        latestPauses = pauses
        latestNames = names
        renderNotificationsFromCache()
        renderBubble()
    }

    private fun renderNotificationsFromCache() {
        val now = clock()
        val active = latestPauses.values
            .filter { it.remainingMs(now) > 0 }
            .sortedBy { it.deadlineMs }

        if (active.isEmpty()) {
            bubble?.detach()
            bubble = null
            bubbleShownLoggedBlockId = -1
            activeNotifIds.forEach(notifications::cancel)
            activeNotifIds.clear()
            if (foregroundMet) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                // A startForegroundService start raced the pause ending;
                // meet the obligation before leaving.
                stopAfterMeetingForegroundObligation()
            }
            return
        }

        val ids = active.map { notificationId(it.blockId) }.toMutableSet()
        active.forEach { state ->
            val notification = buildNotification(state, latestNames[state.blockId], now)
            notifications.notify(notificationId(state.blockId), notification)
        }
        active.first().let { soonest ->
            foregroundMet = true
            ServiceCompat.startForeground(
                this,
                notificationId(soonest.blockId),
                buildNotification(soonest, latestNames[soonest.blockId], now),
                foregroundServiceType(),
            )
        }
        activeNotifIds.minus(ids).forEach(notifications::cancel)
        activeNotifIds = ids
    }

    private fun buildNotification(state: PauseState, name: String?, now: Long): Notification {
        val remaining = state.remainingMs(now)
        val title = if (name == null) {
            getString(R.string.pause_notification_generic_title)
        } else {
            getString(R.string.pause_notification_title, name)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pause)
            .setContentTitle(title)
            .setContentText(getString(R.string.pause_notification_body, PauseFormat.clockText(remaining)))
            // The system chronometer is anchored on the monotonic clock at
            // post time and counts down from this `when` (PRD §6 screen 21);
            // every re-post re-derives it from the monotonic remaining time.
            .setWhen(PauseFormat.chronometerWhen(System.currentTimeMillis(), remaining))
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(NocturneAccent.toArgb())
            .setProgress(1000, PauseFormat.elapsedPerMille(state, now), false)
            .setContentIntent(detailIntent(state.blockId))
            .build()
    }

    private fun promote(state: PauseState, name: String?) {
        foregroundMet = true
        ServiceCompat.startForeground(
            this,
            notificationId(state.blockId),
            buildNotification(state, name, clock()),
            foregroundServiceType(),
        )
    }

    /**
     * A service asked to start must call startForeground even when there is
     * already nothing to show, or the system raises
     * ForegroundServiceDidNotStartInTimeException.
     */
    private fun stopAfterMeetingForegroundObligation() {
        ServiceCompat.startForeground(
            this,
            notificationId(0),
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_pause)
                .setContentTitle(getString(R.string.pause_notification_generic_title))
                .build(),
            foregroundServiceType(),
        )
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun foregroundServiceType(): Int =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    private fun renderBubble() {
        val now = clock()
        val soonest = latestPauses.values.minByOrNull { it.deadlineMs }
        if (soonest == null || soonest.remainingMs(now) <= 0 || !Settings.canDrawOverlays(this)) {
            // Overlay permission denied (PRD §12): the pause and the
            // notification still work; only the bubble disappears.
            bubble?.detach()
            bubble = null
            bubbleShownLoggedBlockId = -1
            return
        }
        val current = bubble
        if (current == null || !current.isAttached()) {
            val view = PauseBubbleView(this)
            if (view.attach(this)) {
                bubble = view
                bubbleShownLoggedBlockId = -1
            }
        }
        val name = viewDisplayName(soonest.blockId)
        bubble?.showContent(soonest.blockId, name, PauseFormat.clockText(soonest.remainingMs(now)))
        val attached = bubble
        if (attached != null && attached.displayedBlockId() != bubbleShownLoggedBlockId) {
            bubbleShownLoggedBlockId = attached.displayedBlockId()
            logEvent { log(EventRepository.EVENT_BUBBLE_SHOWN, blockId = bubbleShownLoggedBlockId) }
        }
    }

    private fun updateBubbleText() {
        val view = bubble ?: return
        val now = clock()
        val soonest = latestPauses.values.minByOrNull { it.deadlineMs } ?: return
        if (soonest.remainingMs(now) <= 0) return
        view.showContent(soonest.blockId, viewDisplayName(soonest.blockId), PauseFormat.clockText(soonest.remainingMs(now)))
    }

    private fun viewDisplayName(blockId: Long): String =
        latestNames[blockId] ?: getString(R.string.pause_notification_generic_title)

    // — bubble host callbacks (§10) —

    override fun onBubbleTap(blockId: Long) {
        logEvent { log(EventRepository.EVENT_BUBBLE_TAPPED, blockId = blockId) }
        // PRD §6 screen 20: tapping returns to the app (Toki Shrine).
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    override fun onBubbleDragged() {
        logEvent { log(EventRepository.EVENT_BUBBLE_DRAGGED) }
    }

    // — plumbing —

    private fun detailIntent(blockId: Long): PendingIntent =
        PendingIntent.getActivity(
            this,
            (PENDING_INTENT_BASE + blockId).toInt(),
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_OPEN_DETAIL_BLOCK_ID, blockId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun logEvent(write: suspend EventRepository.() -> Unit) {
        serviceScope.launch { runCatching { app.eventRepository.write() } }
    }

    private fun scheduleTicks() {
        mainHandler.removeCallbacks(minuteTick)
        mainHandler.removeCallbacks(bubbleTick)
        mainHandler.postDelayed(minuteTick, MINUTE_MS)
        mainHandler.postDelayed(bubbleTick, SECOND_MS)
    }

    private fun createChannel() {
        notifications.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.pause_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.pause_notification_channel_description)
                setShowBadge(false)
            },
        )
    }

    private fun clock(): Long = SystemClock.elapsedRealtime()

    private fun notificationId(blockId: Long): Int = (NOTIFICATION_ID_BASE + blockId).toInt()

    companion object {
        private const val CHANNEL_ID = "pause_countdown"
        private const val NOTIFICATION_ID_BASE = 20_000
        private const val PENDING_INTENT_BASE = 30_000L
        private const val MINUTE_MS = 60_000L
        private const val SECOND_MS = 1_000L
    }
}
