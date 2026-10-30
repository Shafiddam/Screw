package com.fidapp.screw.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fidapp.screw.game.PlayerStore

/**
 * Профиль игрока: экран «Мое» (личные данные + статистика) и выбор аватара.
 * Аватары — эмодзи (внешних картинок в проекте нет); открываются по номеру уровня.
 */
data class Avatar(val emoji: String, val unlockLevel: Int)

val AVATARS: List<Avatar> = listOf(
    Avatar("🐻", 1),
    Avatar("🐰", 1),
    Avatar("🐿️", 25),
    Avatar("🐧", 30),
    Avatar("🦌", 35),
    Avatar("🐨", 40),
    Avatar("🦝", 45),
    Avatar("🦊", 50),
    Avatar("🦭", 55),
    Avatar("🐶", 60),
    Avatar("🦔", 65),
    Avatar("🐱", 70),
)

fun avatarOf(index: Int): Avatar = AVATARS.getOrElse(index) { AVATARS[1] }

/** Квадратная плашка аватара: светлый фон, эмодзи, рамка выбора, затемнение+замок если закрыт. */
@Composable
fun AvatarBox(
    index: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    locked: Boolean = false,
) {
    val shape = RoundedCornerShape(size * 0.24f)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(if (locked) Color(0xFFC3CBD8) else Color(0xFFF6DCE1))
            .border(if (selected) 3.dp else 2.dp, if (selected) Palette.green else Color(0xFFCBD3DF), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(avatarOf(index).emoji, fontSize = (size.value * 0.56f).sp)
        if (locked) {
            Box(Modifier.fillMaxSize().background(Color(0x661A233A)), contentAlignment = Alignment.Center) {
                Text("🔒", fontSize = (size.value * 0.42f).sp)
            }
        } else if (selected) {
            Text(
                "✔",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.28f).sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(size * 0.34f)
                    .clip(CircleShape)
                    .background(Palette.green),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Экран «Мое»: аватар (тап → смена), ник (карандаш → переименовать), медаль уровня, статистика. */
@Composable
fun MyDialog(player: PlayerStore, onOpenAvatars: () -> Unit, onClose: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(player.nickname) }

    Dialog(onDismissRequest = onClose) {
        GameDialog(L("my"), onClose) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // аватар + шестерёнка — тап открывает выбор аватара
                Box {
                    AvatarBox(player.avatar, 76.dp, modifier = Modifier.clickable { onOpenAvatars() })
                    Text(
                        "⚙️",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Palette.panelBlue)
                            .clickable { onOpenAvatars() }
                            .padding(2.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    if (editing) {
                        TextField(
                            value = draft,
                            onValueChange = { draft = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color(0xFF2A3B63), fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = Palette.blue,
                                focusedIndicatorColor = Palette.blue,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CartoonButton("OK", { player.rename(draft); editing = false }, base = Palette.green, edge = Palette.greenDark, height = 40.dp, fontSize = 15, modifier = Modifier.weight(1f))
                            CartoonButton(L("close"), { draft = player.nickname; editing = false }, base = Palette.blue, height = 40.dp, fontSize = 15, modifier = Modifier.weight(1f))
                        }
                    } else {
                        Text(player.nickname, color = Color(0xFF2A3B63), fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "✏️ Изменить",
                            color = Palette.blue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { draft = player.nickname; editing = true },
                        )
                    }
                }
                MedalBadge(player.bestLevel)
            }

            Spacer(Modifier.height(16.dp))
            StatBanner(L("statistics"))
            Spacer(Modifier.height(10.dp))
            StatsGrid(player)
        }
    }
}

/** Золотая медаль с номером уровня. */
@Composable
private fun MedalBadge(level: Int) {
    Box(
        Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFE08A), Color(0xFFF2B705))))
            .border(3.dp, Color(0xFFD99A00), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(L("level_word"), color = Color(0xFF7A5A00), fontWeight = FontWeight.Bold, fontSize = 9.sp)
            Text("$level", color = Color(0xFF3A4A73), fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
    }
}

/** Голубой флажок-заголовок «Статистика». */
@Composable
private fun StatBanner(text: String) {
    Box(
        Modifier
            .width(220.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(Palette.blue.lighten(0.35f), Palette.blue)))
            .border(2.dp, Color(0xFFFFD23F), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) }
}

@Composable
private fun StatsGrid(player: PlayerStore) {
    val cells = listOf(
        Triple("🔥", L("streak_wins"), player.maxWinStreak),
        Triple("🎯", L("first_try"), 0),
        Triple("📅", L("login_streak"), 0),
        Triple("🏎️", L("race_champ"), 0),
        Triple("👑", L("league_champ"), 0),
        Triple("🗓️", L("daily_win"), 0),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cells.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (icon, title, value) ->
                    StatCard(icon, title, value, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCard(icon: String, title: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF6E3))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 28.sp)
        Spacer(Modifier.height(4.dp))
        Text(title, color = Color(0xFF5A4A2A), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .width(52.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Palette.blue),
            contentAlignment = Alignment.Center,
        ) { Text("$value", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp) }
    }
}

/** Выбор аватара: сетка 3×N; закрытые показывают подсказку «Откроется на N-м уровне». */
@Composable
fun AvatarDialog(player: PlayerStore, onClose: () -> Unit) {
    var hint by remember { mutableStateOf<Int?>(null) }
    Dialog(onDismissRequest = onClose) {
        GameDialog(L("avatars"), onClose) {
            hint?.let { lvl ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(2.dp, Palette.panelBlue, RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(L("unlocks_at", lvl), color = Color(0xFF2A3B63), fontWeight = FontWeight.Black, fontSize = 14.sp, textAlign = TextAlign.Center) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AVATARS.chunked(3).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    ) {
                        row.forEachIndexed { _, av ->
                            val index = AVATARS.indexOf(av)
                            val unlocked = player.bestLevel >= av.unlockLevel
                            AvatarBox(
                                index = index,
                                size = 84.dp,
                                selected = player.avatar == index,
                                locked = !unlocked,
                                modifier = Modifier.clickable {
                                    if (unlocked) {
                                        player.chooseAvatar(index)
                                        hint = null
                                    } else {
                                        hint = av.unlockLevel
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
