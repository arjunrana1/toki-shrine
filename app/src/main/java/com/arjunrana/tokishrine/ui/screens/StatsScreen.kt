package com.arjunrana.tokishrine.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.arjunrana.tokishrine.data.apps.InstalledAppsRepository
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.data.stats.StatsDashboard
import com.arjunrana.tokishrine.data.stats.StatsDurationFormat
import com.arjunrana.tokishrine.data.stats.StatsLedger
import com.arjunrana.tokishrine.data.stats.StatsRepository
import com.arjunrana.tokishrine.data.stats.StatsScreenState
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.components.NocturneButton
import com.arjunrana.tokishrine.ui.components.NocturneCtaButton
import com.arjunrana.tokishrine.ui.components.ButtonVariant
import com.arjunrana.tokishrine.ui.components.SectionLabel
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/*
 * Screen 23 "Your time" (PRD §9, approved replacement of 4 October 2026).
 * The sole data API is TokiApplication.statsRepository: entry/resume refresh
 * is owned by MainActivity.onResume, and this screen only collects the
 * state flow, renders the five §9 states and emits the §10 local Stats
 * events. Metric rules (rounding, null handling, dedup, baseline) live in
 * the data layer — display mapping helpers are in StatsPresentation.kt.
 *
 * Layout follows the owner-supplied S1–S6 PNGs; PRD §9 copy overrides the
 * older sheet/abandonment language baked into those exports.
 */

private enum class StatsSheetKind { TimeSaved, NopeRate }

