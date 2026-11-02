package com.fidapp.screw.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.window.Dialog
import com.fidapp.screw.ads.InterstitialManager
import com.fidapp.screw.ads.RewardAdManager
import com.fidapp.screw.game.Board
import com.fidapp.screw.game.Economy
import com.fidapp.screw.game.GameEngine
import com.fidapp.screw.game.ScrewColor
import com.fidapp.screw.game.Levels
import com.fidapp.screw.game.PowerUp
import com.fidapp.screw.game.PlayerStore
import com.fidapp.screw.game.Screw
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.sin

/** Фаза выкручивания и перелёт винта, мс. */
private const val UNSCREW_MS = 520L
private const val FLY_MS = 400L

/**
 * Задержка перед падением детали: ПО СЛОВУ пользователя деталь должна начать
 * падать ОДНОВРЕМЕННО с тем моментом, как с неё улетает последний винт — т.
 * винт закончил выкручиваться (t = UNSCREW_MS) и полетел в ящик. Раньше стоял
 * +30 мс и ждали полного приземления винта (UNSCREW_MS+FLY_MS+30) — это
 * давало заметную задержку, которая выглядела как «залипание» фигуры.
 */
private const val FALL_DELAY_MS = UNSCREW_MS

/** Длительность анимации смены ящика в ряду: старый уезжает вверх, новый въезжает.
 *  Специально длинно — при 320 мс игрок не успевает увидеть сам переезд, а при
 *  700 мс пользователь всё ещё жаловался «ящики улетают слишком быстро, я не
 *  вижу анимацию» — подняли до 1100 мс, сам переезд стал читаемым. */
private const val BOX_SWAP_MS = 1100

/**
 * Окно кадров после выкручивания винта, мс. Нужно специально: бесконечная
 * анимация покачивания внутри Canvas кадров НЕ генерирует (проверено на устройстве
 * через `dumpsys gfxinfo`: 0 отрисовок в покое), поэтому пружина маятника
 * считалась бы только во время полёта винта и деталь замирала на середине взмаха.
 * 1800 мс хватает, чтобы качание полностью погасло (ζ≈0,34, ω≈7,9 рад/с).
 */
private const val SWING_MS = 1800L

