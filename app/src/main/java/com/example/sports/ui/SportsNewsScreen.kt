package com.example.sports.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.sports.model.News
import com.example.sports.viewmodel.NewsViewModel
import com.example.sports.viewmodel.SportsUiState
import com.example.sports.viewmodel.SportsViewModels

/**
 * "News" top tab — trending sports news from POST /news/trending-tag. Completely separate from
 * any movie list. Tapping a card opens [SportsArticleScreen].
 */
@Composable
fun SportsNewsScreen(
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: NewsViewModel = viewModel(factory = SportsViewModels.factory)
    val state by vm.list.collectAsState()

    when (val s = state) {
        is SportsUiState.Loading -> CenterLoader()
        is SportsUiState.Error -> SportsMessage("Couldn't load news. ${s.message}")
        is SportsUiState.Success -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
            ) {
                val list = s.data
                if (list.isNotEmpty()) {
                    item { NewsHeroCard(list.first(), onClick = { onArticleClick(list.first().id) }) }
                    item { Spacer(Modifier.height(12.dp)) }
                    items(list.drop(1), key = { it.id }) { article ->
                        NewsRowCard(article, onClick = { onArticleClick(article.id) })
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsHeroCard(news: News, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SportsCard)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = news.cover.url,
            contentDescription = news.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        Column(Modifier.padding(14.dp)) {
            if (news.tag.isNotBlank()) {
                Text(news.tag.uppercase(), color = SportsAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
            }
            Text(
                news.title, color = SportsText, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text("${news.author} • ${news.date}", color = SportsTextDim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NewsRowCard(news: News, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SportsCard)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        AsyncImage(
            model = news.cover.url,
            contentDescription = news.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 120.dp, height = 80.dp).clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                news.title, color = SportsText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text("${news.author} • ${news.date}", color = SportsTextDim, fontSize = 11.sp)
        }
    }
}
