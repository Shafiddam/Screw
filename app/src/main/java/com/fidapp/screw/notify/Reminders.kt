package com.fidapp.screw.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Напоминания «покрутил бы ты шурупы сегодня»: тонкая обёртка над [ReminderWorker] —
 * канал уведомлений, проверка разрешения и точки старт/отмена.
 *
 * Всё локально: раз в сутки около [HOUR_OF_DAY] просыпается периодическая
 * джоба WorkManager, читает состояние на устройстве и показывает одно
 * уведомление. Сервера, push-транспорта и утечки данных нет — поэтому
 * разрешение `POST_NOTIFICATIONS` единственное, что добавили к VIBRATE.
 */
object Reminders {

    const val CHANNEL_ID = "daily_reminders"

    /** Примерное время ежедневного напоминания; Android может сдвинуть под Doze. */
    private const val HOUR_OF_DAY = 19

    fun manager(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /** На Android 12 и ниже разрешения не существует — считаем, что всё можно. */
    fun hasPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Ежедневные напоминания про уровни и копилку"
            }
            manager(context).createNotificationChannel(channel)
        }
    }

    /** Перезаписывает джобу: следующий запуск — ближайший [HOUR_OF_DAY]. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayToNextHour(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ReminderWorker.WORK_NAME)
    }

    /**
     * Самопроверка для debug-сборки: один раз через пару секунд показывает
     * уведомление, не дожидаясь 19:00 и не глядя на флаг «не пинговать дважды
     * за день». Из релизных путей не вызывается.
     */
    fun debugTest(context: Context) {
        ensureChannel(context)
        val data = androidx.work.workDataOf(ReminderWorker.KEY_TEST to true)
        val request = androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(3, TimeUnit.SECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private fun delayToNextHour(): Long {
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, HOUR_OF_DAY)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        if (next.timeInMillis <= now) next.add(Calendar.DAY_OF_YEAR, 1)
        return next.timeInMillis - now
    }
}
