package com.fidapp.screw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.fidapp.screw.ads.InterstitialManager
import com.fidapp.screw.ads.RewardAdManager
import com.fidapp.screw.game.PlayerStore
import com.fidapp.screw.notify.Reminders
import com.fidapp.screw.ui.game.ScrewGame
import com.fidapp.screw.ui.theme.ScrewTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // предзагрузка реальной рекламы VK Ads (видео с наградой, полноэкранная)
        RewardAdManager.init(this)
        InterstitialManager.init(this)
        val player = PlayerStore(getSharedPreferences("screw_game", MODE_PRIVATE))
        // напоминания: сверяем тумблер с джобой при каждом старте
        // (после отзыва разрешения системой или сброса прогресса джобы быть не должно)
        if (player.reminders && Reminders.hasPermission(this)) {
            Reminders.ensureChannel(this)
            Reminders.schedule(this)
        } else {
            Reminders.cancel(this)
        }
        setContent {
            ScrewTheme(dynamicColor = false, darkTheme = false) {
                // фон первого кадра подстраивается под тему телефона:
                // тёмная — чёрный (как splash values-night), светлая — белый
                val boot = if (isSystemInDarkTheme()) Color.Black else Color(0xFFE9EDF3)
                Box(Modifier.fillMaxSize().background(boot)) {
                    ScrewGame(player)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GamePreview() {
    ScrewTheme(dynamicColor = false) {
        // превью без реального стора
    }
}
