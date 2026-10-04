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
import com.arjunrana.tokishrine.data.stats.StatsRepository
import com.arjunrana.tokishrine.data.stats.StatsScreenState
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.components.NocturneButton
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
                    NocturneButton(
                        "Got it",
                        variant = ButtonVariant.SECONDARY,
                        block = true,
                        height = 42.dp,
                        onClick = { infoSheet = null },
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

        // — Hero —
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Time saved per day",
                fontSize = 12.5.sp,
                color = colors.neutral.step400,
            )
            StatsInfoButton(
                description = "About time saved per day",
                onClick = { onOpenInfoSheet(StatsSheetKind.TimeSaved, "time_saved_hero") },
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = StatsDurationFormat.duration(dashboard.savedPerDayMs),
                style = tabular(fontSize = 68.0, weight = FontWeight.Normal),
                color = colors.accentRamp.step200,
            )
            Text(
                "/ day",
                fontSize = 15.sp,
                color = colors.neutral.step500,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            )
        }
        Text(
            text = "${StatsDurationFormat.duration(dashboard.savedMs)} saved so far",
            fontSize = 14.sp,
            color = colors.neutral.step200,
            modifier = Modifier.padding(top = 4.dp),
        )
        val screenTime = screenTimeLine(dashboard.screenDailyMs, dashboard.screenChangePercent)
        when {
            !dashboard.usageAvailable -> Text(
                // §9/§10: neutral unavailability note; saved/outcome figures
                // above remain honest, nothing is invented below.
                "Usage data isn't available right now",
                fontSize = 12.sp,
                color = colors.neutral.step500,
                modifier = Modifier.padding(top = 6.dp),
            )
            screenTime != null -> Text(
                screenTime,
                fontSize = 12.sp,
                color = colors.neutral.step500,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(24.dp))

        // — Tiles —
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(
                Modifier
                    .weight(1f)
                    .background(colors.surface, RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text = dashboard.attemptsPerDay.roundToInt().toString(),
                    style = tabular(fontSize = 24.0, weight = FontWeight.Medium),
                    color = colors.text,
                )
                Text(
                    "Attempts / day",
                    fontSize = 11.5.sp,
                    color = colors.neutral.step500,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .background(colors.surface, RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                Text(
                    // §9/§10: an em dash with an empty bar when there are no
                    // resolved attempts — never a fabricated 0%.
                    text = if (dashboard.attempts == 0) "—" else "${dashboard.nopeRatePercent}%",
                    style = tabular(fontSize = 24.0, weight = FontWeight.Medium),
                    color = colors.text,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Nope rate",
                        fontSize = 11.5.sp,
                        color = colors.neutral.step500,
                    )
                    StatsInfoButton(
                        description = "About nope rate",
                        iconSize = 13,
                        onClick = { onOpenInfoSheet(StatsSheetKind.NopeRate, "nope_rate") },
                    )
                }
                Spacer(Modifier.height(4.dp))
                RateBar(
                    fraction = if (dashboard.attempts == 0) 0f else dashboard.nopeRatePercent / 100f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        // — Blocked apps (nested list, ~300dp with overflow fade) —
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
            Box {
                Column(
                    Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    appRows.forEachIndexed { index, row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = row.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.text,
                                )
                                Text(
                                    text = appRowSubtitle(row),
                                    fontSize = 11.5.sp,
                                    color = colors.neutral.step500,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
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
                // so list scrolling under it stays live.
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Recalibrate time spent on apps",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text,
            )
            Text(
                "Usual time per app seems off? Hit recalibrate and we'll re-measure it " +
                    "from your last 7 days.",
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                color = colors.neutral.step400,
            )
            if (recalibrateFailed) {
                Text(
                    "Recalibration didn't finish, so your usual time hasn't changed.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            NocturneButton(
                "Recalibrate",
                variant = ButtonVariant.SECONDARY,
                block = true,
                height = 40.dp,
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
    Text(
        "Time saved",
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = colors.text,
    )
    Text(
        "Every time you explicitly nope out, we estimate the time a usual visit " +
            "would have taken.",
        fontSize = 13.5.sp,
        lineHeight = 21.sp,
        color = colors.neutral.step300,
    )
    // Corrected PRD §9 copy — the PNG's pre-install/abandonment claims are
    // superseded and intentionally not reproduced here.
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "How we get \"usual visit length\"",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.text,
        )
        Text(
            "We use recent available app history when you allow usage access, up to " +
                "7 days. If we have fewer than 3 usable visits, we use 10 minutes per " +
                "visit. Recalibrating updates future estimates.",
            fontSize = 12.5.sp,
            lineHeight = 19.sp,
            color = colors.neutral.step400,
        )
    }
    Text(
        "Repeated nopes within 5 minutes count once. Completing a challenge counts " +
            "as a separate try.",
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = colors.neutral.step500,
    )
}

@Composable
private fun NopeRateSheetContent(dashboard: StatsDashboard) {
    val colors = NocturneTheme.colors
    val rateFraction = if (dashboard.attempts == 0) 0f else dashboard.nopeRatePercent / 100f
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
                "${dashboard.nopes} nopes",
                fontSize = 12.sp,
                color = colors.accentRamp.step200,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${dashboard.passes} pushed through",
                fontSize = 12.sp,
                color = colors.neutral.step400,
            )
        }
    }
    // §9: resolved attempt counts; "No tries yet" replaces a fabricated
    // percentage when nothing has been counted.
    Text(
        text = if (dashboard.attempts == 0) {
            buildAnnotatedString { append("No tries yet.") }
        } else {
            buildAnnotatedString {
                append("${dashboard.nopes} of ${dashboard.attempts} tries this week = ")
                withStyle(SpanStyle(color = colors.accentRamp.step200)) {
                    append("${dashboard.nopeRatePercent}%")
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
