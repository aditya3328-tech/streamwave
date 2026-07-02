package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

/**
 * Holds the dynamically acquired MovieBox session token in memory + SharedPreferences.
 *
 * The token is NEVER hardcoded — it is captured at runtime from upstream responses:
 *   - `Set-Cookie: token=...`            (cookie jar → [NetworkModule])
 *   - `x-user: {"token":"..."}`          (response header → [MovieBoxInterceptor])
 *
 * It is then replayed as `Cookie: token=<token>` on subsequent requests so /subject/play
 * runs with the same authenticated session the caption call established.
 */
object MovieBoxSession {
    private const val TRACE = "MOVIEBOX_TRACE"
    private const val PREFS = "moviebox_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_MB_TOKEN = "mb_token"

    private val tokenRef = AtomicReference<String?>(null)
    private val mbTokenRef = AtomicReference<String?>(null)
    @Volatile private var prefs: SharedPreferences? = null

    /** Call once at app start (MainActivity) so the token survives process restarts. */
    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs?.getString(KEY_TOKEN, null)?.let { tokenRef.set(it) }
        prefs?.getString(KEY_MB_TOKEN, null)?.let { mbTokenRef.set(it) }
        Log.d(TRACE, "SESSION init: token=${mask(tokenRef.get())} mb_token=${mask(mbTokenRef.get())}")
    }

    fun token(): String? = tokenRef.get()
    fun mbToken(): String? = mbTokenRef.get()
    fun hasToken(): Boolean = !tokenRef.get().isNullOrBlank()

    fun setToken(value: String?, source: String) {
        if (value.isNullOrBlank()) return
        if (value == tokenRef.get()) return
        tokenRef.set(value)
        prefs?.edit()?.putString(KEY_TOKEN, value)?.apply()
        Log.d(TRACE, "SESSION token <= $source: ${mask(value)}")
    }

    fun setMbToken(value: String?, source: String) {
        if (value.isNullOrBlank()) return
        if (value == mbTokenRef.get()) return
        mbTokenRef.set(value)
        prefs?.edit()?.putString(KEY_MB_TOKEN, value)?.apply()
        Log.d(TRACE, "SESSION mb_token <= $source: ${mask(value)}")
    }

    /** Extract the `token` field from an `x-user` response header value. */
    fun captureXUser(xUser: String?, source: String) {
        if (xUser.isNullOrBlank()) return
        try {
            val parsed = JSONObject(xUser).optString("token", "")
            if (parsed.isNotBlank()) setToken(parsed, "$source/x-user")
        } catch (e: Exception) {
            Log.w(TRACE, "SESSION x-user parse failed ($source): ${e.message}")
        }
    }

    /** `Cookie` header value to attach when a token exists; empty otherwise. */
    fun cookieHeader(): String = buildList {
        token()?.takeIf { it.isNotBlank() }?.let { add("token=$it") }
        mbToken()?.takeIf { it.isNotBlank() }?.let { add("mb_token=$it") }
    }.joinToString("; ")

    private fun mask(v: String?): String =
        if (v.isNullOrBlank()) "<none>" else "${v.take(8)}…(${v.length})"
}
