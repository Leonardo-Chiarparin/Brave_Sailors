package com.example.brave_sailors.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.data.local.database.FleetDao
import com.example.brave_sailors.data.local.database.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameModeDetailsViewModel(
    private val fleetDao: FleetDao,
    private val userDao: UserDao
) : ViewModel() {
    fun validateFleetAndProceed(
        gameModeIndex: Int,
        difficulty: String,
        firingRule: String,
        onFleetFound: (String?, String) -> Unit,
        onFleetMissing: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val localUser = withContext(Dispatchers.IO) {
                userDao.getCurrentUser()
            }

            if (localUser == null) {
                onFleetMissing(gameModeIndex)
                return@launch
            }

            val userId = localUser.id

            val fleet = withContext(Dispatchers.IO) {
                fleetDao.getUserFleet(userId)
            }

            if (fleet.isNotEmpty()) {
                when (gameModeIndex) {
                    0 -> onFleetFound(difficulty, firingRule)
                    else -> onFleetFound(null, firingRule)
                }
            } else {
                onFleetMissing(gameModeIndex)
            }
        }
    }
}

class GameModeDetailsViewModelFactory(
    private val fleetDao: FleetDao,
    private val userDao: UserDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameModeDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameModeDetailsViewModel(fleetDao, userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}