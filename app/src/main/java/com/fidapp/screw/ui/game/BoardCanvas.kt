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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Летящий шуруп: координаты from/to — в пикселях экрана (overlay рисует). */
data class FlyingScrew(
    val screwId: Int,
    val pieceId: Int,
    val color: String,
    val from: Offset,
    val to: Offset,
    /** 0..1 — фаза выкручивания на месте, 1..2 — перелёт (progress-1 — доля). */
    val progress: Float,
    /** Момент старта текущей фазы (выкручивания, затем перелёта), мс. */
    val startAt: Long = 0L,
    /** Поворот шлица, градусы (против часовой). */
    val rotation: Float = 0f,
)

/** Винт в фазе выкручивания: рисуется на доске (координаты доски) с поворотом шлица. */
data class UnscrewingScrew(
    val screwId: Int,
    val pieceId: Int,
    val x: Float,
    val y: Float,
    val color: String,
    val rotation: Float,
)

/** След от выкрученного винта: белый контурный кружок (координаты доски). */
data class ScrewHole(val pieceId: Int, val x: Float, val y: Float)

/**
 * Деталь, у которой выкрутили все винты — падает за нижний край доски.
 * [beginAt] — момент, с которого падение начинается (мс): последний винт сначала
 * успевает выкрутиться и улететь, и только потом деталь трогается с места.
 */
data class FallingPiece(val piece: Piece, val progress: Float, val beginAt: Long = 0L)

/** Наклон одной детали: угол пружины, скорость и точка опоры (координаты доски). */
class PieceTilt {
    var angle = 0f
    var vel = 0f
    var target = 0f
    var pivotX = 0f
    var pivotY = 0f
    var sag = 0f
}

/**
 * Физика «висящей на винтах» детали.
 *
 * Деталь держится на ОСТАВШИХСЯ винтах — они и есть её точка опоры. Как только
 * часть винтов выкручена, корпус повёрнут вокруг новой опоры так, чтобы центр
 * масс встал ровно ПОД неё (обычный маятник): прямоугольник на двух винтах по
 * краям, оставшись на одном, разворачивается вертикально. Движение — пружина с
 * демпфированием, поэтому деталь делает переход через равновесие, качается и
 * останавливается. Жёсткость ограничена: на одном винте до 100°, на двух — 12°,
 * на трёх и больше деталь практически не повёрнута.
 */
class PieceTilts {
    val items = HashMap<Int, PieceTilt>()

    /**
     * Интегрирует пружины всех живых деталей за шаг [dt] (секунды).
     * [sway] — медленное покачивание на весу, градусы. Возвращает true, если ещё есть движение.
     */
    fun step(pieces: List<Piece>, screws: List<Screw>, isCollected: (Int) -> Boolean, dt: Float, sway: Float): Boolean {
        var moving = false
        for (piece in pieces) {
            val t = items.getOrPut(piece.id) { PieceTilt() }
            val geom = tiltGeometry(piece, screws.filter { it.pieceId == piece.id }, isCollected)
            // винтов не осталось — деталь «умирает», угол не трогаем (её рисует слой падения)
            if (geom != null) {
                t.pivotX = geom.pivotX
                t.pivotY = geom.pivotY
                t.sag = geom.sag
                t.target = geom.target + sway * geom.swayWeight
            }
            val acc = (t.target - t.angle) * STIFFNESS - t.vel * DAMPING
            t.vel += acc * dt
            t.angle += t.vel * dt
            if (abs(t.target - t.angle) > 0.2f || abs(t.vel) > 0.5f) moving = true
        }
        return moving
    }

    /** Куда переехала точка доски после наклона её детали (для полёта винта). */
    fun apply(pieceId: Int, p: Offset): Offset {
        val t = items[pieceId] ?: return p
        if (t.angle == 0f && t.sag == 0f) return p
        // порядок как в рисовании: сначала поворот вокруг опоры, затем проседание вниз
        val q = rotateAround(t.pivotX, t.pivotY, t.angle, p.x, p.y)
        return Offset(q.x, q.y + t.sag)
    }

    /** Обратное преобразование: точка доски после наклона -> место винта в детали. */
    fun invert(pieceId: Int, x: Float, y: Float): Offset {
        val t = items[pieceId] ?: return Offset(x, y)
        if (t.angle == 0f && t.sag == 0f) return Offset(x, y)
        return rotateAround(t.pivotX, t.pivotY, -t.angle, x, y - t.sag)
    }

    /** Поворот точки (x; y) вокруг (aroundX; aroundY) на [deg] градусов. */
    private fun rotateAround(aroundX: Float, aroundY: Float, deg: Float, x: Float, y: Float): Offset {
        val a = Math.toRadians(deg.toDouble())
        val ca = cos(a).toFloat()
        val sa = sin(a).toFloat()
        val px = x - aroundX
        val py = y - aroundY
        return Offset(aroundX + px * ca - py * sa, aroundY + px * sa + py * ca)
    }

