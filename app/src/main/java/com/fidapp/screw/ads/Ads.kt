package com.fidapp.screw.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.fidapp.screw.BuildConfig
import com.my.target.ads.Reward
import com.my.target.ads.RewardedAd
import com.my.target.ads.InterstitialAd
import com.my.target.common.MyTargetManager
import com.my.target.common.models.IAdLoadingError

/**
 * Реальная реклама VK Ads (myTarget). Слоты зарегистрированы в кабинете ВК-рекламы
 * и используются одни и те же в debug и релизе: в отладке запросы просто помечаются
 * тестовыми через MyTargetManager.setDebugMode(true). Глобальная инициализация по
 * appId не нужна — кабинет привязан к пакету приложения.
 */
object AdSlots {
    const val BANNER = 2101504        // баннер 320x50 (MyTargetBanner в ui/game/AdBanner.kt)
    const val REWARDED = 2101507      // видео с наградой (кнопки «Бесплатно»)
    const val INTERSTITIAL = 2101510  // полноэкранная после каждых 5 уровней
}

/**
 * Видео с вознаграждением. Игрок досматривает ролик и получает награду
 * (бесплатный бонус, воскрешение, +1 жизнь, удвоение награды, продолжение после проигрыша).
 * Экземпляр RewardedAd одноразовый — после показа/ошибки создаём новый и предзагружаем.
 * Награда выдаётся СТРОГО в колбэке onReward — он гарантирует досмотр.
 */
object RewardAdManager {

    private const val TAG = "RewardAd"

    private var ad: RewardedAd? = null
    private var isLoaded = false
    private var isLoading = false
    private var appContext: Context? = null

    // Действие, выполняемое при получении награды (одноразовое)
    private var pendingReward: (() -> Unit)? = null

    /** Инициализация и предзагрузка. Вызывать при старте MainActivity. */
    fun init(activity: Activity) {
        MyTargetManager.setDebugMode(BuildConfig.DEBUG)
        appContext = activity.applicationContext
        if (ad == null) createAndLoad()
    }

    private fun createAndLoad() {
        val ctx = appContext ?: return
        if (isLoading) return

        val newAd = RewardedAd(AdSlots.REWARDED, ctx)
        newAd.setListener(object : RewardedAd.RewardedAdListener {
            override fun onLoad(rewardedAd: RewardedAd) {
                Log.d(TAG, "onLoad: видео загружено")
                isLoading = false
                isLoaded = true
            }

            override fun onNoAd(error: IAdLoadingError, rewardedAd: RewardedAd) {
                Log.w(TAG, "onNoAd: видео не загружено [${error.code}]: ${error.message}")
                isLoading = false
                isLoaded = false
            }

            override fun onClick(rewardedAd: RewardedAd) = Unit

            override fun onDisplay(rewardedAd: RewardedAd) {
                Log.d(TAG, "onDisplay: видео показывается")
            }

            override fun onFailedToShow(rewardedAd: RewardedAd) {
                Log.w(TAG, "onFailedToShow: показать не удалось, награду НЕ выдаём")
                isLoaded = false
                pendingReward = null
                createAndLoad()
            }

            override fun onDismiss(rewardedAd: RewardedAd) {
                Log.d(TAG, "onDismiss: видео закрыто, предзагружаю следующее")
                isLoaded = false
                pendingReward = null
                createAndLoad()
            }

            override fun onReward(reward: Reward, rewardedAd: RewardedAd) {
                Log.d(TAG, "onReward: награда получена")
                val action = pendingReward
                pendingReward = null
                action?.invoke()
            }
        })

        ad = newAd
        isLoading = true
        newAd.load()
    }

