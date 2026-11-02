package com.fidapp.screw.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fidapp.screw.R

/**
 * Векторные иконки, нарисованные на Canvas в мультяшном стиле скриншотов.
 * Иконки бонусов (сверло/плашка/молоток/ящик) — реальные картинки пользователя
 * из `res/drawable-nodpi` (белый фон, как «стикер» на плашке).
 */

/** Картинка-иконка бонуса поверх плашки: белый фон скруглён — выглядит стикером. */
@Composable
private fun BonusPicture(res: Int, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

/**
 * Золотая монета — картинка пользователя `coin2_1024.jpg` (белый фон). Обрезается
 * СТРОГО по кругу: `clip(CircleShape)` + зум 1.12. Раньше стоял зум 1.10 и ободок
 * `border` шёл по краю КОНТЕЙНЕРА — из-за этого рамка-круг была крупнее самой
 * монеты (на фото она занимает ~94% кадра) и игроку казалось, что монета «не
 * внутри круга, смещена влево-вниз». Теперь ободок уменьшен на 6% и сходится с
 * реальным краем монеты, а зум 1.12 срезает белую кромку с запасом.
 * Тонкий тёмно-золотой ободок отделяет монету от белых карточек.
 *
 * Фото монеты тёмное (средняя яркость ~130/255): в размере 20–24 dp (цены бонусов)
 * оно читалось как тёмно-коричневое пятно, поэтому к картинке применён ColorFilter —
 * та же матрица, что проверена в `tools/coin_preview.ps1`: +18 % насыщенности и
 * лёгкий подъём яркости. На крупных размерах фильтр почти незаметен.
 */
@Composable
fun CoinIcon(modifier: Modifier = Modifier, size: Dp = Dp.Unspecified) {
    Box(
        modifier = modifier.then(if (size == Dp.Unspecified) Modifier else Modifier.size(size)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.coin2_1024),
            contentDescription = null,
            colorFilter = COIN_FILTER,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .graphicsLayer { scaleX = 1.12f; scaleY = 1.12f },
        )
        // ободок по краю самой монеты, а не контейнера: Box урезан на 6%
        Box(Modifier.padding(3.dp).fillMaxSize().border(1.dp, Color(0x80C98600), CircleShape))
    }
}

/** Матрица «подсветки» монеты: насыщенность +18 %, яркость +13 %/10 %/3 % по каналам. */
private val COIN_FILTER = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            1.18f, -0.06f, -0.06f, 0f, 34f,
            -0.06f, 1.18f, -0.06f, 0f, 26f,
            -0.06f, -0.06f, 1.18f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
)

@Composable
fun HeartIcon(modifier: Modifier = Modifier, color: Color = Palette.heart) {
    Canvas(modifier = modifier) {
        drawHeart(color)
    }
}

private fun DrawScope.drawHeart(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.92f)
        cubicTo(w * 0.02f, h * 0.6f, w * 0.08f, h * 0.12f, w * 0.5f, h * 0.36f)
        cubicTo(w * 0.92f, h * 0.12f, w * 0.98f, h * 0.6f, w * 0.5f, h * 0.92f)
        close()
    }
    drawPath(path, color)
    drawPath(path, Color(0x55FFFFFF), style = Stroke(width = w * 0.05f))
}

@Composable
fun DrillIcon(modifier: Modifier = Modifier) {
    // шуруповёрт — картинка пользователя (для кнопки «СВЕРЛИТЬ»)
    BonusPicture(R.drawable.pic_shurupovert, modifier)
}

@Composable
fun FlaskIcon(modifier: Modifier = Modifier) {
    // горелка — картинка пользователя (для кнопки «ПЛАВИТЬ»)
    BonusPicture(R.drawable.pic_gorelka, modifier)
}

@Composable
fun HammerIcon(modifier: Modifier = Modifier) {
    // молоток — картинка пользователя (для кнопки «ЗАДЕЛАТЬ»)
    BonusPicture(R.drawable.pic_molotok, modifier)
}