@Composable
fun GameScreen(
    levelNumber: Int,
    player: PlayerStore,
    feedback: Feedback,
    onExit: () -> Unit,
    onNext: (Int) -> Unit,
) {
    val level = remember(levelNumber) { Levels.forNumber(levelNumber) }
    val engine = remember(levelNumber) { GameEngine(level) }
    var tick by remember { mutableIntStateOf(0) }
    // счётчик «доигровки»: ++ на каждый прилёт винта; по нему отдельный LaunchedEffect
    // через паузу убирает полный ящик и определяет итог (чтобы винт успел показаться в ящике)
    var settleSeq by remember { mutableIntStateOf(0) }
    val flying = remember { mutableStateListOf<FlyingScrew>() }
    val falling = remember { mutableStateListOf<FallingPiece>() }
    // наклон деталей вокруг оставшихся винтов (физика живёт в BoardCanvas, но
    // координаты летящего винта нужны здесь — поэтому модель общая)
    val tilts = remember(levelNumber) { PieceTilts() }
    var frameTime by remember { mutableLongStateOf(0L) }
    // до какого момента тянуть кадры «докачки» маятника (0 — окно закрыто)
    var swingUntil by remember { mutableLongStateOf(0L) }
    // экранные координаты доски, ряда ящиков и склада: чтобы шуруп летел в нужную ячейку
    var boardBounds by remember { mutableStateOf(Rect.Zero) }
    var boxRowBounds by remember { mutableStateOf(Rect.Zero) }
    var holdRowBounds by remember { mutableStateOf(Rect.Zero) }
    // точные прямоугольники каждого ящика (в ряде есть и плитки «Открыть ящик»,
    // делить ширину ряда на число ящиков — смещало цель полёта вправо)
    val boxCells = remember { mutableStateListOf<Rect>() }

    var showPause by remember { mutableStateOf(false) }
    var showWin by remember { mutableStateOf(false) }
    var showLose by remember { mutableStateOf(false) }
    var showRevive by remember { mutableStateOf(false) }
    var showFreeCoins by remember { mutableStateOf(false) }
    var bonus by remember { mutableStateOf<PowerUp?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    // награда за победу уже удвоена просмотром видео (актуально для текущего окна победы)
    var winDoubled by remember(levelNumber) { mutableStateOf(false) }

    // Снимок лотка в наблюдаемом Compose-состоянии: ряд ящиков/склада читает
    // эти значения, а не поля движка напрямую — так перерисовка ряда гарантирована
    // после каждого изменения (engine — обычный класс, Compose его изменения не отслеживает).
    var trayBoxes by remember { mutableStateOf<List<Pair<ScrewColor, Int>>>(emptyList()) }
    var trayHold by remember { mutableStateOf<List<ScrewColor?>>(emptyList()) }
    var trayTiles by remember { mutableStateOf(0) }

    // строки вычисляем здесь (@Composable), чтобы использовать внутри не-composable колбэков
    val click = LocalClickSound.current
    val notEnough = L("not_enough")
    val noBoxes = L("box_none")
    val activity = LocalContext.current as? android.app.Activity

    /** Бонус за просмотр rewarded-видео: награда только после onReward. */
    fun showReward(action: () -> Unit) {
        val act = activity
        if (act != null) RewardAdManager.show(act, action) else action()
    }

    /** Итог уровня (победа или финальное поражение) — счётчик полноэкранной рекламы. */
    fun notifyLevelFinished() {
        activity?.let { InterstitialManager.onLevelFinished(it) }
    }

    /** Обработка событий движка: упавшие детали, победа, поражение (с учётом
     *  того, что итог приходит из settle(), когда винт уже прилетел). */
    fun consume(events: List<GameEngine.Event>) {
        events.forEach { e ->
            when (e) {
                is GameEngine.Event.PieceFell ->
                    falling.add(FallingPiece(e.piece, 0f, beginAt = System.currentTimeMillis() + FALL_DELAY_MS))
                // винт собран — короткое «правильно»
                is GameEngine.Event.ScrewCollected -> feedback.ok(player.sound)
                // приехал новый ящик (полный уехал) — средний «правильно», прогресс
                is GameEngine.Event.BoxArrived -> feedback.right(player.sound)
                is GameEngine.Event.Win -> {
                    feedback.win(player.sound)
                    player.onLevelWon(levelNumber)
                    winDoubled = false
                    showWin = true
                    notifyLevelFinished()
                }
                is GameEngine.Event.Lose -> {
                    feedback.wrong(player.sound)
                    player.spendLife()
                    if (player.canRevive(levelNumber)) showRevive = true
                    else {
                        showLose = true
                        notifyLevelFinished()
                    }
                }
                else -> {}
            }
        }
    }

    /** Обновить снимок лотка из движка (после любой мутации движка). */
    fun refreshTray() {
        trayBoxes = engine.activeBoxes.map { it.color to it.count }
        trayTiles = engine.closedTiles
        trayHold = (0 until engine.holdCapacity).map { i -> engine.hold.getOrNull(i)?.color }
    }

    LaunchedEffect(levelNumber) {
        refreshTray()
        // отметка «сегодня играли» — по ней ReminderWorker решает, пинговать ли
        player.markPlayedToday()
    }

    // тикер анимаций: фаза выкручивания (0..1, шлиц крутится ПРОТИВ часовой ~1 оборот
    // за ~0.52с), затем перелёт в ящик/ПУЛ (1..2) + падение деталей. Обе фазы
    // считаются от одного startAt (момента тапа) — иначе на границе 1f ветви
    // перекрывались и startAt сбрасывался, из-за чего винт залипал и не летел.
    // Плюс окно [SWING_MS] после сбора винта — только так пружины наклона успевают
    // отыгать весь взмах (см. комментарий у SWING_MS).
    LaunchedEffect(flying.size, falling.size, swingUntil) {
        if (flying.isEmpty() && falling.isEmpty() && swingUntil == 0L) return@LaunchedEffect
        val until = swingUntil
        val unscrewDur = UNSCREW_MS
        val flyDur = FLY_MS
        while (flying.isNotEmpty() || falling.isNotEmpty() || System.currentTimeMillis() < until) {
            val now = System.currentTimeMillis()
            frameTime = now
            for (i in flying.indices) {
                val f = flying[i]
                val elapsed = now - f.startAt
                flying[i] = if (elapsed < unscrewDur) {
                    val t = (elapsed / unscrewDur.toFloat()).coerceIn(0.02f, 1f)
                    f.copy(progress = t, rotation = -t * 360f)
                } else {
                    val t = ((elapsed - unscrewDur) / flyDur.toFloat()).coerceIn(0f, 1f)
                    f.copy(progress = 1f + t, rotation = -360f)
                }
            }
            if (flying.isNotEmpty() && flying.all { it.progress >= 2f }) {
                flying.clear()
                // винт приземлился — теперь (и не раньше) он появляется в ящике/ПУЛе;
                // игрок видит заполнение, а через паузу полный ящик уедет в settle()
                tick++
                refreshTray()
                settleSeq++
            }
            for (i in falling.indices) {
                val fp = falling[i]
                // ждём, пока последний винт успеет улететь: до beginAt деталь висит на месте
                if (now >= fp.beginAt) {
                    falling[i] = fp.copy(progress = (fp.progress + 0.03f).coerceAtMost(1f))
                }
            }
            falling.removeAll { it.progress >= 1f }
            delay(16)
        }
    }

    // «доигровка» после прилёта: показав полный ящик, через паузу убираем его,
    // на его место встаёт следующий, и только теперь определяется итог уровня
    LaunchedEffect(settleSeq) {
        if (settleSeq == 0) return@LaunchedEffect
        delay(320)
        consume(engine.settle())
        tick++
        refreshTray()
    }

    fun boardToScreen(screw: Screw): Offset {
        val s = min(boardBounds.width / Board.W, boardBounds.height / Board.H)
        if (s <= 0f) return Offset.Zero
        val ox = boardBounds.left + (boardBounds.width - Board.W * s) / 2f
        val oy = boardBounds.top + (boardBounds.height - Board.H * s) / 2f
        // винт мог уехать вместе с наклоном детали — лететь надо от того места,
        // где игрок его реально видит
        val p = tilts.apply(screw.pieceId, Offset(screw.x, screw.y))
        return Offset(ox + p.x * s, oy + p.y * s)
    }

    fun boxToScreen(index: Int, socket: Int? = null): Offset {
        boxCells.getOrNull(index)?.takeIf { it != Rect.Zero }?.let { cell ->
            // если указано гнездо — летим ровно в него, иначе в центр ящика
            return if (socket != null) toolboxSocketCenter(cell, socket) else Offset(cell.center.x, cell.center.y)
        }
        // запасной расчёт: ячейки ряда делят ящики И плитки «Открыть ящик»
        val n = (engine.activeBoxes.size + engine.closedTiles).coerceAtLeast(1)
        val cell = boxRowBounds.width / n
        val i = index.coerceIn(0, n - 1)
        return Offset(boxRowBounds.left + cell * (i + 0.5f), boxRowBounds.center.y)
    }

    fun holdToScreen(index: Int): Offset {
        val n = engine.holdCapacity.coerceAtLeast(1)
        val cell = holdRowBounds.width / n
        val i = index.coerceIn(0, n - 1)
        return Offset(holdRowBounds.left + cell * (i + 0.5f), holdRowBounds.center.y)
    }

    fun handleTap(screwId: Int) {
        val screw = engine.screws.first { it.id == screwId }
        // винт уже в анимации — повторный тап не запускает второй такой же полёт
        if (flying.any { it.screwId == screwId }) return
        if (!engine.canTap(screw)) {
            feedback.wrong(player.sound)
            // тап невозможен: если это тупик (ПУЛ полон, класть некуда) — поражение сразу
            consume(engine.checkStuck())
            return
        }
        val events = engine.tapScrew(screwId)
        val collected = events.filterIsInstance<GameEngine.Event.ScrewCollected>().firstOrNull()
        if (collected != null) {
            // деталь повисла на оставшихся винтах — открываем окно кадров, чтобы
            // маятник успел покачаться (иначе доска не перерисовывается и она
            // замирает на середине взмаха)
            swingUntil = System.currentTimeMillis() + SWING_MS
            val to = when (val d = collected.destination) {
                // винт летит в конкретное гнездо своего ящика (последнее занятое)
                is GameEngine.Destination.BoxSlot -> {
                    val filled = engine.activeBoxes.getOrNull(d.index)?.count ?: 0
                    boxToScreen(d.index, (filled - 1).coerceAtLeast(0))
                }
                is GameEngine.Destination.HoldSlot -> holdToScreen(d.index)
            }
            flying.add(
                FlyingScrew(
                    screwId = screwId,
                    pieceId = screw.pieceId,
                    color = screw.color.hex,
                    from = boardToScreen(screw),
                    to = to,
                    progress = 0f,
                    startAt = System.currentTimeMillis(),
                ),
            )
        }
        tick++
        if (player.vibration) feedback.vibrate(true, 18)
        consume(events)
    }

    fun applyBonus(type: PowerUp, free: Boolean) {
        val price = Economy.priceOf(type)
        if (!free && !player.spendCoins(price)) {
            toast = notEnough
            return
        }
        when (type) {
            PowerUp.DRILL -> engine.drill()
            PowerUp.MELT -> {
                val melted = engine.melt()
                if (melted != null) falling.add(FallingPiece(melted, 0f))
            }
            PowerUp.SEAL -> engine.seal()
            PowerUp.BOX -> when {
                !engine.canOpenBox() -> toast = noBoxes
                else -> engine.openBoxTile()
            }
        }
        tick++
        refreshTray()
        feedback.coin(player.sound)
        bonus = null
    }

    /**
     * Тап по плашке бонуса (или плитке «Открыть ящик»): сначала расходуем
     * купленный в магазине экземпляр — он применяется сразу и без видео;
     * если инвентарь пуст — открываем окно покупки.
     */
    fun pickBonus(type: PowerUp) {
        if (type == PowerUp.BOX && !engine.canOpenBox()) {
            toast = noBoxes
            return
        }
        if (player.spendBonus(type)) applyBonus(type, free = true) else bonus = type
    }

    // только живые детали: упавшие не рисуем (их анимирует falling)
    val livePieces = remember(tick, levelNumber) { engine.level.pieces.filter { engine.isLive(it) } }

    // доля оставшегося окна «докачки» (1..0): читает frameTime, поэтому обновление
    // происходит каждый кадр тикера. На ней гаснет амплитуда покачивания, чтобы в
    // конце деталь осталась ровно на угле покоя, а не в случайной фазе качели
    val swingLeft = if (swingUntil == 0L) 0f
    else ((swingUntil - frameTime) / SWING_MS.toFloat()).coerceIn(0f, 1f)

    // винты в фазе выкручивания (на доске, шлиц крутится ПРОТИВ часовой ~1 оборот) —
    // берём координаты винта прямо из движка (надёжнее обратного пересчёта из экрана)
    val unscrewing = flying.filter { it.progress < 1f }.map { f ->
        val sc = engine.screws.first { s -> s.id == f.screwId }
        UnscrewingScrew(f.screwId, sc.pieceId, sc.x, sc.y, f.color, f.rotation)
    }
    // БЕЛЫЕ кружки-отверстия: остаются за каждым выкрученным винтом, пока жива его
    // деталь; исчезают только вместе с падением фигуры (дырка = освободившееся место).
    // Падающая деталь уже не «живая», но ещё висит в ожидании улёта последнего винта —
    // её отверстия рисуем в слое падения, поэтому они нужны и для неё
    val fallingIds = falling.map { it.piece.id }
    val holes = engine.screws
        .filter { s ->
            engine.isCollected(s.id) &&
                (engine.isLive(engine.pieceOf(s)) || s.pieceId in fallingIds)
        }
        .map { ScrewHole(it.pieceId, it.x, it.y) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.skyTop, Palette.boardBg))),
    ) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            // верхняя панель
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatPill(player.coins, { CoinIcon(Modifier.size(34.dp)) }, onPlus = { showFreeCoins = true }, showBadge = true, modifier = Modifier.width(120.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        L("level", levelNumber),
                        color = Color(0xFF2A3B63),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF3A4A73).copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                // пауза — как все кнопки: тёмная нижняя кромка-«тень»
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.panelBlueDark),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .offset(y = (-3).dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Palette.panelBlue)
                            .clickable { click(); showPause = true },
                        contentAlignment = Alignment.Center,
                    ) { PauseIcon(Modifier.size(30.dp)) }
                }
            }

            // лоток: ящики-заказы + плитки «Открыть ящик», ниже — склад-кружки
            TrayRow(
                trayBoxes, trayTiles, trayHold,
                onBoxRowPlaced = { boxRowBounds = it },
                onBoxPlaced = { i, r ->
                    while (boxCells.size <= i) boxCells.add(Rect.Zero)
                    boxCells[i] = r
                },
                onHoldRowPlaced = { holdRowBounds = it },
                onOpenTile = { pickBonus(PowerUp.BOX) },
            )

            // доска
            BoardCanvas(
                pieces = livePieces,
                screws = engine.screws,
                isCollected = { id -> tick; engine.isCollected(id) },
                isAccessible = { s -> tick; engine.isScrewAccessible(s) },
                onScrewTap = { handleTap(it) },
                tilts = tilts,
                swayAmp = swingLeft,
                falling = falling.toList(),
                unscrewing = unscrewing,
                holes = holes,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { boardBounds = it.boundsInRoot() },
            )

            // купленный в магазине бонус расходуется из инвентаря сразу и без видео;
            // если инвентарь пуст — обычное окно покупки (монеты или rewarded)
            PowerUpBar(
                owned = { player.bonusCount(it) },
                onPick = { pickBonus(it) },
            )
            // баннер в потоке, а не overlay: раньше перекрывал плашки бонусов
            MyTargetBanner()
        }

        // летящие шурупы — полноэкранный overlay: дугой из доски в конкретное гнездо
        // (фаза выкручивания рисуется на самой доске — в overlay только перелёт)
        if (flying.any { it.progress >= 1f }) {
            val bs = min(boardBounds.width / Board.W, boardBounds.height / Board.H)
            val r = if (bs > 0f) 62f * bs else 20f
            Canvas(Modifier.fillMaxSize()) {
                flying.filter { it.progress >= 1f }.forEach { f ->
                    val t = (f.progress - 1f).coerceIn(0f, 1f)
                    val pos = Offset(
                        f.from.x + (f.to.x - f.from.x) * t,
                        f.from.y + (f.to.y - f.from.y) * t - sin(Math.PI * t).toFloat() * size.height * 0.07f,
                    )
                    drawScrewAt(pos, Color(android.graphics.Color.parseColor(f.color)), r, rotation = f.rotation)
                }
            }
        }

        if (toast != null) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xE6000000))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) { Text(toast!!, color = Color.White, fontWeight = FontWeight.Bold) }
            LaunchedEffect(toast) { if (toast != null) { delay(1600); toast = null } }
        }
    }

    // ---- оверлеи -------------------------------------------------------

    if (showPause) {
        Dialog(onDismissRequest = { showPause = false }) {
            GameDialog(L("settings"), onClose = { showPause = false }) {
                SettingsBody(player)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CartoonButton(L("to_home"), onExit, base = Palette.blue, modifier = Modifier.weight(1f))
                    CartoonButton(L("retry"), { onNext(levelNumber) }, base = Palette.yellow, edge = Palette.yellowDark, textColor = Color(0xFF5A4200), modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (showWin) {
        WinDialog(
            reward = Economy.WIN_COINS,
            doubled = winDoubled,
            onDouble = { showReward { player.addCoins(Economy.WIN_COINS); winDoubled = true } },
            onCollect = { showWin = false; onNext(levelNumber + 1) },
            onHome = { showWin = false; onExit() },
        )
    }

    if (showLose) {
        LoseDialog(
            onHome = { showLose = false; onExit() },
            onRetry = { onNext(levelNumber) },
            onContinue = {
                // видео с наградой: возвращаем потраченную жизнь и перезаводим уровень
                showReward { player.addLives(1); onNext(levelNumber) }
            },
        )
    }

    if (showRevive) {
        ReviveDialog(
            coins = player.coins,
            onReviveCoins = {
                if (player.spendCoins(Economy.PRICE_REVIVE)) {
                    player.markRevived(levelNumber)
                    showRevive = false
                } else toast = notEnough
            },
            onReviveFree = {
                showReward {
                    player.markRevived(levelNumber)
                    showRevive = false
                }
            },
            onDecline = {
                showRevive = false
                showLose = true
                notifyLevelFinished()
            },
        )
    }

    bonus?.let { type ->
        BonusDialog(
            type = type,
            coins = player.coins,
            onBuy = { applyBonus(type, free = false) },
            onFree = { showReward { applyBonus(type, free = true) } },
            onClose = { bonus = null },
        )
    }

    if (showFreeCoins) {
        FreeCoinsDialog(player) { showFreeCoins = false }
    }
}

@Composable
private fun TrayRow(
    boxes: List<Pair<ScrewColor, Int>>,
    tiles: Int,
    holdColors: List<ScrewColor?>,
    onBoxRowPlaced: (Rect) -> Unit,
    onBoxPlaced: (Int, Rect) -> Unit,
    onHoldRowPlaced: (Rect) -> Unit,
    onOpenTile: () -> Unit,
) {
    val holdRender: List<Color?> = holdColors.map { sc ->
        sc?.hex?.let { Color(android.graphics.Color.parseColor(it)) }
    }
    val click = LocalClickSound.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .onGloballyPositioned { onBoxRowPlaced(it.boundsInRoot()) },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Ящики НЕ растягиваются на всю ширину ряда: раньше weight(1f) давал
            // эффект «остался один ящик — он огромен». Фиксируем максимум: при
            // 2-3 ящиках они упираются в ceiling и ряд не «дышит» при смене числа.
            val boxMaxW = 96.dp
            boxes.forEachIndexed { index, (color, count) ->
                // обводка ряда (цель полёта винта) — на неподвижном контейнере,
                // иначе анимация смещения сбила бы координаты гнезда
                Box(
                    Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = boxMaxW)
                        .fillMaxHeight()
                        .onGloballyPositioned { onBoxPlaced(index, it.boundsInRoot()) },
                ) {
                    BoxSwapSlot(color, count, Modifier.fillMaxSize())
                }
            }
            repeat(tiles) {
                // плитка «Открыть ящик» — с тёмной нижней кромкой (объём как у кнопок)
                Box(
                    Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = boxMaxW)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Palette.panelBlueDark),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .offset(y = (-3).dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Palette.panelBlue)
                            .clickable { click(); onOpenTile() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            L("open_box").replace(" ", "\n"),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        HoldRow(
            holdRender,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .onGloballyPositioned { onHoldRowPlaced(it.boundsInRoot()) },
        )
    }
}

/**
 * Ящик в ряду заказов. Когда полный ящик уезжает, а на его место встаёт новый
 * (цвет сменился), старый ещё раз рисуется «призраком» и уползает вверх,
 * а новый въезжает сверху в свою ячейку.
 */
@Composable
private fun BoxSwapSlot(color: ScrewColor, filled: Int, modifier: Modifier = Modifier) {
    val capacity = GameEngine.BOX_CAPACITY
    // цвет ДО смены помним специально: remember(color) даёт призрака ровно в момент смены
    var previous by remember { mutableStateOf(color) }
    val ghost = remember(color) { previous.takeIf { it != color } }
    val slide = remember { Animatable(1f) }
    LaunchedEffect(color, ghost) {
        previous = color
        if (ghost != null) {
            slide.snapTo(0f)
            slide.animateTo(1f, tween(BOX_SWAP_MS))
        }
    }
    val t = if (ghost != null) slide.value else 1f
    Box(modifier) {
        if (ghost != null && t < 1f) {
            ToolboxWidget(
                color = ghost,
                filled = capacity,
                capacity = capacity,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = -size.height * t
                        alpha = 1f - t
                    },
            )
        }
        ToolboxWidget(
            color = color,
            filled = filled,
            capacity = maxOf(filled, capacity),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // новый ящик приезжает следом за уезжающим — сверху вниз в ячейку
                    if (t < 1f) translationY = -size.height * (1f - t)
                },
        )
    }
}

