package com.example.sports.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sports.model.Match
import com.example.sports.model.MatchStatus
import com.example.sports.model.OddItem

/** Circular team crest. Falls back to a tinted disc when [url] is blank. */
@Composable
fun TeamCrest(url: String, size: Int = 44, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(SportsCardAlt),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size.dp).clip(CircleShape),
        )
    }
}

/** LIVE / Upcoming / Finished pill. */
@Composable
fun MatchStatusBadge(status: MatchStatus, time: String, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        MatchStatus.LIVE -> "LIVE" to SportsLive
        MatchStatus.UPCOMING -> "Upcoming" to SportsTextDim
        MatchStatus.FINISHED -> "Finished" to SportsAccentDim
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status == MatchStatus.LIVE) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(SportsLive))
            Spacer(Modifier.width(5.dp))
        }
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        if (time.isNotBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(time, color = SportsTextDim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Horizontal filter chip used on the Home screen ("All", "FIFA World Cup", …). */
@Composable
fun SportsChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) SportsAccent else SportsCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else SportsTextDim,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
fun SportsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = SportsText,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/**
 * The match card — the centerpiece of the Sports UI, matching the reference screenshots:
 *
 *   matchRound + status
 *   team1 crest/name        0 - 0        team2 crest/name
 *   [ ▶ Highlights ]
 *   1  2.605
 *   X  2.45
 *   2  4.19
 *   Fans Supported  36% ████  64% ████████
 *   [ Bet Now ]
 */
@Composable
fun MatchCard(
    match: Match,
    onClick: () -> Unit,
    onHighlights: () -> Unit = onClick,
    onBet: () -> Unit = onClick,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SportsCard)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        // ---- Header: round + status ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = match.matchRound,
                color = SportsTextDim,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            MatchStatusBadge(match.status, match.matchTime)
        }

        Spacer(Modifier.height(14.dp))

        // ---- Scoreboard: team1   score - score   team2 ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamSide(name = match.team1.name, avatar = match.team1.avatar, modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(
                    text = "${match.team1.score} - ${match.team2.score}",
                    color = SportsText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            TeamSide(name = match.team2.name, avatar = match.team2.avatar, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        // ---- Highlights button ----
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SportsCardAlt)
                .clickable(onClick = onHighlights)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = SportsAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Highlights", color = SportsText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        // ---- Odds rows (1 / X / 2) ----
        if (match.oddsInfo.oddsList.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            match.oddsInfo.oddsList.forEach { OddRow(it) }
        }

        // ---- Fans Supported ----
        Spacer(Modifier.height(14.dp))
        FansSupportBar(home = match.homeSupport, away = match.awaySupport)

        // ---- Bet Now ----
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SportsAccent)
                .clickable(onClick = onBet)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Bet Now", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TeamSide(name: String, avatar: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TeamCrest(avatar, size = 48)
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            color = SportsText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OddRow(odd: OddItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SportsCardAlt),
            contentAlignment = Alignment.Center,
        ) {
            Text(odd.label, color = SportsTextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(odd.value, color = SportsAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/** Two-tone proportional bar: home% on the left (blue), away% on the right (amber). */
@Composable
fun FansSupportBar(home: Int, away: Int, modifier: Modifier = Modifier) {
    val total = (home + away).coerceAtLeast(1)
    val homeFraction = home.toFloat() / total
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Fans Supported", color = SportsTextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("$home%", color = SportsHome, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("$away%", color = SportsAway, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        ) {
            Box(Modifier.weight(homeFraction.coerceIn(0.02f, 0.98f)).fillMaxWidth().height(8.dp).background(SportsHome))
            Box(Modifier.weight((1f - homeFraction).coerceIn(0.02f, 0.98f)).fillMaxWidth().height(8.dp).background(SportsAway))
        }
    }
}

/** Compact card used inside the home-screen horizontal "Trending leagues" rail. */
@Composable
fun MatchMiniCard(match: Match, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SportsCard)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MatchStatusBadge(match.status, match.matchTime)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TeamCrest(match.team1.avatar, size = 28)
            Spacer(Modifier.width(8.dp))
            Text(
                match.team1.shortName.ifBlank { match.team1.name },
                color = SportsText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Text("${match.team1.score}", color = SportsText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TeamCrest(match.team2.avatar, size = 28)
            Spacer(Modifier.width(8.dp))
            Text(
                match.team2.shortName.ifBlank { match.team2.name },
                color = SportsText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Text("${match.team2.score}", color = SportsText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            match.matchRound,
            color = SportsTextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Small centered loading / error helpers shared across Sports screens. */
@Composable
fun SportsMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(text, color = SportsTextDim, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}
