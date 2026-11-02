package com.fidapp.screw.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidapp.screw.ads.RewardAdManager
import com.fidapp.screw.game.Economy
import com.fidapp.screw.game.PlayerStore
import com.fidapp.screw.game.PowerUp
import kotlinx.coroutines.delay

/** Магазин: рамка как в настройках (шапка «БОНУСЫ», винтики, красный крестик),
 *  внутри — разделённые блоки: бонусы (тапаются → покупка в инвентарь),
 *  +10 монет по таймеру, кнопки-ссылки. */
@Composable
fun ShopScreen(player: PlayerStore, onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalContext.current as? android.app.Activity
    val click = LocalClickSound.current
    val feedback = rememberFeedback()
    // строка вычислена в композиции: из onClick-лямбды L() вызывать нельзя
    val termsTitle = L("terms")
    val stockNote = L("bonus_stock_note")
    val tapToBuy = L("tap_to_buy")
    // открытые юридические условия (карточка поверх магазина)
    var legal by remember { mutableStateOf<Pair<String, String>?>(null) }
    // триггер «дождя» монет: растёт при каждом получении +10
    var rain by remember { mutableIntStateOf(0) }
    // какой бонус покупаем (диалог с ценой и «Бесплатно»)
    var buy by remember { mutableStateOf<PowerUp?>(null) }

    Box(Modifier.fillMaxSize().background(Palette.skyGradient)) {
        GameFrame(
            title = L("bonus_title").uppercase(),
            onClose = { click(); onClose() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(10.dp),
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                // блок бонусов за монеты
                Column(
                    Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE8EEF7))))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(tapToBuy, color = Color(0xFF5A6B8C), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    BonusRow({ DrillIcon(Modifier.size(48.dp)) }, L("drill"), "${Economy.PRICE_DRILL}", player.bonusCount(PowerUp.DRILL)) { buy = PowerUp.DRILL }
                    BonusRow({ FlaskIcon(Modifier.size(48.dp)) }, L("melt"), "${Economy.PRICE_MELT}", player.bonusCount(PowerUp.MELT)) { buy = PowerUp.MELT }
                    BonusRow({ HammerIcon(Modifier.size(48.dp)) }, L("seal"), "${Economy.PRICE_SEAL}", player.bonusCount(PowerUp.SEAL)) { buy = PowerUp.SEAL }
                    BonusRow({ BoxIcon(Modifier.size(48.dp)) }, L("open_box"), "${Economy.PRICE_BOX}", player.bonusCount(PowerUp.BOX)) { buy = PowerUp.BOX }
                }
                Spacer(Modifier.height(8.dp))
                ShopCoinsCard(player) { rain++ }
                Spacer(Modifier.height(8.dp))
                // блок кнопок-ссылок (ссылки — те же, что в Tetris)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE8EEF7))))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    LinkRow({ Text("⭐", fontSize = 18.sp) }, L("rate")) { click(); Links.openAppPage(context) }
                    LinkRow({ Text("📄", fontSize = 18.sp) }, termsTitle) { click(); legal = termsTitle to TERMS_OF_USE_TEXT }
                    // каталог — через браузер: RuStore перехватывает ссылку и «Назад»
                    // из него выбрасывает игрока на рабочий стол вместо возврата в игру
                    LinkRow({ CatalogIcon(Modifier.size(24.dp)) }, L("catalog")) { click(); Links.openCatalog(context) }
                    LinkRow({ TelegramIcon(Modifier.size(24.dp)) }, L("telegram")) { click(); Links.open(context, Links.TELEGRAM) }
                    LinkRow({ Text("❤️", fontSize = 18.sp) }, L("donate")) { click(); Links.open(context, Links.DONATE) }
                }
                // отступ снизу — под рамкой остаётся панель навигации
                Spacer(Modifier.height(96.dp))
            }
        }
        // дождь золотых монет со свечением — поверх всего магазина
        CoinRain(rain, Modifier.fillMaxSize())
    }

    // покупка бонуса в инвентарь: за монеты или за просмотр rewarded-видео;
    // диалог не закрываем — игрок видит, как растёт счётчик «в инвентаре»
    buy?.let { type ->
        val price = Economy.priceOf(type)
        BonusDialog(
            type = type,
            coins = player.coins,
            owned = player.bonusCount(type),
            note = stockNote,
            onBuy = {
                if (player.spendCoins(price)) {
                    player.addBonus(type)
                    feedback.coin(player.sound)
                }
            },
            onFree = {
                val gain = {
                    player.addBonus(type)
                    feedback.coin(player.sound)
                }
                val act = activity
                if (act != null) RewardAdManager.show(act) { gain() } else gain()
            },
            onClose = { buy = null },
        )
    }

    legal?.let { (title, body) ->
        LegalDialog(title = title, body = body, onDismiss = { legal = null })
    }
}

