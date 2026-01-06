package com.example.brave_sailors.domain.use_case

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import coil.imageLoader
import coil.request.ImageRequest
import com.example.brave_sailors.data.local.database.UserDao
import com.example.brave_sailors.data.local.database.entity.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

sealed class SigningResult {
    data class Success(val user: User, val isNewUser: Boolean) : SigningResult()
    object Cancelled : SigningResult()
    data class Error(val msg: String) : SigningResult()
}

class GoogleSigningUseCase(
    private val userDao: UserDao,
    private val webClientId: String
) {
    // Instance for Realtime Database
    private val database = FirebaseDatabase.getInstance()

    /**
     * @param context Android Context
     * @param onlySilentLogin
     * - If TRUE (Home Screen): Tries to silently log in to show the "Welcome Back" pill.
     * - If FALSE (Login Screen): Forces the interactive account picker.
     */
    suspend operator fun invoke(context: Context, onlySilentLogin: Boolean = false): SigningResult = withContext(Dispatchers.IO) {
        val credentialManager = CredentialManager.create(context)

        try {
            // 1. LOCAL CHECK
            // If a user is already in the local DB, use it without calling Google.
            if (!onlySilentLogin) {
                val user = userDao.getCurrentUser()
                if (user != null) {
                    prepareImage(context, user.profilePictureUrl)
                    return@withContext SigningResult.Success(user, isNewUser = false)
                }
            }

            // 2. PREPARE OPTIONS
            // Determine whether to attempt silent login or go straight to the picker.

            val result = if (onlySilentLogin) {
                // CASE A: Home Screen (Silent Check)
                // Attempt to retrieve credentials silently.
                try {
                    val silentOption = setOption(webClientId, filterAuthorized = true, autoSelect = true)
                    val silentRequest = GetCredentialRequest.Builder().addCredentialOption(silentOption).build()
                    credentialManager.getCredential(request = silentRequest, context = context)
                } catch (e: NoCredentialException) {
                    // Silent login failed, do nothing on the home screen.
                    return@withContext SigningResult.Cancelled
                }
            } else {
                // CASE B: Login Screen (Explicit)
                // We SKIP the silent login attempt here because we want to allow account switching.
                // We go straight to the interactive request with filterAuthorized = FALSE.

                val interactiveOption = setOption(
                    webClientId,
                    filterAuthorized = false, // CRITICAL: Shows all Google accounts on the device
                    autoSelect = false        // CRITICAL: Forces the selection popup, disables auto-login
                )

                val interactiveRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(interactiveOption)
                    .build()

                credentialManager.getCredential(
                    request = interactiveRequest,
                    context = context
                )
            }

            val credential = result.credential

            // 3. CREDENTIAL PARSING
            var gToken = ""
            var gEmail = ""
            var gName = "Sailor"
            var gPhoto: String? = null

            if (credential is GoogleIdTokenCredential) {
                gToken = credential.idToken
                gEmail = credential.id
                gName = credential.displayName ?: "Sailor"
                gPhoto = credential.profilePictureUri?.toString()
            }
            // Fallback for version mismatches (CustomCredential)
            else if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val data = credential.data
                    gToken = data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN") ?: ""
                    gEmail = data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID") ?: ""
                    gName = data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_DISPLAY_NAME") ?: "Sailor"
                    gPhoto = data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_PROFILE_PICTURE_URI")
                } catch (e: Exception) {
                    return@withContext SigningResult.Error("Error parsing CustomCredential: ${e.message}")
                }
            } else {
                return@withContext SigningResult.Error("Credential type not recognized.")
            }

            // 4. REALTIME DATABASE SESSION
            val currentSessionToken = UUID.randomUUID().toString()

            // Prepare Google ID to be used as a Key (emails cannot be keys directly)
            var googleId = getIDFromToken(gToken)
            if (googleId.isEmpty()) {
                googleId = gEmail.replace(".", "_")
            }

            try {
                val userData = hashMapOf(
                    "name" to gName,
                    "email" to gEmail,
                    "sessionToken" to currentSessionToken,
                    "lastLogin" to System.currentTimeMillis()
                )

                // Write/Merge to Realtime Database
                database.getReference("users")
                    .child(googleId)
                    .updateChildren(userData as Map<String, Any>)
                    .await()

            } catch (e: Exception) {
                Log.e("GoogleSigning", "Firebase DB write failed: ${e.message}")
            }

            // 5. FINALIZE LOCAL LOGIN
            return@withContext handleSuccess(
                userId = googleId,
                email = gEmail,
                googleDisplayName = gName,
                googlePhotoUrl = gPhoto,
                sessionToken = currentSessionToken,
                context = context
            )

        } catch (e: GetCredentialCancellationException) {
            Log.d("SigningUseCase", "Login cancelled by user.")
            return@withContext SigningResult.Cancelled
        } catch (e: Exception) {
            Log.e("SigningUseCase", "Login error: ${e.message}")
            return@withContext SigningResult.Error(e.message ?: "Unknown error.")
        }
    }

    // Helper to merge Google Identity with Local Persistence
    private suspend fun handleSuccess(
        userId: String,
        email: String,
        googleDisplayName: String,
        googlePhotoUrl: String?,
        sessionToken: String,
        context: Context
    ): SigningResult {

        val existingUser = userDao.getUserById(userId)
        val safeGooglePhotoUrl = if (googlePhotoUrl.isNullOrEmpty()) "ic_terms" else googlePhotoUrl

        val userToReturn = if (existingUser != null) {
            // Update existing user with new Google info and session token
            existingUser.copy(
                googleName = googleDisplayName,
                googlePhotoUrl = safeGooglePhotoUrl,
                sessionToken = sessionToken
            )
        } else {
            // Create new user structure
            User(
                id = userId,
                email = email,
                name = googleDisplayName,
                profilePictureUrl = null,
                googleName = googleDisplayName,
                googlePhotoUrl = safeGooglePhotoUrl,
                sessionToken = sessionToken,
                countryCode = null
            )
        }

        val isNew = (existingUser == null)
        prepareImage(context, userToReturn.googlePhotoUrl)

        return SigningResult.Success(userToReturn, isNewUser = isNew)
    }

    private fun setOption(clientId: String, filterAuthorized: Boolean, autoSelect: Boolean): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterAuthorized)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(autoSelect)
            .build()
    }

    private fun getIDFromToken(idToken: String): String {
        try {
            val parts = idToken.split(".")
            if (parts.size >= 2) {
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE), Charsets.UTF_8)
                val jsonObject = JSONObject(payload)
                return jsonObject.optString("sub", "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private suspend fun prepareImage(context: Context, url: String?) {
        if (!url.isNullOrEmpty() && url != "ic_terms") {
            try {
                val imgRequest = ImageRequest.Builder(context)
                    .data(url)
                    .build()

                context.imageLoader.execute(imgRequest)
            } catch (e: Exception) {
                Log.e("GoogleSigning", "Failed to preload image: ${e.message}")
            }
        }
    }
}