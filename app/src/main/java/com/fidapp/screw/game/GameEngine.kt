package com.fidapp.screw.game

/**
 * Движок уровня. Контракт (не менять без нужды):
 *  1. тап по доступному шурупу -> шуруп улетает в открытый ЯЩИК СВОЕГО ЦВЕТА;
 *     доступный — не перекрыт даже ЧАСТИЧНО ни одной живой деталью выше слоем;
 *     чтобы добраться до нижних фигур, сначала выкручивают винты верхних;
 *  2. вверху открыто ACTIVE_BOX_SLOTS (2) ящика, каждый ровно под ОДИН цвет,
 *     ёмкость ящика = числу шурупов этого цвета на уровне (но не больше 3);
 *  3. ящик наполнился — он уходит, из очереди приезжает следующий ящик;
 *     шурупы нужного цвета АВТОМАТИЧЕСКИ перелетают в новый ящик из склада;
 *  4. нет открытого ящика цвета — шуруп кладётся на СКЛАД (кружки под ящиками,
 *     level.holdSlots штук); склад забит и ни один доступный винт не подходит
 *     к ящикам — поражение;
 *  5. «Открыть ящик» (плитка-бонус, CLOSED_TILES на уровень) — за цену из
 *     Economy открывает следующий ящик очереди ДОПОЛНИТЕЛЬНЫМ активным;
 *  6. когда у детали не остаётся шурупов — она падает (событие PieceFell);
 *  7. победа — доска пуста (все шурупы в ящиках или на складе);
 *  8. движок не знает ни про деньги, ни про Android: только логика + события.
 */
class GameEngine(val level: Level) {

    companion object {
        /** Радиус шляпки винта в логических единицах доски. */
        const val SCREW_HEAD_R = 62f

        /** Сколько ящиков открыто изначально. */
        const val ACTIVE_BOX_SLOTS = 2

        /** Предельная ёмкость ящика (3 винта одного цвета). */
        const val BOX_CAPACITY = 3

        /** Плиток «Открыть ящик» на уровень. */
        const val CLOSED_TILES = 2
    }

    class PieceState(val piece: Piece) {
        var removed: Boolean = false
    }

    /** Ящик-заказ: один цвет, своя ёмкость. */
    class Box(val color: ScrewColor, val capacity: Int) {
        var count: Int = 0
        val full: Boolean get() = count >= capacity
    }

    /** Куда улетел шуруп. */
    sealed interface Destination {
        data class BoxSlot(val index: Int) : Destination
        data class HoldSlot(val index: Int) : Destination
    }

    sealed interface Event {
        data class ScrewCollected(val screw: Screw, val destination: Destination) : Event
        data class PieceFell(val piece: Piece) : Event
        data class BoxArrived(val box: Box) : Event
        data object Rejected : Event
        data object Win : Event
        data object Lose : Event
    }

    val pieceStates: List<PieceState> = level.pieces.map { PieceState(it) }

    /** Очередь ещё не приехавших ящиков (детерминирована номером уровня). */
    private val queue = ArrayDeque(buildBoxQueue(level))

    /** Открытые ящики слева направо; к ним летят шурупы. */
    val activeBoxes: MutableList<Box> = mutableListOf<Box>().also { boxes ->
        repeat(ACTIVE_BOX_SLOTS) { queue.removeFirstOrNull()?.let(boxes::add) }
    }

    /** Склад: кружки под ящиками, сюда попадают винты без своего ящика. */
    val hold: MutableList<Screw> = mutableListOf()

    /** Плитки «Открыть ящик», оставшиеся для бонуса. */
    var closedTiles: Int = CLOSED_TILES
        private set

    /** Бонус «Сверлить» добавляет свободные кружки склада. */
    var extraHold: Int = 0
        private set

    val holdCapacity: Int get() = level.holdSlots + extraHold

    private val collectedScrews = HashSet<Int>()
    var finished: Event? = null
        private set

    /** Все шурупы, включая уже собранные (UI фильтрует по `collected`). */
    val screws: List<Screw> = level.screws

    fun isCollected(screwId: Int): Boolean = screwId in collectedScrews

    fun pieceOf(screw: Screw): Piece = level.pieces.first { it.id == screw.pieceId }

    fun isLive(piece: Piece): Boolean = !pieceStates[piece.id].removed

