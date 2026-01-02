package com.example.brave_sailors.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.R
import com.example.brave_sailors.data.local.database.FleetDao // Ensure this points to the correct package
import com.example.brave_sailors.data.local.database.UserDao // Ensure this points to the correct package
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.remote.api.Flag
import com.example.brave_sailors.data.remote.api.RetrofitClient
import com.example.brave_sailors.data.repository.UserRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProfileViewModel(
    private val userDao: UserDao,
    private val userRepository: UserRepository // Injected via Constructor
) : ViewModel() {

    // -- User State --
    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState

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

    var showHomeWelcome: Boolean = false

    init {
        fetchFlags()
    }

    /**
     * Initializes the user session token.
     * If not a new user, it updates the token in Firebase and Local DB.
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
                // [ NOTE ]: Ensure RetrofitClient.api is correct based on your file setup
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

    fun loadUser(userId: String) {
        viewModelScope.launch {
            userDao.observeUserById(userId).collectLatest { user ->
                _userState.value = user
                if (user != null && !user.sessionToken.isNullOrEmpty()) {
                    startSessionObserver(user.id, user.sessionToken)
                }
            }
        }
    }

    fun registerUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userDao.insertUser(user)
                // Sync data to cloud immediately
                userRepository.syncLocalToCloud(user)
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

            withContext(Dispatchers.Main) {
                _forceLogoutEvent.value = false
                _userState.value = null
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
        val currentUser = _userState.value
        if (currentUser == null) {
            Log.e("ProfileViewModel", "Error: User is null.")
            return
        }
        val updatedUser = currentUser.copy(countryCode = countryCode)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userDao.updateUser(updatedUser)
                _userState.value = updatedUser
                Log.d("ProfileViewModel", "Country has been changed to: $countryCode")
                // Sync update
                userRepository.syncLocalToCloud(updatedUser)
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
                _userState.value = updatedUser
                // Sync update
                userRepository.syncLocalToCloud(updatedUser)
            }
        }
    }

    fun updateProfilePicture(context: Context, bitmap: Bitmap) {
        val currentUser = _userState.value ?: return
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
                    // Sync update
                    userRepository.syncLocalToCloud(updatedUser)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreGoogleProfile() {
        val currentUser = _userState.value ?: return
        val restoredUser = currentUser.copy(
            name = currentUser.googleName,
            profilePictureUrl = currentUser.googlePhotoUrl,
            lastUpdated = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            userDao.updateUser(restoredUser)
            Log.d("ProfileViewModel", "Profile restored to Google defaults")
            // Sync update
            userRepository.syncLocalToCloud(restoredUser)
        }
    }

    override fun onCleared() {
        super.onCleared()
        removeSessionListener()
    }
}

// Factory Class to handle Dependency Injection from MainActivity
class ProfileViewModelFactory(
    private val userDao: UserDao,
    private val fleetDao: FleetDao, // Kept to match MainActivity params, though unused inside ProfileViewModel
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Pass only what the ViewModel constructor needs
            return ProfileViewModel(userDao, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}