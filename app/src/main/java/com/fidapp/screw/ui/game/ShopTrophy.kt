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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidapp.screw.game.Economy
import com.fidapp.screw.game.PlayerStore

/** Магазин: только бонусы за монеты. Всё за рубли (паки монет, промо) убрано — демо без IAP. */
@Composable
fun ShopScreen(player: PlayerStore) {
    Box(Modifier.fillMaxSize().background(Palette.skyGradient)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            Text(L("shop"), color = Color(0xFF2A3B63), fontWeight = FontWeight.Black, fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(L("shop_hint"), color = Color(0xFF5A6B8C), fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Text(L("bonus_title"), fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF2A3B63))
                Spacer(Modifier.height(8.dp))
                BonusRow({ DrillIcon(Modifier.size(36.dp)) }, L("drill"), "${Economy.PRICE_DRILL}")
                BonusRow({ FlaskIcon(Modifier.size(36.dp)) }, L("melt"), "${Economy.PRICE_MELT}")
                BonusRow({ HammerIcon(Modifier.size(36.dp)) }, L("seal"), "${Economy.PRICE_SEAL}")
                BonusRow({ BoxIcon(Modifier.size(36.dp)) }, L("open_box"), "${Economy.PRICE_BOX}")
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun BonusRow(icon: @Composable () -> Unit, name: String, price: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp)) { icon() }
        Spacer(Modifier.width(10.dp))
        Text(name, fontWeight = FontWeight.Bold, color = Color(0xFF2A3B63), modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp)) { CoinIcon() }
            Text(" $price", fontWeight = FontWeight.Bold, color = Color(0xFF7A4A12))
        }
    }
}

/** Трофеи: прогресс и достижения. */
@Composable
fun TrophyScreen(player: PlayerStore) {
    Box(Modifier.fillMaxSize().background(Palette.skyGradient)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(L("trophy"), color = Color(0xFF2A3B63), fontWeight = FontWeight.Black, fontSize = 28.sp)
            Spacer(Modifier.height(20.dp))
            Box(Modifier.size(96.dp)) { TrophyIcon() }
            Spacer(Modifier.height(16.dp))
            StatCard(L("best"), "${player.bestLevel - 1}")
            Spacer(Modifier.height(10.dp))
            StatCard(L("total_claimed"), "${player.piggyClaimed}")
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(5) { Box(Modifier.size(40.dp)) { if (it < ((player.bestLevel - 1) / 3).coerceAtMost(5)) StarIcon() } }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontWeight = FontWeight.Bold, color = Color(0xFF2A3B63), modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Black, color = Palette.orangeDark, fontSize = 22.sp)
    }
}
