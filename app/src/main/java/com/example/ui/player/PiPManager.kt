package com.example.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.Rational

/**
 * Reusable Picture-in-Picture helper. Guarded for Android 8.0 (API 26)+ and for devices that
 * actually advertise the PiP system feature, so it is a safe no-op on the project's minSdk 24.
 */
object PiPManager {

    /**
     * Set true by the player while it is active and playing, so the host Activity's
     * onUserLeaveHint() can auto-enter PiP when the user presses Home. Reset on dispose.
     */
    @Volatile
    var autoEnterEnabled: Boolean = false

    fun isPipSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    /** Enter PiP with a 16:9 (or supplied) aspect ratio. Silently ignored when unsupported. */
    fun enterPip(activity: Activity?, widthRatio: Int = 16, heightRatio: Int = 9) {
        if (activity == null) return
        if (!isPipSupported(activity)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(widthRatio, heightRatio))
                .build()
            activity.enterPictureInPictureMode(params)
        } catch (e: Exception) {
            Log.w("PiPManager", "enterPictureInPictureMode failed: ${e.message}")
        }
    }
}
