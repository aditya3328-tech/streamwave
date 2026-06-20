package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.repository.MovieRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CineVerseViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Room DB
    val database = Room.databaseBuilder(
      applicationContext,
      AppDatabase::class.java,
      "cineverse_local_database"
    )
    .fallbackToDestructiveMigration()
    .build()

    // Initialize Repository & ViewModel Store (Zustand Android Adapter)
    val movieDao = database.movieDao()
    val repository = MovieRepository(apiService = com.example.data.remote.RetrofitClient.apiService, movieDao = movieDao)
    val viewModel = CineVerseViewModel(repository)

    setContent {
      MyApplicationTheme(darkTheme = true) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          AppNavigation(viewModel = viewModel)
        }
      }
    }
  }
}
