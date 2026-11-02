package com.fidapp.screw.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/** Мультяшная «объёмная» кнопка: цветной верх + тёмный нижний торец. */
@Composable
fun CartoonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    base: Color = Palette.blue,
    edge: Color = Palette.blueDark,
    textColor: Color = Color.White,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    fontSize: Int = 20,
) {
    val shape = RoundedCornerShape(percent = 45)
    val click = LocalClickSound.current
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(edge)
            .clickable(enabled = enabled) { click(); onClick() },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = (-3).dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(base.lighten(0.15f), base))),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Box(Modifier.size(height * 0.5f)) { icon() }
                    Spacer(Modifier.width(6.dp))
                }
                Text(text, color = textColor, fontWeight = FontWeight.Black, fontSize = fontSize.sp)
            }
        }
    }
}

@Composable
fun StatPill(
    value: Int,
    icon: @Composable () -> Unit,
    onPlus: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
) {
    val shape = RoundedCornerShape(percent = 50)
    val click = LocalClickSound.current
    Box(modifier = modifier.height(44.dp)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color(0xFFFDF3D8)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).padding(3.dp), contentAlignment = Alignment.Center) { icon() }
            Text(
                "$value",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color(0xFF5A4A2A),
                modifier = Modifier.padding(end = 10.dp),
            )
            if (onPlus != null) {
                // КРУГ, а не овал: раньше Box был 30dp с внутренним padding(end=4)
                // — Clip shape.CircleShape становился 26×30 и «плюс» выглядел
                // сплюснутым справа (особенно в узкой плашке 120dp в игре).
                // Теперь: 32dp круг + 6dp отступ от края пилюли.
                Box(
                    Modifier
                        .padding(end = 6.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Palette.green)
                        .clickable { click(); onPlus() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }
        }
        if (showBadge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Palette.red),
            )
        }
    }
}

/**
 * Тема цветного «кана» рамки диалога. Синяя — дефолт (настройки/бонус/паузa),
 * золотая — «Кошелёк» (в тон жёлтой кнопке), красная — «Восполнить жизни»,
 * зелёная — «Получить монеты». Пользователь просил крася под стать содержимому.
 */
enum class FrameTheme { Blue, Gold, Red, Green }

/** Голубая рамка диалога: объёмный «кант» (светлый верх → тёмный низ), тень и шапка с шурупами и красным крестиком. */
@Composable
fun GameDialog(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    titleFontSize: Int = 18,
    theme: FrameTheme = FrameTheme.Blue,
    content: @Composable () -> Unit,
) {
    GameFrame(title, onClose, modifier.width(320.dp), titleFontSize, theme, content)
}

/**
 * Рамка стиля диалогов в произвольной ширине (магазин использует её на всю
 * ширину экрана). Внутри — шапка с табличкой заголовка, винтиками и крестиком.
 * [theme] красит кант/шапку/винтики в цвет, подобранный под содержимое окна.
 */
@Composable
fun GameFrame(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    titleFontSize: Int = 18,
    theme: FrameTheme = FrameTheme.Blue,
    content: @Composable () -> Unit,
) {
    val click = LocalClickSound.current
    val shape = RoundedCornerShape(28.dp)
    // Подборка тонов под тему: кант (светлый верх → тёмный низ), шапка-табличка
    // и «винтики-×». Дефолт — синий (panelBlue…), остальные — по Palette.
    val (rimTop, rimMid, rimBot, chipTop, chipBot, dotBg, dotX) = when (theme) {
        FrameTheme.Blue -> Septet(
            Palette.panelBlue, Palette.panelBlueDark, Palette.panelEdge,
            Palette.panelBlue.lighten(0.15f), Palette.panelBlueDark,
            Palette.panelBlueDark, Color(0xFFD6E9FF),
        )
        FrameTheme.Gold -> Septet(
            Palette.yellow, Palette.yellowDark, Color(0xFF8A5A00),
            Palette.yellow.lighten(0.15f), Palette.yellowDark,
            Color(0xFF8A5A00), Color(0xFFFFF1B8),
        )
        FrameTheme.Red -> Septet(
            Palette.red, Palette.redDark, Color(0xFF7F0000),
            Palette.red.lighten(0.15f), Palette.redDark,
            Color(0xFF7F0000), Color(0xFFFFD6D6),
        )
        FrameTheme.Green -> Septet(
            Palette.green, Palette.greenDark, Color(0xFF1B5E20),
            Palette.green.lighten(0.15f), Palette.greenDark,
            Color(0xFF1B5E20), Color(0xFFD6F5D6),
        )
    }
    Column(
        modifier
            // объём: мягкая тень + градиентная кайма (светлый верх, тёмный низ) + белый блик сверху
            .shadow(12.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(rimTop, rimMid, rimBot)))
            .border(2.dp, Color.White.copy(alpha = 0.35f), shape)
            .padding(6.dp),
    ) {
        // шапка
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .width(240.dp)
                    .height(44.dp)
                    .shadow(4.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.verticalGradient(listOf(chipTop, chipBot)))
                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = titleFontSize.sp, maxLines = 1)
                Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    // «×»-винтики в тон канта: тёмный кружок + светлый знак
                    FrameDot(dotBg, dotX)
                    FrameDot(dotBg, dotX)
                }
            }
            CloseIcon(
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(30.dp)
                    .clickable { click(); onClose() },
            )
        }
        // тело
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Palette.dialogGradient)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

/** 7 цветов темы рамки (кант top/mid/bot, шапка top/bot, фон и знак «×» винтика). */
private data class Septet(
    val a: Color, val b: Color, val c: Color, val d: Color, val e: Color, val f: Color, val g: Color,
)

