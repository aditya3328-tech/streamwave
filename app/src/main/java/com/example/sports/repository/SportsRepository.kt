package com.example.sports.repository

import android.util.Log
import com.example.sports.api.SportsApiService
import com.example.sports.api.SportsNetworkModule
import com.example.sports.model.Match
import com.example.sports.model.News
import com.example.sports.model.SportsSection
import java.util.concurrent.ConcurrentHashMap

/**
 * Single source of truth for the Sports module. Talks ONLY to [SportsApiService] and never to
 * any movie repository, API or DAO.
 *
 * Every call degrades gracefully: if the live BFF is unreachable or returns an unexpected
 * shape, the repository falls back to [SportsSampleData] so the screens always render the
 * intended layout. Matches fetched from any call are cached by id so detail/player screens can
 * resolve a fixture without a round-trip.
 */
class SportsRepository(
    private val api: SportsApiService = SportsNetworkModule.api,
) {
    private val matchCache = ConcurrentHashMap<String, Match>()
    private val matchNewsCache = ConcurrentHashMap<String, List<News>>()

    private fun cache(matches: List<Match>) {
        matches.forEach { if (it.id.isNotBlank()) matchCache[it.id] = it }
    }

    suspend fun getHomeSections(): List<SportsSection> = runCatchingSports("home") {
        val data = api.getHomeSections(mapOf("page" to 1, "perPage" to 20)).data
        data?.takeIf { it.isNotEmpty() } ?: SportsSampleData.sections
    }.also { sections -> cache(sections.flatMap { it.matches }) }

    suspend fun getMatches(tag: String? = null): List<Match> = runCatchingSports("matches") {
        val body = buildMap<String, Any?> {
            put("page", 1)
            put("perPage", 30)
            if (!tag.isNullOrBlank() && tag != "all") put("tag", tag)
        }
        api.getMatches(body).data?.takeIf { it.isNotEmpty() } ?: SportsSampleData.allMatches
    }.also { cache(it) }

    suspend fun getMatch(id: String): Match {
        matchCache[id]?.let { return it }
        return runCatchingSports("match-detail") {
            val detail = api.getMatchDetails(id).data
            detail?.newsList?.takeIf { it.isNotEmpty() }?.let { matchNewsCache[id] = it }
            detail?.match
                ?: SportsSampleData.allMatches.firstOrNull { it.id == id }
                ?: SportsSampleData.allMatches.first()
        }.also { cache(listOf(it)) }
    }

    suspend fun getMatchNews(id: String): List<News> {
        matchNewsCache[id]?.let { return it }
        return runCatchingSports("match-news") {
            val detail = api.getMatchDetails(id).data
            detail?.match?.let { cache(listOf(it)) }
            detail?.newsList?.takeIf { it.isNotEmpty() }
                ?: getTrendingNews().take(4)
        }.also { matchNewsCache[id] = it }
    }

    suspend fun getTrendingNews(): List<News> = runCatchingSports("news") {
        api.getTrendingNews(mapOf("page" to 1, "perPage" to 20)).data
            ?.takeIf { it.isNotEmpty() } ?: SportsSampleData.news
    }

    suspend fun getArticle(id: String): News = runCatchingSports("article") {
        val remote = api.getNewsDetail(mapOf("id" to id, "newsId" to id)).data
        val article = remote ?: SportsSampleData.news.firstOrNull { it.id == id }
            ?: SportsSampleData.news.first()
        // Always provide a related rail (other trending stories) if the API omitted one.
        if (article.relatedNews.isEmpty()) {
            article.copy(relatedNews = SportsSampleData.news.filter { it.id != article.id }.take(4))
        } else {
            article
        }
    }

    private inline fun <T> runCatchingSports(tag: String, block: () -> T): T = try {
        block()
    } catch (e: Exception) {
        Log.w("SPORTS_REPO", "[$tag] live fetch failed, using sample data: ${e.message}")
        @Suppress("UNCHECKED_CAST")
        when (tag) {
            "home" -> SportsSampleData.sections as T
            "matches" -> SportsSampleData.allMatches as T
            "news" -> SportsSampleData.news as T
            "match-news" -> SportsSampleData.news.take(4) as T
            "match-detail" -> SportsSampleData.allMatches.first() as T
            "article" -> SportsSampleData.news.first().copy(
                relatedNews = SportsSampleData.news.drop(1).take(4),
            ) as T
            else -> throw e
        }
    }

    companion object {
        /** Shared default instance; the Sports module is a self-contained mini-app. */
        val instance: SportsRepository by lazy { SportsRepository() }
    }
}
