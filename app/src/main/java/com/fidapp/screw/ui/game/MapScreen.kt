package com.fidapp.screw.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidapp.screw.game.Economy
import com.fidapp.screw.game.PlayerStore
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin

/** Карта уровней: спиральная лента узлов, боковые кнопки, «Играть». */
@Composable
fun MapScreen(
    player: PlayerStore,
    onPlay: (Int) -> Unit,
) {
    var showLives by remember { mutableStateOf(false) }
    var showPiggy by remember { mutableStateOf(false) }
    var showMy by remember { mutableStateOf(false) }
    var showAvatars by remember { mutableStateOf(false) }
    var showFreeCoins by remember { mutableStateOf(false) }

    val endLevel = player.bestLevel + 4
    val startLevel = (player.bestLevel - 4).coerceAtLeast(1)
    val click = LocalClickSound.current

    // лента уровней: текущий узел нужно сразу показать ПО СЕРЕДИНЕ окна ленты,
    // иначе он уезжает за кнопку «Играть» и игрок должен свайпать.
    // REFACTOR (детерминированный): высота строки/соединителя — известные константы,
    // поэтому целевой скролл считается АНАЛИТИЧЕСКИ (viewHeight известен из
    // BoxWithConstraints.maxHeight). Никакого onGloballyPositioned/boundsInRoot для
    // центра узла — те измерение после verticalScroll давало CONTENT center вместо
    // VIEWPORT center, delta~0 и лента не ехала.
    val stripScroll = rememberScrollState()
    val rowH = 70.dp
    val connH = 26.dp
    val contentPad = 6.dp

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.skyGradient),
    ) {
        Column(Modifier.fillMaxSize()) {
            // верхняя панель — шестерёнка УБРАНА: «Настройки» переехали в
            // нижнее меню (правая вкладка, см. ScrewGame.BottomNav).
            // ПОРЯДОК: монеты → жизни → аватар. Аватар СПРАВА (пользователь:
            // «расположи аватарку справа, сейчас слева»).
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // счётчик монет ШИРЕ прежнего (110→140): зелёный «плюсик» не должен
                // упираться в край плашки — раньше он был визуально «сжат»
                StatPill(player.coins, { CoinIcon(Modifier.size(34.dp)) }, onPlus = { showFreeCoins = true }, showBadge = true, modifier = Modifier.width(140.dp))
                Spacer(Modifier.width(8.dp))
                // жизни — между монетами и аватаром
                LivesPill(player, Modifier.width(120.dp)) { showLives = true }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { click(); showMy = true },
                ) { AvatarBox(player.avatar, 44.dp) }
            }

            // кошелёк: ОТДЕЛИЛИ от верхней панели — добавили Spacer(14dp),
            // чтобы плашка не «слипалась» с монетами/жизнями
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WalletButton(player) { showPiggy = true }
            }
            Spacer(Modifier.height(10.dp))

            // спиральная лента уровней: синусоида по X, плавная кривая-«река»
            // между соседними узлами; текущий узел — в центре видимой области.
            // BoxWithConstraints даёт viewport-высоту ДО композиции дочерних,
            // так что целевой скролл = nodeCenter - viewport/2 считается точно.
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val viewportPx = with(LocalDensity.current) { maxHeight.toPx() }
                // индекс текущего узла в цикле: number = endLevel - i, number == bestLevel
                // => iCur = endLevel - bestLevel (обычно 4, т.к. endLevel = bestLevel + 4)
                val iCur = (endLevel - player.bestLevel).coerceIn(0, (endLevel - startLevel))
                // центр текущего узла от ВЕРХА КОНТЕНТА (до скролла)
                val nodeCenterPx = with(LocalDensity.current) {
                    (contentPad + (rowH + connH) * iCur.toFloat() + rowH / 2).toPx()
                }
                LaunchedEffect(player.bestLevel, viewportPx) {
                    // подождаемся, пока stripScroll.maxValue рассчитается после layout
                    delay(80)
                    val target = (nodeCenterPx - viewportPx / 2f).roundToInt()
                        .coerceIn(0, stripScroll.maxValue)
                    stripScroll.scrollTo(target)
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(stripScroll)
                        .padding(vertical = contentPad),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val rowCount = endLevel - startLevel + 1
                    // фаза 0.95 рад/шаг ~ полный «виток» на ~6.6 узлов; амплитуда 90dp
                    val phase = 0.95
                    for (i in 0 until rowCount) {
                        val number = endLevel - i
                        val xDp = (sin(i * phase).toFloat() * 90f).dp
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(rowH),
                            contentAlignment = Alignment.Center,
                        ) {
                            LevelNode(
                                number,
                                player,
                                modifier = Modifier
                                    .offset(x = xDp)
                                    .zIndex(1f),
                            ) { onPlay(number) }
                        }
                        if (i < rowCount - 1) {
                            val nextX = (sin((i + 1) * phase).toFloat() * 90f).dp
                            CurveConnector(xDp, nextX, height = connH)
                        }
                    }
                }
            }

            // «Играть» — выше нижней панели ГЛАВНАЯ/FREE и системных кнопок
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 74.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // «Играть» — КРУПНЕЕ по высоте и ширине (по просьбе пользователя):
                // 260×64 → fillMaxWidth+horizontal 16dp (≈ 328dp на 720-пикс. экране),
                // высота 64 → 80dp, текст 30 → 34sp; винты 44dp по бокам
                Box(Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)) {
                    CartoonButton(
                        text = L("play"),
                        onClick = { onPlay(player.bestLevel) },
                        base = Palette.green,
                        edge = Palette.greenDark,
                        modifier = Modifier.fillMaxWidth(),
                        height = 80.dp,
                        fontSize = 34,
                    )
                    ScrewIcon(
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 18.dp)
                            .size(44.dp),
                    )
                    ScrewIcon(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 18.dp)
                            .size(44.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                // «Задание дня» — НАОБОРОТ уже (200→170dp), винты МЕЛЬЧЕ (40→22dp),
                // текст 14→12sp, высота 64→50dp; по-прежнему красная с СИНИМИ винтами
                Box {
                    CartoonButton(
                        text = L("daily"),
                        onClick = { onPlay(player.bestLevel + 5) },
                        base = Palette.red,
                        edge = Palette.redDark,
                        modifier = Modifier.width(170.dp),
                        height = 50.dp,
                        fontSize = 12,
                    )
                    ScrewIcon(
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 6.dp)
                            .size(22.dp),
                        color = Palette.blue,
                    )
                    ScrewIcon(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                            .size(22.dp),
                        color = Palette.blue,
                    )
                }
            }
        }
    }

    if (showLives) LivesDialog(player) { showLives = false }
    if (showFreeCoins) FreeCoinsDialog(player) { showFreeCoins = false }
    if (showPiggy) PiggyDialog(player) { showPiggy = false }
    if (showMy) MyDialog(player, onOpenAvatars = { showAvatars = true }) { showMy = false }
    if (showAvatars) AvatarDialog(player) { showAvatars = false }
}

