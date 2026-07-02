package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaSummary
import com.example.ui.components.*
import com.example.ui.viewmodel.CineVerseViewModel
import kotlinx.coroutines.launch

// Reusable Top Filter Pills Component
@Composable
fun FilterPillRow(
    modifier: Modifier = Modifier,
    pills: List<Pair<String, String>>,
    onPillClick: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(pills, key = { it.first }) { pill ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DEEP_GREY)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onPillClick(pill.first) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${pill.first}: ${pill.second}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "▼",
                        color = Color.Gray,
                        fontSize = 8.sp
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CRUNCHY_ORANGE)
                    .clickable { onFilterClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "All Filters",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Filters",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Reusable Bottom Sheet Category Chips Wrapper
@Composable
fun FilterCategorySection(
    title: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(options, key = { it }) { option ->
                val isSelected = option == selectedOption
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) CRUNCHY_ORANGE else DEEP_GREY)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) CRUNCHY_ORANGE else Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelect(option) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = option,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Reusable Full Filter Bottom Sheet Content
@Composable
fun FilterBottomSheetContent(
    title: String,
    availableGenres: List<String>? = null,
    selectedGenre: String? = null,
    onSelectGenre: ((String) -> Unit)? = null,
    
    availableCountries: List<String>,
    selectedCountry: String,
    onSelectCountry: (String) -> Unit,
    
    availableYears: List<String>,
    selectedYear: String,
    onSelectYear: (String) -> Unit,
    
    availableLanguages: List<String>? = null,
    selectedLanguage: String? = null,
    onSelectLanguage: ((String) -> Unit)? = null,
    
    availableSortBys: List<String> = listOf("Popularity", "Rating", "Newest"),
    selectedSortBy: String,
    onSelectSortBy: (String) -> Unit,
    
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMOLED_BLACK)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter or Sort $title",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (availableGenres != null && selectedGenre != null && onSelectGenre != null) {
                item {
                    FilterCategorySection(
                        title = "Genre",
                        options = availableGenres,
                        selectedOption = selectedGenre,
                        onSelect = onSelectGenre
                    )
                }
            }

            item {
                FilterCategorySection(
                    title = "Country",
                    options = availableCountries,
                    selectedOption = selectedCountry,
                    onSelect = onSelectCountry
                )
            }

            item {
                FilterCategorySection(
                    title = "Year",
                    options = availableYears,
                    selectedOption = selectedYear,
                    onSelect = onSelectYear
                )
            }

            if (availableLanguages != null && selectedLanguage != null && onSelectLanguage != null) {
                item {
                    FilterCategorySection(
                        title = "Language",
                        options = availableLanguages,
                        selectedOption = selectedLanguage,
                        onSelect = onSelectLanguage
                    )
                }
            }

            item {
                FilterCategorySection(
                    title = "Sort By",
                    options = availableSortBys,
                    selectedOption = selectedSortBy,
                    onSelect = onSelectSortBy
                )
            }
        }

        Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onReset,
                modifier = Modifier.weight(1f).testTag("filter_reset_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DEEP_GREY,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Reset", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onApply,
                modifier = Modifier.weight(1f).testTag("filter_apply_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CRUNCHY_ORANGE,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 1. Movies Filter Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
    viewModel: CineVerseViewModel,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.moviesList.collectAsState()
    val isLoading by viewModel.isMoviesLoading.collectAsState()

    // Persistent filters
    val selectedGenre by viewModel.movieSelectedGenre.collectAsState()
    val selectedCountry by viewModel.movieSelectedCountry.collectAsState()
    val selectedYear by viewModel.movieSelectedYear.collectAsState()
    val selectedLanguage by viewModel.movieSelectedLanguage.collectAsState()
    val selectedSortBy by viewModel.movieSelectedSortBy.collectAsState()

    // Temporary local selection states for bottom sheet
    var tempGenre by remember { mutableStateOf(selectedGenre) }
    var tempCountry by remember { mutableStateOf(selectedCountry) }
    var tempYear by remember { mutableStateOf(selectedYear) }
    var tempLanguage by remember { mutableStateOf(selectedLanguage) }
    var tempSortBy by remember { mutableStateOf(selectedSortBy) }

    // Sync temp states if globals change
    LaunchedEffect(selectedGenre, selectedCountry, selectedYear, selectedLanguage, selectedSortBy) {
        tempGenre = selectedGenre
        tempCountry = selectedCountry
        tempYear = selectedYear
        tempLanguage = selectedLanguage
        tempSortBy = selectedSortBy
    }

    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContainerColor = AMOLED_BLACK,
        sheetContentColor = Color.White,
        modifier = modifier,
        sheetContent = {
            FilterBottomSheetContent(
                title = "Movies",
                availableGenres = listOf("Sci-Fi", "Action", "Drama", "Animation", "All"),
                selectedGenre = tempGenre,
                onSelectGenre = { tempGenre = it },
                
                availableCountries = listOf("United States", "India", "Korea", "Nigeria", "All"),
                selectedCountry = tempCountry,
                onSelectCountry = { tempCountry = it },
                
                availableYears = listOf("2026", "2025", "2024", "2023", "All"),
                selectedYear = tempYear,
                onSelectYear = { tempYear = it },
                
                availableLanguages = listOf("English", "Hindi", "Korean", "All"),
                selectedLanguage = tempLanguage,
                onSelectLanguage = { tempLanguage = it },
                
                selectedSortBy = tempSortBy,
                onSelectSortBy = { tempSortBy = it },
                
                onApply = {
                    coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() }
                    viewModel.applyMovieFilters(tempGenre, tempCountry, tempYear, tempLanguage, tempSortBy)
                },
                onReset = {
                    tempGenre = "All"
                    tempCountry = "All"
                    tempYear = "All"
                    tempLanguage = "All"
                    tempSortBy = "Popularity"
                    coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() }
                    viewModel.resetMovieFilters()
                }
            )
        }
    ) { innerPadding ->
        AMOLEDCanvas(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Explore Movies",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // OTT Pill row
                val activePills = listOf(
                    "Genre" to selectedGenre,
                    "Year" to selectedYear,
                    "Country" to selectedCountry
                )
                FilterPillRow(
                    pills = activePills,
                    onPillClick = {
                        coroutineScope.launch { scaffoldState.bottomSheetState.expand() }
                    },
                    onFilterClick = {
                        coroutineScope.launch { scaffoldState.bottomSheetState.expand() }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom states
                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CRUNCHY_ORANGE)
                    }
                } else if (movies.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No results found",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No items found for genre '$selectedGenre', country '$selectedCountry', and year '$selectedYear'. Try clearing filter categories.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val gridState = rememberLazyGridState()
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(movies, key = { it.id }) { media ->
                            MovieCard(
                                media = media,
                                onClick = { onNavigateToDetails(media.id) },
                                modifier = Modifier.testTag("movie_item_card")
                            )
                        }
                    }
                }
            }
        }
    }
}

