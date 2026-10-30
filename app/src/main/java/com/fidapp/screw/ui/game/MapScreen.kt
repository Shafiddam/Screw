package com.fidapp.screw.ui.game

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidapp.screw.game.Economy
import com.fidapp.screw.game.PlayerStore

/** Карта уровней: змейка узлов (как на скриншоте «СЭ»), боковые кнопки, «Играть». */
@Composable
fun MapScreen(
    player: PlayerStore,
    onPlay: (Int) -> Unit,
) {
    var showLives by remember { mutableStateOf(false) }
    var showPiggy by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showMy by remember { mutableStateOf(false) }
    var showAvatars by remember { mutableStateOf(false) }

    val endLevel = player.bestLevel + 4
    val startLevel = (player.bestLevel - 4).coerceAtLeast(1)

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.skyGradient),
    ) {
        Column(Modifier.fillMaxSize()) {
            // верхняя панель
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showMy = true },
                ) { AvatarBox(player.avatar, 44.dp) }
                Spacer(Modifier.width(8.dp))
                StatPill(player.coins, { CoinIcon(Modifier.size(34.dp)) }, onPlus = { showLives = false }, showBadge = true, modifier = Modifier.width(110.dp))
                Spacer(Modifier.width(8.dp))
                LivesPill(player, Modifier.width(120.dp)) { showLives = true }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.panelBlue)
                        .clickable { showSettings = true },
                    contentAlignment = Alignment.Center,
                ) { GearIcon(Modifier.size(28.dp)) }
            }

            Row(Modifier.weight(1f)) {
                // боковой рельс
                Column(
                    Modifier
                        .width(84.dp)
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SideButton({ AdsOffBadge() }, L("noads")) {}
                    SideButton(
                        {
                            Box(Modifier.size(48.dp)) { PigIcon() }
                            Text("${player.piggy}/${Economy.PIGGY_GOAL}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Palette.green).padding(horizontal = 4.dp))
                        },
                        "",
                    ) { showPiggy = true }
                    SideButton({ Box(Modifier.size(48.dp)) { GiftIcon() } }, "23h59m") { onPlay(player.bestLevel) }
                }

                // змейка уровней
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // уровни идут снизу вверх: дальние сверху, текущий и «1» — внизу
                    for (i in 0 until (endLevel - startLevel + 1)) {
                        val number = endLevel - i
                        LevelNode(number, player) { onPlay(number) }
                        if (i < endLevel - startLevel) PathSegment(i % 2 == 0)
                    }
                }
            }

            // «Играть» — выше нижней панели ГЛАВНАЯ/FREE и системных кнопок
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 74.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CartoonButton(
                    text = "🔔 " + L("daily"),
                    onClick = { onPlay(player.bestLevel + 5) },
                    base = Palette.yellow,
                    edge = Palette.yellowDark,
                    textColor = Color(0xFF5A4200),
                    modifier = Modifier.width(220.dp),
                    height = 48.dp,
                    fontSize = 16,
                )
                Spacer(Modifier.height(10.dp))
                CartoonButton(
                    text = L("play"),
                    onClick = { onPlay(player.bestLevel) },
                    base = Palette.green,
                    edge = Palette.greenDark,
                    modifier = Modifier.width(260.dp),
                    height = 64.dp,
                    fontSize = 26,
                )
            }
        }
    }

    if (showLives) LivesDialog(player) { showLives = false }
    if (showPiggy) PiggyDialog(player) { showPiggy = false }
    if (showSettings) SettingsDialog(player) { showSettings = false }
    if (showMy) MyDialog(player, onOpenAvatars = { showAvatars = true }) { showMy = false }
    if (showAvatars) AvatarDialog(player) { showAvatars = false }
}

@Composable
private fun LivesPill(player: PlayerStore, modifier: Modifier = Modifier, onPlus: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Box(modifier = modifier.height(44.dp)) {
        Row(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color(0xFFFDF3D8)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                HeartIcon(Modifier.size(30.dp))
                Text("${player.lives}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Text(
                countdownLabel(player.nextLifeAt),
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = Color(0xFF5A4A2A),
                modifier = Modifier.padding(start = 2.dp),
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(30.dp)
                    .padding(end = 4.dp)
                    .clip(CircleShape)
                    .background(Palette.green)
                    .clickable { onPlus() },
                contentAlignment = Alignment.Center,
            ) { Text("+", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp) }
        }
    }
}

@Composable
private fun countdownLabel(nextAt: Long): String {
    if (nextAt == 0L) return "∞"
    val left = (nextAt - System.currentTimeMillis()).coerceAtLeast(0)
    val m = left / 60000
    val s = (left % 60000) / 1000
    return String.format("%d:%02d", m, s)
}

@Composable
private fun SideButton(icon: @Composable () -> Unit, caption: String, onClick: () -> Unit) {
    Column(
        Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF3A4A73).copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        if (caption.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(caption, color = Color(0xFF2A3B63), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AdsOffBadge() {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(Palette.red))
        Text("ADS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
        Text("🚫", fontSize = 26.sp)
    }
}

@Composable
private fun LevelNode(number: Int, player: PlayerStore, onClick: () -> Unit) {
    val unlocked = player.isUnlocked(number)
    val isCurrent = number == player.bestLevel
    val color = when {
        !unlocked -> Palette.mapLocked
        isCurrent -> Palette.mapCurrent
        else -> Palette.mapDone
    }
    Box(
        Modifier
            .size(58.dp)
            .padding(4.dp),
    ) {
        // «речка»-подложка
        Box(
            Modifier
                .align(Alignment.Center)
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.verticalGradient(listOf(color.lighten(0.25f), color)))
                .clickable(enabled = unlocked) { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        if (!unlocked) {
            Text("🔒", fontSize = 14.sp, modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun PathSegment(goRight: Boolean) {
    Box(
        Modifier
            .width(56.dp)
            .height(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 10.dp, height = 30.dp)
                .background(Palette.mapPath)
                .rotate(if (goRight) 40f else -40f),
        )
    }
}
