package com.example

import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import okhttp3.Request

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun probeSubjectFilter() {
    runBlocking {
      val client = RetrofitClient.okHttpClient
      
      val urls = listOf(
        "https://h5-api.aoneroom.com/wefeed-h5api-bff/subject/filter?host=movie-box.co&tabId=2&classify=All&country=All&genre=All&sort=ForYou&year=All&page=1&perPage=18",
        "https://h5-api.aoneroom.com/wefeed-h5api-bff/subject/filter?host=movie-box.co&tabId=2&filterType=%7B%22classify%22%3A%22All%22%2C%22country%22%3A%22All%22%2C%22genre%22%3A%22All%22%2C%22sort%22%3A%22ForYou%22%2C%22year%22%3A%22All%22%7D&page=1&perPage=18",
        "https://h5-api.aoneroom.com/wefeed-h5api-bff/subject/filter?host=movie-box.co&classify=All&country=All&genre=All&sort=ForYou&year=All&page=1&perPage=18"
      )

      val successes = mutableListOf<String>()
      for (url in urls) {
        val request = Request.Builder().url(url).build()
        try {
          client.newCall(request).execute().use { response ->
            val code = response.code
            val body = response.body?.string() ?: ""
            successes.add("URL: $url -> Code: $code, Preview: ${if (body.length > 500) body.substring(0, 500) else body}")
          }
        } catch (e: Exception) {
          successes.add("URL: $url -> Error: ${e.message}")
        }
      }
      
      throw RuntimeException("RESULTS OF PROBING:\n" + successes.joinToString("\n\n"))
    }
  }
}

