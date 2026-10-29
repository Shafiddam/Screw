package com.fidapp.screw.game

/**
 * Библиотека уровней.
 *
 * Уровни 1..[HandcraftedLevels.Count] описаны руками (обучающие, как на скриншотах),
 * дальше работает [LevelGenerator]. Все уровни проверяются unit-тестом
 * `LevelSolvabilityTest` на проходимость жадским решателем.
 */
object HandcraftedLevels {
    const val Count = 15

    fun all(): List<Level> = buildList {
        add(level1())
        add(level2())
        add(level3())
        add(level4())
        add(level5())
        add(level6())
        add(level7())
        add(level8())
        add(level9())
        add(level10())
        add(level11())
        add(level12())
        add(level13())
        add(level14())
        add(level15())
    }

    fun byNumber(number: Int): Level? = all().firstOrNull { it.number == number }

    // ---- helpers -------------------------------------------------------

    private fun poly(id: Int, color: String, layer: Int, vararg pts: Pair<Float, Float>) =
        Piece(id, "poly", color, layer, points = pts.toList())

    private fun rect(id: Int, color: String, layer: Int, x: Float, y: Float, w: Float, h: Float, c: Float = 60f) =
        Piece(id, "rect", color, layer, x = x, y = y, w = w, h = h, corners = c)

    /** «Капсула»: вертикальная планка со скруглением на всю ширину. */
    private fun capsule(id: Int, color: String, layer: Int, cx: Float, top: Float, bottom: Float, width: Float) =
        rect(id, color, layer, cx - width / 2f, top, width, bottom - top, c = width / 2f)

    /** Выпуклая «капля»/треугольник с вершиной в (cx; cy-r). */
    private fun drop(id: Int, color: String, layer: Int, cx: Float, cy: Float, r: Float) = poly(
        id, color, layer,
        cx to (cy - r),
        (cx + r * 0.95f) to (cy + r * 0.62f),
        (cx - r * 0.95f) to (cy + r * 0.62f),
    )

    /** «Облако» — выпуклый многоугольник из k вершин. */
    private fun cloud(id: Int, color: String, layer: Int, cx: Float, cy: Float, rx: Float, ry: Float, k: Int, rot: Double = 0.0) =
        poly(
            id, color, layer,
            *(0 until k).map { i ->
                val a = Math.toRadians(rot + i * 360.0 / k)
                (cx + rx * Math.cos(a)).toFloat() to (cy + ry * Math.sin(a)).toFloat()
            }.toTypedArray(),
        )

    private fun screwsFor(pieces: List<Piece>, vararg pairs: Pair<Int, Triple<ScrewColor, Int, Int>>): List<Screw> {
        val out = ArrayList<Screw>()
        var id = 0
        pairs.forEach { (pieceId, spec) ->
            out += Screw(id++, pieceId, spec.first, spec.second.toFloat(), spec.third.toFloat())
        }
        check(out.map { it.pieceId }.all { pid -> pieces.any { it.id == pid } })
        // шуруп обязан лежать внутри своей детали
        out.forEach { s ->
            val piece = pieces.first { it.id == s.pieceId }
            check(piece.contains(s.x, s.y)) { "screw ${s.id} outside piece ${piece.id}" }
        }
        return out
    }

    private fun level(number: Int, pieces: List<Piece>, screws: List<Screw>): Level =
        // 5 кружков склада — как на скриншотах; ящики-заказы строит движок по цветам
        Level(number, 5, pieces, screws)

    // ---- уровни --------------------------------------------------------

    private fun level1(): Level {
        val pieces = listOf(drop(0, "#C5E1A5", 0, 500f, 860f, 230f))
        return level(
            1, pieces,
            screwsFor(
                pieces,
                0 to Triple(ScrewColor.GREEN, 500, 760),
                0 to Triple(ScrewColor.GREEN, 590, 930),
                0 to Triple(ScrewColor.BLUE, 410, 930),
            ),
        )
    }