    private companion object {
        /**
         * Жёсткость и демпфирование пружины маятника. Подобраны «на глаз» по
         * замеру на устройстве: с 90/7.5 качание гаслось меньше чем за 0,3 с и
         * игрок не успевал его увидеть. ω = √62 ≈ 7,9 рад/с, ζ = 5,4/(2ω) ≈ 0,34 —
         * один заметный перелёт через равновесие и покой примерно за 1,5 с.
         */
        const val STIFFNESS = 62f
        const val DAMPING = 5.4f
    }
}

/** Геометрия наклона: опора, целевой угол, проседание и доля покачивания. */
private class TiltGeometry(val pivotX: Float, val pivotY: Float, val target: Float, val sag: Float, val swayWeight: Float)

/**
 * Опора — центроид оставшихся (невывернутых) винтов; целевой угол — такой, при
 * котором центр массы корпуса встаёт ровно под опору.
 */
private fun tiltGeometry(piece: Piece, own: List<Screw>, isCollected: (Int) -> Boolean): TiltGeometry? {
    if (own.isEmpty()) return null
    val remaining = own.filter { !isCollected(it.id) }
    if (remaining.isEmpty()) return null
    val removed = own.size - remaining.size
    if (removed == 0) return null
    val b = piece.bounds()
    val comX: Float
    val comY: Float
    if (piece.kind == "rect" || piece.points.size < 3) {
        comX = (b[0] + b[2]) / 2f
        comY = (b[1] + b[3]) / 2f
    } else {
        comX = piece.points.map { it.first }.average().toFloat()
        comY = piece.points.map { it.second }.average().toFloat()
    }
    val pivotX = remaining.map { it.x }.average().toFloat()
    val pivotY = remaining.map { it.y }.average().toFloat()
    val limit = when (remaining.size) {
        1 -> 100f
        2 -> 12f
        else -> 5f
    }
    // экранные координаты (y вниз): atan2(смещение вбок, смещение вниз) = угол по часовой
    val free = Math.toDegrees(atan2((comX - pivotX).toDouble(), (comY - pivotY).toDouble())).toFloat()
    val sev = removed.toFloat() / own.size
    return TiltGeometry(pivotX, pivotY, free.coerceIn(-limit, limit), sev * 8f, if (remaining.size == 1) 1f else 0.3f)
}

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
    tilts: PieceTilts,
    modifier: Modifier = Modifier,
    /** Доля окна «докачки» из GameScreen (1..0): на ней гаснет покачивание. */
    swayAmp: Float = 1f,
    falling: List<FallingPiece> = emptyList(),
    unscrewing: List<UnscrewingScrew> = emptyList(),
    holes: List<ScrewHole> = emptyList(),
) {
    // масштаб/сдвиг пересчитываем в рисовании; для тапов держим те же значения
    var scale = 1f
    var dx = 0f
    var dy = 0f
    // шаг пружин по реальному времени между кадрами (рисование идёт каждый кадр,
    // потому что в draw читается бесконечная фаза покачивания)
    val clock = remember { longArrayOf(0L) }
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
                    // ищем верхний доступный шуруп под точкой: наклонённую деталь
                    // «распрямляем» — тап курсором по уехавшему винту должен попадать
                    val hit = screws
                        .filter { !isCollected(it.id) && isAccessible(it) }
                        .sortedByDescending { s -> pieces.first { p -> p.id == s.pieceId }.layer }
                        .firstOrNull { s ->
                            val local = tilts.invert(s.pieceId, lx, ly)
                            kotlin.math.hypot((local.x - s.x).toDouble(), (local.y - s.y).toDouble()) < 70
                        }
                    if (hit != null) onScrewTap(hit.id)
                }
            },
    ) {
        scale = minOf(size.width / Board.W, size.height / Board.H)
        dx = (size.width - Board.W * scale) / 2f
        dy = (size.height - Board.H * scale) / 2f

        val nowNs = System.nanoTime()
        val dt = if (clock[0] == 0L) 0.016f else ((nowNs - clock[0]) / 1e9f).coerceAtMost(0.05f)
        clock[0] = nowNs
        // амплитуда покачивания задаётся снаружи (swayAmp): сама по себе эта фаза
        // кадров не вызывает — перерисовку держит тикер GameScreen (см. SWING_MS)
        tilts.step(pieces, screws, isCollected, dt, sin(phase * 2.0 * Math.PI).toFloat() * 2.2f * swayAmp)

        val ordered = pieces.sortedBy { it.layer }
        for (piece in ordered) {
            val own = piece.screwsOf(screws)
            val body: DrawScope.() -> Unit = {
                drawPiece(piece, scale, dx, dy, 1f)
                // следы выкрученных винтов, улетевших с этой детали:
                // белый контурный кружок (толщина — как кайма фигур, диаметр — половина винта)
                drawHoles(piece, holes, scale, dx, dy, 1f)
                own.forEach { screw ->
                    // выкручиваемый винт рисуется ниже отдельным слоем (с поворотом)
                    if (isCollected(screw.id) || unscrewing.any { it.screwId == screw.id }) return@forEach
                    drawScrew(screw, scale, dx, dy, dim = !isAccessible(screw))
                }
                // винт в фазе выкручивания — поверх, шлиц крутится против часовой
                unscrewing.filter { it.pieceId == piece.id }.forEach { u ->
                    drawScrewAt(
                        Offset(dx + u.x * scale, dy + u.y * scale),
                        Color(android.graphics.Color.parseColor(u.color)),
                        62f * scale,
                        rotation = u.rotation,
                    )
                }
            }
            // деталь висит на оставшихся винтах: поворот ВОКРУГ них (а не вокруг центра)
            val t = tilts.items[piece.id]
            if (t == null || (t.angle == 0f && t.sag == 0f)) {
                body()
            } else {
                val pivot = Offset(dx + t.pivotX * scale, dy + t.pivotY * scale)
                translate(top = t.sag * scale) { rotate(t.angle, pivot, body) }
            }
        }
        // выкручены ВСЕ винты — деталь падает вниз с поворотом и растворением
        // (пока не наступил beginAt — висит на месте с прежними следами винтов);
        // падать она начинает со своего последнего наклона, иначе «щелкает» обратно
        falling.forEach { f ->
            val p = f.progress
            val b = f.piece.bounds()
            val t = tilts.items[f.piece.id]
            val pivot = if (t != null) {
                Offset(dx + t.pivotX * scale, dy + t.pivotY * scale)
            } else {
                Offset(dx + (b[0] + b[2]) / 2f * scale, dy + (b[1] + b[3]) / 2f * scale)
            }
            val alpha = (1f - p).coerceIn(0f, 1f)
            val frozen = t?.angle ?: 0f
            translate(top = ((t?.sag ?: 0f) + p * p * 950f) * scale) {
                rotate(frozen + p * 26f * (if (f.piece.id % 2 == 0) 1f else -1f), pivot) {
                    drawPiece(f.piece, scale, dx, dy, alpha)
                    // отверстие за улетевшим винтом принадлежит падающей детали —
                    // иначе оно исчезает раньше, чем деталь сорвалась с места
                    drawHoles(f.piece, holes, scale, dx, dy, alpha)
                }
            }
        }
    }
}