@Composable
fun StatsScreen(
    statsRepo: StatsRepository,
    eventRepo: EventRepository,
    appsRepo: InstalledAppsRepository,
    onBack: () -> Unit,
) {
    val colors = NocturneTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val state by statsRepo.state.collectAsState()

    // §10 feature engagement: tap-side logging contained in a background
    // coroutine; a failed event write must never take the screen down.
    val logEvent: (String, Map<String, Any?>) -> Unit = { name, params ->
        scope.launch { runCatching { eventRepo.log(name, params = params) } }
    }

    // stats_viewed: once per entry after the state resolves (§10), never per
    // StateFlow emission. A recreation may log another row — the accepted
    // settings_viewed convention. An Error is not one of §10's resolved
    // states, so the log waits for the next resolved one (retry outcome).
    var viewedLogged by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        val stateParam = when (val resolved = state) {
            StatsScreenState.Loading -> return@LaunchedEffect
            is StatsScreenState.Error -> return@LaunchedEffect
            StatsScreenState.NoUsageAccess -> "no_access"
            StatsScreenState.NoBlocks -> "no_blocks"
            is StatsScreenState.Ready -> statsViewedState(
                resolved.dashboard,
                LocalDate.now().minusDays(6).atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli(),
            )
        }
        if (!viewedLogged) {
            viewedLogged = true
            runCatching {
                eventRepo.log(EventRepository.EVENT_STATS_VIEWED, params = mapOf("state" to stateParam))
            }
        }
    }

    // Recalibration is confirm-gated (PRD §9): tap logs stats_recalibrate_tapped,
    // the confirmation dialog starts the repository call, repeat taps are
    // disabled while it runs, and failure reports without changing anything.
    // baseline_recalibrated is emitted by the repository alone.
    var confirmRecalibrate by rememberSaveable { mutableStateOf(false) }
    var recalibrating by remember { mutableStateOf(false) }
    var recalibrateFailed by remember { mutableStateOf(false) }

    var infoSheet by rememberSaveable { mutableStateOf<StatsSheetKind?>(null) }

    // P7-F6: the per-app visit-length sheet. The selection survives
    // recreation as the package name and re-resolves against the latest
    // dashboard, so a refresh under the sheet cannot desync the row.
    var visitSheetPackage by rememberSaveable { mutableStateOf<String?>(null) }

    fun openInfoSheet(kind: StatsSheetKind, which: String) {
        logEvent(EventRepository.EVENT_STATS_INFO_OPENED, mapOf("which" to which))
        infoSheet = kind
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when (val current = state) {
            StatsScreenState.Loading -> LoadingContent(onBack)
            is StatsScreenState.Error -> ErrorContent(
                message = current.message,
                onBack = onBack,
                onRetry = { scope.launch { statsRepo.refresh() } },
            )
            StatsScreenState.NoUsageAccess -> NoUsageAccessContent(
                eventRepo = eventRepo,
                onBack = onBack,
                onAllowUsageAccess = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                },
            )
            StatsScreenState.NoBlocks -> NoBlocksContent(onBack = onBack)
            is StatsScreenState.Ready -> ReadyContent(
                dashboard = current.dashboard,
                appsRepo = appsRepo,
                recalibrating = recalibrating,
                recalibrateFailed = recalibrateFailed,
                onBack = onBack,
                onOpenInfoSheet = { kind, which -> openInfoSheet(kind, which) },
                onOpenVisitSheet = { visitSheetPackage = it },
                onRecalibrateTapped = {
                    logEvent(EventRepository.EVENT_STATS_RECALIBRATE_TAPPED, emptyMap())
                    confirmRecalibrate = true
                },
            )
        }

        // The nope-rate sheet reads the latest resolved dashboard; it only
        // opens from the Ready screen, so this is normally the same model.
        val readyDashboard = (state as? StatsScreenState.Ready)?.dashboard
        infoSheet?.let { kind ->
            if (readyDashboard != null) {
                InfoSheet(onDismiss = { infoSheet = null }) {
                    when (kind) {
                        StatsSheetKind.TimeSaved -> TimeSavedSheetContent()
                        StatsSheetKind.NopeRate -> NopeRateSheetContent(readyDashboard)
                    }
                    Spacer(Modifier.height(16.dp))
                    NocturneCtaButton(
                        "Got it",
                        variant = ButtonVariant.SECONDARY,
                        onClick = { infoSheet = null },
                    )
                }
            }
        }

        // P7-F6 visit-length sheet: resolves the selected package against the
        // same dashboard the list rendered. A package that vanished in a
        // refresh simply closes the sheet. No §10 event exists for opening
        // it or setting/clearing an override (F-B handback point 3).
        visitSheetPackage?.let { packageName ->
            val row = readyDashboard?.apps
                ?.firstOrNull { it.packageName == packageName }
                ?.let { statsAppRows(listOf(it), appsRepo::labelFor).single() }
            if (row != null) {
                InfoSheet(onDismiss = { visitSheetPackage = null }) {
                    VisitLengthSheetContent(
                        row = row,
                        statsRepo = statsRepo,
                        onDone = { visitSheetPackage = null },
                    )
                }
            }
        }

        if (confirmRecalibrate) {
            Dialog(onDismissRequest = { confirmRecalibrate = false }) {
                Column(
                    Modifier
                        .width(280.dp)
                        .background(NocturneTheme.colors.surface, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                ) {
                    Text(
                        "Recalibrate time spent on apps?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = NocturneTheme.colors.text,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "We'll re-measure your usual time per app from your last 7 days. " +
                            "Your saved time won't change.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = NocturneTheme.colors.neutral.step400,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NocturneButton(
                            "Recalibrate",
                            modifier = Modifier.weight(1f),
                            height = 42.dp,
                            onClick = {
                                confirmRecalibrate = false
                                recalibrateFailed = false
                                recalibrating = true
                                scope.launch {
                                    val replaced = try {
                                        statsRepo.recalibrate()
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        false
                                    }
                                    recalibrating = false
                                    if (!replaced) recalibrateFailed = true
                                }
                            },
                        )
                        NocturneButton(
                            "Cancel",
                            variant = ButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f),
                            height = 42.dp,
                            onClick = { confirmRecalibrate = false },
                        )
                    }
                }
            }
        }
    }
}

