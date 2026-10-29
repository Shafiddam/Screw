package com.fidapp.screw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.fidapp.screw.game.PlayerStore
import com.fidapp.screw.ui.game.ScrewGame
import com.fidapp.screw.ui.theme.ScrewTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val player = PlayerStore(getSharedPreferences("screw_game", MODE_PRIVATE))
        setContent {
            ScrewTheme(dynamicColor = false, darkTheme = false) {
                Box(Modifier.fillMaxSize().background(Color(0xFFE9EDF3))) {
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
