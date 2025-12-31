package com.example.brave_sailors.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.R
import com.example.brave_sailors.data.local.database.UserDao
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.remote.api.Flag
import com.example.brave_sailors.data.remote.api.RetrofitInstance
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

class ProfileViewModel(private val userDao: UserDao) : ViewModel() {

    // -- User State --
    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState

    // [ TO - DO ]: Fix the following instructions in order to be let the flags be ordered according to their name ( the amount of flags has to be the greatest possible )
    // -- Helpers regarding countries --
    private val packageName = "com.example.brave_sailors"

    private fun localUri(resourceId: Int): String {
        return "android.resource://$packageName/$resourceId"
    }

    private val fallbackFlags = listOf(
        Flag("Italy", "IT", localUri(R.drawable.ic_flag_italy)),
        Flag("Spain", "ES", localUri(R.drawable.ic_flag_spain)),
        Flag("France", "FR", localUri(R.drawable.ic_flag_france)),
        Flag("United Kingdom", "GB", localUri(R.drawable.ic_flag_uk)),
        Flag("Belgium", "BE", localUri(R.drawable.ic_flag_belgium)),
        Flag("China", "CN", localUri(R.drawable.ic_flag_china)),
        Flag("Russia", "RU", localUri(R.drawable.ic_flag_russia))
    )

    // -- Flag List State (From PythonAnywhere API) --
    private val _flagList = MutableStateFlow<List<Flag>>(fallbackFlags)
    val flagList: StateFlow<List<Flag>> = _flagList.asStateFlow()

    // -- Session / Security State --
    private val _forceLogoutEvent = MutableStateFlow(false)
    val forceLogoutEvent = _forceLogoutEvent.asStateFlow()

    // -- Firebase Realtime Database --
    private val database = FirebaseDatabase.getInstance()
    private var sessionListener: ValueEventListener? = null
    private var monitoredUserId: String? = null // Keeps track of which ID we are listening to

    // "Welcome Back" Popup
    var showHomeWelcome: Boolean = false

    init {
        // Fetch flags immediately upon creation
        fetchFlags()
    }

    fun initializeSession(user: User, isNewUser: Boolean, onComplete: (User) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val newToken = UUID.randomUUID().toString()

            val updatedUser = user.copy(sessionToken = newToken)

            if (!isNewUser) {
                try {
                    val userRef = database.getReference("users").child(user.id)

                    Tasks.await(userRef.child("sessionToken").setValue(newToken))

                    userDao.insertUser(updatedUser)

                    Log.d("Session", "Token updated: $newToken")
                } catch (e: Exception) {
                    Log.e("Session", "Error while inserting the token: ${e.message}")
                }
            } else
                Log.d("Session", "New user detected: $newToken")

            withContext(Dispatchers.Main) {
                onComplete(updatedUser)
            }
        }
    }

    // -- API Logic (PythonAnywhere) --
    private fun fetchFlags() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("ProfileViewModel", "Starting flags download...")

                val flags = RetrofitInstance.api.getFlags()

                if (flags.isNotEmpty()) {
                    _flagList.value = flags
                    Log.d("ProfileViewModel", "Flags downloaded successfully.")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error while fetching flags: ${e.message}")

                _flagList.value = fallbackFlags
            }
        }
    }

    // -- User Logic & Session Management --

    // Loads the user from Room and starts the Realtime Database Session Observer
    fun loadUser(userId: String) {
        viewModelScope.launch {
            userDao.observeUserById(userId).collectLatest { user ->
                _userState.value = user

                // If user exists and has a session token, start watching for concurrency
                if (user != null && !user.sessionToken.isNullOrEmpty()) {
                    startSessionObserver(user.id, user.sessionToken)
                }
            }
        }
    }

    // 1. REGISTRATION
    fun registerUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userDao.insertUser(user)
                Log.d("ProfileViewModel", "User registered: ${user.id}")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error while inserting user: ${e.message}")
            }
        }
    }

    // 2. SESSION OBSERVER (Realtime Database Implementation)
    private fun startSessionObserver(userId: String, localToken: String) {
        // If we are already listening to this user, do nothing
        if (sessionListener != null && monitoredUserId == userId) return

        // Clean up previous listeners if user changed
        removeSessionListener()

        monitoredUserId = userId
        val userRef = database.getReference("users").child(userId)

        // Create the listener
        sessionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Read the token currently on the server
                val serverToken = snapshot.child("sessionToken").getValue(String::class.java)

                // CRITICAL CHECK:
                // If the token on the server is different from the one on this phone,
                // it means a new login occurred elsewhere. Trigger forced logout.
                if (serverToken != null && serverToken != localToken) {
                    Log.w("Session", "Session Conflict detected! Server: $serverToken vs Local: $localToken")
                    _forceLogoutEvent.value = true
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProfileViewModel", "Database listen failed: ${error.message}")
            }
        }

        // Attach the listener
        userRef.addValueEventListener(sessionListener!!)
    }

    // 3. LOGOUT (Cleanup)
    fun performLocalLogout() {
        removeSessionListener()

        viewModelScope.launch(Dispatchers.IO) {
            userDao.deleteAllUsers() // Clear local data
            _forceLogoutEvent.value = false // Reset trigger
            _userState.value = null
        }
    }

    // Helper to cleanly detach the listener
    private fun removeSessionListener() {
        if (sessionListener != null && monitoredUserId != null) {
            database.getReference("users")
                .child(monitoredUserId!!)
                .removeEventListener(sessionListener!!)
            sessionListener = null
            monitoredUserId = null
        }
    }

    // -- Profile Updates --

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
                Log.d("ProfileViewModel", "Country has been changed to: $countryCode")
                _userState.value = updatedUser
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error during update: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun updateName(newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUser = userState.value
            if (currentUser != null && newName.isNotBlank()) {
                userDao.updateUserName(currentUser.id, newName)
                // Flow will update automatically via observeUserById, but we update state here for immediate UI response
                val updatedUser = currentUser.copy(name = newName)
                _userState.value = updatedUser
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
                    // We don't need to manually update _userState here because 'loadUser' is observing the DB
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Restores the original Google Name and Photo (Backup Identity)
    fun restoreGoogleProfile() {
        val currentUser = _userState.value ?: return

        val restoredUser = currentUser.copy(
            name = currentUser.googleName,
            profilePictureUrl = currentUser.googlePhotoUrl,
            lastUpdated = System.currentTimeMillis()
        )

        viewModelScope.launch(Dispatchers.IO) {
            userDao.updateUser(restoredUser)
            // State updates automatically via Flow
            Log.d("ProfileViewModel", "Profile restored to Google defaults")
        }
    }

    // Lifecycle cleanup
    override fun onCleared() {
        super.onCleared()
        removeSessionListener()
    }
}