package com.fidapp.screw.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(edge)
            .clickable(enabled = enabled) { onClick() },
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
                Box(
                    Modifier
                        .size(30.dp)
                        .padding(end = 4.dp)
                        .clip(CircleShape)
                        .background(Palette.green)
                        .clickable { onPlus() },
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

/** Голубая рамка диалога с «шурупами» по углам шапки и красным крестиком. */
@Composable
fun GameDialog(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .width(320.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Palette.panelBlueDark)
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
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.verticalGradient(listOf(Palette.panelBlue, Palette.panelBlueDark))),
                contentAlignment = Alignment.Center,
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    ScrewDot()
                    ScrewDot()
                }
            }
            CloseIcon(
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(30.dp)
                    .clickable { onClose() },
            )
        }
        // тело
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Palette.dialogGradient)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

@Composable
private fun ScrewDot() {
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
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Palette.green,
                uncheckedTrackColor = Color(0xFFCBD2DA),
            ),
        )
    }
}

/** Фейковый рекламный баннер внизу (заглушка вместо реального SDK). */
@Composable
fun FakeAdBanner(modifier: Modifier = Modifier) {
    val ads = remember {
        listOf(
            "Magic Sort!" to "Погрузитесь в волшебное сортирование",
            "Water Master" to "Управляй потоком воды",
            "Space Shooter" to "Galaxy Attack — спаси галактику",
            "Yarn Sort 3D" to "Jam Puzzle для релакса",
        )
    }
    val index = remember { (0 until ads.size).random() }
    val (name, desc) = ads[index]
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFFF2F2F2))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Palette.green))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF333333))
            Text(desc, fontSize = 12.sp, color = Color(0xFF888888), maxLines = 1)
        }
        Box(Modifier.size(34.dp).clip(CircleShape).background(Palette.green), contentAlignment = Alignment.Center) {
            Text("↓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        Text(" Ad", fontSize = 10.sp, color = Color(0xFFAAAAAA))
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .clickable { onClick() }
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
