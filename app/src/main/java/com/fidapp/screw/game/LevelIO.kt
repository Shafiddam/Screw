package com.fidapp.screw.game

/**
 * Единая точка доступа к уровням: первые [HandcraftedLevels.Count] — ручные,
 * дальше — генератор. Генератор детерминирован по номеру уровня, поэтому
 * карта уровней у игрока всегда показывает те же раскладки.
 */
object Levels {
    private val generator = LevelGenerator()
    val handcraftedCount = HandcraftedLevels.Count

    fun forNumber(number: Int): Level =
        HandcraftedLevels.byNumber(number) ?: generator.generate(number)

    /** Число шурупов/деталей — используется в UI «Вызова дня». */
    fun preview(number: Int): Pair<Int, Int> = forNumber(number).let { it.screws.size to it.pieces.size }
}

/**
 * Генератор уровней сверх ручной десятки.
 *
 * Инвариант проходимости: ящики-заказы строятся движком ровно под фактическое
 * число винтов каждого цвета (ёмкость <= 3), склад — 5 кружков; порядок очереди
 * детерминирован номером уровня, а «милосердный» приезд (сначала — цвет со
 * склада) оставляет жадному решателю проход (тест это и проверяет).
 */
class LevelGenerator(private val seedBase: Long = 20261029L) {

    fun generate(number: Int): Level {
        // детерминированный подбор сида: уровень возвращается только если
        // он проходим — карта уровней у игрока всегда показывает один и тот же
        var attempt = 0
        while (attempt < 24) {
            val level = buildLevel(number, seedBase + number * 7919L + attempt * 1_000_003L)
            if (level.screws.isNotEmpty() && level.pieces.isNotEmpty() &&
                GameEngine(level).greedySolve()
            ) {
                return level
            }
            attempt++
        }
        return fallback(number)
    }

    private fun buildLevel(number: Int, seed: Long): Level {
        val random = Random(seed)
        // максимум 4 цвета: при пяти «чужих» винтов больше, чем кружков склада,
        // и уровень местами нерешаем даже идеальной игрой
        val colorsCount = (2 + ((number - 1) % 3)).coerceAtMost(4)
        val capacity = GameEngine.BOX_CAPACITY
        val pieceCount = (4 + number / 3).coerceAtMost(10)
        // цвета по кругу: каждая фигура — «микс» цветов, иначе верхние слои
        // состоят из цветов без активного ящика, склад забивается и уровень
        // становится нерешаемым по-настоящему
        val colorOrder = ScrewColor.entries.take(colorsCount).shuffled(random)
        val maxScrews = colorsCount * capacity

        val pieces = mutableListOf<Piece>()
        val screws = mutableListOf<Screw>()
        var screwId = 0
        var colorCursor = 0
        var placed = 0

        repeat(pieceCount) { index ->
            if (placed >= maxScrews) return@repeat
            val piece = randomPiece(index, index, random)
            val budget = (maxScrews - placed).coerceAtMost(2 + random.nextInt(3))
            if (budget <= 0) return@repeat
            val bounds = piece.bounds()
            repeat(budget) {
                val (sx, sy) = innerPoint(piece, bounds, random)
                screws += Screw(
                    id = screwId++,
                    pieceId = index,
                    color = colorOrder[colorCursor % colorsCount],
                    x = sx,
                    y = sy,
                )
                colorCursor++
            }
            placed += budget
            pieces += piece
        }

        // пару «заблокированных» серых деталей добавляем на поздних уровнях
        if (number >= 8 && random.nextBoolean()) {
            pieces += randomPiece(pieces.size, pieces.size, random, locked = true)
        }

        val level = Level(number, 5, pieces, screws)
        return level
    }

    private fun fallback(number: Int) = Level(
        number = number,
        holdSlots = 5,
        pieces = listOf(
            Piece(0, "rect", "#A5D6A7", 0, x = 250f, y = 400f, w = 500f, h = 400f, corners = 80f),
        ),
        screws = listOf(
            Screw(0, 0, ScrewColor.RED, 380f, 500f),
            Screw(1, 0, ScrewColor.BLUE, 620f, 500f),
            Screw(2, 0, ScrewColor.GREEN, 500f, 700f),
        ),
    )

