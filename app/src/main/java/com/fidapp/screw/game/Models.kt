package com.fidapp.screw.game

/**
 * Модели уровня «отверточной» сортировки. Чистый Kotlin (без Android) —
 * на этих же моделях работает unit-тест проходимости и генератор уровней.

 * Логическая система координат доски: 1000 x 1400 единиц,
 * реальный Canvas масштабируется в неё через `scale`.
 */
object Board {
    const val W = 1000f
    const val H = 1400f
}

/** Цвет шурупа. Порядок важен: индексы используются в JSON уровней. */
enum class ScrewColor(val hex: String) {
    RED("#E53935"),
    BLUE("#1E88E5"),
    GREEN("#43A047"),
    ORANGE("#FB8C00"),
    PURPLE("#8E24AA"),
    CYAN("#00ACC1"),
    YELLOW("#FDD835"),
    PINK("#EC407A");

    companion object {
        fun of(name: String): ScrewColor =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: RED
    }
}

/** Один шуруп. `pieceId` — деталь, к которой он прикручен. */
data class Screw(
    val id: Int,
    val pieceId: Int,
    val color: ScrewColor,
    val x: Float,
    val y: Float,
)

/**
 * Деталь (форма) с шурупами.
 * kind: "poly" — многоугольник со скруглёнными углами (points),
 *       "rect" — прямоугольник со скруглением corners (x,y,w,h).
 * layer — порядок наложения, больше = выше. Верхняя деталь перекрывает нижние.
 * locked — «заблокированная» деталь (серая, без шурупов): мешает, снимается только бонусом «Плавить».
 */
data class Piece(
    val id: Int,
    val kind: String,
    val colorHex: String,
    val layer: Int,
    val points: List<Pair<Float, Float>> = emptyList(),
    val x: Float = 0f,
    val y: Float = 0f,
    val w: Float = 0f,
    val h: Float = 0f,
    val corners: Float = 40f,
    val locked: Boolean = false,
)

/** Уровень целиком. Ящики-заказы выводятся из цветов шурупов в движке. */
data class Level(
    val number: Int,
    val holdSlots: Int,
    val pieces: List<Piece>,
    val screws: List<Screw>,
)

/**
 * Геометрия детали: содержит ли точка (x; y) саму форму.
 * Для "poly" — выпуклый многоугольник (наши формы строятся выпуклыми),
 * для "rect" — прямоугольник с учётом скругления углов на corners.
 */
fun Piece.contains(px: Float, py: Float): Boolean {
    return when (kind) {
        "rect" -> {
            if (px < x || px > x + w || py < y || py > y + h) {
                false
            } else {
                val r = corners.coerceAtMost(minOf(w, h) / 2f)
                if (r <= 1f) {
                    true
                } else {
                    val dx = when {
                        px < x + r -> x + r - px
                        px > x + w - r -> px - (x + w - r)
                        else -> 0f
                    }
                    val dy = when {
                        py < y + r -> y + r - py
                        py > y + h - r -> py - (y + h - r)
                        else -> 0f
                    }
                    dx == 0f || dy == 0f || dx * dx + dy * dy <= r * r
                }
            }
        }
        else -> {
            if (points.size < 3) return false
            var sign = 0
            for (i in points.indices) {
                val (ax, ay) = points[i]
                val (bx, by) = points[(i + 1) % points.size]
                val cross = (bx - ax) * (py - ay) - (by - ay) * (px - ax)
                when {
                    cross > 0.0001 -> if (sign < 0) return false else sign = 1
                    cross < -0.0001 -> if (sign > 0) return false else sign = -1
                }
            }
            true
        }
    }
}

fun Piece.bounds(): FloatArray {
    return when (kind) {
        "rect" -> floatArrayOf(x, y, x + w, y + h)
        else -> {
            var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
            points.forEach { (px, py) ->
                if (px < minX) minX = px
                if (py < minY) minY = py
                if (px > maxX) maxX = px
                if (py > maxY) maxY = py
            }
            floatArrayOf(minX, minY, maxX, maxY)
        }
    }
}
