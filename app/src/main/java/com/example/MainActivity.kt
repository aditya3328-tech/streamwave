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
import com.example.data.repository.MovieTabRepository
import com.example.data.repository.StreamRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CineVerseViewModel
import com.example.ui.viewmodel.MovieTabViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Restore persisted MovieBox session token (SharedPreferences) before any network call.
    com.example.data.remote.NetworkModule.init(applicationContext)

    // Restore the standalone Sports session token (separate from the movie session).
    com.example.sports.api.SportsNetworkModule.init(applicationContext)

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
    val repository = MovieRepository(
      apiService = com.example.data.remote.RetrofitClient.apiService,
      streamRepository = StreamRepository(applicationContext),
      movieDao = movieDao
    )
    val viewModel = CineVerseViewModel(repository)
    val movieTabViewModel = MovieTabViewModel(MovieTabRepository(movieDao = movieDao))

    setContent {
      MyApplicationTheme(darkTheme = true) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          AppNavigation(viewModel = viewModel, movieTabViewModel = movieTabViewModel)
        }
      }
    }
  }

  // Auto-enter Picture-in-Picture when the user presses Home while a video is playing.
  // The player toggles PiPManager.autoEnterEnabled; PiPManager guards the API level itself.
  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    if (com.example.ui.player.PiPManager.autoEnterEnabled) {
      com.example.ui.player.PiPManager.enterPip(this)
    }
  }
}