// — S1: the full dashboard (also the S4 tough-week/zero layout) —

@Composable
private fun ReadyContent(
    dashboard: StatsDashboard,
    appsRepo: InstalledAppsRepository,
    recalibrating: Boolean,
    recalibrateFailed: Boolean,
    onBack: () -> Unit,
    onOpenInfoSheet: (StatsSheetKind, String) -> Unit,
    onOpenVisitSheet: (String) -> Unit,
    onRecalibrateTapped: () -> Unit,
) {
    val colors = NocturneTheme.colors

    // Display-name resolution and tie-ordering are presentation concerns;
    // remember per apps list so PackageManager lookups don't repeat per frame.
    val appRows = remember(dashboard.apps) { statsAppRows(dashboard.apps, appsRepo::labelFor) }
    val weekdays = remember(dashboard.days) { weekdayLabels(dashboard.days.map { it.date }) }
    val savedBars = remember(dashboard.days) { statsBarSpecs(dashboard.days.map { it.savedMs }) }
    val spentBars = remember(dashboard.days) { statsBarSpecs(dashboard.days.map { it.spentMs }) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        NocturneAppbar(
            title = "Your time",
            onBack = onBack,
            // PRD §9: a static label — never a control.
            trailing = {
                Text(
                    "Weekly info",
                    fontSize = 12.sp,
                    color = colors.neutral.step500,
                )
            },
        )
        Spacer(Modifier.height(8.dp))

        // — Today hero (S1 v2, 8 October §17 addendum: local midnight) —
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Time saved today",
                fontSize = 12.5.sp,
                color = colors.neutral.step400,
            )
            StatsInfoButton(
                description = "About time saved today",
                onClick = { onOpenInfoSheet(StatsSheetKind.TimeSaved, "time_saved_hero") },
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = StatsDurationFormat.duration(dashboard.todaySavedMs),
                style = tabular(fontSize = 68.0, weight = FontWeight.Normal),
                color = colors.accentRamp.step200,
            )
            Text(
                "today",
                fontSize = 15.sp,
                color = colors.neutral.step500,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            )
        }
        when {
            !dashboard.usageAvailable -> Text(
                // §9/§10: neutral unavailability note; saved/outcome figures
                // above remain honest, nothing is invented below.
                "Usage data isn't available right now",
                fontSize = 12.sp,
                color = colors.neutral.step500,
                modifier = Modifier.padding(top = 6.dp),
            )
            else -> todayScreenTimeLine(dashboard.todayScreenMs)?.let { line ->
                // P7-F13: "Screen time X today"; the "vs your usual"
                // comparison is gone, and an unknown day hides the line.
                Text(
                    line,
                    fontSize = 12.sp,
                    color = colors.neutral.step500,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        if (!dashboard.blocksOn) {
            // P7-F10: outcomes this week keep the dashboard alive even with
            // every block off — say so instead of showing the S6 empty state.
            Text(
                "No blocks are on",
                fontSize = 12.sp,
                color = colors.neutral.step400,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(24.dp))

        // — Today tiles (S1 v2, P7-F22): equal height on the tallest tile;
        //    number first, label below, the bar at the right tile's bottom —
        Row(
            Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(colors.surface, RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text = dashboard.todayAttempts.toString(),
                    style = tabular(fontSize = 24.0, weight = FontWeight.Medium),
                    color = colors.text,
                )
                Text(
                    "Attempts today",
                    fontSize = 11.5.sp,
                    color = colors.neutral.step500,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(colors.surface, RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                Text(
                    // §9/§10: an em dash with an empty bar when today has no
                    // resolved attempts — never a fabricated 0%.
                    text = if (dashboard.todayAttempts == 0) "—" else "${dashboard.todayNopeRatePercent}%",
                    style = tabular(fontSize = 24.0, weight = FontWeight.Medium),
                    color = colors.text,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Nope rate today",
                        fontSize = 11.5.sp,
                        color = colors.neutral.step500,
                    )
                    StatsInfoButton(
                        description = "About nope rate",
                        iconSize = 13,
                        onClick = { onOpenInfoSheet(StatsSheetKind.NopeRate, "nope_rate") },
                    )
                }
                Spacer(Modifier.weight(1f))
                RateBar(
                    fraction = if (dashboard.todayAttempts == 0) 0f else dashboard.todayNopeRatePercent / 100f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        // — This week (S1 v2): average saved per day and the N-of-M rate —
        SectionLabel("THIS WEEK")
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            WeekStatLine(
                label = "Avg time saved",
                value = "${StatsDurationFormat.duration(dashboard.savedPerDayMs)} / day",
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 11.dp)
                    .height(1.dp)
                    .background(colors.divider),
            )
            WeekStatLine(
                label = "Nope rate",
                value = weekNopeRateLine(dashboard.nopeRatePercent, dashboard.attempts),
            )
        }
        Spacer(Modifier.height(24.dp))

        // — Blocked apps (P7-F20): rows wrap their content (no fixed row
        //    height, so large fonts never clip); the box caps at about five
        //    rows and scrolls on its own, collapsing when there are few
        //    apps. Tapping anywhere on a row opens the visit sheet —
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(
                    "TIME SAVED ON BLOCKED APPS",
                    modifier = Modifier.weight(1f),
                )
                StatsInfoButton(
                    description = "About time saved on blocked apps",
                    iconSize = 14,
                    onClick = { onOpenInfoSheet(StatsSheetKind.TimeSaved, "time_saved_list") },
                )
            }
            Text(
                "Visit times look off? Tap an app to set your own.",
                fontSize = 12.sp,
                color = colors.neutral.step400,
            )
            val listScroll = rememberScrollState()
            Box {
                Column(
                    Modifier
                        .heightIn(max = (LIST_BOX_ROWS * APP_ROW_APPROX_HEIGHT).dp)
                        .verticalScroll(listScroll),
                ) {
                    appRows.forEach { row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenVisitSheet(row.packageName) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = row.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.text,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp),
                                ) {
                                    // Weighted, so it is measured after the
                                    // chip: at large font the subtitle wraps
                                    // and the chip keeps its width (P7-N16).
                                    Text(
                                        text = appRowSubtitle(row),
                                        fontSize = 11.5.sp,
                                        color = colors.neutral.step500,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    VisitChip(text = row.visitChip)
                                }
                            }
                            Text(
                                text = StatsDurationFormat.duration(row.savedMs),
                                style = tabular(fontSize = 14.0, weight = FontWeight.Normal),
                                color = colors.accentRamp.step200,
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.divider),
                        )
                    }
                }
                // Mock's overflow fade: pure background — no pointer input,
                // so list scrolling under it stays live. Drawn only while more
                // rows sit below, so it never covers the last row (P7-G12-1).
                if (listScroll.canScrollForward) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(34.dp)
                            .background(
                                Brush.verticalGradient(0f to Color.Transparent, 1f to colors.bg),
                            ),
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        WeekBarChartCard(
            title = "Time saved",
            totalLabel = StatsDurationFormat.duration(dashboard.savedMs),
            totalColor = colors.accentRamp.step200,
            weekdayLabels = weekdays,
            bars = savedBars,
            barColor = { isToday, filled ->
                when {
                    !filled -> colors.neutral.step800
                    isToday -> colors.accent
                    else -> colors.accentRamp.step700
                }
            },
            valueColor = { isToday -> if (isToday) colors.accentRamp.step200 else colors.neutral.step400 },
            weekdayColor = { isToday -> if (isToday) colors.neutral.step100 else colors.neutral.step500 },
            glowToday = true,
        )
        Spacer(Modifier.height(24.dp))

        WeekBarChartCard(
            title = "Time spent after the challenge",
            // Null total means some push-throughs lack measurement: an em
            // dash, never a partial sum presented as the whole (§9).
            totalLabel = dashboard.spentMs?.let(StatsDurationFormat::duration) ?: "—",
            totalColor = colors.neutral.step400,
            weekdayLabels = weekdays,
            bars = spentBars,
            barColor = { isToday, filled ->
                when {
                    !filled -> colors.neutral.step800
                    isToday -> colors.neutral.step400
                    else -> colors.neutral.step700
                }
            },
            valueColor = { colors.neutral.step400 },
            weekdayColor = { isToday -> if (isToday) colors.neutral.step100 else colors.neutral.step500 },
            glowToday = false,
            note = if (dashboard.unmeasuredPasses > 0) "Some visits couldn't be measured" else null,
        )
        Spacer(Modifier.height(24.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.neutral.step800, RoundedCornerShape(14.dp))
                .padding(16.dp),
        ) {
            Text(
                "Recalibrate time spent on apps",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Usual time per app seems off? Hit recalibrate and we'll re-measure it " +
                    "from your last 7 days.",
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                color = colors.neutral.step400,
            )
            if (recalibrateFailed) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Recalibration didn't finish, so your usual time hasn't changed.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            // P7-F25: the card's CTA keeps the shared 50dp/10dp/15sp pair
            // spec, ≥16dp below the content above it.
            Spacer(Modifier.height(16.dp))
            NocturneCtaButton(
                "Recalibrate",
                variant = ButtonVariant.SECONDARY,
                enabled = !recalibrating,
                onClick = onRecalibrateTapped,
            )
        }
        Spacer(Modifier.height(22.dp))
    }
}

