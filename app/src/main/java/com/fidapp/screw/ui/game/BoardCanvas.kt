package com.fidapp.screw.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import com.fidapp.screw.game.Board
import com.fidapp.screw.game.GameEngine
import com.fidapp.screw.game.Piece
import com.fidapp.screw.game.Screw
import com.fidapp.screw.game.bounds
import kotlin.math.cos
import kotlin.math.sin

/** Летящий шуруп: координаты from/to — в пикселях экрана (overlay рисует). */
data class FlyingScrew(
    val screwId: Int,
    val color: String,
    val from: Offset,
    val to: Offset,
    val progress: Float,
)

/** Деталь, у которой выкрутили все винты — падает за нижний край доски. */
data class FallingPiece(val piece: Piece, val progress: Float)

/**
 * Доска с деталями и шурупами. Логические координаты [Board.W]x[Board.H]
 * масштабируются в Canvas с сохранением пропорций и центрированием.
 */
@Composable
fun BoardCanvas(
    pieces: List<Piece>,
    screws: List<Screw>,
    isCollected: (Int) -> Boolean,
    isAccessible: (Screw) -> Boolean,
    onScrewTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    falling: List<FallingPiece> = emptyList(),
) {
    // масштаб/сдвиг пересчитываем в рисовании; для тапов держим те же значения
    var scale = 1f
    var dx = 0f
    var dy = 0f
    // фаза «покачивания» деталей, оставшихся на части винтов
    val transition = rememberInfiniteTransition(label = "sway")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "phase",
    )

    Canvas(
        modifier = modifier
            .pointerInput(pieces, screws) {
                detectTapGestures { press ->
                    val lx = (press.x - dx) / scale
                    val ly = (press.y - dy) / scale
                    // ищем верхний доступный шуруп под точкой
                    val hit = screws
                        .filter { !isCollected(it.id) && isAccessible(it) }
                        .sortedByDescending { s -> pieces.first { p -> p.id == s.pieceId }.layer }
                        .firstOrNull { kotlin.math.hypot((lx - s_x(it)).toDouble(), (ly - s_y(it)).toDouble()) < 70 }
                    if (hit != null) onScrewTap(hit.id)
                }
            },
    ) {
        scale = minOf(size.width / Board.W, size.height / Board.H)
        dx = (size.width - Board.W * scale) / 2f
        dy = (size.height - Board.H * scale) / 2f

        val ordered = pieces.sortedBy { it.layer }
        for (piece in ordered) {
            val own = piece.screwsOf(screws)
            val removedCount = own.count { isCollected(it.id) }
            // выкручены НЕ все винты — деталь болтается на оставшихся (наклон + покачивание)
            var angle = 0f
            if (own.isNotEmpty() && removedCount > 0) {
                val sev = removedCount.toFloat() / own.size
                angle = sev * 5f + sin(phase * 2.0 * Math.PI).toFloat() * 3.5f * sev
            }
            val body: DrawScope.() -> Unit = {
                drawPiece(piece, scale, dx, dy, 1f)
                own.forEach { screw ->
                    if (isCollected(screw.id)) return@forEach
                    drawScrew(screw, scale, dx, dy, dim = !isAccessible(screw))
                }
            }
            if (angle == 0f) {
                body()
            } else {
                val b = piece.bounds()
                val pivot = Offset(
                    dx + (b[0] + b[2]) / 2f * scale,
                    dy + (b[1] + b[3]) / 2f * scale,
                )
                rotate(angle, pivot, body)
            }
        }
        // выкручены ВСЕ винты — деталь падает вниз с поворотом и растворением
        falling.forEach { f ->
            val p = f.progress
            val b = f.piece.bounds()
            val pivot = Offset(
                dx + (b[0] + b[2]) / 2f * scale,
                dy + (b[1] + b[3]) / 2f * scale,
            )
            translate(top = p * p * 950f * scale) {
                rotate(p * 26f * (if (f.piece.id % 2 == 0) 1f else -1f), pivot) {
                    drawPiece(f.piece, scale, dx, dy, (1f - p).coerceIn(0f, 1f))
                }
            }
        }
    }
}

private fun s_x(s: Screw) = s.x
private fun s_y(s: Screw) = s.y
private fun Piece.screwsOf(all: List<Screw>) = all.filter { it.pieceId == id }

private fun Offset.scale(f: Float) = Offset(x * f, y * f)