    /**
     * Доступен ли шуруп: его деталь жива и ни одна деталь выше слоем
     * не перекрывает шляпку хотя бы частично (проба по центру и 4 точкам
     * на радиусе — иначе винт «задет» краем верхней фигуры).
     */
    fun isScrewAccessible(screw: Screw): Boolean {
        if (isCollected(screw.id)) return false
        val owner = pieceOf(screw)
        if (owner.locked || !isLive(owner)) return false
        return level.pieces.none { upper ->
            upper.id != owner.id && !upper.locked && isLive(upper) &&
                upper.layer > owner.layer && coversScrew(upper, screw)
        }
    }

    private fun coversScrew(piece: Piece, screw: Screw): Boolean {
        val r = SCREW_HEAD_R * 0.55f
        return piece.contains(screw.x, screw.y) ||
            piece.contains(screw.x + r, screw.y) ||
            piece.contains(screw.x - r, screw.y) ||
            piece.contains(screw.x, screw.y + r) ||
            piece.contains(screw.x, screw.y - r)
    }

    fun accessibleScrews(): List<Screw> = screws.filter { isScrewAccessible(it) }

    /** Индекс открытого ящика, который примет шуруп этого цвета (ближе к полному). */
    fun boxFor(color: ScrewColor): Int? =
        activeBoxes.indices
            .filter { activeBoxes[it].color == color && !activeBoxes[it].full }
            .maxByOrNull { activeBoxes[it].count }

    fun canTap(screw: Screw): Boolean =
        isScrewAccessible(screw) && (boxFor(screw.color) != null || hold.size < holdCapacity)

    fun tapScrew(screwId: Int): List<Event> {
        val screw = screws.firstOrNull { it.id == screwId } ?: return listOf(Event.Rejected)
        if (!isScrewAccessible(screw)) {
            evaluateEnd()
            return listOf(Event.Rejected)
        }
        val events = mutableListOf<Event>()
        val boxIndex = boxFor(screw.color)
        if (boxIndex != null) {
            val box = activeBoxes[boxIndex]
            box.count++
            collectedScrews += screw.id
            events += Event.ScrewCollected(screw, Destination.BoxSlot(boxIndex))
            if (box.full) {
                activeBoxes.removeAt(boxIndex)
                events += arriveNextBox()
            }
        } else if (hold.size < holdCapacity) {
            collectedScrews += screw.id
            hold += screw
            events += Event.ScrewCollected(screw, Destination.HoldSlot(hold.size - 1))
        } else {
            evaluateEnd()
            return listOf(Event.Rejected)
        }
        val owner = pieceOf(screw)
        if (screws.none { it.pieceId == owner.id && !isCollected(it.id) }) {
            pieceStates[owner.id].removed = true
            events += Event.PieceFell(owner)
        }
        events += evaluateEnd()
        return events
    }

    /** Можно ли открыть плитку «Открыть ящик»? */
    fun canOpenBox(): Boolean = closedTiles > 0 && queue.isNotEmpty() && finished == null

    /** Бонус «Открыть ящик»: следующий ящик очереди становится активным. */
    fun openBoxTile(): List<Event> {
        if (!canOpenBox()) return listOf(Event.Rejected)
        closedTiles--
        return arriveNextBox() + evaluateEnd()
    }

    /** Приезд ящика из очереди + автоперелёт подходящих винтов со склада. */
    private fun arriveNextBox(): List<Event> {
        val box = nextQueuedBox() ?: return emptyList()
        activeBoxes += box
        val events = mutableListOf<Event>(Event.BoxArrived(box))
        // склад «разгружается», если приехал ящик его цвета
        var changed = true
        while (changed && !box.full) {
            changed = false
            val waiting = hold.indexOfFirst { it.color == box.color }
            if (waiting >= 0) {
                events += Event.ScrewCollected(hold.removeAt(waiting), Destination.BoxSlot(activeBoxes.lastIndex))
                box.count++
                changed = true
            }
        }
        if (box.full) {
            val idx = activeBoxes.indexOf(box)
            if (idx >= 0) activeBoxes.removeAt(idx)
            events += arriveNextBox()
        }
        return events
    }

