package com.example.brave_sailors.ui.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brave_sailors.data.local.database.UserDao
import com.example.brave_sailors.data.local.database.entity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ProfileViewModel(private val userDao: UserDao) : ViewModel() {

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState

    fun loadUser(userId: String) {
        viewModelScope.launch {
            val user = userDao.getUserById(userId)
            _userState.value = user
        }
    }

    fun updateProfilePicture(context: Context, originalBitmap: Bitmap) {
        val currentUser = _userState.value ?: return

        viewModelScope.launch {
            try {
                // 1. PROCESSING: Convert to Black and White (Grayscale)
                // Use Default dispatcher for CPU-intensive calculations
                val processedBitmap = withContext(Dispatchers.Default) {
                    applyGrayscaleToBitmap(originalBitmap)
                }

                // 2. SAVING: Write the processed file
                val filePath = withContext(Dispatchers.IO) {
                    val fileName = "avatar_${currentUser.id}.jpg"
                    val file = File(context.filesDir, fileName)

                    FileOutputStream(file).use { out ->
                        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    file.absolutePath
                }

                // 3. DB UPDATE
                val updatedUser = currentUser.copy(
                    profilePictureUrl = filePath,
                    lastUpdated = System.currentTimeMillis() // Forces UI refresh
                )

                withContext(Dispatchers.IO) {
                    userDao.updateUser(updatedUser)
                }

                _userState.value = updatedUser

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Native Android function to convert to Grayscale.
     * Equivalent to OpenCV's Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY),
     * but does not require heavy external libraries.
     */
    private fun applyGrayscaleToBitmap(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height

        // Create a destination bitmap
        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(dest)
        val paint = Paint()

        // Saturation matrix at 0 = Black and White
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)

        val filter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = filter

        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }
}