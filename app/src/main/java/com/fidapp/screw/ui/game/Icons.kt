package com.fidapp.screw.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * Векторные иконки, нарисованные на Canvas в мультяшном стиле скриншотов.
 * Никаких внешних PNG — всё кодом, APK лёгкий, стиль единый.
 */

@Composable
fun CoinIcon(modifier: Modifier = Modifier, size: Dp = Dp.Unspecified) {
    Canvas(modifier = modifier.then(if (size == Dp.Unspecified) Modifier else Modifier.size(size))) {
        val c = Offset(center.x, center.y)
        val r = this.size.minDimension / 2f
        drawCircle(Color(0xFFC98600), r * 0.98f, c)
        drawCircle(
            Brush.verticalGradient(listOf(Color(0xFFFFE08A), Color(0xFFF6B500))),
            r * 0.82f, c,
        )
        drawCircle(Color(0xFFE39400), r * 0.55f, c, style = Stroke(width = r * 0.12f))
        // «плюс» как на монете из скриншотов
        val w = r * 0.16f
        val arm = r * 0.34f
        drawRoundRect(Color(0xFFE39400), Offset(c.x - w / 2, c.y - arm), Size(w, arm * 2), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2))
        drawRoundRect(Color(0xFFE39400), Offset(c.x - arm, c.y - w / 2), Size(arm * 2, w), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2))
    }
}

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
    // TODO(art): заменить на Image(painterResource(R.drawable.ic_drill)),
    //  когда картинки бонусов лягут в res/drawable — все вызовы идут через эту функцию
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        // корпус
        drawRoundRect(
            Color(0xFF4FC3F7), Offset(w * 0.42f, h * 0.28f), Size(w * 0.42f, h * 0.34f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
        )
        // рукоятка
        drawRoundRect(
            Color(0xFF0288D1), Offset(w * 0.5f, h * 0.55f), Size(w * 0.2f, h * 0.32f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
        )
        // сверло
        val bit = Path().apply {
            moveTo(w * 0.42f, h * 0.38f)
            lineTo(w * 0.1f, h * 0.48f)
            lineTo(w * 0.42f, h * 0.56f)
            close()
        }
        drawPath(bit, Color(0xFFB0BEC5))
        drawLine(Color(0xFF78909C), Offset(w * 0.12f, h * 0.48f), Offset(w * 0.4f, h * 0.4f), w * 0.04f, cap = StrokeCap.Round)
    }
}

@Composable
fun FlaskIcon(modifier: Modifier = Modifier) {
    // TODO(art): заменить на Image(painterResource(R.drawable.ic_melt)) — см. DrillIcon
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val body = Path().apply {
            moveTo(w * 0.4f, h * 0.15f)
            lineTo(w * 0.4f, h * 0.42f)
            lineTo(w * 0.16f, h * 0.82f)
            quadraticTo(w * 0.12f, h * 0.9f, w * 0.22f, h * 0.9f)
            lineTo(w * 0.78f, h * 0.9f)
            quadraticTo(w * 0.88f, h * 0.9f, w * 0.84f, h * 0.82f)
            lineTo(w * 0.6f, h * 0.42f)
            lineTo(w * 0.6f, h * 0.15f)
            close()
        }
        drawPath(body, Color(0xFFF6C97A))
        drawPath(body, Color(0xFFB07A2E), style = Stroke(width = w * 0.04f))
        drawRect(Color(0xFFE0A94E), Offset(w * 0.36f, h * 0.08f), Size(w * 0.28f, h * 0.09f))
    }
}

@Composable
fun HammerIcon(modifier: Modifier = Modifier) {
    // TODO(art): заменить на Image(painterResource(R.drawable.ic_seal)) — см. DrillIcon
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        // рукоятка
        drawLine(Color(0xFF8D6E63), Offset(w * 0.35f, h * 0.9f), Offset(w * 0.6f, h * 0.35f), w * 0.12f, cap = StrokeCap.Round)
        // головка
        drawRoundRect(
            Color(0xFFB0BEC5), Offset(w * 0.45f, h * 0.12f), Size(w * 0.42f, h * 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f),
        )
        drawRoundRect(
            Color(0xFF78909C), Offset(w * 0.18f, h * 0.16f), Size(w * 0.3f, h * 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
        )
    }
}

@Composable
fun BoxIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFF4FA3F7), dots: Int = 0) {
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
        val dotColor = if (dots > 0) Color(0xFFE53935) else Color(0xFFB0BEC5)
        for (i in 0 until 3) {
            drawCircle(if (i < dots) dotColor else Color(0xFFB0BEC5), w * 0.09f, Offset(w * (0.3f + i * 0.2f), h * 0.52f))
        }
    }
}

@Composable
fun GiftIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(Color(0xFF8E24AA), Offset(w * 0.15f, h * 0.4f), Size(w * 0.7f, h * 0.5f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f))
        drawRect(Color(0xFFFFC534), Offset(w * 0.45f, h * 0.4f), Size(w * 0.1f, h * 0.5f))
        drawRoundRect(Color(0xFFAB47BC), Offset(w * 0.1f, h * 0.28f), Size(w * 0.8f, h * 0.16f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
        drawCircle(Color(0xFFFFC534), w * 0.1f, Offset(w * 0.35f, h * 0.2f))
        drawCircle(Color(0xFFFFC534), w * 0.1f, Offset(w * 0.65f, h * 0.2f))
    }
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

@Composable
fun GearIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF2E7BD6)) {
    Canvas(modifier = modifier) {
        val c = Offset(center.x, center.y)
        val r = size.minDimension / 2f
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            drawLine(color, Offset(c.x + (r * 0.6f * Math.cos(a)).toFloat(), c.y + (r * 0.6f * Math.sin(a)).toFloat()),
                Offset(c.x + (r * 0.95f * Math.cos(a)).toFloat(), c.y + (r * 0.95f * Math.sin(a)).toFloat()), r * 0.34f, cap = StrokeCap.Round)
        }
        drawCircle(color, r * 0.6f, c)
        drawCircle(Color.White, r * 0.26f, c)
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
fun ShopIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(Color(0xFFE53935), Offset(w * 0.16f, h * 0.3f), Size(w * 0.68f, h * 0.12f))
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
