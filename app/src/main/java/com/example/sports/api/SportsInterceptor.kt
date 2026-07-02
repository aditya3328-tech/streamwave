package com.example.sports.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Injects the fixed SportsNow browser-equivalent headers on every sport API request and
 * captures the session token from the response `Set-Cookie` header.
 *
 * Independent of the movie [MovieBoxInterceptor]; it only ever touches sport requests.
 */
class SportsInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val builder = original.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Origin", ORIGIN)
            .header("Referer", REFERER)
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .header("x-client-info", X_CLIENT_INFO)
            .header("x-device-info", X_DEVICE_INFO)
            .header("User-Agent", USER_AGENT)

        // Replay a previously captured token as a Cookie on outgoing requests.
        SportsSession.token()?.takeIf { it.isNotBlank() }?.let {
            builder.header("Cookie", "token=$it")
        }

        val response = chain.proceed(builder.build())

        // Capture any token handed back via Set-Cookie and persist it.
        response.headers("Set-Cookie").forEach { raw ->
            val token = raw.substringAfter("token=", "").substringBefore(';').trim()
            if (token.isNotBlank()) {
                SportsSession.setToken(token, "Set-Cookie")
            }
        }

        Log.d("SPORTS_NET", "${response.code} ${original.method} ${original.url}")
        return response
    }

    companion object {
        const val ORIGIN = "https://sportsnow.top"
        const val REFERER = "https://sportsnow.top/"
        const val USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"
        const val X_CLIENT_INFO = "{\"timezone\":\"Asia/Calcutta\",\"system_language\":\"en\"}"
        const val X_DEVICE_INFO = "{}"
    }
}
