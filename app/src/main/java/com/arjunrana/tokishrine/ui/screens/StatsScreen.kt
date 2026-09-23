package com.arjunrana.tokishrine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.data.apps.InstalledAppsRepository
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.data.repo.StatsSnapshot
import com.arjunrana.tokishrine.data.stats.StatsFormat
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.components.SectionLabel
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

// Screen 23 (PRD §6/§9): all six Stats figures as one query over the event
// store. The hero is the all-time total with days active beneath it; the row
// carries this week / best day / walk-away rate; the leaderboard is per-app
// walk-aways, descending, labelled with user-visible app labels (PRD §6
// screen 15 rule: never a raw package identifier).
@Composable
fun StatsScreen(
    eventRepo: EventRepository,
    appsRepo: InstalledAppsRepository,
    onBack: () -> Unit,
) {
    val colors = NocturneTheme.colors

    // Feature-engagement event (§10): one stats_viewed per intentional entry,
    // matching the settings_viewed pattern.
    LaunchedEffect(Unit) {
        runCatching { eventRepo.log(EventRepository.EVENT_STATS_VIEWED) }
    }

    var snapshot by remember { mutableStateOf<StatsSnapshot?>(null) }
    LaunchedEffect(Unit) {
        snapshot = runCatching { eventRepo.getStats() }.getOrNull()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            NocturneAppbar(title = "Your walk-aways", onBack = onBack)

            val stats = snapshot
            if (stats != null) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stats.totalWalkAways.toString(),
                        style = tabular(fontSize = 76.0, weight = FontWeight.Normal),
                        color = colors.accentRamp.step200,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "times you've chosen to walk away",
                        fontSize = 14.sp,
                        color = colors.neutral.step500,
                    )
                    Text(
                        text = StatsFormat.sinceStartedLine(stats.daysActive),
                        fontSize = 12.sp,
                        color = colors.neutral.step600,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        value = stats.thisWeek.toString(),
                        label = "this week",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = stats.bestDay.toString(),
                        label = "best day",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = StatsFormat.walkAwayRatePercent(stats.walkAwayRate),
                        label = "walk-away rate",
                        modifier = Modifier.weight(1f),
                    )
                }

                if (stats.mostWalkedAwayFrom.isNotEmpty()) {
                    Spacer(Modifier.height(22.dp))
                    SectionLabel("MOST WALKED AWAY FROM")
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        stats.mostWalkedAwayFrom.forEach { entry ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = appsRepo.labelFor(entry.target),
                                    fontSize = 13.5.sp,
                                    color = colors.text,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = entry.count.toString(),
                                    style = tabular(fontSize = 13.5, weight = FontWeight.Normal),
                                    color = colors.neutral.step500,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = NocturneTheme.colors
    Column(
        modifier
            .background(colors.surface, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = tabular(fontSize = 24.0, weight = FontWeight.Medium),
            color = colors.text,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.neutral.step500,
            modifier = Modifier.padding(top = 2.dp),
        )
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
