package com.fidapp.screw.ui.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.fidapp.screw.R

/**
 * Звук и вибрация на реальных ассетах из res/raw:
 *  - fun_back_3 — фоновая музыка (зациклена, уважает переключатель «музыка»);
 *  - tap2 — нажатие кнопок; sound_ok — короткое «правильно»;
 *  - right — правильные действия/пройденный уровень; wrong — ошибка/поражение;
 *  - sound_win — победа; coin1 — звон монет (получение/трата, любой монетный
 *  обмен). Короткие эффекты через SoundPool, музыка через MediaPlayer.
 * Все эффекты уважают переключатель «звук» в настройках.
 */
class Feedback(private val context: Context) {

    private val soundPool: SoundPool? = runCatching {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
    }.getOrNull()

    private val sounds: Map<String, Int> = run {
        val map = HashMap<String, Int>()
        runCatching {
            RAW_EFFECTS.forEach { (name, resId) ->
                soundPool?.load(context, resId, 1)?.let { map[name] = it }
            }
        }
        map
    }

    private var music: MediaPlayer? = null

    private fun play(name: String, volume: Float = 1f) {
        val id = sounds[name] ?: return
        runCatching { soundPool?.play(id, volume, volume, 1, 0, 1f) }
    }

    /** Короткий клик по любой кнопке/винту. */
    fun tap(enabled: Boolean) { if (enabled) play("tap", 0.9f) }

    /** Короткое «правильно» — успешное действие. */
    fun ok(enabled: Boolean) { if (enabled) play("ok") }

    /** Средний «правильно» — пройденный уровень / значимый прогресс. */
    fun right(enabled: Boolean) { if (enabled) play("right") }

    /** Ошибка / поражение. */
    fun wrong(enabled: Boolean) { if (enabled) play("wrong") }

    /** Победа — что-то значительное. */
    fun win(enabled: Boolean) { if (enabled) play("win") }

    /** Монеты — получение/трата, всё, что связано с монетным обменом. */
    fun coin(enabled: Boolean) { if (enabled) play("coin") }

    /** Включает/выключает фоновую музыку (переключатель «музыка»). */
    fun setMusic(on: Boolean) {
        if (!on) {
            runCatching { music?.pause() }
            return
        }
        if (music == null) {
            music = runCatching {
                MediaPlayer.create(context, R.raw.fun_back_3)?.apply {
                    isLooping = true
                    setVolume(0.35f, 0.35f)
                }
            }.getOrNull()
        }
        runCatching { music?.start() }
    }

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

    fun release() {
        runCatching { music?.stop() }
        runCatching { music?.release() }
        music = null
        runCatching { soundPool?.release() }
    }

    private companion object {
        val RAW_EFFECTS = mapOf(
            "tap" to R.raw.tap2,
            "ok" to R.raw.sound_ok,
            "right" to R.raw.right,
            "wrong" to R.raw.wrong,
            "win" to R.raw.sound_win,
            "coin" to R.raw.coin1,
        )
    }
}

/** Звук нажатия на любую кнопку — провайдится из корня приложения. */
val LocalClickSound = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun rememberFeedback(): Feedback {
    val context = LocalContext.current
    return remember(context) { Feedback(context.applicationContext) }
}

@Composable
fun rememberHaptics(): androidx.compose.ui.hapticfeedback.HapticFeedback = LocalHapticFeedback.current
