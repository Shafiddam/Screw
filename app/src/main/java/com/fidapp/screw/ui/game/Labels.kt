package com.fidapp.screw.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.fidapp.screw.game.PlayerStore

/**
 * Словарь. Язык в игре всегда русский (английские значения оставлены про запас,
 * переключателя нет). Ключи короткие, значения — пары (русский, английский).
 */
private val dict: Map<String, Pair<String, String>> = mapOf(
    "app_name" to ("Шуруп" to "Screw Sort"),
    "level" to ("Уровень %d" to "Level %d"),
    "play" to ("Играть" to "Play"),
    "daily" to ("Вызов дня" to "Daily Challenge"),
    "home" to ("Главная" to "Home"),
    "shop" to ("Магазин" to "Shop"),
    "trophy" to ("Трофеи" to "Trophies"),
    "main" to ("Главная" to "Main"),
    "noads" to ("Без рекламы" to "No Ads"),
    "piggy" to ("Свинка-копилка" to "Piggy Bank"),
    "piggy_desc" to ("Зарабатывайте 200 золота за каждый пройденный уровень" to "Earn 200 gold for every level you clear"),
    "claim" to ("Забрать" to "Claim"),
    "open_box" to ("Открыть Ящик" to "Open Box"),
    "drill" to ("Сверлить" to "Drill"),
    "melt" to ("Плавить" to "Melt"),
    "seal" to ("Заделать" to "Seal"),
    "drill_desc" to ("Добавляет 1 кружок склада" to "Adds 1 holding slot"),
    "melt_desc" to ("Плавит 1 стекло" to "Melts 1 blocked piece"),
    "seal_desc" to ("Освобождает весь склад" to "Empties the holding row"),
    "box_desc" to ("Открывает 1 ящик" to "Opens 1 box"),
    "box_none" to ("Свободных ящиков не осталось" to "No boxes left to open"),
    "free" to ("Бес" to "Free"),
    "win" to ("Победа!" to "You Win!"),
    "lose" to ("Провал" to "Failed"),
    "lose_desc" to ("Вы потеряли 1 жизнь, и прогресс уровня" to "You lost 1 life and level progress"),
    "to_home" to ("На главную" to "Home"),
    "retry" to ("Заново" to "Retry"),
    "revive" to ("Воскрешение" to "Revive"),
    "revive_desc" to ("Продолжите игру и откройте 1 ящик" to "Continue and open 1 box"),
    "refill_lives" to ("Восполнить жизни?" to "Refill lives?"),
    "next_refill" to ("Следующее пополнение" to "Next refill"),
    "settings" to ("Настройки" to "Settings"),
    "vibration" to ("Вибрация" to "Vibration"),
    "sound" to ("Звук" to "Sound"),
    "music" to ("Музыка" to "Music"),
    "support" to ("Поддержка" to "Support"),
    "restore" to ("Восст. покупки" to "Restore purchases"),
    "promo" to ("Промокод" to "Promo code"),
    "link" to ("Привязка" to "Link account"),
    "get" to ("Получить" to "Collect"),
    "catch" to ("Улов" to "Win"),
    "coins" to ("монет" to "coins"),
    "lives" to ("жизни" to "lives"),
    "beginner_pack" to ("Набор новичка" to "Starter Pack"),
    "not_enough" to ("Недостаточно монет" to "Not enough coins"),
    "ad_stub" to ("Реклама недоступна в этой сборке — бонус выдан бесплатно (демо)" to "Ads are off in this build — bonus granted free (demo)"),
    "iap_stub" to ("Покупки отключены в демо-сборке" to "Purchases are disabled in the demo build"),
    "locked" to ("Закрыто" to "Locked"),
    "next_level" to ("Следующий уровень" to "Next level"),
    "continue" to ("Продолжить" to "Continue"),
    "close" to ("Закрыть" to "Close"),
    "progress" to ("Прогресс" to "Progress"),
    "best" to ("Лучший уровень" to "Best level"),
    "total_claimed" to ("Всего собрано в копилке" to "Total claimed from piggy"),
    "shop_hint" to ("Бонусы покупаются за монеты" to "Bonuses are bought with coins"),
    "bonus_title" to ("Бонусы" to "Bonuses"),
    "exit_title" to ("Выход" to "Exit"),
    "exit_q" to ("Вы хотите выйти из игры?" to "Do you want to exit the game?"),
    "yes" to ("Да" to "Yes"),
    "no" to ("Нет" to "No"),
)

val LocalPlayerStore = compositionLocalOf<PlayerStore?> { null }

@Composable
fun L(key: String, vararg args: Any): String {
    val pair = dict[key] ?: return key
    val raw = pair.first // всегда русский
    return if (args.isEmpty()) raw else String.format(raw, *args)
}
