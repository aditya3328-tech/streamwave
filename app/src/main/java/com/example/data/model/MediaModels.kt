package com.example.data.model

import com.squareup.moshi.JsonClass

data class MediaSummary(
    val id: String,
    val title: String,
    val posterPath: String,
    val backdropPath: String,
    val mediaType: String, // "movie", "tv", "anime"
    val rating: Double,
    val year: String,
    val genres: List<String> = emptyList()
)

data class MediaDetails(
    val id: String,
    val title: String,
    val posterPath: String,
    val backdropPath: String,
    val mediaType: String,
    val rating: Double,
    val year: String,
    val country: String,
    val language: String,
    val genres: List<String>,
    val runtime: String,
    val description: String,
    val cast: List<StaffSummary> = emptyList(),
    val crew: List<StaffSummary> = emptyList(),
    val trailerUrl: String? = null,
    val episodes: List<EpisodeSummary> = emptyList(),
    val stars: List<StarModel> = emptyList(),
    val dubs: List<DubModel> = emptyList(),
    val seasons: List<SeasonModel> = emptyList(),
    val comments: List<CommentItemModel> = emptyList(),
    val trailerVideoAddressUrl: String? = null,
    val detailPath: String? = null
)

data class StaffSummary(
    val id: String,
    val name: String,
    val role: String, // "Actor", "Director", etc.
    val character: String? = null,
    val profilePath: String
)

data class StaffDetails(
    val id: String,
    val name: String,
    val biography: String,
    val profilePath: String,
    val knownFor: String,
    val filmography: List<MediaSummary> = emptyList()
)

data class EpisodeSummary(
    val id: String,
    val number: Int,
    val title: String,
    val overview: String,
    val thumbnailPath: String,
    val runtime: String,
    val streamUrl: String
)

data class StreamPlayback(
    val mediaId: String,
    val episodeId: String?,
    val streamUrl: String,
    val format: String, // "hls", "mp4"
    val qualities: List<PlaybackQuality> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList()
)

data class PlaybackQuality(
    val label: String, // "1080p", "720p", "480p"
    val url: String
)

data class AudioTrack(
    val label: String,
    val languageCode: String
)

data class SubtitleTrack(
    val label: String,
    val url: String,
    val languageCode: String
)

@JsonClass(generateAdapter = true)
data class ResolveStreamsResponse(
    val streams: List<ResolveStreamItem>? = null,
    val captions: List<ResolveCaptionItem>? = null
)

@JsonClass(generateAdapter = true)
data class ResolveStreamItem(
    val url: String?,
    val link: String? = null,
    val streamUrl: String? = null,
    val quality: String? = null,
    val resolution: String? = null,
    val resolutions: String? = null,
    val label: String? = null,
    val format: String? = null
) {
    fun getResolvedUrl(): String {
        return url ?: link ?: streamUrl ?: ""
    }

    fun getResolvedQuality(): String {
        return quality ?: resolution ?: resolutions ?: label ?: "Auto"
    }

    fun getResolvedFormat(): String {
        val u = getResolvedUrl()
        if (format != null) return format
        if (u.contains(".m3u8", ignoreCase = true)) return "hls"
        return "mp4"
    }
}

@JsonClass(generateAdapter = true)
data class ResolveCaptionItem(
    val url: String?,
    val link: String? = null,
    val lang: String? = null,
    val language: String? = null,
    val label: String? = null
) {
    fun getResolvedUrl(): String {
        return url ?: link ?: ""
    }

    fun getResolvedLanguageCode(): String {
        return lang ?: language ?: "en"
    }

    fun getResolvedLabel(): String {
        return label ?: getResolvedLanguageCode().uppercase()
    }
}

@JsonClass(generateAdapter = true)
data class MovieBoxPlayResponse(
    val code: Int? = null,      // 0 = success; anything else = API-level error
    val message: String? = null, // "ok" on success
    val data: MovieBoxPlayData? = null
)

@JsonClass(generateAdapter = true)
data class MovieBoxPlayData(
    val streams: List<ResolveStreamItem>? = null,
    val captions: List<ResolveCaptionItem>? = null
)