// — S5: Usage Access missing/revoked takes precedence over the dashboard —

@Composable
private fun NoUsageAccessContent(
    eventRepo: EventRepository,
    onBack: () -> Unit,
    onAllowUsageAccess: () -> Unit,
) {
    val colors = NocturneTheme.colors
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        NocturneAppbar(title = "Your time", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(16.dp))
                .border(1.dp, colors.neutral.step800, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "See how much time you're saving",
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text,
            )
            Text(
                "Allow usage access so we can see how long you usually spend in each app. " +
                    "We only read app time, never what you do inside them.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = colors.neutral.step400,
            )
            NocturneButton(
                "Allow usage access",
                block = true,
                height = 44.dp,
                onClick = {
                    scope.launch {
                        runCatching { eventRepo.log(EventRepository.EVENT_STATS_USAGE_ACCESS_CTA_TAPPED) }
                    }
                    onAllowUsageAccess()
                },
            )
        }
    }
}

// — S6: permission granted but no enabled block yet —

@Composable
private fun NoBlocksContent(onBack: () -> Unit) {
    val colors = NocturneTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        NocturneAppbar(title = "Your time", onBack = onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(Modifier.padding(bottom = 60.dp)) {
                Text(
                    "Nothing to brag about... yet.",
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.text,
                )
                Text(
                    "Create a block to see how much time you save.",
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = colors.neutral.step400,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

// §9: loading is a blank content area — no spinner, no fake metrics.

@Composable
private fun LoadingContent(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        NocturneAppbar(title = "Your time", onBack = onBack)
    }
}

// §9: load failure gets a retry state, never zero metrics.

@Composable
private fun ErrorContent(message: String, onBack: () -> Unit, onRetry: () -> Unit) {
    val colors = NocturneTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        NocturneAppbar(title = "Your time", onBack = onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    message,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = colors.neutral.step400,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                NocturneButton(
                    "Retry",
                    variant = ButtonVariant.SECONDARY,
                    height = 42.dp,
                    onClick = onRetry,
                )
            }
        }
    }
}

// — Chart card —
// Bar geometry (stub heights, proportional scaling, glow) comes from
// statsBarSpecs; colors differ between the saved (accent) and spent
// (neutral) charts, so they are passed in as small mapping lambdas.

@Composable
private fun WeekBarChartCard(
    title: String,
    totalLabel: String,
    totalColor: Color,
    weekdayLabels: List<String>,
    bars: List<StatsBarSpec>,
    barColor: (isToday: Boolean, filled: Boolean) -> Color,
    valueColor: (isToday: Boolean) -> Color,
    weekdayColor: (isToday: Boolean) -> Color,
    glowToday: Boolean,
    note: String? = null,
) {
    val colors = NocturneTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            Text(
                totalLabel,
                style = tabular(fontSize = 12.0, weight = FontWeight.Normal),
                color = totalColor,
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(108.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEachIndexed { index, bar ->
                val isToday = index == bars.lastIndex
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        bar.valueLabel,
                        fontSize = 10.sp,
                        color = valueColor(isToday),
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(5.dp))
                    Box {
                        if (glowToday && isToday && bar.filled) {
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .background(colors.accent, RoundedCornerShape(4.dp))
                                    .blur(7.dp, BlurredEdgeTreatment.Unbounded),
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(bar.heightDp.dp)
                                .background(barColor(isToday, bar.filled), RoundedCornerShape(4.dp)),
                        )
                    }
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            weekdayLabels.forEachIndexed { index, label ->
                Text(
                    label,
                    fontSize = 10.sp,
                    color = weekdayColor(index == weekdayLabels.lastIndex),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (note != null) {
            Text(
                note,
                fontSize = 11.5.sp,
                color = colors.neutral.step500,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// — Info sheets (S2/S3): scrim tap, Got it and swipe-down dismissal —

@Composable
private fun InfoSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val colors = NocturneTheme.colors
    val density = LocalDensity.current
    val dismissPx = with(density) { 120.dp.toPx() }
    var dragY by remember { mutableStateOf(0f) }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onDismiss() },
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset { IntOffset(0, dragY.roundToInt()) }
                .background(colors.surface, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                // Consume taps on the sheet itself so they don't fall
                // through to the scrim's dismiss.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            dragY = (dragY + amount).coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            if (dragY > dismissPx) onDismiss() else dragY = 0f
                        },
                        onDragCancel = { dragY = 0f },
                    )
                }
                .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 28.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .background(colors.neutral.step700, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun TimeSavedSheetContent() {
    val colors = NocturneTheme.colors
    // S2 v2 (8 October §17 addendum): the corrected copy with generous
    // spacing; the "Tried again within 5 minutes…" line is removed.
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            "Time saved",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = colors.text,
        )
        Text(
            "Each ‘nope’ saves you a visit. We count the time that visit usually takes.",
            fontSize = 13.5.sp,
            lineHeight = 21.sp,
            color = colors.neutral.step300,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.neutral.step900, RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FormulaChip("Nopes")
            Text("×", fontSize = 12.sp, color = colors.neutral.step500)
            FormulaChip("Usual visit length")
            Text("=", fontSize = 12.sp, color = colors.neutral.step500)
            FormulaChip("Time saved", accent = true)
        }
        Text(
            "Usual visit length comes from your 7 days before Toki. You can set your own per app.",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = colors.neutral.step400,
        )
    }
}

@Composable
private fun NopeRateSheetContent(dashboard: StatsDashboard) {
    val colors = NocturneTheme.colors
    // Opened from the "Nope rate today" tile: explain today's figures (P7-N12).
    val today = todayNopeRateSheetFigures(dashboard)
    val rateFraction = if (today.attempts == 0) 0f else today.ratePercent / 100f
    Text(
        "Nope rate",
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = colors.text,
    )
    Text(
        "How often you walked away instead of pushing through the challenge.",
        fontSize = 13.5.sp,
        lineHeight = 21.sp,
        color = colors.neutral.step300,
    )
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.neutral.step900, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(colors.neutral.step700, RoundedCornerShape(4.dp)),
        ) {
            if (rateFraction > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(rateFraction)
                        .fillMaxHeight()
                        .background(colors.accent, RoundedCornerShape(4.dp)),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                "${today.nopes} nopes",
                fontSize = 12.sp,
                color = colors.accentRamp.step200,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${today.passes} pushed through",
                fontSize = 12.sp,
                color = colors.neutral.step400,
            )
        }
    }
    // §9: resolved attempt counts; "No tries yet" replaces a fabricated
    // percentage when nothing has been counted.
    Text(
        text = if (today.attempts == 0) {
            buildAnnotatedString { append("No tries yet today.") }
        } else {
            buildAnnotatedString {
                append("${today.nopes} of ${today.attempts} tries today = ")
                withStyle(SpanStyle(color = colors.accentRamp.step200)) {
                    append("${today.ratePercent}%")
                }
                append(". Higher is better.")
            }
        },
        fontSize = 13.sp,
        lineHeight = 20.sp,
        color = colors.neutral.step300,
    )
}

// — Shared small pieces —

// @phosphor-icons/web 2.1.1 regular .ph-pencil-simple, verified against the
// same release the bundled font and Ph.Info come from (cmap-checked), for the
// S7 visit-chip edit affordance beyond the shared Ph table.
private const val PhPencilSimple = 0xe3b4

// The S7 app box (P7-F20): at most about five wrapped rows, so the cap is
// five default-height rows (8 + 23.25 + 4 + 27.25 + 8 dp content plus the
// 1dp divider ≈ 72dp; rows wrap and grow with font scale, P7-N17); fewer
// apps leave no reserved empty space.
private const val APP_ROW_APPROX_HEIGHT = 72
private const val LIST_BOX_ROWS = 5

@Composable
private fun WeekStatLine(label: String, value: String) {
    val colors = NocturneTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = colors.neutral.step400,
        )
        Text(
            value,
            style = tabular(fontSize = 14.0, weight = FontWeight.Medium),
            color = colors.text,
        )
    }
}

