package com.arjunrana.tokishrine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import java.util.Locale

/*
 * Supported browsers (P7-F27, 9 October §17 addendum): the Settings row's
 * destination. The browser SET always comes from the bundled
 * assets/detection_config.json through DetectionConfigLoader — nothing here
 * decides which browsers exist — while the pure helpers below only dress each
 * declared package in a display name and derive the Settings subtitle, so a
 * config addition appears without a code change.
 */

// Product names for the packages the bundled detection configuration
// declares (several package names are not derivable from the product name,
// e.g. com.microsoft.emmx → Microsoft Edge). Presentation only: this table
// can never add or remove a browser from the list.
private val BROWSER_DISPLAY_NAMES = mapOf(
    "com.android.chrome" to "Chrome",
    "com.chrome.beta" to "Chrome Beta",
    "com.sec.android.app.sbrowser" to "Samsung Internet",
    "org.mozilla.firefox" to "Firefox",
    "com.brave.browser" to "Brave",
    "com.microsoft.emmx" to "Microsoft Edge",
    "com.opera.browser" to "Opera",
)

// Unknown packages (a config entry without a table row) fall back to the
// last package segment, capitalized — never a raw dotted ID.
fun browserDisplayName(packageName: String): String =
    BROWSER_DISPLAY_NAMES[packageName]
        ?: packageName.substringAfterLast('.').replaceFirstChar { it.uppercase(Locale.ENGLISH) }

// The Settings row subtitle, derived from the same config list: the first
// three names, then "+ N more" ("Chrome, Chrome Beta, Samsung Internet + 4
// more" for today's seven-browser config).
fun supportedBrowsersSubtitle(packages: List<String>): String {
    val names = packages.map(::browserDisplayName)
    return when {
        names.size <= 3 -> names.joinToString(", ")
        else -> names.take(3).joinToString(", ") + " + ${names.size - 3} more"
    }
}

// [browsers] is null while the bundled config loads (and stays null if the
// asset is corrupt — the screen degrades to its frame, like the OEM battery
// instructions).
@Composable
fun SupportedBrowsersScreen(
    browsers: List<String>?,
    onBack: () -> Unit,
) {
    val colors = NocturneTheme.colors

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            NocturneAppbar(title = "Supported browsers", onBack = onBack)
            Text(
                "Website blocking watches for the address bar in these browsers:",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = colors.neutral.step400,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            if (browsers != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(12.dp)),
                ) {
                    browsers.forEachIndexed { index, packageName ->
                        if (index > 0) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 46.dp)
                                    .height(1.dp)
                                    .background(colors.divider),
                            )
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PhosphorIcon(Ph.Globe, tint = colors.neutral.step400, size = 19)
                            Spacer(Modifier.width(13.dp))
                            Column {
                                Text(
                                    browserDisplayName(packageName),
                                    fontSize = 14.sp,
                                    color = colors.text,
                                )
                                Text(
                                    packageName,
                                    fontSize = 11.5.sp,
                                    color = colors.neutral.step500,
                                    modifier = Modifier.padding(top = 1.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
