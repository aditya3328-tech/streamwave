package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.MovieBoxInterceptor
import com.example.data.remote.MovieBoxSession
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Proves the browser-equivalent bootstrap with the REAL production components
 * (MovieBoxInterceptor, MovieBoxSession, and a cross-host CookieJar identical to
 * NetworkModule's) driven against a local plaintext stand-in for both upstreams.
 *
 * Hostnames stay real (h5-api.aoneroom.com / movie-box.co); only the socket is local,
 * via OkHttp DNS + a fixed port — so the interceptor/jar logic under test is unchanged.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CaptionBootstrapFlowTest {

    private val callOrder = CopyOnWriteArrayList<String>()

    companion object {
        // Stand-in for the token the real upstream mints dynamically (NOT hardcoded in app code).
        private const val DYNAMIC_TOKEN = "SESSION_TOKEN_DYNAMIC_9f3a"
    }

    /** Identical cross-host jar to NetworkModule.cookieJar (token → MovieBoxSession). */
    private val jar = object : CookieJar {
        private val store = ConcurrentHashMap<String, MutableList<Cookie>>()
        private fun isMb(h: String) = h.endsWith("movie-box.co") || h.endsWith("aoneroom.com")
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isEmpty()) return
            val b = store.getOrPut(url.host) { mutableListOf() }
            cookies.forEach { c ->
                b.removeAll { it.name == c.name }; b.add(c)
                if (c.name == "token") MovieBoxSession.setToken(c.value, "Set-Cookie@${url.host}")
            }
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val all = if (isMb(url.host)) store.values.flatten() else store[url.host] ?: emptyList()
            return all.distinctBy { it.name }
        }
    }

    @Test
    fun `caption runs first, mints token, play sends it`() {
        MovieBoxSession.init(ApplicationProvider.getApplicationContext<Context>())

        // Local plaintext server standing in for BOTH upstream hosts.
        val server = LocalUpstream { path, cookieHeader ->
            callOrder.add(path)
            when {
                // Real upstream sends the SAME token in Set-Cookie and x-user.
                path.startsWith("/wefeed-h5api-bff/subject/caption") -> LocalUpstream.Resp(
                    code = 200,
                    headers = listOf(
                        "Set-Cookie" to "token=$DYNAMIC_TOKEN; Path=/",
                        "x-user" to """{"token":"$DYNAMIC_TOKEN"}"""
                    ),
                    body = """{"code":0,"data":{"captions":[]}}"""
                )
                path.startsWith("/wefeed-h5api-bff/subject/play") -> LocalUpstream.Resp(
                    code = 200,
                    headers = emptyList(),
                    // Echo back the cookie the app sent so the test can assert on it.
                    body = """{"received_cookie":"$cookieHeader","data":{"streams":[]}}"""
                )
                else -> LocalUpstream.Resp(404, emptyList(), "")
            }
        }
        server.start()

        try {
            val localDns = object : okhttp3.Dns {
                override fun lookup(hostname: String) = listOf(java.net.InetAddress.getByName("127.0.0.1"))
            }
            val client = OkHttpClient.Builder()
                .cookieJar(jar)
                .addInterceptor(MovieBoxInterceptor())
                .dns(localDns)
                .build()

            fun call(host: String, path: String): String {
                val url = HttpUrl.Builder().scheme("http").host(host).port(server.port)
                    .addPathSegments(path.removePrefix("/"))
                    .addQueryParameter("subjectId", "12345")
                    .addQueryParameter("detailPath", "Some-Movie")
                    .apply { if (path.contains("caption")) addQueryParameter("format", "MP4") }
                    .build()
                client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                    return r.body?.string().orEmpty()
                }
            }

            // ----- production order: caption (aoneroom) THEN play (movie-box) -----
            call("h5-api.aoneroom.com", "/wefeed-h5api-bff/subject/caption")
            assertTrue("token must exist after caption", MovieBoxSession.hasToken())
            val tokenAfterCaption = MovieBoxSession.token()

            val playBody = call("movie-box.co", "/wefeed-h5api-bff/subject/play")

            // ----- assertions = proof -----
            assertEquals("caption must be called FIRST",
                "/wefeed-h5api-bff/subject/caption", callOrder[0].substringBefore("?"))
            assertEquals("play must be called SECOND",
                "/wefeed-h5api-bff/subject/play", callOrder[1].substringBefore("?"))
            // token captured from caption (Set-Cookie + x-user) is the dynamic upstream token.
            assertEquals(DYNAMIC_TOKEN, tokenAfterCaption)
            assertTrue("play request must carry the captured token cookie",
                playBody.contains("token=$DYNAMIC_TOKEN"))

            println("PROOF callOrder = $callOrder")
            println("PROOF token after caption = $tokenAfterCaption")
            println("PROOF play received = $playBody")
        } finally {
            server.stop()
        }
    }
}
