package com.fidapp.screw.ui.game

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Звук и вибрация «на коленке» (без аудио-ассетов): короткие гудки ToneGenerator
 * как клики/успех/провал + вибрация. Уважают переключатели в настройках.
 */
class Feedback(private val context: Context) {
    private var tone: ToneGenerator? = null

    private fun play(code: Int, ms: Int) {
        runCatching {
            if (tone == null) tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            tone?.startTone(code, ms)
        }
    }

    fun tap(enabled: Boolean) { if (enabled) play(ToneGenerator.TONE_PROP_BEEP, 60) }
    fun success(enabled: Boolean) { if (enabled) play(ToneGenerator.TONE_PROP_ACK, 120) }
    fun fail(enabled: Boolean) { if (enabled) play(ToneGenerator.TONE_PROP_NACK, 200) }
    fun coin(enabled: Boolean) { if (enabled) play(ToneGenerator.TONE_CDMA_PIP, 80) }

    fun vibrate(enabled: Boolean, ms: Long = 30) {
        if (!enabled) return
        runCatching {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(ms)
            }
        }
    }

    fun release() { runCatching { tone?.release() }; tone = null }
}

@Composable
fun rememberFeedback(): Feedback {
    val context = LocalContext.current
    return remember(context) { Feedback(context.applicationContext) }
}

@Composable
fun rememberHaptics(): androidx.compose.ui.hapticfeedback.HapticFeedback = LocalHapticFeedback.current
