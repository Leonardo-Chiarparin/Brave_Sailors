package com.example.brave_sailors.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    repository: UserRepository
) : ViewModel() {

    // Holds the latest user from the local database as a StateFlow.
    // Whenever the underlying Room table changes (e.g., after registration/login/sync),
    // this flow updates automatically and the UI can react.
    val currentUser: StateFlow<User?> = repository.getUserFlow()
        .stateIn(
            scope = viewModelScope, // Flow collection is tied to the ViewModel lifecycle
            started = SharingStarted.WhileSubscribed(5000), // Stop collecting when no observers for 5s
            initialValue = null // Initial state before Room emits the first value
        )
}

// Factory used to create the ViewModel with a constructor parameter (the repository).
// Required when you don't use DI frameworks (e.g., Hilt/Koin) and need manual creation.
class HomeViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {

    // Creates the requested ViewModel instance.
    // Verifies the requested class type and returns HomeViewModel when appropriate.
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        // Thrown when a different ViewModel class is requested from this factory.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
