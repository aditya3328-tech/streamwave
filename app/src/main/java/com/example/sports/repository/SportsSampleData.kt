package com.example.sports.repository

import com.example.sports.model.Cover
import com.example.sports.model.Highlight
import com.example.sports.model.Match
import com.example.sports.model.News
import com.example.sports.model.OddItem
import com.example.sports.model.Odds
import com.example.sports.model.PlaySource
import com.example.sports.model.SportsSection
import com.example.sports.model.Team

/**
 * Deterministic offline sample content for the Sports module. Used as a graceful fallback when
 * the live BFF is unreachable or returns an unexpected shape, so every Sports screen always
 * renders the intended layout. Mirrors the reference SportsNow screenshots (Paraguay vs
 * Australia, FIFA World Cup Group D Round 3) including the real demo HLS playlist.
 */
internal object SportsSampleData {

    private const val DEMO_HLS = "https://live-pull.aisports.mobi/moviebox/bsy_008/playlist.m3u8"

    private fun crest(seed: String) = "https://api.dicebear.com/7.x/shapes/png?seed=$seed"
    private fun shot(seed: String) = "https://picsum.photos/seed/$seed/640/360"

    private fun odds(o1: String, ox: String, o2: String) = Odds(
        provider = "SportsNow",
        oddsList = listOf(OddItem(1, o1.toDouble()), OddItem(2, ox.toDouble()), OddItem(3, o2.toDouble())),
        betUrl = "https://sportsnow.top/bet",
    )

    private fun liveSources(prefix: String) = listOf(
        PlaySource("Channel 1", DEMO_HLS),
        PlaySource("Channel 2", DEMO_HLS),
        PlaySource("Channel 3", DEMO_HLS),
    )

    private fun highlights(prefix: String) = listOf(
        Highlight("$prefix-h1", "Opening goal", Cover(shot("$prefix-1")), DEMO_HLS, "0:42"),
        Highlight("$prefix-h2", "Great save", Cover(shot("$prefix-2")), DEMO_HLS, "0:31"),
        Highlight("$prefix-h3", "Full match recap", Cover(shot("$prefix-3")), DEMO_HLS, "2:10"),
    )

    fun match(
        id: String,
        round: String,
        t1: String,
        t2: String,
        s1: Int = 0,
        s2: Int = 0,
        status: String = "live",
        time: String = "45'",
        home: Int = 50,
    ): Match = Match(
        id = id,
        matchRound = round,
        league = round.substringBefore(" Group"),
        matchStatus = status,
        matchTime = time,
        team1 = Team(id = "${id}_1", name = t1, avatar = crest(t1), score = s1.toString(), shortName = t1.take(3).uppercase()),
        team2 = Team(id = "${id}_2", name = t2, avatar = crest(t2), score = s2.toString(), shortName = t2.take(3).uppercase()),
        oddsInfo = odds("2.60", "2.45", "4.19"),
        highlights = highlights(id),
        playPath = DEMO_HLS,
        playSource = liveSources(id),
        homeSupport = home,
        awaySupport = 100 - home,
    )

    val sections: List<SportsSection> by lazy {
        listOf(
            SportsSection(
                "Trending leagues",
                listOf(
                    match("m1", "FIFA World Cup Group D Round 3", "Paraguay", "Australia", 0, 0, "live", "45'", 36),
                    match("m2", "Premier League Matchday 21", "Arsenal", "Chelsea", 2, 1, "live", "78'", 58),
                ),
            ),
            SportsSection(
                "World Cup",
                listOf(
                    match("m3", "World Cup Qualifier", "Brazil", "Argentina", 1, 1, "live", "61'", 49),
                    match("m4", "World Cup Group A", "France", "Germany", 0, 0, "upcoming", "Today 21:00", 52),
                ),
            ),
            SportsSection(
                "FIFA World Cup",
                listOf(
                    match("m5", "FIFA World Cup Group B", "Spain", "Portugal", 3, 2, "finished", "FT", 55),
                    match("m6", "FIFA World Cup Group C", "Italy", "Netherlands", 0, 1, "live", "33'", 47),
                ),
            ),
            SportsSection(
                "Premier League",
                listOf(
                    match("m7", "Premier League Matchday 21", "Man City", "Liverpool", 2, 2, "live", "70'", 50),
                    match("m8", "Premier League Matchday 21", "Tottenham", "Newcastle", 1, 0, "live", "55'", 53),
                ),
            ),
            SportsSection(
                "Champions League",
                listOf(
                    match("m9", "Champions League Round of 16", "Real Madrid", "Bayern", 1, 1, "upcoming", "Tomorrow 00:00", 51),
                    match("m10", "Champions League Round of 16", "PSG", "Inter", 0, 0, "upcoming", "Tomorrow 00:00", 48),
                ),
            ),
            SportsSection(
                "Cricket",
                listOf(
                    match("m11", "ICC World Cup Super Eight", "India", "Australia", 0, 0, "live", "32 ov", 62),
                    match("m12", "T20 International", "England", "Pakistan", 0, 0, "upcoming", "Today 19:30", 50),
                ),
            ),
            SportsSection(
                "Basketball",
                listOf(
                    match("m13", "NBA Regular Season", "Lakers", "Celtics", 88, 84, "live", "Q4 5:21", 49),
                    match("m14", "NBA Regular Season", "Warriors", "Nuggets", 102, 99, "live", "Q4 1:02", 51),
                ),
            ),
        )
    }

    val allMatches: List<Match> by lazy { sections.flatMap { it.matches } }

    val news: List<News> by lazy {
        listOf(
            news("n1", "Paraguay hold Australia in tense Group D clash", "FIFA Newsroom", "Football"),
            news("n2", "Arsenal edge Chelsea in London derby thriller", "Premier Wire", "Football"),
            news("n3", "India dominate early as World Cup heats up", "Cricket Today", "Cricket"),
            news("n4", "Lakers hold off Celtics in classic showdown", "Court Report", "Basketball"),
            news("n5", "Champions League draw sets up blockbuster ties", "UEFA Insider", "Football"),
            news("n6", "Transfer window: the deals shaping the season", "Market Watch", "Transfers"),
        )
    }

    private fun news(id: String, title: String, author: String, tag: String): News = News(
        id = id,
        title = title,
        cover = Cover(shot("news-$id")),
        author = author,
        date = "2026-06-26",
        tag = tag,
        content = buildString {
            repeat(4) {
                append(
                    "$title. In a match that kept fans on the edge of their seats, both sides " +
                        "traded chances throughout. The intensity never dropped as the clock wound " +
                        "down, and the result leaves the group finely balanced ahead of the next round.\n\n",
                )
            }
        },
        relatedNews = emptyList(),
    )
}
