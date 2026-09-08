package com.arjunrana.tokishrine.ui.theme

import androidx.compose.ui.graphics.Color

// The Nocturne tokens from toki-shrine-ui-mockups/project/_ds/nocturne-*/styles.css,
// translated once. This is the only file allowed to carry hex values.
// Derived token: --color-divider is color-mix(in srgb, #e9e9ed 16%, transparent).

internal val NocturneBg = Color(0xFF161826)
internal val NocturneSurface = Color(0xFF232532)
internal val NocturneText = Color(0xFFE9E9ED)
internal val NocturneAccent = Color(0xFF9184D9)
internal val NocturneAccent2 = Color(0xFFA7A1DB)
internal val NocturneDivider = Color(0x29E9E9ED)

internal val NocturneNeutral100 = Color(0xFFF3F5FE)
internal val NocturneNeutral200 = Color(0xFFE4E7F5)
internal val NocturneNeutral300 = Color(0xFFCFD3E5)
internal val NocturneNeutral400 = Color(0xFFB2B6CA)
internal val NocturneNeutral500 = Color(0xFF9397AB)
internal val NocturneNeutral600 = Color(0xFF75798C)
internal val NocturneNeutral700 = Color(0xFF595D6C)
internal val NocturneNeutral800 = Color(0xFF3F424D)
internal val NocturneNeutral900 = Color(0xFF292B31)

internal val NocturneAccentRamp100 = Color(0xFFF5F4FF)
internal val NocturneAccentRamp200 = Color(0xFFE7E5FE)
internal val NocturneAccentRamp300 = Color(0xFFD2CEFD)
internal val NocturneAccentRamp400 = Color(0xFFB5ABFC)
internal val NocturneAccentRamp500 = Color(0xFF968AE0)
internal val NocturneAccentRamp600 = Color(0xFF796CBF)
internal val NocturneAccentRamp700 = Color(0xFF5D5294)
internal val NocturneAccentRamp800 = Color(0xFF423A6A)
internal val NocturneAccentRamp900 = Color(0xFF2B2741)

internal val NocturneAccent2Ramp100 = Color(0xFFF5F4FF)
internal val NocturneAccent2Ramp200 = Color(0xFFE7E5FE)
internal val NocturneAccent2Ramp300 = Color(0xFFD2CEFD)
internal val NocturneAccent2Ramp400 = Color(0xFFB5AFE8)
internal val NocturneAccent2Ramp500 = Color(0xFF9690C9)
internal val NocturneAccent2Ramp600 = Color(0xFF7972A9)
internal val NocturneAccent2Ramp700 = Color(0xFF5C5783)
internal val NocturneAccent2Ramp800 = Color(0xFF423E5D)
internal val NocturneAccent2Ramp900 = Color(0xFF2B293A)

internal val NocturneSection = Color(0xFF262A60)
internal val NocturneSectionGlow = Color(0xFF353B80)
internal val NocturneSectionGhost = Color(0xFF4C5397)

// Error family — derived, not authored in styles.css (Nocturne names no error
// colour). Built the way the system builds its own ramps: OKLCH on the shared
// lightness scale, warm-red hue 25 (the hue of the mocks' #8B0000 typo marks),
// chroma in the accent's own range. error sits at the accent's L 0.66 / C 0.125;
// container and on-container mirror the accent-800 / accent-100 steps.
internal val NocturneError = Color(0xFFD4716B) // oklch(0.660 0.125 25)
internal val NocturneErrorContainer = Color(0xFF612421) // oklch(0.350 0.090 25)
internal val NocturneOnErrorContainer = Color(0xFFFFEBE8) // oklch(0.960 0.030 25)

// Scrim — the .dialog-backdrop value from the CSS component layer:
// color-mix(in srgb, neutral-900 50%, transparent).
internal val NocturneScrim = Color(0x80292B31)
