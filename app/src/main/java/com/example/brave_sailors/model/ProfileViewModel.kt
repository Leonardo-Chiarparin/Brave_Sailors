package com.example.brave_sailors.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.R
import com.example.brave_sailors.data.local.MatchStateStorage
import com.example.brave_sailors.data.local.database.FleetDao
import com.example.brave_sailors.data.local.database.UserDao
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.remote.api.Flag
import com.example.brave_sailors.data.remote.api.RetrofitClient
import com.example.brave_sailors.data.repository.UserRepository
import com.example.brave_sailors.data.local.database.entity.RankingPlayer
import com.example.brave_sailors.data.local.database.entity.SavedShip
import com.example.brave_sailors.ui.utils.GameSettingsManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class MatchType {
    COMPUTER,
    GUEST,
    FRIEND
}

class ProfileViewModel(
    private val userDao: UserDao,
    private val fleetDao: FleetDao,
    private val userRepository: UserRepository
) : ViewModel() {

    val userState: StateFlow<User?> = userRepository.getUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

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

    private val _flagList = MutableStateFlow<List<Flag>>(emptyList())
    val flagList: StateFlow<List<Flag>> = _flagList.asStateFlow()

    private val _forceLogoutEvent = MutableStateFlow(false)
    val forceLogoutEvent = _forceLogoutEvent.asStateFlow()

    private val database = FirebaseDatabase.getInstance()
    private var sessionListener: ValueEventListener? = null
    private var monitoredUserId: String? = null
    private var monitoredSessionToken: String? = null

    var showHomeWelcome: Boolean = true

    init {
        fetchFlags()

        startLeaderboardSync()

        viewModelScope.launch {
            userState.collectLatest { user ->
                if (user != null) {
                    if (!user.sessionToken.isNullOrEmpty()) {
                        startSessionObserver(user.id, user.sessionToken)
                    } else {
                        removeSessionListener()
                    }

                    userRepository.observeFriendsLive(user.id)
                }
                else
                    removeSessionListener()
            }
        }
    }

    fun setMatchActive(context: Context, matchId: String, opponentName: String?, type: MatchType) {
        MatchStateStorage.saveState(
            context,
            matchId,
            opponentName ?: "Unknown",
            type,
            true
        )
    }

    fun updateActiveTurn(context: Context, isPlayerOneTurn: Boolean) {
        MatchStateStorage.updateTurn(context, isPlayerOneTurn)
    }

    fun clearActiveMatch(context: Context) {
        MatchStateStorage.clear(context)
    }

    fun handleTimeoutForfeit(context: Context) {
        val appContext = context.applicationContext
        val savedState = MatchStateStorage.getState(appContext) ?: return

        val settingsManager = GameSettingsManager(appContext)
        settingsManager.manageMusic(false)
        settingsManager.releaseResources()

        val matchId = savedState.matchId
        val opponent = savedState.opponent
        val type = savedState.type
        val p1Turn = savedState.isP1Turn

        if (type == MatchType.GUEST && !p1Turn) {
            MatchStateStorage.clear(context)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getCurrentUser()

            if (user != null) {
                if (type == MatchType.FRIEND) {
                    try {
                        val statusSnapshot = Tasks.await(database.getReference("matches").child(matchId).child("status").get())
                        val status = statusSnapshot.getValue(String::class.java)

                        val isAlreadyClosed = (status != null && (
                            status.startsWith("timeout_", ignoreCase = true)
                                    || status.startsWith("surrender_", ignoreCase = true)
                                    || status.startsWith("winner_", ignoreCase = true)
                            )
                        )

                        if (!isAlreadyClosed)
                            database.getReference("matches").child(matchId).child("status").setValue("surrender_${user.id}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val difficultyLabel = when (type) {
                    MatchType.FRIEND -> "Online (Timeout)"
                    MatchType.COMPUTER -> "AI (Timeout)"
                    MatchType.GUEST -> "Guest (Timeout)"
                }

                try {
                    userRepository.saveMatchRecord(
                        userId = user.id,
                        opponentName = opponent,
                        isVictory = false,
                        difficulty = difficultyLabel,
                        moves = emptyList()
                    )
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Failed to save local match record: ${e.message}")
                }
            }

            MatchStateStorage.clear(context)
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

    fun refreshAuthToken() {
        viewModelScope.launch(Dispatchers.IO) {
            val firebaseUser = FirebaseAuth.getInstance().currentUser

            if (firebaseUser != null) {
                try {
                    Tasks.await(firebaseUser.reload())
                    Log.d("Auth", "Token has been refreshed successfully.")
                } catch (e: Exception) {
                    Log.e("Auth", "Token could not be refreshed: ${e.message}")
                }
            }
        }
    }

    fun initializeSession(user: User, onComplete: (User, Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val newToken = UUID.randomUUID().toString()
            val userRef = database.getReference("users").child(user.id)

            var isActuallyNewUser = false

            try {
                val snapshot = Tasks.await(userRef.get())

                if (snapshot.exists())
                    userRef.child("sessionToken").setValue(newToken).await()

                var fleetToSave: List<SavedShip> = emptyList()

                val userToSave: User = if (snapshot.exists()) {
                    val profile = snapshot.child("profile")

                    val info = profile.child("info")
                    val progression = profile.child("progression")
                    val ranking = profile.child("ranking")
                    val statistics = profile.child("statistics")

                    val remoteUser = User(
                        id = user.id,

                        email = info.child("email").getValue(String::class.java) ?: user.email,

                        name = info.child("name").getValue(String::class.java) ?: user.name,
                        profilePictureUrl = info.child("profilePictureUrl").getValue(String::class.java)
                            ?: user.profilePictureUrl
                            ?: "ic_avatar_placeholder",

                        googleName = user.googleName,
                        googlePhotoUrl = user.googlePhotoUrl,

                        aiAvatarPath = info.child("aiAvatarPath").getValue(String::class.java)
                            ?: user.aiAvatarPath
                            ?: "ic_ai_avatar_placeholder",

                        registerEmail = info.child("registerEmail").getValue(String::class.java) ?: user.registerEmail,
                        password = info.child("password").getValue(String::class.java) ?: user.password,

                        countryCode = info.child("countryCode").getValue(String::class.java) ?: user.countryCode,

                        level = progression.child("level").getValue(Int::class.java) ?: 1,
                        currentXp = progression.child("currentXp").getValue(Int::class.java) ?: 0,
                        lastWinTimestamp = progression.child("lastWinTimestamp").getValue(Long::class.java) ?: 0L,

                        totalScore = ranking.child("totalScore").getValue(Long::class.java) ?: 0,

                        wins = statistics.child("wins").getValue(Int::class.java) ?: 0,
                        losses = statistics.child("losses").getValue(Int::class.java) ?: 0,
                        shipsDestroyed = statistics.child("shipsDestroyed").getValue(Int::class.java) ?: 0,
                        totalShotsFired = statistics.child("totalShotsFired").getValue(Long::class.java) ?: 0,
                        totalShotsHit = statistics.child("totalShotsHit").getValue(Long::class.java) ?: 0,

                        sessionToken = newToken,
                        lastUpdated = info.child("lastUpdated").getValue(Long::class.java) ?: System.currentTimeMillis()
                    )

                    val fleetSnapshot = snapshot.child("fleet")
                    val tempFleetList = mutableListOf<SavedShip>()

                    if (fleetSnapshot.exists()) {
                        for (shipSnap in fleetSnapshot.children) {
                            val ship = SavedShip(
                                userId = user.id,
                                shipId = (shipSnap.child("shipId").value as? Number)?.toInt() ?: 0,
                                size = (shipSnap.child("size").value as? Number)?.toInt() ?: 1,
                                row = (shipSnap.child("row").value as? Number)?.toInt() ?: 0,
                                col = (shipSnap.child("col").value as? Number)?.toInt() ?: 0,
                                isHorizontal = shipSnap.child("isHorizontal").getValue(Boolean::class.java) ?: true
                            )

                            tempFleetList.add(ship)
                        }
                    }

                    fleetToSave = tempFleetList

                    remoteUser
                }
                else {
                    isActuallyNewUser = true
                    user.copy(sessionToken = newToken)
                }

                userDao.insertUser(userToSave)

                if (fleetToSave.isNotEmpty()) {
                    fleetDao.clearUserFleet(user.id)
                    fleetDao.saveFleet(fleetToSave)
                    Log.d("Session", "Fleet restored correctly after user insert.")
                }

                if (isActuallyNewUser)
                    userRepository.syncLocalToCloud(userToSave.id)

                withContext(Dispatchers.Main) {
                    onComplete(userToSave, isActuallyNewUser)
                }
            } catch (e: Exception) {
                Log.e("Session", "Error while inserting the token: ${e.message}")

                withContext(Dispatchers.Main) {
                    onComplete(user.copy(sessionToken = newToken), true)
                }
            }
        }
    }

    fun restoreSessionListener() {
        val currentUser = userState.value

        if (currentUser != null && !currentUser.sessionToken.isNullOrEmpty()) {
            startSessionObserver(currentUser.id, currentUser.sessionToken)
            Log.d("Session", "Listener ripristinato manualmente dopo errore.")
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
                else {
                    Log.w("ProfileViewModel", "Server returned empty list. Using fallback.")
                    _flagList.value = fallbackFlags
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error while fetching flags: ${e.message}")
                _flagList.value = fallbackFlags
            }
        }
    }

    private fun startSessionObserver(userId: String, localToken: String) {
        if (sessionListener != null && monitoredUserId == userId && monitoredSessionToken == localToken) return

        removeSessionListener()

        monitoredUserId = userId
        monitoredSessionToken = localToken

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

    fun removeSessionListener() {
        if (sessionListener != null && monitoredUserId != null) {
            database.getReference("users")
                .child(monitoredUserId!!)
                .removeEventListener(sessionListener!!)
            sessionListener = null
            monitoredUserId = null
            monitoredSessionToken = null
        }
    }

    fun updateCountry(countryCode: String) {
        val currentUser = userState.value ?: return

        val updatedUser = currentUser.copy(countryCode = countryCode)
        viewModelScope.launch(Dispatchers.IO) {
            try {
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
                userDao.updateUserName(currentUser.id, newName)
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
                result.onSuccess {
                    onResult(true, it)
                }.onFailure { error ->
                    onResult(false, error.message ?: "Unknown error occurred.")
                }
            }
        }
    }

    fun deleteAccount(user: User, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = userRepository.deleteAccount(user)

            if (result.isSuccess) {
                onResult(true, null)
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "An unknown error occurred."
                onResult(false, errorMessage)
            }
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

            result.onSuccess {
                onResult(true, it)
            }.onFailure { error ->
                onResult(false, error.message ?: "Unknown error occurred.")
            }
        }
    }

    fun addMiniGameWinReward(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUserById(userId) ?: return@launch

            val newXp = user.currentXp + 5
            val newTotalScore = user.totalScore + 5L
            val now = System.currentTimeMillis()

            var currentLevel = user.level
            var adjustedXp = newXp
            if (adjustedXp >= 100) {
                currentLevel += 1
                adjustedXp -= 100
            }

            val updatedUser = user.copy(
                level = currentLevel,
                currentXp = adjustedXp,
                totalScore = newTotalScore,
                lastWinTimestamp = now
            )
            userDao.updateUser(updatedUser)

            userRepository.syncLocalToCloud(userId)

            Log.d("ProfileViewModel", "Reward granted: +5 XP, +5 Score. New Score: $newTotalScore")
        }
    }

    fun getCooldownRemaining(): Long {
        val user = userState.value ?: return 0L

        val lastWin = user.lastWinTimestamp
        val now = System.currentTimeMillis()
        val cooldownPeriod = 3600000L

        val remaining = (lastWin + cooldownPeriod) - now

        return if (remaining > 0L) remaining else 0L
    }

    fun updateAiAvatar(context: Context, bitmap: Bitmap) {
        val currentUser = userState.value ?: return
        viewModelScope.launch {
            try {
                val filePath = withContext(Dispatchers.IO) {
                    val fileName = "ai_avatar_${currentUser.id}.jpg"
                    val file = File(context.filesDir, fileName)
                    if (file.exists()) file.delete()

                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }

                    file.absolutePath
                }

                val updatedUser = currentUser.copy(aiAvatarPath = filePath)

                withContext(Dispatchers.IO) {
                    userDao.updateUser(updatedUser)
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
            return ProfileViewModel(userDao, fleetDao, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}