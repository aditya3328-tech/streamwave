package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Reusable fullscreen controller for the player.
 *
 * - [enter]  → rotate to landscape and hide the status + navigation bars (immersive).
 * - [exit]   → restore portrait and show the system bars.
 * - [isFullscreen] is observable Compose state the player reads to switch its layout
 *   (16:9 letterbox in portrait → edge-to-edge fill in landscape, killing the black bars).
 */
class FullscreenController(
    private val activity: Activity?,
    private val view: View,
) {
    var isFullscreen by mutableStateOf(false)
        private set

    fun toggle() {
        if (isFullscreen) exit() else enter()
    }

    fun enter() {
        val act = activity ?: return
        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setSystemBarsVisible(false)
        isFullscreen = true
    }

    fun exit() {
        val act = activity ?: return
        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setSystemBarsVisible(true)
        isFullscreen = false
    }

    private fun setSystemBarsVisible(visible: Boolean) {
        val act = activity ?: return
        val controller = WindowInsetsControllerCompat(act.window, view)
        if (visible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
fun rememberFullscreenController(): FullscreenController {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val controller = remember(activity, view) { FullscreenController(activity, view) }

    // Always restore portrait + system bars when the player leaves composition.
    DisposableEffect(Unit) {
        onDispose {
            if (activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowInsetsControllerCompat(activity.window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    return controller
}

/** Walk the ContextWrapper chain to find the host Activity (Compose's LocalContext is often a wrapper). */
fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