/**
 * The S7 chip (P7-F20): outlined 1dp/8dp, reading "~6m/visit ✎" for every app —
 * a user-set value carries no separate mark or tint (9 October §17 addendum).
 */
@Composable
private fun VisitChip(text: String) {
    val colors = NocturneTheme.colors
    Row(
        Modifier
            .border(1.dp, colors.neutral.step700, RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontSize = 11.sp, color = colors.neutral.step300)
        Spacer(Modifier.width(3.dp))
        PhosphorIcon(PhPencilSimple, tint = colors.neutral.step300, size = 11)
    }
}

/*
 * The S8 "<App> · time per visit" sheet (P7-F6). Save writes the override
 * through StatsRepository.setVisitOverride; "Use measured (Xm)" clears it.
 * A false return (write failed) keeps the sheet open with an inline note.
 * §10 logs nothing here (F-B handback point 3: no override events exist).
 */
@Composable
private fun VisitLengthSheetContent(
    row: StatsAppRow,
    statsRepo: StatsRepository,
    onDone: () -> Unit,
) {
    val colors = NocturneTheme.colors
    val scope = rememberCoroutineScope()
    var minutes by rememberSaveable(row.packageName) {
        mutableStateOf(initialSheetMinutes(row).coerceIn(StatsLedger.OVERRIDE_MINUTES_MIN, StatsLedger.OVERRIDE_MINUTES_MAX))
    }
    var working by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    fun submit(clear: Boolean) {
        working = true
        failed = false
        scope.launch {
            val ok = try {
                if (clear) statsRepo.clearVisitOverride(row.packageName)
                else statsRepo.setVisitOverride(row.packageName, minutes)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
            working = false
            if (ok) onDone() else failed = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            "${row.label} · time per visit",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = colors.text,
        )
        Text(
            "How long does a typical visit last for you? " +
                visitSheetMeasuredLabel(row.usesFallback, row.measuredVisitMs),
            fontSize = 13.5.sp,
            lineHeight = 21.sp,
            color = colors.neutral.step300,
        )
        // P7-F21 (S8): the stepper sits in a tinted rounded box with the
        // number shown large; presets fill the width in equal widths.
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.accentRamp.step900, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VisitStepperButton(
                glyph = Ph.Minus,
                enabled = !working && minutes > StatsLedger.OVERRIDE_MINUTES_MIN,
                onClick = { minutes-- },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    minutes.toString(),
                    style = tabular(fontSize = 56.0, weight = FontWeight.Medium),
                    color = colors.text,
                )
                Text(
                    "min",
                    fontSize = 13.sp,
                    color = colors.neutral.step500,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            VisitStepperButton(
                glyph = Ph.Plus,
                enabled = !working && minutes < StatsLedger.OVERRIDE_MINUTES_MAX,
                onClick = { minutes++ },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 5, 10, 15, 20).forEach { preset ->
                val selected = minutes == preset
                Text(
                    "${preset}m",
                    fontSize = 12.sp,
                    color = if (selected) colors.accentRamp.step200 else colors.neutral.step300,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (selected) colors.accent else colors.neutral.step700,
                            RoundedCornerShape(8.dp),
                        )
                        .clickable(enabled = !working) { minutes = preset }
                        .padding(vertical = 7.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                visitSheetNopesLine(row.nopes, minutes),
                fontSize = 13.sp,
                color = colors.neutral.step300,
            )
            Text(
                visitSheetSavedLine(row.nopes * minutes * 60_000L),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.accentRamp.step200,
            )
        }
        if (failed) {
            Text(
                "Couldn't save that. Try again.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.error,
            )
        }
        // P7-F25: the sheet's CTA pair follows the shared spec — Save
        // accent-outlined, "Use measured" secondary, a 12dp gap between
        // them; the sheet's own 18dp rhythm keeps ≥16dp above the pair.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NocturneCtaButton(
                "Save",
                enabled = !working,
                onClick = { submit(clear = false) },
            )
            NocturneCtaButton(
                "Use measured (${StatsDurationFormat.visitMinutes(row.measuredVisitMs)}m)",
                variant = ButtonVariant.SECONDARY,
                enabled = !working,
                onClick = { submit(clear = true) },
            )
        }
    }
}

