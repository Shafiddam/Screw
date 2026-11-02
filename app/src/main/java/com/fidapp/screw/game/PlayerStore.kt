package com.fidapp.screw.game

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Экономика и прогресс игрока. Единый источник правды для UI (Compose State),
 * поверх [SharedPreferences]. Реклама — реальная VK Ads (см. `ads/Ads.kt`);
 * покупок нет — цены в монетах и «Бесплатно» (rewarded-видео) работают в демо-режиме IAP.
 */
object Economy {
    const val MAX_LIVES = 5
    const val LIFE_REGEN_MS = 5 * 60 * 1000L           // 1 жизнь каждые 5 минут
    const val WIN_COINS = 15                            // монет за победу (как на скрине «Победа»)
    const val PIGGY_PER_LEVEL = 200                     // свинка-копилка: +200 за уровень
    const val PIGGY_GOAL = 1600

    const val FREE_COINS = 10                           // бесплатные монеты по кнопке «+»
    const val FREE_COINS_COOLDOWN_MS = 12 * 60 * 60 * 1000L  // раз в 12 часов

    const val SHOP_COINS = 10                           // «получить 10» в магазине
    const val SHOP_COINS_COOLDOWN_MS = 6 * 60 * 60 * 1000L   // раз в 6 часов

    // цены бонусов в монетах (повторяем скриншоты)
    const val PRICE_DRILL = 120
    const val PRICE_MELT = 150
    const val PRICE_SEAL = 150
    const val PRICE_BOX = 300
    const val PRICE_REVIVE = 300
    const val PRICE_LIVES_2 = 300

    /** Цена бонуса в монетах — один источник для магазина, игры и диалогов. */
    fun priceOf(type: PowerUp): Int = when (type) {
        PowerUp.DRILL -> PRICE_DRILL
        PowerUp.MELT -> PRICE_MELT
        PowerUp.SEAL -> PRICE_SEAL
        PowerUp.BOX -> PRICE_BOX
    }
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

    /** Количество побед всего (для трофеев). */
    var totalWins by mutableIntStateOf(prefs.getInt("wins", 0))
        private set

    // ---- инвентарь бонусов (куплены в магазине, применяются в игре бесплатно) --

    private val bonusCounts = PowerUp.values().associateWith {
        mutableIntStateOf(prefs.getInt("bonus_${it.ordinal}", 0))
    }

    fun bonusCount(type: PowerUp): Int = bonusCounts.getValue(type).intValue

    fun addBonus(type: PowerUp, n: Int = 1) {
        val s = bonusCounts.getValue(type)
        s.intValue += n
        editor.putInt("bonus_${type.ordinal}", s.intValue).apply()
    }

    /** Списать 1 бонус из инвентаря; false — если пуст. */
    fun spendBonus(type: PowerUp): Boolean {
        val s = bonusCounts.getValue(type)
        if (s.intValue <= 0) return false
        s.intValue--
        editor.putInt("bonus_${type.ordinal}", s.intValue).apply()
        return true
    }

    // ---- профиль («Мое») ------------------------------------------------

    /** Ник игрока: по умолчанию «Игрок», можно переименовать в «Мое». */
    var nickname by mutableStateOf(prefs.getString("nickname", "").orEmpty())
        private set

    /** Индекс выбранного аватара в списке AVATARS (по умолчанию — заяц). */
    var avatar by mutableIntStateOf(prefs.getInt("avatar", 1))
        private set

    /** Лучшая серия побед подряд (сбрасывается при проигрыше). */
    var maxWinStreak by mutableIntStateOf(prefs.getInt("maxWinStreak", 0))
        private set

    private var winStreak by mutableIntStateOf(prefs.getInt("winStreak", 0))

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

    /** Локальные ежедневные напоминания «поиграй сегодня» (WorkManager). */
    private val remindersState = mutableStateOf(prefs.getBoolean("reminders", false))
    var reminders: Boolean
        get() = remindersState.value
        set(v) { remindersState.value = v; editor.putBoolean("reminders", v).apply() }

    /** Метка календарного дня, в который хоть раз открывали уровень — для напоминаний. */
    fun markPlayedToday() {
        editor.putString("lastPlayedDay", dayKey()).apply()
    }

    fun playedToday(): Boolean = prefs.getString("lastPlayedDay", "") == dayKey()

    private fun dayKey(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    /** Когда в последний раз брали бесплатные монеты (epoch ms); 0 — ещё не брали. */
    var lastFreeCoinAt by mutableLongStateOf(prefs.getLong("lastFreeCoinAt", 0L))
        private set

    private val editor get() = prefs.edit()

    init {
        if (nickname.isEmpty()) {
            nickname = "Игрок"
            editor.putString("nickname", nickname).apply()
        }
    }

    /** Бесплатные +10 монеты доступны раз в 12 часов. */
    fun freeCoinsReady(now: Long = System.currentTimeMillis()): Boolean =
        now - lastFreeCoinAt >= Economy.FREE_COINS_COOLDOWN_MS

    /** До окончания недоступности бесплатных монет (ms); 0 — уже можно брать. */
    fun freeCoinsWaitMs(now: Long = System.currentTimeMillis()): Long =
        (lastFreeCoinAt + Economy.FREE_COINS_COOLDOWN_MS - now).coerceAtLeast(0L)

    fun claimFreeCoins(now: Long = System.currentTimeMillis()): Boolean {
        if (!freeCoinsReady(now)) return false
        lastFreeCoinAt = now
        editor.putLong("lastFreeCoinAt", now).apply()
        addCoins(Economy.FREE_COINS)
        return true
    }

    /** Когда в последний раз получали +10 монет в магазине (epoch ms); 0 — ещё не брали. */
    var lastShopCoinAt by mutableLongStateOf(prefs.getLong("lastShopCoinAt", 0L))
        private set

    /** Бесплатные +10 монет в магазине — раз в 6 часов. */
    fun shopCoinsReady(now: Long = System.currentTimeMillis()): Boolean =
        now - lastShopCoinAt >= Economy.SHOP_COINS_COOLDOWN_MS

    /** До следующего +10 в магазине (ms); 0 — уже можно брать. */
    fun shopCoinsWaitMs(now: Long = System.currentTimeMillis()): Long =
        (lastShopCoinAt + Economy.SHOP_COINS_COOLDOWN_MS - now).coerceAtLeast(0L)

    fun claimShopCoins(now: Long = System.currentTimeMillis()): Boolean {
        if (!shopCoinsReady(now)) return false
        lastShopCoinAt = now
        editor.putLong("lastShopCoinAt", now).apply()
        addCoins(Economy.SHOP_COINS)
        return true
    }

    fun rename(value: String) {
        val v = value.trim().take(24)
        if (v.isNotEmpty()) {
            nickname = v
            editor.putString("nickname", v).apply()
        }
    }

    fun chooseAvatar(index: Int) {
        avatar = index
        editor.putInt("avatar", index).apply()
    }

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
        // проигрыш обнуляет серию побед (даже если жизней уже не было)
        if (winStreak != 0) {
            winStreak = 0
            editor.putInt("winStreak", 0).apply()
        }
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
        totalWins++
        winStreak++
        if (winStreak > maxWinStreak) maxWinStreak = winStreak
        editor.putInt("winStreak", winStreak).putInt("maxWinStreak", maxWinStreak).putInt("wins", totalWins).apply()
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