@Composable
private fun PowerUpBar(owned: (PowerUp) -> Int, onPick: (PowerUp) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PowerUpTile(Palette.blue, { DrillIcon(Modifier.size(60.dp)) }, L("drill"), owned(PowerUp.DRILL), { onPick(PowerUp.DRILL) }, Modifier.weight(1f))
        PowerUpTile(Palette.blue, { FlaskIcon(Modifier.size(60.dp)) }, L("melt"), owned(PowerUp.MELT), { onPick(PowerUp.MELT) }, Modifier.weight(1f))
        PowerUpTile(Palette.blue, { HammerIcon(Modifier.size(60.dp)) }, L("seal"), owned(PowerUp.SEAL), { onPick(PowerUp.SEAL) }, Modifier.weight(1f))
    }
}

@Composable
private fun PowerUpTile(
    color: Color,
    icon: @Composable () -> Unit,
    label: String,
    count: Int,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val click = LocalClickSound.current
    // объём: тёмная нижняя кромка под плашкой
    Box(
        modifier
            .height(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color.darkenForEdge()),
    ) {
    Box(
        Modifier
            .fillMaxSize()
            .offset(y = (-3).dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(color.lighten(0.2f), color)))
            .clickable { click(); onPlus() },
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            icon()
            Spacer(Modifier.height(2.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
        // счётчик купленных бонусов: тап применит его сразу, без диалога
        if (count > 0) {
            Text(
                "× $count",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Palette.green)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

/** Тёмный оттенок цвета плашки — для нижней кромки-«тени» (darkenSelf — из BoardCanvas). */
private fun Color.darkenForEdge(): Color = darkenSelf()
