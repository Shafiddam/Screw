package com.fidapp.screw.game

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Экономика и прогресс игрока. Единый источник правды для UI (Compose State),
 * поверх [SharedPreferences]. Никаких покупок и рекламы — цены и «Бес» работают
 * как заглушки: списывают виртуальные монеты / сразу выдают бонус.
 */
object Economy {
    const val MAX_LIVES = 5
    const val LIFE_REGEN_MS = 30 * 60 * 1000L          // 1 жизнь каждые 30 минут
    const val WIN_COINS = 15                            // монет за победу (как на скрине «Победа»)
    const val PIGGY_PER_LEVEL = 200                     // свинка-копилка: +200 за уровень
    const val PIGGY_GOAL = 1600

    // цены бонусов в монетах (повторяем скриншоты)
    const val PRICE_DRILL = 120
    const val PRICE_MELT = 150
    const val PRICE_SEAL = 150
    const val PRICE_BOX = 300
    const val PRICE_REVIVE = 300
    const val PRICE_LIVES_2 = 300
}

/** Бонусы, покупаемые за монеты или «бесплатно» (заглушка под rewarded-видео). */
enum class PowerUp { DRILL, MELT, SEAL, BOX }

class PlayerStore(private val prefs: SharedPreferences) {

    var coins by mutableIntStateOf(prefs.getInt("coins", 15))
        private set

    var lives by mutableIntStateOf(prefs.getInt("lives", Economy.MAX_LIVES))
        private set

    /** Когда восстановится следующая жизнь (epoch ms); 0 — жизни полные. */
    var nextLifeAt by mutableLongStateOf(prefs.getLong("nextLifeAt", 0L))
        private set

    var bestLevel by mutableIntStateOf(prefs.getInt("bestLevel", 1))
        private set

    var piggy by mutableIntStateOf(prefs.getInt("piggy", 0))
        private set

    var piggyClaimed by mutableIntStateOf(prefs.getInt("piggyClaimed", 0))
        private set

    var reviveUsedForLevel by mutableIntStateOf(prefs.getInt("reviveLevel", 0))
        private set

    // настройки
    private val vibrationState = mutableStateOf(prefs.getBoolean("vibration", true))
    var vibration: Boolean
        get() = vibrationState.value
        set(v) { vibrationState.value = v; editor.putBoolean("vibration", v).apply() }

    private val soundState = mutableStateOf(prefs.getBoolean("sound", true))
    var sound: Boolean
        get() = soundState.value
        set(v) { soundState.value = v; editor.putBoolean("sound", v).apply() }

    private val musicState = mutableStateOf(prefs.getBoolean("music", false))
    var music: Boolean
        get() = musicState.value
        set(v) { musicState.value = v; editor.putBoolean("music", v).apply() }

    private val editor get() = prefs.edit()

    // ---- монеты ---------------------------------------------------------

    fun addCoins(amount: Int) {
        coins += amount
        editor.putInt("coins", coins).apply()
    }

    /** Возвращает true, если монет хватило и они списаны. */
    fun spendCoins(amount: Int): Boolean {
        if (coins < amount) return false
        coins -= amount
        editor.putInt("coins", coins).apply()
        return true
    }

    fun canAfford(amount: Int): Boolean = coins >= amount

    // ---- жизни ----------------------------------------------------------

    /** Догоняет регенерацию жизней по текущему времени. Вызывать на тике. */
    fun tickLives(now: Long = System.currentTimeMillis()) {
        if (lives >= Economy.MAX_LIVES) {
            if (nextLifeAt != 0L) {
                nextLifeAt = 0L
                editor.putLong("nextLifeAt", 0L).apply()
            }
            return
        }
        if (nextLifeAt == 0L) {
            nextLifeAt = now + Economy.LIFE_REGEN_MS
            editor.putLong("nextLifeAt", nextLifeAt).apply()
            return
        }
        var changed = false
        while (lives < Economy.MAX_LIVES && now >= nextLifeAt) {
            lives++
            nextLifeAt += Economy.LIFE_REGEN_MS
            changed = true
        }
        if (lives >= Economy.MAX_LIVES) nextLifeAt = 0L
        if (changed) editor.putInt("lives", lives).putLong("nextLifeAt", nextLifeAt).apply()
    }

    fun spendLife(): Boolean {
        if (lives <= 0) return false
        lives--
        if (nextLifeAt == 0L) nextLifeAt = System.currentTimeMillis() + Economy.LIFE_REGEN_MS
        editor.putInt("lives", lives).putLong("nextLifeAt", nextLifeAt).apply()
        return true
    }

    fun addLives(n: Int) {
        lives = (lives + n).coerceAtMost(Economy.MAX_LIVES)
        if (lives >= Economy.MAX_LIVES) nextLifeAt = 0L
        editor.putInt("lives", lives).putLong("nextLifeAt", nextLifeAt).apply()
    }

    fun refillLives() = addLives(Economy.MAX_LIVES)

    // ---- прогресс -------------------------------------------------------

    fun onLevelWon(level: Int) {
        if (level >= bestLevel) {
            bestLevel = level + 1
            editor.putInt("bestLevel", bestLevel).apply()
        }
        addCoins(Economy.WIN_COINS)
        addPiggy(Economy.PIGGY_PER_LEVEL)
    }

    fun isUnlocked(level: Int): Boolean = level <= bestLevel

    // ---- свинка-копилка -------------------------------------------------

    private fun addPiggy(amount: Int) {
        if (piggy < Economy.PIGGY_GOAL) {
            piggy = (piggy + amount).coerceAtMost(Economy.PIGGY_GOAL)
            editor.putInt("piggy", piggy).apply()
        }
    }

    /** Забрать накопленное в свинке (после «покупки» — заглушка выдаёт сразу). */
    fun claimPiggy() {
        if (piggy > 0) {
            addCoins(piggy)
            piggyClaimed += piggy
            piggy = 0
            editor.putInt("piggy", 0).putInt("piggyClaimed", piggyClaimed).apply()
        }
    }

    // ---- воскрешение ----------------------------------------------------

    fun canRevive(level: Int): Boolean = reviveUsedForLevel != level
    fun markRevived(level: Int) {
        reviveUsedForLevel = level
        editor.putInt("reviveLevel", level).apply()
    }

    // ---- настройки ------------------------------------------------------
    // язык в игре всегда русский — переключатель удалён по требованию.
}
