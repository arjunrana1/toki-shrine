package com.arjunrana.tokishrine

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arjunrana.tokishrine.ui.screens.BlockDetailScreen
import com.arjunrana.tokishrine.ui.screens.BlockListScreen
import com.arjunrana.tokishrine.ui.screens.CreateFlowScreen
import com.arjunrana.tokishrine.ui.screens.TurnOnScreen
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

// Hand-rolled navigation: a small back stack over the five Phase 2 routes.
// The screen count doesn't justify a navigation dependency (AGENTS.md §2:
// AndroidX/Compose only, nothing added without asking).
private sealed interface Route {
    data object BlockList : Route
    data class Create(val editBlockId: Long? = null) : Route
    data class TurnOn(val blockId: Long) : Route
    data class Detail(val blockId: Long) : Route
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
                val stack = remember { mutableStateListOf<Route>(Route.BlockList) }
                BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(NocturneTheme.colors.bg),
                ) {
                    when (val route = stack.last()) {
                        Route.BlockList -> BlockListScreen(
                            blockRepo = app.blockRepository,
                            eventRepo = app.eventRepository,
                            appsRepo = app.installedAppsRepository,
                            onCreate = { stack.add(Route.Create()) },
                            onOpenDetail = { stack.add(Route.Detail(it)) },
                            onTurnOn = { stack.add(Route.TurnOn(it)) },
                        )

                        is Route.Create -> CreateFlowScreen(
                            editBlockId = route.editBlockId,
                            blockRepo = app.blockRepository,
                            eventRepo = app.eventRepository,
                            appsRepo = app.installedAppsRepository,
                            onClose = { stack.removeAt(stack.lastIndex) },
                        )

                        is Route.TurnOn -> TurnOnScreen(
                            blockId = route.blockId,
                            blockRepo = app.blockRepository,
                            eventRepo = app.eventRepository,
                            onClose = { stack.removeAt(stack.lastIndex) },
                        )

                        is Route.Detail -> BlockDetailScreen(
                            blockId = route.blockId,
                            blockRepo = app.blockRepository,
                            eventRepo = app.eventRepository,
                            appsRepo = app.installedAppsRepository,
                            onBack = { stack.removeAt(stack.lastIndex) },
                            onTurnOn = { stack.add(Route.TurnOn(it)) },
                            onEdit = { stack.add(Route.Create(it)) },
                        )
                    }
                }
            }
        }
    }
}