private fun DrawScope.drawPiece(piece: Piece, scale: Float, dx: Float, dy: Float, alpha: Float) {
    // серая «заблокированная» деталь = стекло: полупрозрачная пластина с бликом;
    // она РЕАЛЬНО перекрывает винты под собой, снимается только бонусом «Плавить»
    val glass = piece.locked
    val base = if (glass) Color(0xFFAFC3D6) else Color(android.graphics.Color.parseColor(piece.colorHex))
    val fillAlpha = if (glass) 0.62f else 1f
    val path = Path()
    when (piece.kind) {
        "rect" -> {
            val left = dx + piece.x * scale
            val top = dy + piece.y * scale
            val right = left + piece.w * scale
            val bottom = top + piece.h * scale
            val r = (piece.corners.coerceAtMost(minOf(piece.w, piece.h) / 2f)) * scale
            drawRoundRect(
                color = base.copy(alpha = fillAlpha * alpha),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                cornerRadius = CornerRadius(r, r),
            )
            // белая «обводка»-каркас
            drawRoundRect(
                color = Color.White.copy(alpha = alpha),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = 10f * scale),
            )
            if (glass) drawGlassShine(left + r, top + r, right - r, bottom - r, scale, alpha)
        }
        else -> {
            piece.points.forEachIndexed { i, (px, py) ->
                val x = dx + px * scale
                val y = dy + py * scale
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, base.copy(alpha = fillAlpha * alpha))
            drawPath(path, Color.White.copy(alpha = alpha), style = Stroke(width = 12f * scale))
            if (glass) {
                val b = piece.bounds()
                drawGlassShine(dx + b[0] * scale, dy + b[1] * scale, dx + b[2] * scale, dy + b[3] * scale, scale, alpha)
            }
        }
    }
}

/** Диагональные блики поверх «стекла», чтобы пластина читалась как стеклянная. */
private fun DrawScope.drawGlassShine(l: Float, t: Float, rr: Float, b: Float, scale: Float, alpha: Float) {
    val w = (rr - l).coerceAtLeast(1f)
    val h = (b - t).coerceAtLeast(1f)
    val stripe = Color.White.copy(alpha = 0.30f * alpha)
    val sw = 8f * scale
    listOf(0.30f, 0.52f).forEach { f ->
        drawLine(stripe, Offset(l + w * f, t), Offset(l + w * f - h * 0.35f, b), sw)
    }
}

private fun DrawScope.drawScrew(screw: com.fidapp.screw.game.Screw, scale: Float, dx: Float, dy: Float, dim: Boolean) {
    val c = Offset(dx + screw.x * scale, dy + screw.y * scale)
    drawScrewAt(c, Color(android.graphics.Color.parseColor(screw.color.hex)).copy(alpha = if (dim) 0.5f else 1f), 62f * scale, dim)
}

internal fun DrawScope.drawScrewAt(center: Offset, color: Color, radius: Float, dim: Boolean = false) {
    // тень
    drawCircle(Color.Black.copy(alpha = if (dim) 0.1f else 0.22f), radius, Offset(center.x, center.y + radius * 0.14f))
    // тело
    drawCircle(color, radius, center)
    // тёмный ободок
    drawCircle(color.darkenSelf(), radius, center, style = Stroke(width = radius * 0.16f))
    // блик
    drawCircle(Color.White.copy(alpha = 0.35f), radius * 0.5f, Offset(center.x - radius * 0.25f, center.y - radius * 0.28f))
    // крест-шлиц
    val w = radius * 0.16f
    val arm = radius * 0.5f
    drawLine(Color.Black.copy(alpha = 0.55f), Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), w, cap = StrokeCap.Round)
    drawLine(Color.Black.copy(alpha = 0.55f), Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), w, cap = StrokeCap.Round)
}

private fun Color.darkenSelf(): Color {
    fun ch(v: Float) = (v * 0.7f).coerceIn(0f, 1f)
    return Color(ch(red), ch(green), ch(blue), alpha)
}