/** Винтик-«×» в шапке рамки; фон и знак задаются темой (см. [GameFrame]). */
@Composable
internal fun FrameDot(bg: Color, cross: Color) {
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text("×", color = cross, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

@Composable
internal fun ScrewDot() {
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(Palette.panelBlueDark),
        contentAlignment = Alignment.Center,
    ) {
        Text("×", color = Color(0xFFD6E9FF), fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

/** Лучистый «взрыв» на фоне иконок бонусов (как на скриншотах «Сверлить/Плавить»). */
@Composable
fun Starburst(modifier: Modifier = Modifier, rays: Int = 20, inner: Color = Color(0xFFFFF1B8), outer: Color = Color(0xFFFFD54F)) {
    Canvas(modifier = modifier) {
        val c = Offset(center.x, center.y)
        val r = size.minDimension / 2f
        drawCircle(Brush.radialGradient(listOf(inner, outer.copy(alpha = 0f)), c, r), r, c)
        for (i in 0 until rays) {
            val a0 = i * (2 * Math.PI / rays)
            val a1 = a0 + (Math.PI / rays) * 0.6
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(c.x, c.y)
                lineTo(c.x + (r * cos(a0)).toFloat(), c.y + (r * sin(a0)).toFloat())
                lineTo(c.x + (r * cos(a1)).toFloat(), c.y + (r * sin(a1)).toFloat())
                close()
            }
            drawPath(path, outer.copy(alpha = 0.45f))
        }
    }
}

/** Переключатель в стиле настроек. */
@Composable
fun SwitchRow(icon: @Composable () -> Unit, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val click = LocalClickSound.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp)) { icon() }
        Spacer(Modifier.width(10.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF4A3B1A), modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = { click(); onChange(it) },
            // без обводки; выключенный тумблер — просто бледнее (общее правило для всех игр)
            modifier = Modifier.alpha(if (checked) 1f else 0.5f),
            colors = SwitchDefaults.colors(
                checkedTrackColor = Palette.green,
                checkedBorderColor = Color.Transparent,
                uncheckedBorderColor = Color.Transparent,
                // выключенный — не серый, а светло-зелёный
                uncheckedTrackColor = Color(0xFFC9E8C9),
            ),
        )
    }
}

/**
 * Экран «Воскрешение»: большое бьюющееся сердечко жизни на фоне вращающихся
 * солнечных лучей (анимация из лучей вместо прежнего зелёного «плюсика»).
 */
@Composable
fun ReviveBurst(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "revive")
    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "spin",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.82f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse",
    )
    val beat by transition.animateFloat(
        initialValue = 0.94f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "beat",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        // вращающиеся лучи
        Starburst(
            Modifier.fillMaxSize().rotate(spin),
            rays = 16,
            inner = Color(0xFFFFF3C4),
            outer = Palette.yellow,
        )
        // мягкое свечение под сердцем
        Box(
            Modifier.size(96.dp).scale(pulse).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFFFE082), Color(0x00FFE082)))),
        )
        HeartIcon(Modifier.size(78.dp).scale(beat))
    }
}

/** Промо-блок «набор» с ценой в рублях (заглушка под IAP). */
@Composable
fun PromoBundle(
    title: String,
    discount: String,
    price: String,
    modifier: Modifier = Modifier,
    pink: Boolean = false,
    onClick: () -> Unit,
) {
    val top = if (pink) Palette.promoPinkTop else Palette.promoTop
    val bottom = if (pink) Color(0xFFE23E97) else Palette.promoBottom
    val click = LocalClickSound.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .clickable { click(); onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CoinIcon(Modifier.size(28.dp))
                Box(Modifier.size(28.dp)) { DrillIcon() }
                Box(Modifier.size(28.dp)) { HammerIcon() }
                Box(Modifier.size(28.dp)) { FlaskIcon() }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(discount, color = Palette.red, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.green)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) { Text(price, color = Color.White, fontWeight = FontWeight.Black) }
        }
    }
}

/** Плавное осветление цвета на [amount] (0..1). */
fun Color.lighten(amount: Float): Color {
    fun ch(v: Float) = (v + (1f - v) * amount).coerceIn(0f, 1f)
    return Color(ch(red), ch(green), ch(blue), alpha)
}

/**
 * «Дождь» из золотых монет со свечением: проигрывается заново при каждом
 * изменении [trigger] (0 — ничего не рисуем). Летит «верёвочкой» сверху вниз
 * с ускорением, в конце монеты растворяются.
 */
@Composable
fun CoinRain(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger <= 0) return
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val coinPx = with(LocalDensity.current) { 40.dp.toPx() }
        key(trigger) {
            val prog = remember { Animatable(0f) }
            LaunchedEffect(Unit) { prog.animateTo(1f, tween(1500, easing = LinearEasing)) }
            listOf(0.18f, 0.36f, 0.5f, 0.64f, 0.82f).forEachIndexed { i, xf ->
                val shift = 90 * i
                val t = ((prog.value * 1500 - shift) / (1500 - shift)).coerceIn(0f, 1f)
                val fall = t * t
                val fade = if (t > 0.9f) ((1f - t) / 0.1f).coerceIn(0f, 1f) else 1f
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (wPx * xf - coinPx / 2).toInt(),
                                (fall * (hPx + coinPx * 2) - coinPx).toInt(),
                            )
                        }
                        .size(40.dp)
                        .rotate(fall * 320f)
                        .alpha(fade),
                    contentAlignment = Alignment.Center,
                ) {
                    // мягкое золотое свечение вокруг монеты
                    Box(
                        Modifier
                            .fillMaxSize()
                            .scale(2f)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color(0xFFFFE082), Color(0x00FFE082)))),
                    )
                    CoinIcon(Modifier.fillMaxSize())
                }
            }
        }
    }
}