    private fun level2(): Level {
        val pieces = listOf(
            capsule(0, "#C5E1A5", 0, 500f, 620f, 1180f, 200f),
            drop(1, "#C5E1A5", 1, 400f, 520f, 170f),
            drop(2, "#C5E1A5", 2, 640f, 1180f, 160f),
        )
        return level(
            2, pieces,
            screwsFor(
                pieces,
                1 to Triple(ScrewColor.GREEN, 330, 480),
                1 to Triple(ScrewColor.GREEN, 430, 590),
                0 to Triple(ScrewColor.BLUE, 500, 720),
                0 to Triple(ScrewColor.BLUE, 500, 880),
                0 to Triple(ScrewColor.BLUE, 500, 1040),
                2 to Triple(ScrewColor.PURPLE, 600, 1150),
                2 to Triple(ScrewColor.GREEN, 690, 1200),
            ),
        )
    }

    private fun level3(): Level {
        val pieces = listOf(
            rect(0, "#B39DDB", 0, 250f, 640f, 500f, 480f, c = 40f),
            capsule(1, "#FFCDD2", 1, 700f, 560f, 1180f, 190f),
            cloud(2, "#C5E1A5", 2, 300f, 900f, 190f, 210f, k = 8, rot = 20.0),
        )
        return level(
            3, pieces,
            screwsFor(
                pieces,
                0 to Triple(ScrewColor.BLUE, 320, 700),
                0 to Triple(ScrewColor.RED, 500, 800),
                0 to Triple(ScrewColor.ORANGE, 400, 1050),
                1 to Triple(ScrewColor.RED, 700, 650),
                1 to Triple(ScrewColor.RED, 700, 860),
                1 to Triple(ScrewColor.ORANGE, 700, 1080),
                2 to Triple(ScrewColor.ORANGE, 250, 830),
                2 to Triple(ScrewColor.BLUE, 350, 960),
            ),
        )
    }

    private fun level4(): Level {
        val pieces = listOf(
            rect(0, "#90CAF9", 0, 120f, 560f, 380f, 620f, c = 70f),
            rect(1, "#B39DDB", 1, 420f, 480f, 460f, 520f, c = 50f),
            cloud(2, "#FFE0B2", 2, 500f, 1000f, 260f, 200f, k = 7, rot = 10.0),
            drop(3, "#F8BBD0", 3, 780f, 700f, 190f),
        )
        return level(
            4, pieces,
            screwsFor(
                pieces,
                0 to Triple(ScrewColor.CYAN, 220, 650),
                0 to Triple(ScrewColor.BLUE, 220, 900),
                0 to Triple(ScrewColor.RED, 340, 1080),
                1 to Triple(ScrewColor.BLUE, 520, 560),
                1 to Triple(ScrewColor.PURPLE, 700, 620),
                1 to Triple(ScrewColor.ORANGE, 600, 880),
                2 to Triple(ScrewColor.ORANGE, 420, 980),
                2 to Triple(ScrewColor.CYAN, 560, 1060),
                3 to Triple(ScrewColor.RED, 760, 680),
                3 to Triple(ScrewColor.PURPLE, 830, 760),
            ),
        )
    }

