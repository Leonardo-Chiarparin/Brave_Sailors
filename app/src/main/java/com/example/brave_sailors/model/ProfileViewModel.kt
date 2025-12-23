package com.example.brave_sailors.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.data.local.database.UserDao
import com.example.brave_sailors.data.local.database.entity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ProfileViewModel(private val userDao: UserDao) : ViewModel() {

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState

    fun loadUser(userId: String) {
        viewModelScope.launch {
            userDao.observeUserById(userId).collectLatest { user ->
                _userState.value = user
            }
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
                Log.d("ProfileViewModel", "Country has been changed to: $countryCode")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error during the update: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    fun updateName(newName: String) {
        viewModelScope.launch {
            val currentUser = userState.value

            if (currentUser != null && newName.isNotBlank()) {
                userDao.updateUserName(currentUser.id, newName)
            }
        }
    }
    fun updateProfilePicture(context: Context, originalBitmap: Bitmap) {
        val currentUser = _userState.value ?: return

        viewModelScope.launch {
            try {
                val processedBitmap = withContext(Dispatchers.Default) {
                    applyGrayscaleToBitmap(originalBitmap)
                }

                val updateTime = System.currentTimeMillis()
                val filePath = withContext(Dispatchers.IO) {
                    val fileName = "avatar_${currentUser.id}.jpg"
                    val file = File(context.filesDir, fileName)

                    FileOutputStream(file).use { out ->
                        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }

                    file.absolutePath
                }

                withContext(Dispatchers.IO) {
                    userDao.updateProfilePicture(currentUser.id, filePath, updateTime)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun applyGrayscaleToBitmap(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint()
        val colorMatrix = ColorMatrix()

        colorMatrix.setSaturation(0f)

        val filter = ColorMatrixColorFilter(colorMatrix)

        paint.colorFilter = filter
        canvas.drawBitmap(src, 0f, 0f, paint)

        return dest
    }
}