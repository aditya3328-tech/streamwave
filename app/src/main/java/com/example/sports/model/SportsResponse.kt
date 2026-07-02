package com.example.sports.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Generic envelope returned by the wefeed sport BFF: `{ code, message, data }`.
 */
@JsonClass(generateAdapter = false)
data class SportsResponse<T>(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "message") val message: String = "",
    @Json(name = "data") val data: T? = null,
)

/**
 * A titled group of matches on the Sports Home screen, e.g. "Trending leagues",
 * "FIFA World Cup", "Premier League", "Cricket", "Basketball".
 */
@JsonClass(generateAdapter = false)
data class SportsSection(
    @Json(name = "title") val title: String = "",
    @Json(name = "matches") val matches: List<Match> = emptyList(),
)

/** A filter chip on the Sports Home screen ("All", "FIFA World Cup", "Premier League", …). */
@JsonClass(generateAdapter = false)
data class LeagueChip(
    val id: String = "",
    val label: String = "",
)

@JsonClass(generateAdapter = false)
data class MatchResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "message") val message: String = "",
    @Json(name = "data") val data: MatchData? = null,
)

@JsonClass(generateAdapter = false)
data class MatchData(
    @Json(name = "match") val match: Match = Match(),
    @Json(name = "newsList") val newsList: List<News> = emptyList(),
    @Json(name = "currentStage") val currentStage: CurrentStage = CurrentStage(),
)

@JsonClass(generateAdapter = false)
data class CurrentStage(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "status") val status: String = "",
)
