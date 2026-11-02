package com.fidapp.screw.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fidapp.screw.game.PlayerStore
import kotlinx.coroutines.delay

enum class Screen { MAP, SHOP, TROPHY }

/** Корень приложения: навигация, тикер жизней, локальный провайдер стора. */
@Composable
fun ScrewGame(player: PlayerStore) {
    val feedback = rememberFeedback()
    val clickSound = { feedback.tap(player.sound) }
    CompositionLocalProvider(
        LocalPlayerStore provides player,
        LocalClickSound provides clickSound,
    ) {
        var screen by remember { mutableStateOf(Screen.MAP) }
        var gameLevel by remember { mutableStateOf<Int?>(null) }
        // «заново» на том же уровне: gameLevel не меняется, поэтому крутим ключ,
        // чтобы GameScreen пересоздался и движок сбросился
        var gameNonce by remember { mutableStateOf(0) }
        var noLives by remember { mutableStateOf(false) }
        var showExitAsk by remember { mutableStateOf(false) }
        // «Настройки» — теперь вкладка нижнего меню (правая); сам тулбар живёт
        // на ScrewGame, поэтому диалог тоже поднян сюда: работает с MAP/SHOP/TROPHY
        var showSettings by remember { mutableStateOf(false) }

        DisposableEffect(Unit) { onDispose { feedback.release() } }

        // фоновая музыка: включаем по переключателю и на возврате в фор-план,
        // глушим на паузе/выходе из активности
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, player.music) {
            val observer = LifecycleEventObserver { _, event ->
                when {
                    event == Lifecycle.Event.ON_RESUME -> feedback.setMusic(player.music)
                    event == Lifecycle.Event.ON_PAUSE -> feedback.setMusic(false)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                feedback.setMusic(false)
            }
        }

        // назад: в игре — на карту; с вкладок магазин/трофеи — сначала на карту
        // (иначе возврат из внешних ссылок вроде «Каталога» и один «Назад»
        // вели сразу к алерту выхода); на карте — алерт «выйти?», алерт — закрыть.
        // «Призрачный бэк»: когда внешнее приложение (браузер/RuStore с каталогом)
        // закрывают системным «Назад», событие иногда доходит уже до нашего
        // только что возобновлённого окна — игрок из каталога мгновенно получает
        // алерт выхода (или выход). Поэтому первые 900 мс после ON_RESUME назад глушим.
        var resumedAt by remember { mutableLongStateOf(0L) }
        DisposableEffect(lifecycleOwner) {
            val guard = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) resumedAt = System.currentTimeMillis()
            }
            lifecycleOwner.lifecycle.addObserver(guard)
            onDispose { lifecycleOwner.lifecycle.removeObserver(guard) }
        }
        BackHandler {
            if (System.currentTimeMillis() - resumedAt < 900L) return@BackHandler
            when {
                showExitAsk -> showExitAsk = false
                noLives -> noLives = false
                showSettings -> showSettings = false
                gameLevel != null -> gameLevel = null
                screen != Screen.MAP -> screen = Screen.MAP
                else -> showExitAsk = true
            }
        }

        // регенерация жизней по времени
        LaunchedEffect(Unit) {
            while (true) {
                player.tickLives()
                delay(1000)
            }
        }

        Box(Modifier.fillMaxSize()) {
            when (screen) {
                Screen.MAP -> MapScreen(player, onPlay = { level ->
                    if (player.lives > 0) gameLevel = level else noLives = true
                })
                Screen.SHOP -> ShopScreen(player, onClose = { screen = Screen.MAP })
                Screen.TROPHY -> TrophyScreen(player, onClose = { screen = Screen.MAP })
            }

            gameLevel?.let { level ->
                val click = LocalClickSound.current
                key(gameNonce) {
                    GameScreen(
                        levelNumber = level,
                        player = player,
                        feedback = feedback,
                        onExit = { click(); gameLevel = null },
                        onNext = { gameLevel = it; gameNonce++ },
                    )
                }
            }

            if (gameLevel == null) {
                // панель прижата вниз и за системной навигацией: раньше она
                // рисовалась вверху Box и перекрывала шестерёнку/стат-бар
                Box(Modifier.align(Alignment.BottomCenter)) {
                    BottomNav(screen, showSettings, onSelect = { screen = it }, onSettings = { showSettings = !showSettings })
                }
            }
        }

        if (noLives) {
            LivesDialog(player) { noLives = false }
        }

        if (showSettings) {
            SettingsDialog(player) { showSettings = false }
        }

        if (showExitAsk) {
            val ctx = LocalContext.current
            Dialog(onDismissRequest = { showExitAsk = false }) {
                GameDialog(L("exit_title"), onClose = { showExitAsk = false }) {
                    Text(L("exit_q"), color = Color(0xFF2A3B63), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        CartoonButton(L("no"), { showExitAsk = false }, base = Palette.blue, modifier = Modifier.weight(1f))
                        CartoonButton(
                            L("yes"),
                            { (ctx as? android.app.Activity)?.finish() },
                            base = Palette.red,
                            edge = Palette.redDark,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNav(
    current: Screen,
    settingsSelected: Boolean,
    onSelect: (Screen) -> Unit,
    onSettings: () -> Unit,
) {
    // Все иконки нижней панели — ЖЁЛТЫЕ (пользователь: «закрась шестерёнку
    // и вообще все иконки нижней панели жёлтым — как у трофеев»). Тот же
    // тон, что у TrophyIcon по умолчанию.
    val tabYellow = Color(0xFFF6B500)
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF2E7BD6))
            .navigationBarsPadding()
            .height(64.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavTab(Screen.SHOP, current, { ShopIcon(Modifier.size(26.dp), color = tabYellow) }, L("shop"), Modifier.weight(1f)) { onSelect(it) }
        NavTab(Screen.MAP, current, { HomeIcon(Modifier.size(24.dp), tabYellow) }, L("main"), Modifier.weight(1f)) { onSelect(it) }
        NavTab(Screen.TROPHY, current, { TrophyIcon(Modifier.size(26.dp), color = tabYellow) }, L("trophy"), Modifier.weight(1f)) { onSelect(it) }
        // «Настройки» — правая вкладка: сама по себе диалог, а не экран,
        // поэтому «selected» крутим по состоянию самого диалога
        SettingsTab(
            selected = settingsSelected,
            icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = tabYellow, modifier = Modifier.size(24.dp)) },
            label = L("settings"),
            modifier = Modifier.weight(1f),
            onClick = onSettings,
        )
    }
}

// вкладки фиксированного размера: раньше выбранная расширялась и меню «прыгало»
@Composable
private fun NavTab(
    tab: Screen,
    current: Screen,
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onSelect: (Screen) -> Unit,
) {
    val click = LocalClickSound.current
    val selected = tab == current
    Box(
        modifier
            .padding(horizontal = 2.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFF6FB0F5) else Color.Transparent)
            .clickable { click(); onSelect(tab) },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(26.dp)) { icon() }
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
        }
    }
}

/** Вкладка-«триггер» для SettingsDialog — с тем же размером/скруглением, что и
 *  обычные NavTab, но без смены экрана (это же диалог, а не экран). */
@Composable
private fun SettingsTab(
    selected: Boolean,
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val click = LocalClickSound.current
    Box(
        modifier
            .padding(horizontal = 2.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFF6FB0F5) else Color.Transparent)
            .clickable { click(); onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(26.dp)) { icon() }
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
        }
    }
}