/** +10 монет раз в 6 часов: жёлтый винт, «10», «Получить» → оранжевый таймер «05:59:59». */
@Composable
private fun ShopCoinsCard(player: PlayerStore, onClaimed: () -> Unit) {
    val feedback = rememberFeedback()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val ready = player.shopCoinsReady(now)
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE8EEF7))))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // монета вместо винта: блок про монеты, и картинка та же, что в счётчике.
        // «+10» ТЕПЕРЬ ПОД монетой, а не справа от неё (пользователь: «такое же
        // смещение в бонусах, там где +10» — в карточке монеты текст лез на край
        // круга); колонка слева, кнопка/таймер справа как были.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CoinIcon(Modifier.size(46.dp))
            Text(
                "+${Economy.SHOP_COINS}",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = Color(0xFF7A4A12),
            )
        }
        Spacer(Modifier.width(10.dp))
        Spacer(Modifier.weight(1f))
        if (ready) {
            CartoonButton(
                text = L("get"),
                onClick = {
                    // выдача только по факту списания кулдауна — звук и дождь тогда же
                    if (player.claimShopCoins()) {
                        feedback.coin(player.sound)
                        onClaimed()
                    }
                },
                base = Palette.green,
                edge = Palette.greenDark,
                height = 48.dp,
                fontSize = 16,
                modifier = Modifier.width(140.dp),
            )
        } else {
            // оранжевый таймер вместо кнопки — до следующего получения
            Box(
                Modifier
                    .width(140.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(percent = 45))
                    .background(Brush.verticalGradient(listOf(Palette.orange.lighten(0.12f), Palette.orange))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    shopCountdown(player.shopCoinsWaitMs(now)),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                )
            }
        }
    }
}

/** Таймер магазина в формате «05:59:59». */
private fun shopCountdown(waitMs: Long): String {
    val h = waitMs / 3600_000
    val m = (waitMs % 3600_000) / 60_000
    val s = (waitMs % 60_000) / 1000
    return String.format("%02d:%02d:%02d", h, m, s)
}

/** Строка-ссылка: иконка (эмодзи или Canvas-рисунок) + подпись + стрелка. */
@Composable
private fun LinkRow(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(10.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2A3B63), modifier = Modifier.weight(1f))
        Text("›", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF9AA7C0))
    }
}

/** Строка бонуса в магазине: тап по строке открывает окно покупки в инвентарь. */
@Composable
private fun BonusRow(icon: @Composable () -> Unit, name: String, price: String, owned: Int, onClick: () -> Unit) {
    val click = LocalClickSound.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { click(); onClick() }
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp)) { icon() }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold, color = Color(0xFF2A3B63))
            // сколько уже куплено — применяются в игре по тапу без видео
            if (owned > 0) {
                Text(
                    "× $owned",
                    fontWeight = FontWeight.Black,
                    color = Palette.green,
                    fontSize = 13.sp,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp)) { CoinIcon() }
            Text(" $price", fontWeight = FontWeight.Bold, color = Color(0xFF7A4A12))
            Text(" ›", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF9AA7C0))
        }
    }
}

/** Трофеи: та же объёмная рамка, что в магазине и настройках, + статистика и звание. */
@Composable
fun TrophyScreen(player: PlayerStore, onClose: () -> Unit) {
    val click = LocalClickSound.current
    val wins = player.totalWins
    // звание и путь к следующему — по числу побед
    val (rankKey, nextKey, nextAt) = when {
        wins >= 100 -> Triple("rank_legend", null, 0)
        wins >= 40 -> Triple("rank_super", "rank_legend", 100)
        wins >= 10 -> Triple("rank_pro", "rank_super", 40)
        else -> Triple("rank_novice", "rank_pro", 10)
    }
    val rank = L(rankKey)
    val toNext = if (nextKey != null) L("rank_next", L(nextKey), nextAt - wins) else L("rank_max")
    val stars = (wins / 10).coerceAtMost(5)

    Box(Modifier.fillMaxSize().background(Palette.skyGradient)) {
        GameFrame(
            title = L("trophy").uppercase(),
            onClose = { click(); onClose() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(10.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // кубок на лучах + звание — compact-версия (пользователь: «надо
                // чуть ближе таблицу поднять к тексту, чтобы не было лишнего скрола»):
                // блок 150→128dp, зазоры 10→6 и 14→6, звёзды 34→28dp.
                Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) {
                    Starburst(Modifier.fillMaxWidth())
                    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) { TrophyIcon() }
                }
                Text(rank, color = Color(0xFF2A3B63), fontWeight = FontWeight.Black, fontSize = 22.sp)
                Spacer(Modifier.height(4.dp))
                Text(toNext, color = Color(0xFF5A6B8C), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(5) { Box(Modifier.size(28.dp)) { if (it < stars) StarIcon() } }
                }
                Spacer(Modifier.height(6.dp))
                // статистика — одной белой карточкой, строки разделены линией
                Column(
                    Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE8EEF7))))
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    StatCard(L("best"), "${player.bestLevel - 1}", { StarIcon(Modifier.size(22.dp)) })
                    StatCard(L("wins_total"), "$wins", { TrophyIcon(Modifier.size(22.dp)) })
                    StatCard(L("streak_wins"), "${player.maxWinStreak}", { ScrewIcon(Modifier.size(22.dp)) })
                    StatCard(L("lives_word"), "${player.lives} / ${Economy.MAX_LIVES}", { HeartIcon(Modifier.size(22.dp)) })
                    StatCard(L("coins_now"), "${player.coins}", { CoinIcon(Modifier.size(22.dp)) })
                    StatCard(L("total_claimed"), "${player.piggyClaimed}", { PigIcon(Modifier.size(22.dp)) }, divider = false)
                }
                Spacer(Modifier.height(72.dp))
            }
        }
    }
}

/** Строка статистики: иконка, подпись слева, значение справа; разделитель — тонкая линия. */
@Composable
private fun StatCard(label: String, value: String, icon: @Composable () -> Unit, divider: Boolean = true) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(22.dp)) { icon() }
            Spacer(Modifier.width(10.dp))
            Text(label, fontWeight = FontWeight.Bold, color = Color(0xFF2A3B63), fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(value, fontWeight = FontWeight.Black, color = Palette.orangeDark, fontSize = 20.sp)
        }
        if (divider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFDDE4EF)),
            )
        }
    }
}
