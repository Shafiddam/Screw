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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fidapp.screw.game.Economy
import com.fidapp.screw.game.PlayerStore
import com.fidapp.screw.game.PowerUp

/** Тело настроек (вибрация/звук/музыка/язык) — переиспользуется в паузе и на карте. */
@Composable
fun SettingsBody(player: PlayerStore) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        SwitchRow({ /* phone */ Text("📳", fontSize = 26.sp) }, L("vibration"), player.vibration) { player.vibration = it }
        SwitchRow({ Text("🔊", fontSize = 26.sp) }, L("sound"), player.sound) { player.sound = it }
        SwitchRow({ Text("🎵", fontSize = 26.sp) }, L("music"), player.music) { player.music = it }
    }
}

@Composable
fun SettingsDialog(player: PlayerStore, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        GameDialog(L("settings"), onClose) {
            SettingsBody(player)
        }
    }
}

/** Окно покупки бонуса: цена в монетах или «Бес» (заглушка rewarded). */
@Composable
fun BonusDialog(type: PowerUp, coins: Int, onBuy: () -> Unit, onFree: () -> Unit, onClose: () -> Unit) {
    val (title, desc, price) = when (type) {
        PowerUp.DRILL -> Triple(L("drill"), L("drill_desc"), Economy.PRICE_DRILL)
        PowerUp.MELT -> Triple(L("melt"), L("melt_desc"), Economy.PRICE_MELT)
        PowerUp.SEAL -> Triple(L("seal"), L("seal_desc"), Economy.PRICE_SEAL)
        PowerUp.BOX -> Triple(L("open_box"), L("box_desc"), Economy.PRICE_BOX)
    }
    Dialog(onDismissRequest = onClose) {
        GameDialog(title, onClose) {
            Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                Starburst(Modifier.fillMaxWidth())
                Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) { BonusIcon(type) }
            }
            Spacer(Modifier.height(6.dp))
            Text(desc, color = Color(0xFF7A4A12), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CartoonButton(
                    text = "$price",
                    onClick = onBuy,
                    base = Palette.blue,
                    icon = { CoinIcon(Modifier.size(24.dp)) },
                    enabled = coins >= price,
                    modifier = Modifier.weight(1f),
                )
                CartoonButton(
                    text = L("free"),
                    onClick = onFree,
                    base = Palette.yellow,
                    edge = Palette.yellowDark,
                    textColor = Color(0xFF5A4200),
                    icon = { VideoIcon(Modifier.size(24.dp), Color(0xFF5A4200)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BonusIcon(type: PowerUp) {
    when (type) {
        PowerUp.DRILL -> DrillIcon(Modifier.size(96.dp))
        PowerUp.MELT -> FlaskIcon(Modifier.size(96.dp))
        PowerUp.SEAL -> HammerIcon(Modifier.size(96.dp))
        PowerUp.BOX -> BoxIcon(Modifier.size(96.dp))
    }
}

@Composable
fun WinDialog(coins: Int, onCollect: () -> Unit, onHome: () -> Unit) {
    Dialog(onDismissRequest = onCollect) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("👑", fontSize = 60.sp)
            Banner(L("win"), Palette.red)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(64.dp)) { PigIcon() }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp)) { CoinIcon() }
                    Text("$coins", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CartoonButton(L("to_home"), onHome, base = Palette.green, edge = Palette.greenDark, modifier = Modifier.weight(1f))
                CartoonButton(L("next_level"), onCollect, base = Palette.yellow, edge = Palette.yellowDark, textColor = Color(0xFF5A4200), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun LoseDialog(onHome: () -> Unit, onRetry: () -> Unit) {
    Dialog(onDismissRequest = onHome) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("💔", fontSize = 60.sp)
            Banner(L("lose"), Color(0xFF7B3FF2))
            Spacer(Modifier.height(8.dp))
            Text(L("lose_desc"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CartoonButton(L("to_home"), onHome, base = Palette.blue, modifier = Modifier.weight(1f))
                CartoonButton(L("retry"), onRetry, base = Palette.yellow, edge = Palette.yellowDark, textColor = Color(0xFF5A4200), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ReviveDialog(coins: Int, onReviveCoins: () -> Unit, onReviveFree: () -> Unit, onDecline: () -> Unit) {
    Dialog(onDismissRequest = onDecline) {
        GameDialog(L("revive"), onDecline) {
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                Starburst(Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(80.dp)) { HeartIcon() }
                    Text("+", color = Palette.green, fontWeight = FontWeight.Black, fontSize = 34.sp)
                    Box(Modifier.size(80.dp)) { BoxIcon(dots = 3) }
                }
            }
            Text(L("revive_desc"), color = Color(0xFF7A4A12), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CartoonButton(
                    text = "${Economy.PRICE_REVIVE}",
                    onClick = onReviveCoins,
                    base = Palette.blue,
                    icon = { CoinIcon(Modifier.size(24.dp)) },
                    enabled = coins >= Economy.PRICE_REVIVE,
                    modifier = Modifier.weight(1f),
                )
                CartoonButton(
                    text = L("free"),
                    onClick = onReviveFree,
                    base = Palette.yellow,
                    edge = Palette.yellowDark,
                    textColor = Color(0xFF5A4200),
                    icon = { VideoIcon(Modifier.size(24.dp), Color(0xFF5A4200)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** «Восполнить жизни» — +2 за монеты или +1 бесплатно. */
@Composable
fun LivesDialog(player: PlayerStore, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        GameDialog(L("refill_lives"), onClose) {
            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                HeartIcon(Modifier.fillMaxSize())
                Text("${player.lives}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 34.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(L("next_refill"), color = Color(0xFF7A4A12), fontWeight = FontWeight.Bold)
            Text(countdown(player.nextLifeAt), color = Color(0xFF2A3B63), fontWeight = FontWeight.Black, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CartoonButton(
                    text = "♥ +2",
                    onClick = { if (player.spendCoins(Economy.PRICE_LIVES_2)) player.addLives(2); },
                    base = Palette.blue,
                    icon = { CoinIcon(Modifier.size(22.dp)) },
                    enabled = player.coins >= Economy.PRICE_LIVES_2,
                    modifier = Modifier.weight(1f),
                )
                CartoonButton(
                    text = "♥ +1",
                    onClick = { player.addLives(1); onClose() },
                    base = Palette.yellow,
                    edge = Palette.yellowDark,
                    textColor = Color(0xFF5A4200),
                    icon = { VideoIcon(Modifier.size(22.dp), Color(0xFF5A4200)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun PiggyDialog(player: PlayerStore, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFB57BEA), Color(0xFF9A4FD6))))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CloseIcon(Modifier.size(28.dp).clickable { onClose() })
            }
            Text(L("piggy"), color = Color(0xFFF3D9FF), fontWeight = FontWeight.Black, fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(L("piggy_desc"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                Starburst(Modifier.fillMaxWidth(), outer = Color(0xFFFFF1B8))
                Box(Modifier.size(120.dp)) { PigIcon() }
            }
            Spacer(Modifier.height(12.dp))
            ProgressLine(player.piggy, Economy.PIGGY_GOAL)
            Spacer(Modifier.height(18.dp))
            CartoonButton(
                text = L("claim") + " ${player.piggy}",
                onClick = { player.claimPiggy(); onClose() },
                base = Palette.green,
                edge = Palette.greenDark,
                enabled = player.piggy > 0,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProgressLine(value: Int, goal: Int) {
    val frac = (value.toFloat() / goal).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF5E3A82)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Palette.green),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("$value/$goal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun Banner(text: String, color: Color) {
    Box(
        Modifier
            .width(240.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(color.lighten(0.2f), color))),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp) }
}

private fun countdown(nextAt: Long): String {
    if (nextAt == 0L) return "∞"
    val left = (nextAt - System.currentTimeMillis()).coerceAtLeast(0)
    val m = left / 60000
    val s = (left % 60000) / 1000
    return String.format("%d:%02d", m, s)
}
