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
            // 1. Retrieve the user from the LOCAL DB instead of directly from Firebase.
            // This ensures we use the same ID with which the fleet was saved.
            val localUser = withContext(Dispatchers.IO) {
                userDao.getCurrentUser()
            }

            if (localUser == null) {
                onFleetMissing(gameModeIndex)
                return@launch
            }

            val userId = localUser.id

            // 2. Check the fleet
            val fleet = withContext(Dispatchers.IO) {
                fleetDao.getUserFleet(userId)
            }

            // Debug log to understand what is happening (optional)
            // Log.d("FleetCheck", "User: $userId, Ships found: ${fleet.size}")

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

// Update the Factory
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