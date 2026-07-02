package com.example.sports.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * One row of a betting market — e.g. label "1" → value "2.605", label "X" → "2.45".
 */
@JsonClass(generateAdapter = false)
data class OddItem(
    @Json(name = "type") val type: Int = 0,
    @Json(name = "odds") val odds: Double = 0.0,
) {
    val label: String get() = when (type) {
        1 -> "1"
        2 -> "X"
        3 -> "2"
        else -> ""
    }

    val value: String get() = if (odds == 0.0) "" else odds.toString()
}

/**
 * The odds block attached to a [Match] (`match.oddsInfo`). [oddsList] drives the 1 / X / 2 rows
 * on the match card and the dedicated odds section on the details screen.
 */
@JsonClass(generateAdapter = false)
data class Odds(
    @Json(name = "source") val provider: String = "",
    @Json(name = "advertiserName") val advertiserName: String = "",
    @Json(name = "icon") val icon: String = "",
    @Json(name = "oddsList") val oddsList: List<OddItem> = emptyList(),
    /** Deep link opened by the "Bet Now" button. */
    @Json(name = "jumpUrl") val betUrl: String = "",
)
