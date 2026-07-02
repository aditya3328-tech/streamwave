package com.example.data.model.movie

import com.example.data.model.BannerImage
import com.example.data.model.BannerItem
import com.example.data.model.HomeSection
import com.example.data.model.Subject
import com.google.gson.JsonElement

data class MovieTabOperatingResponse(
    val code: Int = 0,
    val message: String = "",
    val data: MovieTabOperatingData? = null,
)

data class MovieTabOperatingData(
    val operatingList: List<HomeSection> = emptyList(),
)

data class MovieTrendingResponse(
    val code: Int = 0,
    val message: String = "",
    val data: MovieTrendingData? = null,
)

data class MovieTrendingData(
    val items: List<MovieSubject> = emptyList(),
    val subjects: List<MovieSubject> = emptyList(),
    val list: List<MovieSubject> = emptyList(),
) {
    val allItems: List<MovieSubject>
        get() = when {
            items.isNotEmpty() -> items
            subjects.isNotEmpty() -> subjects
            else -> list
        }
}

data class MovieSubject(
    val subjectId: String = "",
    val subjectType: Int = 1,
    val title: String = "",
    val description: String? = null,
    val cover: BannerImage? = null,
    val detailPath: String? = null,
    val genre: String? = null,
    val countryName: String? = null,
    val releaseDate: String? = null,
    val duration: Long? = null,
    val imdbRatingValue: String? = null,
    val corner: String? = null,
    val hasResource: Boolean? = null,
    val staffList: JsonElement? = null,
    val seasonInfo: JsonElement? = null,
    val subtitles: String? = null,
    val ops: String? = null,
    val trailer: JsonElement? = null,
    val appointmentCnt: Int? = null,
    val appointmentDate: String? = null,
    val imdbRatingCount: Long? = null,
    val stills: BannerImage? = null,
    val postTitle: String? = null,
    val season: Int? = null,
    val dubs: List<String> = emptyList(),
) {
    fun toSubject(): Subject = Subject(
        subjectId = subjectId,
        subjectType = subjectType,
        title = title,
        description = description,
        releaseDate = releaseDate,
        duration = duration,
        genre = genre,
        cover = cover,
        countryName = countryName,
        imdbRatingValue = imdbRatingValue,
        subtitles = subtitles,
        ops = ops,
        hasResource = hasResource,
        trailer = trailer?.asStringOrJson(),
        detailPath = detailPath,
        appointmentCnt = appointmentCnt,
        appointmentDate = appointmentDate,
        corner = corner,
        imdbRatingCount = imdbRatingCount,
        stills = stills,
        postTitle = postTitle,
        season = season,
        dubs = dubs,
        staffList = staffList,
        seasonInfo = seasonInfo,
    )
}

private fun JsonElement.asStringOrJson(): String =
    if (isJsonPrimitive && asJsonPrimitive.isString) asString else toString()

data class MovieTabUiData(
    val banners: List<BannerItem>,
    val sections: List<HomeSection>,
)
