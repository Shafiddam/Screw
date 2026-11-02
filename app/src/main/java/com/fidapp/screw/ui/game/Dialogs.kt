package com.fidapp.screw.ui.game

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fidapp.screw.BuildConfig
import com.fidapp.screw.ads.RewardAdManager
import com.fidapp.screw.game.Economy
import com.fidapp.screw.game.PlayerStore
import com.fidapp.screw.game.PowerUp
import com.fidapp.screw.notify.Reminders
import kotlinx.coroutines.delay

/** Тело настроек (вибрация/звук/музыка/напоминания) — переиспользуется в паузе и на карте. */
@Composable
fun SettingsBody(player: PlayerStore) {
    val context = LocalContext.current
    var reminders by remember { mutableStateOf(player.reminders) }

    fun enableReminders() {
        player.reminders = true
        reminders = true
        Reminders.ensureChannel(context)
        Reminders.schedule(context)
        // в debug — самопроверка: через ~3с покажем тестовое уведомление
        if (BuildConfig.DEBUG) Reminders.debugTest(context)
    }

    // Android 13+ спрашивает разрешение на уведомления в рантайме;
    // ниже — тумблер включает джобу сразу
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            enableReminders()
        } else {
            // отказали — тумблер должен честнее показать выключенное состояние
            player.reminders = false
            reminders = false
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        SwitchRow({ /* phone */ Text("📳", fontSize = 26.sp) }, L("vibration"), player.vibration) { player.vibration = it }
        SwitchRow({ Text("🔊", fontSize = 26.sp) }, L("sound"), player.sound) { player.sound = it }
        SwitchRow({ Text("🎵", fontSize = 26.sp) }, L("music"), player.music) { player.music = it }
        SwitchRow({ Text("🔔", fontSize = 26.sp) }, L("reminders"), reminders) { on ->
            if (!on) {
                player.reminders = false
                reminders = false
                Reminders.cancel(context)
            } else if (Reminders.hasPermission(context)) {
                enableReminders()
            } else {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun SettingsDialog(player: PlayerStore, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        GameDialog(L("settings"), onClose) {
            SettingsBody(player)
        }
    }
}

/** Окно бонуса: покупка за монеты или «Бесплатно» (rewarded-видео). Одно и то же
 *  окно служит магазину (покупка в инвентарь) и игре (применение сейчас). */
@Composable
fun BonusDialog(
    type: PowerUp,
    coins: Int,
    onBuy: () -> Unit,
    onFree: () -> Unit,
    onClose: () -> Unit,
    owned: Int = 0,
    note: String? = null,
) {
    val (title, desc, price) = when (type) {
        PowerUp.DRILL -> Triple(L("drill"), L("drill_desc"), Economy.PRICE_DRILL)
        PowerUp.MELT -> Triple(L("melt"), L("melt_desc"), Economy.PRICE_MELT)
        PowerUp.SEAL -> Triple(L("seal"), L("seal_desc"), Economy.PRICE_SEAL)
        PowerUp.BOX -> Triple(L("open_box"), L("box_desc"), Economy.PRICE_BOX)
    }
    Dialog(onDismissRequest = onClose) {
        GameDialog(title, onClose) {
            Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                Starburst(Modifier.fillMaxWidth())
                Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) { BonusIcon(type) }
            }
            Spacer(Modifier.height(6.dp))
            Text(desc, color = Color(0xFF7A4A12), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (owned > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    L("in_stock", owned),
                    color = Palette.green,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                )
            }
            if (note != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    note,
                    color = Color(0xFF5A6B8C),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CartoonButton(
                    text = "$price",
                    onClick = onBuy,
                    base = Palette.blue,
                    icon = { CoinIcon(Modifier.size(24.dp)) },
                    enabled = coins >= price,
                    modifier = Modifier.weight(1f),
                )
                CartoonButton(
                    text = L("free"),
                    onClick = onFree,
                    base = Palette.yellow,
                    edge = Palette.yellowDark,
                    textColor = Color(0xFF5A4200),
                    icon = { VideoIcon(Modifier.size(20.dp), Color(0xFF5A4200)) },
                    fontSize = 12,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BonusIcon(type: PowerUp) {
    when (type) {
        PowerUp.DRILL -> DrillIcon(Modifier.size(140.dp))
        PowerUp.MELT -> FlaskIcon(Modifier.size(140.dp))
        PowerUp.SEAL -> HammerIcon(Modifier.size(140.dp))
        PowerUp.BOX -> BoxIcon(Modifier.size(140.dp))
    }
}

@Composable
fun WinDialog(reward: Int, doubled: Boolean, onDouble: () -> Unit, onCollect: () -> Unit, onHome: () -> Unit) {
    val click = LocalClickSound.current
    Dialog(onDismissRequest = onCollect) {
        // Лучи + серпантин: пользователь просил «добавить анимацию серпантина
        // и лучей» — Starburst под шапкой (вращающиеся солнечные лучи как в
        // ReviveBurst) и Confetti (падающие цветные прямоугольники со вращением)
        // поверх всего содержимого.
        Box {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Starburst(
                    Modifier.size(340.dp).align(Alignment.TopCenter).padding(top = 30.dp),
                    rays = 18,
                    inner = Color(0xFFFFF3C4),
                    outer = Palette.yellow,
                )
            }
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("👑", fontSize = 68.sp)
                Spacer(Modifier.height(2.dp))
                // «Победа» — просто крупная надпись, не похожая на кнопку
                Text(
                    L("win"),
                    color = Color(0xFFFFD23F),
                    fontWeight = FontWeight.Black,
                    fontSize = 42.sp,
                )
                Spacer(Modifier.height(14.dp))
                // награда уровня + кнопка удвоения за rewarded-видео
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(Modifier.size(32.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("+$reward", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
                }
                if (!doubled) {
                    // 40dp пустоты между числом монет и кнопкой «Удвоить награду»
                    // — раньше 12dp, «слипались» в один блок (просьба пользователя)
                    Spacer(Modifier.height(40.dp))
                    CartoonButton(
                        text = L("double"),
                        onClick = onDouble,
                        base = Palette.yellow,
                        edge = Palette.yellowDark,
                        textColor = Color(0xFF5A4200),
                        icon = { VideoIcon(Modifier.size(22.dp), Color(0xFF5A4200)) },
                        fontSize = 16,
                        height = 52.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(18.dp))
                // «Следующий уровень» — по центру, на всю ширину
                CartoonButton(
                    text = L("next_level"),
                    onClick = onCollect,
                    base = Palette.green,
                    edge = Palette.greenDark,
                    modifier = Modifier.fillMaxWidth(),
                    height = 60.dp,
                    fontSize = 24,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    L("to_home"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { click(); onHome() }
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                )
            }
            // серпантин — поверх всего, не перехватывает тапы
            // (matchParentSize — расширение BoxScope, отдельный импорт не нужен)
            Confetti(Modifier.matchParentSize())
        }
    }
}

/**
 * Падающий серпантин для окна победы: 46 цветных «конфетти» с индивидуальной
 * скоростью, фазой и вращением. Детерминированно (без Random в кадре) — все
 * параметры считаются от индекса, поэтому при перерисовке ленточки не «телеport».
 * `Modifier.matchParentSize()` + отсутствие clickable → табы проходят сквозь.
 */
@Composable
private fun Confetti(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "confetti")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "fall",
    )
    val colors = listOf(
        Color(0xFFFF5A5A), Color(0xFFFFD23F), Color(0xFF4DD0E1), Color(0xFF8E44AD),
        Color(0xFF66BB6A), Color(0xFFFF8A3D), Color(0xFFEC407A), Color(0xFF3D5AFE),
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val count = 46
        for (i in 0 until count) {
            // псевдослучайные параметры из индекса (стабильны между кадрами)
            val seed = (i * 37) % 100 / 100f
            val seed2 = (i * 53 % 97) / 97f
            val speed = 0.6f + seed * 0.9f
            val phase = seed2
            val x = (seed * 0.83f + seed2 * 0.17f).coerceIn(0f, 1f) * w
            val yRaw = ((t * speed + phase) % 1f)
            val y = yRaw * (h + 40f) - 20f
            val rot = (t * 720f * speed + i * 45f)
            val color = colors[i % colors.size]
            val cw = 8f + (i % 5) * 2f
            val chh = 14f + (i % 3) * 4f
            rotate(rot, pivot = Offset(x, y)) {
                drawRect(color, Offset(x - cw / 2f, y - chh / 2f), Size(cw, chh))
            }
        }
    }
}

@Composable
fun LoseDialog(onHome: () -> Unit, onRetry: () -> Unit, onContinue: () -> Unit) {
    Dialog(onDismissRequest = onHome) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("💔", fontSize = 60.sp)
            Spacer(Modifier.height(2.dp))
            // «Вы проиграли» — просто крупная надпись, БЕЗ подложки-«кнопки»
            Text(
                L("lose"),
                color = Color(0xFFFF6B6B),
                fontWeight = FontWeight.Black,
                fontSize = 40.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                L("lose_desc"),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(16.dp))
            // «Продолжить» за rewarded-видео: возвращает жизнь и перезаводит уровень
            CartoonButton(
                text = L("continue"),
                onClick = onContinue,
                base = Palette.green,
                edge = Palette.greenDark,
                icon = { VideoIcon(Modifier.size(20.dp), Color.White) },
                fontSize = 15,
                height = 52.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CartoonButton(L("to_home"), onHome, base = Palette.blue, modifier = Modifier.weight(1f))
                CartoonButton(L("retry"), onRetry, base = Palette.yellow, edge = Palette.yellowDark, textColor = Color(0xFF5A4200), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ReviveDialog(coins: Int, onReviveCoins: () -> Unit, onReviveFree: () -> Unit, onDecline: () -> Unit) {
    Dialog(onDismissRequest = onDecline) {
        GameDialog(L("revive"), onDecline) {
            ReviveBurst(Modifier.size(150.dp))
            Text(L("revive_desc"), color = Color(0xFF7A4A12), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CartoonButton(
                    text = "${Economy.PRICE_REVIVE}",
                    onClick = onReviveCoins,
                    base = Palette.blue,
                    icon = { CoinIcon(Modifier.size(24.dp)) },
                    enabled = coins >= Economy.PRICE_REVIVE,
                    modifier = Modifier.weight(1f),
                )
                CartoonButton(
                    text = L("free"),
                    onClick = onReviveFree,
                    base = Palette.yellow,
                    edge = Palette.yellowDark,
                    textColor = Color(0xFF5A4200),
                    icon = { VideoIcon(Modifier.size(20.dp), Color(0xFF5A4200)) },
                    fontSize = 12,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** «Восполнить жизни» — +2 за монеты или +1 бесплатно (за rewarded-видео). */
@Composable
fun LivesDialog(player: PlayerStore, onClose: () -> Unit) {
    // `now` — тикающий каждые 1с state; countdown обязан READ its value,
    // иначе Compose не перерисует Text (считает, что аргументы не поменялись),
    // и таймер «замерзнет» на первом значении — ровно та жалоба пользователя.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val feedback = rememberFeedback()
    val activity = LocalContext.current as? android.app.Activity
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    Dialog(onDismissRequest = onClose) {
        // Красная рамка — окна «Восполнить жизни» (пользователь: «фон рамки окна
        // восполнить жизни замени на красный»).
        GameDialog(L("refill_lives"), onClose, theme = FrameTheme.Red) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                HeartIcon(Modifier.fillMaxSize())
                // число чуть ниже геометрического центра — центр сердечка смещён вниз
                Text("${player.lives}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, modifier = Modifier.offset(y = 6.dp))
            }
            Spacer(Modifier.height(2.dp))
            if (player.nextLifeAt == 0L) {
                Text(L("ready"), color = Palette.green, fontWeight = FontWeight.Black, fontSize = 15.sp)
            } else {
                Text(L("next_refill"), color = Color(0xFF7A4A12), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                // ЯВНО читаем `now` — иначе Text не перекомпозируется и часы замирают
                Text(countdown(player.nextLifeAt, now), color = Color(0xFF2A3B63), fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
            Spacer(Modifier.height(10.dp))
            // ЖИЗНИ ЗАФИКСИрованы лимитом MAX_LIVES=5 — «внутреннего счётчика про
            // запас» нет, addLives() обрезает по максимуму. Раньше при 5 жизнях тап
            // по «за монеты» СПИСЫВАЛ 300 монет, а жизни оставались 5 (жалоба
            // пользователя). Теперь: при полных жизнях обе кнопки неактивны, а при
            // неполных берётся только то число жизней, которое влезет (150/жизнь).
            val room = (Economy.MAX_LIVES - player.lives).coerceAtLeast(0)
            val pricePerLife = Economy.PRICE_LIVES_2 / 2
            val coinPrice = room * pricePerLife
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("за монеты", color = Color(0xFF5A6B8C), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(2.dp))
                    CartoonButton(
                        text = if (room > 0) "+$room ♥ · $coinPrice" else "♥ ${Economy.MAX_LIVES}/${Economy.MAX_LIVES}",
                        onClick = {
                            if (room > 0 && player.spendCoins(coinPrice)) {
                                feedback.coin(player.sound)
                                player.addLives(room); onClose()
                            }
                        },
                        base = Palette.blue,
                        icon = { CoinIcon(Modifier.size(22.dp)) },
                        enabled = room > 0 && player.coins >= coinPrice,
                        fontSize = 13,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("за рекламу", color = Color(0xFF5A6B8C), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(2.dp))
                    CartoonButton(
                        text = if (room > 0) "+1 ♥ · видео" else "♥ ${Economy.MAX_LIVES}/${Economy.MAX_LIVES}",
                        onClick = {
                            // +1 жизнь за просмотр rewarded-видео (награда только в onReward);
                            // при полных жизнях кнопка неактивна — видео не тратится зря
                            if (room <= 0) return@CartoonButton
                            val act = activity
                            if (act != null) RewardAdManager.show(act) {
                                feedback.coin(player.sound)
                                player.addLives(1); onClose()
                            } else {
                                feedback.coin(player.sound)
                                player.addLives(1); onClose()
                            }
                        },
                        base = Palette.yellow,
                        edge = Palette.yellowDark,
                        textColor = Color(0xFF5A4200),
                        icon = { VideoIcon(Modifier.size(20.dp), Color(0xFF5A4200)) },
                        enabled = room > 0,
                        fontSize = 13,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Бесплатные +10 монет раз в 12 часов (по «плюсику» на плашке монет). */
@Composable
fun FreeCoinsDialog(player: PlayerStore, onClose: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val feedback = rememberFeedback()
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val ready = player.freeCoinsReady(now)
    Dialog(onDismissRequest = onClose) {
        // Заголовок: «Монеты» → «Получить монеты» (просьба пользователя, ключ
        // free_coins_get в Labels.kt). titleFontSize чуть уменьшен — строка длиннее.
        // Зелёная рамка (пользователь: ««получить монеты» — на зеленый»).
        GameDialog(L("free_coins_get"), onClose, titleFontSize = 15, theme = FrameTheme.Green) {
            // Верхний мини-блок (мелкая монета + «+10» над большой картинкой)
            // УБРАН по просьбе пользователя: «убери лишнюю сверху число и картинку —
            // оставь только крупную». Остался один большой CoinIcon на лучах.
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                Starburst(Modifier.fillMaxWidth(), outer = Color(0xFFFFF1B8))
                Box(Modifier.size(110.dp)) { CoinIcon() }
            }
            // «+10» ВЫНЕСЕНЫ ИЗ-ПОД монеты (пользователь: «+10 прямо на монете —
            // надо ниже»): раньше висели на BottomCenter Box'а и перекрывали низ
            // картинки.
            Text("+${Economy.FREE_COINS}", color = Palette.green, fontWeight = FontWeight.Black, fontSize = 30.sp)
            Spacer(Modifier.height(10.dp))
            if (ready) {
                CartoonButton(
                    text = L("get") + " +${Economy.FREE_COINS}",
                    onClick = { player.claimFreeCoins(); feedback.coin(player.sound); onClose() },
                    base = Palette.green,
                    edge = Palette.greenDark,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(L("next_free_in"), color = Color(0xFF7A4A12), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(longCountdown(player.freeCoinsWaitMs(now)), color = Color(0xFF2A3B63), fontWeight = FontWeight.Black, fontSize = 24.sp)
                Spacer(Modifier.height(12.dp))
                // кнопка явно «неактивная»: бледная, серая надпись — не манит нажать
                CartoonButton(
                    text = L("get"),
                    onClick = {},
                    base = Color(0xFFE4D8AC),
                    edge = Color(0xFFBFB17F),
                    textColor = Color(0xFF9A9384),
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun PiggyDialog(player: PlayerStore, onClose: () -> Unit) {
    val feedback = rememberFeedback()
    Dialog(onDismissRequest = onClose) {
        // Золотая рамка — в цвет самой кнопки-кошелька (пользователь: «фон в
        // кошельке смени с синего на такой же как у кнопки — там, где заголовок
        // и крестик»). GameFrame(theme=Gold) красит кант и шапку в Palette.yellow.
        GameDialog(L("piggy"), onClose, theme = FrameTheme.Gold) {
            Text(
                L("piggy_desc"),
                color = Color(0xFF7A5A00),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                Starburst(Modifier.fillMaxWidth(), outer = Color(0xFFFFF1B8))
                WalletIcon(Modifier.size(130.dp))
            }
            Spacer(Modifier.height(12.dp))
            ProgressLine(player.piggy, Economy.PIGGY_GOAL)
            Spacer(Modifier.height(18.dp))
            // при пустой копилке — кнопка явно неактивна и без «Забрать 0»
            val canClaim = player.piggy > 0
            CartoonButton(
                text = if (canClaim) L("claim") + " ${player.piggy}" else L("claim"),
                onClick = { if (canClaim) { player.claimPiggy(); feedback.coin(player.sound); onClose() } },
                base = if (canClaim) Palette.green else Color(0xFFE4D8AC),
                edge = if (canClaim) Palette.greenDark else Color(0xFFBFB17F),
                textColor = if (canClaim) Color.White else Color(0xFF9A9384),
                enabled = canClaim,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProgressLine(value: Int, goal: Int) {
    val frac = (value.toFloat() / goal).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFD6A400)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Palette.green),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("$value/$goal", color = Color(0xFF6B4E00), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun countdown(nextAt: Long, now: Long = System.currentTimeMillis()): String {
    if (nextAt == 0L) return "∞"
    val left = (nextAt - now).coerceAtLeast(0)
    val m = left / 60000
    val s = (left % 60000) / 1000
    return String.format("%d:%02d", m, s)
}

/** Обратный отсчёт в формате Ч:ММ:СС (для кулдауна бесплатных монет). */
private fun longCountdown(waitMs: Long): String {
    val left = waitMs.coerceAtLeast(0)
    val h = left / 3600000
    val m = (left % 3600000) / 60000
    val s = (left % 60000) / 1000
    return String.format("%d:%02d:%02d", h, m, s)
}
