package com.fidapp.screw.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fidapp.screw.MainActivity
import com.fidapp.screw.R
import com.fidapp.screw.game.PlayerStore

/**
 * Просыпается примерно раз в сутки, читает состояние на устройстве и показывает
 * одно уведомление — или не показывает ничего, если трогать игрока незачем:
 *
 *   1. сегодня ещё не играли (сильнейший повод — вчерашний день без партии
 *      обрывает привычку, а копилка сама себя не наполнит);
 *   2. играл, но прогресс ещё небольшой — зовём открыть следующий уровень.
 *
 * Текст каждый день новый (ротация по номеру дня), чтобы не примелькался.
 */
class ReminderWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val isTest = inputData.getBoolean(KEY_TEST, false)
        val player = PlayerStore(context.getSharedPreferences(PREFS_GAME, Context.MODE_PRIVATE))
        if (!isTest && !player.reminders) return Result.success()
        if (!Reminders.hasPermission(context)) return Result.success()

        // WorkManager умеет перезапускать джобу — не пингуем дважды за календарный день
        val state = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = System.currentTimeMillis() / MILLIS_PER_DAY
        if (!isTest && state.getLong(KEY_LAST_DAY, -1L) == today) return Result.success()

        val playedToday = player.playedToday()
        val message = pickMessage(player, playedToday, today.toInt())
        val (title, body) = when {
            message != null -> message
            isTest -> TEST_MSG
            else -> return Result.success()
        }

        Reminders.ensureChannel(context)
        val tapIntent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, Reminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        Reminders.manager(context).notify(NOTIF_ID, notification)
        if (!isTest) state.edit().putLong(KEY_LAST_DAY, today).apply()
        return Result.success()
    }

    /** Повод напомнить или null, когда напоминать не о чём. */
    private fun pickMessage(player: PlayerStore, playedToday: Boolean, day: Int): Pair<String, String>? {
        if (!playedToday) return NOT_PLAYED[day % NOT_PLAYED.size]
        // Играл сегодня, но в начале пути — второй по силе повод открыть следующий уровень
        if (player.bestLevel in 2..MAX_CARE_ABOUT_PROGRESS) return PLAYED[day % PLAYED.size]
        return null
    }

    companion object {
        const val WORK_NAME = "daily_reminders"

        /** Флаг debug-самопроверки: обходит запрет на второй показ за день. */
        const val KEY_TEST = "test"

        private const val PREFS = "reminders_state"
        private const val PREFS_GAME = "screw_game"
        private const val KEY_LAST_DAY = "last_notify_day"
        private const val NOTIF_ID = 1001
        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

        /** Пока лучший уровень меньше — всё ещё есть куда расти и напоминание уместно. */
        private const val MAX_CARE_ABOUT_PROGRESS = 50

        private val NOT_PLAYED = listOf(
            "🔩 Шурупы ждут тебя" to
                "Загляни на пару минут: один уровень займёт не больше пяти, а копилка сама себя не наполнит.",
            "⚙️ Не сбавляй темп!" to
                "Вчера крутил шурупы, сегодня тишина. Собери хотя бы один ящик, чтобы привычка не оборвалась.",
            "🧰 Ящики сами не соберутся" to
                "Открой «Мастер сборки» — следующий уровень уже ждёт тебя на карте.",
        )

        private val PLAYED = listOf(
            "🏅 Есть куда расти" to
                "Сегодня ты уже играл. Открой следующий уровень — в копилке за него ещё 200 монет.",
            "🔧 Ещё один заход?" to
                "Серия побед ждёт. Один свободный уровень займёт всего пару минут.",
        )

        private val TEST_MSG = "🔔 Тест уведомлений" to
            "Так будет выглядеть ежедневное напоминание. Если вы это видите — уведомления работают."
    }
}
