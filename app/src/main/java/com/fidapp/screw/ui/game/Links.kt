package com.fidapp.screw.ui.game

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Внешние ссылки приложения (карточка игры в RuStore, страница разработчика,
 * Телеграм-канал, донат). Все ссылки — те же, что в других играх пользователя
 * (Tetris/Finddifferences).
 *
 * Открытие никогда не роняет игру: нет магазина/браузера — тихо ничего не
 * делаем (в остальных проектах ровно такой же подход).
 */
object Links {

    private const val TAG = "Links"

    /** Каталог разработчика в RuStore */
    const val CATALOG = "https://www.rustore.ru/catalog/developer/tivtpi"

    /** Наш Телеграм-канал */
    const val TELEGRAM = "https://t.me/fidappstudio"

    /** Добровольный донат (Ozon Bank / СБП) — та же ссылка, что в предках */
    const val DONATE = "https://finance.ozon.ru/apps/sbp/ozonbankpay/01a0d312-eb15-7eeb-85e4-da4d6f546754"

    /** Почта для контактов (упоминается в юридических текстах) */
    const val EMAIL = "fidappstudio@gmail.com"

    /**
     * Пакеты RuStore в разных версиях. Голый `market://` перехватывает Google
     * Play, а `https://` на части устройств открывает браузер — поэтому целим
     * строго в магазин через setPackage и перебираем варианты.
     */
    private val RUSTORE_PACKAGES = listOf(
        "ru.rustore",
        "ru.onlab.android",
        "ru.zwebks.rustore.gateway",
    )

    /**
     * Браузеры по убыванию вероятности на устройствах. Нужны, чтобы обойти
     * перехват app-ссылки магазином: RuStore на `https://www.rustore.ru/...`
     * закрывает запущенное от игры окно и пересоздаёт своё с флагом
     * EXCLUDE_FROM_RECENTS — из-за этого «Назад» из каталога уводит в рабочий
     * стол, минуя игру (воспроизведено на realme RMX3263, Android 11).
     */
    private val BROWSERS = listOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.yandex.browser",
        "com.opera.browser",
        "com.microsoft.emmx",
        "com.vivaldi.browser",
        "com.duckduckgo.mobile.android",
        "com.brave.browser",
        "com.sec.android.app.sbrowser",
        "com.heytap.browser",
        "com.android.browser",
    )

    fun open(context: Context, url: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "open: нечем открыть $url (${e.message})")
        }
    }

    /** Каталог игр разработчика — только в браузере (см. [BROWSERS]). */
    fun openCatalog(context: Context) {
        if (!openInBrowser(context, CATALOG)) open(context, CATALOG)
    }

    /** Несёт ссылку явно в первый найденный браузер. `setPackage` не требует
     *  `<queries>` в манифесте: отсутствующий пакет просто кидает исключение. */
    private fun openInBrowser(context: Context, url: String): Boolean {
        for (pkg in BROWSERS) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setPackage(pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val ok = runCatching { context.startActivity(intent) }.isSuccess
            if (ok) return true
        }
        Log.w(TAG, "openInBrowser: ни один браузер не принял ссылку")
        return false
    }

    /**
     * Карточка приложения в RuStore: сначала сам магазин (пользователь останется
     * внутри и попадёт в форму отзыва), при неудаче — веб-страница каталога.
     */
    fun openAppPage(context: Context) {
        val pkg = context.packageName
        for (store in RUSTORE_PACKAGES) {
            for (uri in listOf("ru.market://details?id=$pkg", "market://details?id=$pkg")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(store)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val ok = runCatching {
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                        true
                    } else false
                }.getOrDefault(false)
                if (ok) return
            }
        }
        open(context, "https://apps.rustore.ru/app/$pkg")
    }
}