    private fun level5(): Level {
        val pieces = listOf(
            rect(0, "#EF9A9A", 0, 150f, 500f, 700f, 420f, c = 60f),
            capsule(1, "#80CBC4", 1, 320f, 640f, 1240f, 200f),
            capsule(2, "#CE93D8", 2, 680f, 560f, 1160f, 200f),
            cloud(3, "#FFF59D", 3, 500f, 1120f, 240f, 190f, k = 8, rot = 15.0),
            drop(4, "#A5D6A7", 4, 500f, 420f, 170f),
        )
        return level(
            5, pieces,
            screwsFor(
                pieces,
                0 to Triple(ScrewColor.RED, 250, 560),
                0 to Triple(ScrewColor.ORANGE, 750, 600),
                0 to Triple(ScrewColor.BLUE, 700, 820),
                1 to Triple(ScrewColor.CYAN, 320, 760),
                1 to Triple(ScrewColor.CYAN, 320, 960),
                1 to Triple(ScrewColor.GREEN, 320, 1150),
                2 to Triple(ScrewColor.PURPLE, 680, 700),
                2 to Triple(ScrewColor.PURPLE, 680, 900),
                3 to Triple(ScrewColor.YELLOW, 430, 1100),
                3 to Triple(ScrewColor.ORANGE, 580, 1150),
                4 to Triple(ScrewColor.GREEN, 500, 400),
                4 to Triple(ScrewColor.BLUE, 560, 470),
            ),
        )
    }

    private fun level6(): Level {
        val pieces = listOf(
            rect(0, "#A5D6A7", 0, 130f, 480f, 350f, 700f, c = 80f),
            rect(1, "#F48FB1", 1, 520f, 460f, 350f, 700f, c = 80f),
            cloud(2, "#B39DDB", 2, 500f, 830f, 230f, 300f, k = 6, rot = 5.0),
            drop(3, "#90CAF9", 3, 250f, 1180f, 190f),
            drop(4, "#FFE0B2", 4, 760f, 1180f, 190f),
            rect(5, "#9E9E9E", 5, 380f, 300f, 240f, 180f, c = 40f, ).copy(locked = true),
        )
        return level(
            6, pieces,
            screwsFor(
                pieces,
                0 to Triple(ScrewColor.GREEN, 220, 560),
                0 to Triple(ScrewColor.BLUE, 220, 800),
                0 to Triple(ScrewColor.RED, 300, 1050),
                1 to Triple(ScrewColor.PINK, 620, 560),
                1 to Triple(ScrewColor.RED, 780, 700),
                1 to Triple(ScrewColor.ORANGE, 620, 1000),
                2 to Triple(ScrewColor.PURPLE, 430, 760),
                2 to Triple(ScrewColor.CYAN, 570, 900),
                3 to Triple(ScrewColor.BLUE, 250, 1160),
                3 to Triple(ScrewColor.GREEN, 320, 1220),
                4 to Triple(ScrewColor.ORANGE, 760, 1160),
                4 to Triple(ScrewColor.CYAN, 830, 1220),
            ),
        )
    }

    private fun level7(): Level {
        val pieces = listOf(
            cloud(0, "#80CBC4", 0, 300f, 620f, 240f, 220f, k = 5, rot = 30.0),
            rect(1, "#FFF59D", 1, 480f, 420f, 400f, 400f, c = 50f),
            capsule(2, "#CE93D8", 2, 500f, 700f, 1300f, 220f),
            drop(3, "#A5D6A7", 3, 220f, 1120f, 220f),
            rect(4, "#9E9E9E", 4, 600f, 900f, 260f, 200f, c = 40f).copy(locked = true),
        )
        return level(
            7, pieces,
            screwsFor(
                pieces,
                0 to Triple(ScrewColor.CYAN, 240, 560),
                0 to Triple(ScrewColor.BLUE, 360, 680),
                1 to Triple(ScrewColor.YELLOW, 580, 500),
                1 to Triple(ScrewColor.YELLOW, 780, 700),
                1 to Triple(ScrewColor.ORANGE, 620, 740),
                2 to Triple(ScrewColor.PURPLE, 500, 800),
                2 to Triple(ScrewColor.PURPLE, 500, 1000),
                2 to Triple(ScrewColor.RED, 500, 1200),
                3 to Triple(ScrewColor.GREEN, 180, 1080),
                3 to Triple(ScrewColor.RED, 280, 1180),
            ),
        )
    }

