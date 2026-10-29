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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.fidapp.screw.game.PlayerStore
import kotlinx.coroutines.delay

enum class Screen { MAP, SHOP, TROPHY }

/** Корень приложения: навигация, тикер жизней, локальный провайдер стора. */
@Composable
fun ScrewGame(player: PlayerStore) {
    CompositionLocalProvider(LocalPlayerStore provides player) {
        var screen by remember { mutableStateOf(Screen.MAP) }
        var gameLevel by remember { mutableStateOf<Int?>(null) }
        var noLives by remember { mutableStateOf(false) }
        var showExitAsk by remember { mutableStateOf(false) }
        val feedback = rememberFeedback()

        DisposableEffect(Unit) { onDispose { feedback.release() } }

        // назад: в игре — на карту, на карте — алерт «выйти?», алерт — закрыть
        BackHandler {
            when {
                showExitAsk -> showExitAsk = false
                noLives -> noLives = false
                gameLevel != null -> gameLevel = null
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
                Screen.SHOP -> ShopScreen(player)
                Screen.TROPHY -> TrophyScreen(player)
            }

            gameLevel?.let { level ->
                GameScreen(
                    levelNumber = level,
                    player = player,
                    feedback = feedback,
                    onExit = { gameLevel = null },
                    onNext = { gameLevel = it },
                )
            }

            if (gameLevel == null) {
                // панель прижата вниз и за системной навигацией: раньше она
                // рисовалась вверху Box и перекрывала шестерёнку/стат-бар
                Box(Modifier.align(Alignment.BottomCenter)) {
                    BottomNav(screen) { screen = it }
                }
            }
        }

        if (noLives) {
            LivesDialog(player) { noLives = false }
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
private fun BottomNav(current: Screen, onSelect: (Screen) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF2E7BD6))
            .navigationBarsPadding()
            .height(64.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavTab(Screen.SHOP, current, { ShopIcon(Modifier.size(28.dp)) }, L("shop")) { onSelect(it) }
        NavTab(Screen.MAP, current, { HomeIcon(Modifier.size(26.dp), Color(0xFFE53935)) }, L("main")) { onSelect(it) }
        NavTab(Screen.TROPHY, current, { TrophyIcon(Modifier.size(28.dp)) }, L("trophy")) { onSelect(it) }
    }
}

// вкладки фиксированного размера: раньше выбранная расширялась и меню «прыгало»
@Composable
private fun NavTab(tab: Screen, current: Screen, icon: @Composable () -> Unit, label: String, onSelect: (Screen) -> Unit) {
    val selected = tab == current
    Box(
        Modifier
            .width(112.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFF6FB0F5) else Color.Transparent)
            .clickable { onSelect(tab) },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(28.dp)) { icon() }
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}
