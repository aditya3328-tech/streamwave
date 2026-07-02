package com.example.data.model

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json

data class HomeResponse(
    val code: Int,
    val message: String,
    val data: HomeData?
)

data class HomeData(
    val platformList: List<PlatformItem> = emptyList(),
    val operatingList: List<HomeSection> = emptyList()
)

data class PlatformItem(
    val name: String,
    val uploadBy: String?
)

@Immutable
data class HomeSection(
    val type: String, // "BANNER", "FILTER", "SPORT_LIVE", "SUBJECTS_MOVIE", "CUSTOM", "APPOINTMENT_LIST"
    val position: Int,
    val title: String,
    val subjects: List<Subject> = emptyList(),
    val banner: BannerData?,
    val opId: String?,
    val url: String?,
    val liveList: List<LiveMatch> = emptyList(),
    val filters: List<FilterItem> = emptyList(),
    val customData: CustomData?,
    val genreTopId: String?,
    val detailPath: String?
)

typealias Banner = BannerData

@Immutable
data class BannerData(
    val items: List<BannerItem> = emptyList()
)

@Immutable
data class BannerItem(
    val id: String,
    val title: String,
    val image: BannerImage?,
    val url: String?,
    val subjectId: String?,
    val subjectType: Int?,
    val subject: Subject?,
    val detailPath: String?
)

@Immutable
data class CustomData(
    val rowCount: Int?,
    val items: List<BannerItem> = emptyList(),
    val hiddenTitle: Boolean?
)

@Immutable
data class FilterItem(
    val title: String,
    val url: String?,
    val query: String?,
    val image: BannerImage?
)

@Immutable
data class LiveMatch(
    val matchId: String,
    val team1: Team?,
    val team2: Team?,
    val startTime: String?,
    val content: String?,
    val status: String?, // "MatchIng", "MatchNotStart" etc.
    val url: String?,
    val image: BannerImage?
)

@Immutable
data class Team(
    val id: String,
    val name: String,
    val score: String?,
    val avatar: String?,
    val voteCount: String?,
    val type: String?,
    val abbreviation: String?,
    val regularScore: String?,
    val nameTag: String?,
    val isVirtual: Boolean?
)

@Immutable
data class Subject(
    val subjectId: String,
    val subjectType: Int, // 1 = Movie, 2 = TV Show, 5 = Course, 7 = Short TV, 9 = Sport Replay
    val title: String,
    val description: String?,
    val releaseDate: String?,
    val duration: Long?,
    val genre: String?,
    val cover: BannerImage?,
    val countryName: String?,
    val imdbRatingValue: String?,
    val subtitles: String?,
    val ops: String?,
    val hasResource: Boolean?,
    val trailer: String?,
    val detailPath: String?,
    val appointmentCnt: Int?,
    val appointmentDate: String?,
    val corner: String?, // "Hindi", "Telugu", etc.
    val imdbRatingCount: Long?,
    val stills: BannerImage?,
    val postTitle: String?,
    val season: Int?,
    val dubs: List<String> = emptyList(),
    @Transient val staffList: Any? = null,
    @Transient val seasonInfo: Any? = null
)

typealias ImageObject = BannerImage

@Immutable
data class BannerImage(
    val url: String,
    val width: Int?,
    val height: Int?,
    val size: Long?,
    val format: String?,
    val thumbnail: String?,
    val blurHash: String?,
    val gif: String?,
    val avgHueLight: String?,
    val avgHueDark: String?,
    val id: String?
)