    private fun level8(): Level {
        val pieces = listOf(
            rect(0, "#90CAF9", 0, 120f, 420f, 760f, 360f, c = 60f),
            capsule(1, "#F48FB1", 1, 260f, 600f, 1260f, 210f),
            capsule(2, "#80CBC4", 2, 740f, 560f, 1220f, 210f),
            cloud(3, "#B39DDB", 3, 500f, 940f, 250f, 230f, k = 8, rot = 12.0),
            drop(4, "#FFF59D", 4, 500f, 1280f, 190f),
        )
        return level(
            8, pieces,
            screwsFor(
                pieces,
                0 to Triple(ScrewColor.BLUE, 220, 480),
                0 to Triple(ScrewColor.CYAN, 500, 500),
                0 to Triple(ScrewColor.PINK, 780, 520),
                1 to Triple(ScrewColor.PINK, 260, 720),
                1 to Triple(ScrewColor.GREEN, 260, 940),
                1 to Triple(ScrewColor.YELLOW, 260, 1160),
                2 to Triple(ScrewColor.CYAN, 740, 680),
                2 to Triple(ScrewColor.ORANGE, 740, 900),
                2 to Triple(ScrewColor.RED, 740, 1120),
                3 to Triple(ScrewColor.PURPLE, 420, 880),
                3 to Triple(ScrewColor.PURPLE, 580, 1000),
                4 to Triple(ScrewColor.YELLOW, 440, 1270),
                4 to Triple(ScrewColor.RED, 560, 1300),
            ),
        )
    }

    private fun level9(): Level {
        val pieces = listOf(
            cloud(0, "#FFCC80", 0, 500f, 700f, 330f, 280f, k = 9, rot = 0.0),
            rect(1, "#A5D6A7", 1, 120f, 900f, 340f, 380f, c = 60f),
            rect(2, "#EF9A9A", 2, 540f, 900f, 340f, 380f, c = 60f),
            capsule(3, "#B39DDB", 3, 500f, 1000f, 1340f, 200f),
            rect(4, "#9E9E9E", 4, 360f, 380f, 280f, 200f, c = 40f).copy(locked = true),
        )
        return level(
            9, pieces,
            screwsFor(
                pieces,
                0 to Triple(ScrewColor.ORANGE, 380, 640),
                0 to Triple(ScrewColor.BLUE, 620, 640),
                0 to Triple(ScrewColor.GREEN, 500, 820),
                1 to Triple(ScrewColor.GREEN, 220, 980),
                1 to Triple(ScrewColor.CYAN, 300, 1180),
                2 to Triple(ScrewColor.RED, 660, 980),
                2 to Triple(ScrewColor.PINK, 760, 1180),
                3 to Triple(ScrewColor.PURPLE, 500, 1080),
                3 to Triple(ScrewColor.PURPLE, 500, 1260),
            ),
        )
    }

    private fun level10(): Level {
        val pieces = listOf(
            rect(0, "#90CAF9", 0, 100f, 400f, 800f, 320f, c = 50f),
            rect(1, "#FFF59D", 1, 100f, 760f, 380f, 560f, c = 50f),
            rect(2, "#F48FB1", 2, 520f, 760f, 380f, 560f, c = 50f),
            capsule(3, "#80CBC4", 3, 500f, 520f, 1100f, 210f),
            cloud(4, "#CE93D8", 4, 500f, 1240f, 280f, 160f, k = 7, rot = 8.0),
        )
        return level(
            10, pieces,
            screwsFor(
                pieces,
                0 to Triple(ScrewColor.BLUE, 200, 480),
                0 to Triple(ScrewColor.YELLOW, 500, 500),
                0 to Triple(ScrewColor.CYAN, 800, 480),
                1 to Triple(ScrewColor.YELLOW, 200, 850),
                1 to Triple(ScrewColor.GREEN, 300, 1100),
                2 to Triple(ScrewColor.PINK, 700, 850),
                2 to Triple(ScrewColor.RED, 620, 1100),
                3 to Triple(ScrewColor.CYAN, 500, 640),
                3 to Triple(ScrewColor.PURPLE, 500, 860),
                4 to Triple(ScrewColor.ORANGE, 420, 1230),
                4 to Triple(ScrewColor.ORANGE, 580, 1260),
            ),
        )
    }

