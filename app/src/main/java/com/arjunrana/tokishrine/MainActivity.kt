package com.arjunrana.tokishrine

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.arjunrana.tokishrine.data.permissions.AppPermission
import com.arjunrana.tokishrine.data.permissions.BatteryInstructions
import com.arjunrana.tokishrine.data.permissions.BatteryOem
import com.arjunrana.tokishrine.data.permissions.PermissionEventLogic
import com.arjunrana.tokishrine.data.permissions.Permissions
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.ui.navigation.ChecklistMode
import com.arjunrana.tokishrine.ui.navigation.Route
import com.arjunrana.tokishrine.ui.navigation.RouteStackSaver
import com.arjunrana.tokishrine.ui.screens.AccessibilityExplainerScreen
import com.arjunrana.tokishrine.ui.screens.BatteryInstructionsScreen
import com.arjunrana.tokishrine.ui.screens.BlockDetailScreen
import com.arjunrana.tokishrine.ui.screens.BlockListScreen
import com.arjunrana.tokishrine.ui.screens.CreateFlowScreen
import com.arjunrana.tokishrine.ui.screens.FeedbackScreen
import com.arjunrana.tokishrine.ui.screens.PermissionChecklistScreen
import com.arjunrana.tokishrine.ui.screens.SettingsScreen
import com.arjunrana.tokishrine.ui.screens.StatsScreen
import com.arjunrana.tokishrine.ui.screens.TurnOnScreen
import com.arjunrana.tokishrine.ui.screens.WelcomeScreen
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import com.arjunrana.tokishrine.ui.util.FeedbackEmail
import com.arjunrana.tokishrine.ui.util.feedbackMailIntent
import kotlinx.coroutines.launch
import android.content.pm.PackageManager

// Hand-rolled navigation over a saveable route stack (review blocker 1):
// routes encode to strings (RouteCodec), so a recreated activity restores
// the same flow — checklist mode, block id, explainer/battery position —
// without a navigation dependency (AGENTS.md §2).
//
// Outstanding permission requests survive recreation the same way: event
// values in a saveable list, settled exactly once against real system
// state on the next resume.
private val PendingPermissionsSaver: Saver<SnapshotStateList<AppPermission>, ArrayList<String>> =
    Saver(
        save = { pending ->
            ArrayList<String>().apply { pending.forEach { add(it.eventValue) } }
        },
        restore = { saved ->
            SnapshotStateList<AppPermission>().apply {
                addAll(saved.mapNotNull { value ->
                    AppPermission.values().firstOrNull { it.eventValue == value }
                })
            }
        },
    )

class MainActivity : ComponentActivity() {

