package com.arjunrana.tokishrine.ui.util

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager

/*
 * Haptic confirmation for block activation changes (owner addendum,
 * 13 September): a stronger pulse whenever a block turns on, a lighter
 * one when it turns off. Predefined effects keep each device's own
 * haptic tuning; a missing vibrator or failed effect is a silent no-op.
 * Requires the (install-time, normal) VIBRATE permission.
 */
object BlockHaptics {

    fun turnedOn(context: Context) = vibrate(context, VibrationEffect.EFFECT_HEAVY_CLICK)

    fun turnedOff(context: Context) = vibrate(context, VibrationEffect.EFFECT_CLICK)

    private fun vibrate(context: Context, effectId: Int) {
        val vibrator = runCatching {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        }.getOrNull() ?: return
        runCatching {
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        }
    }
}
