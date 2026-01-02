package com.example.brave_sailors.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction // ADDED: Necessary for the @Transaction annotation
import androidx.room.Update
import com.example.brave_sailors.data.local.database.entity.User
import kotlinx.coroutines.flow.Flow
import com.example.brave_sailors.data.local.database.entity.SavedShip

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

    @Query("UPDATE users SET level = :level, currentXp = :xp, lastWinTimestamp = :timestamp WHERE id = :id")
    suspend fun updateGameStats(id: String, level: Int, xp: Int, timestamp: Long)

    @Query("UPDATE users SET sessionToken = NULL WHERE id = :userId")
    suspend fun clearSession(userId: String)
}

@Dao
interface FleetDao {

    // Retrieves the list of ships saved by a specific user to reconstruct the fleet
    @Query("SELECT * FROM user_fleet WHERE userId = :userId")
    suspend fun getUserFleet(userId: String): List<SavedShip>

    // Inserts or updates the list of ships in bulk
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFleet(ships: List<SavedShip>)

    // Removes all ships associated with a user (useful before saving a new configuration)
    @Query("DELETE FROM user_fleet WHERE userId = :userId")
    suspend fun clearUserFleet(userId: String)

    // Atomic operation: clears the old fleet and saves the new one in a single transaction
    // This prevents data inconsistency (e.g., having duplicate ships or mixed states)
    @Transaction
    suspend fun replaceUserFleet(userId: String, ships: List<SavedShip>) {
        clearUserFleet(userId)
        saveFleet(ships)
    }
}