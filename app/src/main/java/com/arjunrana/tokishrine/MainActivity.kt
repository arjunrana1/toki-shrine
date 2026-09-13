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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arjunrana.tokishrine.data.permissions.AppPermission
import com.arjunrana.tokishrine.data.permissions.BatteryOem
import com.arjunrana.tokishrine.data.permissions.PermissionEventLogic
import com.arjunrana.tokishrine.data.permissions.Permissions
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.ui.screens.AccessibilityExplainerScreen
import com.arjunrana.tokishrine.ui.screens.BatteryInstructionsScreen
import com.arjunrana.tokishrine.ui.screens.BlockDetailScreen
import com.arjunrana.tokishrine.ui.screens.BlockListScreen
import com.arjunrana.tokishrine.ui.screens.ChecklistMode
import com.arjunrana.tokishrine.ui.screens.CreateFlowScreen
import com.arjunrana.tokishrine.ui.screens.PermissionChecklistScreen
import com.arjunrana.tokishrine.ui.screens.SettingsScreen
import com.arjunrana.tokishrine.ui.screens.TurnOnScreen
import com.arjunrana.tokishrine.ui.screens.WelcomeScreen
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import kotlinx.coroutines.launch

// Hand-rolled navigation: a small back stack over the app's routes.
// The screen count doesn't justify a navigation dependency (AGENTS.md §2:
// AndroidX/Compose only, nothing added without asking).
private sealed interface Route {
    data object BlockList : Route
    // startStep: the create flow's entry step (PRD §17 R9) — 1 for the
    // contents editor, 3 when detail's THE FRICTION → Edit opens it directly
    // at configuration.
    data class Create(val editBlockId: Long? = null, val startStep: Int = 1) : Route
    data class TurnOn(val blockId: Long) : Route
    data class Detail(val blockId: Long) : Route
    // Permission checklist is one component reached three ways (PRD §6
    // screen 2 / §12): first-run onboarding, the activation gate, and the
    // Settings permission-health row.
    data class Checklist(val mode: ChecklistMode) : Route
    data object AccessibilityExplainer : Route
    data object BatteryInstructions : Route
    data object Settings : Route
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val app = application as TokiApplication

        setContent {
            NocturneTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val stack = remember { mutableStateListOf<Route>(Route.BlockList) }
                BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

                // — Permission state and §10 permission events (Phase 3) —
                // Displayed state is a hoisted snapshot; every app resume
                // settles outstanding permission requests against actual
                // system state, logs the outcome events, and refreshes the
                // snapshot so checklist rows flip without manual refresh
                // (PRD §6 screen 2).
                val permissionEvents = remember {
                    PermissionEventLogic { name, params ->
                        app.eventRepository.log(name, params = params)
                    }
                }
                var permissionStates by remember {
                    mutableStateOf(Permissions.snapshot(context))
                }

                val lifecycleOwner = LocalLifecycleOwner.current
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

                // First run shows the welcome screen (screens 1–2) until
                // onboarding is completed; never a gate — Back simply
                // reveals the block list (PRD §12).
                var showWelcome by remember { mutableStateOf<Boolean?>(null) }
                LaunchedEffect(Unit) {
                    showWelcome = !app.eventRepository.isOnboardingCompleted()
                }

                // The activation gate (PRD §12): every ON-toggle entry path
                // routes through here. Without accessibility the turn-on
                // confirmation is never shown — the checklist opens instead.
                val openTurnOnOrGate: (Long) -> Unit = { blockId ->
                    if (Permissions.isGranted(context, AppPermission.ACCESSIBILITY)) {
                        stack.add(Route.TurnOn(blockId))
                    } else {
                        stack.add(Route.Checklist(ChecklistMode.GATE))
                    }
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(NocturneTheme.colors.bg),
                ) {
                    when (showWelcome) {
                        null -> {}
                        true -> {
                            WelcomeScreen(
                                onGetStarted = {
                                    scope.launch {
                                        app.eventRepository.log(EventRepository.EVENT_ONBOARDING_STARTED)
                                    }
                                    showWelcome = false
                                    stack.add(Route.Checklist(ChecklistMode.ONBOARDING))
                                },
                            )
                            // Composed after the stack handler, so Back from
                            // the welcome screen reveals the block list
                            // instead of leaving the app.
                            BackHandler { showWelcome = false }
                        }
                        false -> when (val route = stack.last()) {
                            Route.BlockList -> BlockListScreen(
                                blockRepo = app.blockRepository,
                                eventRepo = app.eventRepository,
                                appsRepo = app.installedAppsRepository,
                                onCreate = { stack.add(Route.Create()) },
                                onOpenDetail = { stack.add(Route.Detail(it)) },
                                onTurnOn = openTurnOnOrGate,
                                onOpenSettings = { stack.add(Route.Settings) },
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
                                eventRepo = app.eventRepository,
                                // Final confirmation re-checks accessibility:
                                // the permission can be revoked while this
                                // screen is open (owner instruction, Phase 3).
                                canActivate = {
                                    Permissions.isGranted(context, AppPermission.ACCESSIBILITY)
                                },
                                onPermissionsNeeded = {
                                    stack.add(Route.Checklist(ChecklistMode.GATE))
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
                                onEditContents = { stack.add(Route.Create(editBlockId = it)) },
                                onEditFriction = { stack.add(Route.Create(editBlockId = it, startStep = 3)) },
                            )

                            is Route.Checklist -> PermissionChecklistScreen(
                                mode = route.mode,
                                states = permissionStates,
                                permissionEvents = permissionEvents,
                                eventRepo = app.eventRepository,
                                onStatesChanged = { refreshPermissionStates() },
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
                                    // Pop the explainer first so returning
                                    // from the system screen lands on the
                                    // checklist with refreshed state.
                                    stack.removeAt(stack.lastIndex)
                                    scope.launch {
                                        permissionEvents.requested(AppPermission.ACCESSIBILITY)
                                    }
                                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                            )

                            Route.BatteryInstructions -> BatteryInstructionsScreen(
                                detectedManufacturer = Build.MANUFACTURER,
                                onBack = { stack.removeAt(stack.lastIndex) },
                                onOpenBatterySettings = { oem ->
                                    stack.removeAt(stack.lastIndex)
                                    scope.launch {
                                        permissionEvents.requested(AppPermission.BATTERY)
                                    }
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
                                },
                                onDone = { stack.removeAt(stack.lastIndex) },
                            )

                            Route.Settings -> SettingsScreen(
                                states = permissionStates,
                                eventRepo = app.eventRepository,
                                onBack = { stack.removeAt(stack.lastIndex) },
                                onOpenPermissionHealth = {
                                    stack.add(Route.Checklist(ChecklistMode.SETTINGS))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
