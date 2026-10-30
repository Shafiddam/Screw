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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fidapp.screw.game.Board
import com.fidapp.screw.game.Economy
import com.fidapp.screw.game.GameEngine
import com.fidapp.screw.game.Levels
import com.fidapp.screw.game.PowerUp
import com.fidapp.screw.game.PlayerStore
import com.fidapp.screw.game.Screw
import com.fidapp.screw.game.ScrewColor
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.sin

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
    val flying = remember { mutableStateListOf<FlyingScrew>() }
    val falling = remember { mutableStateListOf<FallingPiece>() }
    var frameTime by remember { mutableLongStateOf(0L) }
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
    var bonus by remember { mutableStateOf<PowerUp?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    // строки вычисляем здесь (@Composable), чтобы использовать внутри не-composable колбэков
    val notEnough = L("not_enough")
    val adStub = L("ad_stub")
    val noBoxes = L("box_none")

    // тикер анимаций: полёт шурупов + падение деталей
    LaunchedEffect(flying.size, falling.size) {
        if (flying.isEmpty() && falling.isEmpty()) return@LaunchedEffect
        val start = System.currentTimeMillis()
        val dur = 400L
        while (flying.isNotEmpty() || falling.isNotEmpty()) {
            val now = System.currentTimeMillis()
            frameTime = now
            val p = ((now - start) / dur.toFloat()).coerceIn(0f, 1f)
            for (i in flying.indices) flying[i] = flying[i].copy(progress = p)
            if (p >= 1f) flying.clear()
            for (i in falling.indices) {
                falling[i] = falling[i].let { it.copy(progress = (it.progress + 0.04f).coerceAtMost(1f)) }
            }
            falling.removeAll { it.progress >= 1f }
            delay(16)
        }
    }

    fun boardToScreen(screw: Screw): Offset {
        val s = min(boardBounds.width / Board.W, boardBounds.height / Board.H)
        if (s <= 0f) return Offset.Zero
        val ox = boardBounds.left + (boardBounds.width - Board.W * s) / 2f
        val oy = boardBounds.top + (boardBounds.height - Board.H * s) / 2f
        return Offset(ox + screw.x * s, oy + screw.y * s)
    }

    fun boxToScreen(index: Int): Offset {
        boxCells.getOrNull(index)?.takeIf { it != Rect.Zero }?.let {
            return Offset(it.center.x, it.center.y)
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
        if (!engine.canTap(screw)) {
            feedback.tap(player.sound)
            return
        }
        val events = engine.tapScrew(screwId)
        val collected = events.filterIsInstance<GameEngine.Event.ScrewCollected>().firstOrNull()
        if (collected != null) {
            val to = when (val d = collected.destination) {
                is GameEngine.Destination.BoxSlot -> boxToScreen(d.index)
                is GameEngine.Destination.HoldSlot -> holdToScreen(d.index)
            }
            flying.add(FlyingScrew(screwId, screw.color.hex, boardToScreen(screw), to, 0f))
        }
        tick++
        feedback.tap(player.sound)
        if (player.vibration) feedback.vibrate(true, 18)
        events.forEach { e ->
            when (e) {
                is GameEngine.Event.PieceFell -> falling.add(FallingPiece(e.piece, 0f))
                is GameEngine.Event.Win -> {
                    feedback.success(player.sound)
                    player.onLevelWon(levelNumber)
                    showWin = true
                }
                is GameEngine.Event.Lose -> {
                    feedback.fail(player.sound)
                    player.spendLife()
                    if (player.canRevive(levelNumber)) showRevive = true else showLose = true
                }
                else -> {}
            }
        }
    }

    fun applyBonus(type: PowerUp, free: Boolean) {
        val price = when (type) {
            PowerUp.DRILL -> Economy.PRICE_DRILL
            PowerUp.MELT -> Economy.PRICE_MELT
            PowerUp.SEAL -> Economy.PRICE_SEAL
            PowerUp.BOX -> Economy.PRICE_BOX
        }
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
        feedback.coin(player.sound)
        bonus = null
    }

    // только живые детали: упавшие не рисуем (их анимирует falling)
    val livePieces = remember(tick, levelNumber) { engine.level.pieces.filter { engine.isLive(it) } }

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
                StatPill(player.coins, { CoinIcon(Modifier.size(34.dp)) }, onPlus = { bonus = PowerUp.BOX }, showBadge = true, modifier = Modifier.width(120.dp))
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
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.panelBlue)
                        .clickable { showPause = true },
                    contentAlignment = Alignment.Center,
                ) { PauseIcon(Modifier.size(30.dp)) }
            }

            // лоток: ящики-заказы + плитки «Открыть ящик», ниже — склад-кружки
            TrayRow(
                engine, tick,
                onBoxRowPlaced = { boxRowBounds = it },
                onBoxPlaced = { i, r ->
                    while (boxCells.size <= i) boxCells.add(Rect.Zero)
                    boxCells[i] = r
                },
                onHoldRowPlaced = { holdRowBounds = it },
                onOpenTile = { bonus = PowerUp.BOX },
            )

            // доска
            BoardCanvas(
                pieces = livePieces,
                screws = engine.screws,
                isCollected = { id -> tick; engine.isCollected(id) },
                isAccessible = { s -> tick; engine.isScrewAccessible(s) },
                onScrewTap = { handleTap(it) },
                falling = falling.toList(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { boardBounds = it.boundsInRoot() },
            )

            PowerUpBar(onPick = { bonus = it })
            // баннер в потоке, а не overlay: раньше перекрывал плашки бонусов
            FakeAdBanner()
        }

        // летящие шурупы — полноэкранный overlay: дугой из доски в конкретное гнездо
        if (flying.isNotEmpty()) {
            val bs = min(boardBounds.width / Board.W, boardBounds.height / Board.H)
            val r = if (bs > 0f) 62f * bs else 20f
            Canvas(Modifier.fillMaxSize()) {
                flying.forEach { f ->
                    val t = f.progress
                    val pos = Offset(
                        f.from.x + (f.to.x - f.from.x) * t,
                        f.from.y + (f.to.y - f.from.y) * t - sin(Math.PI * t).toFloat() * size.height * 0.07f,
                    )
                    drawScrewAt(pos, Color(android.graphics.Color.parseColor(f.color)), r)
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
            coins = Economy.WIN_COINS,
            onCollect = { showWin = false; onNext(levelNumber + 1) },
            onHome = { showWin = false; onExit() },
        )
    }

    if (showLose) {
        LoseDialog(
            onHome = { showLose = false; onExit() },
            onRetry = { onNext(levelNumber) },
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
                // заглушка rewarded-видео: сразу воскрешаем
                player.markRevived(levelNumber)
                showRevive = false
                toast = adStub
            },
            onDecline = { showRevive = false; showLose = true },
        )
    }

    bonus?.let { type ->
        BonusDialog(
            type = type,
            coins = player.coins,
            onBuy = { applyBonus(type, free = false) },
            onFree = { applyBonus(type, free = true) },
            onClose = { bonus = null },
        )
    }
}

