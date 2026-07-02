package com.example.data.model

import androidx.compose.runtime.Immutable
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class CoverUrl(
    val url: String?,
    val width: Int? = null,
    val height: Int? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class SubjectModel(
    val subjectId: String,
    val subjectType: Int? = null,
    val title: String,
    val description: String? = null,
    val releaseDate: String? = null,
    val duration: Int? = null,
    val genre: String? = null,
    val cover: CoverUrl? = null,
    val imdbRatingValue: String? = null,
    val trailer: TrailerModel? = null,
    val detailPath: String? = null,
    // The backend returns the available audio/subtitle languages nested inside the
    // subject object (data.subject.dubs), NOT at the top-level data.dubs.
    val dubs: List<DubModel> = emptyList()
)

@JsonClass(generateAdapter = true)
data class VideoAddress(
    val videoId: String? = null,
    val url: String? = null,
    val duration: Int? = null,
    val width: Int? = null,
    val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class TrailerModel(
    val videoAddress: VideoAddress? = null,
    val cover: CoverUrl? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class DubModel(
    val subjectId: String,
    val lanName: String? = null,
    val lanCode: String? = null,
    val original: Boolean? = null,
    val type: Int? = null,
    val detailPath: String? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class StarModel(
    val staffId: String,
    val staffType: Int? = null,
    val name: String,
    val character: String? = null,
    val avatarUrl: String? = null,
    val detailPath: String? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class ResolutionModel(
    val resolution: Int? = null,
    val epNum: Int? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class SeasonModel(
    val se: Int? = null,
    val maxEp: Int? = null,
    val allEp: String? = null,
    val resolutions: List<ResolutionModel>? = null
)

@JsonClass(generateAdapter = true)
data class ResourceModel(
    val seasons: List<SeasonModel>? = null,
    val source: String? = null,
    val uploadBy: String? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class UserCommentModel(
    val userId: String,
    val nickname: String? = null,
    val avatar: String? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class CommentItemModel(
    val postId: String,
    val userId: String,
    val content: String? = null,
    val createdAt: String? = null,
    val user: UserCommentModel? = null
)

@JsonClass(generateAdapter = true)
data class PostListModel(
    val items: List<CommentItemModel>? = null
)

@JsonClass(generateAdapter = true)
data class DetailData(
    val subject: SubjectModel,
    val countryName: String? = null,
    val imdbRatingValue: String? = null,
    val subtitles: String? = null,
    val hasResource: Boolean? = null,
    val trailer: TrailerModel? = null,
    val imdbRatingCount: Int? = null,
    val stars: List<StarModel>? = null,
    val resource: ResourceModel? = null,
    val postList: PostListModel? = null
)

@JsonClass(generateAdapter = true)
data class DetailResponse(
    val code: Int,
    val message: String,
    val data: DetailData? = null
)

// Recommendations Models
@JsonClass(generateAdapter = true)
data class RecItemModel(
    val subjectId: String,
    val subjectType: Int? = null,
    val title: String,
    val description: String? = null,
    val releaseDate: String? = null,
    val duration: Int? = null,
    val genre: String? = null,
    val cover: CoverUrl? = null
)

@JsonClass(generateAdapter = true)
data class RecData(
    val items: List<RecItemModel>? = null
)

@JsonClass(generateAdapter = true)
data class RecResponse(
    val code: Int,
    val message: String,
    val data: RecData? = null
)
