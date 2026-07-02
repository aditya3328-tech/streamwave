package com.example.sports.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * One selectable live stream / TV channel for a match (`match.playSource[]`).
 * The player's channel chips ("Channel 1/2/3") are built from this list.
 */
@JsonClass(generateAdapter = false)
data class PlaySource(
    @Json(name = "title") val name: String = "",
    @Json(name = "path") val url: String = "",
    @Json(name = "id") val id: String = "",
)

/** Coarse match lifecycle state used for the LIVE / Upcoming / Finished badge. */
enum class MatchStatus { LIVE, UPCOMING, FINISHED;

    companion object {
        fun from(raw: String?): MatchStatus = when (raw?.trim()?.lowercase()) {
            "live", "1", "playing", "inprogress", "in_progress" -> LIVE
            "finished", "end", "ended", "2", "ft", "full_time" -> FINISHED
            else -> UPCOMING
        }
    }
}

/**
 * The central Sports domain object — a single fixture. This is the standalone counterpart of a
 * movie "media" item but shares NOTHING with the movie models.
 *
 * UI bindings (per spec):
 *   match.matchRound, team1/team2 (.name/.avatar/.score), oddsInfo.oddsList,
 *   match.highlights[], match.playPath, match.playSource[].
 */
@JsonClass(generateAdapter = false)
data class Match(
    @Json(name = "id") val id: String = "",
    /** e.g. "FIFA World Cup Group D Round 3". */
    @Json(name = "matchRound") val matchRound: String = "",
    @Json(name = "league") val league: String = "",
    /** Raw status string from the API; normalize via [status]. */
    @Json(name = "status") val matchStatus: String = "",
    /** Kick-off / current clock label, e.g. "45'", "20:00", "Today 21:00". */
    @Json(name = "startTime") val matchTime: String = "",

    @Json(name = "team1") val team1: Team = Team(),
    @Json(name = "team2") val team2: Team = Team(),

    @Json(name = "oddsInfo") val oddsInfo: Odds = Odds(),
    @Json(name = "highlights") val highlights: List<Highlight> = emptyList(),

    /** Primary HLS playlist url for the live stream. */
    @Json(name = "playPath") val playPath: String = "",
    /** Alternate channels/streams selectable in the player. */
    @Json(name = "playSource") val playSource: List<PlaySource> = emptyList(),

    /** Fan support split for the "Fans Supported" bar (0..100). */
    @Json(name = "homeSupport") val homeSupport: Int = 50,
    @Json(name = "awaySupport") val awaySupport: Int = 50,
) {
    val status: MatchStatus get() = MatchStatus.from(matchStatus)
}