@Composable
private fun VisitStepperButton(glyph: Int, enabled: Boolean, onClick: () -> Unit) {
    val colors = NocturneTheme.colors
    Box(
        Modifier
            .size(44.dp)
            .border(1.dp, colors.neutral.step700, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        PhosphorIcon(
            glyph,
            tint = if (enabled) colors.neutral.step200 else colors.neutral.step700,
            size = 16,
        )
    }
}

@Composable
private fun RateBar(fraction: Float, modifier: Modifier = Modifier) {
    val colors = NocturneTheme.colors
    Box(
        modifier
            .height(3.dp)
            .background(colors.neutral.step800, RoundedCornerShape(2.dp)),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(colors.accent, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun FormulaChip(text: String, accent: Boolean = false) {
    val colors = NocturneTheme.colors
    Text(
        text = text,
        fontSize = 12.sp,
        color = if (accent) colors.accentRamp.step200 else colors.neutral.step300,
        modifier = Modifier
            .border(1.dp, if (accent) colors.accent else colors.neutral.step700, RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

// Info targets are 44dp squares with a named button role (the Phase 7
// accessibility convention for glyph-only controls), slightly larger than
// the mock's bare 13–15px glyph.
@Composable
private fun StatsInfoButton(description: String, iconSize: Int = 15, onClick: () -> Unit) {
    val colors = NocturneTheme.colors
    Box(
        Modifier
            .size(44.dp)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        PhosphorIcon(Ph.Info, tint = colors.neutral.step500, size = iconSize)
    }
}

// The mock's .num rule: tabular numerals so columns of counts align.
@Composable
private fun tabular(fontSize: Double, weight: FontWeight) = TextStyle(
    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
    fontWeight = weight,
    fontSize = fontSize.sp,
    fontFeatureSettings = "tnum",
)
