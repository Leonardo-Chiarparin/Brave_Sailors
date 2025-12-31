package com.example.brave_sailors.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.brave_sailors.data.local.database.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // Used for both Registration (New User) and Syncing Google Data (Existing User)
    // REPLACE works as an "Upsert": if ID exists, it updates; otherwise, it inserts.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    // Used in GoogleSigningUseCase to check if the user exists
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): User?

    // Generic update for the whole object
    @Update
    suspend fun updateUser(user: User)

    // Used in ViewModel to observe changes in the UI
    @Query("SELECT * FROM users WHERE id = :id")
    fun observeUserById(id: String): Flow<User?>

    // Used in MainActivity to check if auto-login is possible
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): User?

    // --- SECURITY / SESSION MANAGEMENT ---

    // REQUIRED: Used to clear local data upon Logout or Session Conflict
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    // --- SPLIT IDENTITY QUERIES ---

    // Updates ONLY the Game Name.
    // The 'googleName' field remains untouched (preserving the original identity).
    @Query("UPDATE users SET name = :name WHERE id = :id")
    suspend fun updateUserName(id: String, name: String)

    // Updates the country code separately.
    @Query("UPDATE users SET countryCode = :countryCode WHERE id = :id")
    suspend fun updateCountryCode(id: String, countryCode: String)

    // Updates ONLY the Game Avatar.
    // The 'googlePhotoUrl' field remains untouched (preserving the original image).
    @Query("UPDATE users SET profilePictureUrl = :url, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateProfilePicture(id: String, url: String, timestamp: Long)
}