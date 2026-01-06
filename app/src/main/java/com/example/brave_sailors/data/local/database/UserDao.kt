package com.example.brave_sailors.data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.local.database.entity.FriendEntity
import kotlinx.coroutines.flow.Flow
import com.example.brave_sailors.data.local.database.entity.SavedShip

@Dao
interface UserDao {

    // Inserts a user into the local DB.
    // Used both for:
    // - Registration (new user)
    // - Syncing Google data (existing user)
    // REPLACE ensures we overwrite the local row if the same id already exists.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    // Returns a reactive stream of the (single) stored user.
    // Emits updates whenever the user row changes.
    @Query("SELECT * FROM users LIMIT 1")
    fun getUserFlow(): Flow<User?>

    // Retrieves a user by id (one-shot).
    // Used during Google sign-in to check whether the user already exists locally.
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): User?

    // Updates the entire user entity.
    // Use when you already have the full object with all fields.
    @Update
    suspend fun updateUser(user: User)

    // Observes a user by id as a Flow.
    // Useful for ViewModels to keep the UI in sync with DB changes.
    @Query("SELECT * FROM users WHERE id = :id")
    fun observeUserById(id: String): Flow<User?>

    // Returns the currently stored user (one-shot).
    // Used in MainActivity (or similar) to check if auto-login is possible.
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): User?

    // --- SECURITY / SESSION MANAGEMENT ---

    // Clears all local users.
    // REQUIRED: use on logout or when a session conflict happens to wipe local identity data.
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    // Deletes the provided user row.
    // Useful when you want to remove a specific user instance instead of wiping the whole table.
    @Delete
    suspend fun delete(user: User)

    // --- PARTIAL (FIELD-LEVEL) UPDATES ---

    // Updates only the user's name.
    // Avoids rewriting the whole row when only one field changes.
    @Query("UPDATE users SET name = :name WHERE id = :id")
    suspend fun updateUserName(id: String, name: String)

    // Updates only the user's country code.
    @Query("UPDATE users SET countryCode = :countryCode WHERE id = :id")
    suspend fun updateCountryCode(id: String, countryCode: String)

    // Updates only the profile picture URL and the lastUpdated timestamp.
    // Useful when the remote profile image changes and you want to track freshness locally.
    @Query("UPDATE users SET profilePictureUrl = :url, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateProfilePicture(id: String, url: String, timestamp: Long)

    // Updates game-related stats and the timestamp of the last win.
    // Keeping this as a partial update avoids overwriting unrelated fields.
    @Query("UPDATE users SET level = :level, currentXp = :xp, lastWinTimestamp = :timestamp WHERE id = :id")
    suspend fun updateGameStats(id: String, level: Int, xp: Int, timestamp: Long)

    // Clears the session token (sets it to NULL) for the given user.
    // Typically called on logout or when the token is invalid/expired.
    @Query("UPDATE users SET sessionToken = NULL WHERE id = :userId")
    suspend fun clearSession(userId: String)
}

@Dao
interface FleetDao {

    // Loads the saved fleet for a given user.
    // Returns a one-shot list (not reactive).
    @Query("SELECT * FROM user_fleet WHERE userId = :userId")
    suspend fun getUserFleet(userId: String): List<SavedShip>

    // Saves a list of ships to the local DB.
    // REPLACE updates existing rows (same primary key) and inserts missing ones.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFleet(ships: List<SavedShip>)

    // Deletes all ships saved for the given user.
    // Used to reset the fleet locally (e.g., on logout or re-sync).
    @Query("DELETE FROM user_fleet WHERE userId = :userId")
    suspend fun clearUserFleet(userId: String)

    // Atomically replaces the entire fleet for a user:
    // 1) clear existing fleet rows
    // 2) insert the new list
    // @Transaction ensures both steps are treated as one DB operation.
    @Transaction
    suspend fun replaceUserFleet(userId: String, ships: List<SavedShip>) {
        clearUserFleet(userId)
        saveFleet(ships)
    }
}

@Dao
interface FriendDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Query("SELECT * FROM friends")
    fun getAllFriendsFlow(): Flow<List<FriendEntity>>
}