    /** «Милосердная» очередь: первым приезжает цвет, которого больше всего на складе. */
    private fun nextQueuedBox(): Box? {
        if (queue.isEmpty()) return null
        if (hold.isNotEmpty()) {
            val mostHeld = hold.groupingBy { it.color }.eachCount().maxByOrNull { it.value }!!.key
            val idx = queue.indexOfFirst { it.color == mostHeld }
            if (idx >= 0) return queue.removeAt(idx)
        }
        return queue.removeFirst()
    }

    /** Бонус «Сверлить»: +1 кружок склада (не больше трёх за уровень). */
    fun drill(): Boolean {
        if (extraHold >= 3) return false
        extraHold++
        return true
    }

    /** Бонус «Плавить»: убирает верхнюю заблокированную деталь. */
    fun melt(): Piece? {
        val target = level.pieces
            .filter { it.locked && isLive(it) }
            .maxByOrNull { it.layer } ?: level.pieces
            .filter { isLive(it) && screws.none { s -> s.pieceId == it.id } }
            .maxByOrNull { it.layer } ?: return null
        pieceStates[target.id].removed = true
        evaluateEnd()
        return target
    }

    /** Бонус «Заделать»: освобождает склад (винты «упакованы», кружки пустеют). */
    fun seal(): Boolean {
        if (hold.isEmpty()) return false
        hold.clear()
        evaluateEnd()
        return true
    }

    private fun evaluateEnd(): List<Event> {
        if (finished != null) return emptyList()
        return when {
            collectedScrews.size == screws.size -> {
                finished = Event.Win
                listOf(Event.Win)
            }
            // поражение: шурупы ещё на доске, но ни один ход легален
            screws.any { !isCollected(it.id) } &&
                accessibleScrews().none { canTap(it) } -> {
                finished = Event.Lose
                listOf(Event.Lose)
            }
            else -> emptyList()
        }
    }

    /**
     * Жадная авто-проверка проходимости (используется unit-тестом уровней).
     * На каждом шаге выбирает ход по симуляции на клоне движка (просмотр на 1 ход):
     * сначала ходы, ведущие к победе, затем — сколько винтов собрано,
     * прогресс открытых ящиков и количество доступных винтов после хода.
     */
    fun greedySolve(maxSteps: Int = 4000): Boolean {
        val history = mutableListOf<Int>()
        fun replay(moves: List<Int>): GameEngine {
            val clone = GameEngine(level)
            moves.forEach { clone.tapScrew(it) }
            return clone
        }
        fun score(e: GameEngine): Int = when (e.finished) {
            Event.Win -> 1_000_000
            Event.Lose -> -1_000_000
            else -> e.collectedScrews.size * 100 +
                e.activeBoxes.sumOf { it.count } * 10 +
                e.accessibleScrews().size
        }
        var steps = 0
        while (steps++ < maxSteps) {
            if (finished != null) break
            val candidates = accessibleScrews().filter { canTap(it) }
            if (candidates.isEmpty()) break
            val best = candidates
                .map { it.id to replay(history + it.id) }
                .maxByOrNull { (_, sim) -> score(sim) }
                ?.first ?: break
            history += best
            tapScrew(best)
        }
        return finished == Event.Win
    }

    /**
     * Очередь ящиков: каждый цвет делится на ящики ёмкостью <= 3
     * (7 зелёных -> 3+2+2), порядок детерминирован номером уровня,
     * первые два — разных цветов, чтобы на старте было два разных заказа.
     */
    private fun buildBoxQueue(level: Level): List<Box> {
        val counts = level.screws.groupingBy { it.color }.eachCount()
        val boxes = counts.flatMap { (color, total) ->
            val parts = (total + BOX_CAPACITY - 1) / BOX_CAPACITY
            val base = total / parts
            val bigger = total % parts
            List(parts) { i -> Box(color, base + if (i < bigger) 1 else 0) }
        }
        if (boxes.size < 2) return boxes
        val random = Random(20261029L + level.number * 104729L)
        val shuffled = ArrayList(boxes)
        for (i in shuffled.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = shuffled[i]; shuffled[i] = shuffled[j]; shuffled[j] = tmp
        }
        if (shuffled[0].color == shuffled[1].color) {
            val other = shuffled.indices.first { shuffled[it].color != shuffled[0].color }
            val tmp = shuffled[1]; shuffled[1] = shuffled[other]; shuffled[other] = tmp
        }
        return shuffled
    }
}