    private fun level11(): Level {
        val pieces = listOf(
            cloud(0, "#A5D6A7", 0, 300f, 560f, 250f, 230f, k = 6, rot = 18.0),
            cloud(1, "#90CAF9", 1, 700f, 620f, 250f, 230f, k = 6, rot = 40.0),
            rect(2, "#FFCC80", 2, 260f, 820f, 480f, 460f, c = 60f),
            capsule(3, "#EF9A9A", 3, 160f, 760f, 1320f, 190f),
            capsule(4, "#B39DDB", 4, 840f, 760f, 1320f, 190f),
            rect(5, "#9E9E9E", 5, 420f, 380f, 220f, 180f, c = 40f).copy(locked = true),
        )
        return level(
            11, pieces,
            screwsFor(
                pieces,
                0 to Triple(ScrewColor.GREEN, 240, 520),
                0 to Triple(ScrewColor.BLUE, 360, 620),
                1 to Triple(ScrewColor.CYAN, 660, 560),
                1 to Triple(ScrewColor.ORANGE, 760, 680),
                2 to Triple(ScrewColor.YELLOW, 380, 920),
                2 to Triple(ScrewColor.RED, 600, 1160),
                3 to Triple(ScrewColor.GREEN, 160, 880),
                3 to Triple(ScrewColor.PURPLE, 160, 1160),
                4 to Triple(ScrewColor.CYAN, 840, 880),
                4 to Triple(ScrewColor.PINK, 840, 1160),
            ),
        )
    }

    private fun level12(): Level {
        val pieces = listOf(
            rect(0, "#CE93D8", 0, 120f, 420f, 760f, 340f, c = 60f),
            capsule(1, "#FFF59D", 1, 300f, 560f, 1240f, 220f),
            capsule(2, "#80CBC4", 2, 700f, 560f, 1240f, 220f),
            drop(3, "#F48FB1", 3, 500f, 1180f, 240f),
            cloud(4, "#90CAF9", 4, 500f, 820f, 220f, 200f, k = 8, rot = 22.0),
        )
        return level(
            12, pieces,
            screwsFor(
                pieces,
                0 to Triple(ScrewColor.PURPLE, 220, 480),
                0 to Triple(ScrewColor.BLUE, 500, 500),
                0 to Triple(ScrewColor.ORANGE, 780, 480),
                1 to Triple(ScrewColor.YELLOW, 300, 700),
                1 to Triple(ScrewColor.YELLOW, 300, 960),
                1 to Triple(ScrewColor.GREEN, 300, 1180),
                2 to Triple(ScrewColor.CYAN, 700, 700),
                2 to Triple(ScrewColor.RED, 700, 960),
                2 to Triple(ScrewColor.PINK, 700, 1180),
                4 to Triple(ScrewColor.BLUE, 430, 780),
                4 to Triple(ScrewColor.ORANGE, 570, 860),
                3 to Triple(ScrewColor.RED, 440, 1180),
                3 to Triple(ScrewColor.GREEN, 560, 1220),
            ),
        )
    }

