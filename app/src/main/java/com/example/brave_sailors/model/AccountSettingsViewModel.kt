package com.example.brave_sailors.model

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.data.repository.UserRepository
import kotlinx.coroutines.launch

// Possible UI states for the Registration/Login screen
sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    data class Success(val message: String) : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

class RegisterViewModel(
    private val repository: UserRepository
) : ViewModel() {

    // Tracks the current state of the UI (Loading, Error, Success)
    var uiState by mutableStateOf<RegisterUiState>(RegisterUiState.Idle)
        private set

    // Whitelist of allowed email domains to restrict registration
    private val allowedDomains = listOf(
        "gmail.com", "googlemail.com",
        "outlook.com", "outlook.it", "hotmail.com", "uniroma1.it", "hotmail.it", "live.com",
        "yahoo.com", "yahoo.it",
        "icloud.com", "me.com",
        "libero.it", "virgilio.it", "tiscali.it", "fastwebnet.it"
    )

    fun register(email: String, pass: String, confirmPass: String) {
        // Normalize input: remove whitespace, while the domain is later converted to lowercase for consistency
        val cleanEmail = email.trim().lowercase()

        uiState = RegisterUiState.Loading

        // --- DOMAIN VALIDATION ---
        // Extract the domain substring and check against the allowed list
        val domain = cleanEmail.substringAfter("@", "")
        if (domain !in allowedDomains) {
            uiState = RegisterUiState.Error("Please use a correct domain (e.g., Gmail, Outlook, Yahoo, Libero, etc.).")
            return
        }

        // --- FORMAT VALIDATION ---
        // Use the cleaned email to ensure no invisible spaces cause regex failure
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            uiState = RegisterUiState.Error("Invalid email format.")
            return
        }

        if (pass.length < 6) {
            uiState = RegisterUiState.Error("Password must be at least 6 characters long.")
            return
        }

        if (pass != confirmPass) {
            uiState = RegisterUiState.Error("Passwords do not match.")
            return
        }

        // --- NAME GENERATION ---
        // Automatically create a display name by taking the email prefix (before '@')
        // and capitalizing the first letter (e.g., mario@... -> Mario)
        val generatedName = cleanEmail.substringBefore("@")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        // --- EXECUTION ---
        viewModelScope.launch {
            // Proceed with registration using the repository.
            // Note: We send 'generatedName' so the backend User entity is created with a name,
            // avoiding the default "Sailor".
            val result = repository.registerUser(cleanEmail, pass, generatedName)

            result.onSuccess { msg ->
                uiState = RegisterUiState.Success(msg)
            }.onFailure { err ->
                uiState = RegisterUiState.Error(err.message ?: "Unknown error")
            }
        }
    }

    fun login(email: String, pass: String) {
        val cleanEmail = email.trim().lowercase()
        uiState = RegisterUiState.Loading

        // --- INPUT VALIDATION ---
        if (cleanEmail.isBlank() || pass.isBlank()) {
            uiState = RegisterUiState.Error("Please enter both email and password.")
            return
        }

        // Basic format check before making network request
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            uiState = RegisterUiState.Error("Invalid email format.")
            return
        }

        // --- EXECUTION ---
        viewModelScope.launch {
            val result = repository.loginUser(cleanEmail, pass)

            result.onSuccess { msg ->
                uiState = RegisterUiState.Success(msg)
            }.onFailure { err ->
                uiState = RegisterUiState.Error(err.message ?: "Login failed")
            }
        }
    }

    fun resetState() {
        uiState = RegisterUiState.Idle
    }
}

// Factory to instantiate the ViewModel with dependencies (UserRepository)
class RegisterViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}