package com.example.sports.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * A sports news article. Used by the News tab (list) and the article detail screen.
 * `relatedNews` powers the "Related news" rail at the bottom of an article.
 */
@JsonClass(generateAdapter = false)
data class News(
    @Json(name = "id") val id: String = "",
    @Json(name = "title") val title: String = "",
    /** Large hero / list image url. */
    @Json(name = "cover") val cover: Cover = Cover(),
    @Json(name = "author") val author: String = "",
    @Json(name = "createdAt") val date: String = "",
    @Json(name = "summary") val summary: String = "",
    @Json(name = "detailPath") val detailPath: String = "",
    @Json(name = "mediaType") val mediaType: String = "",
    @Json(name = "stat") val stat: NewsStat = NewsStat(),
    /** Tag the article was grouped under (from /news/trending-tag). */
    @Json(name = "tag") val tag: String = "",
    /** Full article body text (article detail screen only). */
    @Json(name = "content") val content: String = "",
    @Json(name = "relatedNews") val relatedNews: List<News> = emptyList(),
)

@JsonClass(generateAdapter = false)
data class Cover(
    @Json(name = "url") val url: String = "",
    @Json(name = "width") val width: Int = 0,
    @Json(name = "height") val height: Int = 0,
)

@JsonClass(generateAdapter = false)
data class NewsStat(
    @Json(name = "viewCount") val viewCount: Int = 0,
    @Json(name = "likeCount") val likeCount: Int = 0,
    @Json(name = "commentCount") val commentCount: Int = 0,
    @Json(name = "shareCount") val shareCount: Int = 0,
)
