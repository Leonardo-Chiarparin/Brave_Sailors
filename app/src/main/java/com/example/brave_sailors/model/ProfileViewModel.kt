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
    // Observes the local Room Database through the repository
    val userState: StateFlow<User?> = userRepository.getUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // -- Leaderboard State --
    private val _leaderboard = MutableStateFlow<List<RankingPlayer>>(emptyList())
    val leaderboard = _leaderboard.asStateFlow()

    private val packageName = "com.example.brave_sailors"

    private fun localUri(resourceId: Int): String {
        return "android.resource://$packageName/$resourceId"
    }

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
        startLeaderboardSync()

        viewModelScope.launch {
            userState.collectLatest { user ->
                if (user != null && !user.sessionToken.isNullOrEmpty()) {
                    startSessionObserver(user.id, user.sessionToken!!)
                } else {
                    removeSessionListener()
                }
            }
        }
    }

    private fun startLeaderboardSync() {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.getLeaderboard()
                .collect { list ->
                    _leaderboard.value = list
                }
        }
    }

    /**
     * Initializes the user session token and updates both Cloud and Local DB.
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
                val flags = RetrofitClient.api.getFlags()
                if (flags.isNotEmpty()) {
                    _flagList.value = flags.sortedBy { it.name }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error while fetching flags: ${e.message}")
                _flagList.value = fallbackFlags
            }
        }
    }

    /**
     * Updates game-related stats (XP and Level) and syncs to cloud.
     */
    fun updateGameStats(id: String, level: Int, xp: Int, timestamp: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userDao.updateGameStats(id, level, xp, timestamp)
                userRepository.syncLocalToCloud(id)
                Log.d("ProfileViewModel", "Stats updated: Level $level, XP $xp")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update stats: ${e.message}")
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
                    _forceLogoutEvent.value = true
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        userRef.addValueEventListener(sessionListener!!)
    }

    fun performLocalLogout() {
        removeSessionListener()
        viewModelScope.launch(Dispatchers.IO) {
            userDao.deleteAllUsers()
            withContext(Dispatchers.Main) {
                _forceLogoutEvent.value = false
            }
        }
    }

    private fun removeSessionListener() {
        if (sessionListener != null && monitoredUserId != null) {
            database.getReference("users").child(monitoredUserId!!).removeEventListener(sessionListener!!)
            sessionListener = null
            monitoredUserId = null
        }
    }

    fun updateCountry(countryCode: String) {
        val currentUser = userState.value ?: return
        val updatedUser = currentUser.copy(countryCode = countryCode)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userDao.updateUser(updatedUser)
                userRepository.syncLocalToCloud(updatedUser.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateName(newName: String) {
        val currentUser = userState.value ?: return
        if (newName.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                userDao.updateUserName(currentUser.id, newName)
                userRepository.syncLocalToCloud(currentUser.id)
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
                    userRepository.syncLocalToCloud(currentUser.id)
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
                result.onSuccess { onResult(true, it) }
                    .onFailure { onResult(false, it.message ?: "Error") }
            }
        }
    }

    fun deleteAccount(user: User, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = userRepository.deleteAccount(user)
            onResult(result.isSuccess)
        }
    }

    fun sendFriendRequest(targetId: String, onResult: (Boolean, String) -> Unit) {
        val currentUser = userState.value ?: return
        if (targetId == currentUser.id) {
            onResult(false, "You cannot add your own ID!")
            return
        }
        viewModelScope.launch {
            val result = userRepository.sendFriendRequest(currentUser, targetId)
            result.onSuccess { onResult(true, it) }
                .onFailure { onResult(false, it.message ?: "Error") }
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
            userRepository.syncLocalToCloud(restoredUser.id)
        }
    }

    /**
     * Specialized function for Training/Challenge rewards.
     * Grants +5 XP and +5 Ranking Score.
     */
    fun addMiniGameWinReward(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUserById(userId) ?: return@launch

            // Increment XP and Ranking Total Score
            val newXp = user.currentXp + 5
            val newTotalScore = user.totalScore + 5L
            val now = System.currentTimeMillis()

            // Logic for Level Up (every 100 XP)
            var currentLevel = user.level
            var adjustedXp = newXp
            if (adjustedXp >= 100) {
                currentLevel += 1
                adjustedXp -= 100
            }

            // Update user in local database to trigger UI update via Flow
            val updatedUser = user.copy(
                level = currentLevel,
                currentXp = adjustedXp,
                totalScore = newTotalScore,
                lastWinTimestamp = now
            )
            userDao.updateUser(updatedUser)

            // Sync with Firebase to update global leaderboard
            userRepository.syncLocalToCloud(userId)

            Log.d("ProfileViewModel", "Reward granted: +5 XP, +5 Score. New Score: $newTotalScore")
        }
    }

    /**
     * Calculates the cooldown for challenges.
     * Uses userState.value directly as it is the StateFlow source.
     */
    fun getCooldownRemaining(): Long {
        // Use userState.value directly
        val user = userState.value ?: return 0L

        // Handle potential null values and use Long for calculations to avoid ambiguity
        val lastWin = user.lastWinTimestamp
        val now = System.currentTimeMillis()
        val cooldownPeriod = 3600000L // 1 hour

        val remaining = (lastWin + cooldownPeriod) - now

        return if (remaining > 0L) remaining else 0L
    }

    /**
     * Updates the win timestamp to trigger cooldown.
     */
    fun updateChallengeCooldown(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            userDao.updateLastWinTimestamp(userId, now)
        }
    }
    fun updateAiAvatar(context: Context, bitmap: Bitmap) {
        val currentUser = userState.value ?: return
        viewModelScope.launch {
            try {
                // Save to internal storage
                val filePath = withContext(Dispatchers.IO) {
                    val fileName = "ai_avatar_${currentUser.id}.jpg"
                    val file = File(context.filesDir, fileName)
                    if (file.exists()) file.delete()
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    file.absolutePath
                }

                // Update User Entity
                val updatedUser = currentUser.copy(aiAvatarPath = filePath)

                withContext(Dispatchers.IO) {
                    userDao.updateUser(updatedUser)
                    // Note: We usually don't sync this specific local setting to cloud
                    // unless you want the AI image to persist across devices.
                    // If yes: userRepository.syncLocalToCloud(updatedUser.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        removeSessionListener()
    }
}

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