/** Ящик-заказ: рамка цвета винтов, ручка сверху, гнёзда (заполненные — шляпками). */
@Composable
fun ToolboxWidget(
    color: com.fidapp.screw.game.ScrewColor,
    filled: Int,
    capacity: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val c = Color(android.graphics.Color.parseColor(color.hex))
        val dark = c.darkenSelf()
        val handleH = size.height * 0.16f
        val handleW = size.width * 0.56f
        // ручка
        drawRoundRect(
            color = dark,
            topLeft = Offset((size.width - handleW) / 2f, 0f),
            size = androidx.compose.ui.geometry.Size(handleW, handleH * 1.6f),
            cornerRadius = CornerRadius(handleW * 0.25f, handleW * 0.25f),
        )
        // корпус
        val bodyTop = handleH
        drawRoundRect(
            color = c,
            topLeft = Offset(0f, bodyTop),
            size = androidx.compose.ui.geometry.Size(size.width, size.height - bodyTop),
            cornerRadius = CornerRadius(size.width * 0.14f, size.width * 0.14f),
        )
        drawRoundRect(
            color = dark,
            topLeft = Offset(0f, bodyTop),
            size = androidx.compose.ui.geometry.Size(size.width, size.height - bodyTop),
            cornerRadius = CornerRadius(size.width * 0.14f, size.width * 0.14f),
            style = Stroke(width = size.width * 0.04f),
        )
        // белое нутро
        val m = size.width * 0.1f
        drawRoundRect(
            color = Color(0xFFF6F8FB),
            topLeft = Offset(m, bodyTop + m * 0.7f),
            size = androidx.compose.ui.geometry.Size(size.width - 2 * m, size.height - bodyTop - m * 0.7f - m * 0.5f),
            cornerRadius = CornerRadius(m * 0.9f, m * 0.9f),
        )
        // гнёзда: пустые — серые углубления, заполненные — шляпки винтов;
        // размер шляпок подобран так, чтобы винт был ОТЧЁТЛИВО виден в ящике
        val n = capacity.coerceAtLeast(1)
        val innerLeft = m * 1.6f
        val innerW = size.width - 3.2f * m
        val gap = innerW / n
        val r = (gap * 0.42f).coerceAtMost((size.height - bodyTop) * 0.3f)
        val cy = bodyTop + (size.height - bodyTop) * 0.5f
        for (k in 0 until n) {
            val cx = innerLeft + gap * (k + 0.5f)
            if (k < filled) {
                // лунка под винтом, чтобы шляпка читалась как вложенный в ящик
                drawCircle(Color(0xFF8E97A6).copy(alpha = 0.35f), r * 1.12f, Offset(cx, cy))
                drawScrewAt(Offset(cx, cy), c, r)
            } else {
                drawCircle(Color(0xFFB9BFC9), r, Offset(cx, cy))
                drawCircle(Color(0xFFEDEFF3), r * 0.72f, Offset(cx, cy))
            }
        }
    }
}

/** Склад: ряд кружков под ящиками; заполненный кружок — шляпка винта. */
@Composable
fun HoldRow(slots: List<Color?>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val n = slots.size
        if (n == 0) return@Canvas
        val gap = size.width / n
        val r = (gap * 0.42f).coerceAtMost(size.height / 2f)
        slots.forEachIndexed { i, color ->
            val cx = gap * (i + 0.5f)
            val cy = size.height / 2f
            drawCircle(Color(0xFFB8C0CC), r, Offset(cx, cy))
            drawCircle(Color(0xFFDCE2EA), r * 0.82f, Offset(cx, cy))
            if (color != null) drawScrewAt(Offset(cx, cy), color, r * 0.78f)
        }
    }
}

/**
 * Экранный центр гнезда ящика-заказа — тот же расчёт, что рисует
 * [ToolboxWidget], чтобы летящий винт приземлялся ровно в своё гнездо.
 */
fun toolboxSocketCenter(cell: Rect, socket: Int, capacity: Int = GameEngine.BOX_CAPACITY): Offset {
    val bodyTop = cell.height * 0.16f
    val m = cell.width * 0.1f
    val n = capacity.coerceAtLeast(1)
    val gap = (cell.width - 3.2f * m) / n
    val k = socket.coerceIn(0, n - 1)
    val cx = m * 1.6f + gap * (k + 0.5f)
    val cy = bodyTop + (cell.height - bodyTop) * 0.5f
    return Offset(cell.left + cx, cell.top + cy)
}

@Suppress("unused")
private fun polar(cx: Float, cy: Float, r: Float, deg: Double) =
    Offset(cx + (r * cos(Math.toRadians(deg))).toFloat(), cy + (r * sin(Math.toRadians(deg))).toFloat())

@Suppress("unused")
private fun rectOf(left: Float, top: Float, w: Float, h: Float) = Rect(left, top, left + w, top + h)
