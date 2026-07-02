package com.example.sports.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * A single competing team/side in a [Match]. Fully independent of any movie model.
 *
 * Field names follow the SportsNow / aoneroom sport API. Where the upstream key is uncertain,
 * the most likely name is used and the reflective Moshi adapter falls back to the Kotlin
 * default so a missing field never crashes the screen.
 */
@JsonClass(generateAdapter = false)
data class Team(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    /** Crest / logo image url. Bound to `team1.avatar` / `team2.avatar` in the UI spec. */
    @Json(name = "avatar") val avatar: String = "",
    @Json(name = "score") val score: String = "0",
    @Json(name = "voteCount") val voteCount: String = "",
    /** Optional short country / club code, e.g. "PAR", "AUS". */
    @Json(name = "abbreviation") val shortName: String = "",
)