@Composable
private fun TrayRow(
    engine: GameEngine,
    tick: Int,
    onBoxRowPlaced: (Rect) -> Unit,
    onBoxPlaced: (Int, Rect) -> Unit,
    onHoldRowPlaced: (Rect) -> Unit,
    onOpenTile: () -> Unit,
) {
    // сам факт изменения tick в параметре гарантирует перерисовку ряда после каждого тапа
    val boxes = engine.activeBoxes.map { it.color to it.count }
    val tiles = engine.closedTiles
    val holdCells = engine.holdCapacity
    val holdColors: List<Color?> = (0 until holdCells).map { i ->
        engine.hold.getOrNull(i)?.color?.hex?.let { Color(android.graphics.Color.parseColor(it)) }
    }
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
            boxes.forEachIndexed { index, (color, count) ->
                ToolboxWidget(
                    color = color,
                    filled = count,
                    capacity = maxOf(count, GameEngine.BOX_CAPACITY),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { onBoxPlaced(index, it.boundsInRoot()) },
                )
            }
            repeat(tiles) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Palette.panelBlue)
                        .clickable { onOpenTile() },
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
        Spacer(Modifier.height(6.dp))
        HoldRow(
            holdColors,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .onGloballyPositioned { onHoldRowPlaced(it.boundsInRoot()) },
        )
    }
}

@Composable
private fun PowerUpBar(onPick: (PowerUp) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PowerUpTile(Palette.blue, { DrillIcon(Modifier.size(40.dp)) }, L("drill"), { onPick(PowerUp.DRILL) }, Modifier.weight(1f))
        PowerUpTile(Palette.blue, { FlaskIcon(Modifier.size(40.dp)) }, L("melt"), { onPick(PowerUp.MELT) }, Modifier.weight(1f))
        PowerUpTile(Palette.blue, { HammerIcon(Modifier.size(40.dp)) }, L("seal"), { onPick(PowerUp.SEAL) }, Modifier.weight(1f))
    }
}

@Composable
private fun PowerUpTile(color: Color, icon: @Composable () -> Unit, label: String, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(color.lighten(0.2f), color)))
            .clickable { onPlus() },
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            icon()
            Spacer(Modifier.height(2.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) { Text("+", color = color, fontWeight = FontWeight.Black, fontSize = 16.sp) }
    }
}
