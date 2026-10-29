package com.fidapp.screw.ui.game

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Палитра в стиле скриншотов: насыщенный «пластиковый» синий, золотые монеты,
 * красные жизни, оранжевые промо-блоки. Держим одной точкой, чтобы клона-стиль
 * было легко подкрутить.
 */
object Palette {
    // фон / панели
    val skyTop = Color(0xFF9FC6F5)
    val skyBottom = Color(0xFFE7F0FB)
    val boardBg = Color(0xFFE9EDF3)
    val panelBlue = Color(0xFF4FA3F7)
    val panelBlueDark = Color(0xFF2E7BD6)
    val panelEdge = Color(0xFF1C5FB0)
    val dialogBg = Color(0xFFEAF3FF)

    // валюта / жизни
    val coin = Color(0xFFF6B500)
    val coinLight = Color(0xFFFFE08A)
    val coinDark = Color(0xFFC98600)
    val heart = Color(0xFFE53935)
    val heartDark = Color(0xFFB71C1C)
    val green = Color(0xFF4CAF50)
    val greenDark = Color(0xFF2E7D32)

    // кнопки
    val blue = Color(0xFF2F80ED)
    val blueDark = Color(0xFF1B5FBF)
    val yellow = Color(0xFFFFC534)
    val yellowDark = Color(0xFFE09E00)
    val orange = Color(0xFFFF8A00)
    val orangeDark = Color(0xFFE06A00)
    val red = Color(0xFFE53935)
    val redDark = Color(0xFFB71C1C)

    // карта уровней
    val mapLocked = Color(0xFF6A4E9C)
    val mapCurrent = Color(0xFF4CAF50)
    val mapDone = Color(0xFF2F80ED)
    val mapPath = Color(0xFFF6C97A)

    // промо «наборы»
    val promoTop = Color(0xFFFFB300)
    val promoBottom = Color(0xFFFF7A00)
    val promoPinkTop = Color(0xFFFF6EC0)

    val dialogGradient = Brush.verticalGradient(listOf(Color(0xFFF7FBFF), Color(0xFFE3EEFC)))
    val skyGradient = Brush.verticalGradient(listOf(skyTop, skyBottom))
}
