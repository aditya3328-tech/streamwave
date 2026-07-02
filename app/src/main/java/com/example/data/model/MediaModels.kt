package com.example.data.model

import androidx.compose.runtime.Immutable
import com.squareup.moshi.JsonClass

@Immutable
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

@Immutable
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

@Immutable
data class StaffSummary(
    val id: String,
    val name: String,
    val role: String, // "Actor", "Director", etc.
    val character: String? = null,
    val profilePath: String
)

@Immutable
data class StaffDetails(
    val id: String,
    val name: String,
    val biography: String,
    val profilePath: String,
    val knownFor: String,
    val filmography: List<MediaSummary> = emptyList()
)

// --- Staff Detail (Person Profile) screen models ---

@Immutable
data class StaffProfileInfo(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val roles: String,        // e.g. "Actor • Director • Writer"
    val biography: String,
    val totalTitles: Int
)

@Immutable
data class RelatedCelebrity(
    val id: String,
    val name: String,
    val role: String,
    val avatarUrl: String
)

// One page of /staff/subject-list, plus the staff profile (page 1 only).
@Immutable
data class StaffFilmographyPage(
    val items: List<MediaSummary>,
    val totalTitles: Int,
    val hasMore: Boolean,
    val profile: StaffProfileInfo?
)

// Aggregate state backing the Staff Detail screen.
@Immutable
data class StaffPageData(
    val profile: StaffProfileInfo,
    val filmography: List<MediaSummary>,
    val related: List<RelatedCelebrity>,
    val totalTitles: Int,
    val hasMore: Boolean,
    val isLoadingMore: Boolean,
    val currentPage: Int
)

@Immutable
data class EpisodeSummary(
    val id: String,
    val number: Int,
    val title: String,
    val overview: String,
    val thumbnailPath: String,
    val runtime: String,
    val streamUrl: String
)

@Immutable
data class StreamPlayback(
    val mediaId: String,
    val episodeId: String?,
    val streamUrl: String,
    val format: String, // "hls", "mp4"
    val qualities: List<PlaybackQuality> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList()
)

@Immutable
data class PlaybackQuality(
    val label: String, // "1080p", "720p", "480p"
    val url: String
)

@Immutable
data class AudioTrack(
    val label: String,
    val languageCode: String
)

@Immutable
data class SubtitleTrack(
    val label: String,
    val url: String,
    val languageCode: String
)

data class ResolveStreamsResponse(
    val success: Boolean,
    val streams: List<StreamItem>,
    val selectedStream: StreamItem?
)

data class StreamItem(
    val url: String,
    val quality: String?,
    val type: String?,
    val provider: String?
)


