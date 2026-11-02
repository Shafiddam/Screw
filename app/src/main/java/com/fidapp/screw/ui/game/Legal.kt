package com.fidapp.screw.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private const val APP = "«Мастер сборки: винты и шурупы»"

/** Условия использования — тот же текст, что в Tetris, адаптирован под монеты/жизни/копилку. */
val TERMS_OF_USE_TEXT = """
    Дата вступления в силу: текущая версия приложения.

    1. Использование приложения.
    Устанавливая и используя Приложение $APP, вы соглашаетесь
    с настоящими Условиями. Приложение предоставляется бесплатно.

    2. Игровые ценности и награды.
    Монеты, жизни, уровни, копилка и бонусы являются виртуальными
    объектами, не являются денежными средствами, не подлежат обмену
    на реальные деньги и не могут быть выведены. Виртуальные ценности
    не являются объектами гражданских прав и возврату не подлежат.

    3. Права пользователя.
    Вы можете:
    — проходить любое количество уровней без ограничений;
    — отключать звук, музыку, вибрацию и напоминания;
    — отключать напоминания тем же тумблером в настройках;
    — в любой момент удалить прогресс и само Приложение.
    Рекламные баннеры отключению не подлежат: Приложение распространяется
    бесплатно и оплачивается их показом; персональные данные при этом
    не собираются.

    4. Запрещается.
    — использовать модифицированные версии Приложения;
    — предпринимать попытки накрутки монет и обхода рекламы;
    — публиковать материалы Приложения с целью перепродажи.

    5. Отказ от ответственности.
    Приложение предоставляется «как есть». Разработчик не несёт
    ответственности за возможную потерю игрового прогресса, вызванную
    действиями пользователя, сбоем устройства или операционной системы.

    6. Интеллектуальная собственность.
    Оформление, код и графика Приложения охраняются авторским правом.

    7. Изменение условий.
    Разработчик вправе изменять условия. Актуальная версия доступна
    в разделе «Условия использования» внутри Приложения.
""".trimIndent()

/**
 * Разбивает текст на абзацы: строка с точкой на конце и короткая — считаем
 * заголовком раздела, она рисуется жирным.
 */
fun splitLegal(body: String): List<Pair<String, Boolean>> =
    body.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
        val isHeading = line.length <= 60 && line.endsWith(".") &&
            line.first().isDigit().not() && line.first().isUpperCase()
        line to isHeading
    }

/** Читаемый показ юридического текста: заголовок в синей шапке, тело скроллится. */
@Composable
fun LegalDialog(title: String, body: String, onDismiss: () -> Unit) {
    val click = LocalClickSound.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Palette.blue)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    splitLegal(body).forEach { (line, isHeading) ->
                        Text(
                            line,
                            fontSize = if (isHeading) 17.sp else 15.sp,
                            fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal,
                            color = Color(0xFF2A3B63),
                            lineHeight = if (isHeading) 22.sp else 21.sp,
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Palette.panelBlueDark)
                        .clickable { click(); onDismiss() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        L("close").uppercase(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}