@Composable
private fun LivesPill(player: PlayerStore, modifier: Modifier = Modifier, onPlus: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .clickable { onPlus() },
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color(0xFFFDF3D8)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                HeartIcon(Modifier.size(30.dp))
                // число чуть ниже — центр сердечка смещён вниз
                Text("${player.lives}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.offset(y = 3.dp))
            }
            Text(
                countdownLabel(player.nextLifeAt, now),
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = Color(0xFF5A4A2A),
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

@Composable
private fun countdownLabel(nextAt: Long, now: Long): String {
    if (nextAt == 0L) return L("ready")
    val left = (nextAt - now).coerceAtLeast(0)
    val m = left / 60000
    val s = (left % 60000) / 1000
    return String.format("%d:%02d", m, s)
}

/** Жёлтый «кошелёк» с торцем-тенью: шириной как «Играть», иконка + прогресс копилки + подпись. */
@Composable
private fun WalletButton(player: PlayerStore, onClick: () -> Unit) {
    val click = LocalClickSound.current
    val shape = RoundedCornerShape(percent = 45)
    Box(Modifier.width(260.dp).height(56.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Palette.yellowDark)
                .clickable { click(); onClick() },
        )
        Row(
            Modifier
                .fillMaxSize()
                .offset(y = (-3).dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Palette.yellow.lighten(0.15f), Palette.yellow)))
                // оранжевая рамка темнее жёлтого фона — кнопка читается как объёмная
                .border(2.5.dp, Color(0xFFC98200), shape),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = Color(0xFF141C2E),
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "${player.piggy}/${Economy.PIGGY_GOAL}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Palette.green)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(L("piggy"), color = Color(0xFF6B4E00), fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
private fun LevelNode(
    number: Int,
    player: PlayerStore,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val click = LocalClickSound.current
    val unlocked = player.isUnlocked(number)
    val isCurrent = number == player.bestLevel
    val color = when {
        !unlocked -> Palette.mapLocked
        isCurrent -> Palette.mapCurrent
        else -> Palette.mapDone
    }
    // кнопка уровня: текущая — вдвое шире, чтобы винты влезли слева и справа от числа
    val tileW = if (isCurrent) 138.dp else 70.dp
    val tileH = 64.dp
    Box(
        modifier
            .width(tileW + 8.dp)
            .height(tileH + 8.dp)
            .padding(4.dp),
    ) {
        // «речка»-подложка
        Box(
            Modifier
                .align(Alignment.Center)
                .size(width = tileW + 4.dp, height = tileH + 4.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White),
        )
        // кнопка уровня: БЕЛАЯ кайма у всех; номер; у текущего — жёлтый винт и жёлтая рамка
        Box(
            Modifier
                .align(Alignment.Center)
                .size(width = tileW, height = tileH)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(color.lighten(0.25f), color)))
                .border(
                    3.dp,
                    if (isCurrent) Palette.yellow else Color.White,
                    RoundedCornerShape(16.dp),
                )
                .clickable(enabled = unlocked) { click(); onClick() },
            contentAlignment = Alignment.Center,
        ) {
            if (isCurrent) {
                // у текущего: крупный жёлтый винт слева и справа от числа (как на «Играть»)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ScrewIcon(Modifier.size(26.dp))
                    Text("$number", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    ScrewIcon(Modifier.size(26.dp))
                }
            } else {
                Text("$number", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
        if (!unlocked) {
            Text("🔒", fontSize = 14.sp, modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

/**
 * Плавная кубическая кривая-«река» между двумя соседними узлами ленты:
 * рисуется в Box ширины ленты, от (fromX + center, 0) до (toX + center, height).
 * Используется вместо прежнего `PathSegment` (короткий повёрнутый прямоугольник)
 * — «спиралька», о которой просил пользователь: визуально лента читается
 * как текучая дорожка, а не лесенка.
 */
@Composable
private fun CurveConnector(fromX: Dp, toX: Dp, height: Dp) {
    val density = LocalDensity.current
    val strokePx = with(density) { 10.dp.toPx() }
    Box(Modifier.fillMaxWidth().height(height)) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val from = cx + with(density) { fromX.toPx() }
            val to = cx + with(density) { toX.toPx() }
            val path = Path().apply {
                moveTo(from, 0f)
                cubicTo(
                    from, size.height * 0.45f,
                    to, size.height * 0.55f,
                    to, size.height,
                )
            }
            // белая подложка шире — «берег», как в парковых дорогах
            drawPath(
                path,
                color = Color.White,
                style = Stroke(width = strokePx + with(density) { 4.dp.toPx() }, cap = StrokeCap.Round),
            )
            drawPath(
                path,
                color = Palette.mapPath,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
    }
}