    private fun randomPiece(id: Int, layer: Int, random: Random, locked: Boolean = false): Piece {
        val palette = listOf(
            "#A5D6A7", "#F48FB1", "#B39DDB", "#90CAF9", "#FFCC80",
            "#80CBC4", "#CE93D8", "#FFF59D", "#F8BBD0",
        )
        val color = if (locked) "#9E9E9E" else palette[random.nextInt(palette.size)]
        return when (random.nextInt(4)) {
            0 -> { // выпуклый «многолепестковый» контур вокруг центра
                val cx = 200f + random.nextInt(600)
                val cy = 350f + random.nextInt(700)
                val r = 170f + random.nextInt(110)
                val k = 3 + random.nextInt(4)
                val shift = random.nextInt(360).toFloat()
                val pts = (0 until k).map { i ->
                    val a = Math.toRadians((shift + i * 360.0 / k))
                    cx + (r * Math.cos(a)).toFloat() to cy + (r * Math.sin(a)).toFloat()
                }
                Piece(id, "poly", color, layer, points = pts, locked = locked)
            }
            1 -> { // широкая вертикальная планка
                val w = 260f + random.nextInt(140)
                val h = 620f + random.nextInt(320)
                val x = (150 + random.nextInt((Board.W - w - 300).toInt())).toFloat()
                val y = (280 + random.nextInt((Board.H - h - 500).toInt())).toFloat()
                Piece(id, "rect", color, layer, x = x, y = y, w = w, h = h, corners = 90f, locked = locked)
            }
            2 -> { // квадрат/пластина
                val side = 340f + random.nextInt(180)
                val x = (140 + random.nextInt((Board.W - side - 280).toInt())).toFloat()
                val y = (300 + random.nextInt((Board.H - side - 520).toInt())).toFloat()
                Piece(id, "rect", color, layer, x = x, y = y, w = side, h = side, corners = 60f, locked = locked)
            }
            else -> { // «капля»/треугольник
                val cx = 220f + random.nextInt(560)
                val cy = 420f + random.nextInt(620)
                val r = 200f + random.nextInt(90)
                val pts = listOf(
                    cx to (cy - r),
                    (cx + r * 0.92f) to (cy + r * 0.6f),
                    (cx - r * 0.92f) to (cy + r * 0.6f),
                )
                Piece(id, "poly", color, layer, points = pts, locked = locked)
            }
        }
    }

    /** Точка внутри детали, не прилипая к краям (чтобы шурупы не «вылезали»). */
    private fun innerPoint(piece: Piece, bounds: FloatArray, random: Random): Pair<Float, Float> {
        val minX = bounds[0] + 90f
        val maxX = bounds[2] - 90f
        val minY = bounds[1] + 90f
        val maxY = bounds[3] - 90f
        if (maxX > minX && maxY > minY) {
            repeat(24) {
                val x = minX + random.nextFloat() * (maxX - minX)
                val y = minY + random.nextFloat() * (maxY - minY)
                if (piece.contains(x, y)) return x to y
            }
        }
        return ((bounds[0] + bounds[2]) / 2f) to ((bounds[1] + bounds[3]) / 2f)
    }
}

/** Детерминированный ГПСЧ (xorshift), чтобы генератор давал одинаковые уровни на любом устройстве. */
class Random(private var state: Long) {
    fun nextInt(bound: Int): Int {
        if (bound <= 0) return 0
        val v = (next() % bound).toInt()
        return if (v < 0) v + bound else v
    }

    fun nextFloat(): Float = (next() % 100000L).toFloat() / 100000f

    fun nextBoolean(): Boolean = next() % 2L == 0L

    private fun next(): Long {
        var x = state
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        state = x
        return x and Long.MAX_VALUE
    }
}

/** Перемешивание по детерминированному ГПСЧ (свой, чтобы не зависеть от java.util). */
private fun <T> List<T>.shuffled(random: Random): List<T> {
    val src = ArrayList(this)
    val out = ArrayList<T>(src.size)
    while (src.isNotEmpty()) {
        val idx = random.nextInt(src.size)
        out += src.removeAt(idx)
    }
    return out
}
