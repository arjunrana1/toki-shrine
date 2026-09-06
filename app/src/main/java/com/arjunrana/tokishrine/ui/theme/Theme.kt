package com.arjunrana.tokishrine.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class TonalRamp(
    val step100: Color,
    val step200: Color,
    val step300: Color,
    val step400: Color,
    val step500: Color,
    val step600: Color,
    val step700: Color,
    val step800: Color,
    val step900: Color,
)

// All roles from the Nocturne :root, including the machine-derived accent-2
// stand-in, which the design readme says to treat as one role with accent.
@Immutable
data class NocturneColors(
    val bg: Color,
    val surface: Color,
    val text: Color,
    val accent: Color,
    val accent2: Color,
    val divider: Color,
    val neutral: TonalRamp,
    val accentRamp: TonalRamp,
    val accent2Ramp: TonalRamp,
    val section: Color,
    val sectionGlow: Color,
    val sectionGhost: Color,
)

@Immutable
data class NocturneSpacing(
    val s1: Dp,
    val s2: Dp,
    val s3: Dp,
    val s4: Dp,
    val s6: Dp,
    val s8: Dp,
)

@Immutable
data class NocturneShadow(
    val edge: Color,
    val shadowColor: Color,
    val elevation: Dp,
)

@Immutable
data class NocturneElevation(
    val sm: NocturneShadow,
    val md: NocturneShadow,
    val lg: NocturneShadow,
)

private val NeutralRamp = TonalRamp(
    step100 = NocturneNeutral100,
    step200 = NocturneNeutral200,
    step300 = NocturneNeutral300,
    step400 = NocturneNeutral400,
    step500 = NocturneNeutral500,
    step600 = NocturneNeutral600,
    step700 = NocturneNeutral700,
    step800 = NocturneNeutral800,
    step900 = NocturneNeutral900,
)

private val AccentRamp = TonalRamp(
    step100 = NocturneAccentRamp100,
    step200 = NocturneAccentRamp200,
    step300 = NocturneAccentRamp300,
    step400 = NocturneAccentRamp400,
    step500 = NocturneAccentRamp500,
    step600 = NocturneAccentRamp600,
    step700 = NocturneAccentRamp700,
    step800 = NocturneAccentRamp800,
    step900 = NocturneAccentRamp900,
)

private val Accent2Ramp = TonalRamp(
    step100 = NocturneAccent2Ramp100,
    step200 = NocturneAccent2Ramp200,
    step300 = NocturneAccent2Ramp300,
    step400 = NocturneAccent2Ramp400,
    step500 = NocturneAccent2Ramp500,
    step600 = NocturneAccent2Ramp600,
    step700 = NocturneAccent2Ramp700,
    step800 = NocturneAccent2Ramp800,
    step900 = NocturneAccent2Ramp900,
)

private val DefaultColors = NocturneColors(
    bg = NocturneBg,
    surface = NocturneSurface,
    text = NocturneText,
    accent = NocturneAccent,
    accent2 = NocturneAccent2,
    divider = NocturneDivider,
    neutral = NeutralRamp,
    accentRamp = AccentRamp,
    accent2Ramp = Accent2Ramp,
    section = NocturneSection,
    sectionGlow = NocturneSectionGlow,
    sectionGhost = NocturneSectionGhost,
)

private val DefaultSpacing = NocturneSpacing(
    s1 = 2.8.dp,
    s2 = 5.6.dp,
    s3 = 8.4.dp,
    s4 = 11.2.dp,
    s6 = 16.8.dp,
    s8 = 22.4.dp,
)

// Nocturne's dark-ground elevation is a hairline edge plus ambient darkness.
// The CSS blur radii (18px, 40px) map to Compose elevation as blur ÷ 2, and the
// rgba(0,0,0,α) layers carry their alpha in shadowColor. Shade black is the
// readme's stated exception to "no pure black".
private val DefaultElevation = NocturneElevation(
    sm = NocturneShadow(
        edge = NocturneNeutral800,
        shadowColor = Color.Transparent,
        elevation = 0.dp,
    ),
    md = NocturneShadow(
        edge = NocturneNeutral700,
        shadowColor = Color.Black.copy(alpha = 0.55f),
        elevation = 9.dp,
    ),
    lg = NocturneShadow(
        edge = NocturneNeutral500,
        shadowColor = Color.Black.copy(alpha = 0.65f),
        elevation = 20.dp,
    ),
)

private val LocalNocturneColors = staticCompositionLocalOf { DefaultColors }
private val LocalNocturneSpacing = staticCompositionLocalOf { DefaultSpacing }
private val LocalNocturneElevation = staticCompositionLocalOf { DefaultElevation }

val NocturneColorScheme: ColorScheme = darkColorScheme(
    primary = NocturneAccent,
    onPrimary = NocturneBg,
    primaryContainer = NocturneAccentRamp800,
    onPrimaryContainer = NocturneAccentRamp100,
    secondary = NocturneAccent2,
    onSecondary = NocturneBg,
    secondaryContainer = NocturneAccent2Ramp800,
    onSecondaryContainer = NocturneAccent2Ramp100,
    tertiary = NocturneAccent,
    onTertiary = NocturneBg,
    tertiaryContainer = NocturneAccentRamp800,
    onTertiaryContainer = NocturneAccentRamp100,
    background = NocturneBg,
    onBackground = NocturneText,
    surface = NocturneSurface,
    onSurface = NocturneText,
    surfaceVariant = NocturneNeutral800,
    onSurfaceVariant = NocturneNeutral400,
    outline = NocturneNeutral700,
    outlineVariant = NocturneDivider,
    surfaceDim = NocturneBg,
    surfaceBright = NocturneSurface,
    surfaceContainerLowest = NocturneBg,
    surfaceContainerLow = NocturneSurface,
    surfaceContainer = NocturneSurface,
    surfaceContainerHigh = NocturneSurface,
    surfaceContainerHighest = NocturneSurface,
)

object NocturneTheme {
    val colors: NocturneColors
        @Composable get() = LocalNocturneColors.current

    val spacing: NocturneSpacing
        @Composable get() = LocalNocturneSpacing.current

    val elevation: NocturneElevation
        @Composable get() = LocalNocturneElevation.current
}

// Dark only by product decision (PRD §13) — no isSystemInDarkTheme branch.
@Composable
fun NocturneTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNocturneColors provides DefaultColors,
        LocalNocturneSpacing provides DefaultSpacing,
        LocalNocturneElevation provides DefaultElevation,
    ) {
        MaterialTheme(
            colorScheme = NocturneColorScheme,
            typography = NocturneTypography,
            shapes = NocturneShapes,
            content = content,
        )
    }
}
