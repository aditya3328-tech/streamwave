package com.example.sports.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.concurrent.atomic.AtomicReference

/**
 * Standalone session store for the Sports module. Persists the token captured from the
 * sport BFF `Set-Cookie` header so it survives process restarts and is replayed on every
 * subsequent request. Completely separate from the movie [MovieBoxSession].
 */
object SportsSession {
    private const val TAG = "SPORTS_NET"
    private const val PREFS = "sports_session"
    private const val KEY_TOKEN = "sports_token"

    private val tokenRef = AtomicReference<String?>(null)
    @Volatile private var prefs: SharedPreferences? = null

    /** Call once at app start so the token survives restarts. */
    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs?.getString(KEY_TOKEN, null)?.let { tokenRef.set(it) }
        Log.d(TAG, "session init: token=${mask(tokenRef.get())}")
    }

    fun token(): String? = tokenRef.get()

    fun setToken(value: String?, source: String) {
        if (value.isNullOrBlank() || value == tokenRef.get()) return
        tokenRef.set(value)
        prefs?.edit()?.putString(KEY_TOKEN, value)?.apply()
        Log.d(TAG, "token <= $source: ${mask(value)}")
    }

    private fun mask(v: String?): String =
        if (v.isNullOrBlank()) "<none>" else "${v.take(8)}…(${v.length})"
}
