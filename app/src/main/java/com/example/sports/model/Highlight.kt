package com.example.sports.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * A short video highlight clip surfaced on the match card ("Highlights") and in the
 * match-details highlights carousel (`match.highlights[]`).
 */
@JsonClass(generateAdapter = false)
data class Highlight(
    @Json(name = "id") val id: String = "",
    @Json(name = "title") val title: String = "",
    /** Thumbnail / poster image url. */
    @Json(name = "cover") val cover: Cover = Cover(),
    /** Playable clip url (HLS or mp4). */
    @Json(name = "path") val url: String = "",
    @Json(name = "duration") val duration: String = "",
    @Json(name = "createTime") val createTime: String = "",
)
