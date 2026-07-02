package com.example.data.remote.providers

import android.util.Log
import com.example.data.model.Caption
import com.example.data.model.ProviderStreamResponse
import com.example.data.model.Stream
import com.example.data.remote.ApiClient
import com.example.data.remote.MovieBoxSession
import com.example.data.remote.NetworkModule
import com.example.data.remote.StreamValidator

/**
 * MovieBox / AoneRoom Provider Adapter
 * Calls the upstream API directly — no custom backend proxy.
 *
 * Matches the website's backend/providers/moviebox.js exactly:
 *   - Base URL: https://h5-api.aoneroom.com/wefeed-h5api-bff  (RetrofitClient.PROD_API_BASE)
 *   - Headers: Accept, lang=en, versioncode=11, clienttype=0, Authorization (Bearer JWT),
 *     User-Agent (Nexus 5), Referer (movie-box.co), Origin (movie-box.co)
 *     → injected by RetrofitClient.HeaderInterceptor
 *   - Endpoints: GET /subject/play?subjectId&se&ep&detailPath
 *                GET /subject/caption?id&subjectId&detailPath&format=MP4
 */
object MovieBoxProvider {
    private const val TAG = "MovieBoxProvider"
    private const val PROVIDER_NAME = "MovieBox"

    suspend fun getStreams(
        subjectId: String,
        detailPath: String = "",
        season: Int = 0,
        episode: Int = 0
    ): ProviderStreamResponse {
        return try {
            ApiClient.log(
                "info", "Attempting MovieBox stream fetch",
                "subjectId=$subjectId, season=$season, episode=$episode, detailPath=$detailPath"
            )

            // STEP 1 — bootstrap the browser-equivalent session BEFORE /subject/play.
            // The caption call (h5-api.aoneroom.com) returns Set-Cookie: token=... and
            // x-user:{"token":...}; the shared CookieJar + MovieBoxSession capture it so the
            // play request below carries the same authenticated session the browser has.
            bootstrapSession(subjectId, detailPath)

            // STEP 2 — /subject/play (movie-box.co) WITH the captured session cookie.
            val service = NetworkModule.playApi
            val raw = service.getStreamsDirect(subjectId, season, episode, detailPath)

            Log.d(TAG, "=== MOVIEBOX RAW RESPONSE (keys) ===")
            Log.d(TAG, raw.keys.toString())

            @Suppress("UNCHECKED_CAST")
            val data = raw["data"] as? Map<String, Any>
            if (data == null) {
                Log.e(TAG, "No 'data' field in MovieBox response")
                return ProviderStreamResponse(
                    provider = PROVIDER_NAME,
                    success = false,
                    streams = emptyList(),
                    message = "No data received from MovieBox"
                )
            }

            val candidates = mutableListOf<Stream>()

            @Suppress("UNCHECKED_CAST")
            val streams = data["streams"] as? List<Map<String, Any>>
            streams?.forEach { item ->
                StreamValidator.normalizeStream(item, PROVIDER_NAME)?.let {
                    candidates.add(it)
                }
            }

            @Suppress("UNCHECKED_CAST")
            val list = data["list"] as? List<Map<String, Any>>
            list?.forEach { item ->
                StreamValidator.normalizeStream(item, PROVIDER_NAME)?.let {
                    candidates.add(it)
                }
            }

            val videoUrl = data["videoUrl"] as? String
            if (!videoUrl.isNullOrEmpty()) {
                val quality = data["quality"] as? String ?: "Unknown"
                StreamValidator.normalizeStream(
                    mapOf("url" to videoUrl, "quality" to quality),
                    PROVIDER_NAME
                )?.let { candidates.add(it) }
            }

            val unique = candidates.distinctBy { it.url }

            // FLOW_TRACE LOG 0 — provider boundary. Proves/disproves a PARSING bug (hypothesis J):
            // rawStreamsArray = how many objects the JSON "streams" array actually held,
            // normalized       = how many survived normalizeStream() (url present + type != unknown).
            // If rawStreamsArray > 0 but normalized == 0, the bug is parsing/format mapping here.
            // If both are equal and > 0, parsing is innocent and the bug is downstream (validator).
            Log.e("FLOW_TRACE", "Provider rawStreamsArray = ${streams?.size ?: 0}, " +
                "normalized = ${candidates.size}, unique = ${unique.size}")
            Log.e("FLOW_TRACE", "Provider streams = ${unique.map { it.quality to it.type }}")

            Log.d(TAG, "MovieBox returned ${unique.size} unique streams")

            ProviderStreamResponse(
                provider = PROVIDER_NAME,
                success = unique.isNotEmpty(),
                streams = unique,
                message = if (unique.isNotEmpty())
                    "MovieBox returned ${unique.size} streams"
                else
                    "MovieBox did not return playable streams"
            )
        } catch (e: Exception) {
            Log.e(TAG, "MovieBox fetch failed: ${e.message}", e)
            ApiClient.log("error", "MovieBox provider failed", e.message ?: "Unknown error")
            ProviderStreamResponse(
                provider = PROVIDER_NAME,
                success = false,
                streams = emptyList(),
                message = "MovieBox request failed",
                error = e.message
            )
        }
    }

    /**
     * Browser bootstrap: call /subject/caption FIRST so upstream mints the session.
     * Idempotent — skips the network hop once a token is already held.
     * `id` defaults to subjectId (the website's streamId for the default track).
     */
    private suspend fun bootstrapSession(subjectId: String, detailPath: String) {
        if (MovieBoxSession.hasToken()) {
            Log.d(TAG, "Session token already present — skipping caption bootstrap")
            return
        }
        try {
            Log.d(TAG, "Bootstrapping session via /subject/caption (before /subject/play)")
            // Response Set-Cookie / x-user are captured by MovieBoxInterceptor + CookieJar.
            NetworkModule.captionApi.getCaptionsDirect(subjectId, subjectId, detailPath, "MP4")
        } catch (e: Exception) {
            Log.w(TAG, "Caption bootstrap failed (continuing to play): ${e.message}")
        }
    }

    suspend fun getCaptions(
        id: String,
        subjectId: String,
        detailPath: String
    ): List<Caption> {
        return try {
            val service = NetworkModule.captionApi
            val raw = service.getCaptionsDirect(id, subjectId, detailPath, "MP4")

            @Suppress("UNCHECKED_CAST")
            val data = raw["data"] as? Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val captionsRaw = data?.get("captions") as? List<Map<String, Any>>

            captionsRaw?.mapNotNull { captionMap ->
                try {
                    Caption(
                        id = captionMap["id"] as? String,
                        url = captionMap["url"] as? String ?: return@mapNotNull null,
                        language = captionMap["lan"] as? String ?: "en",
                        languageName = captionMap["lanName"] as? String ?: "English"
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Caption fetch failed: ${e.message}")
            emptyList()
        }
    }
}
