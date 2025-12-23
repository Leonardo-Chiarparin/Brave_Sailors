package com.example.brave_sailors.domain.use_case

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
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
            val user = userDao.getCurrentUser()

            if (user != null) {
                prepareImage(context, user.profilePictureUrl)
                return@withContext SigningResult.Success(user)
            }

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

            if (credential is GoogleIdTokenCredential) {
                val googleIdToken = credential.idToken
                val googleEmail = credential.id
                val googleName = credential.displayName ?: "Sailor"
                val googlePhoto = credential.profilePictureUri?.toString()

                var userId = getIDFromToken(googleIdToken)
                userId = userId.ifEmpty { googleEmail }

                var entry = userDao.getUserById(userId)

                if (entry == null) {
                    entry = User(
                        id = userId,
                        name = googleName,
                        email = googleEmail,
                        profilePictureUrl = googlePhoto
                    )

                    userDao.insertUser(entry)
                }

                prepareImage(context, entry.profilePictureUrl)
                return@withContext SigningResult.Success(entry)

            } else {
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