private fun Piece.screwsOf(all: List<Screw>) = all.filter { it.pieceId == id }

/** Белые следы-отверстия выкрученных винтов детали (в координатах доски). */
private fun DrawScope.drawHoles(piece: Piece, holes: List<ScrewHole>, scale: Float, dx: Float, dy: Float, alpha: Float) {
    val stroke = if (piece.kind == "rect") 10f * scale else 12f * scale
    holes.filter { it.pieceId == piece.id }.forEach { h ->
        drawCircle(
            Color.White.copy(alpha = alpha),
            31f * scale,
            Offset(dx + h.x * scale, dy + h.y * scale),
            style = Stroke(width = stroke),
        )
    }
}

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

internal fun DrawScope.drawScrewAt(center: Offset, color: Color, radius: Float, dim: Boolean = false, rotation: Float = 0f) {
    // тень
    drawCircle(Color.Black.copy(alpha = if (dim) 0.1f else 0.22f), radius, Offset(center.x, center.y + radius * 0.14f))
    // тело
    drawCircle(color, radius, center)
    // тёмный ободок
    drawCircle(color.darkenSelf(), radius, center, style = Stroke(width = radius * 0.16f))
    // блик
    drawCircle(Color.White.copy(alpha = 0.35f), radius * 0.5f, Offset(center.x - radius * 0.25f, center.y - radius * 0.28f))
    // крест-шлиц (крутится при выкручивании — rotation в градусах)
    val w = radius * 0.16f
    val arm = radius * 0.5f
    if (rotation == 0f) {
        drawLine(Color.Black.copy(alpha = 0.55f), Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), w, cap = StrokeCap.Round)
        drawLine(Color.Black.copy(alpha = 0.55f), Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), w, cap = StrokeCap.Round)
    } else {
        rotate(rotation, center) {
            drawLine(Color.Black.copy(alpha = 0.55f), Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), w, cap = StrokeCap.Round)
            drawLine(Color.Black.copy(alpha = 0.55f), Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), w, cap = StrokeCap.Round)
        }
    }
}

internal fun Color.darkenSelf(): Color {
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

/**
 * Склад: ряд кружков под ящиками; заполненный кружок — шляпка винта.
 * Радиус считается от МАКСИМАЛЬНОГО числа слотов, а не от текущего: игрок
 * может «сверлом» добавить до 5 дырок (итого 10), и ещё на стартовых 5
 * кружки выглядят компактно — место справа/слева заранее «зарезервировано»,
 * новый слот не расталкивает соседей и не «упирается» в край ряда.
 */
private const val MAX_HOLD_SLOTS = 10

@Composable
fun HoldRow(slots: List<Color?>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val n = slots.size
        if (n == 0) return@Canvas
        val gap = size.width / n
        val ref = size.width / maxOf(n, MAX_HOLD_SLOTS)
        val r = (ref * 0.42f).coerceAtMost(size.height / 2f)
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