@Composable
fun BoxIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFF4FA3F7), dots: Int = 0) {
    // ящик — картинка пользователя (диалог «Открыть ящик»); dots>0 (ревайв)
    // остаётся нарисованным — нужен цветной ящик с красными точками
    if (dots == 0) {
        BonusPicture(R.drawable.pic_yashik, modifier)
    } else {
        Canvas(modifier = modifier) {
            val w = size.width; val h = size.height
            // ручка
            drawRoundRect(tint, Offset(w * 0.38f, h * 0.06f), Size(w * 0.24f, h * 0.12f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
            // корпус
            drawRoundRect(
                Color.White, Offset(w * 0.1f, h * 0.18f), Size(w * 0.8f, h * 0.68f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f),
            )
            drawRoundRect(
                tint, Offset(w * 0.1f, h * 0.18f), Size(w * 0.8f, h * 0.68f),
                style = Stroke(width = w * 0.08f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f),
            )
            val dotColor = Color(0xFFE53935)
            for (i in 0 until 3) {
                drawCircle(if (i < dots) dotColor else Color(0xFFB0BEC5), w * 0.09f, Offset(w * (0.3f + i * 0.2f), h * 0.52f))
            }
        }
    }
}

@Composable
fun WalletIcon(modifier: Modifier = Modifier) {
    // кошелёк — картинка пользователя для диалога «Кошелёк» (белый фон вырезан)
    BonusPicture(R.drawable.pic_koshelek, modifier)
}

@Composable
fun PigIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawCircle(Color(0xFFF7A8B8), w * 0.36f, Offset(w * 0.5f, h * 0.55f))
        // уши
        drawCircle(Color(0xFFF7A8B8), w * 0.1f, Offset(w * 0.3f, h * 0.28f))
        drawCircle(Color(0xFFF7A8B8), w * 0.1f, Offset(w * 0.7f, h * 0.28f))
        // пятачок
        drawRoundRect(Color(0xFFEC7C95), Offset(w * 0.4f, h * 0.5f), Size(w * 0.2f, h * 0.14f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f))
        drawCircle(Color(0xFFC9566E), w * 0.02f, Offset(w * 0.46f, h * 0.57f))
        drawCircle(Color(0xFFC9566E), w * 0.02f, Offset(w * 0.54f, h * 0.57f))
        // глаза
        drawCircle(Color(0xFF5A2A34), w * 0.03f, Offset(w * 0.4f, h * 0.45f))
        drawCircle(Color(0xFF5A2A34), w * 0.03f, Offset(w * 0.6f, h * 0.45f))
    }
}

@Composable
fun VideoIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val tri = Path().apply {
            moveTo(w * 0.32f, h * 0.28f)
            lineTo(w * 0.72f, h * 0.5f)
            lineTo(w * 0.32f, h * 0.72f)
            close()
        }
        drawPath(tri, color)
    }
}

/** Шляпка винта — как на доске (тот же drawScrewAt), для кнопки «Играть». */
@Composable
fun ScrewIcon(modifier: Modifier = Modifier, color: Color = Color(0xFFFFC534)) {
    Canvas(modifier = modifier) {
        drawScrewAt(Offset(center.x, center.y), color, size.minDimension / 2f * 0.9f)
    }
}

@Composable
fun PauseIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF2E7BD6)) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(color, Offset(w * 0.28f, h * 0.24f), Size(w * 0.16f, h * 0.52f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
        drawRoundRect(color, Offset(w * 0.56f, h * 0.24f), Size(w * 0.16f, h * 0.52f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
    }
}

@Composable
fun CloseIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val c = Offset(center.x, center.y)
        val r = size.minDimension / 2f
        drawCircle(Color(0xFFB71C1C), r, c)
        drawCircle(Color(0xFFE53935), r * 0.86f, c)
        val w = r * 0.16f
        drawLine(Color.White, Offset(c.x - r * 0.4f, c.y - r * 0.4f), Offset(c.x + r * 0.4f, c.y + r * 0.4f), w, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(c.x + r * 0.4f, c.y - r * 0.4f), Offset(c.x - r * 0.4f, c.y + r * 0.4f), w, cap = StrokeCap.Round)
    }
}

