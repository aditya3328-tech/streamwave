package com.example.data.remote

import android.util.Log

/**
 * Lightweight logging utility (mirrors the web's console logger).
 *
 * This object holds NO networking. All HTTP goes directly to the upstream
 * MovieBox / AoneRoom API through [RetrofitClient] — there is no custom backend,
 * no localhost, and no second Retrofit/OkHttp stack here.
 */
object ApiClient {
    private const val TAG = "StreamWave"
    private const val LOG_LEVEL = "warn"

    private fun shouldLog(level: String): Boolean {
        val levels = mapOf(
            "debug" to 0,
            "info" to 1,
            "warn" to 2,
            "error" to 3
        )
        val currentLevel = levels[LOG_LEVEL] ?: 2
        val messageLevel = levels[level] ?: 0
        return messageLevel >= currentLevel
    }

    fun log(level: String, message: String, data: String = "") {
        if (shouldLog(level)) {
            val logMessage = "[StreamWave] $level: $message $data"

            when (level) {
                "debug" -> Log.d(TAG, logMessage)
                "info" -> Log.i(TAG, logMessage)
                "warn" -> Log.w(TAG, logMessage)
                "error" -> Log.e(TAG, logMessage)
            }
        }
    }
}
