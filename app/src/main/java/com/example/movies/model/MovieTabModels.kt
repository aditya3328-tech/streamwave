package com.example.movies.model

data class MovieTabResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: MovieTabData? = null,
    @get:JvmName("getRawOperatingList")
    val operatingList: List<com.example.data.model.HomeSection>? = null
) {
    fun getOperatingList(): List<com.example.data.model.HomeSection> =
        data?.operatingList ?: operatingList ?: emptyList()
}

data class MovieTabData(
    val operatingList: List<com.example.data.model.HomeSection> = emptyList()
)

data class TrendingResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: TrendingData? = null,
    @get:JvmName("getRawSubjectList")
    val subjectList: List<com.example.data.model.Subject>? = null
) {
    fun getSubjectList(): List<com.example.data.model.Subject> =
        data?.subjectList ?: subjectList ?: emptyList()
}

data class TrendingData(
    val subjectList: List<com.example.data.model.Subject> = emptyList()
)