@Composable
fun HomeIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val roof = Path().apply {
            moveTo(w * 0.5f, h * 0.14f)
            lineTo(w * 0.88f, h * 0.5f)
            lineTo(w * 0.12f, h * 0.5f)
            close()
        }
        drawPath(roof, color)
        drawRect(color, Offset(w * 0.24f, h * 0.5f), Size(w * 0.52f, h * 0.36f))
    }
}

@Composable
fun TrophyIcon(modifier: Modifier = Modifier, color: Color = Color(0xFFF6B500)) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(color, Offset(w * 0.28f, h * 0.16f), Size(w * 0.44f, h * 0.34f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f))
        drawRect(color, Offset(w * 0.44f, h * 0.5f), Size(w * 0.12f, h * 0.2f))
        drawRect(color, Offset(w * 0.3f, h * 0.7f), Size(w * 0.4f, h * 0.12f))
        drawCircle(color, w * 0.12f, Offset(w * 0.22f, h * 0.26f), style = Stroke(width = w * 0.05f))
        drawCircle(color, w * 0.12f, Offset(w * 0.78f, h * 0.26f), style = Stroke(width = w * 0.05f))
    }
}

@Composable
fun ShopIcon(modifier: Modifier = Modifier, color: Color = Color(0xFFE53935)) {
    // color красит «навес» магазина: в нижней панели вкладки теперь ВСЕ жёлтые
    // (пользователь: «закрась шестерёнку и вообще все иконки нижней панели
    // жёлтым — как у трофеев»). Корпус остаётся белым, «витрины» — синими,
    // чтобы иконка не превращалась в жёлтое пятно.
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(color, Offset(w * 0.16f, h * 0.3f), Size(w * 0.68f, h * 0.12f))
        drawRect(Color.White, Offset(w * 0.16f, h * 0.42f), Size(w * 0.68f, h * 0.36f))
        drawRect(Color(0xFF1E88E5), Offset(w * 0.28f, h * 0.5f), Size(w * 0.16f, h * 0.28f))
        drawRect(Color(0xFF1E88E5), Offset(w * 0.56f, h * 0.5f), Size(w * 0.16f, h * 0.28f))
    }
}

@Composable
fun StarIcon(modifier: Modifier = Modifier, color: Color = Color(0xFFFFD54F)) {
    Canvas(modifier = modifier) {
        val c = Offset(center.x, center.y)
        val r = size.minDimension / 2f
        val path = Path()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else r * 0.45f
            val a = Math.toRadians(-90.0 + i * 36.0)
            val x = c.x + (rad * Math.cos(a)).toFloat()
            val y = c.y + (rad * Math.sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
    }
}

/** Бумажный самолётик — канал в Telegram (рисунок как в проекте Tetris). */
@Composable
fun TelegramIcon(modifier: Modifier = Modifier, color: Color = Palette.blue) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val plane = Path().apply {
            moveTo(w * 0.06f, h * 0.48f)
            lineTo(w * 0.94f, h * 0.10f)
            lineTo(w * 0.62f, h * 0.90f)
            lineTo(w * 0.46f, h * 0.60f)
            close()
        }
        drawPath(plane, color)
        // сгиб крыла — «прорезан» цветом белой карточки
        drawLine(Color(0xFFF2F5FB), Offset(w * 0.46f, h * 0.60f), Offset(w * 0.94f, h * 0.10f), w * 0.06f)
    }
}

/** Каталог приложений — сетка 2x2 «плиток» (как в проекте Tetris). */
@Composable
fun CatalogIcon(modifier: Modifier = Modifier, color: Color = Palette.blue) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val s = w * 0.36f
        val gap = w * 0.08f
        val r = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f)
        drawRoundRect(color, Offset(w * 0.10f, h * 0.10f), Size(s, s), r)
        // правая верхняя — полупрозрачная: «и ещё много чего»
        drawRoundRect(color.copy(alpha = 0.55f), Offset(w * 0.10f + s + gap, h * 0.10f), Size(s, s), r)
        drawRoundRect(color, Offset(w * 0.10f, h * 0.10f + s + gap), Size(s, s), r)
        drawRoundRect(color, Offset(w * 0.10f + s + gap, h * 0.10f + s + gap), Size(s, s), r)
    }
}