    // Phase 6 pause-notification tap destination (PRD §6 screen 21): the
    // block's detail screen. Compose-owned, cleared once consumed so the
    // same detail never stacks twice.
    private var pendingDetailBlockId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readDetailHint(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val app = application as TokiApplication

        setContent {
            NocturneTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val lifecycleOwner = LocalLifecycleOwner.current

                val stack = rememberSaveable(saver = RouteStackSaver) {
                    mutableStateListOf(Route.BlockList)
                }
                BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

                // — Permission state and §10 permission events (Phase 3) —
                // Displayed state is a hoisted snapshot; every app resume
                // settles outstanding permission requests against actual
                // system state, logs the outcome events, and refreshes the
                // snapshot so checklist rows flip without manual refresh
                // (PRD §6 screen 2). The pending list is saveable, so a
                // recreation mid-request settles it after restoration.
                val pendingPermissions = rememberSaveable(saver = PendingPermissionsSaver) {
                    mutableStateListOf<AppPermission>()
                }
                val permissionEvents = remember(pendingPermissions) {
                    PermissionEventLogic(pendingPermissions) { name, params ->
                        app.eventRepository.log(name, params = params)
                    }
                }
                var permissionStates by remember {
                    mutableStateOf(Permissions.snapshot(context))
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            scope.launch {
                                permissionEvents.settle { Permissions.isGranted(context, it) }
                                permissionStates = Permissions.snapshot(context)
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                fun refreshPermissionStates() {
                    permissionStates = Permissions.snapshot(context)
                }

                // Launches a system permission UI (review blocker 1): the
                // request is marked pending synchronously and the
                // permission_requested event durably recorded before the
                // system UI opens; a failed record or launch rolls the mark
                // back so no phantom granted/denied can settle later.
                val requestPermission: (AppPermission, () -> Unit) -> Unit = { permission, launchSystemUi ->
                    permissionEvents.markPending(permission)
                    lifecycleOwner.lifecycleScope.launch {
                        try {
                            permissionEvents.emitRequested(permission)
                        } catch (t: Throwable) {
                            permissionEvents.unmarkPending(permission)
                            return@launch
                        }
                        if (runCatching(launchSystemUi).isFailure) {
                            permissionEvents.unmarkPending(permission)
                        }
                    }
                }

                // First run shows the welcome screen until onboarding is
                // completed (app_meta flag); never a gate (PRD §12).
                // launchResolved is itself saved, but flips to true only
                // after the decision below has fully finished — a cancelled
                // lookup leaves it unresolved, so the recreated activity
                // retries instead of skipping Welcome; a restored non-root
                // stack resolves without re-running the root decision.
                var launchResolved by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (!launchResolved) {
                        if (stack.size == 1 && stack[0] == Route.BlockList) {
                            // Genuine root launch: complete the suspended
                            // onboarding lookup and the route it decides
                            // before marking the launch resolved.
                            if (!app.eventRepository.isOnboardingCompleted()) {
                                stack.add(Route.Welcome)
                            }
                        }
                        // Any other stack shape is a restored session that
                        // already left the root — resolve without pushing
                        // Welcome or repeating the lookup.
                        launchResolved = true
                    }
                }

                // The activation gate (PRD §12): every ON-toggle entry path
                // routes through here. Without accessibility the turn-on
                // confirmation is never shown — the checklist opens instead
                // (once; a repeated tap cannot stack a second one).
                val openTurnOnOrGate: (Long) -> Unit = { blockId ->
                    if (Permissions.isGranted(context, AppPermission.ACCESSIBILITY)) {
                        stack.add(Route.TurnOn(blockId))
                    } else if (stack.last() !is Route.Checklist) {
                        stack.add(Route.Checklist(ChecklistMode.GATE))
                    }
                }

                // A pause-notification tap (Phase 6) lands on that block's
                // detail screen once the normal launch resolution has run,
                // then clears the hint so it cannot replay on recreation.
                LaunchedEffect(pendingDetailBlockId, launchResolved) {
                    val blockId = pendingDetailBlockId
                    if (blockId != null && launchResolved) {
                        pendingDetailBlockId = null
                        if (stack.last() != Route.Detail(blockId)) {
                            stack.add(Route.Detail(blockId))
                        }
                    }
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(NocturneTheme.colors.bg),
                ) {
                    if (!launchResolved) {
                        // Welcome decision still pending — keep the frame
                        // empty rather than flashing the block list.
                    } else when (val route = stack.last()) {
                        Route.Welcome -> WelcomeScreen(
                            onGetStarted = {
                                // The synchronous stack check makes a rapid
                                // double tap log onboarding_started and push
                                // the checklist exactly once.
                                if (stack.last() is Route.Welcome) {
                                    scope.launch {
                                        app.eventRepository.log(EventRepository.EVENT_ONBOARDING_STARTED)
                                    }
                                    stack.removeAt(stack.lastIndex)
                                    stack.add(Route.Checklist(ChecklistMode.ONBOARDING))
                                }
                            },
                        )

                        Route.BlockList -> BlockListScreen(
                            blockRepo = app.blockRepository,
                            appsRepo = app.installedAppsRepository,
                            onCreate = { stack.add(Route.Create()) },
                            onOpenDetail = { stack.add(Route.Detail(it)) },
                            onTurnOn = openTurnOnOrGate,
                            onTurnOff = { startActivity(BlockActivity.turnOffIntent(context, it)) },
                            onOpenStats = { stack.add(Route.Stats) },
                            onOpenSettings = { stack.add(Route.Settings) },
                            onOpenFeedback = { stack.add(Route.Feedback) },
                        )

                        is Route.Create -> CreateFlowScreen(
                            editBlockId = route.editBlockId,
                            blockRepo = app.blockRepository,
                            eventRepo = app.eventRepository,
                            appsRepo = app.installedAppsRepository,
                            onClose = { stack.removeAt(stack.lastIndex) },
                            initialStep = route.startStep,
                        )

                        is Route.TurnOn -> TurnOnScreen(
                            blockId = route.blockId,
                            blockRepo = app.blockRepository,
                            // Final confirmation re-checks accessibility:
                            // the permission can be revoked while this
                            // screen is open (owner instruction, Phase 3).
                            canActivate = {
                                Permissions.isGranted(context, AppPermission.ACCESSIBILITY)
                            },
                            onPermissionsNeeded = {
                                if (stack.last() !is Route.Checklist) {
                                    stack.add(Route.Checklist(ChecklistMode.GATE))
                                }
                            },
                            onClose = { stack.removeAt(stack.lastIndex) },
                        )

                        is Route.Detail -> BlockDetailScreen(
                            blockId = route.blockId,
                            blockRepo = app.blockRepository,
                            eventRepo = app.eventRepository,
                            appsRepo = app.installedAppsRepository,
                            onBack = { stack.removeAt(stack.lastIndex) },
                            onTurnOn = openTurnOnOrGate,
                            onTurnOff = { startActivity(BlockActivity.turnOffIntent(context, it)) },
                            onEditContents = { stack.add(Route.Create(editBlockId = it)) },
                            onEditFriction = { stack.add(Route.Create(editBlockId = it, startStep = 3)) },
                        )

                        is Route.Checklist -> PermissionChecklistScreen(
                            mode = route.mode,
                            states = permissionStates,
                            permissionEvents = permissionEvents,
                            eventRepo = app.eventRepository,
                            onStatesChanged = { refreshPermissionStates() },
                            onRequestPermission = requestPermission,
                            onOpenAccessibilityExplainer = {
                                stack.add(Route.AccessibilityExplainer)
                            },
                            onOpenBatteryInstructions = {
                                stack.add(Route.BatteryInstructions)
                            },
                            onBack = { stack.removeAt(stack.lastIndex) },
                            onContinue = {
                                if (route.mode == ChecklistMode.ONBOARDING) {
                                    while (stack.size > 1) stack.removeAt(stack.lastIndex)
                                } else {
                                    stack.removeAt(stack.lastIndex)
                                }
                            },
                        )

                        Route.AccessibilityExplainer -> AccessibilityExplainerScreen(
                            onBack = { stack.removeAt(stack.lastIndex) },
                            onContinueToSettings = {
                                // Pop the explainer first so returning from
                                // the system screen lands on the checklist
                                // with refreshed state; the request itself is
                                // marked and recorded by requestPermission.
                                stack.removeAt(stack.lastIndex)
                                requestPermission(AppPermission.ACCESSIBILITY) {
                                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                }
                            },
                        )

                        Route.BatteryInstructions -> {
                            // Authored OEM text comes from the bundled
                            // detection JSON (Phase 4); null means still
                            // loading or a corrupt asset, and the screen
                            // degrades to its frame without the text.
                            var instructionsByOem by remember {
                                mutableStateOf<Map<BatteryOem, BatteryInstructions>?>(null)
                            }
                            LaunchedEffect(Unit) {
                                instructionsByOem = runCatching { app.detectionConfigLoader.load() }
                                    .getOrNull()
                                    ?.batteryInstructions
                            }
                            BatteryInstructionsScreen(
                                detectedManufacturer = Build.MANUFACTURER,
                                instructionsByOem = instructionsByOem,
                                onBack = { stack.removeAt(stack.lastIndex) },
                                onOpenBatterySettings = { oem ->
                                    stack.removeAt(stack.lastIndex)
                                    requestPermission(AppPermission.BATTERY) {
                                        startActivity(
                                            when (oem) {
                                                // Samsung's authored steps walk the
                                                // app-info → Battery path (PRD §6
                                                // screen 4); every other maker gets
                                                // the universal Android dialog.
                                                BatteryOem.SAMSUNG -> Intent(
                                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                    Uri.parse("package:$packageName"),
                                                )
                                                BatteryOem.GENERIC -> Intent(
                                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                    Uri.parse("package:$packageName"),
                                                )
                                            },
                                        )
                                    }
                                },
                            )
                        }

                        Route.Settings -> SettingsScreen(
                            states = permissionStates,
                            eventRepo = app.eventRepository,
                            onBack = { stack.removeAt(stack.lastIndex) },
                            onOpenPermissionHealth = {
                                stack.add(Route.Checklist(ChecklistMode.SETTINGS))
                            },
                            onOpenFeedback = { stack.add(Route.Feedback) },
                        )

                        // Screen 23: the §9 figures over the event store.
                        Route.Stats -> StatsScreen(
                            eventRepo = app.eventRepository,
                            appsRepo = app.installedAppsRepository,
                            onBack = { stack.removeAt(stack.lastIndex) },
                        )

                        // Screen 25: the §13 email-intent handoff. The
                        // populated intent goes to an external mail app only
                        // when one is visible (the mailto <queries> block);
                        // feedback_sent is logged solely for a successful
                        // handoff, never for confirmed delivery, and a failed
                        // handoff leaves the screen and the draft in place.
                        Route.Feedback -> FeedbackScreen(
                            eventRepo = app.eventRepository,
                            onSend = { body ->
                                val intent = feedbackMailIntent(body)
                                val hasHandler = runCatching {
                                    packageManager.resolveActivity(
                                        intent,
                                        PackageManager.ResolveInfoFlags.of(0),
                                    ) != null
                                }.getOrDefault(false)
                                val launchSucceeded = hasHandler &&
                                    runCatching { startActivity(intent) }.isSuccess
                                if (FeedbackEmail.handedOff(hasHandler, launchSucceeded)) {
                                    scope.launch {
                                        runCatching {
                                            app.eventRepository.log(EventRepository.EVENT_FEEDBACK_SENT)
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            },
                            onSent = { stack.removeAt(stack.lastIndex) },
                            onBack = { stack.removeAt(stack.lastIndex) },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readDetailHint(intent)
    }

    private fun readDetailHint(intent: Intent?) {
        pendingDetailBlockId = intent
            ?.getLongExtra(EXTRA_OPEN_DETAIL_BLOCK_ID, -1L)
            ?.takeIf { it >= 0 }
    }

    companion object {
        const val EXTRA_OPEN_DETAIL_BLOCK_ID = "open_detail_block_id"
    }
}
