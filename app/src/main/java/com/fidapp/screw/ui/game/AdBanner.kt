package com.fidapp.screw.ui.game

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fidapp.screw.ads.AdSlots
import com.my.target.ads.MyTargetView
import com.my.target.common.models.IAdLoadingError

/**
 * Боевой баннер VK Ads (myTarget) 320x50 внизу игрового экрана.
 * В Compose встраивается через AndroidView: MyTargetView создаётся с контекстом
 * Activity, грузится в onLoad и только тогда выделяется высота (50dp) — без
 * объявления ряд не занимает место и не «прыгает».
 */
@Composable
fun MyTargetBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() } ?: return
    var loaded by remember { mutableStateOf(false) }

    val adView = remember(activity) {
        MyTargetView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setSlotId(AdSlots.BANNER)
            // фиксированный формат 320x50 (адаптивный SDK считал бы высоту сам)
            setAdSize(MyTargetView.AdSize.ADSIZE_320x50)
            setListener(object : MyTargetView.MyTargetViewListener {
                override fun onLoad(view: MyTargetView) { loaded = true }
                override fun onNoAd(error: IAdLoadingError, view: MyTargetView) { loaded = false }
                override fun onShow(view: MyTargetView) = Unit
                override fun onClick(view: MyTargetView) = Unit
            })
            load()
        }
    }

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(if (loaded) 50.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
    }
}

/** Compose-контекст в одно-Activity приложении — это сама Activity. */
private fun android.content.Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx.baseContext is Activity) return ctx.baseContext as Activity
        ctx = ctx.baseContext
    }
    return ctx as? Activity
}
