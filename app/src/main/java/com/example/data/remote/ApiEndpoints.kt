package com.example.data.remote

/**
 * API Endpoints Configuration
 * Mirrors web's endpoints.js with all StreamWave API routes
 */
object ApiEndpoints {
    // Content Search & Discovery
    const val SEARCH_SUGGEST = "/subject/search-suggest"
    const val SEARCH = "/subject/search"
    const val EVERYONE_SEARCH = "/subject/everyone-search"
    
    // Playback & Streaming
    const val PLAY = "/subject/play"
    const val CAPTION = "/subject/caption"
    
    // Content Details & Recommendations
    const val RECOMMENDATIONS = "/subject/detail-rec"
    
    // Navigation & Configuration
    const val BOTTOM_TABS = "/tab/get-bottom-tab-list"
    const val WEB_CONFIG = "/webConfig"
    
    const val DETAIL = "/detail"
    const val DETAILS = "/subject/details"
    const val EPISODES = "/subject/episodes"
    
    const val HOME = "/home"
    const val HOME_EXTRA = "/home-extra"
    const val CATEGORY = "/category"
    
    const val CLIENT_INFO = "/client-info"
    const val STAFF_RELATED = "/staff/staff-related"
    const val STAFF_SUBJECTS = "/staff/subject-list"
    
    const val MOVIE = "/subject/movie"
    const val RANKING_CONTENT = "/ranking-list/content"
    const val SUBJECT_FILTER = "/subject/filter"
}
