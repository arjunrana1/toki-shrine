package com.arjunrana.tokishrine.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.R

internal val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
)

// The Nocturne base type rules (styles.css: h1–h6, body, line-heights, tracking)
// mapped onto the Material 3 slots. Headings stay at the 500 weight per the
// design readme; the derived sizes 14/13/12/11/10 come from the component layer
// (.btn/.input 14, .card-body 13, .field label 12, figcaption 11, .card-kicker 10).
private fun heading(size: Int) = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Medium,
    fontSize = size.sp,
    lineHeight = size.sp * 1.12f,
    letterSpacing = size.sp * -0.015f,
)

private fun body(size: Int) = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = size.sp * 1.55f,
)

val NocturneTypography = Typography(
    displayLarge = heading(42),
    displayMedium = heading(32),
    displaySmall = heading(25),
    headlineLarge = heading(25),
    headlineMedium = heading(20),
    headlineSmall = heading(16),
    titleLarge = heading(16),
    titleSmall = heading(13).copy(
        letterSpacing = 13.sp * 0.08f,
    ),
    bodyLarge = body(15),
    bodyMedium = body(14),
    bodySmall = body(13),
    labelLarge = body(12),
    labelMedium = body(11),
    labelSmall = body(10).copy(
        letterSpacing = 10.sp * 0.1f,
    ),
)
