package com.example.sports.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sports.player.SportsPlayerScreen

/**
 * Self-contained navigation graph for the Sports mini-app. Owns its OWN [NavHostController],
 * fully separate from the movie [com.example.ui.navigation.AppNavigation] graph. Rendered inside
 * the SPORTS bottom tab.
 *
 * Routes: home → match details → live player, and home(News) → article.
 */
object SportsRoute {
    const val Home = "sports_home"
    const val Details = "sports_match/{matchId}"
    const val Player = "sports_player/{matchId}"
    const val Article = "sports_article/{articleId}"

    fun details(id: String) = "sports_match/$id"
    fun player(id: String) = "sports_player/$id"
    fun article(id: String) = "sports_article/$id"
}

@Composable
fun SportsRoot(modifier: Modifier = Modifier) {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = SportsRoute.Home,
        modifier = modifier.fillMaxSize().background(SportsBg),
    ) {
        composable(SportsRoute.Home) {
            SportsHomeScreen(
                onMatchClick = { nav.navigate(SportsRoute.details(it)) },
                onArticleClick = { nav.navigate(SportsRoute.article(it)) },
            )
        }

        composable(
            route = SportsRoute.Details,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("matchId").orEmpty()
            MatchDetailsScreen(
                matchId = id,
                onBack = { nav.popBackStack() },
                onOpenPlayer = { nav.navigate(SportsRoute.player(it)) },
                onArticleClick = { nav.navigate(SportsRoute.article(it)) },
            )
        }

        composable(
            route = SportsRoute.Player,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("matchId").orEmpty()
            SportsPlayerScreen(matchId = id, onBack = { nav.popBackStack() })
        }

        composable(
            route = SportsRoute.Article,
            arguments = listOf(navArgument("articleId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("articleId").orEmpty()
            SportsArticleScreen(
                articleId = id,
                onBack = { nav.popBackStack() },
                onArticleClick = { nav.navigate(SportsRoute.article(it)) },
            )
        }
    }
}
