package com.example.ui.theme

import android.content.Context
import android.content.res.Resources
import android.view.animation.AnimationUtils
import androidx.annotation.AnimRes
import androidx.annotation.DrawableRes
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.R
import org.xmlpull.v1.XmlPullParser

@Immutable
data class UiMotionSpecs(
    val panelEnterMs: Int = 220,
    val panelExitMs: Int = 180,
    val overlayFadeInMs: Int = 200,
    val overlayFadeOutMs: Int = 160,
    val dialogScaleFadeMs: Int = 200
) {
    fun panelEnterTransition(): EnterTransition =
        fadeIn(animationSpec = tween(durationMillis = panelEnterMs)) +
            slideInVertically(animationSpec = tween(durationMillis = panelEnterMs)) { it / 6 }

    fun panelExitTransition(): ExitTransition =
        fadeOut(animationSpec = tween(durationMillis = panelExitMs)) +
            slideOutVertically(animationSpec = tween(durationMillis = panelExitMs)) { it / 6 }

    fun overlayFadeInTransition(): EnterTransition =
        fadeIn(animationSpec = tween(durationMillis = overlayFadeInMs))

    fun overlayFadeOutTransition(): ExitTransition =
        fadeOut(animationSpec = tween(durationMillis = overlayFadeOutMs))
}

@Immutable
data class PackagedUiResourceSnapshot(
    @DrawableRes val threeDRotateIcon: Int = R.drawable.ic_3d_rotate,
    @DrawableRes val threeDScaleIcon: Int = R.drawable.ic_3d_scale,
    @DrawableRes val threeDSkewIcon: Int = R.drawable.ic_3d_skew,
    @AnimRes val panelEnterAnim: Int = R.anim.panel_slide_up_enter,
    @AnimRes val panelExitAnim: Int = R.anim.panel_slide_down_exit,
    @AnimRes val overlayFadeInAnim: Int = R.anim.overlay_fade_in,
    @AnimRes val overlayFadeOutAnim: Int = R.anim.overlay_fade_out,
    @AnimRes val dialogScaleFadeInAnim: Int = R.anim.dialog_scale_fade_in
)

object UiResourceLoader {
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

    fun loadAnimationDurationMs(context: Context, @AnimRes animResId: Int, fallbackMs: Int): Int {
        return runCatching {
            val anim = AnimationUtils.loadAnimation(context, animResId)
            val loaded = anim.duration.toInt()
            if (loaded > 0) loaded else parseXmlDurationMs(context.resources, animResId, fallbackMs)
        }.getOrElse { fallbackMs }
    }

    private fun parseXmlDurationMs(resources: Resources, @AnimRes animResId: Int, fallbackMs: Int): Int {
        return runCatching {
            resources.getAnimation(animResId).use { parser ->
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        val raw = parser.getAttributeValue(ANDROID_NS, "duration")?.toIntOrNull()
                        if (raw != null && raw > 0) return raw
                    }
                    event = parser.next()
                }
                fallbackMs
            }
        }.getOrDefault(fallbackMs)
    }

    fun loadMotionSpecs(context: Context): UiMotionSpecs = UiMotionSpecs(
        panelEnterMs = loadAnimationDurationMs(context, R.anim.panel_slide_up_enter, 220),
        panelExitMs = loadAnimationDurationMs(context, R.anim.panel_slide_down_exit, 180),
        overlayFadeInMs = loadAnimationDurationMs(context, R.anim.overlay_fade_in, 200),
        overlayFadeOutMs = loadAnimationDurationMs(context, R.anim.overlay_fade_out, 160),
        dialogScaleFadeMs = loadAnimationDurationMs(context, R.anim.dialog_scale_fade_in, 200)
    )

    fun packagedResourceSnapshot(): PackagedUiResourceSnapshot = PackagedUiResourceSnapshot()
}

@Composable
fun rememberUiMotionSpecs(): UiMotionSpecs {
    val context = LocalContext.current
    return remember(context) { UiResourceLoader.loadMotionSpecs(context) }
}
