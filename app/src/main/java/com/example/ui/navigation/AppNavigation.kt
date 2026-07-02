package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.AMOLED_BLACK
import com.example.ui.components.CRUNCHY_ORANGE
import com.example.ui.components.DEEP_GREY
import com.example.ui.screens.*
import com.example.ui.screens.movie.MovieTabScreen
import com.example.ui.viewmodel.CineVerseViewModel
import com.example.ui.viewmodel.MovieTabViewModel
import com.example.sports.ui.SportsRoot

object Screen {
    const val TabsShell = "tabs_shell"
    const val Details = "details/{mediaId}"
    const val Staff = "staff/{staffId}"
    const val Player = "player/{mediaId}?episodeId={episodeId}"

    fun details(id: String) = "details/$id"
    fun staff(id: String) = "staff/$id"
    fun player(mediaId: String, episodeId: String? = null): String {
        return if (episodeId != null) {
            "player/$mediaId?episodeId=$episodeId"
        } else {
            "player/$mediaId"
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: CineVerseViewModel,
    movieTabViewModel: MovieTabViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.TabsShell,
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Bottom Tabs Shell Screen
        composable(Screen.TabsShell) {
            TabsShellScreen(
                viewModel = viewModel,
                movieTabViewModel = movieTabViewModel,
                onNavigateToDetails = { id ->
                    navController.navigate(Screen.details(id))
                },
                onNavigateToPlayer = { mId, epId ->
                    navController.navigate(Screen.player(mId, epId))
                }
            )
        }

        // 2. Movie/Show Details Screen
        composable(
            route = Screen.Details,
            arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
            DetailsScreen(
                mediaId = mediaId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { mId, epId ->
                    navController.navigate(Screen.player(mId, epId))
                },
                onNavigateToStaff = { sId ->
                    navController.navigate(Screen.staff(sId))
                },
                onNavigateToDetails = { mId ->
                    navController.navigate(Screen.details(mId))
                }
            )
        }

        // 3. Cast/Staff Profile Screen
        composable(
            route = Screen.Staff,
            arguments = listOf(navArgument("staffId") { type = NavType.StringType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getString("staffId") ?: ""
            StaffScreen(
                staffId = staffId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { mId ->
                    navController.navigate(Screen.details(mId))
                },
                onNavigateToStaff = { sId ->
                    navController.navigate(Screen.staff(sId))
                }
            )
        }

        // 4. Custom Advanced ExoPlayer Screen (Supports optional EpisodeId)
        composable(
            route = Screen.Player,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("episodeId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
            val episodeId = backStackEntry.arguments?.getString("episodeId")
            PlayerScreen(
                mediaId = mediaId,
                episodeId = episodeId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { mId ->
                    navController.navigate(Screen.details(mId))
                },
                onNavigateToPlayer = { mId, epId ->
                    navController.navigate(Screen.player(mId, epId))
                },
                onNavigateToStaff = { sId ->
                    navController.navigate(Screen.staff(sId))
                }
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TabsShellScreen(
    viewModel: CineVerseViewModel,
    movieTabViewModel: MovieTabViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToPlayer: (String, String?) -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DEEP_GREY,
                contentColor = Color.LightGray,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                // Visible order: Home, Movies, Explore, Search, Library.
                // NOTE: each item's index (selected / selectTab) still maps to its
                // screen in the `when (tabIndex)` block below — only the DECLARATION
                // ORDER changed, so routing is unaffected.
                //   0 = Home, 1 = Search, 2 = Movies, 3 = Explore, 4 = Library, 5 = Sports

                // Bottom Tab 1: Home  (screen index 0)
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = CRUNCHY_ORANGE,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                // Bottom Tab 2: Movies  (screen index 2)
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(imageVector = Icons.Default.Movie, contentDescription = "Movies") },
                    label = { Text("Movies", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = CRUNCHY_ORANGE,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                // Bottom Tab 3: Explore  (screen index 3)
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(imageVector = Icons.Default.Tv, contentDescription = "Explore") },
                    label = { Text("Explore", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = CRUNCHY_ORANGE,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                // Bottom Tab 4: Search  (screen index 1)
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = CRUNCHY_ORANGE,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                // Bottom Tab 5: Library  (screen index 4)
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { viewModel.selectTab(4) },
                    icon = { Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = "Library") },
                    label = { Text("Library", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = CRUNCHY_ORANGE,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                // Sports (screen index 5) is intentionally HIDDEN from the bottom
                // navigation for now. The NavigationBarItem is removed, but ALL Sports
                // code — the `5 -> SportsRoot(...)` branch below, SportsRoot, its APIs,
                // repositories, ViewModels, routes and screens — is left fully intact.
            }
        }
    ) { innerPadding ->
        // Render corresponding Screen centered on StateFlow active variables
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "tab_switches"
        ) { tabIndex ->
            val scopeModifier = Modifier.padding(innerPadding)
            when (tabIndex) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = onNavigateToDetails,
                    onNavigateToPlayer = onNavigateToPlayer,
                    modifier = scopeModifier
                )
                1 -> SearchScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = onNavigateToDetails,
                    modifier = scopeModifier
                )
                2 -> MovieTabScreen(
                    viewModel = movieTabViewModel,
                    onNavigateToDetails = onNavigateToDetails,
                    onNavigateToPlayer = onNavigateToPlayer,
                    modifier = scopeModifier
                )
                3 -> ExploreScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = onNavigateToDetails,
                    modifier = scopeModifier
                )
                4 -> LibraryScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = onNavigateToDetails,
                    modifier = scopeModifier
                )
                // Sports mini-app — its own nested NavHost; no movie callbacks are threaded in.
                5 -> SportsRoot(modifier = scopeModifier)
            }
        }
    }
}
