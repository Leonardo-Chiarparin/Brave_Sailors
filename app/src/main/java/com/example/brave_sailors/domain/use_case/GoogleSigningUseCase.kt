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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed class SigningResult {
    data class Success(val user: User) : SigningResult()
    object Cancelled : SigningResult()
    data class Error(val msg: String) : SigningResult()
}

class GoogleSigningUseCase(
    private val userDao: UserDao,
    private val webClientId: String
) {
    suspend operator fun invoke(context: Context): SigningResult = withContext(Dispatchers.IO) {
        val credentialManager = CredentialManager.create(context)

        try {
            // 1. Check if user is already logged in locally
            val user = userDao.getCurrentUser()
            if (user != null) {
                prepareImage(context, user.profilePictureUrl)
                return@withContext SigningResult.Success(user)
            }

            // 2. Attempt Silent Login first, then Interactive
            val result = try {
                val silentOption = setOption(
                    webClientId,
                    filterAuthorized = true,
                    autoSelect = true
                )

                val silentRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(silentOption)
                    .build()

                credentialManager.getCredential(
                    request = silentRequest,
                    context = context
                )
            } catch (e: NoCredentialException) {
                // If silent fails, open interactive popup
                val interactiveOption = setOption(
                    webClientId,
                    filterAuthorized = false,
                    autoSelect = false
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

            // 3. Credential Handling (Standard + Fallback)

            // CASE A: GoogleIdTokenCredential
            // Standard case: Works perfectly if libraries are aligned
            if (credential is GoogleIdTokenCredential) {
                return@withContext handleSuccess(
                    token = credential.idToken,
                    email = credential.id,
                    name = credential.displayName ?: "Sailor",
                    photoUrl = credential.profilePictureUri?.toString(),
                    context = context
                )
            }
            // CASE B: CustomCredential
            // Fallback case: Fixes "Credential type not recognized" when version mismatch occurs
            else if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val data = credential.data
                    val googleIdToken = data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                    val googleEmail = data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID")
                    val googleName = data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_DISPLAY_NAME")
                    val googlePhoto = data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_PROFILE_PICTURE_URI")

                    if (!googleIdToken.isNullOrEmpty() && !googleEmail.isNullOrEmpty()) {
                        return@withContext handleSuccess(
                            token = googleIdToken,
                            email = googleEmail,
                            name = googleName ?: "Sailor",
                            photoUrl = googlePhoto,
                            context = context
                        )
                    } else {
                        return@withContext SigningResult.Error("Missing data in CustomCredential.")
                    }
                } catch (e: Exception) {
                    return@withContext SigningResult.Error("Error parsing CustomCredential: ${e.message}")
                }
            }
            // CASE C: Unknown Type
            else {
                Log.e("AuthDebug", "Unknown Credential Type: ${credential.type} (${credential.javaClass.simpleName})")
                return@withContext SigningResult.Error("Credential type not recognized.")
            }

        } catch (e: GetCredentialCancellationException) {
            Log.d("SigningUseCase", "Login cancelled.")
            return@withContext SigningResult.Cancelled
        } catch (e: Exception) {
            Log.e("SigningUseCase", "Login error: ${e.message}")
            return@withContext SigningResult.Error(e.message ?: "Unknown error.")
        }
    }

    // Helper function to save user and prevent code duplication
    private suspend fun handleSuccess(
        token: String,
        email: String,
        name: String,
        photoUrl: String?,
        context: Context
    ): SigningResult {
        var userId = getIDFromToken(token)
        // Fallback if token decoding fails
        userId = userId.ifEmpty { email }

        var entry = userDao.getUserById(userId)

        if (entry == null) {
            entry = User(
                id = userId,
                name = name,
                email = email,
                profilePictureUrl = photoUrl
            )
            userDao.insertUser(entry)
        }

        prepareImage(context, entry.profilePictureUrl)
        return SigningResult.Success(entry)
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

    private fun prepareImage(context: Context, url: String?) {
        if (!url.isNullOrEmpty() && url != "ic_terms") {
            try {
                val imgRequest = ImageRequest.Builder(context)
                    .data(url)
                    .build()
                context.imageLoader.enqueue(imgRequest)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}