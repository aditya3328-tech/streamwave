package com.example.sports.viewmodel

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Single factory for every Sports ViewModel. Each uses the shared [SportsRepository.instance]
 * default, so construction is parameterless here. Use at call sites via:
 *
 *   val vm: SportsHomeViewModel = viewModel(factory = SportsViewModels.factory)
 */
object SportsViewModels {
    val factory = viewModelFactory {
        initializer { SportsHomeViewModel() }
        initializer { MatchDetailsViewModel() }
        initializer { NewsViewModel() }
    }
}