    /**
     * Показать видео. [onReward] вызывается только если пользователь получил
     * награду (досмотрел). Если видео не готово — тост, [onReward] НЕ вызывается.
     */
    fun show(activity: Activity, onReward: () -> Unit) {
        val current = ad
        if (current == null || !isLoaded) {
            Log.w(TAG, "show: видео не готово")
            Toast.makeText(activity, "Видео пока недоступно, попробуй позже", Toast.LENGTH_SHORT).show()
            createAndLoad()
            return
        }
        if (activity.isFinishing || activity.isDestroyed) return

        pendingReward = onReward
        try {
            current.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "show: ошибка показа", e)
            pendingReward = null
            Toast.makeText(activity, "Видео пока недоступно, попробуй позже", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Полноэкранная реклама: показывается после каждых 5 завершённых уровней
 * (победа или поражение — неважно). В debug-сборке полностью отключена,
 * чтобы не мешать тестам. Экземпляр InterstitialAd одноразовый (перезагрузка
 * даёт ошибку 4001) — после показа создаём новый.
 */
object InterstitialManager {

    private const val TAG = "Interstitial"
    private const val PREFS_NAME = "ad_prefs"
    private const val KEY_LEVELS_DONE = "levels_since_ad"
    private const val EVERY_LEVELS = 5
    private const val ENABLED_IN_DEBUG = false

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoaded = false
    private var isLoading = false
    private var appContext: Context? = null

    fun init(activity: Activity) {
        MyTargetManager.setDebugMode(BuildConfig.DEBUG)
        if (BuildConfig.DEBUG && !ENABLED_IN_DEBUG) {
            Log.d(TAG, "init: debug-сборка, полноэкранная отключена")
            return
        }
        appContext = activity.applicationContext
        if (interstitialAd == null) createAndLoad()
    }

    private fun createAndLoad() {
        val ctx = appContext ?: return
        if (isLoading) return

        val ad = InterstitialAd(AdSlots.INTERSTITIAL, ctx)
        ad.setListener(object : InterstitialAd.InterstitialAdListener {
            override fun onLoad(ad: InterstitialAd) {
                Log.d(TAG, "onLoad: реклама загружена")
                isLoading = false
                isAdLoaded = true
            }

            override fun onNoAd(error: IAdLoadingError, ad: InterstitialAd) {
                Log.w(TAG, "onNoAd: реклама не загружена [${error.code}]: ${error.message}")
                isLoading = false
                isAdLoaded = false
            }

            override fun onClick(ad: InterstitialAd) = Unit

            override fun onDisplay(ad: InterstitialAd) {
                Log.d(TAG, "onDisplay: реклама показана")
                isAdLoaded = false
            }

            override fun onDismiss(ad: InterstitialAd) {
                Log.d(TAG, "onDismiss: реклама закрыта, создаю новый экземпляр")
                isAdLoaded = false
                isLoading = false
                createAndLoad()
            }

            override fun onVideoCompleted(ad: InterstitialAd) = Unit

            override fun onFailedToShow(ad: InterstitialAd) {
                Log.w(TAG, "onFailedToShow: создаю новый экземпляр")
                isAdLoaded = false
                isLoading = false
                createAndLoad()
            }
        })

        interstitialAd = ad
        isLoading = true
        ad.load()
    }

    /** Вызывать на итог уровня (победа или финальное поражение). */
    fun onLevelFinished(activity: Activity) {
        if (BuildConfig.DEBUG && !ENABLED_IN_DEBUG) {
            Log.d(TAG, "onLevelFinished: debug-сборка, пропускаю")
            return
        }
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val done = prefs.getInt(KEY_LEVELS_DONE, 0) + 1
        if (done >= EVERY_LEVELS) {
            prefs.edit().putInt(KEY_LEVELS_DONE, 0).apply()
            show(activity)
        } else {
            prefs.edit().putInt(KEY_LEVELS_DONE, done).apply()
        }
    }

    private fun show(activity: Activity) {
        val ad = interstitialAd
        if (ad == null || !isAdLoaded) {
            // реклама не готова — пропускаем этот показ, подгружаем к следующему разу
            createAndLoad()
            return
        }
        if (activity.isFinishing || activity.isDestroyed) return
        try {
            ad.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "show: ошибка показа рекламы", e)
        }
    }
}