    private fun level13(): Level {
        val pieces = listOf(
            cloud(0, "#FFCC80", 0, 500f, 620f, 360f, 240f, k = 10, rot = 6.0),
            rect(1, "#A5D6A7", 1, 100f, 860f, 360f, 440f, c = 50f),
            rect(2, "#EF9A9A", 2, 540f, 860f, 360f, 440f, c = 50f),
            capsule(3, "#B39DDB", 3, 500f, 900f, 1340f, 200f),
            rect(4, "#9E9E9E", 4, 120f, 420f, 240f, 200f, c = 40f).copy(locked = true),
            rect(5, "#9E9E9E", 5, 660f, 380f, 240f, 200f, c = 40f).copy(locked = true),
        )
        return level(
            13, pieces,
            screwsFor(
                pieces,
                0 to Triple(ScrewColor.ORANGE, 380, 560),
                0 to Triple(ScrewColor.CYAN, 500, 700),
                0 to Triple(ScrewColor.BLUE, 620, 560),
                1 to Triple(ScrewColor.GREEN, 220, 960),
                1 to Triple(ScrewColor.PURPLE, 300, 1200),
                2 to Triple(ScrewColor.RED, 700, 960),
                2 to Triple(ScrewColor.PINK, 620, 1200),
                3 to Triple(ScrewColor.YELLOW, 500, 1000),
                3 to Triple(ScrewColor.YELLOW, 500, 1240),
            ),
        )
    }

    private fun level14(): Level {
        val pieces = listOf(
            rect(0, "#90CAF9", 0, 100f, 380f, 800f, 320f, c = 60f),
            cloud(1, "#F48FB1", 1, 280f, 860f, 240f, 300f, k = 7, rot = 14.0),
            cloud(2, "#80CBC4", 2, 720f, 860f, 240f, 300f, k = 7, rot = 34.0),
            capsule(3, "#FFF59D", 3, 500f, 1000f, 1360f, 210f),
            drop(4, "#CE93D8", 4, 500f, 560f, 200f),
        )
        return level(
            14, pieces,
            screwsFor(
                pieces,
                0 to Triple(ScrewColor.BLUE, 200, 440),
                0 to Triple(ScrewColor.CYAN, 500, 440),
                0 to Triple(ScrewColor.PURPLE, 800, 440),
                1 to Triple(ScrewColor.PINK, 220, 780),
                1 to Triple(ScrewColor.GREEN, 340, 960),
                2 to Triple(ScrewColor.CYAN, 780, 780),
                2 to Triple(ScrewColor.ORANGE, 660, 960),
                3 to Triple(ScrewColor.YELLOW, 500, 1080),
                3 to Triple(ScrewColor.RED, 500, 1300),
                4 to Triple(ScrewColor.PURPLE, 440, 540),
                4 to Triple(ScrewColor.BLUE, 560, 600),
            ),
        )
    }

    private fun level15(): Level {
        val pieces = listOf(
            rect(0, "#A5D6A7", 0, 100f, 360f, 380f, 500f, c = 50f),
            rect(1, "#EF9A9A", 1, 520f, 360f, 380f, 500f, c = 50f),
            capsule(2, "#B39DDB", 2, 300f, 700f, 1340f, 210f),
            capsule(3, "#FFCC80", 3, 700f, 700f, 1340f, 210f),
            cloud(4, "#90CAF9", 4, 500f, 1000f, 260f, 260f, k = 9, rot = 10.0),
            rect(5, "#9E9E9E", 5, 400f, 1080f, 200f, 180f, c = 40f).copy(locked = true),
        )
        return level(
            15, pieces,
            screwsFor(
                pieces,
                0 to Triple(ScrewColor.GREEN, 200, 440),
                0 to Triple(ScrewColor.BLUE, 300, 700),
                1 to Triple(ScrewColor.RED, 800, 440),
                1 to Triple(ScrewColor.PINK, 700, 700),
                2 to Triple(ScrewColor.PURPLE, 300, 820),
                2 to Triple(ScrewColor.CYAN, 300, 1100),
                3 to Triple(ScrewColor.ORANGE, 700, 820),
                3 to Triple(ScrewColor.YELLOW, 700, 1100),
                4 to Triple(ScrewColor.BLUE, 430, 940),
                4 to Triple(ScrewColor.RED, 570, 1060),
            ),
        )
    }
}
