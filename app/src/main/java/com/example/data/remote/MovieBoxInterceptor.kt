package com.example.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Injects the MovieBox website's playback headers, replays the dynamically acquired
 * session cookie (token / mb_token from [MovieBoxSession]), captures the `x-user`
 * response header back into the session, and traces everything under MOVIEBOX_TRACE.
 *
 * Used for BOTH hosts so the session captured from caption (h5-api.aoneroom.com)
 * is reused on play (netfilm.world):
 *   - GET .../subject/caption
 *   - GET .../subject/play
 */
class MovieBoxInterceptor : Interceptor {

    companion object {
        private const val TRACE = "MOVIEBOX_TRACE"

        const val ACCEPT = "application/json"
        const val ACCEPT_LANGUAGE = "en-US,en;q=0.9"
        const val CACHE_CONTROL = "no-cache"
        const val PRAGMA = "no-cache"
        const val X_CLIENT_INFO = "{\"timezone\":\"Asia/Calcutta\"}"
        const val USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Version/18.5 Mobile/15E148 Safari/604.1"

        const val ORIGIN = "https://netfilm.world"

        fun buildReferer(subjectId: String, detailPath: String): String =
            "https://netfilm.world/spa/videoPlayPage/movies/$detailPath" +
            "?id=$subjectId&type=/movie/detail&detailSe=&detailEp=&lang=en"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url
        val path = url.encodedPath

        val tag = when {
            path.endsWith("/subject/caption") -> "CAPTION"
            path.endsWith("/subject/play") -> "PLAY"
            else -> "MOVIEBOX"
        }

        val subjectId = url.queryParameter("subjectId").orEmpty()
        val detailPath = url.queryParameter("detailPath").orEmpty()
        val referer = buildReferer(subjectId, detailPath)
        val sessionCookie = MovieBoxSession.cookieHeader()

        val builder = original.newBuilder()
            .header("Accept", ACCEPT)
            .header("Accept-Language", ACCEPT_LANGUAGE)
            .header("Cache-Control", CACHE_CONTROL)
            .header("Pragma", PRAGMA)
            .header("x-client-info", X_CLIENT_INFO)
            .header("Referer", referer)
            .header("Origin", ORIGIN)
            .header("User-Agent", USER_AGENT)

        // Merge our session cookie with anything the CookieJar already attached.
        if (sessionCookie.isNotEmpty()) {
            val existing = original.header("Cookie")
            val merged = if (existing.isNullOrBlank()) sessionCookie else "$existing; $sessionCookie"
            builder.header("Cookie", merged)
        }

        val request = builder.build()
        val sentCookies = request.header("Cookie") ?: "<none>"

        Log.d(TRACE, "──────── $tag REQUEST ────────")
        Log.d(TRACE, "$tag REQUEST URL: ${request.url}")
        Log.d(TRACE, "$tag REQUEST HEADERS:")
        request.headers.forEach { (n, v) -> Log.d(TRACE, "$tag   > $n: $v") }
        Log.d(TRACE, "$tag REQUEST COOKIES: $sentCookies")

        val response = chain.proceed(request)

        // Capture x-user → token, before anyone consumes the body.
        val xUser = response.header("x-user")
        MovieBoxSession.captureXUser(xUser, tag)

        val setCookies = response.headers("Set-Cookie")
        val bodyString = try {
            response.peekBody(1_048_576L).string()
        } catch (e: Exception) {
            "<unreadable: ${e.message}>"
        }

        Log.d(TRACE, "──────── $tag RESPONSE ────────")
        Log.d(TRACE, "$tag RESPONSE CODE: ${response.code} ${response.message}")
        Log.d(TRACE, "$tag SET-COOKIE: ${if (setCookies.isEmpty()) "<none>" else setCookies.joinToString(" | ")}")
        Log.d(TRACE, "$tag X-USER: ${xUser ?: "<none>"}")
        Log.d(TRACE, "$tag RESPONSE BODY: $bodyString")

        return response
    }
}
