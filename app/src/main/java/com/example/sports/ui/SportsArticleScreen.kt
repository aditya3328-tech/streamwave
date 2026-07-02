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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * Sports article detail — large image, headline, date, author, body text, and a related-news
 * rail. Completely separate from any movie screen, exactly like the SportsNow article page.
 */
@Composable
fun SportsArticleScreen(
    articleId: String,
    onBack: () -> Unit,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: NewsViewModel = viewModel(factory = SportsViewModels.factory)
    val state by vm.article.collectAsState()

    LaunchedEffect(articleId) { vm.loadArticle(articleId) }

    Box(modifier = modifier.fillMaxSize().background(SportsBg)) {
        when (val s = state) {
            is SportsUiState.Loading -> CenterLoader()
            is SportsUiState.Error -> SportsMessage("Couldn't load article. ${s.message}")
            is SportsUiState.Success -> ArticleContent(s.data, onBack = onBack, onArticleClick = onArticleClick)
        }
    }
}

@Composable
private fun ArticleContent(news: News, onBack: () -> Unit, onArticleClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        // ---- Large hero image with back button ----
        item {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                AsyncImage(
                    model = news.cover.url,
                    contentDescription = news.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .padding(2.dp)
                        .clickable(onClick = onBack),
                )
            }
        }

        // ---- Headline / date / author ----
        item {
            Column(Modifier.padding(16.dp)) {
                if (news.tag.isNotBlank()) {
                    Text(news.tag.uppercase(), color = SportsAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                }
                Text(news.title, color = SportsText, fontSize = 22.sp, fontWeight = FontWeight.Black, lineHeight = 28.sp)
                Spacer(Modifier.height(10.dp))
                Text("${news.author}  •  ${news.date}", color = SportsTextDim, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(SportsDivider))
            }
        }

        // ---- Body ----
        item {
            Text(
                text = news.content.ifBlank { "No content available." },
                color = SportsText.copy(alpha = 0.9f),
                fontSize = 15.sp,
                lineHeight = 23.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // ---- Related news ----
        if (news.relatedNews.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                SportsSectionHeader("Related news")
            }
            items(news.relatedNews, key = { it.id }) { related ->
                RelatedNewsRow(related, onClick = { onArticleClick(related.id) })
            }
        }
    }
}

@Composable
private fun RelatedNewsRow(news: News, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SportsCard)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        AsyncImage(
            model = news.cover.url,
            contentDescription = news.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 110.dp, height = 72.dp).clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                news.title, color = SportsText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text("${news.author} • ${news.date}", color = SportsTextDim, fontSize = 11.sp)
        }
    }
}