// 3. Animation Filter Screen (Crunchy Anime Portal expanded)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimationScreen(
    viewModel: CineVerseViewModel,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val animeList by viewModel.animeList.collectAsState()
    val isLoading by viewModel.isAnimeLoading.collectAsState()

    // Persistent filters
    val selectedCountry by viewModel.animeSelectedCountry.collectAsState()
    val selectedYear by viewModel.animeSelectedYear.collectAsState()
    val selectedSortBy by viewModel.animeSelectedSortBy.collectAsState()

    // Temporary local selection states for bottom sheet
    var tempCountry by remember { mutableStateOf(selectedCountry) }
    var tempYear by remember { mutableStateOf(selectedYear) }
    var tempSortBy by remember { mutableStateOf(selectedSortBy) }

    // Sync temp states if globals change
    LaunchedEffect(selectedCountry, selectedYear, selectedSortBy) {
        tempCountry = selectedCountry
        tempYear = selectedYear
        tempSortBy = selectedSortBy
    }

    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContainerColor = AMOLED_BLACK,
        sheetContentColor = Color.White,
        modifier = modifier,
        sheetContent = {
            FilterBottomSheetContent(
                title = "Animation",
                availableCountries = listOf("Japan", "United States", "All"),
                selectedCountry = tempCountry,
                onSelectCountry = { tempCountry = it },
                
                availableYears = listOf("2026", "2025", "2024", "2023", "All"),
                selectedYear = tempYear,
                onSelectYear = { tempYear = it },
                
                selectedSortBy = tempSortBy,
                onSelectSortBy = { tempSortBy = it },
                
                onApply = {
                    coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() }
                    viewModel.applyAnimeFilters(tempCountry, tempYear, tempSortBy)
                },
                onReset = {
                    tempCountry = "All"
                    tempYear = "All"
                    tempSortBy = "Popularity"
                    coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() }
                    viewModel.resetAnimeFilters()
                }
            )
        }
    ) { innerPadding ->
        AMOLEDCanvas(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Crunchy Anime Portal",
                        color = CRUNCHY_ORANGE,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // OTT Pill row
                val activePills = listOf(
                    "Country" to selectedCountry,
                    "Year" to selectedYear
                )
                FilterPillRow(
                    pills = activePills,
                    onPillClick = {
                        coroutineScope.launch { scaffoldState.bottomSheetState.expand() }
                    },
                    onFilterClick = {
                        coroutineScope.launch { scaffoldState.bottomSheetState.expand() }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom states
                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CRUNCHY_ORANGE)
                    }
                } else if (animeList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No results found",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No animation releases found matching country '$selectedCountry' or release year '$selectedYear'. Try adjusting filters.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val gridState = rememberLazyGridState()
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(animeList, key = { it.id }) { media ->
                            MovieCard(
                                media = media,
                                onClick = { onNavigateToDetails(media.id) },
                                modifier = Modifier.testTag("anime_item_card")
                            )
                        }
                    }
                }
            }
        }
    }
}
