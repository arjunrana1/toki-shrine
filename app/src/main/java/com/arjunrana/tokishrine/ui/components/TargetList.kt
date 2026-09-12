package com.arjunrana.tokishrine.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import com.arjunrana.tokishrine.ui.util.toImageBitmap
import androidx.compose.foundation.ScrollState
import kotlin.math.roundToInt

// One row of a block's target list: an app's icon (or a fallback glyph) and
// its label, or the globe glyph and a site domain.
data class TargetRow(
    val key: String,
    val label: String,
    val icon: Drawable? = null,
    val glyph: Int = Ph.Globe,
)

// PRD §17 R6 and the 13 September addendum: the create review and the block
// detail screen share one vertical target list — icon + label rows with
// hairline dividers in a surface card, apps first then sites. Four 46 dp
// rows are visible; longer lists scroll inside the card and show a small
// scrollbar at the right edge. Labels wrap; nothing overflows horizontally.
private val TARGET_ROW_HEIGHT = 46.dp
private val TARGET_LIST_MAX_HEIGHT = TARGET_ROW_HEIGHT * 4 + 3.dp

@Composable
fun BoundedTargetList(rows: List<TargetRow>, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = TARGET_LIST_MAX_HEIGHT)
                .verticalScroll(scroll)
                .background(NocturneTheme.colors.surface, RoundedCornerShape(12.dp)),
        ) {
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(NocturneTheme.colors.divider),
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = TARGET_ROW_HEIGHT)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TargetIcon(drawable = row.icon, glyph = row.glyph)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        row.label,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = NocturneTheme.colors.text,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // The scrollbar exists only once the content overflows the four-row
        // cap — while it fits, scroll.maxValue is 0 and nothing renders.
        if (scroll.maxValue > 0) {
            TargetListScrollbar(
                scroll = scroll,
                viewportHeight = TARGET_LIST_MAX_HEIGHT,
            )
        }
    }
}

// Minimal right-edge indicator: a 3 dp thumb whose size is the viewport's
// share of the content and whose position tracks the scroll offset.
@Composable
private fun BoxScope.TargetListScrollbar(scroll: ScrollState, viewportHeight: Dp) {
    val density = LocalDensity.current
    val viewportPx = with(density) { viewportHeight.roundToPx() }
    val contentPx = scroll.maxValue + viewportPx
    val thumbPx = (viewportPx.toLong() * viewportPx / contentPx)
        .toInt()
        .coerceAtLeast(with(density) { 24.dp.roundToPx() })
    val maxTravelPx = viewportPx - thumbPx
    val offsetPx = maxTravelPx.toFloat() * scroll.value / scroll.maxValue

    Box(
        Modifier
            .align(Alignment.TopEnd)
            .padding(end = 4.dp)
            .offset(y = with(density) { offsetPx.roundToInt().toDp() })
            .width(3.dp)
            .height(with(density) { thumbPx.toDp() })
            .background(NocturneTheme.colors.neutral.step600, RoundedCornerShape(2.dp)),
    )
}

@Composable
private fun TargetIcon(drawable: Drawable?, glyph: Int) {
    val bitmap = remember(drawable) { drawable?.toImageBitmap() }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(24.dp))
    } else {
        PhosphorIcon(glyph, tint = NocturneTheme.colors.neutral.step400, size = 20)
    }
}
