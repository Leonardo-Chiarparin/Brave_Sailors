package com.example.brave_sailors.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.R
import com.example.brave_sailors.data.local.database.FleetDao
import com.example.brave_sailors.data.local.database.UserDao
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.remote.api.Flag
import com.example.brave_sailors.data.remote.api.RetrofitClient
import com.example.brave_sailors.data.repository.UserRepository
import com.example.brave_sailors.data.local.database.entity.RankingPlayer
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProfileViewModel(
    private val userDao: UserDao,
    private val userRepository: UserRepository
) : ViewModel() {

    // -- User State (REACTIVE) --
    // This connects directly to the local Room Database.
    // When the user logs in or updates their profile, this flow emits the new data automatically.
    val userState: StateFlow<User?> = userRepository.getUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // -- Leaderboard State --
    // Holds the list of top 25 players fetched from Firebase Realtime Database.
    private val _leaderboard = MutableStateFlow<List<RankingPlayer>>(emptyList())
    val leaderboard = _leaderboard.asStateFlow()

    // -- Helpers regarding countries --
    private val packageName = "com.example.brave_sailors"

    private fun localUri(resourceId: Int): String {
        return "android.resource://$packageName/$resourceId"
    }

    // Default flags in case API fails
    private val fallbackFlags = listOf(
        Flag("Belgium", "BE", localUri(R.drawable.ic_flag_belgium)),
        Flag("China", "CN", localUri(R.drawable.ic_flag_china)),
        Flag("France", "FR", localUri(R.drawable.ic_flag_france)),
        Flag("Germany", "DE", localUri(R.drawable.ic_flag_germany)),
        Flag("Italy", "IT", localUri(R.drawable.ic_flag_italy)),
        Flag("Japan", "JP", localUri(R.drawable.ic_flag_japan)),
        Flag("Russia", "RU", localUri(R.drawable.ic_flag_russia)),
        Flag("Spain", "ES", localUri(R.drawable.ic_flag_spain)),
        Flag("United Kingdom", "GB", localUri(R.drawable.ic_flag_uk))
    ).sortedBy { it.name }

    private val _flagList = MutableStateFlow<List<Flag>>(fallbackFlags)
    val flagList: StateFlow<List<Flag>> = _flagList.asStateFlow()

    private val _forceLogoutEvent = MutableStateFlow(false)
    val forceLogoutEvent = _forceLogoutEvent.asStateFlow()

    private val database = FirebaseDatabase.getInstance()
    private var sessionListener: ValueEventListener? = null
    private var monitoredUserId: String? = null

    var showHomeWelcome: Boolean = true

    init {
        fetchFlags()

        // Start listening to the global leaderboard updates
        startLeaderboardSync()

        // -- Session Monitor --
        // Automatically starts observing session token when a user logs in (userState becomes non-null)
        viewModelScope.launch {
            userState.collectLatest { user ->
                if (user != null && !user.sessionToken.isNullOrEmpty()) {
                    startSessionObserver(user.id, user.sessionToken)
                } else {
                    removeSessionListener()
                }
            }
        }
    }

    /**
     * Starts observing the leaderboard data from the repository.
     */
    private fun startLeaderboardSync() {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.getLeaderboard()
                .collect { list ->
                    _leaderboard.value = list
                }
        }
    }

    /**
     * Initializes the user session token.
     */
    fun initializeSession(user: User, isNewUser: Boolean, onComplete: (User) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val newToken = UUID.randomUUID().toString()
            val updatedUser = user.copy(sessionToken = newToken)

            try {
                val userRef = database.getReference("users").child(user.id)
                Tasks.await(userRef.child("sessionToken").setValue(newToken))

                if (!isNewUser) {
                    userDao.insertUser(updatedUser)
                }

                Log.d("Session", "Token updated: $newToken")
            } catch (e: Exception) {
                Log.e("Session", "Error while inserting the token: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                onComplete(updatedUser)
            }
        }
    }

    private fun fetchFlags() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("ProfileViewModel", "Starting flags download...")
                val flags = RetrofitClient.api.getFlags()
                if (flags.isNotEmpty()) {
                    _flagList.value = flags.sortedBy { it.name }
                    Log.d("ProfileViewModel", "Flags downloaded and sorted successfully.")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error while fetching flags: ${e.message}")
                _flagList.value = fallbackFlags
            }
        }
    }

    // NOTE: 'loadUser' is no longer needed because userState observes the DB automatically.
    // We keep 'registerUser' just in case, but usually RegisterViewModel handles creation.
    fun registerUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userDao.insertUser(user)
                userRepository.syncLocalToCloud(user.id)
                Log.d("ProfileViewModel", "User registered and synced: ${user.id}")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error while inserting user: ${e.message}")
            }
        }
    }

    private fun startSessionObserver(userId: String, localToken: String) {
        if (sessionListener != null && monitoredUserId == userId) return
        removeSessionListener()
        monitoredUserId = userId
        val userRef = database.getReference("users").child(userId)
        sessionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val serverToken = snapshot.child("sessionToken").getValue(String::class.java)
                if (serverToken != null && serverToken != localToken) {
                    Log.w("Session", "Session Conflict detected! Server: $serverToken vs Local: $localToken")
                    _forceLogoutEvent.value = true
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ProfileViewModel", "Database listen failed: ${error.message}")
            }
        }
        userRef.addValueEventListener(sessionListener!!)
    }

    fun performLocalLogout() {
        removeSessionListener()

        viewModelScope.launch(Dispatchers.IO) {
            userDao.deleteAllUsers()
            // No need to manually set _userState to null, the Flow will do it automatically
            // when the table becomes empty.
            withContext(Dispatchers.Main) {
                _forceLogoutEvent.value = false
            }
        }
    }

    private fun removeSessionListener() {
        if (sessionListener != null && monitoredUserId != null) {
            database.getReference("users")
                .child(monitoredUserId!!)
                .removeEventListener(sessionListener!!)
            sessionListener = null
            monitoredUserId = null
        }
    }

    fun updateCountry(countryCode: String) {
        val currentUser = userState.value ?: return

        val updatedUser = currentUser.copy(countryCode = countryCode)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // We update the DB. Room will emit the new value to 'userState'.
                userDao.updateUser(updatedUser)
                Log.d("ProfileViewModel", "Country has been changed to: $countryCode")
                userRepository.syncLocalToCloud(updatedUser.id)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error during update: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun updateName(newName: String) {
        val currentUser = userState.value ?: return
        if (newName.isNotBlank()) {
            val updatedUser = currentUser.copy(name = newName)
            viewModelScope.launch(Dispatchers.IO) {
                // Update Local DB
                userDao.updateUserName(currentUser.id, newName)
                // Sync Cloud
                userRepository.syncLocalToCloud(updatedUser.id)
            }
        }
    }

    fun updateProfilePicture(context: Context, bitmap: Bitmap) {
        val currentUser = userState.value ?: return
        viewModelScope.launch {
            try {
                val updateTime = System.currentTimeMillis()
                val filePath = withContext(Dispatchers.IO) {
                    val fileName = "avatar_${currentUser.id}.jpg"
                    val file = File(context.filesDir, fileName)
                    if (file.exists()) file.delete()
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    file.absolutePath
                }
                withContext(Dispatchers.IO) {
                    userDao.updateProfilePicture(currentUser.id, filePath, updateTime)
                    // We construct the object just for the Cloud Sync
                    val updatedUser = currentUser.copy(
                        profilePictureUrl = filePath,
                        lastUpdated = updateTime
                    )
                    userRepository.syncLocalToCloud(updatedUser.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = userRepository.resetPassword(email)

            withContext(Dispatchers.Main) {
                result.onSuccess { message ->
                    onResult(true, message) // Pass the success message
                }.onFailure { error ->
                    // Pass the error message (the one defined in the Repository)
                    onResult(false, error.message ?: "Unknown error occurred.")
                }
            }
        }
    }

    fun deleteAccount(user: User, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            // Here we use 'userRepository' instead of 'repository'
            val result = userRepository.deleteAccount(user)
            if (result.isSuccess) {
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun sendFriendRequest(targetId: String, onResult: (Boolean, String) -> Unit) {
        val currentUser = userState.value ?: return

        // Prevent adding yourself
        if (targetId == currentUser.id) {
            onResult(false, "You cannot add your own ID!")
            return
        }

        viewModelScope.launch {
            // This now calls the updated logic in Repository that adds the friend directly ("accepted")
            val result = userRepository.sendFriendRequest(currentUser, targetId)
            result.onSuccess { message ->
                onResult(true, message)
            }.onFailure { error ->
                // This will capture the "Player ID does not exist" message
                onResult(false, error.message ?: "An unknown error occurred.")
            }
        }
    }

    fun restoreGoogleProfile() {
        val currentUser = userState.value ?: return
        val restoredUser = currentUser.copy(
            name = currentUser.googleName,
            profilePictureUrl = currentUser.googlePhotoUrl,
            lastUpdated = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            userDao.updateUser(restoredUser)
            Log.d("ProfileViewModel", "Profile restored to Google defaults")
            userRepository.syncLocalToCloud(restoredUser.id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        removeSessionListener()
    }
}

// Factory (Kept compatible with the dependency injection)
class ProfileViewModelFactory(
    private val userDao: UserDao,
    private val fleetDao: FleetDao,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userDao, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}