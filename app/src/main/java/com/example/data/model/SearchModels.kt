package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResponse(
    val code: Int? = null,
    val msg: String? = null,
    val data: SearchData? = null
)

@JsonClass(generateAdapter = true)
data class SearchData(
    val items: List<SearchItem>? = null,
    val count: SearchCount? = null,
    val pager: SearchPager? = null
)

@JsonClass(generateAdapter = true)
data class SearchCount(
    val all: Int? = null,
    val movies: Int? = null,
    val series: Int? = null,
    val anime: Int? = null
)

@JsonClass(generateAdapter = true)
data class SearchPager(
    val pageNum: Int? = null,
    val pageSize: Int? = null,
    val totalCount: Int? = null,
    val isLast: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class SearchItem(
    val subjectId: String,
    val subjectType: Int,
    val title: String,
    val cover: SearchCover? = null,
    val countryName: String? = null,
    val imdbRatingValue: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val detailPath: String? = null,
    val corner: String? = null
)

@JsonClass(generateAdapter = true)
data class SearchCover(
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class SearchSuggestionResponse(
    val code: Int? = null,
    val msg: String? = null,
    val data: SearchSuggestionData? = null
)

@JsonClass(generateAdapter = true)
data class SearchSuggestionData(
    val items: List<SearchSuggestionItem>? = null
)

@JsonClass(generateAdapter = true)
data class SearchSuggestionItem(
    val type: Int? = null,
    val word: String? = null
)

typealias SearchSuggestResponse = SearchSuggestionResponse

@JsonClass(generateAdapter = true)
data class SearchSuggestRequest(
    val keyword: String
)

@JsonClass(generateAdapter = true)
data class SearchRequest(
    val keyword: String,
    val page: Int = 1,
    val perPage: Int = 28,
    val subjectType: Int = 0
)

@JsonClass(generateAdapter = true)
data class EveryoneSearchRequest(
    val page: Int = 1,
    val perPage: Int = 20
)

@JsonClass(generateAdapter = true)
data class FilterRequest(
    val subjectType: Int,
    val genre: String? = null,
    val country: String? = null,
    val year: String? = null,
    val language: String? = null,
    val sort: String? = null,
    val page: Int = 1,
    val perPage: Int = 20
)

