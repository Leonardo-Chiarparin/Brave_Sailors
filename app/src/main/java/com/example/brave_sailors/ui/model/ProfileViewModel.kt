package com.example.brave_sailors.ui.model

import android.content.Context
import android.graphics.Bitmap
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

    // Carica l'utente dal database usando l'ID ricevuto dalla MainActivity
    fun loadUser(userId: String) {
        viewModelScope.launch {
            val user = userDao.getUserById(userId)
            _userState.value = user
        }
    }

    // Gestisce il salvataggio della foto e l'aggiornamento del DB
    fun updateProfilePicture(context: Context, bitmap: Bitmap) {
        val currentUser = _userState.value ?: return

        viewModelScope.launch {
            try {
                // 1. Salvataggio fisico del file (Operazione I/O)
                val filePath = withContext(Dispatchers.IO) {
                    // Usiamo l'ID nel nome del file per evitare conflitti tra utenti diversi
                    val fileName = "avatar_${currentUser.id}.jpg"
                    val file = File(context.filesDir, fileName)

                    // Se il file esiste già, viene sovrascritto automaticamente
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    file.absolutePath
                }

                // 2. Aggiorniamo l'oggetto User
                // Importante: aggiorniamo 'lastUpdated' così Coil capisce che l'immagine è cambiata
                val updatedUser = currentUser.copy(
                    profilePictureUrl = filePath,
                    lastUpdated = System.currentTimeMillis()
                )

                // 3. Scriviamo nel Database (Operazione I/O)
                withContext(Dispatchers.IO) {
                    userDao.updateUser(updatedUser)
                }

                // 4. Aggiorniamo lo stato della UI immediatamente
                _userState.value = updatedUser

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}